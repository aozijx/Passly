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

- [x] 调整相机 success listener 的候选选择，在每个候选上先筛掉空白 `rawValue`，再检查 `ScannerRecognitionRegion.contains(barcode)`；无效候选不会遮蔽其后的有效候选。
- [ ] 增加可确定性测试覆盖候选顺序：视框内无 raw value 后接视框内有效值、视框外有效值后接视框内有效值、blank 值后接有效值，以及没有有效候选；断言只把首个有效视框内字符串送入 `currentOnBarcodeDetected`。
- [x] 将缺失 PreviewView 尺寸、无效旋转/矩阵逆变换处理为跳过当前帧；准入规则改为条码检测框四角全部落入视框。
- [x] 同步[扫描输入与生命周期](../../features/scanner/input-lifecycle.md)，记录筛选顺序、几何边界和仍待进行的设备/自动化验证。

**当前证据（2026-11-06）：** `ScannerCameraHost` 对每帧先创建 `ScannerRecognitionRegion`：使用共享 ViewPort 下的 `cropRect`、旋转角和预览尺寸构造图像到预览矩阵，再反向映射取景框一次。矩阵或布局不可用时关闭并跳过当前 `ImageProxy`，不调用 ML Kit；有区域后先筛除空白 `rawValue`，再检查条码框四角，避免逐候选重复变换。右/下边缘按 `RectF.contains` 半开语义处理。仍缺少候选序列与几何边界的自动化覆盖，以及真实设备上的方向/裁剪验证。

**验收：** 只接受映射后检测框四角全部落入当前视框且 raw value 非空、非 blank 的首个候选；后续候选不再被无效首项遮蔽；缺少变换时继续拒绝检测；自动化覆盖候选顺序和矩形边界。旧的“中心点落入即可”条件已被当前四角全包含规则取代。

## Task 3：扫码结果只交付原始数据

- [x] 将 `ScannerResult` 收敛到原始文本；`ScannerRoute` 通过一次性回执将文本交给当前 owner。
- [x] Vault、OTP 编辑器、Wi-Fi 编辑器在各自入口使用 Domain parser / 业务入口；链接处理归 Vault。删除 Scanner 内按业务类型分类、策略映射的代码和不再适用的测试。
- [x] 结果卡统一展示原始文本和通用动作，清除按类型维护的标题、CTA、分类映射与旧中英文资源；同步 Scanner 主题文档。
- [x] 执行 `:app:compileDebugKotlin` 和 `git diff --check`；设备/单测验证另按剩余任务安排。不要为旧类型合同保留双轨桥。

**当前工作区证据（2026-11-06）：** `ScannerViewModel.completeScan(raw)` 只拒绝空白内容并保存 `ScannerResult(rawValue)`；`ScannerRoute.onResult(rawValue)` 保留一次确认回执，`Handled` 关闭、`Rejected` 保留卡片。业务 owner 从原始字符串分别解析 OTP、Wi-Fi、HTTP/HTTPS。图片回调在宿主短暂仍为 `Stopped` 时会先恢复状态再启动整图解析；相机取景框准入检查条码四角。`:app:compileDebugKotlin` 与 `git diff --check` 已通过；测试未运行，设备上的图片识别与相机裁剪效果仍待验证。
