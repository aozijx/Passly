# Passly OTP 审查待办

<a id="task-1"></a>

## Task 1：补足 Vault 列表 HOTP 复制的 owner 级断言

- [ ] 增加 `VaultViewModel`/Route 级 HOTP 复制回归，覆盖授权成功后计数器只消费一次且验证码写入剪贴板；授权拒绝、事务失败、页面离开或会话撤权时不得写入剪贴板或记录复制活动。普通 OTP 状态与滑动动作映射已有测试；授权等待期间离页的通用 owner 合同由[主维护 TODO Task 27](2026-10-30-passly-maintainability-todo.md#task-27)跟踪，此处只验证 HOTP 语义。

**当前实现（2026-11-11）：** `VaultRoute` 把密码滑动复制映射为 `VaultUiAction.CopyOtp`；`VaultEntryCommandWorkflow.copyOtp()` 先要求完整安全会话，再用 `CopyOtpCodeUseCase` 完成授权并调用 `OtpCodeRuntime.codeForCopy()`。Runtime 对 HOTP 通过 `generateHotpCode()` 和 `consumeHotpCode()` 原子消费计数器；use case 在取码、剪贴板写入和活动记录前验证调用 Job、页面 owner 与完整会话。产品行为已由当前实现确定为支持列表 HOTP 复制，无待裁定策略。现有 `VaultViewModelTest.routeExitWhileOtpCopyWaitsForAuthorizationPreventsClipboardWrite` 覆盖普通 OTP 请求离页后的剪贴板保护，但未断言 HOTP 的成功消费、拒绝或事务失败结果。

**权威实现合同：** [OTP 运行时与复制](../../../product/vault/otp-runtime.md)。
