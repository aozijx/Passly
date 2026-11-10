# 扫描结果交接

Scanner 不解析 OTP、Wi-Fi、链接等业务类型。`ScannerResult` 只保存原始 `rawValue`。结果出现时，打开 Scanner 的页面通过 `ScannerRoute.resolveResult(rawValue)` 在自己的边界解析并生成 `ScannerResultPlan`；计划携带结果卡展示策略和确认动作，Route 只管理确认生命周期与结果回执。

## 调用方处理

- Vault 首页用 `OtpAuthUriCodec` 解析 OTP 并交给 `ScannedOtpImportViewModel`；用 `WifiQrPayloadParser` 解析 Wi-Fi，再通过 `VaultNavigation` 打开 Wi-Fi 编辑器。有效 HTTP/HTTPS 链接由 Vault owner 发起 `ACTION_VIEW`。有效 OTP 隐藏复制操作，主操作显示“保存”，确认时直接导入；未识别内容保留在卡片上供复制。
- OTP 编辑器只解析 OTP URI，再调用 `AddOtpViewModel.tryApplyScannedConfig(config)`；Wi-Fi 编辑器只解析 Wi-Fi 载荷，再调用 `AddWifiViewModel.tryApplyScannedWifi(payload)`。有效值使用“填入”操作。解析失败或当前表单拒绝应用时，结果卡保留，表单不被覆盖。
- owner 在 `resolveResult` 中解析一次，并把解析值捕获在 `ScannerResultPlan.confirm` 闭包中；点击确认不重新解析。Scanner 不新增内容类型子类、purpose 映射或按类型分发的 UI 分支。

各入口的参数传递如下：

```text
VaultRoute -> ScannerRoute(resolveResult = { rawValue -> ... })
  -> OtpAuthUriCodec.parse(rawValue) -> Parsed.config
  -> ScannerResultPlan(presentation = Save/no-copy, confirm = {
       scannedOtpImportViewModel.importScannedOtp(config)
     })
  -> ScannedOtpImportViewModel.importScannedOtp(config)
  -> ImportScannedOtpUseCase.invoke(config)
  -> config.toNewEntryDraft() -> EntryCreation(draft)

VaultRoute -> WifiQrPayloadParser.parse(rawValue) -> Parsed.payload
  -> ScannerResultPlan(confirm = {
       navigation.navigate(VaultDestinationRequest.EditScannedWifi(payload))
     })
  -> VaultNavigation.navigate(request)
  -> seedStager.stage(EntryEditorSeed.Wifi(request.payload))
  -> navigateToSingleTopRoute(AppRoute.AddWifiFromSeed.createRoute(staged.token.value))

AddOtpEditorRoute -> ScannerRoute(resolveResult = { rawValue -> ... })
  -> OtpAuthUriCodec.parse(rawValue) -> Parsed.config
  -> ScannerResultPlan(presentation = Use/no-copy, confirm = {
       viewModel.tryApplyScannedConfig(config)
     })

AddWifiEditorRoute -> ScannerRoute(resolveResult = { rawValue -> ... })
  -> WifiQrPayloadParser.parse(rawValue) -> Parsed.payload
  -> ScannerResultPlan(presentation = Use, confirm = {
       viewModel.tryApplyScannedWifi(payload)
     })
```

所有确认均由 `ScannerRoute.handleScreenAction(ConfirmResult)` 启动 `scope.launch { resultSession.confirm { plan.confirm() } }`。`Handled` 关闭 Scanner，`Rejected` 保留卡片并提示此内容无法处理；owner 返回 `Failed` 或普通 `Exception` 时，确认会话恢复 `Ready`、保留结果并显示通用重试提示。取消继续传播，fatal `Error` 清理状态后继续传播；关闭后才到达的失败回执被忽略。OTP 导入和编辑器应用仍由各自 owner 返回拒绝结果；Wi-Fi 导航返回 `NavigationFailed` 时映射为 `Failed`，不再误报格式拒绝。

## 内容与生命周期边界

OTP 首页的调用链为 `VaultRoute` → `ScannedOtpImportViewModel.importScannedOtp(config)` → `ImportScannedOtpUseCase.invoke(config)` → `EntryCreation(draft)`。保存前校验完整访问和认证会话身份；Activity ViewModel 用 `Mutex.tryLock()` 拒绝并行导入，已确认任务由 Activity ViewModel scope 持有。Use case 取消会传播，普通失败返回拒绝结果。

