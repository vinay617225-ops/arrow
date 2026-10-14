# Arrow Escape — native Android

An original Kotlin / Jetpack Compose puzzle game. All rendering uses Compose Canvas. Minimum Android 8.0 (API 26), target/compile SDK 35. Material 3, MVVM with StateFlow and Coroutines, Room progress storage, and DataStore settings.

## Run in Android Studio

1. Open `/workspace/arrow` using a recent Android Studio that supports Android Gradle Plugin 8.7.3.
2. Install Android SDK Platform 35, Build Tools 34.0.0 (AGP 8.7 default), and Platform Tools through SDK Manager. Select a full JDK 17 or newer with `javac`, not a JRE. The verified cloud build used Temurin JDK 17.0.16.
3. Set `sdk.dir` in your untracked `local.properties`, or set `ANDROID_HOME` to your SDK directory.
4. Sync, select the `app` run configuration, and run on an Android 8.0+ device or emulator.

The checked-in standard Gradle wrapper includes its JAR, pins Gradle **8.10.2**, and verifies the official distribution SHA-256. Android Gradle Plugin **8.7.3** supports SDK 35 and requires Gradle 8.9+ / JDK 17+; this build satisfies those requirements. Kotlin **2.0.21** and the Compose compiler plugin **2.0.21** match. KSP **2.0.21-1.0.28** matches Kotlin; the Compose BOM **2024.12.01** selects compatible UI/runtime libraries. Do not set an old `kotlinCompilerExtensionVersion` alongside the Kotlin 2 Compose plugin.

Repositories are Google's Android Maven repository, Maven Central, and the Gradle Plugin Portal (for plugin markers). No insecure repositories or mirrors are configured.

## Build and test

```sh
./gradlew testDebugUnitTest assembleDebug lintDebug
./gradlew connectedDebugAndroidTest
./gradlew bundleRelease
```

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release AAB: `app/build/outputs/bundle/release/app-release.aab`
- Local test reports: `app/build/reports/tests/testDebugUnitTest/`
- Instrumentation tests require an emulator or connected device. Use a clean app data directory for the full home-to-game test.
- Configure your own release signing through Android Studio's **Generate Signed Bundle / APK**. The repository does not include signing secrets. `bundleRelease` creates an unsigned bundle until signing is configured.

## Implemented behavior

- 100 selectable deterministic levels with progressive shape introduction: straight paths, L paths at level 5, U paths at level 15, winding paths at level 30. Board size and requested density grow with difficulty. The number of arrows depends on valid packing.
- Unique arrow IDs, ordered connected half-integer grid cell centers, occupied cell sets, tail, head, pointing direction, rounded continuous centerlines, and complete escape trajectories.
- Full body hit testing, vector arrowheads, smooth arc-length path-following animation, hint glow, blocked bounce/red flash, animated hearts, dialog/screen transitions and a completion particle effect.
- Five hearts. Blocked moves cost one heart. Zero hearts prevents more moves. Restart creates a fresh attempt. Undo restores the most recently escaped arrow (it does not refund collision hearts).
- Shared engine rules for moves, solver, generation validation and hints. Hints solve the remaining board and show its first removable arrow.
- Pinch zoom, drag pan, double-tap zoom and Fit. Tap coordinates are converted using the displayed camera transform. Multi-touch and drag gestures consume their movements to avoid accidental tap moves.
- Splash/loading state, home, level selection, gameplay, interactive tutorial, pause, settings, daily challenges, achievements, completion and game-over dialogs.
- Room snapshots include level seed, removed IDs, hearts, hints, undo history and completion state. A separate completion ledger prevents replay farming. Profile and daily records preserve unlocks, achievement counters and streaks. DataStore preserves sound, melody and vibration preferences.
- Local-date deterministic daily boards, a calendar for the current month, past-day play, completion marks and streaks. Daily generation passes the same validator as regular levels.
- Original synthesized tap/escape/collision/hint/completion/game-over sounds, optional sparse ambient melody and supported-device vibration. Music stops in the background and while paused. No external audio assets or network permission.
- Accessibility descriptions and per-arrow custom actions on the Canvas.

## Movement and collision design

Each arrow is an ordered, non-repeating, orthogonally connected grid path. The last cell determines an **integer cardinal direction**, so occupancy comparisons do not depend on floating point normalization. Corners use quadratic curves flattened into 16 steps, with under 0.002 board-unit deviation. The same flattened centerline drives drawing, full-body touch detection and animation.

The moving head follows its forward ray; the tail and intervening body points advance by arc length along the original path, then along that ray. The rendered body remains continuous until it has fully left the board.

