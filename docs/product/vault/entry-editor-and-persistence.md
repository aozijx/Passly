# 条目编辑器与写入架构

状态：当前实现。本文记录生产代码的职责与已知缺口，不把未实现的接口当作依赖边界。

## 创建与详情编辑

详情标签提交使用 Domain 的 EntryTagPolicy 执行拆分、去重、排序与数量/长度校验；输入建议及可编辑草稿状态属于 Presentation。编辑弹层由互斥的 `DetailTagEditorState.Closed/Open` 统一持有；`Open` 包含初始标签、草稿、候选、输入、校验及放弃确认，chips 状态从同一草稿/候选派生。Domain 校验结果在 Presentation 映射为资源化的单数/复数反馈，Composable 不维护第二份标签编辑状态。

当前独立创建页支持 Login、OTP、Bank Card、Wi-Fi。详情页为这四种类型提供完整表单编辑，并继续为其余类型
提供按一个逻辑字段提交的编辑方式。完整表单复用创建表单及类型验证；更新只替换表单拥有的字段，保留身份、
Profile/Secret 中其余数据和未揭示字段。完整编辑不覆盖用户自定义字段。

| 层 | 当前职责 | 主要入口 |
| --- | --- | --- |
| Domain | 字段定义、草稿类型、标签及凭据校验、物化、单字段及完整表单应用 | `EntryTypeDefinitions`、`EntryTagPolicy`、`EntryDraftMaterializer`、`EntryFieldEditor`、`EntryFormEditor`、`EntryEdit` |
| App integration | 跨功能创建能力、会话检查、日志和 Repository 编排；编辑读取/更新用例 | `CreateEntryUseCase`、`LoadEditableEntryUseCase`、`EditEntryUseCase` |
| App presentation | 类型专属表单、敏感内容回填、状态、Action、ViewModel、Route 和 Compose 内容 | `feature/vault/presentation/editor/<类型>`、`presentation/detail` |
| Data | 事务、乐观锁、高敏字段保留/替换/清除、加密、修订和活动写入 | `UpdateEntryExecutor`、`SecretFieldStore`、`EntryRevisionWriter` |

Domain 不依赖 Android、导航或表单组件；物化器不生成身份、不访问 Repository。
Presentation 经用例或 Domain 端口访问数据，不直接使用 DAO。完整模块边界见[包与模块职责](../../platform/architecture/package-boundaries.md)。

## 新建链路

```text
类型 Action -> ViewModel 的表单快照 -> toEntryDraft()
  -> EntryCreation -> CreateEntryUseCase -> EntryDraftMaterializer -> EntryValidation
  -> EntryCommandRepository.createEntry -> Data 事务
```

Domain 将创建权限拆成两个合同：[`EntryCreation`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/port/EntryCreation.kt)
只暴露 `invoke(draft: EntryDraft)`，供 Vault 表单和扫码 OTP 导入使用；[`AutofillEntryCreation`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/autofill/port/AutofillEntryCreation.kt)
暴露 `createFromAutofill(draft: EntryDraft, inboxDraftId: AutofillInboxDraftId?)`，由 Autofill 流程使用。App 根组合中的
[`CreateEntryUseCase`](../../../app/src/main/java/com/aozijx/passly/app/entry/creation/CreateEntryUseCase.kt)
同时实现两者，并由 [`EntryCreationModule`](../../../app/src/main/java/com/aozijx/passly/app/entry/creation/EntryCreationModule.kt)
分别绑定到对应 port。

