# OTP 运行时与复制

状态：当前实现。本文记录 Vault 列表和 Detail 的验证码运行时、HOTP 持久化边界及复制路径；页面输入与扫码创建见[动态口令创建](otp-entry.md)，URI 合同见[动态口令 URI 导入与导出](otp-uri.md)。

## 配置加载与验证码发布

`OtpRuntimeModule` 在 `SingletonComponent` 中只提供一个 `OtpCodeRuntime`。Runtime 使用应用级协程作用域，并在一次 `start(signals)` 中独自启动 ticker，只持续收集会话锁定和新鲜认证状态。Vault 图级 `VaultViewModel` 是列表和 Detail 共用的 Runtime/状态投影 owner；每个可见列表行与 Detail OTP 区块分别 acquire 自己的 `OtpLease`。列表行在 OTP 展示开启时 acquire，关闭展示或离开组合时通过 `DisposableEffect` 释放；Detail 只在已加载条目 ID 与路由 ID 一致、且有 OTP 区块时 acquire，详情切换或离开组合时释放。独立 lease 让列表和 Detail 可同时使用同一条目，单个 owner 退出不会释放另一个 owner 的 active demand。最后一个 lease 关闭后 Runtime 清除验证码、生成材料并停止刷新与重试；重新可见时重新 acquire。会话锁定会清除已生成验证码和调度状态，但保留仍有效的 lease，解锁后由 Runtime 恢复。页面恢复不负责重订阅或重试，ticker 按当前时间校正时间窗。

`OtpRuntimeModule` 使用应用级 `CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)` 创建该单例，绑定生成器、OTP 数据端口、OTP 策略快照读取端口和初始会话状态，并把锁定、新鲜认证映射为 `OtpRuntimeSignals` 后只调用一次 `start(signals)`。Runtime 自己只建立一组会话信号收集和一个事件驱动 ticker；ticker 在最近的 TOTP 时间窗边界或重试时刻唤醒，最多每 30 秒检查一次系统时钟跳变。租约新增/删除、配置变化、认证状态变化和重试安排会唤醒 ticker 重算截止时刻。重复调用 `start` 不会创建第二组任务。`OtpRuntimePolicy` 是 OTP 运行策略的不可变快照：设置界面观察 `OtpRuntimePolicyRepository.otpRuntimePolicy` 并映射到 UI state；Runtime 只依赖 `OtpRuntimePolicySnapshotReader`，在 TOTP 复制命令中读取最新快照，不接触 Flow、不缓存设置值。DataStore 仍使用既有 `VaultViewPreferences.copy_next_totp_near_expiry` 持久字段，Domain 不把它归入通用列表外观设置。普通快照读取失败只影响本次复制，下一次用户操作会重新读取；取消传播，fatal `Error` 不会降级。

`OtpConfigRepository.getGenerationMaterial(entryId)` 返回调用者持有的 `OtpGenerationMaterial`：非敏感配置中不再放置字符串 Secret，种子保存在可清零字节数组中。首次激活一个 TOTP/Steam lease 时，Runtime 将种子复制到自己的可清零缓存，并立即关闭仓库返回的原始材料；随后每个时间窗都使用这份会话内种子生成当前码和下一码，不再依赖 30 秒新鲜认证密钥重复解密。每次生成会从 Runtime 缓存创建独立材料副本，并在 `finally` 清零。HOTP lease 只为显示当前未消费码读取一次材料，生成后立即关闭，不缓存种子。最后一个 lease 释放、保险库锁定、条目变更/删除、保险库替换或永久输入错误时，Runtime 清零持有的缓存。种子不会进入 StateFlow、UI 状态或持久化数据。首次读取仍遵守敏感字段的新鲜认证门限；锁定后恢复 lease 时重新读取。`OtpGenerator` 在 Domain 的 `entry/otp/generation` 中只接受字节材料；Base32/Base64 解码不经过种子 `String`。Security 解密直接返回字节，HOTP 事务和 Autofill 仍在 `finally` 清理应用持有的种子与解码缓冲。普通 OTP 配置编辑、URI 与归档仍使用其既有格式字符串合同，不进入生成路径。调度另保留条目 ID、配置类型、周期和 moving factor。普通存储异常会清除旧码并保留后续 ticker 重试所需的调度信息，其他条目的刷新继续运行；协程取消传播，认证过期和会话锁定走各自状态迁移，fatal `Error` 不会被降级成验证码失败。