For this snake movement, the original occupied path only vacates cells. All newly occupied cells belong to the head's forward ray. Therefore checking **every cell of the complete forward ray** against stationary arrows is equivalent to simulating the entire full-body escape, without timestep tunneling. The engine checks that ray before allowing animation. Self-crossing exits and overlapping initial occupancy are rejected by geometry validation. The grid is authoritative; this is not bounding-box collision. Stroke/head widths fit within the cell corridor.

Levels are generated in reverse removal order. A newly inserted arrow must be removable past all previously inserted arrows, while it may introduce blockers for those arrows. An independent greedy solver validates the resulting board before it reaches gameplay. Removal can only eliminate blockers, so greedy removal is complete for these rules. Templates ensure each new shape appears at its introduction level. Generation does not yet enforce a numerical difficulty/dependency score.

Animations read their progress inside the Canvas draw scope. Stationary centerlines and arc-length tables are cached in the arrow objects. A single arrow moves at a time. Pause cancels the animation job and resumes from its current distance. An interrupted animation is not committed as a removal; process recreation restores that arrow to its starting position.

## Test coverage and actual results

The actual Kotlin engine tests validate **all 100 initial seeds**, solve each puzzle, and independently advance full grid bodies step-by-step to check every intermediate occupancy. Additional tests cover straight/L/U/winding geometry, connectivity, occupancy, malformed paths and self-crossing exits, full-ray collisions, parallel paths, solver cycles, safe hints, arc-length continuity and body length, hearts/game-over, pause, undo, restart, completion, snapshot restoration and camera coordinate conversion.

The executed Gradle command was:

```sh
scripts/cloud-gradle.sh testDebugUnitTest assembleDebug lintDebug assembleDebugAndroidTest
```

**Build passed. All 13 Kotlin unit tests passed, including all 100 levels. Lint passed with no issues.** Both the debug application APK and Android instrumentation APK were produced. **All nine Android instrumentation tests passed on API 35**, including actual Canvas movement/rendering and on-device Room/DataStore checks. See `VALIDATION.md` for the evidence, environment limitations and APK checksum.

Android test sources cover full-body Canvas taps, curved path pixels, blocked curved arrows, replay after undo/restart, pause/resume, gameplay controls, Room transactions and disk reopen, and DataStore recreation. A separate real-app screen tap advanced progress from `0/12` to `1/12`; force-stop and cold relaunch restored `1/12` from Room.

The independent Python oracle remains available as a supplementary algorithm check: `python scripts/verify_grid_rules.py` (19,439 randomized cases). It is separate from the now-executed Kotlin tests.

## Managed cloud build

The earlier curl error was caused by the shell command's default network sandbox denying access to the configured proxy, not an unavailable Maven artifact. Run dependency-download/build commands with the environment's supported network permission. Keep the inherited HTTP(S) proxy and certificate verification enabled.

Tooling and caches live in ignored `work/`: `work/jdk`, `work/android-sdk`, `work/android-user`, and `work/gradle-home`. `local.properties` points to the cloud SDK and is ignored. `scripts/cloud-gradle.sh` selects the downloaded full JDK when needed, directs caches to writable paths, configures Gradle's Java proxy from the inherited HTTPS proxy, uses the environment's Java CA trust store, and compiles Kotlin in process. It does not change `$HOME`, bypass the proxy, disable TLS checks, or modify the network policy.

On a normal local workstation use `./gradlew` directly; the cloud helper is optional. If your network needs a proxy, configure Android Studio's HTTP proxy / Gradle system proxy settings and trust the relevant CA normally.

For cloud instrumentation without the native ADB client's writable-home requirement:

```sh
python -m pip install --target work/python -r scripts/requirements-emulator.txt
# Boot an authorized emulator with ADB transport on localhost:5555 first.
PYTHONPATH=work/python python scripts/emulator-tests.py
```

This installs the two built APKs and executes the real AndroidJUnitRunner over the emulator's standard ADB transport, with an RSA key stored in `work/`. On a local workstation use `./gradlew connectedDebugAndroidTest` instead. Instrumentation coverage is not a frame-rate benchmark.

## Release readiness

Compilation, engine solvability, lint, API 35 gameplay and forced-process-death persistence are verified; this is still **not a production-readiness claim**. Before release, finish physical-device / API-range testing, low-end frame profiling, rotation checks, camera gesture and audio/haptic review, difficulty playtesting, and release signing. Software-emulator timing does not establish 60 FPS on hardware.

Public research of the supplied Easybrain Play Store reference could not be completed: the search tool returned an error and environment network downloads could not connect to the proxy. No proprietary assets/code were used. Mechanics follow the user's complete master prompt.