普通 `invoke(draft)` 将 `secureSessionAccessState::hasFullSecureSessionAccess` 和 `entryCommandRepository::createEntry`
传给共同的 `observeCreate(...)`；`createFromAutofill(draft, inboxDraftId)` 则传 `hasAutofillSessionAccess`，并按
`inboxDraftId == null` 选择 `autofillEntryWriter.create(entry)` 或 `createFromInbox(entry)`。Inbox draft ID 会先映射成稳定条目 ID，
让 Inbox 重试具有幂等身份。共同的 `create(...)` 在生成 ID/时间前检查 required 与 `invalidProvidedFields`，随后物化并调用
`EntryValidation.validate(entry)`；预期字段错误返回 `ValidationError`，通过后才调用对应 `persist(entry)`。因此两类入口共享纯校验与物化，
但保留不同的 session access、ID 策略和写入端口。

[`EntryDraftMaterializer`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/policy/EntryDraftMaterializer.kt)
位于 Domain，目前显式转换四种创建类型。它再次检查草稿字段及身份类型；不能假定草稿一定经 `withValue()` 构造。
无效草稿以字段键表达失败，不能把用户输入拼入异常。`EntryValidation` 仍负责最终完整凭据校验。

`EntryTypeDefinitions` 是所有 `EntryType` 的领域字段目录，按类型拆分定义文件，并在建立索引时检查重复、缺失和意外类型；
目录完整表示每种类型的字段元数据都有定义，不表示每种类型都支持创建或完整表单编辑。创建物化仍只实现 Login、OTP、
Bank Card、Wi-Fi 四类，其余类型明确拒绝。新增类型时分别更新字段定义和实际支持的用例/表单，不用完整目录代替能力闭环，
也不为每个类型添加只转发的物化器策略类。

草稿只描述待创建内容；身份与初始版本由创建用例生成。
已有条目通过 `EntryEdit` 与带预期版本的 Repository 更新合同修改，条目 ID 与版本由更新用例和端口参数表达。

## 四类创建表单

| 类型 | 状态与映射 | 专属规则 |
| --- | --- | --- |
| Login | `AddPasswordFormState` / `AddPasswordUiState`、`PasswordFormMapper.kt` | 应用选择用 `AssociatedAppSelection.NotSelected/Selected` 显式表示；展示投影区分未选、已安装与未解析包名，Domain draft 映射负责省略未选 ID；网站写入 `PRIMARY_URL`，密码保留原值 |
| OTP | `OtpFormState` / `AddOtpEditorUiState`、`OtpFormMapper.kt` | `OtpConfigInput` 与扫码配置共用 Domain 校验；编辑状态由 `uiState` 显式暴露，URI 反馈只表达成功应用 |
| Bank Card | `AddBankCardFormState` / `AddBankCardUiState`、`BankCardFormMapper.kt` | 卡类型与有效期选择用 `Unselected/Selected`、`NotSet/Selected` 显式建模；仅在映射到 Domain draft 时省略未设置值 |
| Wi-Fi | `AddWifiFormState` / `AddWifiUiState`、`WifiFormMapper.kt` | 网络名原样保留并同时作为初始标题；Presentation 投影安全类型文案、密码栏可见性与行内校验反馈，Domain 仍负责校验；密码及隐藏标记一起转换，扫码种子只初始化一次 |

每类 ViewModel 暴露不可变 `uiState`，保留类型专属表单和 `Idle / Saving` 展示阶段。
共享的 `EditorWriteSession` 持有保存任务，驱动保存、失败恢复和清空回调；保存取点击时的表单快照，期间拒绝表单更新和重复保存。
成功清空表单并发送 `EditorSaveEffect.Saved`，失败回到可重试状态。成功事件不包含条目 ID；宿主按保存成功回调离开创建流程。

`EditorWriteSession.save { draft }` 将普通 `AppResult.Failure` 或普通 `Exception` 映射为 `EditorSaveEffect.Failed` 并调用 `onIdle()`；`CancellationException` 仅在相同认证 owner 仍有效时恢复 Editing/Idle，否则清空，然后继续传播。fatal `Error` 不映射为普通失败：若保存 attempt 仍是当前 owner，session 会关闭为不可重新激活的终态、清空表单并停止 effects，再原样重抛 Error；清理异常作为 suppressed failure 附加，不替换原错误。若 attempt 已被锁定/关闭流程接管，不重复清理已失效的状态。

