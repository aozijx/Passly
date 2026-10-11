# 二维码扫描

状态：当前实现。

Scanner 是应用内通用二维码入口，不绑定某一种密码类型。当前识别结果统一转换为
`ScannerResult`：有效 `otpauth://` 为动态验证码，受支持的 `WIFI:` 内容为 Wi-Fi 凭据，带有效主机的
HTTP/HTTPS 地址为网页链接，其余非空内容为普通文本。无法解析或不受支持的专用格式不会静默生成条目。

## 职责边界

- `ScannerCameraHost` 只负责相机权限、CameraX 预览、ML Kit 分析和资源释放，并向上报告原始值。
- `ScannerViewModel` 负责单次扫描去重、分类、停止分析、震动和安全剪贴板写入；状态以
  `Scanning / ProcessingImage / ShowingResult / Stopped` 明确表达，重新扫描会清空结果并重置去重会话。
- `ScannerScreen` 只根据不可变状态渲染取景框、动画和结果卡，并通过 `ScannerScreenAction` 报告交互。
- `ScannerRoute` 负责相册选择、Toast、打开外部链接和类型化导入确认；`ScannerPurpose` 明确限制当前
  调用方能够接收的结果类型。

相机权限被拒绝时 Scanner 直接退出，权限申请与 CameraX 生命周期不会泄漏到 Vault 或编辑器。
原始二维码和分类结果只保存在当前 Scanner 状态中：停止或退出时清空，不写入数据库、设置或日志，
也不经 Navigation 参数传递。

## 调用方语义

Vault 首页以 `General` 打开 Scanner：OTP 可直接交给 Vault 创建链路，Wi-Fi 必须先进入编辑页确认；
OTP 与 Wi-Fi 编辑器分别使用 `ApplyToOtpEditor`、`ApplyToWifiEditor`，只接收匹配的类型。链接由系统处理，
普通文本使用安全剪贴板复制。调用方只处理 `ScannerImport`，不接管 Scanner 内部状态。

首页 Wi-Fi 结果不会把 SSID 或密码写入导航参数。`EntryEditorSeedCoordinator` 在 Vault 图作用域内保存一份
只可消费一次的临时内容，导航只携带随机 token；锁定、恢复模式、消费完成或作用域销毁都会清空该内容。
Wi-Fi 结果卡和无障碍文本不显示密码，编辑页可重新扫码，只替换 Wi-Fi 专属字段。

## 扩展规则

新增二维码类型时，应先为 `ScannerResult` 增加明确的数据类型和分类测试，再为结果卡、`ScannerPurpose`
和 `ScannerImport` 增加穷尽分支。状态阶段和解析失败必须使用该流程自己的 sealed 类型，不能用 nullable、
未定义枚举值或互相组合的布尔值代替。不要在 CameraHost 中按字符串分支，也不要重新增加并行的字符串/类型状态、回调式
Composable 桥或持久化原始内容。

## 相关实现

- [`ScannerResult`](../../app/src/main/java/com/aozijx/passly/presentation/feature/scanner/ScannerResult.kt)
- [`ScannerResultClassifier`](../../app/src/main/java/com/aozijx/passly/presentation/feature/scanner/ScannerResultClassifier.kt)
- [`ScannerRoute`](../../app/src/main/java/com/aozijx/passly/presentation/feature/scanner/ScannerRoute.kt)
- [`ScannerCameraHost`](../../app/src/main/java/com/aozijx/passly/presentation/feature/scanner/ScannerCameraHost.kt)
