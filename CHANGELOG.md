# Changelog

## 1.0.0+1.21.11 (2026-09-07)

First release.

- Triggers TotemCounter's own reset when you die or the player you are fighting dies.
- Opponent tracking from the server's damage events (crystal and anchor explosions included) and
  melee hits, with a 30 s combat timeout; cleared on death, respawn, world change or disconnect.
- Deaths confirmed by the DEATH entity event, the combat-kill packet or health reaching 0;
  duplicate signals for the same death are ignored.
- Mod Menu settings screen: reset on own death, reset on opponent death, combat timeout, toast on/off.
- Requires TotemCounter 1.11+ (with ukulib) and Fabric API. Mod Menu is optional.
