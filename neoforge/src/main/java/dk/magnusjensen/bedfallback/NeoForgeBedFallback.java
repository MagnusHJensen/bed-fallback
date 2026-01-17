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

import dk.magnusjensen.bedfallback.config.ServerConfig;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.event.entity.player.PlayerSetSpawnEvent;
import net.neoforged.neoforge.event.level.BlockEvent;


@Mod(Constants.MOD_ID)
@EventBusSubscriber(modid = Constants.MOD_ID)
public class NeoForgeBedFallback {
    
    public NeoForgeBedFallback(IEventBus modEventBus, ModContainer modContainer) {
    

        CommonClass.init();


        modContainer.registerConfig(ModConfig.Type.SERVER, ServerConfig.CONFIG_SPEC);
    }

    @SubscribeEvent
    public static void onConfigUpdates(final ModConfigEvent.Reloading event) {
        ServerConfig.onModConfigEvent(event.getConfig().getLoadedConfig().config());
    }

    @SubscribeEvent
    public static void onConfigUpdatesLoading(final ModConfigEvent.Loading event) {
        ServerConfig.onModConfigEvent(event.getConfig().getLoadedConfig().config());
    }

    @SubscribeEvent
    public static void onPlayerSetSpawn(PlayerSetSpawnEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        if (ServerConfig.CONFIG.needsSleepingToSetSpawnPoint) {
            return; // Skip this as it will be handled after waking up the player
        }

        CommonClass.handlePlayerSetSpawn(serverPlayer, event.getNewSpawn());
    }

    @SubscribeEvent
    public static void onPlayerBreakBlock(BlockEvent.BreakEvent event) {
        if (event.getPlayer().level().isClientSide()) {
            return;
        }
        if (!(event.getPlayer() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        var blockEntity = event.getLevel().getBlockEntity(event.getPos());
        CommonClass.handleBlockBroken(serverPlayer, event.getPos(), blockEntity);
    }
}