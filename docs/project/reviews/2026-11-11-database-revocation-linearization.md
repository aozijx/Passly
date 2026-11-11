# 数据库撤权线性化审查

## 当前合同

`AppDatabaseSession` 从 Security access-state provider 读取授权状态，在请求运行时 session 前检查一次，并在取得 lease、解析数据库 handle 后、执行 DAO block 前再次检查。第二次检查是 Data 命令 admission point：已发布 `AuthenticationState.Locking` 后才到达的请求不得进入 DAO；已经准入的事务持有 lease，soft-lock/seal 要等它真实退出后才能关闭资源。Room observation 由 `observeFullVaultWhenReadable()` 统一组合认证状态、Runtime lock 和 generation，不由 Repository 另行拼接授权 Flow。数据库维护入口仍有显式权限与 Runtime lease 边界。

Security lock 先撤销访问，再排空 Runtime lease，随后清理敏感会话密钥；Database reset 与 lock 在同一 Security session mutex 下串行。当前实现和数据入口函数见[数据库专题](../../platform/data/database.md#所有权与访问边界)及[Session 关闭审查](2026-11-07-session-close-and-dek-wipe.md)。

## 代码与验证记录

| 范围 | 实现与证据 |
| --- | --- |
| 锁定、reset 与敏感 key 收尾 | [`VaultSessionController.lock()` 与 lifecycle tests](../../../security/src/main/java/com/aozijx/passly/security/session/VaultSessionController.kt)、[`VaultSessionControllerLifecycleTest`](../../../security/src/test/java/com/aozijx/passly/security/session/VaultSessionControllerLifecycleTest.kt)、[Session/DEK 审查](2026-11-07-session-close-and-dek-wipe.md) |
| Data 命令授权和 lease admission | [`AppDatabaseSession`](../../../data/src/main/java/com/aozijx/passly/data/local/database/session/AppDatabaseSession.kt)、[`AppDatabaseSessionResource`](../../../data/src/main/java/com/aozijx/passly/data/local/database/session/AppDatabaseSessionResource.kt)、[数据库专题](../../platform/data/database.md#查询与事务的调用链) |
| Room observation 锁定/重开 | [`AppDatabaseSessionObservations`](../../../data/src/main/java/com/aozijx/passly/data/local/database/session/AppDatabaseSessionObservations.kt)、[`DatabaseObservationRecoveryTest`](../../../data/src/test/java/com/aozijx/passly/data/local/database/session/DatabaseObservationRecoveryTest.kt)、[`RoomObservationSessionTest`](../../../data/src/androidTest/java/com/aozijx/passly/data/local/database/session/RoomObservationSessionTest.kt) |
| 附件 GC 授权 | 具体 drain、分页和引用保护合同见[附件 GC 审查](2026-11-11-attachment-gc-postcommit-review.md)；GC 通过 Full Vault session API 访问数据库。 |

维护计划中的历史记录报告了 Security/Runtime 测试、Data 主代码及 AndroidTest 源码编译和 `verifyArchitecture` 通过；Room observation 曾有 SQLCipher 测试执行记录。另一轮 Data admission instrumentation 因无连接设备未运行。以上是历史结果，本记录未重跑测试；这些测试分别覆盖局部状态转换、准入或 Room 行为，不证明真实 Security、Runner、Full Vault/Autofill Repository 与 reset/lock 交错后的统一提交/取消终态。

## 未闭合事项

[主维护计划 Task 37](../work/plans/2026-10-30-passly-maintainability-todo.md#task-37) 继续跟踪跨层可控并发终态、RecoveryMode/Autofill/idle/reset 撤权顺序以及可用设备上的 SQLCipher/Room 交错验收。当前局部测试不能替代这些 owner 级证据。