配置变化通过 `entryChanged(entryId)` 递增该条目的 activation epoch 并使旧读取或 HOTP 消费失效。OTP 编辑保存和 Detail 历史修订恢复后，由当前 owner 调用 `entryChanged` 立即重新加载配置，无需 ViewModel 间全局失效事件。覆盖导入通过 `VaultBackupStore.restore` 的 `onCommitted` 同步通知 `vaultReplaced()`；通知发生在 Room 事务提交后、任何可取消的文件清理前，重新加载所有仍有活动租约的条目。追加导入不改写现有条目，不触发重载。若旧 HOTP 事务完成时条目、会话或 epoch 已失效，已提交计数器不会回滚，但验证码不会发布到旧页面或剪贴板。可恢复的存储异常和 `CryptoError` 进入 Runtime 退避重试（1 秒起、上限 30 秒）；成功后清除退避计数。非法 Secret/计数器错误等待配置变化后再试。协程取消传播，认证过期和会话锁定走各自状态迁移，fatal `Error` 不会被降级成验证码失败。

## HOTP 计数器与详情复制

`OtpGenerator` 生成 HOTP 时同时计算下一计数器。当前计数器为 `Long.MAX_VALUE - 1` 时允许生成并返回 `Long.MAX_VALUE`；当前或覆盖计数器已是 `Long.MAX_VALUE` 时，在解码 Secret 和计算验证码前返回 `CounterExhausted`。负计数器返回 `InvalidCounter`。耗尽值仍可存储和归档，但不能通过溢出或回绕继续生成。Vault 与 Detail 的 HOTP 卡片都显示当前计数器对应的预览码；Runtime 生成预览后立即清理种子，未消费计数器。Detail 点按验证码时才进入现有复制授权，再要求数据库事务中的计数器仍等于预览值，生成验证码并持久化下一计数器。成功消费后 Runtime 刷新并显示下一码。Detail 的“上一次”只允许撤销 Runtime 记录的最近一次消费；事务仅在持久计数器仍等于该次消费后的值时将其回退一位。条目变更、删除或最后一个 lease 释放时清除撤销令牌；令牌仅保存在进程内，应用重启后不提供旧操作撤销。

详情复制的调用顺序如下：

```text
DetailViewModel.copyOtpCode()
  -> copyOtpCodeUseCase(
       entryId = entry.id,
       canDeliver = { workSession.isCurrent(token) },
     )
  -> AuthorizationGate.authorize(AuthorizationScope.Global(COPY_SECRET))
  -> CopyOtpCodeUseCase -> OtpCodeRuntime.codeForCopy(entryId.value)
  -> HOTP: generateHotpCode(entryId) -> consumeHotpCode(entryId)
  -> CopyOtpCodeUseCase 检查 coroutine Job 与 canDeliver，再调用
     SensitiveClipboardWriter.writeSensitiveIf(code, canWrite)
  -> 仅 Copied 且 owner 仍有效时记录 ActivityType.COPY_OTP_CODE
```

HOTP 的 `consumeHotpCode(entryId, expectedCounter)` 在一个数据库事务内确认预览计数器仍有效、生成验证码并写入下一计数器；计数器已变化时返回 `CounterChanged` 并刷新预览，不消费错误的码。事务成功后验证码才返回给复制用例。普通异常会映射为本次复制不可用，不自动重试消费；用户再次主动复制时才会重新尝试。取消和 fatal `Error` 继续传播。该更新不生成条目修订或内容编辑记录。认证拒绝、复制未完成或详情 owner 失效时不允许将验证码写入剪贴板。页面关闭或会话锁定若发生在事务挂起期间，事务可能已经推进计数器；此时旧运行时令牌会丢弃返回码，用户重新授权后会生成下一码。

未持有活动租约的按需复制会捕获开始时的会话 epoch。TOTP 生成或 HOTP 消费挂起期间若会话锁定，Runtime 在返回验证码前复核会话仍解锁且 epoch 未变化；有租约的请求还会复核该条目的 activation token。HOTP 事务已经提交时不会回滚计数器，但旧会话不会收到验证码。

