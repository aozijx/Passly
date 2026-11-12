# Passly OTP 审查待办

<a id="task-1"></a>

## Task 1：补足 Vault 列表 HOTP 复制的 owner 级断言

- [x] 补足 Vault 列表 owner 与 HOTP Runtime 的联动回归：成功只消费一次；事务失败、授权取消、离页或会话撤权时不写入剪贴板或活动记录。`VaultViewModelTest.vaultHOTPcopy*` 使用生产 Runtime 与确定性 repository 验证完整交付链；`CopyOtpCodeUseCaseTest` 覆盖授权后按需取码。定向 App JVM 测试和 KtLint 通过。Route/NavHost 生命周期仍由[主维护 TODO Task 27](2026-10-30-passly-maintainability-todo.md#task-27)跟踪。

当前行为与具体函数见[OTP 运行时与复制](../../../product/vault/otp-runtime.md)。