`ScannerResultSession.confirm { plan.confirm() }` 只接受一次活动确认：返回 `Handled` 后关闭 Scanner，`Rejected` 或 `Failed` 保留结果供重试；处理中锁住结果操作，关闭后的迟到回执被忽略。普通异常映射为不含异常详情的失败回执；取消与 fatal `Error` 不映射为普通拒绝。OTP 导入将普通创建异常映射为 `false`；Wi-Fi 的 `EntryEditorSeedCoordinator.navigateWithSeed()` 在导航成功前按 token 暂存，并在取消、fatal `Error` 或普通异常时尝试按 token 清理，再传播或返回 `NavigationFailed`。`VaultRoute` 将 `NavigationFailed` 转成失败回执。HTTP/HTTPS 分支的 `openScannedWebLink(uri)` 捕获普通 `Exception` 并返回 `false`。仍待运行验证：确认失败后的保留与重试、并发点击/关闭竞争、OTP/Wi-Fi/外链 owner 闭包组合及 token 精确清理；敏感扫码文本不得进入日志或 Toast。本轮没有运行这些验证。

Wi-Fi 首页导航只携带一次性随机 token。`EntryEditorSeedCoordinator` / Store 将 seed 绑定当前认证对象；暂存、消费均核对所有权，编辑目的地组合后离开会通过 `DisposableEffect` 按 token 丢弃未消费 seed，锁定或图作用域销毁也会清除受控引用。导航在目的地组合前失败时，Coordinator 按对应 token 补偿清理，不会删除被后续 token 替换的 seed。Wi-Fi 编辑页重新扫码只替换 SSID、安全类型、密码和隐藏网络，备注与标签保留；不直接保存。开放网络保存前清空密码，WEP/WPA personal 密码沿用 Domain 校验。

复制结果仍由 `ScannerViewModel` 通过 `SensitiveClipboardWriter.writeSensitiveIf(text) { work === owner }` 执行，只有当前 owner 有效时才写入剪贴板。Route 只消费不含扫码内容的反馈 state，不显示扫码内容相关异常。原始内容不得进入日志、持久化状态、普通 Navigation 参数或 SavedState。

## 代码位置

| 职责 | 类/函数 | 源码 |
| --- | --- | --- |
| 原始结果和一次性确认回执 | `ScannerResult(rawValue)`、`ScannerRoute(resolveResult)`、`ScannerResultSession.confirm(handle)` | [`ScannerResult.kt`](../../../app/src/main/java/com/aozijx/passly/feature/scanner/presentation/result/ScannerResult.kt)、[`ScannerRoute.kt`](../../../app/src/main/java/com/aozijx/passly/feature/scanner/presentation/route/ScannerRoute.kt)、[`ScannerResultSession.kt`](../../../app/src/main/java/com/aozijx/passly/feature/scanner/presentation/result/ScannerResultSession.kt) |
| 调用方解析和卡片展示/确认策略 | `ScannerResultPlan`、`ScannerResultPresentation` | [`ScannerResultPlan.kt`](../../../app/src/main/java/com/aozijx/passly/feature/scanner/presentation/result/ScannerResultPlan.kt)、[`ScannerResultPresentation.kt`](../../../app/src/main/java/com/aozijx/passly/feature/scanner/presentation/result/ScannerResultPresentation.kt) |
| Vault / OTP / Wi-Fi owner 转换 | `VaultRoute`、`AddOtpEditorRoute`、`AddWifiEditorRoute` | [`VaultRoute.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/list/VaultRoute.kt)、[`AddOtpEditorRoute.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/editor/otp/AddOtpEditorRoute.kt)、[`AddWifiEditorRoute.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/editor/wifi/AddWifiEditorRoute.kt) |
| Domain 格式解析 | `OtpAuthUriCodec.parse(rawValue)`、`WifiQrPayloadParser.parse(rawValue)` | [`OtpAuthUriCodec.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/otp/OtpAuthUriCodec.kt)、[`WifiQrPayloadParser.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/policy/WifiQrPayloadParser.kt) |
| 认证绑定的 Wi-Fi seed | `EntryEditorSeedCoordinator`、`EntryEditorSeedStore.consume(token)` | [`EntryEditorSeedCoordinator.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/navigation/EntryEditorSeedCoordinator.kt)、[`EntryEditorSeedStore.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/entry/EntryEditorSeedStore.kt) |
| OTP 导入的认证、单飞与创建边界 | `importScannedOtp(config)` 检查 `AuthenticationState.Authenticated`，`Mutex.tryLock()` 后由 ViewModel scope 执行 use case；`invoke(config)` 校验 Full Vault，再 `config.toNewEntryDraft()` 并调用 `createEntry(draft)` | [`ScannedOtpImportViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/ScannedOtpImportViewModel.kt)、[`ImportScannedOtpUseCase.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/entry/ImportScannedOtpUseCase.kt) |
| Wi-Fi seed 路由 | `navigate(VaultDestinationRequest.EditScannedWifi(payload))` -> `stage(EntryEditorSeed.Wifi(request.payload))` -> `navigateToSingleTopRoute(createRoute(staged.token.value))` | [`VaultNavigation.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/navigation/VaultNavigation.kt) |
