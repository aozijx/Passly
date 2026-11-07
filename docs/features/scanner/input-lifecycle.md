# 扫描输入与生命周期

状态：相机/图片主链路已实现；`Work.HostPaused` 与显式 `Work.Stopped` 已区分，仍待 Route/Activity Result 关闭竞态、图片解码器取消/客户端清理及设备坐标验收。

相机帧和相册图片都产生原始扫码文本，但输入资源及取消合同不同。`ScannerCameraHost` 持有 CameraX/ML Kit 与 `ImageProxy`；`ScannerViewModel` 持有扫描任务 owner、原始结果状态、剪贴板写入和语义 effect；`ScannerRoute` 持有相册选择、生命周期 effect 与 owner 提供的结果计划。扫码层不解析内容类型，调用方在自己的业务入口转换。

## 相机输入

相机授权由 `ScannerCameraHost` 请求；它只管理权限、Compose 生命周期和共享资源。`bindScannerCamera()` 创建 Preview 与 ImageAnalysis，`ScannerCameraSession.bind()` 等待 `previewView.viewPort`、布局尺寸和未销毁状态就绪后，再将 use case 放入同一 `UseCaseGroup`。`createScannerImageAnalysis()` 专管帧处理：用当前 `ImageProxy.cropRect`、旋转角度、预览尺寸和共享取景框创建 `ScannerRecognitionRegion`；坐标暂不可用时关闭并跳过该帧，不启动 ML Kit。有效时才构造 `InputImage`；回调先筛选有 `rawValue` 且实际 `cornerPoints` 四角都位于区域内的候选，无 corner points 时才检查 `boundingBox` 四角。`addOnCompleteListener` 关闭当前 `ImageProxy`。退出 effect 时设置原子 `disposed` 标志并由 Session 停止相机；组件释放时也关闭 executor 和 ML Kit scanner。

`ScannerRoute` 从 `BoxWithConstraints` 读取可用尺寸，`ScannerViewfinderFrame.fitContainer()` 统一计算视框尺寸并一次性将 dp 几何转换为像素矩形；纯 `ScannerScreen` 只接收 `ScannerScreenModel`、这份几何和用户动作回调，`ScannerViewfinder` 绘制它，Route 同时把同一份几何交给 `ScannerCameraHost`。`ScannerScreenAction` 只表示屏幕上的用户操作；相机权限、识别结果和相机错误由 Host 回调直接进入 Route/ViewModel，不混入 UI 事件类型。`ScannerResultSession` 由 Route 生命周期持有，不随扫码 UI 状态重建；确认或关闭期间锁住结果操作，退出动画中的相册按钮保持禁用，避免页面退出时误触失效动作。`ScannerRecognitionRegion.create()` 使用 CameraX `ImageProxyTransformFactory` 和 PreviewView 输出变换构造坐标转换；识别时将 ML Kit 返回的旋转后条码角点转换到 PreviewView 坐标，再与同一份取景框几何比较。候选的实际四角必须全部落在取景区域内，防止只因中心进入框内就接收实际越界的码。

只有区域有效且条码四角完整位于取景区域内的候选才会触发扫描完成；没有 `rawValue` 的候选会在映射之前排除，避免挡住后续有效候选。

`ScannerScreen` 以 Material 3 `CenterAlignedTopAppBar` 显示返回箭头、标题和相册操作；格式提示及中英文资源已删除。结果 `AnimatedContent` 使用 `.using(null)` 禁用父容器的尺寸变换，卡片保持底部锚定并只做淡入和上移动画。Vault、OTP 与 Wi-Fi 编辑器通过 `AnimatedVisibility` 展示 Scanner。关闭事件由 `ScannerRoute.dismissScanner()` 先派发 `StopScanning`、关闭导入会话，再通知父层开始退出动画，因此 Route/Screen 无需重复接收 `active` 可见性参数。

`ScannerRoute` 将检测回调映射为 `ScannerUiAction.BarcodeDetected(action.rawValue)`。`ScannerViewModel.onAction(action)` 只在 `uiState.value == Scanning` 时接收帧，并调用 `completeScan(raw)`；该函数拒绝空白值，替换当前 `Work.Active` owner、取消旧 owner、提交 `ScanCompleted(ScannerResult(raw))` 并发送一次 `ScanSucceeded` effect。之后的相机帧不覆盖已展示结果；Domain 解析和调用方处理不在此 ViewModel 内执行。

