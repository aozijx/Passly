# Vault Presentation 边界审查记录

## 审查结论

本次审查覆盖 Vault 列表、详情、四种编辑器、回收站，以及 Route、导航和 seed/扫码交接。实现已完成；当前合同以 [列表与搜索](../../product/vault/list-and-search.md)、[编辑与写入](../../product/vault/entry-editor-and-persistence.md)、[完整表单编辑](../../product/vault/full-form-editing.md) 和 [二维码扫描结果交接](../../product/scanner/result-handoff.md) 为准。

页面 owner 按状态、任务、生命周期和资源职责划分。Vault 列表命令由 `VaultEntryCommandWorkflow` 管理；Detail 的 revision、favicon、QR、密码关联和访问页工作流保留在具有独立任务或资源合同的 owner；四类编辑器共享加载失败投影与写入 session，同时保留各自表单规则；Route 负责宿主生命周期、导航和平台 effect。内部调用方已迁移，旧路径与无消费者的转发层已清理。

## 验收证据

- Vault 单元测试：71 个测试类、472 个用例通过。
- `:app:compileDebugAndroidTestKotlin`、`verifyArchitecture`、`:app:compileDebugKotlin` 通过。
- 应用关联选择器状态迁移：`AppPackagePickerUiStateTest`、`AddPasswordViewModelTest`、`DetailReducerTest` 和 `DetailViewModelTest` 共 70 项通过；AndroidTest Kotlin 编译与 `verifyArchitecture` 通过。该结果沿用原计划记录的历史验收，未在本次复核重跑。
- 修订历史工作流迁移：`ObserveEntryRevisionMetadataUseCase` 负责先初始化全库历史，再提供当前条目的 metadata Flow；`DetailRevisionWorkflow` 保留页面 token、generation、失败投影和重试。原维护记录报告 UseCase 顺序/初始化失败/取消用例、Detail 初始化失败后重试用例与 `DetailViewModelTest` 定向类通过；本记录本次未重跑。2026-11-12 源码复核的当前覆盖与剩余缺口见下文。
- Detail Favicon preparation identity 已改为直接以 coroutine `Job` 作为 token，移除重复 sealed identity；`DetailFaviconSessionTest` 13 项通过，Release Kotlin 编译、Debug unit test 源码编译与 `verifyArchitecture` 通过。该历史记录未在本次复核重跑；当前 staging/promote 与条目保存 owner 复核见 2026-11-12 补充。
- 旧符号、生产调用方、测试、DI、导航、资源与正式文档引用已完成静态闭包扫描。
- 以上结果记录于 2026-11-08 的审查快照；本记录不代表之后代码变更已经重新验证。

## 2026-11-10 Vault 状态归属补充

Detail 标签编辑以 `DetailTagEditorState` 作为 Route/Composable 消费的唯一状态；标签 chips 从草稿与候选派生。列表 OTP 使用非空 `VaultOtpKindUiModel` 和互斥 `VaultOtpUiState`；OTP 表单由 `OtpFormState` 投影 HOTP counter 与 Steam 参数编辑能力。URI 成功反馈由 `uriAppliedFeedback` 表达，不保留未使用的失败事件分支。当前行为合同已写入[编辑与写入](../../product/vault/entry-editor-and-persistence.md)、[列表与搜索](../../product/vault/list-and-search.md)、[OTP 创建](../../product/vault/otp-entry.md)及[OTP URI](../../product/vault/otp-uri.md)。

原维护计划记录这些改动的生产、单测与 AndroidTest 源集编译及 `verifyArchitecture` 通过，测试未运行；本记录本次只核对当前声明和专题文档，没有重跑这些命令。Vault 的剩余 Route、Trash、导航状态和跨层依赖审计仍开放，由主维护计划 Task 32、42、83、84 跟踪。

## 2026-11-12 Trash Flow 恢复 owner 补充

