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

package dk.magnusjensen.bedfallback.mixin;

import dk.magnusjensen.bedfallback.CommonClass;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BedBlock.class)
public class BedBlockMixin {

    // We have to mixin here, as the fabric PlayerBreakBlockEvents.AFTER does not fire if the foot part is destroyed.
    @Inject(method = "playerWillDestroy", at = @At("HEAD"))
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player, CallbackInfo ci) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        CommonClass.handleBlockBroken(serverPlayer, pos, level.getBlockEntity(pos));
    }

}
