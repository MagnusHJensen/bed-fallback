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
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class BedFallbackSavedData extends SavedData {
    public static final String DATA_NAME = "bed_fallbacks_data";

    // Map of player UUID to linked hash set of block positions of bed spawns
    // We use a hash set to get O(1) lookups and linked to preserve insertion order
    private Map<UUID, LinkedHashSet<BlockPos>> lastBedSpawnPositions = new HashMap<>();

    public void addBedSpawnPosition(UUID playerUUID, BlockPos bedPos) {
        Constants.LOG.debug("Adding bed spawn position {} for player {}", bedPos, playerUUID);
        var positions = lastBedSpawnPositions.computeIfAbsent(playerUUID, k -> new LinkedHashSet<>());
        var didAdd = positions.add(bedPos);
        if (!didAdd) {
            // We remove the entry and then re-add it as the old spawn was overridden at a newer time
            positions.remove(bedPos);
            positions.add(bedPos);
        }

        // Enforce maximum number of bed fallbacks
        while (positions.size() > ServerConfig.MAXIMUM_BED_FALLBACKS) {
           if (positions.iterator().hasNext()) {
               var firstPos = positions.iterator().next();
               Constants.LOG.debug("Removing oldest bed spawn position {} for player {} to enforce maximum of {}", firstPos, playerUUID, ServerConfig.MAXIMUM_BED_FALLBACKS);
               positions.remove(firstPos);
           }
        }

        setDirty();
    }

    @Nullable
    public BlockPos getLastBedSpawnPosition(UUID playerUUID) {
        LinkedHashSet<BlockPos> positions = lastBedSpawnPositions.get(playerUUID);
        if (positions != null && !positions.isEmpty()) {
            // Return the last added position
            Iterator<BlockPos> iterator = positions.iterator();
            BlockPos lastPos = null;
            while (iterator.hasNext()) {
                lastPos = iterator.next();
            }
            return lastPos;
        }
        return null;
    }

    public boolean hasBedSpawnPosition(UUID playerUUID, BlockPos pos) {
        LinkedHashSet<BlockPos> positions = lastBedSpawnPositions.get(playerUUID);
        return positions != null && positions.contains(pos);
    }

    public void removeBedSpawnPosition( BlockPos bedPos) {
        Constants.LOG.debug("Removing bed spawn position {}", bedPos);
        // Loop over all players and remove the bed position if it exists
        for (var entry : lastBedSpawnPositions.entrySet()) {
            var playerUUID = entry.getKey();
            var positions = entry.getValue();
            if (positions.remove(bedPos)) {
                if (positions.isEmpty()) {
                    lastBedSpawnPositions.remove(playerUUID);
                }
                setDirty();
            }
        }
    }

    public static BedFallbackSavedData create() {
        return new BedFallbackSavedData();
    }

    public static BedFallbackSavedData load(CompoundTag compoundTag, HolderLookup.Provider provider) {
        var lastBedSpawnPositions = new HashMap<UUID, LinkedHashSet<BlockPos>>();
        var bedFallbackNBT = compoundTag.getCompound("bed_fallbacks");
        for (String key : bedFallbackNBT.getAllKeys()) {
            var playerUUID = UUID.fromString(key);
            var positionsList = bedFallbackNBT.getCompound(key);
            var size = positionsList.getInt("size");
            var positionsSet = new LinkedHashSet<BlockPos>();
            for (int i = 0; i < size; i++) {
                var posTag = positionsList.getCompound("" + i);
                var pos = new BlockPos(posTag.getInt("x"), posTag.getInt("y"), posTag.getInt("z"));
                positionsSet.add(pos);
            }
            lastBedSpawnPositions.put(playerUUID, positionsSet);
        }
        return new BedFallbackSavedData(lastBedSpawnPositions);
    }

    public BedFallbackSavedData() {
    }

    public BedFallbackSavedData(Map<UUID, LinkedHashSet<BlockPos>> lastBedSpawnPositions) {
        this.lastBedSpawnPositions = lastBedSpawnPositions;
    }

    @Override
    public CompoundTag save(CompoundTag compoundTag, HolderLookup.Provider provider) {
        var bedFallbacks = new CompoundTag();
        for (var entry : lastBedSpawnPositions.entrySet()) {
            var playerUUID = entry.getKey();
            var positions = entry.getValue();

            var positionsList = new CompoundTag();
            int index = 0;
            for (var pos : positions) {
                var posTag = new CompoundTag();
                posTag.putInt("x", pos.getX());
                posTag.putInt("y", pos.getY());
                posTag.putInt("z", pos.getZ());
                positionsList.put("" + index, posTag); // Use index as key to preserve order
                index++;
            }
            positionsList.putInt("size", index); // Store size for deserialization
            bedFallbacks.put(playerUUID.toString(), positionsList);
        }
        compoundTag.put("bed_fallbacks", bedFallbacks);
        return compoundTag;
    }

    public static BedFallbackSavedData getData(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(BedFallbackSavedData::create, BedFallbackSavedData::load, null), BedFallbackSavedData.DATA_NAME);
    }
}
