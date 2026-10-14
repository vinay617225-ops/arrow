# Android build and gameplay validation

Verified on 14 October 2026 in `/workspace/arrow`. The native application now compiles and produces a signed debug APK. This report records executed checks; it does not claim production readiness.

## Actual results

| Check | Result |
| --- | --- |
| `testDebugUnitTest` | **Passed: 13 tests, 0 failures, 0 errors** |
| Actual Kotlin engine, initial seeds 1–100 | **Every level validated and solved**; all intermediate discrete body occupancies checked independently |
| `assembleDebug` | **Passed**; application APK produced |
| `lintDebug` | **Passed: no issues** |
| `assembleDebugAndroidTest` | **Passed**; instrumentation APK produced |
| Android 15 / API 35 instrumentation | **Passed: 9 tests**, AndroidJUnitRunner reported `OK (9 tests)` |
| Main app touch + forced process death | **Passed**: `0/12` → `1/12`, force-stop / cold relaunch / Continue restored `1/12` |
| APK signature verification | **Passed**, APK Signature Scheme v2, one signer |
| Packaged SDK levels | **Verified**: minSdk 26, targetSdk/compileSdk 35 |
| Independent Python grid-rule oracle | **Passed: 19,439 randomized cases** (supplementary; not a substitute for Kotlin tests) |
| Gradle/helper shell syntax and source whitespace | **Passed** |

Executed build command:

```sh
scripts/cloud-gradle.sh testDebugUnitTest assembleDebug lintDebug assembleDebugAndroidTest
```

Executed device command after emulator startup and APK installation:

```sh
PYTHONPATH=work/python python scripts/emulator-tests.py --skip-install
```

This runs **real AndroidJUnitRunner instrumentation**, through the standard authenticated ADB transport to the local emulator, rather than a simulation of Android UI behavior. The native ADB client requires a writable `/home/agent/.android` and cannot use this environment's read-only home; the Python ADB transport stores its RSA key in ignored `work/` instead. Normal workstations can run `./gradlew connectedDebugAndroidTest`.

## Tested gameplay and persistence

The Kotlin suite exercises straight/L/U/winding geometry, connectivity and occupancy, duplicate/disconnected/off-grid paths and self-intersecting exits, long-distance blockers, clear parallel paths, solver cycles, safe hints, continuous curved arc-length movement and constant body length, hearts/game-over, pause, undo, restart, completion, durable snapshot restoration and camera conversion at zoom 1–4 with pan.

The 100-level test executes the **application's actual Kotlin `Engine.generate`, validator, collision engine and solver**. For each solution it independently advances every ordered cell body through the complete exit and rejects any collision with stationary occupancy. All 100 seeds pass, and seeded regeneration matches.

Nine executed Android tests:

1. Tap a curved Canvas body and escape its entire arrow.
2. Undo and Restart, then replay the same arrow with a fresh animation rather than its prior completed distance.
3. Block a curved arrow with another arrow: lose a heart, remove no arrows.
4. Pause an escape, prevent early completion, then resume and finish.
5. Capture rendered pixels along the curved centerline: no visual gaps.
6. Navigate from Home to gameplay, use Hint, Restart and Pause/Resume.
7. Toggle DataStore settings and recover them through a new repository.
8. Round-trip a Room session and atomically persist completion/profile/daily history.
9. Close and reopen an on-disk Room database and recover the session, hearts, hints, history, unlocks and completion ledger.

A separate main-application check launched `com.arrowescape/.MainActivity`, selected a legal arrow through the compiled Kotlin engine, converted its tail to the real displayed board bounds, and dispatched an Android screen tap. Progress advanced from `0/12` to `1/12`. After `am force-stop com.arrowescape`, a cold relaunch and Continue restored `1/12`. The real gameplay screenshot was inspected. This check initially encountered a **System UI** ANR overlay (not an Arrow Escape error dialog); dismissing that overlay with its normal Wait action allowed the check to complete. No app state or database was edited to create the result.

Evidence remains in ignored build/work paths:

- `app/build/test-results/testDebugUnitTest/TEST-*.xml`
- `app/build/reports/tests/testDebugUnitTest/`
- `app/build/reports/lint-results-debug.txt` (`No issues found.`)
- `work/build-verified.log` (`BUILD SUCCESSFUL`)
- `work/instrumentation.log` / `work/device-tests-retry.log` (`OK (9 tests)`)
- `work/relaunch-retry.log` and `work/gameplay.png` (main app touch, force-stop and restored board)

## Network failure and resolution

