# 扫描结果导入与调用方

状态：当前实现（代码核对：2026-11-06）。

Scanner 不直接保存识别内容。`ScannerPurpose.resolveImport(result)` 将结果与调用方能力相交，Route 再以 `ScannerImportSession.confirm(import)` 进行一次性确认。确认结果的语义由调用方决定：`Applied` 才关闭 Scanner；`Rejected` 保留结果供重试；重复确认、关闭后的回调返回 `Ignored`。

## OTP 与调用方入口

Vault 列表用 `ScannerPurpose.General`：OTP 交给 Activity 生命周期的 `ScannedOtpImportViewModel.importScannedOtp(config)`，Wi-Fi 进入独立编辑页；OTP 编辑器和 Wi-Fi 编辑器分别传 `ApplyToOtpEditor`、`ApplyToWifiEditor`，只接受匹配的结果。网页链接交系统，普通文本由 Scanner 复制。`resolveImport` 对 purpose 和 sealed `ScannerResult` 穷尽分支，链接与文本不会生成可导入对象。

Vault 首页 OTP 的调用链为 `ScannerRoute.onImport(ScannerImport.Otp)` → `ScannedOtpImportViewModel.importScannedOtp(config)` → `ImportScannedOtpUseCase.invoke(config)` → `config.toNewEntryDraft()` → `EntryCreation(draft)`。前两层在保存前验证完整访问与认证会话身份；Activity ViewModel 用 `Mutex.tryLock()` 拒绝并行导入，已确认任务由 Activity ViewModel scope 持有。Use case 只在创建结果成功时返回 `true`；输入无效或持久化普通异常返回失败，取消传播。页面等待仍有效时，成功映射为 `ScannerImportHandlingResult.Applied`，失败映射为 `Rejected`。

OTP 编辑器将 `ScannerImport.Otp.config` 传给 `AddOtpViewModel.tryApplyScannedConfig(config)`；Wi-Fi 编辑器将 `ScannerImport.Wifi.payload` 传给 `AddWifiViewModel.tryApplyScannedWifi(payload)`。两个方法返回 `Boolean` 决定 `Applied/Rejected`：锁定、保存中或页面销毁后拒绝，不得关闭结果或覆盖现有表单。

`ScannerImportSession.confirm(import)` 在 UI dispatcher 上只接受 `Ready` 阶段，将阶段设为 `Importing`，调用挂起闭包后检查协程仍 active，再确认 session 未 `Closed`，最后映射回执。`finally` 只在仍为 `Importing` 时还原 `Ready`；`close()` 使迟到的回执返回 `Ignored`。Session 本身不保存扫码 payload。

## Wi-Fi 解析和安全传递

`ScannerResultClassifier.classify(rawValue)` 调 `WifiQrPayloadParser.parse(rawValue)`。Parser 以忽略大小写方式识别 `WIFI:` 前缀，解析转义字段，要求非空 SSID；企业认证键与未知安全类型返回 `Unsupported`，缺字段或坏转义返回 `Malformed`。开放网络无论 payload 是否含密码都产出空密码；WEP/WPA personal 保留密码。仅 `Parsed(WifiQrPayload)` 成为 `ScannerResult.Wifi`，不支持/损坏的专用格式按普通文本显示。

首页导航把 `WifiQrPayload` 交给 Vault 导航协调器暂存，route 参数只携带一次性随机 token。`EntryEditorSeedCoordinator` / Store 将 seed 绑定当前认证对象，消费和暂存均核对所有权；锁定、消费或图作用域销毁会清除敏感内容。Wi-Fi 编辑 Route 取得初始化资格后才消费 token，并将 `AddWifiInitialContent` 交给 `AddWifiViewModel`；重复 Route 组合不会再次消费。锁定时返回 `Unavailable`，不在稍后解锁时重放旧 seed。

Wi-Fi 编辑器重新扫码只替换 SSID、安全类型、密码和隐藏网络，密码显示态重置为隐藏，备注与标签保留；不直接保存。创建和手动编辑共用 `EntryDraft` 写入链路，开放网络保存前清空密码，WEP/WPA 密码沿用 Domain 校验。条目标题使用当前 SSID，名称首尾空格保留。图片和首页扫描中的原始 SSID/密码不进入 Navigation 参数、无障碍结果卡或日志。

已保存 Wi-Fi 条目的二维码展示属于 Vault 详情流程，见[条目二维码展示](../vault/qr-display.md)。OTP URI 的参数规则见[OTP URI 导入与导出](../vault/otp-uri.md)。

## 扩展规则

新增扫描类型时，为 `ScannerResult` 增加明确的数据类型和分类测试，再为结果卡、`ScannerPurpose` 与 `ScannerImport` 添加穷尽分支。阶段与失败原因继续用 sealed 类型；不要以 nullable、未定义 enum 值、并列布尔值、CameraHost 字符串分支、回调式 Composable bridge 或持久化原始内容扩展流程。

`ScannerPurposeTest` 验证通用/编辑器 purpose 的导入权限，`ScannerImportSessionTest` 验证确认防重入及回执，`WifiQrPayloadParserTest` 验证转义、安全类型与损坏输入；[`ScannedOtpImportViewModelTest`](../../../app/src/test/java/com/aozijx/passly/feature/vault/presentation/ScannedOtpImportViewModelTest.kt) 覆盖首页 OTP 确认后离页仍写入、撤权时序与防并发。编辑器回执另由 OTP/Wi-Fi ViewModel 测试覆盖；这些测试分层证明 parser、purpose、导入 owner 和表单应用，不应合并成单一 Scanner reducer 测试。

## 代码位置

| 职责 | 类/函数 | 源码 |
| --- | --- | --- |
| Purpose 与结果可用性 | `ScannerPurpose.resolveImport(result)` | [`ScannerPurpose.kt`](../../../app/src/main/java/com/aozijx/passly/feature/scanner/presentation/ScannerPurpose.kt) |
| 防重入确认及回执 | `ScannerImportSession.confirm(import)`、`close()` | [`ScannerImportSession.kt`](../../../app/src/main/java/com/aozijx/passly/feature/scanner/presentation/ScannerImportSession.kt) |
| Vault OTP 创建导入 | `ScannedOtpImportViewModel.importScannedOtp(config)`、`ImportScannedOtpUseCase.invoke(config)` | [`ScannedOtpImportViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/ScannedOtpImportViewModel.kt)、[`ImportScannedOtpUseCase.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/entry/ImportScannedOtpUseCase.kt) |
| OTP/Wi-Fi 编辑器应用 | `tryApplyScannedConfig(config)`、`tryApplyScannedWifi(payload)` | [`AddOtpViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/editor/otp/AddOtpViewModel.kt)、[`AddWifiViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/editor/wifi/AddWifiViewModel.kt) |
| Wi-Fi 内容解析 | `WifiQrPayloadParser.parse(rawValue)` | [`WifiQrPayloadParser.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/policy/WifiQrPayloadParser.kt) |
| 认证绑定的 seed 暂存 | `EntryEditorSeedCoordinator`、`EntryEditorSeedStore.consume(token)` | [`EntryEditorSeedCoordinator.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/navigation/EntryEditorSeedCoordinator.kt)、[`EntryEditorSeedStore.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/entry/EntryEditorSeedStore.kt) |
