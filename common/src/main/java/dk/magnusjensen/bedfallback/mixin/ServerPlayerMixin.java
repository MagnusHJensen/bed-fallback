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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelData;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends Player {


    public ServerPlayerMixin(Level level, GameProfile gameProfile) {
        super(level, gameProfile);
    }

    @Shadow
    @Final
    private MinecraftServer server;


    @Shadow
    public abstract ServerLevel level();

    @Shadow
    private ServerPlayer.@Nullable RespawnConfig respawnConfig;

    @Inject(method = "getRespawnConfig", at = @At("HEAD"))
    public void getRespawnPosition(CallbackInfoReturnable<ServerPlayer.RespawnConfig> ci) {
        ServerPlayer.RespawnConfig currentConfig = this.respawnConfig;
        if (currentConfig != null) {
            var state = this.level().getBlockState(currentConfig.respawnData().globalPos().pos());
            if (state.is(Blocks.RESPAWN_ANCHOR)) {
                return; // Respawn anchor is valid, do nothing
            }
        }

        var data = BedFallbackSavedData.getData(this.level());
        // Recorded positions are overworld ones, so they have to be checked against the overworld. Checking them
        // against whatever level the player died in would find no bed and throw away every position they have.
        var lastBedPos = data.findLastStandingBedSpawnPosition(this.server.overworld(), this.getUUID());
        if (lastBedPos == null) {
            return;
        }

        // A player whose respawn was cleared after a failed respawn has no config left to take the angles from.
        LevelData.RespawnData currentRespawn = currentConfig != null ? currentConfig.respawnData() : LevelData.RespawnData.DEFAULT;
        this.respawnConfig = new ServerPlayer.RespawnConfig(
            LevelData.RespawnData.of(this.server.overworld().dimension(), lastBedPos, currentRespawn.yaw(), currentRespawn.pitch()),
            currentConfig != null && currentConfig.forced()
        );
    }
}
