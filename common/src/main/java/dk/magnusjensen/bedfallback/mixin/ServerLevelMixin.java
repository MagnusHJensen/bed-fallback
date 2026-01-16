/*
 *     Custom Chest Menus, a Minecraft mod that allows servers to create custom chest menus.
 *     Copyright (c) 2026  legenden (MagnusHJensen)
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dk.magnusjensen.bedfallback.mixin;

import dk.magnusjensen.bedfallback.CommonClass;
import dk.magnusjensen.bedfallback.config.ServerConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.Consumer;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

    @ModifyArg(method = "wakeUpAllPlayers", at = @At(value = "INVOKE", target = "Ljava/util/List;forEach(Ljava/util/function/Consumer;)V"))
    private Consumer modifyWakeUpAllPlayersArg(Consumer<ServerPlayer> original) {
        if (!ServerConfig.NEEDS_SLEEPING_TO_SET_SPAWN_POINT) {
            return original; // Skip this as it will be handled in the injected method
        }

        // We need to call original.accept before each return to ensure other logic is preserved
        // But also call it after our method is run since we rely on the sleepingPos to be set which is cleared in the original method
        return (Consumer<ServerPlayer>) serverPlayer -> {

            // Get current pos and blockstate
            BlockPos bedPos = serverPlayer.getSleepingPos().orElse(null);
            if (bedPos == null) {
                original.accept(serverPlayer);
                return;
            }

            BlockState state = serverPlayer.serverLevel().getBlockState(bedPos);
            if (!state.is(BlockTags.BEDS)) {
                original.accept(serverPlayer);
                return;
            }

            var part = state.getValue(BedBlock.PART);
            if (part == BedPart.FOOT) {
                bedPos = bedPos.relative(BedBlock.getConnectedDirection(state));
            }

            CommonClass.handlePlayerSetSpawn(serverPlayer, bedPos);
            original.accept(serverPlayer);
        };
    }
}
