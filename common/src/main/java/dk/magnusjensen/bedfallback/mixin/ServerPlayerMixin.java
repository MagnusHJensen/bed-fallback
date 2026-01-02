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

import com.mojang.authlib.GameProfile;
import dk.magnusjensen.bedfallback.data.BedFallbackSavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends Player {
    @Shadow
    private BlockPos respawnPosition;

    @Shadow
    public abstract ServerLevel serverLevel();

    @Shadow
    @Final
    public MinecraftServer server;

    public ServerPlayerMixin(Level level, BlockPos pos, float yRot, GameProfile gameProfile) {
        super(level, pos, yRot, gameProfile);
    }

    @Inject(method = "getRespawnPosition", at = @At("HEAD"))
    public void getRespawnPosition(CallbackInfoReturnable<BlockPos> ci) {
        if (this.respawnPosition != null) {
            var state = this.level().getBlockState(this.respawnPosition);
            if (state.is(Blocks.RESPAWN_ANCHOR)) {
                return; // Respawn anchor is valid, do nothing
            }
        }

        var data = BedFallbackSavedData.getData(this.serverLevel());
        var lastBedPos = data.getLastBedSpawnPosition(this.getUUID());
        if (lastBedPos != null) {
            this.respawnPosition = lastBedPos;
        }
    }
}
