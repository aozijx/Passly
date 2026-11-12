# 兼容与冗余清理审计

本文只跟踪尚未完成的全仓声明与重复规则审计。已完成清理及留存合同见[审计记录](../../reviews/2026-11-08-compatibility-deprecated-redundancy-cleanup.md)；当前 Backup 边界见[工作流总览](../../../product/backup/backup-workflows.md)和[验证要求](../../../product/backup/verification.md)。

## Task 1：完成全仓未引用声明与重复规则审计

- [ ] 按生产 owner 检索各模块 Kotlin/Java 未引用声明、重复策略及测试专用生产接口；对每个候选闭合生产调用、DI/注册、持久化或协议、测试和正式文档证据。只将已证明冗余的候选移除；有效合同或缺少证据的候选记为具体后续项，不按搜索命中直接删除。
- [ ] 更新对应正式主题文档和本审查记录，移除已完成事项；将本任务改成仅包含剩余候选，或在审计闭合后删除本计划并修复索引。

范围包括 `app`、`domain`、`data`、`security`、`runtime`、`core` 和 `buildSrc` 当前生产源集。既有工作区改动需先排除；本计划不包含构建或测试验收，除非用户另行要求。

## 已审计模块

### `runtime/session`（2026-11-12）

模块内七个生产 Kotlin 文件及仓库调用点已逐项核对，未发现有证据支持删除的声明或重复实现。`SessionStateProvider` 提供锁状态和 generation 读取，供 Data 清理队列与 App OTP Runtime 使用；`DatabaseSessionLifecycle` 提供 unlock、soft-lock、seal 命令，供 Security 锁定/重试和 Data reset 使用。两者由 [`AppDatabaseSession`](../../../../data/src/main/java/com/aozijx/passly/data/local/database/session/AppDatabaseSession.kt) 分别实现，DI 绑定见 [`VaultStorageModule`](../../../../data/src/main/java/com/aozijx/passly/data/local/database/VaultStorageModule.kt)。`SessionResource` 由 Data 的 [`AppDatabaseSessionResource`](../../../../data/src/main/java/com/aozijx/passly/data/local/database/session/AppDatabaseSessionResource.kt) 实现，`SessionKeySource` 由 Security 的 [`SecureSessionKeySource`](../../../../security/src/main/java/com/aozijx/passly/security/session/SecureSessionKeySource.kt) 通过 [`SensitiveKeyModule`](../../../../security/src/main/java/com/aozijx/passly/security/di/SensitiveKeyModule.kt) 绑定；并发 lease 和状态转场由 [`RuntimeSessionManagerTest`](../../../../runtime/session/src/test/kotlin/com/aozijx/passly/runtime/session/RuntimeSessionManagerTest.kt)、[`RuntimeSessionLifecycleConcurrencyTest`](../../../../runtime/session/src/test/kotlin/com/aozijx/passly/runtime/session/RuntimeSessionLifecycleConcurrencyTest.kt) 覆盖，当前行为见[数据库会话合同](../../../platform/data/database.md#session-生命周期)和[运行时流程](../../../platform/architecture/runtime-flows.md)。此次为源码、注册、调用方与文档静态审查，没有修改实现，也没有运行测试；其余模块仍未完成审计。