`TrashViewModel` 将普通 `deletedEntries` 查询异常映射为可见的 `TrashContentState.Failed`；`RetryLoad` 只在失败态新建一次冷 Flow 订阅，之后继续接收仓库更新。每次订阅使用 ticket 隔离旧结果，页面关闭或 ViewModel 销毁时取消读取并清理页面状态；取消和 fatal `Error` 不作为可重试页面失败。合同与生产入口已补到[列表与搜索](../../product/vault/list-and-search.md#代码位置)。当前 `TrashViewModelTest` 源码覆盖首次及后续读取失败、连续重试、读取发出数据后的失败、取消、fatal 传播和销毁后的迟到数据；本次仅核对测试定义，未重跑测试。

## 2026-11-12 修订历史测试覆盖边界

当前 `DetailViewModelTest` 源码覆盖 metadata 订阅首次失败后的手动重试、历史初始化失败后再订阅、metadata fatal `Error` 传播，以及关闭并重开同一条目的历史弹层后丢弃旧观察者失败；`ObserveEntryRevisionMetadataUseCaseTest` 覆盖先初始化再请求 metadata Flow。Data 的 `EntryRevisionHistoryInitializationFlowTest` 覆盖已完成 generation 跳过重复重建，以及并发初始化在首个事务提交后复查 generation。上述为源码核对，本次未重跑测试。

仍未见 Detail revision metadata 发出数据后再失败并重新订阅、revision observer 活跃时会话锁定、替换条目或 Detail visit 后旧 metadata 结果被拒绝的对应 owner 测试。已完成的“重复初始化幂等”不再列为 Task 83 待办；剩余边界继续由[主维护计划 Task 83](../work/plans/2026-10-30-passly-maintainability-todo.md#task-83)跟踪。

## 2026-11-12 Favicon 编辑 owner 复核

`DetailFaviconSession` 已独立拥有图片准备/保存 Job、staged/promoted candidate 资源登记、迟到结果淘汰、取消裁剪清理，以及关闭时等待保存 Job 结束后释放资源。`DetailViewModel` 仍持有用户编辑草稿和 Action 映射；`stageFaviconInput()` / `processPendingFavicon()` 调用平台图片端口，`saveFavicon()` 先 promote，再通过当前 `DetailWorkSession` token 调用 `EditEntryUseCase`，在持久化成功回调中转移 promoted candidate 所有权。过期 token 时不会发布旧详情状态，promote 后的 candidate 在不可取消区回收。

据此保留现有边界，不新增只转发状态与回调的 Favicon Coordinator/UseCase：平台图片转换、页面草稿、Detail visit 与持久化各有明确 owner，新的包装层不能独立拥有详情 reducer 状态或提交 token。稳定合同已补入[条目编辑与写入专题](../../product/vault/entry-editor-and-persistence.md#已有条目的逐项编辑)。本次只做源码静态复核；`DetailFaviconSessionTest` 的历史 13 项结果不代表本次执行，DetailViewModel 层的图片处理与提交集成也未由此复核验证。全仓测试逻辑审查仍由[主维护计划 Task 74](../work/plans/2026-10-30-passly-maintainability-todo.md#task-74)跟踪。

## 2026-11-12 EditorWriteSession owner 审查

`AddPasswordViewModel`、`AddOtpViewModel`、`AddBankCardViewModel` 与 `AddWifiViewModel` 各自以 `viewModelScope` 创建一个 `EditorWriteSession`，并在各自 `onCleared()` 中关闭。Session 只拥有共享的保存尝试、认证身份检查、取消/fatal 收尾及最新结果交付；类型专属表单、校验、扫码初始化和目录加载仍由对应 ViewModel 持有，onClear callback 将共享保存结果投影回该表单状态。`EditorWriteSessionTest` 覆盖保存去重、认证锁定与重新认证、迟到结果、效果淘汰、取消和关闭边界；正式合同见[编辑与写入专题](../../product/vault/entry-editor-and-persistence.md#四类创建表单)。本次静态核对测试源码，未重跑。

保留当前共享类，不再为每个表单建立保存 Coordinator 或把类型专属状态搬进通用 Session：现有 Session 有真实跨类型保存合同，但没有拥有四种表单状态的职责。该项从 Task 84 剩余 Coordinator 审查清单移除；四个 ViewModel 其余业务与依赖边界仍待逐项检查。

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