TOTP/Steam 的 `TimedReady` 状态保存当前码、下一窗码和当前时间窗截止时间/周期，不保存复制阶段。Runtime 只在时间窗边界发布新的码。应用导航根组合提供唯一的进度时钟：宿主处于 `STARTED` 时按整秒边界更新一次；Vault 与 Detail 的 M3 Expressive 进度指示器共享这个时钟，并根据各自状态中的绝对截止时间和周期计算剩余进度。卡片不创建自己的 ticker/动画协程，因此列表滚出再进入时仍按绝对截止时间对齐，且每秒进度变化不会写入 Runtime 的共享 StateFlow。复制时 Runtime 先读 `OtpRuntimePolicy` 快照，再按决策时刻判断是否位于最后 3 秒，避免偏好变化依赖订阅时序。策略读取或计算期间若跨过时间窗，有活动租约时 Runtime 使用同一份租约种子按新的时间窗重算；无活动租约时使用本次按需读取的临时材料重算并清理。无法稳定在当前时间窗时不返回验证码。`OtpRuntimePolicy.copyNextTotpNearExpiry` 默认开启并可关闭临期复制下一窗码。部分服务在窗口开始前可能拒绝下一窗码。

## Vault 列表复制路径

列表滑动动作经 `resolveCopyRequest(item, FieldKey.PASSWORD)` 判断条目是否带 OTP；若是，则 `VaultRoute` 派发 `VaultUiAction.CopyOtp(request.entryId)`。`VaultEntryCommandWorkflow.copyOtp()` 先要求完整安全会话，再捕获页面 owner 并调用 `CopyOtpCodeUseCase`；用例先完成复制授权，随后通过 `OtpCodeRuntime.codeForCopy(entryId)` 取码。HOTP 卡片显示 lease 生成的当前预览码；密码滑动复制仍通过事务消费该计数器。隐藏或未激活卡片也会按需读取并清理材料。用例把 coroutine Job、页面 owner 和完整会话条件合成为交付检查，在取码前、剪贴板写入时和使用记录前复核；Route 离开、会话撤权或授权未通过时不会向旧剪贴板目标写入。Detail 同样在 `DetailViewModel.copyOtpCode()` 传入 `workSession.isCurrent(token)`，并在取码和剪贴板写入前复核页面 owner。

## 代码位置

