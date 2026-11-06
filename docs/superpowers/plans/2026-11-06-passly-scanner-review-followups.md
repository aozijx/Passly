# Passly Scanner 待办

**状态：** 待修复与验证。本页记录图片解码器取消/资源清理边界及实时多条码候选筛选问题；当前实现描述见 `docs/features/scanner/`。

## Task 1：图片解码器保留取消语义并关闭 ML Kit 客户端

- [ ] 在 `BarcodeImageDecoder.decode(ref)` 创建 ML Kit scanner 后的 listener 注册边界，先单独传播 `CancellationException`，再处理普通 `Exception`；取消不能被包装成 `BarcodeImageResult.Failed` 并显示“解码失败”。
- [ ] 确保 `scanner.process(image)` / `addOnCompleteListener(...)` 同步抛出普通异常、取消或 fatal `Error` 时，scanner client 都在窄 `finally` 清理边界关闭；清理后继续传播取消或 fatal `Error`，普通异常才映射为 `Failed`。
- [ ] 为成功 Task、Task 失败、listener 注册普通异常、listener 注册取消、协程取消后晚到回调、fatal `Error` 加测试；断言 client 恰好关闭且旧 continuation 不读取或发布扫码文本。
- [ ] 更新[扫描输入与生命周期](../../features/scanner/input-lifecycle.md)中的失败/取消合同；保留 `ScannerViewModel` owner 检查和 `BarcodeImageDecoder` 的 IO 线程边界。

**当前证据（2026-11-06）：** `BarcodeImageDecoder.decode(ref)` 在 `withContext(Dispatchers.IO)` 创建 `InputImage`，随后 `suspendCancellableCoroutine` 中创建 scanner，并在 `try` 里调用 `scanner.process(image).addOnCompleteListener(...)`。该注册块只捕获 `Exception`，调用 `scanner.close()` 后对仍 active 的 continuation `resume(BarcodeImageResult.Failed(error))`；若捕获值是 `CancellationException`，会作为普通失败返回。`scanner.close()` 当前由普通异常 catch 和 complete listener 负责，若注册 API 直接抛 fatal `Error` 则绕过两处清理。现有测试有 `ScannerViewModelTest` 和 Route/结果解析测试，但未发现 `BarcodeImageDecoder` 专用测试或注入的 ML Kit 客户端 seam；以上是静态实现合同问题，尚未以设备或模拟 Task 复现。

**验收：** 取消不被转为用户可见失败；每条已创建 scanner 路径恰好关闭一次；fatal `Error` 在清理后传播；图片结果 owner 校验与迟到回调丢弃行为保持通过。

## Task 2：继续扫描视框内第一个可解码条码

- [ ] 调整相机 success listener 的候选选择，使 `rawValue` 非空且非 blank 的检查与 `isInsideViewfinder(...)` 一起应用于每个候选；不能先选一个视框内但 `rawValue == null` 的条码，再因安全调用返回 null 而跳过其后的有效候选。
- [ ] 增加可确定性测试覆盖候选顺序：视框内无 raw value 后接视框内有效值、视框外有效值后接视框内有效值、blank 值后接有效值，以及没有有效候选；断言只把首个有效视框内字符串送入 `currentOnBarcodeDetected`。
- [ ] 保留 `isInsideViewfinder()` 对缺失 bounding box/output transform、无效尺寸和未布局 PreviewView 的拒绝行为；单独验证它当前以映射检测框中心落在方形视框内作为准入规则，并裁定是否应改成检测框与视框相交。
- [ ] 同步[扫描输入与生命周期](../../features/scanner/input-lifecycle.md)，记录已确认的筛选顺序、几何边界和验证范围。

**当前证据（2026-11-06）：** `ScannerCameraHost` 的成功回调先执行 `barcodes.firstOrNull { barcode.isInsideViewfinder(...) }`，只在此后读取可空的 `?.rawValue` 并调用 `currentOnBarcodeDetected(value)`。因此首个视框内但未解码出 raw value 的候选会阻止遍历后续有效候选。图片解码器已用 `barcode.rawValue?.takeIf(String::isNotBlank)` 在候选选择期间过滤空白值；相机路径没有相同过滤。当前搜索未发现对实时候选筛选或 `isInsideViewfinder()` 几何边界的专用测试。另，Viewfinder 计划和实现当前采用中心点包含规则，框边右/下侧依 `RectF.contains` 的半开边界不算命中；是否接受此规则仍待明确验收。

**验收：** 只接受映射后中心位于当前视框内且 raw value 非空、非 blank 的首个候选；后续候选不再被无效首项遮蔽；缺少变换时继续拒绝检测；自动化覆盖候选顺序和矩形边界。
