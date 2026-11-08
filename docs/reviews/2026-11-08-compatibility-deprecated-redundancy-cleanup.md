# 兼容、弃用与冗余清理审查

**状态：** 第一批调用闭包已清理；全仓声明与重复规则审计仍开放，后续由[活动计划](../superpowers/plans/2026-11-08-compatibility-deprecated-redundancy-cleanup.md)跟踪。

本记录保存已完成修改的精简验收依据。当前合同分别由对应正式主题页说明；本表不作为待办清单。

| 已清理项 | 证据与保留边界 |
| --- | --- |
| Autofill `LEGACY_SAVE` 交互状态与合成测试 | 无生产创建者/消费者；旧词仅保留在 [ADR-0023](../decisions/ADR-0023-typed-autofill-interactions.md) 的历史决策中。实际 Legacy 保存仍使用 `AutofillInboxSource.LEGACY_SERVICE`。 |
| `VaultIcons.getIconByName()`、`SecureSessionAccessState.isDatabaseOpen()`、`AppResult.onSuccessSuspend()`、`BackupStorageSupport.defaultDocumentsTreeUri()` | 均无生产调用者；图标 fallback、`isUnlocked()`、既有 Result 组合及 SAF 用户目录选择仍是现有路径。 |
| Settings 路由重复表与 `AuthenticationMethods.restrictTo()` | 路由 key lookup 共用 detail route catalog；认证请求在边界使用集合交集。 |
| `LocalAlertPolicy.activeAlert()`、`BackupArchiveKeyRegistry` 资源 kind 重复映射、`SensitiveRevisionRestorePolicy.isExactAuthorization()` | 移除测试专用访问器或重复派生逻辑；告警通过 `observe()` 行为验证，资源 kind 由 `@SerialName` 定义，恢复授权由 `affectedFields()` 的真实用例覆盖。 |
| `AuthMethodCatalog`、`Authenticator` Domain ports 及旧 KDoc | 无实现、DI binding 或 caller；认证入口归 `AuthenticationManager.authenticate()`。 |
| `OtpCodeRuntime.activate()`、`AuthorizationPermitRegistry.activePermitCount()` | 仅测试直接调用或读取私有状态；测试改由生产订阅/刷新/ticker 路径激活，并通过 consume/reject 行为验证 permit。 |
| `LogStatements.backgroundLockSkipped()` factory | 无调用者而删除；稳定事件键 `application.background_lock_skipped` 仍保留以读取历史持久化日志。 |
| 未使用的 Room DAO 批量活动写入/删除、存在性、清理、能力保留与多条秘密字段读取方法 | 搜索未发现源代码/测试调用；表结构和持久化格式未改。 `countByEntryIdAndType()` 仍由 revision restore instrumentation 使用。 |

审计期间曾因 Kotlin trailing-lambda 调用形式漏检 `AppDatabaseSession.queryForMaintenance()`；全仓搜索发现 Backup store、附件 GC 与 Room session 的 AndroidTest 调用，因此保留为内部测试检查入口，并在[数据库文档](../data/database.md)记录其维护边界。它不是兼容 API。

对以上已删除符号进行的最终源码/测试搜索未发现剩余代码引用；生产 Kotlin/Java `@Deprecated` 搜索未发现声明。原审计运行 `git diff --check` 通过，未运行测试或构建。该证据仅适用于本记录中的已清理批次，不代表全仓审计已完成。
