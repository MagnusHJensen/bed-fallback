# 1.3.0 - 26.2
Ported to Minecraft 26.2 on both Fabric and NeoForge.
Bed fallbacks now work in every dimension where a bed can set spawn, modded ones included, instead of the overworld only. One list per player covers the whole server, so a bed in one dimension can be the fallback for a death in another.
Bed fallbacks recorded on 1.21.11 are carried over the first time a world is opened on 26.2.
Fixed bed fallbacks being forgotten on every server restart on Fabric.
A bed destroyed by anything other than a player, such as a creeper, no longer strands you at world spawn: the fallback now skips to the newest bed that is still standing.
Fixed a respawn anchor being overridden by a bed fallback.
Fixed a crash when a player with a recorded bed fallback had no spawn point set at all.
