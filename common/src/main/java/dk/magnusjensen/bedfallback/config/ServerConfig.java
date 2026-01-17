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

package dk.magnusjensen.bedfallback.config;


import com.electronwill.nightconfig.core.CommentedConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class ServerConfig {
    public static final ServerConfig CONFIG;
    public static final ModConfigSpec CONFIG_SPEC;

    public int maximumBedFallbacks;
    public boolean needsSleepingToSetSpawnPoint;

    private ServerConfig(ModConfigSpec.Builder builder) {
        builder.comment("How many bed spawn-points is tracked per player.")
            .defineInRange("maximumBedFallbacks", 3, 2, Integer.MAX_VALUE);

        builder.comment(
            "If true, players need to sleep in the bed to set their spawn point. If false, just interacting with the bed sets the spawn point."
        ).define("needsSleepingToSetSpawnPoint", true);
    }
    static {
        Pair<ServerConfig, ModConfigSpec> pair =
            new ModConfigSpec.Builder().configure(ServerConfig::new);

        CONFIG = pair.getLeft();
        CONFIG_SPEC = pair.getRight();
    }

    public static void onModConfigEvent(CommentedConfig config) {
        CONFIG.maximumBedFallbacks = config.getInt("maximumBedFallbacks");
        CONFIG.needsSleepingToSetSpawnPoint = config.get("needsSleepingToSetSpawnPoint");
    }
}