## 相册图片输入

`ScannerRoute` 通过 `rememberImagePicker { uri, _ -> ... }` 将平台 URI 包装为 `ImageRef(uri.toString())`，并派发 `ScannerUiAction.DecodeImage(image)`。选择照片前先派发 `StartScanning`。若 picker 期间宿主停止，`ScannerViewModel.pauseForHost()` 把 owner 设为 `Work.HostPaused` 并让 UI reducer 进入 `Stopped`；图片结果到达时，`decodeImage(image)` 允许从 `HostPaused` 接管新 owner、恢复扫描状态并开始处理。显式 `StopScanning` 则设为 `Work.Stopped`，同一 `DecodeImage(image)` 会被拒绝；销毁后也拒绝输入。接管后在新 owner scope 中调用注入的 `decode(image)`。

默认解码器 `BarcodeImageDecoder.decode(ref)` 以 `withContext(Dispatchers.IO)` 调用 `InputImage.fromFilePath(context, ref.value.toUri())`，之后通过 `suspendCancellableCoroutine` 创建 ML Kit client 并调用 `scanner.process(image)`。完成 listener 先关闭 client；若 continuation 已取消就丢弃结果，否则把首个非空值变为 `Decoded(rawValue)`，无值返回 `NoBarcode`，Task 失败返回 `Failed(error)`。回到 ViewModel 后先执行 `currentCoroutineContext().ensureActive()` 并比较 `work === owner`，再把文本交给 `completeScan(rawValue)` 或发出图片失败 effect；这样旧图片的迟到回调不能更新新任务。

`BarcodeImageDecoder` 的 listener 注册边界当前以 `catch (Exception)` 处理同步异常；若第三方调用同步抛出 `CancellationException`，内层 catch 会将它包装成 `BarcodeImageResult.Failed`，偏离项目取消合同。该问题只记录在[扫码待办](../../superpowers/plans/2026-11-06-passly-scanner-review-followups.md)，本轮不改运行代码。测试应证明取消原样传播且 `scanner.close()` 仍执行。

## 生命周期与敏感状态

`collectScannerRouteEffects()` 在宿主 `STARTED` 时派发 `HostStarted` 并消费 effect，离开该状态派发 `HostStopped`。`ScannerViewModel.pauseForHost()` 轮换 owner：显示结果可保留，纯扫描和图片处理中状态转 `Stopped` 并记录为 `Work.HostPaused`；宿主恢复后重新开始扫描。若相册结果在宿主仍暂停时到达，`DecodeImage` 可以从 `Work.HostPaused` 接管为新 owner。显式 `StopScanning` 则记录为 `Work.Stopped`，迟到的照片结果和 `HostStarted` 都不能重建 owner；`onCleared()` 设为 `Work.Destroyed`，拒绝所有后续输入。

`ScannerViewModelTest` 分别覆盖显式停止、销毁后的迟到输入拒绝，以及宿主暂停期间 picker 结果接管 owner；HostStarted 只恢复 `Work.HostPaused`。这些回归断言和实现已更新，但本轮尚未运行测试。复制文本同样在当前 owner scope 中运行，传给 `SensitiveClipboardWriter.writeSensitiveIf(text) { work === owner }` 的资格闭包会在平台写入前再次核对 owner。