Route 按生命周期收集状态；Screen 接收状态和 Action。`EditorSaveEffectHandler` 消费保存结果并调用宿主回调或显示失败提示。
焦点、动画和展开状态留在 Compose；字段值、扫码结果和保存状态回到表单状态。敏感输入不进入导航参数或 SavedState。

`EditorWriteSession` 观察认证状态，将表单和保存尝试绑定到当前完整认证对象；锁定或认证归属变化先使尝试失效，再取消保存并清空表单。
迟到结果不能清空新表单或发出新保存效果。缓冲效果在消费时再次核对归属，新的编辑或重试会淘汰旧效果；只保留最近一次结果。
关闭 ViewModel 后协调器不能重新激活。取消继续传播，不生成失败效果；独立取消且认证仍有效时恢复可编辑状态。
取消或丢弃 UI 结果不撤销已经提交的数据库事务。类型专属的应用列表加载、扫码初始化与事件仍由对应 ViewModel 管理，不能把保存协调器当作整个表单的生命周期实现。

Login 的应用列表加载先经过编辑会话守卫，并在查询前后核对同一完整认证对象；查询返回后继续检查协程取消状态。
清空表单会取消持有的目录请求，迟到结果不能重建已清空缓存或覆盖新请求。目录查询的普通异常保持可重试状态；取消继续传播，fatal `Error` 不会被降级为空列表。
详情页的关联应用选择器在目录查询普通异常时显示可重试失败提示；成功后清除失败状态。失败反馈绑定当前条目访问 token，条目切换后的迟到失败不会污染新页面；取消继续传播，fatal `Error` 不映射为普通加载失败。

专属细节见[银行卡资料与有效期](card-entry.md)、[OTP 创建](otp-entry.md)及[Wi-Fi / OTP 二维码](qr-display.md)。

保存反馈统一为不携带输入、异常或错误码的 `Saved / Failed` 事件。各 Route 提供对应类型的本地化失败文案，
普通领域/存储失败和未知异常都不把内部错误码直接显示给用户；诊断仍由创建用例的结构化日志记录。
`EditorSaveEffectHandler` 仅在 Route 生命周期达到 `STARTED` 时收集。停止时取消未结束的 Snackbar，
恢复后消费仍有效的新结果；已经消费的提示不重放。Snackbar 是生命周期下的独立子任务，新的保存结果会请求取消旧提示，
不等待旧提示的取消清理完成；成功回调在结果收集路径同步执行，避免因提示清理而延后导航或认证归属检查。
会话的认证身份过滤在实际收集时执行，后台锁定或换会话后的旧反馈不能恢复导航。

密码、Wi-Fi 密码及银行卡的卡号、付款 PIN、CVV 显隐使用同一
`SensitiveFieldVisibilityButton`。控件采用 M3 `IconToggleButton`，以中英文资源表达“显示/隐藏某字段”、
当前可见状态和选中状态；只接收字段标签资源，不接收凭据值。可见性仍由各自表单及 Action 管理，
控件不缓存密码或另存一份可见状态。文本框的遮罩和输入内容继续由页面状态驱动。

## 已有条目的逐项编辑

[`EditEntryUseCase`](../../../app/src/main/java/com/aozijx/passly/feature/vault/entry/EditEntryUseCase.kt)
在互斥区内读取最新条目，经 Domain `EntryEdit.applyTo()` 得到类型化结果，再提交 `EntryUpdate` 和预期版本。
无变化不写入；版本冲突最多重新读取并重试一次。失败返回明确错误，不把缺失字段补成默认凭据。

`EntryFieldEditor` 只校验本次替换字段，保留其他未揭示字段；不能对删去高敏值的详情模型执行整条创建校验。
Data 根据真实高敏行键维护能力标记，按 Preserve / Clear / Replace 合同处理敏感字段。
映射与存储边界见[数据映射](../../platform/data/mapping.md)。

