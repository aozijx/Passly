# Scanner 相机与结果确认 owner 审查

**范围：** 复核 Task 84 中的 `ScannerCameraHost` 和 `ScannerResultSession`，同时检查 App 并发计划里相机资源生命周期条目的当前状态。结论关闭 Task 84 的 owner 分类候选；相机 host 的实际生命周期行为由并发计划跟踪，Scanner 与各业务 owner 的 handoff 组合验收由[Scanner 行为与设备计划](../work/plans/2026-11-06-scanner-entry-and-framing.md)跟踪，合同留在产品文档。

## ScannerCameraHost

`ScannerRoute` 是平台和 Compose 生命周期边界：它投影相机状态、取景框、权限与识别 callback；`ScannerCameraHost` 组合相机权限 host、PreviewView、ML Kit scanner、单线程分析 executor 和 `ScannerCameraSession`。资源都由 Composition owner 创建，不进入 `ScannerViewModel` 或 Domain。

Camera `DisposableEffect` 以 lifecycle owner、权限状态和相机状态为 key；每次重开先置旧闭包的 `disposed` 标志并停止旧 session，再由当前许可的状态注册 provider listener/bind。异步 provider listener 在 bind 前检查该标志，session bind 前也先停止已有 use case；`ScannerCameraSession.stop()` 清 analyzer、移除布局 listener，并仅 unbind 此 session 自己的 Preview/ImageAnalysis。组件 dispose 再停止 session、shutdown executor、关闭 ML Kit scanner。每帧由 completion listener 关闭 `ImageProxy`，同步失败时也关闭，`AtomicBoolean` 防止多次关闭。

没有发现可移入 ViewModel、Route 以外或通用 Camera owner 的重复状态；`ScannerCameraHost` 与 session 分别持有组合资源和已绑定 use case，边界有效。当前测试源没有 `ScannerCameraHost`、`ScannerCameraSession` 或 executor/ML Kit teardown 专项覆盖，所以以上是源码合同，不等同于真实重组、权限变化、执行中释放或设备行为验证。App 并发计划保留一项具体的 host lifecycle 验证。

## ScannerResultSession

Route 创建并拥有一次 `ScannerResultSession`；它只持有 `Ready / Handling / Handled / Closed` 确认阶段，不存储扫码 payload。确认开始时占用单次操作；普通拒绝/失败会返回 `Ready` 供卡片重试，成功切到 `Handled`，并发确认被忽略。Route 关闭 session 后，迟到的成功/失败回执忽略；取消继续传播，fatal `Error` 不被转换为普通失败，`finally` 只恢复仍处于 `Handling` 的阶段。

该状态与 `ScannerViewModel` 的扫描任务、结果值、反馈各有独立真相源：确认闭包来自当前 Route owner 解析出的 `ScannerResultPlan`，不同 Vault/OTP/Wi-Fi 调用方共享同一确认合同；将阶段挪入 ViewModel 会让通用 Route 状态依赖某个业务 caller。保留该 Session。现有 `ScannerResultSessionTest` 覆盖单飞、拒绝后重试、关闭后的迟到确认和取消后重试；`ScannedOtpImportViewModelTest` 另覆盖 scanner 重建后由导入 ViewModel 拒绝重复写入。结果解析、确认和待验证边界见[输入生命周期](../../product/scanner/input-lifecycle.md)与[结果交接](../../product/scanner/result-handoff.md)。

## 代码位置

| 职责 | 类/函数 | 源码或测试 |
| --- | --- | --- |
| 组合权限、CameraX、ML Kit 与 Composition 资源 | `ScannerCameraHost()`、`bindScannerCamera()` | [`ScannerCameraHost.kt`](../../../app/src/main/java/com/aozijx/passly/feature/scanner/presentation/camera/ScannerCameraHost.kt) |
| 绑定/停止本 host 持有的 Preview 和 ImageAnalysis | `ScannerCameraSession.bind()`、`stop()` | [`ScannerCameraSession.kt`](../../../app/src/main/java/com/aozijx/passly/feature/scanner/presentation/camera/ScannerCameraSession.kt) |
| 持有 Route 生命周期和 camera/result 两个 owner | `ScannerRoute()` | [`ScannerRoute.kt`](../../../app/src/main/java/com/aozijx/passly/feature/scanner/presentation/route/ScannerRoute.kt) |
| 防止并发确认并忽略关闭后的回执 | `ScannerResultSession.confirm()`、`close()` | [`ScannerResultSession.kt`](../../../app/src/main/java/com/aozijx/passly/feature/scanner/presentation/result/ScannerResultSession.kt)、[`ScannerResultSessionTest.kt`](../../../app/src/test/java/com/aozijx/passly/feature/scanner/presentation/result/ScannerResultSessionTest.kt) |
| 认证期间拒绝 Scanner 重建导致的重复 OTP 写入 | `ScannedOtpImportViewModel.importScannedOtp()` | [`ScannedOtpImportViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/ScannedOtpImportViewModel.kt)、[`ScannedOtpImportViewModelTest.kt`](../../../app/src/test/java/com/aozijx/passly/feature/vault/presentation/ScannedOtpImportViewModelTest.kt) |
