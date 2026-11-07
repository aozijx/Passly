# Domain 职责凝聚迁移审查记录

**日期：** 2026-11-07

**范围：** 移除 Domain `entry` / `access` 包对 Autofill 模型的实现型依赖，分离 Entry 与 Autofill 创建合同，并记录模块/包边界的正式解释。

**状态：** 源码迁移与记录内验证已完成；剩余 Autofill host/device 验收见[Autofill Task 21](../superpowers/plans/2026-11-07-passly-autofill-followups.md#task-21)。

## 迁移结果

- 新增 `DOMAIN_ENTRY_NO_AUTOFILL` 与 `DOMAIN_ACCESS_NO_AUTOFILL` 源码边界规则，限制 Domain 包间源码依赖。
- 将 `AutofillInboxRecordStore` 放到 `domain.autofill.port`，Security 继续负责 sealing/provisioning，Data 负责耐久存储实现。
- 将 Autofill 专属 `createFromAutofill(draft, inboxDraftId)` 从通用 `EntryCreation` 拆入
  `AutofillEntryCreation`。App `CreateEntryUseCase` 同时实现两个能力，并由 `EntryCreationModule` 分别绑定；
  `SaveAutofillCredential` 只依赖 Autofill 专属 port。
- 保留有明确业务含义的 `access ↔ entry`、`settings → entry`、`backup → entry` 关系；不把纯 UI 展示分类提升为 Domain 政策。
- 维持单一纯 Kotlin `:domain` 模块。正式边界与 Android/Kotlin 指南适配见[包与模块边界](../architecture/package-boundaries.md)。

## 验收记录

原实施计划记录：`:buildSrc:test`、`:domain:test`、`:security:testDebugUnitTest`、`:data:testDebugUnitTest`、
`:feature:autofill:testDebugUnitTest` 与 `:app:testDebugUnitTest` 通过；完整 Autofill suite 为 135 项。App 全套测试
曾暴露一个依赖 transient `Loading` 状态的同步脆弱断言；测试已改为使用现有 Flow retry latch，在首次读取时同步，之后
单测和完整 App suite 通过。`verifyArchitecture` 与 Security/Data/Autofill/App compile 通过；
`git diff --check` 通过。以上引用自实施计划记录，本次归档未重跑 Gradle。

## 代码位置

| 职责 | 生产符号 | 源码 |
| --- | --- | --- |
| Autofill-owned persistence port | `AutofillInboxRecordStore` | [`AutofillInboxRecordStore.kt`](../../domain/src/main/kotlin/com/aozijx/passly/domain/autofill/port/AutofillInboxRecordStore.kt) |
| 普通 Entry creation | `EntryCreation.invoke(draft)` | [`EntryCreation.kt`](../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/port/EntryCreation.kt) |
| Autofill Entry creation | `AutofillEntryCreation.createFromAutofill(draft, inboxDraftId)` | [`AutofillEntryCreation.kt`](../../domain/src/main/kotlin/com/aozijx/passly/domain/autofill/port/AutofillEntryCreation.kt) |
| App 创建策略与双合同绑定 | `CreateEntryUseCase`、`EntryCreationModule` | [`CreateEntryUseCase.kt`](../../app/src/main/java/com/aozijx/passly/app/entry/creation/CreateEntryUseCase.kt)、[`EntryCreationModule.kt`](../../app/src/main/java/com/aozijx/passly/app/entry/creation/EntryCreationModule.kt) |
| Autofill 保存调用方 | `SaveAutofillCredential` | [`SaveAutofillCredential.kt`](../../feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/core/SaveAutofillCredential.kt) |
