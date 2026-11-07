# OTP 运行时与复制

状态：当前实现。本文记录 Vault 列表和 Detail 的验证码运行时、HOTP 持久化边界及复制路径；页面输入与扫码创建见[动态口令创建](otp-entry.md)，URI 合同见[动态口令 URI 导入与导出](otp-uri.md)。

## 配置加载与验证码发布

Vault 列表与 Detail 各自持有独立的 `OtpCodeRuntime`，不会共享验证码 State。订阅通过 `subscribe(entryId)` 记录请求并在首次订阅时激活条目；最后一个消费者退订后移除不再需要的运行时状态。Route 在回到前台时分别调用所属 ViewModel 的刷新入口，运行时只重读仍有消费者或仍在请求的条目。会话锁定会清除已生成验证码和调度状态；保留的请求可在新会话认证后重新激活。

`OtpCodeRuntimeFactory.create(viewModelScope)` 为每个 ViewModel 创建独立 Runtime，并绑定 `codeGenerator = OtpGenerator::generate`、`loadOtpConfig = otpConfigRepository::getConfig`、`consumeHotpCode = otpConfigRepository::consumeHotpCode`；`initiallyUnlocked` 取 `sessionStateProvider.isWritable`。Factory 同时在该 scope 收集 `lockStateFlow` 与 `sensitiveKeyFreshnessState.generation.drop(1)`，分别调用 `runtime.onSessionStateChanged(state == SecureSessionState.UNLOCKED)` 和 `runtime.onFreshAuthentication()`。

`activate(entryId)` 从凭据存储加载完整 OTP 配置。运行时调度仅保留条目 ID、配置类型及生成所需元数据，不缓存解密出的 Secret；TOTP 和 Steam 在周期边界重新读取配置并生成当前码。普通存储异常会清除旧码并保留后续 ticker 重试所需的调度信息，其他条目的刷新继续运行；协程取消传播，认证过期和会话锁定走各自状态迁移，fatal `Error` 不会被降级成验证码失败。

配置变化通过 `entryChanged(entryId)` 递增该条目的 activation epoch 并使旧读取或 HOTP 消费失效。OTP 编辑保存返回后，Vault/Detail 在重新进入或生命周期恢复时重新加载配置，无需 ViewModel 间全局失效事件。Detail 恢复历史修订后也会刷新当前条目。若旧 HOTP 事务完成时条目、会话或 epoch 已失效，已提交计数器不会回滚，但验证码不会发布到旧页面或剪贴板。

## HOTP 计数器与详情复制

`OtpGenerator` 生成 HOTP 时同时计算下一计数器。当前计数器为 `Long.MAX_VALUE - 1` 时允许生成并返回 `Long.MAX_VALUE`；当前或覆盖计数器已是 `Long.MAX_VALUE` 时，在解码 Secret 和计算验证码前返回 `CounterExhausted`。负计数器返回 `InvalidCounter`。耗尽值仍可存储和归档，但不能通过溢出或回绕继续生成。

详情复制的调用顺序如下：

```text
DetailViewModel.copyOtpCode()
  -> copyOtpCodeUseCase(
       entryId = entry.id,
       canDeliver = { workSession.isCurrent(token) },
       codeProvider = { otpRuntime.codeForCopy(entry.id.value) },
     )
  -> AuthorizationGate.authorize(AuthorizationScope.Global(COPY_SECRET))
  -> codeProvider() -> OtpCodeRuntime.codeForCopy(entry.id.value)
  -> HOTP: generateHotpCode(entryId) -> consumeHotpCode(entryId)
  -> CopyOtpCodeUseCase 检查 coroutine Job 与 canDeliver，再调用
     SensitiveClipboardWriter.writeSensitiveIf(code, canWrite)
  -> 仅 Copied 且 owner 仍有效时记录 ActivityType.COPY_PASSWORD
```

HOTP 的 `consumeHotpCode(entryId)` 在一个数据库事务内读取当前计数器、生成验证码并写入下一计数器；失败不推进计数器，事务成功后验证码才返回给复制用例。该更新不生成条目修订或内容编辑记录。认证拒绝、复制未完成或详情 owner 失效时不允许将验证码写入剪贴板。页面关闭或会话锁定若发生在事务挂起期间，事务可能已经推进计数器；此时旧运行时令牌会丢弃返回码，用户重新授权后会生成下一码。

TOTP/Steam 的 `codeForCopy(entryId)` 只返回 Runtime 当前 `Ready` 状态中的时间窗验证码，不另行生成历史窗口的码。

## Vault 列表复制路径与待办

列表滑动动作经 `resolveCopyRequest(item, FieldKey.PASSWORD)` 判断条目是否带 OTP；若是，则 `VaultRoute` 派发 `VaultUiAction.CopyOtp(request.entryId)`。`VaultViewModel.copyOtp(entryId)` 在启动 `viewModelScope` 任务前读取 `pageEffects.currentOwner`，调用 `copyOtpCode(EntryId(entryId), canDeliver = { pageEffects.isActive(owner) }, codeProvider = { (totp.states.value[entryId] as? OtpCodeState.Ready)?.code })`。`CopyOtpCodeUseCase.invoke(entryId, canDeliver, codeProvider)` 将调用 coroutine 的 `Job.isActive` 与页面 owner 条件合成为 `canWrite`，在取码前、剪贴板写入时和使用记录前复核；Route 离开后，仍等待授权的任务不会写剪贴板，对应 ViewModel 回归见主维护 TODO Task 27。

