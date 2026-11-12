# SecureDatabaseResetCoordinator owner 审查

**范围：** 复核 Task 84 中的 `SecureDatabaseResetCoordinator` 候选，沿 Domain 端口、Security 注册、App 适配器、session 锁与生命周期测试检查真实调用闭包。结论只关闭该 coordinator 候选；数据库跨层锁定/reset 交错的剩余验收仍由 Task 37 跟踪。

## Owner 与调用链

`VaultSessionController` 持有 session mutex、认证状态、数据库失败 StateFlow，并实现 Domain `DatabaseResetSessionCoordinator`。Security DI 将同一个 Controller 绑定到 Domain 端口；App `DatabaseResetGatewayAdapter` 通过该端口执行 `DatabaseResetController.reset()`，重置 I/O 放在 IO dispatcher，再把异常映射为 App 结果。没有页面或 Feature 直接依赖 Security coordinator。

`SecureDatabaseResetCoordinator` 是 Controller 委托的内部安全生命周期 owner：它使用 Controller 的同一 mutex 与 lock/unlock/retry 串行；确认完整会话访问后才封存 Runtime；在不可取消阶段根据 Runtime 实际状态决定保留会话或失败关闭。若 Runtime 不为 `UNLOCKED`，或操作产生 fatal error，清理会撤销授权、清除敏感数据密钥、发布 `Locked` 并保留失败；若 Runtime 仍为 `UNLOCKED` 且没有 fatal error，即使操作向调用方返回普通失败，也保留会话。取消与 fatal error 仍须完成必要清理，fatal error 按原合同传播。

它与 `DatabaseSessionRetryCoordinator` 共享外层互斥和错误真相，但 owner 不同：reset 管理外部 destructive operation 的授权撤销、敏感 key 清理和认证恢复；retry 管理单纯的 database reopen 和成功发布顺序。合并会把 reset 专属清理与普通重试语义混为一体，拆除委托则会把取消/fatal 清理代码推入 Controller 的锁定/解锁状态机，未减少任何状态依赖或副本。两者均保留；详细合同见[认证专题](../../platform/security/authentication.md#安全会话可观测性)、[Runtime 流程](../../platform/architecture/runtime-flows.md)和[数据库专题](../../platform/data/database.md)。

## 证据边界

`VaultSessionControllerLifecycleTest` 包含 reset 失败关闭、失败 reopen 后可重试、reset 与安全 lock 排队、重置结果 fatal 后清理等 owner 级路径。它们通过 Controller 间接覆盖内部 coordinator；没有单独的 `SecureDatabaseResetCoordinatorTest`。本轮只审阅源码与已有测试，不运行测试。更宽的跨层 Data/Full Vault 查询准入与 Room 交错尚不能由这些 Security 单测证明，继续留在[主维护计划 Task 37](../work/plans/2026-10-30-passly-maintainability-todo.md#task-37)。

## 代码位置

| 职责 | 类/函数 | 源码或测试 |
| --- | --- | --- |
| 持有共享会话锁、认证状态和数据库故障真相 | `VaultSessionController` | [`VaultSessionController.kt`](../../../security/src/main/java/com/aozijx/passly/security/session/VaultSessionController.kt) |
| 实现 Domain reset boundary 与授权/密钥/认证清理 | `SecureDatabaseResetCoordinator.withExclusiveDatabaseReset()` | [`SecureDatabaseResetCoordinator.kt`](../../../security/src/main/java/com/aozijx/passly/security/session/SecureDatabaseResetCoordinator.kt) |
| 将 reset 操作交给安全会话 owner 并执行平台数据重置 | `DatabaseResetGatewayAdapter.reset()` | [`DatabaseResetGatewayAdapter.kt`](../../../app/src/main/java/com/aozijx/passly/app/database/DatabaseResetGatewayAdapter.kt) |
| 验证失败关闭、lock 排队与 reopen 恢复 | `VaultSessionControllerLifecycleTest` | [`VaultSessionControllerLifecycleTest.kt`](../../../security/src/test/java/com/aozijx/passly/security/session/VaultSessionControllerLifecycleTest.kt) |