`DetailViewModel.load()` 为每次详情访问调用 `DetailWorkSession.open(entryId)`，`clearSensitiveState()` 关闭访问；同一 Active token 供 ViewModel、`DetailPasswordRelationsWorkflow` 与 `DetailRevisionWorkflow` 共享。Session 只持有访问身份，并用 `isCurrent()` 同时检查 token 身份和安全会话授权，不拥有页面 UI 状态或这些工作流的 Job。异步结果通过该访问凭据发布。内联草稿在提交后发生修改或重开时，旧完成保留当前编辑；
标签提交期间冻结草稿。图标资源另由 `DetailFaviconSession` 与引用检查回收端口管理，不能因页面关闭直接删除已提交文件。
取消图标裁剪时，即使详情 ViewModel 同时销毁，正在执行的临时输入删除也会在不可取消区完成；未完成的临时文件另由进程启动时的 staging 清理兜底。
这些都是现有详情机制，不代表四类创建页已经共享统一 Session。

Favicon 编辑状态和 Action 仍归 `DetailViewModel`：它发起上传/URL staging、图片处理和保存，并把结果映射回详情 reducer。`DetailFaviconSession` 独立拥有 preparation/save Job、staged/promoted 文件资源和关闭清理；`DetailWorkSession.persist()` 使用当前 Detail visit token 提交 `EntryEdit.SetIcon`，仅在 `EditEntryUseCase` 返回持久化成功后调用 `onCommitted` 将 promoted candidate 从临时资源集合中移除。若页面 token 已过期，promote 后的 candidate 会在不可取消区回收。该边界不再额外包装成 UseCase 或 Coordinator：图片转换是平台能力，条目更新已有完整 UseCase，页面草稿和提交 owner 也都已有明确归属。

详情复制会把当前访问凭据传到复制用例，并在异步读取结束后、剪贴板平台写入前再次核对。剪贴板写入器若需先挂起读取保留策略，必须在该读取后核对凭据，再执行同步的平台写入；已关闭或切换的详情不能把旧结果写入剪贴板。

## 详情页密码关联

已保存且活动的 Login 详情可以通过关联密码组添加其他活动 Login。候选搜索只展示标题和用户名摘要，不读取密码明文。账户组与其他关系仍保留在可导航的关联条目组中；直接的 LOGIN↔LOGIN `RELATED_TO` 关系单独显示，避免解绑后因账户组仍包含同一条目而看起来没有变化。

左滑直接密码行会打开确认框。确认前会按当前详情重新验证两端仍是活动 Login，并核对所选 `RELATED_TO` link ID 及对称端点；确认后只调用现有 `EntryLinkRepository.delete(linkId)` 删除这条关系，两个密码项都保留。现有 Room 删除事务继续负责关系修订快照和附件清理；不增加表、字段、迁移、DAO 或 Repository 契约，也不更改备份格式。

取消确认不改变关系。删除失败会保留关系和可重试确认框；每次尝试后刷新当前详情关系投影。关联写入与解绑由当前详情页和安全会话持有，页面替换或会话撤销会取消旧任务并抑制迟到结果。普通操作异常由所属服务记录；关系投影刷新失败由详情工作流记录。

唯一未完成的设备验收见[详情页密码关系计划](../../project/work/plans/2026-11-08-related-password-management.md)。

## 应用关联选择器

详情编辑与 Password 创建页各自持有已安装应用目录请求和搜索状态，共用不可变的 `AppPackagePickerUiState<T>` 结果合同及无状态 `AppPackagePickerBottomSheet`。状态显式区分 `NotRequested`、`Loading`、`Failed`、`NoApps`、`NoMatches` 和 `Matches`。搜索完成态 `NoMatches` 与 `Matches` 都保留完整目录及原始查询文本；`Matches` 另保存当前搜索结果，因此搜索过滤不会改变已选应用是否仍安装的判断。搜索按应用标签或包名忽略大小写，去除查询首尾空白用于匹配，但保留原输入以供界面显示。

