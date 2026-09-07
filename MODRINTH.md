# TotemCounter Auto Reset

**Unofficial companion mod for [TotemCounter by uku](https://modrinth.com/mod/totemcounter).**
It does one thing: when **you die** or **the player you are fighting dies**, it triggers
TotemCounter's own reset, so every fight starts with the pop counter back at 0.

- No counter or HUD of its own. TotemCounter keeps doing all the counting and drawing.
- Nothing in TotemCounter is modified. The mod calls the same public reset its reset key uses.
- Client-side only, works on any server. Tiny (a handful of classes, two observe-only mixins).

## Who counts as "the player you are fighting"

The last player who damaged you, the last player you damaged, or the last player you hit.
Damage is read from the server's damage-event packet, which names the player responsible, so
**crystal and respawn-anchor explosions count**, not just melee. Only one opponent is tracked at a
time, so random deaths elsewhere on the server never reset anything. The opponent is forgotten
after their death, your death, a respawn or server/world change, or 30 seconds without any damage
exchanged (adjustable).

## How a death is confirmed

- The server's DEATH entity event for you or your opponent. Vanilla sends it for every player
  death, and practice servers that "fake" a death at round end send the very same packet.
- The combat-kill packet for you (the one that opens the death screen).
- Synced health reaching 0, as a fallback.

A death reported more than one way still resets exactly once.

## Settings

With [Mod Menu](https://modrinth.com/mod/modmenu) installed, the mod gets a gear button with four
options: reset when I die, reset when my opponent dies, combat timeout (5 to 120 s), and whether to
show TotemCounter's "Successfully reset pop counter" toast. Without Mod Menu the same values live
in `config/totemautoreset.json`.

## Requirements

- Minecraft 1.21.11 with Fabric Loader
- [Fabric API](https://modrinth.com/mod/fabric-api)
- [TotemCounter](https://modrinth.com/mod/totemcounter) 1.11 or newer, and its library
  [ukulib](https://modrinth.com/mod/ukulib)
- [Mod Menu](https://modrinth.com/mod/modmenu), optional, for the settings screen

The mod refuses to load without TotemCounter installed.

## Credits

TotemCounter and ukulib are made by uku; this addon is not affiliated with or endorsed by them.
The icon uses Minecraft's Totem of Undying texture. Source code is on
[GitHub](https://github.com/lawspandayt-jpg/TotemAutoReset) under the MIT license.
