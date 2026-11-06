# Legacy Autofill Service

状态：当前实现（API 31+，代码核对：2026-11-06）。

`LegacyAutofillService` 解析 `AssistStructure`、处理取消信号并构造 Legacy `FillResponse`/`Dataset`。它不构造
Credential Manager response。

## Fill 和 SaveInfo

`LegacyResponseFactory` 从已分类角色创建 `SaveInfo`：密码 IDs 为 required，用户名 IDs 为 optional；没有密码
字段时不广告保存。SaveInfo 使用 password data type，有用户名时增加 username type，并设置
`FLAG_SAVE_ON_ALL_VIEWS_INVISIBLE`。即使没有候选，也可以返回只含 SaveInfo 的 FillResponse。

发现结果只在 `LegacyResponseFactory` 映射一次，同时生成 `FillResponse` 和供 Service 记录日志、继承 interaction
trace 的 `LegacyDiscoveryObservation`。Service 不再重复解释 `Candidates`、`RequiresUnlock` 等 domain outcome。
Credential Manager 仍使用自己的 response 合同，不与 Legacy 结果类型合并。

Legacy 服务的 `onDisconnected()` 不是交互结束点：Android 可在 `FillCallback.onSuccess()` 后立即断开服务，随后才显示候选并等待用户选择。此时认证 `PendingIntent` 仍需读取原 interaction，因此断连回调不得过期 Legacy interaction。`onDestroy()` 只取消服务请求 scope，也不批量清理尚未完成的 interaction。

交互的正常完成点是最终认证结果交付给 Android 后。`AutofillResultDelivery` 在平台结果回调返回后记录 `autofill.result.delivered`，完成 selection、关闭交互并释放 Autofill 解锁租约；用户取消或系统没有继续调用认证入口时，由 `AutofillInteractionTimeout` 过期空闲交互。关闭是幂等的，重复终态转换不会重复释放租约。

Android 没有回调确认目标应用控件最终接受了值。`TYPE_DATASET_SELECTED` 表示数据集被选中；API 36 的 `onSessionDestroyed(FillEventHistory)` 表示 Autofill 会话结束，并提供会话事件历史。这些信号都不等同于目标字段写入成功，且 `onSessionDestroyed()` 在 API 31–35 不可用。因此 `autofill.result.delivered` 表示 Android 已收到认证结果，不应命名或解释为字段填充成功。

系统悬浮候选菜单使用 RemoteViews。业务层先生成 `AutofillRemoteItem.SingleLine/DoubleLine`，渲染器只负责选择对应
XML 并绑定文字；XML 不包含匹配类型徽标、自定义卡片背景或业务判断。这样候选、解锁入口和选择器入口共享同一
展示协议，而 domain 候选模型不会泄漏到 Android View 渲染器。

应用内 BottomSheet 由统一宿主 `AutofillAuthActivity` 承载。Manifest 将该 Activity 配置为 `Theme.Passly.AutofillFill`；日间与夜间主题都显式设 `windowIsFloating=false`，使 Compose 模态 BottomSheet 可按屏幕宽度和底边布局。该宿主也承载 Credential Manager 与无障碍入口。排查 Legacy BottomSheet 时按 `presentation.selected` → `discovery.completed` → 候选状态 → BottomSheet 渲染检查，不能只凭候选 Dataset 已构造就认定应用内选择器已显示。

## Autofill 诊断事件

Legacy 服务按处理阶段记录结构化摘要，便于区分 Android 是否调用服务、结构解析是否找到输入框、角色分类是否成功，
以及最终是否生成 Dataset 或 `SaveInfo`：

| 事件 | 诊断内容 |
| --- | --- |
| `autofill.legacy.fill_request_received` | 回调到达、FillContext 数量、请求 flags、Inline 请求是否存在 |
| `autofill.legacy.structure_parsed` | Fill/Save 阶段、窗口/节点遍历数量与截断状态、可编辑/有 hint/焦点/密码类型字段数量、包名和网页域是否存在 |
| `autofill.fields_classified` | 可编辑字段数量、用户名/密码/OTP 角色数量，以及是否识别出凭据字段 |
| `autofill.discovery.*` | 发现开始/完成或不可用原因；认证后的业务失败会附带枚举字段 `failure_kind`。凭据解析的普通异常会额外记录脱敏异常类型、应用内栈帧和 cause 类型，不记录异常消息或凭据 |
| `autofill.result.delivery_*`、`autofill.result.delivered`、`autofill.session.close_*` | 最终结果是否交给 Android，以及交付后的交互关闭状态；`result.delivered` 不证明目标控件已写入 |
| `autofill.legacy.response_built` | Android response 是否存在、Dataset 数量、`SaveInfo` 是否存在 |
| `autofill.legacy.save_request_received`、`autofill.save.*` | 保存回调是否到达及保存/排队/失败结果 |

