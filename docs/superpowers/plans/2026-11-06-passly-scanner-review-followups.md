# Passly Scanner 待办

**状态：** 部分待验证。本页记录图片解码器取消/资源清理边界，以及扫码视框几何的自动化/设备验证；当前实现描述见 `docs/features/scanner/`。

## Task 1：图片解码器保留取消语义并关闭 ML Kit 客户端

- [ ] 在 `BarcodeImageDecoder.decode(ref)` 创建 ML Kit scanner 后的 listener 注册边界，先单独传播 `CancellationException`，再处理普通 `Exception`；取消不能被包装成 `BarcodeImageResult.Failed` 并显示“解码失败”。
- [ ] 确保 `scanner.process(image)` / `addOnCompleteListener(...)` 同步抛出普通异常、取消或 fatal `Error` 时，scanner client 都在窄 `finally` 清理边界关闭；清理后继续传播取消或 fatal `Error`，普通异常才映射为 `Failed`。
- [ ] 为成功 Task、Task 失败、listener 注册普通异常、listener 注册取消、协程取消后晚到回调、fatal `Error` 加测试；断言 client 恰好关闭且旧 continuation 不读取或发布扫码文本。
- [ ] 更新[扫描输入与生命周期](../../features/scanner/input-lifecycle.md)中的失败/取消合同；保留 `ScannerViewModel` owner 检查和 `BarcodeImageDecoder` 的 IO 线程边界。

**当前证据（2026-11-06）：** `BarcodeImageDecoder.decode(ref)` 在 `withContext(Dispatchers.IO)` 创建 `InputImage`，随后 `suspendCancellableCoroutine` 中创建 scanner，并在 `try` 里调用 `scanner.process(image).addOnCompleteListener(...)`。该注册块只捕获 `Exception`，调用 `scanner.close()` 后对仍 active 的 continuation `resume(BarcodeImageResult.Failed(error))`；若捕获值是 `CancellationException`，会作为普通失败返回。`scanner.close()` 当前由普通异常 catch 和 complete listener 负责，若注册 API 直接抛 fatal `Error` 则绕过两处清理。现有测试有 `ScannerViewModelTest` 和 Route/结果解析测试，但未发现 `BarcodeImageDecoder` 专用测试或注入的 ML Kit 客户端 seam；以上是静态实现合同问题，尚未以设备或模拟 Task 复现。

**验收：** 取消不被转为用户可见失败；每条已创建 scanner 路径恰好关闭一次；fatal `Error` 在清理后传播；图片结果 owner 校验与迟到回调丢弃行为保持通过。

## Task 2：继续扫描视框内第一个可解码条码

- [ ] 增加可确定性测试覆盖候选顺序：视框内无 raw value 后接视框内有效值、视框外有效值后接视框内有效值、blank 值后接有效值，以及没有有效候选；断言只把首个有效视框内字符串送入 `currentOnBarcodeDetected`。

**当前证据（2026-11-06）：** `ScannerCameraHost` 对每帧先创建 `ScannerRecognitionRegion`：使用共享 ViewPort 下的 `cropRect`、旋转角和预览尺寸构造图像到预览矩阵，再反向映射取景框一次。矩阵或布局不可用时关闭并跳过当前 `ImageProxy`，不调用 ML Kit；有区域后先筛除空白 `rawValue`，再检查条码框四角，避免逐候选重复变换。右/下边缘按 `RectF.contains` 半开语义处理。仍缺少候选序列与几何边界的自动化覆盖，以及真实设备上的方向/裁剪验证。

**验收：** 只接受映射后检测框四角全部落入当前视框且 raw value 非空、非 blank 的首个候选；后续候选不再被无效首项遮蔽；缺少变换时继续拒绝检测；自动化覆盖候选顺序和矩形边界。旧的“中心点落入即可”条件已被当前四角全包含规则取代。

## Task 3：为扫码结果 owner 回调定义异常反馈边界

