# Device validation — 2026-09-17

Final installed version 12.2-rayneo.2 / 315, package com.limelight.debug, label Moonlight.
APK SHA256: cc6eb158ebb5cf8f820dee8326f952a171e97950478a6b96ca089abee6a640d1

Updated Sunshine 2026.914.233613 paired and streamed successfully. Actual decoder c2.qti.hevc.decoder.low_latency, hardware/vendor true; 1280x720, 30 fps, 5 Mbps SDR; Adreno 621. EGL/shader/OES/decoder initialization and first-frame logs succeeded.

Performance (desktop content, before final input/overlay refinements): 28–30 fps, CPU 37.4–39.0% of ONE core, PSS 78242 KiB (~76.4 MiB), RSS 201020 KiB. GPU device-wide gpubusy ratios 7.4–8.2%; GPU clock unreadable. Battery sensor 35.5 C -> 37.5 C -> 38.0 C while USB charging; Thermal Status 0, no temperature channels. Not surface temperature or sustained thermal acceptance; final software-host-layer changes were not rebenchmarked.

Boot ID unchanged throughout deployment/testing: no whole-device reboot observed. Installed in place, pairing preserved. Final injected touchscreen test: first triple stays in Game and shows confirmation in BOTH eyes; second triple within 3 seconds returns to AppView. Surface/EGL destruction and final handle release logged. Device left in AppView, streaming ended.

Input: menus retain native focus; stream temple motion controls pointer; single tap left, two-finger tap right; remote double click preserved with distinct 100 ms pulses. First triple arms exit and second confirms. Unit tests cover pointer lift order, click sequencing, cancellation, secondary mouse filtering, confirmation timeout. Physical right-temple identification/directions/multi-touch capability remain unverified; ADB/scrcpy is not temple acceptance. Known cyttsp/capsense devices are accepted rather than assuming enumeration establishes physical side.

Dynamic shared-UI Toast had a persistent right-eye refresh defect. Exit confirmation uses two independent native text views over the video and was verified visually in both eyes. General dynamic Toast/overlay coverage still needs investigation; do not claim all application windows physically accepted. Full-host software caching and damage expansion remain in this build.

21 JUnit/Robolectric tests, 21 standalone assertions, Python capture test, build/lint and APK signature/alignment checks passed. Existing lint warnings remain. Local diagnostic data/screenshots are under out/device-20260917, excluded from Git. No Windows lock-screen/display-off/sleep test was performed.
