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
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.properties.BedPart;

public class CommonClass {


    public static void init() {

    }

    public static void handlePlayerSetSpawn(ServerPlayer player, BlockPos bedPos) {
        var blockState = player.level().getBlockState(bedPos);
        if (!blockState.is(BlockTags.BEDS)) {
            return;
        }

        // Whether a bed sets spawn is an environment attribute, so it answers for modded dimensions as well, and it
        // varies by position rather than by dimension. The Nether and the End say no through the rule that blows beds up.
        BedRule bedRule = player.level().environmentAttributes().getValue(EnvironmentAttributes.BED_RULE, bedPos);
        if (!bedRule.canSetSpawn(player.level())) {
            Constants.LOG.info("Skipped setting bed spawn for player {} in dimension {}, beds do not set spawn there", player.getName().getString(), player.level().dimension().identifier());
            return;
        }

        var data = BedFallbackSavedData.getData(player.level().getServer());

        // We don't need to check block positions, as the BedBlock class handles always calling code with it's HEAD part.
        data.addBedSpawnPosition(player.getUUID(), GlobalPos.of(player.level().dimension(), bedPos));
    }

    public static void handleBlockBroken(ServerPlayer player, BlockPos brokenPos) {
        // Beds lost their block entity in 26.2, so the block state is the only thing left to recognise one by.
        var state = player.level().getBlockState(brokenPos);
        if (!state.is(BlockTags.BEDS)) {
            return;
        }

        // Always get the head part to ensure consistency with other code pieces
        BedPart bedPart = state.getValue(BedBlock.PART);
        if (bedPart == BedPart.FOOT) {
            brokenPos = brokenPos.relative(BedBlock.getConnectedDirection(state));
        }

        var data = BedFallbackSavedData.getData(player.level().getServer());

        data.removeBedSpawnPosition(GlobalPos.of(player.level().dimension(), brokenPos));
    }
}