`response_built.datasetCount` 统计实际写入 `FillResponse` 的 Dataset，包括锁定 BottomSheet 的认证占位 Dataset；系统菜单/inline 的 response 级认证不增加 Dataset 数量。该摘要由 `LegacyResponseFactory.result()` 从对应 builder 结果复制，再由 `LegacyAutofillDiagnostics.responseBuilt()` 记录。

事件记录目标包名的脱敏哈希；不记录网页域、节点 ID、字段文字、输入值或凭据。若复现后没有任何
`autofill.legacy.*` 回调事件，可先判断系统没有调用 Legacy 服务，或当前使用的是其他 Autofill 入口；若出现
`fill_request_received`，后续阶段计数和 `discovery.unavailable` 原因可定位中断点。诊断结构由
`AutofillLogSnapshot` 中的请求、遍历、字段、目标范围及响应摘要组成，日志语句不再接收易错的长参数列表。
诊断页面逐行显示完整持久化记录并自动换行；Logcat 单条长度上限为 3500 字符，先输出错误/异常、trace，再输出按关键字段优先级排序的上下文。超过上限仍会退化为带 `truncated=true` 的摘要，因此排查完整异常时应从诊断页面复制 trace 或导出 JSONL。

键盘 Inline 模式在服务元数据中声明支持，读取 `FillRequest.inlineSuggestionsRequest` 的最大数量和规格，
仅在键盘规格声明支持 AndroidX v1 模板时，为锁定入口与候选 Dataset 构造 `InlinePresentation`。请求为空、
容量为零、未给规格或模板版本不兼容时只提供菜单呈现；
悬浮模式始终只提供菜单呈现。认证后重建 response 的入口继续携带平台 Inline 请求。选择底部模式时，
认证占位 Dataset 打开应用内 `ModalBottomSheet`，不把 BottomSheet 伪装成 Inline。

## 认证结果目标与 BottomSheet 选择

Legacy 平台的认证结果按入口显式区分，不能复用一个含隐式布尔默认值的 Intent：

- 锁定的 `BOTTOM_SHEET` 入口使用需认证的占位 `Dataset`。认证后在 Passly 的 BottomSheet 选择条目，返回所选 `Dataset`，Android 随即填充字段；
- 锁定的系统菜单/inline 入口使用 `RESPONSE`，认证后返回重新构造的 `FillResponse`，由 Android 展示可用 Dataset；
- 单候选入口和已解锁保险库的底部候选选择器使用 `DATASET`，选择后直接向 Android 返回所选 `Dataset` 并触发填充。

结果目标沿 Intent 显式传递：`LegacyResponseFactory.buildUnlockResponse()` 调用
`LegacyAutofillResultTarget.forLockedPresentation(presentation)` 穷尽映射一次，并把同一个结果目标同时传给
`AutofillPendingIntentFactory.createUnlockIntent(..., resultTarget, ...)` 和协议 builder：
`BOTTOM_SHEET` 写入 `DATASET`，`FLOATING` 和 `SYSTEM_INLINE` 写入 `RESPONSE`。底部模式的锁定占位 Dataset 由
`createAuthenticatedPickerDataset()` 构造；系统/inline 模式用 `FillResponse.setAuthentication()`。
认证后，已解锁 BottomSheet 候选仍由 `buildCandidateResponse()`、`addPickerAuthentication()` 和 `createPickerIntent()` 构造
`DATASET` 认证占位；其他展示模式逐项调用 `addCandidateAuthentication()` 和 `createCandidateIntent()`，每个候选 Dataset
也使用 `DATASET`。

`AutofillFillRequestParser.toAutofillFillRequest()` 要求 extras 同时包含可解析的 interaction ID、包名、展示模式和结果目标；
展示模式由 `AutofillPresentation.fromWireKey()` 按显式 `wireKey` 查找，结果目标由 `LegacyAutofillResultTarget.fromWireValue()` 按显式 `wireValue` 查找；两者对未知或缺失值都返回 `null`，Parser 随后拒绝请求。当前两个反向映射都通过 enum `entries.firstOrNull` 查找，并非穷尽 `when`；稳定键仍为 `floating`、`system_inline`、`bottom_sheet`、`response`、`dataset`。映射形式的维护问题见[维护 TODO Task 85](../../superpowers/plans/2026-10-30-passly-maintainability-todo.md)。代码不读取旧的
`returnDataset` 标志作为兼容回退。认证选中后，
`AutofillAuthActivity.finishLegacyWithResult(request, result)` 以 `AutofillFillResult.SelectedCredential.target` 分支：`DATASET` 直接作为
`EXTRA_AUTHENTICATION_RESULT` 返回，并附带 `EXTRA_AUTHENTICATION_RESULT_EPHEMERAL_DATASET=true`；`RESPONSE` 则包装在
只含该 Dataset 的新 `FillResponse` 中。写结果前仍由 `AutofillResultDelivery.commitResult(receipt)` 执行终态交付与收尾。