一次性复制、图片提示和震动由 Route 消费；用户可见提示只传 effect 类型，异常文本不进入 Toast。`rememberImagePicker` 的 Activity Result callback 派发 `DecodeImage(ImageRef(uri.toString()))`；`collectScannerRouteEffects()` 在生命周期进入 `STARTED` 时派发 `HostStarted`。源码没有建立这两个回调的先后屏障，不能假设照片结果一定在恢复 effect 之后到达。ViewModel 已允许 `HostPaused` 接管有效图片结果，并拒绝显式 `Stopped`/`Destroyed` 后的输入；Route/Activity Result 与关闭交错仍见[扫码待办 Task 4](../../superpowers/plans/2026-11-06-passly-scanner-review-followups.md#task-4区分显式关闭与宿主暂停后的输入恢复资格)。CameraHost 内的原始回调不直接写库或持久化扫描内容。

任务 owner、迟到图片结果和宿主暂停行为由 [`ScannerViewModelTest`](../../../app/src/test/java/com/aozijx/passly/feature/scanner/presentation/ScannerViewModelTest.kt) 覆盖；Route 生命周期 effect 由 [`ScannerRouteEffectsTest`](../../../app/src/test/java/com/aozijx/passly/feature/scanner/presentation/ScannerRouteEffectsTest.kt) 覆盖。它们使用注入的 decode lambda 验证 ViewModel 编排，不调用真正的 ML Kit client；`BarcodeImageDecoder` 同步注册取消/错误与 scanner client 清理仍由扫码待办要求补专用测试。

相机坐标矩阵按 [CameraX 输出变换说明](https://developer.android.com/media/camera/camerax/transform-output)从 crop rect 和 rotation 映射；共享 [`ViewPort`](https://developer.android.com/reference/androidx/camera/view/PreviewView#getViewPort()) 使 Preview 与 ImageAnalysis 使用同一可视裁剪。ML Kit 将条码坐标定义在输入图像中，并提供 [`cornerPoints`](https://developers.google.com/android/reference/com/google/mlkit/vision/barcode/common/Barcode#getCornerPoints())；当前规则要求四个角都通过 [`RectF.contains(x, y)`](https://developer.android.com/reference/android/graphics/RectF#contains(float,%20float))，因此右/下边界不包含。设备上的旋转方向和取景裁剪仍待实机验证。

## 代码位置

| 职责 | 类/函数 | 源码 |
| --- | --- | --- |
| 权限与 Compose 生命周期 | `ScannerCameraHost()`、`bindScannerCamera()` | [`ScannerCameraHost.kt`](../../../app/src/main/java/com/aozijx/passly/feature/scanner/presentation/ScannerCameraHost.kt) |
| CameraX 共享 ViewPort、绑定重试与释放 | `ScannerCameraSession.bind()`、`stop()` | [`ScannerCameraSession.kt`](../../../app/src/main/java/com/aozijx/passly/feature/scanner/presentation/ScannerCameraSession.kt) |
| 每帧识别及取景框坐标变换 | `createScannerImageAnalysis()`、`ScannerRecognitionRegion.create()`、`contains(barcode)` | [`ScannerCameraFrameAnalyzer.kt`](../../../app/src/main/java/com/aozijx/passly/feature/scanner/presentation/ScannerCameraFrameAnalyzer.kt)、[`ScannerRecognitionRegion.kt`](../../../app/src/main/java/com/aozijx/passly/feature/scanner/presentation/ScannerRecognitionRegion.kt) |
| Route 统一计算并共享取景框几何；纯 UI 按模型渲染 | `ScannerViewfinderFrame.fitContainer()`、`ScannerScreen(model, frame, onAction)`、`ScannerScreenModel` | [`ScannerViewfinderFrame.kt`](../../../app/src/main/java/com/aozijx/passly/feature/scanner/presentation/ScannerViewfinderFrame.kt)、[`ScannerScreenModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/scanner/presentation/ui/ScannerScreenModel.kt)、[`ScannerScreen.kt`](../../../app/src/main/java/com/aozijx/passly/feature/scanner/presentation/ui/ScannerScreen.kt) |
| 图片输入解码 | `BarcodeImageDecoder.decode(ref)` | [`BarcodeImageDecoder.kt`](../../../app/src/main/java/com/aozijx/passly/feature/scanner/presentation/BarcodeImageDecoder.kt) |
| 任务 owner、状态与剪贴板 | `ScannerViewModel.onAction()`、`decodeImage(image)`、`completeScan(raw)` | [`ScannerViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/scanner/presentation/ScannerViewModel.kt) |
| 生命周期 effect 收集 | `collectScannerRouteEffects()`、`ScannerRoute()` | [`ScannerRouteEffects.kt`](../../../app/src/main/java/com/aozijx/passly/feature/scanner/presentation/ScannerRouteEffects.kt)、[`ScannerRoute.kt`](../../../app/src/main/java/com/aozijx/passly/feature/scanner/presentation/ScannerRoute.kt) |
