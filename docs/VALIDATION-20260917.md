# 2026-09-17 host validation

Upstream was fetched and rechecked at `b48494cb96bff23d8886c4775cc4f39a1075495d` (Moonlight 12.2). Recursive submodules are clean at their pinned upstream commits.

## Artifact

- File: `out/Moonlight-12.2-rayneo.1-debug.apk`
- Package: `com.limelight.debug`
- Version: `12.2-rayneo.1` / code 315
- minSdk 21, targetSdk 36, compileSdk 37, debuggable
- ABIs: arm64-v8a, armeabi-v7a, x86, x86_64
- Size: 17,792,344 bytes
- SHA-256: `09cc9f4e7b5e9726b5657f8dc6cf331bfe8b770c7595e28db5dad651edf4bcd0`
- APK signature verification: v1 and v2 passed; Android Debug RSA 2048 certificate
- Certificate SHA-256: `03a937d17b2d6a88181d35e686809dc1a3640ee237a22d85a250b5e92e522292`
- `zipalign -c -P 16 4`: passed

## Executed checks

JDK 17.0.15; Gradle 9.7.1; AGP 9.4.0; NDK 29.0.14206865.

`scripts/build-rayneo.ps1` ran `assembleNonRootDebug`, `testNonRootDebugUnitTest`, `lintNonRootDebug`, standalone Java policy/gesture tests, and Python capture test successfully.

- Android UI tests: 7 passed, no failures/skips. Single software traversal at logical size; detached target cancellation; stable confirmation exactly once; second-contact cancellation; relabeled-target cancellation; normal boundary retention; recovery from removed controls without activation.
- Standalone Java: 9 device/geometry assertions + 12 gesture assertions passed.
- Capture test: simulated boot-ID change stopped logcat and recorded reboot without launch/install/reboot commands.
- Lint task passed: **0 errors, 194 warnings**. The tool also prints an internal Turkish spelling quick-fix exception for an upstream string. This is not a claim of zero warnings or flawless lint-engine coverage.
- Source review corrected hardware Bitmap/software Canvas incompatibility, pending-tap cancellation, dialog logical measurement, and GL-thread exit cleanup ordering.
- `git diff --check`: passed.

## Not established

No device install or launch was performed. `adb devices -l` returned no online device during this run. Real host pairing, network streaming, hardware decoder availability, temple mappings, optical stereo, popup timing/alignment, sustained CPU/GPU/thermal cost, and whole-device reboot avoidance remain unverified.

Software UI avoids repeated hardware View drawing, but OES video still uses the GPU and hardware decoder. Diagnostic stages and persistent logs narrow failure boundaries; they do not establish a cause for previous device reboots. See [RAYNEO.md](RAYNEO.md) for capture workflow and the window inventory.
