# ByeDPI – Material 3 Expressive (Jetpack Compose) port

Only the UI changed. Services, JNI/native code, `core/`, `data/` and the SharedPreferences
keys/file are untouched, so existing settings keep working and the VPN/proxy behave as before.

## IMPORTANT: native sources
The upstream repo uses git submodules (`app/src/main/cpp/byedpi`,
`app/src/main/jni/hev-socks5-tunnel`). They are **empty in the GitHub "master.zip"**.
Clone the real repo with submodules and copy this project's files over it:

    git clone --recurse-submodules https://github.com/dovecoteescapee/ByeDPIAndroid
    # then overwrite with the contents of this folder (build files, app/src/main/java, res, manifest)

## Toolchain (what this project is configured for)
| Piece | Version | Why |
|---|---|---|
| Android Studio | newest stable/canary that supports AGP 9.4 | AGP 9.4 requirement |
| AGP | 9.4.0 | needed for compileSdk 37.x |
| Gradle | 9.6.0 (wrapper updated) | AGP 9.4 minimum |
| JDK | 17+ | AGP requirement |
| Kotlin | 2.3.0 (AGP 9 built-in Kotlin + Compose compiler plugin) | |
| compileSdk | 37.1 (`byedpi.compileSdkMinor=1`) | Compose 1.13 alphas pulled by Material3 1.5.0-alpha29 |
| minSdk / targetSdk | 28 (Android 9) / 34 | targetSdk kept so service behavior does not change |
| Compose BOM | `compose-bom-alpha:2026.09.00` | coherent alpha set |
| Material3 | `1.5.0-alpha29` (pinned) | Material 3 Expressive |

Install "Android SDK Platform 37.1" (SDK Manager → show package details) and NDK/CMake 3.22.1 as before.

## Where Expressive lives
`ui/Expressive.kt` is the ONLY file using alpha/expressive APIs:
`MaterialExpressiveTheme` + `MotionScheme.expressive()`, `Button(shapes = ButtonDefaults.shapes())`,
`LoadingIndicator`, `LargeFlexibleTopAppBar`. Each wrapper has its stable fallback in its comment.
Everything else (settings rows, dialogs, switches, navigation) uses long-stable Material 3 APIs.

## If the build fails – fallback ladder
1. *"requires compileSdk 37.1 / platform not found"*: install the 37.1 platform; or set
   `byedpi.compileSdkMinor=0` in gradle.properties (only works if the resolved Compose version allows 37.0).
2. *AGP/Gradle/Studio mismatch*: use the Studio version matching AGP 9.4 or lower `agp` in
   `gradle/libs.versions.toml` to 9.1.1 (+ Gradle 9.3.1).
3. *Unresolved reference / signature error in `ui/Expressive.kt`*: an alpha renamed something.
   Fix inside that file only (or switch the wrapper to its stable fallback).
4. *Want zero alpha*: in `libs.versions.toml` set `material3 = "1.4.0"` and `composeBom` to the stable BOM
   (`compose-bom`, not `-alpha`), set `byedpi.compileSdkMinor=0`, and switch the wrappers in
   `Expressive.kt` to their fallbacks. Look: standard Material 3 instead of Expressive.

## What changed
- Removed: AppCompat, Fragments, Preference/takisoft, ViewBinding, XML layouts/menus/preference XML, SettingsActivity.
- Added: single-activity Compose UI (`ui/`), expressive theme, segmented-card settings, dialogs for text/choice prefs.
- `MainActivity` keeps its class name (QuickTile + manifest rely on it).
- Settings read/write the same keys with the same defaults and validation ranges as before.
- Small fixes: log-save toast now runs on the main thread; empty OOB char is rejected (it used to crash the proxy args builder).
- Version name is now `1.2.0-m3e` (versionCode 11).
- Release minify is still off, as upstream. Compose makes the APK larger; enabling R8 needs keep rules for the JNI classes.

## Building with GitHub Actions (`.github/workflows/build.yml`)
Runs on every push to `main`/`master`, on pull requests, on tags `v*`, and manually (Actions → Build → Run workflow).

- **Debug APK** is always built and uploaded as the `ByeDPI-debug-apk` artifact (installable as is).
- **Signed release APK** is built only if these repository secrets exist
  (Settings → Secrets and variables → Actions): `KEYSTORE_BASE64` (`base64 -w0 my.keystore`),
  `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
- Pushing a tag like `v1.2.0-m3e` also attaches the APKs to a GitHub Release.
- Native sources: if your repo has the real submodules (fork upstream and push over it) they are used with the
  exact upstream pins. If the folders are empty, the workflow clones byedpi (`v0.13`) and hev-socks5-tunnel
  (default branch) itself; change `BYEDPI_REF` / `HEV_REF` at the top of the workflow to pin other versions.
- Installs SDK platform 37.1, NDK 28.2.13676358, CMake 3.22.1 on the runner. If the 37.1 package id is not
  available yet the log shows the platforms the runner can see (step "Install SDK packages").

## UI structure (v2)
- Bottom navigation: **Connection** (hero + status), **Configuration** (ByeDPI group: command-line switch, UI editor,
  command-line editor), **Settings** (General, About, Reset).
- While the service runs, Configuration/Settings stay visible but locked (banner); Theme and Colors stay editable.
- Settings > General > **Colors**: "System colors (Material You)" (default, Android 12+) or "ByeDPI blue".
  With a gray/monochrome wallpaper the system palette is gray by design; pick "ByeDPI blue" for a colorful look.
- `ui/ConnectionHero.kt`: circular button, wavy loading ring, power-on wave, wobbling ambient ripples,
  reversed "power-off" collapse. All alpha-only calls stay in `ui/Expressive.kt`.