HOTP 列表行为仍未决定：provider 只读列表 Runtime 的 `Ready` 码，普通 HOTP 订阅为 `Pending`，因此返回 `Unavailable`，ViewModel 对此结果不显示提示；列表路径不会调用 `OtpCodeRuntime.codeForCopy()` 或推进 HOTP counter。产品决策与 HOTP 专项 ViewModel/Route 覆盖仍见[OTP 审查待办 Task 1](../../superpowers/plans/2026-11-06-passly-otp-review-followups.md#task-1)。Detail 路径传入 `workSession.isCurrent(token)`，并在取码和写剪贴板前复核 owner。

## 代码位置

| 职责 | 类/函数与参数传递 | 源码文件 |
| --- | --- | --- |
| Route 给列表 Runtime 绑定状态与行订阅 callback | `rememberVaultOtpStateProvider(states = vaultViewModel.totpStatesFlow, onSubscribe = vaultViewModel::subscribeVisibleOtp, onUnsubscribe = vaultViewModel::unsubscribeVisibleOtp)`；`VaultScreen` 的行可见性经 provider 调用 `subscribe(entryId)` / `unsubscribe(entryId)` | [`VaultRoute.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/list/VaultRoute.kt)、[`VaultOtpStateProvider.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/list/VaultOtpStateProvider.kt) |
| Route 恢复后刷新仍请求的验证码 | `Lifecycle.Event.ON_RESUME` -> `VaultViewModel.refreshVisibleOtp()` -> `OtpCodeRuntime.refreshRequestedEntries()` | [`VaultRoute.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/list/VaultRoute.kt)、[`OtpCodeRuntime.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/otp/OtpCodeRuntime.kt) |
| 列表与 Detail 各自创建 Runtime 并传入配置读取/HOTP 消费函数 | `OtpCodeRuntimeFactory.create(scope)` 注入 `OtpGenerator::generate`、`OtpConfigRepository::getConfig`、`OtpConfigRepository::consumeHotpCode` 与 `SessionStateProvider.isWritable` 初始状态 | [`OtpCodeRuntimeFactory.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/otp/OtpCodeRuntimeFactory.kt) |
| Detail 恢复配置及复制时携带 owner token | `DetailViewModel.load()`、`copyOtpCode()` -> `copyOtpCodeUseCase(entry.id, canDeliver = { workSession.isCurrent(token) }, codeProvider = { otpRuntime.codeForCopy(entry.id.value) })` | [`DetailViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/DetailViewModel.kt) |
| 对 HOTP 主动消费，对 TOTP/Steam 读取当前 Ready 码 | `OtpCodeRuntime.codeForCopy(entryId)` -> HOTP `generateHotpCode(entryId)` -> `consumeHotpCode(entryId)` | [`OtpCodeRuntime.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/otp/OtpCodeRuntime.kt) |
| 认证、敏感字段读取、剪贴板写入和使用记录顺序 | `CopyOtpCodeUseCase.invoke(entryId, canDeliver, codeProvider)` | [`CopyEntryFieldUseCase.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/entry/CopyEntryFieldUseCase.kt) |
| 密码滑动复制在带 OTP 条目上改派 OTP 请求 | `resolveCopyRequest(item, fieldKey)` -> `VaultCopyRequest.Otp(item.id)` | [`VaultSwipeActionHandler.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/list/action/VaultSwipeActionHandler.kt)、[`VaultRoute.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/list/VaultRoute.kt) |
| HOTP 结果与下一计数器计算 | `OtpGenerator.generate(config, overrideCounter, timestamp)` | [`OtpGenerator.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/otp/OtpGenerator.kt) |
| 配置读取与 HOTP 原子消费入口 | `getConfig(entryId)` -> `queryFullVault(onDenied = { null }) { secretFieldStore.readOtpConfig(this, entryId) }`；`consumeHotpCode(entryId)` -> `transactionFullVault(onDenied = { OtpResult.Failure(OtpGenerationError.InvalidSecret) }) { hotpCodeConsumption.consume(this, entryId) }` | [`RoomOtpConfigRepository.kt`](../../../data/src/main/java/com/aozijx/passly/data/repository/otp/RoomOtpConfigRepository.kt) |
| 事务内读取 OTP 凭据、生成 HOTP 并保存下一计数器 | `hotpCodeConsumption.consume(db = this, entryId = entryId)` 使用默认 `generate = OtpGenerator::generate`；只接受未删除 OTP/HOTP，读取 OTP secret，成功后 `secretFieldStore.updateExisting(...)` 写入下一计数器 | [`HotpCodeConsumptionTransaction.kt`](../../../data/src/main/java/com/aozijx/passly/data/repository/otp/HotpCodeConsumptionTransaction.kt) |
