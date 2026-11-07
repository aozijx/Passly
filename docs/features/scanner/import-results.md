# 扫描结果交接

Scanner 不解析 OTP、Wi-Fi、链接等业务类型。`ScannerResult` 只保存原始 `rawValue`。结果出现时，打开 Scanner 的页面通过 `ScannerRoute.resolveResult(rawValue)` 在自己的边界解析并生成 `ScannerResultPlan`；计划携带结果卡展示策略和确认动作，Route 只管理确认生命周期与结果回执。

## 调用方处理

- Vault 首页用 `OtpAuthUriCodec` 解析 OTP 并交给 `ScannedOtpImportViewModel`；用 `WifiQrPayloadParser` 解析 Wi-Fi，再通过 `VaultNavigation` 打开 Wi-Fi 编辑器。有效 HTTP/HTTPS 链接由 Vault owner 发起 `ACTION_VIEW`。有效 OTP 隐藏复制操作，主操作显示“保存”，确认时直接导入；未识别内容保留在卡片上供复制。
- OTP 编辑器只解析 OTP URI，再调用 `AddOtpViewModel.tryApplyScannedConfig(config)`；Wi-Fi 编辑器只解析 Wi-Fi 载荷，再调用 `AddWifiViewModel.tryApplyScannedWifi(payload)`。有效值使用“填入”操作。解析失败或当前表单拒绝应用时，结果卡保留，表单不被覆盖。
- owner 在 `resolveResult` 中解析一次，并把解析值捕获在 `ScannerResultPlan.confirm` 闭包中；点击确认不重新解析。Scanner 不新增内容类型子类、purpose 映射或按类型分发的 UI 分支。

## 内容与生命周期边界

OTP 首页的调用链为 `VaultRoute` → `ScannedOtpImportViewModel.importScannedOtp(config)` → `ImportScannedOtpUseCase.invoke(config)` → `EntryCreation(draft)`。保存前校验完整访问和认证会话身份；Activity ViewModel 用 `Mutex.tryLock()` 拒绝并行导入，已确认任务由 Activity ViewModel scope 持有。Use case 取消会传播，普通失败返回拒绝结果。

`ScannerResultSession.confirm { plan.confirm() }` 只接受一次活动确认：返回 `Handled` 后关闭 Scanner，返回 `Rejected` 后保留结果供重试；处理中锁住结果操作，关闭后的迟到回执被忽略。普通异常仍从 Route 的确认 coroutine 逸出，当前没有映射为扫码页可见失败状态。各 owner 的异常合同仍不一致：Vault OTP 导入将普通创建异常映射为 `false`；Wi-Fi 分支的 `VaultNavigation.navigate(EditScannedWifi(payload))` 会经 `seedStager.stage(EntryEditorSeed.Wifi(payload))` 和 `navigateToSingleTopRoute(route)` 完成交接，这些步骤抛出的异常未映射成 `VaultNavigationResult`；HTTP/HTTPS 分支的 `openScannedWebLink(uri)` 捕获普通 `Exception` 并返回 `false`。Route 与 Wi-Fi seed 的失败清理边界见[Scanner 待办 Task 3](../../superpowers/plans/2026-11-06-passly-scanner-review-followups.md)。取消与 fatal `Error` 不应被映射成普通拒绝。

Wi-Fi 首页导航只携带一次性随机 token。`EntryEditorSeedCoordinator` / Store 将 seed 绑定当前认证对象；暂存、消费均核对所有权，编辑目的地组合后离开会通过 `DisposableEffect` 按 token 丢弃未消费 seed，锁定或图作用域销毁也会清除受控引用。若 `navigateToSingleTopRoute(...)` 在目的地组合前抛错，暂存 seed 仍可能留在单槽中；stager 端尚无按 token discard 的补偿入口，见[Scanner Task 3](../../superpowers/plans/2026-11-06-passly-scanner-review-followups.md)和[维护 TODO Task 42](../../superpowers/plans/2026-10-30-passly-maintainability-todo.md)。Wi-Fi 编辑页重新扫码只替换 SSID、安全类型、密码和隐藏网络，备注与标签保留；不直接保存。开放网络保存前清空密码，WEP/WPA personal 密码沿用 Domain 校验。

复制结果仍由 `ScannerViewModel` 通过 `SensitiveClipboardWriter.writeSensitiveIf(text) { work === owner }` 执行，只有当前 owner 有效时才写入剪贴板。Route 只展示成功 effect，不显示扫码内容相关异常。原始内容不得进入日志、持久化状态、普通 Navigation 参数或 SavedState。

## 代码位置

| 职责 | 类/函数 | 源码 |
| --- | --- | --- |
| 原始结果和一次性确认回执 | `ScannerResult(rawValue)`、`ScannerRoute(resolveResult)`、`ScannerResultSession.confirm(handle)` | [`ScannerResult.kt`](../../../app/src/main/java/com/aozijx/passly/feature/scanner/presentation/ScannerResult.kt)、[`ScannerRoute.kt`](../../../app/src/main/java/com/aozijx/passly/feature/scanner/presentation/ScannerRoute.kt)、[`ScannerResultSession.kt`](../../../app/src/main/java/com/aozijx/passly/feature/scanner/presentation/ScannerResultSession.kt) |
| 调用方解析和卡片展示/确认策略 | `ScannerResultPlan`、`ScannerResultPresentation` | [`ScannerResultPlan.kt`](../../../app/src/main/java/com/aozijx/passly/feature/scanner/presentation/ScannerResultPlan.kt)、[`ScannerResultPresentation.kt`](../../../app/src/main/java/com/aozijx/passly/feature/scanner/presentation/ScannerResultPresentation.kt) |
| Vault / OTP / Wi-Fi owner 转换 | `VaultRoute`、`AddOtpEditorRoute`、`AddWifiEditorRoute` | [`VaultRoute.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/list/VaultRoute.kt)、[`AddOtpEditorRoute.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/editor/otp/AddOtpEditorRoute.kt)、[`AddWifiEditorRoute.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/editor/wifi/AddWifiEditorRoute.kt) |
| Domain 格式解析 | `OtpAuthUriCodec.parse(rawValue)`、`WifiQrPayloadParser.parse(rawValue)` | [`OtpAuthUriCodec.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/otp/OtpAuthUriCodec.kt)、[`WifiQrPayloadParser.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/policy/WifiQrPayloadParser.kt) |
| 认证绑定的 Wi-Fi seed | `EntryEditorSeedCoordinator`、`EntryEditorSeedStore.consume(token)` | [`EntryEditorSeedCoordinator.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/navigation/EntryEditorSeedCoordinator.kt)、[`EntryEditorSeedStore.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/entry/EntryEditorSeedStore.kt) |