候选 BottomSheet 位于 `:feature:autofill` 的 `presentation/legacy/ui/AutofillCandidateBottomSheet.kt`，由 Legacy
Fill Route 和 App 认证宿主共用。Route 将候选列表与目标应用包名分别传给 `candidates`、`targetAppPackageName`；
BottomSheet 通过 `rememberTargetApplicationIcon(targetAppPackageName)` 调用 `PackageManager.getApplicationIcon()`，
并以 `toBitmap(width = 96, height = 96).asImageBitmap()` 转为展示图标。`NameNotFoundException`、
`Resources.NotFoundException` 或 `IllegalArgumentException` 时返回 `null` 并隐藏图标；候选数据仍只提供
`AutofillCandidateSummary` 的标题、用户名和 `EntryId`，不依赖 App 的 `VaultItemIcon`，也不回退到 Passly 图标。

BottomSheet 直接调用 `LazyColumn.items(items = candidates, key = { candidate -> candidate.entry.id.value })`，
只在 Compose key 边界将 `EntryId.value` 作为稳定 `String` key；点击仍调用
`onCandidateSelected(candidate.entry.id)` 并保留领域 `EntryId`。Legacy Route 将参数封装为
`AutofillFillUiAction.CandidateSelected(entryId)`，ViewModel 再经 `ResolveAutofillSelection.invoke(interactionId, scope, entryId, includeOtp)`
验证当前 interaction 与已展示候选归属。此处没有额外的 LazyList key 转发 helper。

认证后继续 Legacy Fill 时，已有 `CandidatesReady` interaction 且请求带有 direct entry ID 的续接分支由 `DiscoverAutofillCandidates` 返回语义 `ResumeSelection`；Presentation 不读取具体 interaction store，也不自行判断状态。后续仍调用 `ResolveAutofillSelection`，由其重新检查 scope、interaction 状态和条目是否属于已展示候选。非 direct-entry discovery 仍走普通候选搜索。

Legacy Fill ViewModel 的终态只保留 discovery outcome 或选中凭据语义结果，不构造 Android `FillResponse`/`Dataset`。
Activity-retained ViewModel 持有一次性结果 claim，使配置重建后未确认的终态仍可交付，而已交付结果不会重放；Activity
使用当前 request 调用 `LegacyResponseFactory` 构造平台对象，并在 callback 成功后确认清除终态。凭据 payload 在交付前
暂存在结果状态中以支持 Activity 重建，交付失败时保留以供当前 claim 回滚后的取消收尾；成功后结果状态清空。选中条目
仍先由 `AutofillResultDelivery` 将平台结果交给 Android，再完成 interaction 与 lease 收尾。手工复测记录：API 36 AVD 上，Legacy 服务构造一个候选，系统断开服务后用户选择候选；认证后恢复 interaction，出现 `autofill.result.delivered` 与 `autofill.session.closed`，测试应用的账号和密码字段均被填入。此复测证明该路径可成功，不代表存在框架级字段写入成功回调，也不覆盖其他 OEM 或系统版本。

Android 的该 flag 表示所有 required views 不可见时可触发保存，不等同于 Passly 自己持久化或排队凭据。
`FLAG_DELAY_SAVE` 只把 FillContext 传递到同一 task 的后续 Autofill 请求，不能让 Passly 在未来解锁时主动取回
一份挂起凭据，因此不是 sealed Inbox 的替代实现。

## 当前 onSaveRequest

服务从最新 `FillContext` 重新解析并分类，不查找或重开已结束的 fill interaction。最低草稿要求是非空包名和至少
一个已分类非空密码；用户名可空，page title 当前固定为 null。

- 已有完整 `Authenticated` 会话：验证草稿后直接写入。
- 否则：将草稿密封写入耐久 Autofill Inbox；写入成功后调用 `SaveCallback.onSuccess()`，不启动认证 Activity。
  Vault 下次完整解锁后由 Inbox drainer 导入；回调表示已耐久排队。

