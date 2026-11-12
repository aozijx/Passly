# Passly OTP 审查待办

<a id="task-1"></a>

## Task 1：补足 Vault 列表 HOTP 复制的 owner 级断言

- [ ] 补足 Vault owner 与真实 HOTP Runtime 的联动回归。现有 [`CopyEntryFieldUseCaseTest`](../../../../app/src/test/java/com/aozijx/passly/feature/vault/entry/CopyEntryFieldUseCaseTest.kt) 已验证 OTP 授权成功后只读取一次并写入剪贴板/记录活动，以及拒绝时不读取、不写入；[`OtpCodeRuntimeTest`](../../../../app/src/test/java/com/aozijx/passly/feature/vault/otp/OtpCodeRuntimeTest.kt) 已验证 HOTP 计数消费与事务失败；[`VaultViewModelTest`](../../../../app/src/test/java/com/aozijx/passly/feature/vault/presentation/list/VaultViewModelTest.kt) 已验证一般 OTP 授权等待期间离页、会话撤权时不写剪贴板。当前 ViewModel fixture 没有把这三者组合成 HOTP owner 路径，仍需覆盖真实 HOTP 成功只消费一次，以及 HOTP 事务失败、授权拒绝、离页和撤权时不写入剪贴板或活动记录。授权等待期间离页的通用 owner 合同另由[主维护 TODO Task 27](2026-10-30-passly-maintainability-todo.md#task-27)跟踪。

**当前实现（2026-11-11）：** `VaultRoute` 把密码滑动复制映射为 `VaultUiAction.CopyOtp`；`VaultEntryCommandWorkflow.copyOtp()` 先要求完整安全会话，再用 `CopyOtpCodeUseCase` 完成授权并调用 `OtpCodeRuntime.codeForCopy()`。Runtime 对 HOTP 通过 `generateHotpCode()` 和 `consumeHotpCode()` 原子消费计数器；use case 在取码、剪贴板写入和活动记录前验证调用 Job、页面 owner 与完整会话。产品行为已由当前实现确定为支持列表 HOTP 复制，无待裁定策略。现有 `VaultViewModelTest.routeExitWhileOtpCopyWaitsForAuthorizationPreventsClipboardWrite` 覆盖普通 OTP 请求离页后的剪贴板保护，但未断言 HOTP 的成功消费、拒绝或事务失败结果。

**权威实现合同：** [OTP 运行时与复制](../../../product/vault/otp-runtime.md)。
