# Publishing on Modrinth

Everything below is prepared in this repo. Steps marked **(you)** need your Modrinth or GitHub
account and cannot be done from here.

## 1. Source on GitHub (you, 2 minutes)

Create an **empty** repository named `TotemAutoReset` under `lawspandayt-jpg` (no README, no
license, the repo already has both), then from this folder:

```bat
git remote add origin https://github.com/lawspandayt-jpg/TotemAutoReset.git
git push -u origin main
```

The jar's `contact` links and MODRINTH.md already point at that URL.

## 2. Create the project (you, on modrinth.com)

**Create a project** → type **Mod**:

| Field | Value |
| --- | --- |
| Name | `TotemCounter Auto Reset` |
| URL slug | `totemcounter-auto-reset` (was free on 2026-09-07; if taken, use `totemautoreset` and change `modrinth_slug` in `gradle.properties` and the homepage link in `fabric.mod.json`) |
| Summary | `Unofficial addon for uku's TotemCounter: automatically resets its pop counter when you die or the player you are fighting dies. No counter or HUD of its own.` |
| Visibility | Private until everything below is filled in |

Then in the project's **Settings**:

| Section | Value |
| --- | --- |
| Description | paste `MODRINTH.md` (or run `gradlew.bat modrinthSyncBody` after step 4) |
| Icon | `modrinth/icon.png` |
| Categories | **Utility** |
| Client-side / Server-side | **Required** / **Unsupported** |
| License | **MIT**, URL `https://github.com/lawspandayt-jpg/TotemAutoReset/blob/main/LICENSE` |
| Links | Source `https://github.com/lawspandayt-jpg/TotemAutoReset`, Issues `https://github.com/lawspandayt-jpg/TotemAutoReset/issues` |
| Gallery | `modrinth/banner.png` (featured), `modrinth/gallery-settings.png` ("Settings screen via Mod Menu"), `modrinth/gallery-modmenu.png` ("In Mod Menu") |

## 3. Upload the version

**Website:** Versions → **Create a version**

| Field | Value |
| --- | --- |
| File | `build\libs\totemautoreset-1.0.0+1.21.11.jar` |
| Version number | `1.0.0+1.21.11` |
| Version title | `TotemCounter Auto Reset 1.0.0` |
| Channel | Release |
| Loaders | Fabric |
| Game versions | 1.21.11 |
| Dependencies | TotemCounter **required**, ukulib **required**, Fabric API **required**, Mod Menu **optional** |
| Changelog | the `1.0.0+1.21.11` section of `CHANGELOG.md` |

**Or from Gradle** (once the project exists): create a personal access token at
modrinth.com/settings/pats with the *Create versions* and *Write projects* scopes, put it in the
`MODRINTH_TOKEN` environment variable (never in the repo), then

```bat
gradlew.bat modrinth
```

uploads the jar with the dependencies and changelog above, and

```bat
gradlew.bat modrinthSyncBody
```

pushes `MODRINTH.md` as the description.

## 4. Submit for review (you)

Set visibility to **Public**/submit. Review normally takes one to two days; reviewers check that
the jar loads and the description says what the mod does. Both are covered.

## Future releases

1. Bump `mod_version` in `gradle.properties`, add a section at the top of `CHANGELOG.md`.
2. `gradlew.bat build`, then `gradlew.bat runClient -Psmoke -PquickPlay="New World"` and check
   `[smoke] RESULT: PASS` in `run/logs/latest.log`.
3. `gradlew.bat modrinth` (or upload the jar on the website).
