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

package dk.magnusjensen.bedfallback;

import dk.magnusjensen.bedfallback.data.BedFallbackSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.entity.BedBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BedPart;

public class CommonClass {


    public static void init() {

    }

    public static void handlePlayerSetSpawn(ServerPlayer player, BlockPos bedPos) {
        // Check that we are in the overworld to not cause weirdness but rather just not support other dimensions setting bed spawns
        if (player.level().dimension() != player.level().getServer().overworld().dimension()) {
            Constants.LOG.info("Skipped setting bed spawn for player {} in dimension {}", player.getName().getString(), player.level().dimension().location());
            return;
        }

        var data = player.serverLevel().getDataStorage().computeIfAbsent(BedFallbackSavedData::load, BedFallbackSavedData::new, BedFallbackSavedData.DATA_NAME);

        var blockState = player.serverLevel().getBlockState(bedPos);
        if (!blockState.is(BlockTags.BEDS)) {
            return;
        }

        // We don't need to check block positions, as the BedBlock class handles always calling code with it's HEAD part.
        data.addBedSpawnPosition(player.getUUID(), bedPos);
    }

    public static void handleBlockBroken(ServerPlayer player, BlockPos brokenPos, BlockEntity blockEntity) {
        if (!(blockEntity instanceof BedBlockEntity)) {
            // We can rely on that beds have block entities, so if it's not a bed block entity, we can just return.
            return;
        }

        // Always get the head part to ensure consistency with other code pieces
        var state = player.level().getBlockState(brokenPos);
        BedPart bedPart = state.getValue(BedBlock.PART);
        if (bedPart == BedPart.FOOT) {
            brokenPos = brokenPos.relative(BedBlock.getConnectedDirection(state));
        }

        var data = player.serverLevel().getDataStorage().computeIfAbsent(BedFallbackSavedData::load, BedFallbackSavedData::new, BedFallbackSavedData.DATA_NAME);

        data.removeBedSpawnPosition(brokenPos);
    }
}