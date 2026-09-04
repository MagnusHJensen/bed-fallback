# 1.3.0 - 26.2
Ported to Minecraft 26.2 on both Fabric and NeoForge.
Bed fallbacks recorded on 1.21.11 are carried over the first time a world is opened on 26.2.
Fixed bed fallbacks being forgotten on every server restart on Fabric.
Fixed a crash when a player with a recorded bed fallback had no spawn point set at all.
A bed destroyed by anything other than a player, such as a creeper, no longer strands you at world spawn: the fallback now skips to the newest bed that is still standing.
