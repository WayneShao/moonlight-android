# Moonlight 12.2 · RayNeo X3 Pro

维护分支：`rayneo/x3-pro`；官方基线：`b48494cb`，版本 12.2 / 315。

这是有源码的适配与诊断构建。源码检查、构建、主机测试与眼镜实测分别记录；没有把主机测试当作固件稳定性或性能证明。

## 默认配置

- 首次在已识别设备上运行时，设置 1280×720、30 fps、5 Mbps、SDR、等比显示、关闭屏幕虚拟手柄。该预设只应用一次，随后保留用户修改。
- 640×480 是单眼显示区域；当前适配要求物理窗口 1280×480。720p 画面在每眼显示为 640×360，上下各留 60 像素。未修改系统 `wm size`、全局 density 或资源 displayMetrics。
- 仅在 API 29+、设备型号 ARGF20 或设备名 MercuryLiteXR、实际显示 1280×480 时启用。其他设备保留上游路径。
- 菜单采用原生焦点导航：四向滑动、单击确认、双击原 BACK、长按原上下文菜单。没有新增自由移动鼠标指针。串流界面保留原鼠标/手柄输入；镜腿方向/确认走原按键入口。
- OES 路径仅支持 SDR，运行串流时明确关闭 HDR 与 PiP，并记录日志；用户可调整串流分辨率、帧率、码率及拉伸。重新开启虚拟手柄时，其根仍留在双眼 UI 内，但实体多点交互待验。

## 渲染与生命周期

视频：单个 MediaCodec → SurfaceTexture/OES → 同一帧一次 `updateTexImage()` → 两个 viewport → 单次 EGL swap。使用生产者 transform matrix，保留 crop/翻转；实际 decoder output size 变化会更新视频和输入区域。按新帧 `requestRender()`，不使用连续空转模式，不进行视频 CPU Bitmap/PixelCopy 循环。

普通 UI：单棵逻辑内容树 → 640×480 软件 Bitmap → 两眼复用 Bitmap。每次 UI 重绘只遍历一次子树；Bitmap 仅在尺寸改变时重新分配，避免重复硬件 `drawChild`/RenderNode。游戏封面在支持设备上用软件 allocator，防止硬件 Bitmap 进入软件 Canvas。视频独立 Surface 不参与 UI 复制。

仍然使用硬件解码和 GPU 视频合成；这不等于已排除所有硬件/固件重启风险。没有证据把既往重启归因于 CPU 阉割、缺库或具体驱动。EGL/Surface 丢失后结束本次串流，重新进入游戏建立新会话，不复用旧 decoder。关闭时先通知原回调，再释放资源；GL thread 退出后有幂等 Java Surface 清理，处理退出时队列任务被跳过的情况。

## 窗口创建路径清单

| 创建路径 | 接入位置 | 本次证据 / 待验项 |
|---|---|---|
| PcView 主机页、AppView 游戏页 | Application → WindowInspector → StereoHost | 源码、构建；网格滚动/实际焦点待验 |
| AddComputerManually、StreamSettings | 同一普通窗口入口 | 源码；文本输入及设置交互待验 |
| HelpActivity WebView、缩放/文本选择窗口 | 普通窗口 + 进程子窗口发现 | 软件 WebView、选区/菜单待验 |
| Dialog 公共错误/帮助提示、UiHelper 删除/退出确认 | 独立 Dialog decor 同一入口 | 源码；按钮、背景、外部取消待验 |
| SpinnerDialog ProgressDialog | 同一独立窗口入口 | 源码；加载动画/取消待验 |
| ListPreference、LanguagePreference、SeekBarPreference、ConfirmDeleteOscPreference | 框架创建的独立窗口，同一入口 | 列表选中、滚动、关闭焦点待验 |
| PcView/AppView 原生上下文菜单 | WindowInspector 子窗口/对话框 | 按原 callback 返回；实际长按待验 |
| 虚拟手柄配置/颜色对话框 | 原构造路径 + 同一窗口入口 | 原功能保留，实机待验 |
| Game 视频 | 专用全宽 StereoVideoView | GL 生命周期/着色器编译已构建；真实解码待验 |
| Game 性能/警告叠层、虚拟手柄 | 独立 StereoHost，早于 controller 创建 | 源码；视频叠加和多点输入待验 |
| 原有 Toast 调用 | StereoToast，前台双眼窗口内呈现 | 保留时长/替换；无前台窗口回退系统 Toast |
| ShortcutTrampoline | 无独立业务内容，跳转既有目标 | 目标使用对应 Activity 路径 |
| PcView 临时 GLSurfaceView 能力探测 | 上游探测流程 | 非业务显示；不宣称普通 UI 能复制其 Surface |
| IME、系统权限、外部浏览器、无前台系统 Toast | 系统/其他进程拥有 | 不接管，不计作应用双眼覆盖 |

