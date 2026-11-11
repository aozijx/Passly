# Vault Presentation 边界审查记录

## 审查结论

本次审查覆盖 Vault 列表、详情、四种编辑器、回收站，以及 Route、导航和 seed/扫码交接。实现已完成；当前合同以 [列表与搜索](../../product/vault/list-and-search.md)、[编辑与写入](../../product/vault/entry-editor-and-persistence.md)、[完整表单编辑](../../product/vault/full-form-editing.md) 和 [二维码扫描结果交接](../../product/scanner/result-handoff.md) 为准。

页面 owner 按状态、任务、生命周期和资源职责划分。Vault 列表命令由 `VaultEntryCommandWorkflow` 管理；Detail 的 revision、favicon、QR、密码关联和访问页工作流保留在具有独立任务或资源合同的 owner；四类编辑器共享加载失败投影与写入 session，同时保留各自表单规则；Route 负责宿主生命周期、导航和平台 effect。内部调用方已迁移，旧路径与无消费者的转发层已清理。

## 验收证据

- Vault 单元测试：71 个测试类、472 个用例通过。
- `:app:compileDebugAndroidTestKotlin`、`verifyArchitecture`、`:app:compileDebugKotlin` 通过。
- 应用关联选择器状态迁移：`AppPackagePickerUiStateTest`、`AddPasswordViewModelTest`、`DetailReducerTest` 和 `DetailViewModelTest` 共 70 项通过；AndroidTest Kotlin 编译与 `verifyArchitecture` 通过。该结果沿用原计划记录的历史验收，未在本次复核重跑。
- 修订历史工作流迁移：`ObserveEntryRevisionMetadataUseCase` 负责先初始化全库历史，再提供当前条目的 metadata Flow；`DetailRevisionWorkflow` 保留页面 token、generation、失败投影和重试。原维护记录报告 UseCase 顺序/初始化失败/取消用例、Detail 初始化失败后重试用例与 `DetailViewModelTest` 定向类通过；本记录本次未重跑。Task 83 仍需验证 session lock、owner 替换迟到结果、metadata Flow 后续失败重订阅与重复初始化幂等性。
- Detail Favicon preparation identity 已改为直接以 coroutine `Job` 作为 token，移除重复 sealed identity；`DetailFaviconSessionTest` 13 项通过，Release Kotlin 编译、Debug unit test 源码编译与 `verifyArchitecture` 通过。该历史记录未在本次复核重跑；图片 staging/promote 与条目保存流程是否提取仍由 Task 83 审查。
- 旧符号、生产调用方、测试、DI、导航、资源与正式文档引用已完成静态闭包扫描。
- 以上结果记录于 2026-11-08 的审查快照；本记录不代表之后代码变更已经重新验证。

## 2026-11-10 Vault 状态归属补充

Detail 标签编辑以 `DetailTagEditorState` 作为 Route/Composable 消费的唯一状态；标签 chips 从草稿与候选派生。列表 OTP 使用非空 `VaultOtpKindUiModel` 和互斥 `VaultOtpUiState`；OTP 表单由 `OtpFormState` 投影 HOTP counter 与 Steam 参数编辑能力。URI 成功反馈由 `uriAppliedFeedback` 表达，不保留未使用的失败事件分支。当前行为合同已写入[编辑与写入](../../product/vault/entry-editor-and-persistence.md)、[列表与搜索](../../product/vault/list-and-search.md)、[OTP 创建](../../product/vault/otp-entry.md)及[OTP URI](../../product/vault/otp-uri.md)。

原维护计划记录这些改动的生产、单测与 AndroidTest 源集编译及 `verifyArchitecture` 通过，测试未运行；本记录本次只核对当前声明和专题文档，没有重跑这些命令。Vault 的剩余 Route、Trash、导航状态和跨层依赖审计仍开放，由主维护计划 Task 32、42、83、84 跟踪。

## 后续事项

跨 Feature 的 ViewModel 职责审计仍由维护 TODO 的 Task 83 跟踪；本次 Vault 页面审查只关闭其 Vault 子范围，不代替 Task 83 的整体验收。

## 代码位置

| 职责 | 代码 |
| --- | --- |
| Vault 列表状态与命令 | [`VaultViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/list/VaultViewModel.kt)、[`VaultEntryCommandWorkflow.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/list/VaultEntryCommandWorkflow.kt) |
| Detail 页面协调与修订工作流 | [`DetailViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/DetailViewModel.kt)、[`DetailRevisionWorkflow.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/history/DetailRevisionWorkflow.kt) |
| 修订历史初始化与 metadata 订阅顺序 | [`ObserveEntryRevisionMetadataUseCase.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/history/ObserveEntryRevisionMetadataUseCase.kt)、[`ObserveEntryRevisionMetadataUseCaseTest.kt`](../../../app/src/test/java/com/aozijx/passly/feature/vault/history/ObserveEntryRevisionMetadataUseCaseTest.kt) |
| Detail Favicon preparation/save 资源与 Job owner | [`DetailFaviconSession.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/DetailFaviconSession.kt)、[`DetailFaviconSessionTest.kt`](../../../app/src/test/java/com/aozijx/passly/feature/vault/presentation/detail/DetailFaviconSessionTest.kt) |
| 编辑器写入与失败提示 | [`EditorWriteSession.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/editor/EditorWriteSession.kt)、[`EditorSaveEffectHandler.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/editor/EditorSaveEffectHandler.kt) |
| 应用关联选择器状态投影与页面 owner | `AppPackagePickerUiState.fromInstalledApps()`、`search()`；`DetailViewModel.loadPackagePickerApps()`、`AddPasswordViewModel.loadAssociatedApps()` | [`AppPackagePickerUiState.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/common/AppPackagePickerUiState.kt)、[`DetailViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/DetailViewModel.kt)、[`AddPasswordViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/editor/password/AddPasswordViewModel.kt) |
| 应用页面导航 | [`PasslyAppNavigation.kt`](../../../app/src/main/java/com/aozijx/passly/presentation/PasslyAppNavigation.kt) |