普通目录读取异常进入 `Failed` 并提供重试；取消继续传播，fatal `Error` 不降级。Detail 请求以当前条目 `DetailWorkSession` token 约束，Password 请求以当前认证状态约束；条目、认证 owner 替换或 ViewModel 清除后，旧结果不能覆盖新状态。BottomSheet 只渲染传入状态并回传查询、重试和选择事件，不保存第二份搜索查询。

## 四类完整表单编辑

详情页对 Login、OTP、Bank Card、Wi-Fi 提供完整表单编辑，其余类型仍逐项编辑。加载、认证后的一次性 seed 交接、保存及字段保留规则见[完整表单编辑](full-form-editing.md)。

详情保存遇到非预期异常时，当前访问仍有效就转为可重试失败状态，避免界面永久停在 Saving；协程取消继续传播。异常也可能发生在数据库已提交、详情展示刷新失败之后，因此失败提示不等于保证事务回滚；重新打开详情会从持久化真相源重新加载。

条目创建、编辑、修订恢复、关系写入与回收站删除的附件/图标回收均发生在数据库提交之后。普通回收异常只记录结构化诊断，
不把已提交写入报告成数据库失败；附件 `PENDING_GC` 在授权解锁期间按每页最多 64 条稳定分页并排空，普通文件/数据库故障按有上限的指数退避继续重试；会话封存立即停止后续数据库操作，下次会话恢复后从 durable 队列继续。图标队列保留待处理路径并在图片处理器重建时补扫。
编辑器保存仍由对应 ViewModel owner 执行。扫码 OTP 确认后，导入任务由 Activity 作用域的
`ScannedOtpImportViewModel` 持有；关闭 Scanner 或离开 Vault 只取消结果等待，不取消已确认的保存。页面重新进入后应重新读取条目或列表状态，以持久数据确定结果。

## 创建入口与能力边界

[`VaultEditorDestinationRegistry`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/navigation/VaultEditorDestinationRegistry.kt)
目前只映射 Password、TOTP、Bank Card、Wi-Fi 四个导航目的地。不支持的请求返回 `UnsupportedDestination`，不回退到 Login。
注册表从目的地映射派生 `creatableTypes`，由列表 UI model 传入 FAB 和创建弹层；快捷项也与该列表取交集。
组件只渲染传入的创建选项，不自行遍历类型枚举。筛选继续使用独立的全量选项，读取、导入支持的类型不等于可创建类型。
不支持的请求不会进入备用表单；创建入口只展示注册表提供的受支持类型。

新增可创建类型应同时核对字段定义、强类型表单与映射、Domain 物化和校验、事务存储、详情读取及备份往返；
声明 Autofill、扫码等能力时，再验证对应集成。增加注册表只能解决实际的路由或组合需求，不能替代能力闭环。

## 当前缺口与验证

- 创建页已共享保存协调器，表单更新、校验和类型专属异步能力仍由类型 ViewModel 持有；没有泛型 `EntryEditorSession`、`EntryFormAdapter`、`EntryEditorRegistry` 或 `EntryMaterializerRegistry` 实现。
- 集中式物化器目前只有四个创建分支；需要扩展类型时按实际语义调整，不把每个分支强制包装成空转发类。
- 完整表单编辑目前只覆盖上述四种既有创建类型；其余 EntryType 仍由详情逐项编辑，不存在通用表单或类型插件注册系统。

