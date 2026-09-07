# TotemCounter Auto Reset

An unofficial, tiny Fabric companion mod for **[TotemCounter by uku](https://modrinth.com/mod/totemcounter)** on
Minecraft **1.21.11**. It adds one thing: when **you die** or **the player you are fighting dies**,
it triggers TotemCounter's own reset, so every fight starts with the counter back at 0.

It does **not** count totems, draw anything, or change how TotemCounter detects pops. There is no
second counter and no GUI. If TotemCounter is not installed the mod refuses to load (hard dependency).

## Install

Put these in your `mods` folder (all three are required):

| Mod | Where |
| --- | --- |
| Fabric API | Modrinth |
| ukulib + TotemCounter (1.11.x for 1.21.11) | Modrinth (TotemCounter pulls in ukulib) |
| `totemautoreset-1.0.0+1.21.11.jar` | `build/libs/` after building (see below) |

## Build the jar

Requires a JDK 21 or newer on `PATH` (the project was built with JDK 25). Everything else,
including TotemCounter and ukulib for compiling, is downloaded by Gradle.

```bat
gradlew.bat build
```

The jar is written to `build\libs\totemautoreset-1.0.0+1.21.11.jar`
(the `-sources.jar` next to it is not needed in-game).

To test in a dev client with TotemCounter already on the classpath:

```bat
gradlew.bat runClient
```

### In-game self-test

`SmokeTest.java` is a dev-only end-to-end check (excluded from the release jar) that runs with
`-Dtotemautoreset.smoke=true`. Put any 1.21.11 singleplayer world in `run/saves/` (the folder is
git-ignored), then:

```bat
gradlew.bat runClient -Psmoke -PquickPlay="New World"
```

It joins the world, kills the player through the integrated server (the real death path: damage
event, DEATH entity event, combat-kill packet, health 0), then adds a client-side fake opponent,
delivers a DEATH entity event to it through the mixin, and checks that TotemCounter's map was
cleared both times. It then opens the settings screen and Mod Menu's list filtered to this mod,
saves a screenshot of each to `run/screenshots/`, and checks Mod Menu hands back the settings
screen. Look for `[smoke] RESULT: PASS` in `run/logs/latest.log`.
Last run: PASS on 2026-09-07 against TotemCounter 1.11.1 / ukulib 1.10.1 / Mod Menu 17.0.0.

## How it works

TotemCounter keeps its pops in a static map and exposes `TotemCounter.resetPopCounter()`, the exact
method its reset keybind and `/totemcounter reset` call. This mod only ever calls that (or, with the
toast turned off, clears the same map through `TotemCounter.getPops()`). Nothing in TotemCounter's
jar is modified.

**Who counts as the opponent** (the *most recently attacked or attacking* player):

* a player whose damage lands on you, or
* a player your damage lands on, or
* a player you left-click.

Damage is read from the server's damage-event packet, whose source names the responsible player,
so crystal and respawn-anchor explosions count, not just melee hits. Only one opponent is tracked at
a time; any other player dying on the server is ignored. The opponent is forgotten after their
death, your death, a respawn or server/world change, disconnecting, or `combatTimeoutSeconds`
with no damage exchanged.

**How a death is confirmed** (nothing else ever triggers a reset):

* the server's `DEATH` entity event for you or the opponent. Vanilla sends it for every player death
  to the dying player and everyone watching; practice servers that use Bukkit's `EntityEffect.DEATH`
  for "fake" round-ending deaths send the very same packet;
* the player-combat-kill packet for you (the one that opens the death screen);
* synced health reaching 0, as a fallback.

A death that arrives through more than one of these resets once (duplicates within 1.5 s for the
same player are dropped).

## Settings

With [Mod Menu](https://modrinth.com/mod/modmenu) installed, the mod shows up with its icon and a
gear button that opens a small vanilla-style settings screen:

* **Reset when I die** (ON)
* **Reset when my opponent dies** (ON)
* **Combat timeout** slider, 5 to 120 s (30 s)
* **Show reset toast** (ON)

Changes apply immediately and are saved when you press Done. Mod Menu is optional; without it the
same values live in `config/totemautoreset.json`, created on first launch (edit and restart).

```json
{
  "resetOnOwnDeath": true,
  "resetOnOpponentDeath": true,
  "combatTimeoutSeconds": 30,
  "showToast": true
}
```

`showToast: true` uses TotemCounter's own reset, including its "Successfully reset pop counter"
toast, exactly like pressing its reset key. `false` clears the counter silently.

Every reset is logged to `latest.log` as `[TotemAutoReset] Reset TotemCounter: <who died>`, which is
the quickest way to check it fired.

## Publishing

`docs/MODRINTH_UPLOAD.md` has the exact form values for the Modrinth project and version,
`MODRINTH.md` is the project description, `CHANGELOG.md` feeds the version changelog and
`modrinth/` holds the icon, banner and gallery images. Once the project exists on Modrinth,
`gradlew.bat modrinth` uploads a new version (token from the `MODRINTH_TOKEN` environment variable).

## Project layout

```
src/main/java/com/ohmarker/totemautoreset/
  TotemAutoReset.java             entrypoint: registers the tick + attack events
  FightTracker.java               opponent tracking and death confirmation
  CounterReset.java               the only code that touches TotemCounter (debounced)
  AutoResetConfig.java            the JSON config above
  AutoResetConfigScreen.java      the settings screen (vanilla OptionsSubScreen widgets)
  ModMenuIntegration.java         Mod Menu entrypoint that provides the gear button
  SmokeTest.java                  dev-only in-game self-test; left out of the release jar
  mixin/LivingEntityMixin.java    observes DEATH entity events and damage events
  mixin/ClientPacketListenerMixin.java  observes the combat-kill packet
src/main/resources/fabric.mod.json      depends on totemcounter >= 1.11.0, suggests modmenu
src/main/resources/assets/totemautoreset/icon.png    vanilla Totem of Undying inside a green reset arrow, 512 px (Mod Menu, Modrinth)
src/main/resources/assets/totemautoreset/lang/en_us.json
```

Built against TotemCounter `1.11.1+mc1.21.11` / ukulib `1.10.1+1.21.11` and Mod Menu `17.0.0` from
the Modrinth Maven (`gradle.properties`); TotemCounter's reset API is unchanged in 1.11.2.
