# Passly OTP 审查待办

<a id="task-1"></a>

## Task 1：补足 Vault 列表 HOTP 复制的 owner 级断言

- [ ] 补足 Vault 列表 owner 与真实 HOTP Runtime 的联动回归：成功只消费一次；事务失败、授权拒绝、离页或撤权时不写入剪贴板或活动记录。现有单测覆盖与缺口见[OTP Runtime owner 与失效审查](../../reviews/2026-11-11-otp-runtime-owner-and-invalidation.md)；授权等待期间离页的通用 owner 合同另由[主维护 TODO Task 27](2026-10-30-passly-maintainability-todo.md#task-27)跟踪。

当前行为与具体函数见[OTP 运行时与复制](../../../product/vault/otp-runtime.md)。