验证以现有测试为准：`LoadEditableEntryUseCaseTest`、`EditEntryUseCaseTest`、四类表单映射及 ViewModel 测试覆盖完整编辑；Domain 的
`EntryDraftMaterializerTest`、`EntryEditTest`、`EntryFormEditorTest`、`EntryFieldEditorTest` 覆盖纯规则；详情状态测试覆盖逐项更新与发布边界。
这些测试不证明未支持类型的完整编辑或真实设备交互已经完成。
`EditorWriteSessionTest` 覆盖保存去重、锁定与重新认证、迟到结果、缓冲效果淘汰、取消和销毁边界；Wi-Fi 创建测试通过实际用例复现锁定期间的保存窗口。
`VaultListUiMapperTest` 与 `VaultNavigationTest` 检查创建选项均有目的地、筛选能力独立保留；
`AddEntryBottomSheetTest` 提供按传入选项渲染和转发选择的仪器用例，编译通过不等于设备运行通过。
修改实现时按[测试要求](../../engineering/development/testing.md)运行相关模块、App 门禁及所需仪器测试。

## 代码位置

| 职责 | 类/函数 | 源码文件 |
| --- | --- | --- |
| Vault/扫码 OTP 创建能力合同 | `EntryCreation.invoke(draft: EntryDraft)` | [`EntryCreation.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/port/EntryCreation.kt) |
| Autofill 创建能力合同 | `AutofillEntryCreation.createFromAutofill(draft, inboxDraftId)` | [`AutofillEntryCreation.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/autofill/port/AutofillEntryCreation.kt) |
| 分别绑定两个创建 port | `bindEntryCreation(implementation)`、`bindAutofillEntryCreation(implementation)` | [`EntryCreationModule.kt`](../../../app/src/main/java/com/aozijx/passly/app/entry/creation/EntryCreationModule.kt) |
| 创建普通条目及 Autofill 条目 | `invoke(draft)`、`createFromAutofill(draft, inboxDraftId)` -> `observeCreate(draft, trigger, hasAccess, persist, forcedId)` | [`CreateEntryUseCase.kt`](../../../app/src/main/java/com/aozijx/passly/app/entry/creation/CreateEntryUseCase.kt) |
| 将新建草稿转换为可持久化条目 | `EntryDraftMaterializer.materialize()` | [`EntryDraftMaterializer.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/policy/EntryDraftMaterializer.kt) |
| 登记所有条目的字段定义并检查目录覆盖 | `EntryTypeDefinitions`、`buildEntryTypeDefinitionIndex()` | [`EntryTypeDefinitions.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/policy/EntryTypeDefinitions.kt)、[`EntryTypeDefinitionRegistry.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/policy/EntryTypeDefinitionRegistry.kt) |
| 将完整表单应用到已有条目并保留未管理状态 | `EntryFormEditor.apply()`、`EntryEdit.ReplaceForm` | [`EntryFormEditor.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/policy/EntryFormEditor.kt)、[`EntryEdit.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/policy/EntryEdit.kt) |
| 编辑详情条目与完整表单 | `EditEntryUseCase.edit(entryId, edit)` | [`EditEntryUseCase.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/entry/EditEntryUseCase.kt) |
| Favicon 编辑 Action、图片准备/处理与条目图标保存协调 | `DetailViewModel.stageFaviconInput()`、`processPendingFavicon()`、`saveFavicon()` | [`DetailViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/DetailViewModel.kt) |
| Favicon 准备/保存 Job、staged/promoted 资源 owner 与关闭清理 | `DetailFaviconSession.launchPreparation()`、`publishPreparation()`、`launchSave()`、`close()` | [`DetailFaviconSession.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/DetailFaviconSession.kt) |
| Detail visit 有效性检查、`EntryEdit` 提交与持久成功后的 UI 发布 | `DetailWorkSession.persist(token, edit, completion, onCommitted)` | [`DetailWorkSession.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/DetailWorkSession.kt) |
| 保存任务、owner、取消与 fatal Error 清理 | `EditorWriteSession.save()`、`close()` | [`EditorWriteSession.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/editor/EditorWriteSession.kt) |
| 获授权读取完整编辑表单 | `LoadEditableEntryUseCase.invoke()` | [`LoadEditableEntryUseCase.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/detail/LoadEditableEntryUseCase.kt) |
| 编辑认证后的一次性表单交接 | `EntryEditorSeedStore`、`EntryEditorSeedCoordinator` | [`EntryEditorSeedStore.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/entry/EntryEditorSeedStore.kt) |
| 按条目类型选择创建/完整编辑目标 | `VaultEditorDestinationRegistry.routeFor()`、`editRouteFor()` | [`VaultEditorDestinationRegistry.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/navigation/VaultEditorDestinationRegistry.kt) |
| 注册详情与四个编辑目的地 | `registerVaultGraph()`、`registerEditorEditDestination()` | [`VaultGraphRegistration.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/navigation/VaultGraphRegistration.kt) |
| 通过 Domain `EntryDetailQuery` 获取条目、敏感字段存在性与关系快照；App 实现单次 link 查询与关系投影 | `open()`、`snapshot()`、`reloadRelations()` | [`EntryDetailQuery.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/port/EntryDetailQuery.kt)、[`EntryDetailQueryImpl.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/detail/EntryDetailQueryImpl.kt)、[`EntryDetailData.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/model/query/EntryDetailData.kt)、[`VaultDetailDomainBindingsModule.kt`](../../../app/src/main/java/com/aozijx/passly/app/entry/di/VaultDetailDomainBindingsModule.kt) |
| 通过 Domain `RelatedPasswordOperations` 查找候选并校验绑定/解绑的活动 Login 与关系 ID；App 实现并在组合根绑定 | `candidates()`、`bind()`、`unlink()` | [`RelatedPasswordOperations.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/port/RelatedPasswordOperations.kt)、[`RelatedPasswordOperationsImpl.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/detail/RelatedPasswordOperationsImpl.kt)、[`VaultDetailDomainBindingsModule.kt`](../../../app/src/main/java/com/aozijx/passly/app/entry/di/VaultDetailDomainBindingsModule.kt) |
| 持有确认状态、页面/会话取消和解绑后刷新 | `DetailPasswordRelationsWorkflow.requestUnlink()`、`confirmUnlink()` | [`DetailPasswordRelationsWorkflow.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/DetailPasswordRelationsWorkflow.kt) |
| 展示关系分组、绑定选择器和解绑确认 | `RelatedEntriesSection()`、`RelatedPasswordPickerSheet()` | [`RelatedEntriesSection.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/ui/component/RelatedEntriesSection.kt)、[`RelatedPasswordPickerSheet.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/ui/component/RelatedPasswordPickerSheet.kt) |
| 维护详情标签弹层的单一状态及草稿、校验和 chips 投影 | `DetailTagEditorReducer.reduce()`、`DetailTagEditorState`、`DetailEditorOverlays()` | [`DetailTagEditorReducer.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/DetailTagEditorReducer.kt)、[`DetailTagEditorState.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/DetailTagEditorState.kt)、[`DetailEditorOverlays.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/ui/DetailEditorOverlays.kt) |
| 共享应用目录状态与搜索投影 | `AppPackagePickerUiState`、`fromInstalledApps()`、`search()`、`mapItems()` | [`AppPackagePickerUiState.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/common/AppPackagePickerUiState.kt) |
| Detail 与 Password 各自持有目录加载/失败/重试 | `DetailViewModel.loadPackagePickerApps()`、`AddPasswordViewModel.loadAssociatedApps()` | [`DetailViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/DetailViewModel.kt)、[`AddPasswordViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/editor/password/AddPasswordViewModel.kt) |
| 渲染互斥 picker 状态并转发查询/重试/选择事件 | `AppPackagePickerBottomSheet()` | [`AppPackagePickerBottomSheet.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/common/ui/AppPackagePickerBottomSheet.kt) |
