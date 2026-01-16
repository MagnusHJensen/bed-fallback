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
import fuzs.forgeconfigapiport.fabric.api.neoforge.v4.NeoForgeConfigRegistry;
import fuzs.forgeconfigapiport.fabric.api.neoforge.v4.NeoForgeModConfigEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.neoforged.fml.config.ModConfig;

public class FabricBedFallback implements ModInitializer {
    
    @Override
    public void onInitialize() {

        CommonClass.init();

        EntitySleepEvents.ALLOW_SETTING_SPAWN.register((player, sleepingPos) -> {
            if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
                return true;
            }

            if (ServerConfig.NEEDS_SLEEPING_TO_SET_SPAWN_POINT) {
                return true; // Skip this as it will be handled after waking up the player
            }

            CommonClass.handlePlayerSetSpawn(serverPlayer, sleepingPos);
            return true;
        });

        NeoForgeConfigRegistry.INSTANCE.register(Constants.MOD_ID, ModConfig.Type.SERVER, ServerConfig.SPEC);
        NeoForgeModConfigEvents.reloading(Constants.MOD_ID).register((config -> ServerConfig.onModConfigEvent()));
        NeoForgeModConfigEvents.loading(Constants.MOD_ID).register((config -> ServerConfig.onModConfigEvent()));
        NeoForgeModConfigEvents.unloading(Constants.MOD_ID).register((config) -> ServerConfig.onModConfigEvent());
    }
}
