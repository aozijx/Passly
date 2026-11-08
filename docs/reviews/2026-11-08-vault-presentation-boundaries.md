# Vault Presentation 边界审查记录

## 审查结论

本次审查覆盖 Vault 列表、详情、四种编辑器、回收站，以及 Route、导航和 seed/扫码交接。实现已完成；当前合同以 [列表与搜索](../features/vault/list-and-search.md)、[编辑与写入](../features/vault/entry-editor-and-persistence.md)、[完整表单编辑](../features/vault/full-form-editing.md) 和 [二维码扫描结果交接](../features/scanner/result-handoff.md) 为准。

页面 owner 按状态、任务、生命周期和资源职责划分。Vault 列表命令由 `VaultEntryCommandWorkflow` 管理；Detail 的 revision、favicon、QR、密码关联和访问页工作流保留在具有独立任务或资源合同的 owner；四类编辑器共享加载失败投影与写入 session，同时保留各自表单规则；Route 负责宿主生命周期、导航和平台 effect。内部调用方已迁移，旧路径与无消费者的转发层已清理。

## 验收证据

- Vault 单元测试：71 个测试类、472 个用例通过。
- `:app:compileDebugAndroidTestKotlin`、`verifyArchitecture`、`:app:compileDebugKotlin` 通过。
- 旧符号、生产调用方、测试、DI、导航、资源与正式文档引用已完成静态闭包扫描。
- 以上结果记录于 2026-11-08 的审查快照；本记录不代表之后代码变更已经重新验证。

## 后续事项

跨 Feature 的 ViewModel 职责审计仍由维护 TODO 的 Task 83 跟踪；本次 Vault 页面审查只关闭其 Vault 子范围，不代替 Task 83 的整体验收。

## 代码位置

| 职责 | 代码 |
| --- | --- |
| Vault 列表状态与命令 | [`VaultViewModel.kt`](../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/list/VaultViewModel.kt)、[`VaultEntryCommandWorkflow.kt`](../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/list/VaultEntryCommandWorkflow.kt) |
| Detail 页面协调与修订工作流 | [`DetailViewModel.kt`](../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/DetailViewModel.kt)、[`DetailRevisionWorkflow.kt`](../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/history/DetailRevisionWorkflow.kt) |
| 编辑器写入与失败提示 | [`EditorWriteSession.kt`](../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/editor/EditorWriteSession.kt)、[`EditorSaveEffectHandler.kt`](../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/editor/EditorSaveEffectHandler.kt) |
| 应用页面导航 | [`PasslyAppNavigation.kt`](../../app/src/main/java/com/aozijx/passly/presentation/PasslyAppNavigation.kt) |
