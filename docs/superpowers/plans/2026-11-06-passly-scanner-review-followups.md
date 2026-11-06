# Passly Scanner 待办

**状态：** 待修复与验证。本页只记录 2026-11-06 扫描到的解码器取消/资源清理边界；当前实现描述见 `docs/features/scanner/`。

## Task 1：图片解码器保留取消语义并关闭 ML Kit 客户端

- [ ] 在 `BarcodeImageDecoder.decode(ref)` 创建 ML Kit scanner 后的 listener 注册边界，先单独传播 `CancellationException`，再处理普通 `Exception`；取消不能被包装成 `BarcodeImageResult.Failed` 并显示“解码失败”。
- [ ] 确保 `scanner.process(image)` / `addOnCompleteListener(...)` 同步抛出普通异常、取消或 fatal `Error` 时，scanner client 都在窄 `finally` 清理边界关闭；清理后继续传播取消或 fatal `Error`，普通异常才映射为 `Failed`。
- [ ] 为成功 Task、Task 失败、listener 注册普通异常、listener 注册取消、协程取消后晚到回调、fatal `Error` 加测试；断言 client 恰好关闭且旧 continuation 不读取或发布扫码文本。
- [ ] 更新[扫描输入与生命周期](../../features/scanner/input-lifecycle.md)中的失败/取消合同；保留 `ScannerViewModel` owner 检查和 `BarcodeImageDecoder` 的 IO 线程边界。

**当前证据（2026-11-06）：** `BarcodeImageDecoder.decode(ref)` 在 `withContext(Dispatchers.IO)` 创建 `InputImage`，随后 `suspendCancellableCoroutine` 中创建 scanner，并在 `try` 里调用 `scanner.process(image).addOnCompleteListener(...)`。该注册块只捕获 `Exception`，调用 `scanner.close()` 后对仍 active 的 continuation `resume(BarcodeImageResult.Failed(error))`；若捕获值是 `CancellationException`，会作为普通失败返回。`scanner.close()` 当前由普通异常 catch 和 complete listener 负责，若注册 API 直接抛 fatal `Error` 则绕过两处清理。现有测试有 `ScannerViewModelTest` 和 Route/结果解析测试，但未发现 `BarcodeImageDecoder` 专用测试或注入的 ML Kit 客户端 seam；以上是静态实现合同问题，尚未以设备或模拟 Task 复现。

**验收：** 取消不被转为用户可见失败；每条已创建 scanner 路径恰好关闭一次；fatal `Error` 在清理后传播；图片结果 owner 校验与迟到回调丢弃行为保持通过。