| 职责 | 类/函数与参数传递 | 源码文件 |
| --- | --- | --- |
| Vault 图 ViewModel 提供 Runtime lease；可见 Compose owner 各自管理 lease 生命周期 | `VaultViewModel.acquireOtp(entryId)`；Vault 行与 Detail OTP 区块在 `DisposableEffect` 中 acquire，并在 effect 键变化或离开组合时 close | [`VaultViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/list/VaultViewModel.kt)、[`VaultEntryRow.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/list/ui/component/list/VaultEntryRow.kt)、[`DetailRoute.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/DetailRoute.kt) |
| 创建应用级共享 Runtime 并只启动一组持续状态收集 | `OtpRuntimeModule.provideOtpCodeRuntime()` 注入生成/存储/策略读取端口并把会话状态映射成 `OtpRuntimeSignals`，Runtime 的 `start(signals)` 幂等启动 ticker 和两条会话信号流 | [`OtpRuntimeModule.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/otp/OtpRuntimeModule.kt)、[`OtpCodeRuntime.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/otp/OtpCodeRuntime.kt) |
| Detail 观察共享 Runtime 状态并为当前 OTP 条目持有独立 lease | `DetailRoute` 收集 `VaultViewModel.otpStates`，仅在当前路由与已加载条目 ID 匹配且有 OTP 区块时调用 `acquireOtp(entryId)`；退出或切换条目时关闭 lease。`DetailViewModel` 不拥有 Runtime 或重复 OTP 状态 | [`DetailRoute.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/DetailRoute.kt)、[`DetailViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/DetailViewModel.kt) |
| Detail 复制时携带 owner token | `copyOtpCode()` -> `CopyOtpCodeUseCase.invoke(entryId, canDeliver)`；use case 只在授权后请求 Runtime 生成码 | [`DetailViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/DetailViewModel.kt)、[`CopyOtpCodeUseCase.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/otp/CopyOtpCodeUseCase.kt) |
| Vault 列表复制先校验完整安全会话与页面 owner，再授权、取码和记录活动 | `VaultEntryCommandWorkflow.copyOtp()` -> `CopyOtpCodeUseCase.invoke(entryId, canDeliver)`；列表工作流不持有 Runtime，HOTP 取码由 use case 请求 Runtime 原子消费 | [`VaultEntryCommandWorkflow.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/list/VaultEntryCommandWorkflow.kt)、[`CopyOtpCodeUseCase.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/otp/CopyOtpCodeUseCase.kt) |
| HOTP lease 预览当前码；复制使用预览计数器原子消费，Detail 可撤销 Runtime 记录的最近一次消费 | 激活时生成 `HotpReady(code, counter, canRollbackPrevious)`；复制调用 `consumeHotpCode(entryId, expectedCounter)`；撤销调用 `rollbackHotpCounter(entryId, expectedCurrentCounter)` | [`OtpCodeRuntime.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/otp/OtpCodeRuntime.kt)、[`RollbackHotpCounterUseCase.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/entry/RollbackHotpCounterUseCase.kt) |
| 区分 UI 偏好观察与 OTP 操作策略读取 | `OtpRuntimePolicyRepository.otpRuntimePolicy` 供设置 UI 观察；`OtpRuntimePolicySnapshotReader.readOtpRuntimePolicySnapshot()` 供复制命令读取单次快照 | [`OtpRuntimePolicyRepository.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/settings/port/OtpRuntimePolicyRepository.kt)、[`ProtoAppPreferencesStore.kt`](../../../data/src/main/java/com/aozijx/passly/data/repository/settings/ProtoAppPreferencesStore.kt) |
| OTP 认证、Runtime 取码、剪贴板写入和使用记录顺序 | `CopyOtpCodeUseCase.invoke(entryId, canDeliver)` | [`CopyOtpCodeUseCase.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/otp/CopyOtpCodeUseCase.kt) |
| HOTP 列表 owner 复制与错误交付合同 | `vaultHOTPcopyConsumesCounterOnceAndRecordsOnlyDeliveredCopy` 及失败、取消、离页和撤权用例 | [`VaultViewModelTest.kt`](../../../app/src/test/java/com/aozijx/passly/feature/vault/presentation/list/VaultViewModelTest.kt)、[`CopyOtpCodeUseCaseTest.kt`](../../../app/src/test/java/com/aozijx/passly/feature/vault/otp/CopyOtpCodeUseCaseTest.kt) |
| 密码滑动复制在带 OTP 条目上改派 OTP 请求 | `resolveCopyRequest(item, fieldKey)` -> `VaultCopyRequest.Otp(item.id)` | [`VaultSwipeActionHandler.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/list/action/VaultSwipeActionHandler.kt)、[`VaultRoute.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/list/VaultRoute.kt) |
| 根据强类型非敏感参数与可擦除字节种子生成 OTP | `OtpGenerator.generate(material: OtpGenerationMaterial, overrideCounter, timestamp)`；生成参数由 `OtpConfig.toGenerationConfig()` 映射，不携带 nullable 字符串种子 | [`OtpGenerator.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/otp/generation/OtpGenerator.kt)、[`OtpGenerationConfig.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/otp/generation/OtpGenerationConfig.kt) |
| generation material 读取与 HOTP 消费/撤销入口 | `getGenerationMaterial(entryId)` -> `queryFullVault { secretFieldStore.readOtpGenerationMaterial(this, entryId) }`；`consumeHotpCode(entryId, expectedCounter)` 和 `rollbackHotpCounter(entryId, expectedCurrentCounter)` -> `transactionFullVault` | [`RoomOtpConfigRepository.kt`](../../../data/src/main/java/com/aozijx/passly/data/repository/otp/RoomOtpConfigRepository.kt) |
| 事务内读取 OTP 凭据、生成 HOTP 并保存下一计数器 | `hotpCodeConsumption.consume(db = this, entryId = entryId)` 以字节 seed 建立 generation material，成功后 `secretFieldStore.updateExisting(...)` 写入下一计数器，并在 `finally` 关闭材料 | [`HotpCodeConsumptionTransaction.kt`](../../../data/src/main/java/com/aozijx/passly/data/repository/otp/HotpCodeConsumptionTransaction.kt) |
