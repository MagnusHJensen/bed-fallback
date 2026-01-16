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


import net.neoforged.neoforge.common.ModConfigSpec;

public class ServerConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.IntValue MAXIMUM_BED_FALLBACKS_CONFIG = BUILDER
        .comment("How many bed spawn-points is tracked per player.")
        .defineInRange("maximumBedFallbacks", 3, 2, Integer.MAX_VALUE);
    private static final ModConfigSpec.BooleanValue NEEDS_SLEEPING_TO_SET_SPAWN_POINT_CONFIG = BUILDER
        .comment("If true, players need to sleep in the bed to set their spawn point. If false, just interacting with the bed sets the spawn point.")
        .define("needsSleepingToSetSpawnPoint", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    public static int MAXIMUM_BED_FALLBACKS;
    public static boolean NEEDS_SLEEPING_TO_SET_SPAWN_POINT;

    public static void onModConfigEvent() {
        MAXIMUM_BED_FALLBACKS = MAXIMUM_BED_FALLBACKS_CONFIG.get();
        NEEDS_SLEEPING_TO_SET_SPAWN_POINT = NEEDS_SLEEPING_TO_SET_SPAWN_POINT_CONFIG.get();
    }
}
