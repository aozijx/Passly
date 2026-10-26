# 二维码扫描

状态：当前实现。

Scanner 是应用内通用二维码入口，不绑定某一种密码类型。当前识别结果统一转换为
`ScannerResult`：有效 `otpauth://` 为动态验证码，受支持的 `WIFI:` 内容为 Wi-Fi 凭据，带有效主机的
HTTP/HTTPS 地址为网页链接，其余非空内容为普通文本。无法解析或不受支持的专用格式不会静默生成条目。

OTP 分类直接调用 Domain 的类型化 URI 解析器，与手动输入和导出共用规则；无效算法或缺失 HOTP 计数器
不会被补成另一份有效配置，具体边界见[动态口令 URI 导入与导出](vault/otp-uri.md)。

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

导入回调为挂起调用：首页 OTP 经 `ImportScannedOtpUseCase` 转换、校验并等待仓库保存成功后，
才返回 `Applied` 并关闭 Scanner；拒绝、无效配置或写入失败返回 `Rejected`，保留结果供重试。
编辑器的 `Applied` 表示内容已被表单接受，Wi-Fi 首页的 `Applied` 表示已进入编辑页，不表示已保存。
OTP 与 Wi-Fi 编辑器都通过带接受回执的 ViewModel 方法应用扫码内容；锁定、保存中或销毁后返回
`Rejected`，不改写表单，也不因一次被忽略的 action 而关闭扫码结果。
`ScannerImportSession` 在确认期间拦截重复确认、重扫与相册切换；退出会取消 Route 的确认协程，
旧结果的迟到回执不再关闭页面或弹提示。取消不代表已完成的数据库写入会回滚。

首页 Wi-Fi 结果不会把 SSID 或密码写入导航参数。`EntryEditorSeedCoordinator` 在 Vault 图作用域内保存一份
只可消费一次的临时内容，导航只携带随机 token；内容绑定暂存时的认证对象，读取和暂存都会核对所有权，
即使未观察到中间锁定状态，重新认证后也不能消费旧内容。锁定、恢复模式、消费完成或作用域销毁都会清空内容，
销毁后的协调器拒绝继续暂存或消费。
Wi-Fi 结果卡和无障碍文本不显示密码，编辑页可重新扫码，只替换 Wi-Fi 专属字段。

导航向 Wi-Fi Route 提供按需读取入口，不在 `remember` 中保留已消费的预填载荷。`AddWifiViewModel.initialize`
只在首次初始化且允许编辑时调用读取入口，将内容直接写入表单；重复初始化不再次消费，拒绝后也不在解锁时重放。
初始化结果为 `Ready / Unavailable / AlreadyInitialized`，页面仅凭结果显示种子不可用提示。

## Wi-Fi 解析与编辑边界

Domain 的 `WifiQrPayloadParser` 以纯 Kotlin 解析 `WIFI:` 载荷并正确处理反斜杠转义；SSID 是必需字段。支持开放网络、
WEP，以及 WPA/WPA2/WPA3/SAE Personal。企业认证字段、未知安全类型、缺失 SSID 或损坏转义不会生成半成品
凭据，而是保留原始内容并降级为普通文本。

Domain 只接受 `WifiSecurityType.Open / Wep / WpaPersonal`。数据库与备份边界使用稳定的 `nopass`、`WEP`、
`WPA` 字符串，并通过 `Supported / Missing / Unsupported` 解析结果穷尽处理；不得使用 `valueOf()`、nullable
或默认 WPA 掩盖未知存储值。开放网络保存前必须清空密码，WEP 和 WPA 密码由同一 Domain 校验器约束，手动
添加与扫码预填共用同一 `EntryDraft` 创建链路。

首页扫码不会直接保存 Wi-Fi。用户确认后通过一次性 seed token 进入独立 Wi-Fi 编辑页；seed 只存在于 Vault
图作用域内存，消费、锁定或作用域销毁后清除。编辑页重新扫码只替换 SSID、安全类型、密码和隐藏网络，
密码显示状态重置为隐藏，保留备注与标签。创建的条目标题使用当前 SSID，保留网络名称首尾空格。

## 扩展规则

已保存的 Wi-Fi 条目可以在详情页显示连接二维码，生成、授权与清理边界见[条目二维码展示](vault/qr-display.md)。

新增二维码类型时，应先为 `ScannerResult` 增加明确的数据类型和分类测试，再为结果卡、`ScannerPurpose`
和 `ScannerImport` 增加穷尽分支。状态阶段和解析失败必须使用该流程自己的 sealed 类型，不能用 nullable、
未定义枚举值或互相组合的布尔值代替。不要在 CameraHost 中按字符串分支，也不要重新增加并行的字符串/类型状态、回调式
Composable 桥或持久化原始内容。

## 相关实现

- [`ScannerResult`](../../app/src/main/java/com/aozijx/passly/feature/scanner/presentation/ScannerResult.kt)
- [`ScannerResultClassifier`](../../app/src/main/java/com/aozijx/passly/feature/scanner/presentation/ScannerResultClassifier.kt)
- [`ScannerRoute`](../../app/src/main/java/com/aozijx/passly/feature/scanner/presentation/ScannerRoute.kt)
- [`ScannerCameraHost`](../../app/src/main/java/com/aozijx/passly/feature/scanner/presentation/ScannerCameraHost.kt)