窗口发现使用公开 API，无私有 WindowManager 反射。前台 pre-draw 加 250 ms 定时发现；不在后台轮询。独立窗口扩展物理宽度，逻辑面板在单眼内重新测量；窗口位置更新后维持双眼间距。发现机制可能出现短暂首帧单眼，动态 Popup anchor、非模态弹层触摸区域和对话框在小高度下的布局仍需实机验收。

## 构建与自动验证

要求 JDK 17、Android SDK platform 37 / build tools 36、NDK 29.0.14206865。Gradle wrapper 为上游 9.7.1。NDK/SDK 由 Gradle 按已接受的许可证安装缺失组件。测试使用 Robolectric API 32，运行 JDK 17；本机默认 JDK 24 与当前 Robolectric 不兼容。

```powershell
git submodule update --init --recursive
# local.properties 配置本地 sdk.dir；Windows 盘符冒号须转义。
./scripts/build-rayneo.ps1 -JavaHome 'PATH_TO_JDK_17'
```

产物 `out/Moonlight-12.2-rayneo.1-debug.apk`，包名 `com.limelight.debug`，版本 `12.2-rayneo.1`，保留上游非官方调试包边界。由本机调试证书签名，跨机器重建的 debug 证书不保证相同，覆盖安装前须核对签名。源码 fork 并未发布签名私钥。

测试：设备/尺寸策略 9 项，手势状态机 12 项，Android UI 7 项，主机重启捕获 1 项。Lint 沿用上游规则；存在上游警告及土耳其语拼写 quick-fix 内部异常，构建日志必须保留，不把任务成功描述成所有检查器无警告。

## 崩溃定位日志

应用日志写在私有目录 `files/rayneo-diagnostics/events.jsonl`，2 MiB 轮换为 `previous.jsonl`。记录 wall time、elapsed time、PID、线程、固件/ABI、boot ID、内存类别、CPU 核数和 GL 能力。关键初始化/销毁边界同步刷盘；10 秒帧统计不 fsync，不逐帧刷盘。没有记录主机地址、配对口令或视频内容。

关键阶段：

1. `process.start` / `capabilities` / `activity.created`：基础环境。
2. `first-draw begin` → `software-complete` → `composite-recorded`：普通 UI 绘制边界，最后一项只证明记录了合成命令。
3. `egl.context.create`、`egl.window.create` 的 begin/end 与 EGL error；GL vendor/renderer/version/extensions。
4. `shader.compile` / `shader.link` / `oes.ready`：GL 程序与 decoder surface。
5. `codec.create` / `codec.configure` / `codec.start` 的 begin/end；实际硬件 decoder、格式、Surface validity、CodecException 与 output format/crop。
6. `video.first-frame` → `first-stereo-commands-recorded`：收到解码帧、提交双眼绘制命令。后者不证明显示器已呈现或光学同步。
7. `renderer.close` / `resources-released` / `detached handles-released` / EGL destroy：回收边界。

调试构建可导出自己的日志（设备在线后执行）：

```powershell
adb -s SERIAL exec-out run-as com.limelight.debug cat files/rayneo-diagnostics/events.jsonl
```

主机采集先于用户启动应用，持续时间默认 180 秒；输出目录必须是新的。Ctrl+C 结束本次 logcat 子进程。脚本只读，不连接新地址、不启动应用、不切 USB 模式、不重启任何组件。

```powershell
adb devices -l
python scripts/capture_rayneo.py --serial SERIAL --seconds 180 --output out/diagnostics-NEW_SESSION
```

USB 掉线会分段保存日志，恢复后比较 boot ID 与 uptime；boot ID 改变立即停止采集并记录 boot reason/pstore 可见性，不自动再次启动故障应用。无法读取的证据会明确保留命令失败/超时。私有日志也可能因瞬时掉电/内核问题丢失最后几条；最后日志只能收窄阶段，不等于根因。

## 仍需眼镜验收

本次未安装、未启动。执行 `adb devices -l` 时无在线设备。依次验收普通界面/弹窗/镜腿，再短时 720p30 串流；记录真实 decoder、双眼帧、延迟、CPU/GPU/温度。覆盖连接中退出、串流退出、后台/重新进入、EGL 丢失、分辨率变化和系统方向开关。发生整机重启时保留日志/构建并停止重现，再判断下一步；USB 断连本身不证明重启。
