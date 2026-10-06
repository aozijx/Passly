# 二维码扫描

状态：当前实现。

Scanner 是应用内通用二维码入口，不绑定某一种密码类型。当前识别结果统一转换为
`ScannerResult`：有效 `otpauth://` 为动态验证码，带有效主机的 HTTP/HTTPS 地址为网页链接，
其余非空内容为普通文本。无法解析的 OTP 或网址不会丢失，而是降级为普通文本。

## 职责边界

- `ScannerCameraHost` 只负责相机权限、CameraX 预览、ML Kit 分析和资源释放，并向上报告原始值。
- `ScannerViewModel` 负责单次扫描去重、分类、停止分析、震动和安全剪贴板写入；重新扫描会清空结果并重置去重会话。
- `ScannerScreen` 只根据不可变状态渲染取景框、动画和结果卡，并通过 `ScannerScreenAction` 报告交互。
- `ScannerRoute` 负责相册选择、Toast、打开外部链接以及调用方指定的 OTP 确认动作。

相机权限被拒绝时 Scanner 直接退出，权限申请与 CameraX 生命周期不会泄漏到 Vault 或编辑器。
原始二维码和分类结果只保存在当前 Scanner 状态中：停止或退出时清空，不写入数据库、设置或日志，
也不经 Navigation 参数传递。

## 调用方语义

Vault 首页以 `ADD_TO_VAULT` 打开 Scanner，确认 OTP 后发送 `VaultUiAction.AddScannedOtp`；
OTP 编辑器以 `APPLY_TO_EDITOR` 打开，确认后把配置填入当前编辑状态。链接由系统处理，普通文本使用
安全剪贴板复制。调用方只提供 OTP 确认语义，不接管 Scanner 内部状态。

## 扩展规则

新增 Wi-Fi 等二维码类型时，应先为 `ScannerResult` 增加明确的数据类型和分类测试，再为结果卡和 Route
增加对应动作。不要在 CameraHost 中按字符串分支，也不要重新增加并行的字符串/类型状态、回调式
Composable 桥或持久化原始内容。

## 相关实现

- [`ScannerResult`](../../app/src/main/java/com/aozijx/passly/presentation/feature/scanner/ScannerResult.kt)
- [`ScannerResultClassifier`](../../app/src/main/java/com/aozijx/passly/presentation/feature/scanner/ScannerResultClassifier.kt)
- [`ScannerRoute`](../../app/src/main/java/com/aozijx/passly/presentation/feature/scanner/ScannerRoute.kt)
- [`ScannerCameraHost`](../../app/src/main/java/com/aozijx/passly/presentation/feature/scanner/ScannerCameraHost.kt)
