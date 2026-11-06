# 扫描结果交接

Scanner 不解析 OTP、Wi-Fi、链接等业务类型。`ScannerResult` 只包含 `rawValue`；用户确认后，`ScannerRoute` 将字符串交给打开页面提供的 `onResult(rawValue)`。调用方在自己的页面边界解析并调用业务入口，返回 `Handled` 才关闭 Scanner；`Rejected` 保留结果供用户重试或复制，重复确认和关闭后的迟到回执会被 `ScannerResultSession` 忽略。

## 调用方处理

- Vault 首页用 `OtpAuthUriCodec` 解析 OTP 并交给 `ScannedOtpImportViewModel`；用 `WifiQrPayloadParser` 解析 Wi-Fi，再通过 `VaultNavigation` 打开 Wi-Fi 编辑器。有效 HTTP/HTTPS 链接由 Vault owner 发起 `ACTION_VIEW`。普通内容保留在卡片供复制，交给 Vault 处理会返回 `Rejected`。
- OTP 编辑器只解析 OTP URI，再调用 `AddOtpViewModel.tryApplyScannedConfig(config)`。Wi-Fi 编辑器只解析 Wi-Fi 载荷，再调用 `AddWifiViewModel.tryApplyScannedWifi(payload)`。无法解析或当前表单拒绝应用时，结果卡保留，表单不被覆盖。
- 结果卡只展示原始扫描文本，并提供重扫、复制、继续三个操作。新增业务类型只需在对应消费页面使用已有 parser / use case；Scanner 不新增结果子类型、purpose 映射或 per-type UI 映射。

## 内容与生命周期边界

OTP 首页的调用链为 `VaultRoute` → `ScannedOtpImportViewModel.importScannedOtp(config)` → `ImportScannedOtpUseCase.invoke(config)` → `EntryCreation(draft)`。保存前校验完整访问和认证会话身份；Activity ViewModel 用 `Mutex.tryLock()` 拒绝并行导入，已确认任务由 Activity ViewModel scope 持有。Use case 取消会传播，普通失败返回拒绝结果。

结果回调正常返回 `Handled`/`Rejected` 时由 `ScannerResultSession.confirm(handle)` 关闭或保留结果；若 `onResult(rawValue)` 抛异常，当前 `ScannerRoute` 在 `rememberCoroutineScope().launch` 中直接调用 `confirm { onResult(rawValue) }`，没有映射异常的 `catch`。`confirm()` 的 `finally` 会在仍处于 `Handling` 时复位为 `Ready`，随后原异常仍从 Route coroutine 逸出，当前没有扫码页失败提示。Vault 的 OTP 分支调用 `importScannedOtp()`，其 UseCase 将普通条目写入异常映射为 `false`；Wi-Fi 分支调用 `VaultNavigation.navigate(EditScannedWifi(payload))`，再由 `seedStager.stage(EntryEditorSeed.Wifi(payload))` 和 `navigateToSingleTopRoute(route)` 完成交接，异常没有在该调用链转换成 `VaultNavigationResult`。HTTP/HTTPS 分支的 `openScannedWebLink(uri)` 则捕获普通 `Exception` 并返回 `false`。这些分支的异常合同不一致，Route callback 边界待补齐见[Scanner 待办 Task 3](../../superpowers/plans/2026-11-06-passly-scanner-review-followups.md)。取消与 fatal `Error` 不应被映射成普通拒绝。

Wi-Fi 首页导航只携带一次性随机 token。`EntryEditorSeedCoordinator` / Store 将 seed 绑定当前认证对象；暂存、消费均核对所有权，编辑目的地组合后离开会通过 `DisposableEffect` 按 token 丢弃未消费 seed，锁定或图作用域销毁也会清除受控引用。若 `navigateToSingleTopRoute(...)` 在目的地组合前抛错，暂存 seed 仍可能留在单槽中；stager 端尚无按 token discard 的补偿入口，见[Scanner Task 3](../../superpowers/plans/2026-11-06-passly-scanner-review-followups.md)和[维护 TODO Task 42](../../superpowers/plans/2026-10-30-passly-maintainability-todo.md)。Wi-Fi 编辑页重新扫码只替换 SSID、安全类型、密码和隐藏网络，备注与标签保留；不直接保存。开放网络保存前清空密码，WEP/WPA personal 密码沿用 Domain 校验。

复制结果仍由 `ScannerViewModel` 通过 `SensitiveClipboardWriter.writeSensitiveIf(text) { work === owner }` 执行，只有当前 owner 有效时才写入剪贴板；Route 只展示成功 effect，不显示扫码内容相关异常。原始内容不得进入日志、持久化状态、普通 Navigation 参数或 SavedState。

## 代码位置

| 职责 | 类/函数 | 源码 |
| --- | --- | --- |
| 原始结果和一次性确认回执 | `ScannerResult(rawValue)`、`ScannerRoute(onResult)`、`ScannerResultSession.confirm(handle)` | [`ScannerResult.kt`](../../../app/src/main/java/com/aozijx/passly/feature/scanner/presentation/ScannerResult.kt)、[`ScannerRoute.kt`](../../../app/src/main/java/com/aozijx/passly/feature/scanner/presentation/ScannerRoute.kt)、[`ScannerResultSession.kt`](../../../app/src/main/java/com/aozijx/passly/feature/scanner/presentation/ScannerResultSession.kt) |
| Vault / OTP / Wi-Fi owner 转换 | `VaultRoute`、`AddOtpEditorRoute`、`AddWifiEditorRoute` | [`VaultRoute.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/list/VaultRoute.kt)、[`AddOtpEditorRoute.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/editor/otp/AddOtpEditorRoute.kt)、[`AddWifiEditorRoute.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/editor/wifi/AddWifiEditorRoute.kt) |
| Domain 格式解析 | `OtpAuthUriCodec.parse(rawValue)`、`WifiQrPayloadParser.parse(rawValue)` | [`OtpAuthUriCodec.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/otp/OtpAuthUriCodec.kt)、[`WifiQrPayloadParser.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/policy/WifiQrPayloadParser.kt) |
| 认证绑定的 Wi-Fi seed | `EntryEditorSeedCoordinator`、`EntryEditorSeedStore.consume(token)` | [`EntryEditorSeedCoordinator.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/navigation/EntryEditorSeedCoordinator.kt)、[`EntryEditorSeedStore.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/entry/EntryEditorSeedStore.kt) |
