# Moonlight 12.2 RayNeo maintenance design

User scope: update official upstream and repair/rebuild existing adaptation; user explicitly permits replacing the old implementation. Default streaming target requested for weak 640x480-per-eye hardware: 1280x720 at 30 fps, fit into 640x360 with letterboxing. Settings remain editable. No installation or launch requested. User additionally requires efficient rendering without repeating known reboot-prone hardware View mirroring.

Baseline: origin/master b48494cb (12.2 / 315). Original seven dirty adaptation files preserved in stash `rayneo-pre-sync-20260917`; master still references f10085f5. Work branch rayneo/x3-pro.

## Architecture

- Restrict adaptation to identified X3 Pro family and 1280x480 active display, Android 29+; never use resolution alone. No global display-metric mutations.
- Application lifecycle registration and public WindowInspector enumerate process-owned windows. Install one single-content stereo host per root, including AlertDialog, ProgressDialog, preference dialogs and framework context menus. Exclude Game's already adapted video root, retain system windows outside application ownership. A pre-draw discovery hook plus bounded foreground-only periodic discovery handles asynchronous windows; no private-field reflection.
- Stereo host measures logical eye content at half the physical width, preserves child layout params, renders ordinary UI once into an eye-sized software bitmap and composites that bitmap twice. Never repeat hardware drawChild/RenderNode. No SurfaceView mirroring claim. Preserve default focus, implement temple gesture handling at host dispatch boundary; ordinary mouse/gamepad semantics remain unchanged.
- Native focus navigation: four directions; inspect real focus and AdapterView selection after dispatch; recover only invalid targets from current tree; no direction replay after handled events. Single-click confirmation waits for double-click resolution, binds to original focus/adapter/row identity, cancels on data/window changes. Double-click emits one original BACK; long press uses native context menu. No movable pointer for application UI.
- Game replaces its inflated StreamView with a visible input proxy at the same location before input listeners/capture setup. A dedicated full-width GLSurfaceView renders one decoded OES frame into two viewports in one swap. Explicit SurfaceHolder bridge preserves original decoder callback entry points and output size; retains one decoder and upstream stop/reconnect behavior. No bitmap video-frame copies. Keep original SurfaceView on normal devices.
- 720p30 default preset is initialized once on supported hardware, with 5 Mbps, SDR, fit and no virtual-controller overlay; subsequent user settings remain authoritative. Treat performance and physical interaction as pending until measured.
- Process-owned toast messages use an in-window presenter for stereo; system IME/permissions remain explicit external boundaries.

## Verification

Pure Java policy/gesture regression tests before implementation; Android build, unit tests and lint; inspect APK version/package/ABIs/signature and hash. Window inventory maps every constructor/creation path to host or explicit system boundary. No device launch implied by build. Physical stereo, temple mapping, network streaming, hardware decoder load and thermal behavior require separate real-device verification.

## Tradeoffs

Retaining only UiHelper is too narrow (misses independent windows). Replacing every dialog/preference duplicates maintenance touchpoints. Public process-window enumeration centralizes coverage with a small discovery delay (foreground scan bounded to 250 ms), to be checked on hardware. OES still uses GPU rendering and hardware decoding: do not claim that avoidance of repeated HWUI proves firmware stability or thermal gains. No automatic device launch to validate this path.
