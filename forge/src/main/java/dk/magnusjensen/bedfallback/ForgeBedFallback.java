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
import net.minecraftforge.event.entity.player.PlayerSetSpawnEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Constants.MOD_ID)
@Mod.EventBusSubscriber(modid = Constants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeBedFallback {
    
    public ForgeBedFallback() {
    

        CommonClass.init();
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::onConfigUpdates);

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
    }

    private void onConfigUpdates(final ModConfigEvent event) {
        ServerConfig.onModConfigEvent(event.getConfig());
    }

    @SubscribeEvent
    public static void onPlayerSetSpawn(PlayerSetSpawnEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
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