- [ ] 明确 `ScannerRoute.handleScreenAction(ConfirmResult)` 中 `rememberCoroutineScope().launch { resultSession.confirm { onResult(result.rawValue) } }` 遇到普通异常时的 owner 与用户反馈。结果回调的普通失败应成为当前页面可见、可重试的结果，而不是仅依赖 `ScannerResultSession.confirm()` 的 `finally` 恢复 `Ready` 后让异常逸出 coroutine handler；保留 `CancellationException` 传播和 fatal `Error` 传播。
- [ ] 在真实调用闭包验证结果：Vault OTP 分支将 `OtpAuthUriCodec.parse(rawValue)` 的 Parsed config 传给 `ScannedOtpImportViewModel.importScannedOtp(config)`，并由其等待 `ImportScannedOtpUseCase.invoke(config)`；Wi-Fi 分支把 `WifiQrPayloadParser.parse(rawValue)` 的 payload 放入 `VaultDestinationRequest.EditScannedWifi(payload)`，经 `VaultNavigation.navigate(request)` → `seedStager.stage(EntryEditorSeed.Wifi(request.payload))` → `navigateToSingleTopRoute(AppRoute.AddWifiFromSeed.createRoute(staged.token.value))`；外链分支的 `openScannedWebLink(uri)` 已把普通 `Exception` 转为 `false`。统一错误合同不得把不同 parser/import/navigation owner 的异常都伪装成格式拒绝，也不能在取消/退出后重放过期回调。
- [ ] 在 Wi-Fi 导航回调抛错/取消时同时清理对应 seed token：当前 staging 已先写入 graph-scoped 单槽 Store，之后才调用 `navigateToSingleTopRoute(...)`；导航抛错时 Store 保留 `WifiQrPayload`，且 `VaultNavigation` 依赖的 `EntryEditorSeedStager` 没有 discard 能力。与[维护 TODO Task 42](2026-10-30-passly-maintainability-todo.md)共享 seed stage/navigation 失败合同，不能以关闭 Scanner 代替清理 seed，也不能清掉被其他 token 替换的新 seed。
- [ ] 覆盖 callback 普通异常后的结果卡状态、用户提示与重试、回调恰好执行次数、期间的第二次点击、关闭/取消竞争和敏感扫码文本不进入日志/Toast；取消与 fatal `Error` 应继续传播。至少覆盖可能抛错的 Wi-Fi stage/navigation 调用，不只测 `ScannerResultSession` 的 `finally`。
- [ ] 更新[扫描结果交接](../../features/scanner/import-results.md)和本待办证据；在 Route/owner 行为覆盖前，不宣称 callback 异常已可靠映射。

**当前证据（2026-11-06）：** `ScannerRoute` 以 `rememberCoroutineScope()` 创建确认 coroutine，直接执行 `resultSession.confirm { onResult(result.rawValue) }`，仅对正常返回的 `Handled`/`Rejected`/`Ignored` 分支作处理，没有 `catch`。`ScannerResultSession.confirm(handle)` 在 `finally` 中仅于当前 phase 仍为 `Handling` 时复位为 `Ready`；它不捕获或映射 callback 异常，因此普通异常会继续逸出 `launch`，没有 Snackbar/effect 反馈。当前 Vault OTP owner 的 `ImportScannedOtpUseCase.invoke(config)` 捕获普通创建异常并返回 false；Wi-Fi owner 的 `VaultNavigation.navigate(EditScannedWifi(payload))` 依次调用 `EntryEditorSeedStager.stage(EntryEditorSeed.Wifi(payload))` 与 `navigateToSingleTopRoute(...)`，不捕获这两步抛出的异常；而 `stage()` 已把 payload 以新 token 写入单槽 `EntryEditorSeedStore`，导航异常时没有 token-aware discard，敏感 payload 会继续由 graph coordinator/store 持有。HTTP helper `openScannedWebLink(uri)` 已单独 catch 普通 `Exception` 并返回 false。`ScannerResultSessionTest` 覆盖正常 Handled/Rejected、重复确认、关闭晚到回执和取消后复用，但没有 callback 普通异常或 Route/Navigation 异常行为覆盖。此为调用图与异常边界的静态缺口，尚未以 Route 行为测试或设备复现；当前只记录 TODO，未改生产逻辑。共享 seed 释放合同见[维护 TODO Task 42](2026-10-30-passly-maintainability-todo.md)。