平台依据：[Build autofill services](https://developer.android.com/identity/autofill/autofill-services)、
[FillResponse.Builder](https://developer.android.com/reference/android/service/autofill/FillResponse.Builder.html)、
[Dataset.Builder](https://developer.android.com/reference/android/service/autofill/Dataset.Builder)、
[SaveInfo](https://developer.android.com/reference/android/service/autofill/SaveInfo)、
[AutofillService lifecycle](https://developer.android.com/reference/android/service/autofill/AutofillService)、
[FillEventHistory.Event](https://developer.android.com/reference/android/service/autofill/FillEventHistory.Event)。

## 代码位置

| 职责 | 类/函数 | 源码文件 |
| --- | --- | --- |
| 处理旧版 Autofill 填充与保存请求 | `LegacyAutofillService.onFillRequest()`、`onSaveRequest()` | [`LegacyAutofillService.kt`](../../../feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/legacy/service/LegacyAutofillService.kt) |
| 将 Android 请求与解析结果转换为隐私安全的 Autofill 诊断事件 | `LegacyAutofillDiagnostics` | [`LegacyAutofillDiagnostics.kt`](../../../feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/legacy/service/LegacyAutofillDiagnostics.kt) |
| 记录认证后发现/选中失败的安全枚举诊断 | `AutofillFillViewModel` | [`AutofillFillViewModel.kt`](../../../feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/presentation/legacy/AutofillFillViewModel.kt) |
| 解析候选凭据并报告脱敏异常快照 | `RoomAutofillCredentialResolver.resolve()` | [`RoomAutofillCredentialResolver.kt`](../../../data/src/main/java/com/aozijx/passly/data/repository/autofill/RoomAutofillCredentialResolver.kt) |
| 交付平台结果后完成交互与租约收尾 | `AutofillResultDelivery.commitResult()` | [`AutofillResultDelivery.kt`](../../../feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/runtime/AutofillResultDelivery.kt) |
| 准备旧版保存流程 | `LegacySaveFlow.prepare()` | [`LegacySaveFlow.kt`](../../../feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/legacy/LegacySaveFlow.kt) |
| 构造锁定 response、候选 response 与 picker/candidate Dataset | `buildUnlockResponse()`、`buildCandidateResponse()`、`addPickerAuthentication()`、`addCandidateAuthentication()` | [`LegacyResponseFactory.kt`](../../../feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/legacy/service/builder/LegacyResponseFactory.kt) |
| 写入入口专属认证目标及候选身份 | `createUnlockIntent(...)`、`createPickerIntent(...)`、`createCandidateIntent(..., entryId, ...)`、`putCommonExtras(..., resultTarget, ...)` | [`AutofillPendingIntentFactory.kt`](../../../feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/legacy/AutofillPendingIntentFactory.kt) |
| 验证必需 extras 并解析结果目标、ID 和展示模式 | `Intent.toAutofillFillRequest()`、`LegacyAutofillResultTarget.fromWireValue(value)` | [`AutofillFillRequestParser.kt`](../../../feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/presentation/legacy/AutofillFillRequestParser.kt)、[`LegacyAutofillResultTarget.kt`](../../../feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/legacy/LegacyAutofillResultTarget.kt) |
| 统一认证宿主解析 Legacy action 并交付 DATASET / RESPONSE 结果 | `AutofillAuthActivity.parseLaunch(source)`、`finishLegacyWithResult(request, result)`、`responseFactory.buildDataset(payload, usernameIds, passwordIds, otpIds)`、`resultDelivery.commitResult(receipt) { ... }` | [`AutofillAuthActivity.kt`](../../../app/src/main/java/com/aozijx/passly/app/autofill/presentation/AutofillAuthActivity.kt) |
| 用条目稳定键渲染候选并以 `EntryId` 传递选择 | `LazyColumn.items(..., key = { candidate.entry.id.value })`、`onCandidateSelected(candidate.entry.id)`、`CandidateSelected(entryId)` | [`AutofillCandidateBottomSheet.kt`](../../../feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/presentation/legacy/ui/AutofillCandidateBottomSheet.kt)、[`AutofillFillRoute.kt`](../../../feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/presentation/legacy/AutofillFillRoute.kt)、[`AutofillFillViewModel.kt`](../../../feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/presentation/legacy/AutofillFillViewModel.kt) |
| 以目标应用包名读取候选选择器图标，缺失时隐藏 | `AutofillCandidateBottomSheet(..., targetAppPackageName, ...)`、`rememberTargetApplicationIcon(targetAppPackageName)` | [`AutofillCandidateBottomSheet.kt`](../../../feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/presentation/legacy/ui/AutofillCandidateBottomSheet.kt)、[`AutofillFillRoute.kt`](../../../feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/presentation/legacy/AutofillFillRoute.kt)、[`AccessibilityFillRoute.kt`](../../../feature/autofill/src/main/java/com/aozijx/passly/feature/autofill/presentation/accessibility/AccessibilityFillRoute.kt) |
| 密封收件箱的模型与处理流程 | [密封收件箱文档](sealed-inbox.md) | [sealed-inbox.md](sealed-inbox.md) |
