# Session 关闭与 DEK 擦除审查记录

## 验收结果

### 活动 lease 排空后关闭资源

旧逻辑在 seal drain 超时、请求取消 lease 后等待固定 grace period，可能在仍有活动 lease 时关闭数据库资源。屏障测试先复现该行为；修复后，Runtime 在 active lease 归零后才关闭资源。忽略取消的操作会让锁定继续等待，不会强制关闭仍在使用的句柄。`SessionLeaseGate` 的超时和关闭失败测试、Runtime Session 单测与 `verifyArchitecture` 通过。

### 状态替换时擦除缓存 DEK

锁定流程先保留原 `Unlocked` 状态引用，再撤权并发布 `Locking`，随后在 `NonCancellable` 收尾中覆盖 DEK、清除字段密钥并发布 `Locked`。日志或清理失败不跳过状态收尾；fatal `Error` 在清理后继续传播。DEK 派生字段密钥失败或取消时也会擦除候选缓存并回到锁定态。记录内的 Core/Security 单测、架构验证和编译门禁均通过；未运行设备仪器测试，也不声称擦除 JVM/JCA 的所有副本。

当前合同见[数据库会话](../data/database.md)、[安全总览](../security/overview.md)和[密钥管理](../security/key-management.md)。

## 代码位置

| 职责 | 类/函数 | 源码文件 |
| --- | --- | --- |
| Lease admission、排空及资源关闭 | `SessionLeaseGate.seal()`、`softLock()` | [`SessionLeaseGate.kt`](../../runtime/session/src/main/kotlin/com/aozijx/passly/runtime/session/SessionLeaseGate.kt) |
| 会话资源生命周期 | `RuntimeSessionManager.seal()`、`softLock()` | [`RuntimeSessionManager.kt`](../../runtime/session/src/main/kotlin/com/aozijx/passly/runtime/session/RuntimeSessionManager.kt) |
| 撤权、软锁/封存与密钥清理顺序 | `VaultSessionController.lock()` | [`VaultSessionController.kt`](../../security/src/main/java/com/aozijx/passly/security/session/VaultSessionController.kt) |
| DEK 擦除与状态转换 | `DekManager.lock()`、`unlockAfterFreshAuthentication()` | [`DekManager.kt`](../../security/src/main/java/com/aozijx/passly/security/dek/DekManager.kt) |
