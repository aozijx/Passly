# 扫描输入与生命周期

状态：当前实现（代码核对：2026-11-06）。

相机帧和相册图片共用 `ScannerResult` 分类器，但输入资源及取消合同不同。`ScannerCameraHost` 持有 CameraX/ML Kit 与 `ImageProxy`；`ScannerViewModel` 持有扫描任务 owner、分类状态、剪贴板写入和语义 effect；`ScannerRoute` 持有相册选择、生命周期 effect 和本地化提示。`ScannerScreen` 只接收状态与交互事件。

## 相机输入

相机授权由 `ScannerCameraHost` 请求。`DisposableEffect(lifecycleOwner, hasPermission, isScanning)` 先 `cameraSession.stop()`，仅在已授权且处于扫描态时取得 `ProcessCameraProvider`，以 `ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST` 绑定后摄预览和分析。Analyzer 将 `ImageProxy.image` 与旋转角构造成 `InputImage`，调用共享 `barcodeScanner.process(image)`；成功时取第一个非空 `rawValue` 调用 `currentOnBarcodeDetected(value)`，失败时将 Throwable 交 `currentOnFailure(error)`，`addOnCompleteListener` 关闭当前 `ImageProxy`。退出 effect 时设置原子 `disposed` 标志并停止相机；组件释放时也关闭 executor 和 ML Kit scanner。

`ScannerRoute` 将检测回调映射为 `ScannerUiAction.BarcodeDetected(action.rawValue)`。`ScannerViewModel.onAction(action)` 只在 `uiState.value == Scanning` 时接收帧，并调用 `completeScan(raw)`；该函数调用 `ScannerResultClassifier.classify(raw)`，替换当前 `Work.Active` owner、取消旧 owner、提交 `ScanCompleted(result)` 并发送一次 `ScanSucceeded` effect。之后的相机帧不覆盖已展示结果。

## 相册图片输入

`ScannerRoute` 通过 `rememberImagePicker { uri, _ -> ... }` 将平台 URI 包装为 `ImageRef(uri.toString())`，并派发 `ScannerUiAction.DecodeImage(image)`。选择照片前先派发 `StartScanning`。`ScannerViewModel.decodeImage(image)` 创建新 owner 并取消旧任务，再把状态改为 `ProcessingImage`，在 owner scope 中调用注入的 `decode(image)`。

默认解码器 `BarcodeImageDecoder.decode(ref)` 以 `withContext(Dispatchers.IO)` 调用 `InputImage.fromFilePath(context, ref.value.toUri())`，之后通过 `suspendCancellableCoroutine` 创建 ML Kit client 并调用 `scanner.process(image)`。完成 listener 先关闭 client；若 continuation 已取消就丢弃结果，否则把首个非空值变为 `Decoded(rawValue)`，无值返回 `NoBarcode`，Task 失败返回 `Failed(error)`。回到 ViewModel 后先执行 `currentCoroutineContext().ensureActive()` 并比较 `work === owner`，再分类或发出图片失败 effect；这样旧图片的迟到回调不能更新新任务。

`BarcodeImageDecoder` 的 listener 注册边界当前以 `catch (Exception)` 处理同步异常；若第三方调用同步抛出 `CancellationException`，内层 catch 会将它包装成 `BarcodeImageResult.Failed`，偏离项目取消合同。该问题只记录在[扫码待办](../../superpowers/plans/2026-11-06-passly-scanner-review-followups.md)，本轮不改运行代码。测试应证明取消原样传播且 `scanner.close()` 仍执行。

## 生命周期与敏感状态

`collectScannerRouteEffects()` 在宿主 `STARTED` 时派发 `HostStarted` 并消费 effect，离开该状态派发 `HostStopped`。`ScannerViewModel.pauseForHost()` 轮换 owner：显示结果可保留，纯扫描和图片处理中状态转 `Stopped`；恢复后重新开始扫描。`StopScanning` 和 `onCleared()` 清除结果并取消 owner。复制文本同样在当前 owner scope 中运行，传给 `SensitiveClipboardWriter.writeSensitiveIf(text) { work === owner }` 的资格闭包会在平台写入前再次核对 owner。

一次性复制、图片提示和震动由 Route 消费；用户可见提示只传 effect 类型，异常文本不进入 Toast。相册选择返回后生命周期 collector 先恢复扫描，随后图片回调派发解码 action。CameraHost 内的原始回调不直接写库或持久化扫描内容。

任务 owner、迟到图片结果和宿主暂停行为由 [`ScannerViewModelTest`](../../../app/src/test/java/com/aozijx/passly/feature/scanner/presentation/ScannerViewModelTest.kt) 覆盖；Route 生命周期 effect 由 [`ScannerRouteEffectsTest`](../../../app/src/test/java/com/aozijx/passly/feature/scanner/presentation/ScannerRouteEffectsTest.kt) 覆盖。它们使用注入的 decode lambda 验证 ViewModel 编排，不调用真正的 ML Kit client；`BarcodeImageDecoder` 同步注册取消/错误与 scanner client 清理仍由扫码待办要求补专用测试。

## 代码位置

| 职责 | 类/函数 | 源码 |
| --- | --- | --- |
| CameraX 帧输入及资源释放 | `ScannerCameraHost()`、`ScannerCameraHost.stop()` | [`ScannerCameraHost.kt`](../../../app/src/main/java/com/aozijx/passly/feature/scanner/presentation/ScannerCameraHost.kt) |
| 图片输入解码 | `BarcodeImageDecoder.decode(ref)` | [`BarcodeImageDecoder.kt`](../../../app/src/main/java/com/aozijx/passly/feature/scanner/presentation/BarcodeImageDecoder.kt) |
| 任务 owner、状态与剪贴板 | `ScannerViewModel.onAction()`、`decodeImage(image)`、`completeScan(raw)` | [`ScannerViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/scanner/presentation/ScannerViewModel.kt) |
| 生命周期 effect 收集 | `collectScannerRouteEffects()`、`ScannerRoute()` | [`ScannerRouteEffects.kt`](../../../app/src/main/java/com/aozijx/passly/feature/scanner/presentation/ScannerRouteEffects.kt)、[`ScannerRoute.kt`](../../../app/src/main/java/com/aozijx/passly/feature/scanner/presentation/ScannerRoute.kt) |