The initial default-sandbox request failed with curl exit 7, reporting `Failed to connect to proxy port 8080`. A direct connection diagnostic in that same sandbox returned `PermissionError: [Errno 1] Operation not permitted`. DNS correctly resolves `proxy` to `172.31.5.61`. The environment reports its package-manager network policy as enforced.

Repeating the request with the tool's supported **network permission enabled for the command** succeeds through `http://proxy:8080`. The sidecar and repository services are healthy. No destination policy, proxy route, TLS verification or authentication protection was bypassed or disabled; no environment policy file was edited.

Previously attempted URLs, now reachable through the authorized proxy:

- `https://downloads.gradle.org/distributions/gradle-8.10.2-bin.zip` (redirects to the official GitHub Gradle distribution)
- `https://downloads.gradle.org/distributions/gradle-8.10.2-bin.zip.sha256`
- `https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip`
- `https://dl.google.com/android/repository/repository2-3.xml` and related SDK manifests
- `https://dl.google.com/dl/android/maven2/com/android/tools/build/gradle/8.7.3/gradle-8.7.3.pom`
- `https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-gradle-plugin/2.0.21/kotlin-gradle-plugin-2.0.21.pom`
- `https://plugins.gradle.org/m2/com/google/devtools/ksp/com.google.devtools.ksp.gradle.plugin/2.0.21-1.0.28/com.google.devtools.ksp.gradle.plugin-2.0.21-1.0.28.pom`

No usable Gradle/Kotlin/Android SDK caches existed initially. Downloads now populate ignored workspace caches. Switching repository mirrors would not have solved the command's network sandbox denial. Google Maven, Maven Central and Gradle Plugin Portal remain the configured repositories.

## Toolchain compatibility and fixes

- **Gradle 8.10.2**: standard wrapper scripts and JAR generated by Gradle; distribution pinned to SHA-256 `31c55713e40233a8303827ceb42ca48a47267a0ad4bab9177123121e71524c26`.
- **Android Gradle Plugin 8.7.3**: requires Gradle 8.9+ / JDK 17+ and supports compile SDK 35; verified compatible here.
- **Kotlin 2.0.21 / Compose compiler 2.0.21 / KSP 2.0.21-1.0.28**: matching versions; compilation and generated Room code pass.
- **Compose BOM 2024.12.01** resolves Compose UI/runtime 1.7.6; native Compose tests pass.
- **SDK Platform 35 and Build Tools 34.0.0**: the latter is AGP 8.7's default; both installed. Build Tools 35.0.0 is also available, but this build uses 34.0.0.
- The supplied Java 21 installation was a **JRE without a Java compiler**. Installed checksum-verified Temurin **JDK 17.0.16**, including `javac`, in `work/jdk`.
- SDK downloads initially also failed because their default home cache was read-only. `ANDROID_USER_HOME` and Gradle caches now point into writable `work/`; the helper also directs Java home-dependent tooling there without changing `$HOME`.
- Java dependency downloads use the configured proxy and environment CA trust store. The cloud helper compiles Kotlin in process to avoid the initial daemon startup/cache failure.
- Fixed replay animation state surviving Undo/Restart or another level. Paused animations now resume with the remaining duration rather than restarting a full-duration timer.
- Shared the displayed camera conversion between Canvas drawing and hit testing, with zoom/pan regression coverage.
- Fixed the two lint findings with an original vector launcher icon and explicit Android 12+ data extraction rules.

## Emulator limitations and remaining release work

The environment has no `/dev/kvm`. Testing used an Android 15 x86_64 software emulator with SwiftShader, then a 480×800 / 160dpi display to reduce rendering load. Initial Android system startup suffered ANRs in system UI, launcher and phone services; the first instrumentation process also hit a startup ANR before reporting any test. No application Kotlin exception was logged. After the emulator settled and standard ahead-of-time package compilation, the retry completed all nine tests in 340.474 seconds. The initial failed attempt is recorded in `work/device-tests.log` and `work/instrumentation-crash.log`.

This verifies a build and gameplay behavior on one software-emulated API level. It does **not** establish 60 FPS, physical-device audio/haptic behavior, all supported API/device layouts, complete camera gesture coverage or release-store readiness. Finish device/API-range and performance testing, difficulty review and release signing before publishing. Local Android Studio setup, debug APK and release AAB instructions are in README.md.

## Debug APK

- Path: `app/build/outputs/apk/debug/app-debug.apk`
- Size: **10,185,577 bytes**
- SHA-256: `b797ba83236ca015630be5449ffeda1595c3c53b304b706e4dc11bbb7fa1ba31`
- Package: `com.arrowescape`; version 1.0 (code 1)
- Debug-signed; not a production/release signing key.
