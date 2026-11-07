# Passly OTP 审查待办

<a id="task-1"></a>

## Task 1：明确 Vault 列表 HOTP 滑动复制的产品行为

- [ ] 决定列表密码滑动动作命中带 OTP 的条目时，HOTP 是否应在授权后生成并复制，或该动作是否应对 HOTP 明确禁用/反馈；当前行为是静默无操作。
- [ ] 若支持 HOTP，复用有 owner/session 归属保护的消费路径，确保计数器只在认证通过后的成功事务中推进；授权拒绝、事务失败、条目切换和页面/会话失效均不得把验证码写入旧剪贴板目标。若业务决定明确不支持，则在动作/UI 中显式告知，并让测试覆盖该反馈。
- [ ] 增加 ViewModel/Route 级测试，验证 HOTP 列表复制的产品选择、结果和副作用；保留现有 `OtpCodeRuntimeTest` 对详情消费语义的覆盖，并为 `VaultViewModel.copyOtp` 路径增加独立断言。列表 copy 在授权等待期间离开页面的 owner/剪贴板合同由[主维护 TODO Task 27](2026-10-30-passly-maintainability-todo.md#task-27)唯一详细跟踪。

**代码证据（2026-11-06）：** `VaultRoute` 将 `resolveCopyRequest(item, FieldKey.PASSWORD)` 的 `VaultCopyRequest.Otp` 派发为 `VaultUiAction.CopyOtp(entryId)`。`VaultViewModel.copyOtp(entryId)` 调用 `copyOtpCode(EntryId(entryId))`，provider 只读取 `(totp.states.value[entryId] as? OtpCodeState.Ready)?.code`；普通 HOTP 订阅保持 `Pending`，因此得到 `null`。该 ViewModel 对 `CopyEntryFieldResult.Unavailable` 与 `NotAuthorized` 均无反馈。相比之下，详情路径的 provider 调用 `OtpCodeRuntime.codeForCopy(entryId)`，其中 HOTP 会调用 `generateHotpCode(entryId)` 并消费计数器。

**已有覆盖：** `OtpCodeRuntimeTest` 覆盖运行时 `codeForCopy` 的 HOTP 计数器消费及旧运行时结果丢弃；`VaultSwipeActionHandlerTest` 只覆盖 OTP 条目选择 `VaultCopyRequest.Otp`，没有验证列表 ViewModel 对 HOTP 的生成/反馈。未运行测试。

**2026-11-07 复核：** 旧记录中“列表未传 `canDeliver`”已过时。当前 `VaultViewModel.copyOtp(entryId)` 捕获 `pageEffects.currentOwner`，并传 `canDeliver = { pageEffects.isActive(owner) }` 与 `codeProvider = { (totp.states.value[entryId] as? OtpCodeState.Ready)?.code }`。`CopyOtpCodeUseCase.invoke(...)` 通过 `deliveryGuard(canDeliver)` 同时检查调用 Job 和页面 owner，在读取验证码、剪贴板写入及活动记录前复核；`VaultViewModelClipboardOwnerTest.routeExitWhileOtpCopyWaitsForAuthorizationPreventsClipboardWrite` 覆盖授权等待期间离页后不写剪贴板。该 owner 缺口已由主维护 TODO Task 27 关闭，本 Task 不重复跟踪。仍未决的是 HOTP 的列表产品行为：普通列表订阅没有 `Ready` 码，provider 返回 `null`，结果为静默 `Unavailable`；本 Task 保留该策略决定及 HOTP 列表 ViewModel/Route 行为覆盖。

**相关主题：** [OTP 运行时与复制](../../features/vault/otp-runtime.md)。
