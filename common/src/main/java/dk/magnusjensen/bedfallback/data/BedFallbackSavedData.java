/*
 *  BedFallback, a Minecraft mod that stores last bed spawn positions
 *  Copyright (C) 2025 legenden (MagnusHJensen)
 *
 *  This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.magnusjensen.bedfallback.data;

import dk.magnusjensen.bedfallback.Constants;
import dk.magnusjensen.bedfallback.config.ServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class BedFallbackSavedData extends SavedData {
    public static final String DATA_NAME = "bed_fallbacks_data";

    private static final String DATA_FILE_NAME = DATA_NAME + ".dat";

    // Map of player UUID to linked hash set of global positions of bed spawns, across every dimension
    // We use a hash set to get O(1) lookups and linked to preserve insertion order
    private Map<UUID, LinkedHashSet<GlobalPos>> lastBedSpawnPositions = new HashMap<>();

    // Set by the no-argument constructor, which only the storage's factory calls. Decoded instances go through the
    // map constructor and leave this false, so it tells the two apart. See loadFromDisk.
    private transient boolean freshlyCreated;

    public static final SavedDataType<BedFallbackSavedData> ID = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(Constants.MOD_ID, DATA_NAME),

        BedFallbackSavedData::new,
        CompoundTag.CODEC.xmap(
            BedFallbackSavedData::load,
            bedFallbackSavedData -> bedFallbackSavedData.save(new CompoundTag())
        ),
        null
    );

    public void addBedSpawnPosition(UUID playerUUID, GlobalPos bedPos) {
        Constants.LOG.debug("Adding bed spawn position {} for player {}", bedPos, playerUUID);
        var positions = lastBedSpawnPositions.computeIfAbsent(playerUUID, k -> new LinkedHashSet<>());
        var didAdd = positions.add(bedPos);
        if (!didAdd) {
            // We remove the entry and then re-add it as the old spawn was overridden at a newer time
            positions.remove(bedPos);
            positions.add(bedPos);
        }

        trimToMaximum(playerUUID, positions);

        setDirty();
    }

    // Oldest first, so dropping from the front of the insertion order drops the least recently used bed.
    private static void trimToMaximum(UUID playerUUID, LinkedHashSet<GlobalPos> positions) {
        var iterator = positions.iterator();
        while (positions.size() > ServerConfig.CONFIG.maximumBedFallbacks && iterator.hasNext()) {
            var oldestPos = iterator.next();
            Constants.LOG.debug("Removing oldest bed spawn position {} for player {} to enforce maximum of {}", oldestPos, playerUUID, ServerConfig.CONFIG.maximumBedFallbacks);
            iterator.remove();
        }
    }

    // Walks newest to oldest and drops every position that no longer holds a bed on the way, so the newest bed that
    // still stands is the one returned. Only a player breaking a bed reaches removeBedSpawnPosition; a creeper, a
    // piston or lava takes one away without telling us, so the list is only known to match the world when it is read.
    // A position in a dimension the server no longer has counts as gone too, which is what happens when the mod that
    // added that dimension is removed.
    @Nullable
    public GlobalPos findLastStandingBedSpawnPosition(MinecraftServer server, UUID playerUUID) {
        LinkedHashSet<GlobalPos> positions = lastBedSpawnPositions.get(playerUUID);
        if (positions == null) {
            return null;
        }

        // A snapshot, because dropping a position below writes to the very set this walks.
        List<GlobalPos> oldestFirst = List.copyOf(positions);
        for (int i = oldestFirst.size() - 1; i >= 0; i--) {
            GlobalPos bedPos = oldestFirst.get(i);
            ServerLevel level = server.getLevel(bedPos.dimension());
            if (level != null && level.getBlockState(bedPos.pos()).is(BlockTags.BEDS)) {
                return bedPos;
            }

            Constants.LOG.debug("Dropping bed spawn position {} for player {}, no bed stands there any more", bedPos, playerUUID);
            removeBedSpawnPosition(bedPos);
        }

        Constants.LOG.debug("No recorded bed is still standing for player {}", playerUUID);
        return null;
    }

    public boolean hasBedSpawnPosition(UUID playerUUID, GlobalPos pos) {
        LinkedHashSet<GlobalPos> positions = lastBedSpawnPositions.get(playerUUID);
        return positions != null && positions.contains(pos);
    }

    public void removeBedSpawnPosition(GlobalPos bedPos) {
        Constants.LOG.debug("Removing bed spawn position {}", bedPos);
        // Loop over all players and remove the bed position if it exists. A player left with no positions drops out
        // of the map, which has to go through the iterator: removing from the map itself here would fail the walk.
        var entries = lastBedSpawnPositions.entrySet().iterator();
        while (entries.hasNext()) {
            var positions = entries.next().getValue();
            if (positions.remove(bedPos)) {
                if (positions.isEmpty()) {
                    entries.remove();
                }
                setDirty();
            }
        }
    }

    public static BedFallbackSavedData load(CompoundTag compoundTag) {
        return new BedFallbackSavedData(parsePositions(compoundTag));
    }

    private static Map<UUID, LinkedHashSet<GlobalPos>> parsePositions(CompoundTag compoundTag) {
        var lastBedSpawnPositions = new HashMap<UUID, LinkedHashSet<GlobalPos>>();
        var bedFallbackNBT = compoundTag.getCompoundOrEmpty("bed_fallbacks");
        for (String key : bedFallbackNBT.keySet()) {
            var playerUUID = UUID.fromString(key);
            var positionsList = bedFallbackNBT.getCompoundOrEmpty(key);
            var size = positionsList.getIntOr("size", 0);
            var positionsSet = new LinkedHashSet<GlobalPos>();
            for (int i = 0; i < size; i++) {
                var posTag = positionsList.getCompoundOrEmpty("" + i);
                var pos = new BlockPos(posTag.getIntOr("x", 0), posTag.getIntOr("y", 0), posTag.getIntOr("z", 0));
                // Positions written before the mod tracked dimensions were all overworld ones, so that is what a
                // missing key means. Anything unparsable is dropped rather than guessed at.
                var dimension = parseDimension(posTag.getStringOr("dimension", ""));
                if (dimension != null) {
                    positionsSet.add(GlobalPos.of(dimension, pos));
                }
            }
            lastBedSpawnPositions.put(playerUUID, positionsSet);
        }
        return lastBedSpawnPositions;
    }

    public BedFallbackSavedData() {
        this.freshlyCreated = true;
    }

    public BedFallbackSavedData(Map<UUID, LinkedHashSet<GlobalPos>> lastBedSpawnPositions) {
        this.lastBedSpawnPositions = lastBedSpawnPositions;
    }

    public CompoundTag save(CompoundTag compoundTag) {
        var bedFallbacks = new CompoundTag();
        for (var entry : lastBedSpawnPositions.entrySet()) {
            var playerUUID = entry.getKey();
            var positions = entry.getValue();

            var positionsList = new CompoundTag();
            int index = 0;
            for (var pos : positions) {
                var posTag = new CompoundTag();
                posTag.putInt("x", pos.pos().getX());
                posTag.putInt("y", pos.pos().getY());
                posTag.putInt("z", pos.pos().getZ());
                posTag.putString("dimension", pos.dimension().identifier().toString());
                positionsList.put("" + index, posTag); // Use index as key to preserve order
                index++;
            }
            positionsList.putInt("size", index); // Store size for deserialization
            bedFallbacks.put(playerUUID.toString(), positionsList);
        }
        compoundTag.put("bed_fallbacks", bedFallbacks);
        return compoundTag;
    }

    @Nullable
    private static ResourceKey<Level> parseDimension(String identifier) {
        if (identifier.isEmpty()) {
            return Level.OVERWORLD;
        }

        var parsed = Identifier.tryParse(identifier);
        if (parsed == null) {
            Constants.LOG.warn("Dropping bed spawn position in unreadable dimension '{}'", identifier);
            return null;
        }

        return ResourceKey.create(Registries.DIMENSION, parsed);
    }

    // One list per player for the whole server, not one per dimension, so a bed in any dimension can be the fallback
    // for a death in any other. The file does not move by switching storages: the overworld's per-dimension data
    // folder and the server's are both <world>/data.
    public static BedFallbackSavedData getData(MinecraftServer server) {
        var data = server.getDataStorage().computeIfAbsent(BedFallbackSavedData.ID);
        if (data.freshlyCreated) {
            data.freshlyCreated = false;
            data.loadFromDisk(server);
        }
        return data;
    }

    // The storage hands back a brand new instance both for a world that has never seen this mod and for one whose
    // file it could not read, so reading the file is left to us in either case. It cannot read ours: a mod has no
    // data fixers to name, and vanilla dereferences the DataFixTypes of a SavedDataType without a null check.
    // NeoForge patches that check in, Fabric runs vanilla as it is.
    //
    // Reading it here also carries pre-26.2 data forward. Before 26.2 the saved data id was a bare name, which put
    // the file straight in the level's data folder instead of under the mod id.
    private void loadFromDisk(MinecraftServer server) {
        Path dataFolder = server.getWorldPath(LevelResource.DATA);
        Path currentFile = dataFolder.resolve(Constants.MOD_ID).resolve(DATA_FILE_NAME);
        Path legacyFile = dataFolder.resolve(DATA_FILE_NAME);

        Path file = Files.exists(currentFile) ? currentFile : legacyFile;
        if (!Files.exists(file)) {
            Constants.LOG.info("No stored bed fallbacks at {} or {}, starting with none", currentFile, legacyFile);
            return;
        }

        Map<UUID, LinkedHashSet<GlobalPos>> storedPositions;
        try {
            // Saved data files wrap the payload the codec sees in a "data" compound.
            var tag = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
            storedPositions = parsePositions(tag.getCompoundOrEmpty("data"));
        } catch (Exception e) {
            // A broken file costs the players their fallbacks, but must not stop the world from loading.
            Constants.LOG.error("Could not read bed fallbacks from {}, starting with none", file, e);
            return;
        }

        // The file may have been written under a higher maximum than the one configured now.
        storedPositions.forEach(BedFallbackSavedData::trimToMaximum);
        this.lastBedSpawnPositions = storedPositions;
        setDirty();
        Constants.LOG.info("Loaded bed fallbacks for {} players from {}", storedPositions.size(), file);
    }
}
