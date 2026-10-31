# 条目编辑器与写入架构

状态：当前实现。本文记录生产代码的职责与已知缺口，不把未实现的接口当作依赖边界。

## 创建与详情编辑

当前独立创建页支持 Login、OTP、Bank Card、Wi-Fi。详情页为这四种类型提供完整表单编辑，并继续为其余类型
提供按一个逻辑字段提交的编辑方式。完整表单复用创建表单及类型验证；更新只替换表单拥有的字段，保留身份、
Profile/Secret 中其余数据和未揭示字段。完整编辑不覆盖用户自定义字段。

| 层 | 当前职责 | 主要入口 |
| --- | --- | --- |
| Domain | 字段定义、草稿类型、纯校验、物化、单字段及完整表单应用 | `EntryTypeDefinitions`、`EntryDraftMaterializer`、`EntryFieldEditor`、`EntryFormEditor`、`EntryEdit` |
| App feature | 会话检查、敏感读取授权、冲突处理、日志和 Repository 编排 | `CreateEntryUseCase`、`LoadEditableEntryUseCase`、`EditDetailEntryUseCase` |
| App presentation | 类型专属表单、敏感内容回填、状态、Action、ViewModel、Route 和 Compose 内容 | `feature/vault/presentation/editor/<类型>`、`presentation/detail` |
| Data | 事务、乐观锁、高敏字段保留/替换/清除、加密、修订和活动写入 | `UpdateEntryExecutor`、`SecretFieldStore`、`EntryRevisionWriter` |

Domain 不依赖 Android、导航或表单组件；物化器不生成身份、不访问 Repository。
Presentation 经用例或 Domain 端口访问数据，不直接使用 DAO。完整模块边界见[包与模块职责](../../architecture/package-boundaries.md)。

## 新建链路

```text
类型 Action -> ViewModel 的表单快照 -> toEntryDraft()
  -> CreateEntryUseCase -> EntryDraftMaterializer -> EntryValidation
  -> EntryCommandRepository.createEntry -> Data 事务
```

[`CreateEntryUseCase`](../../../app/src/main/java/com/aozijx/passly/feature/vault/entry/CreateEntryUseCase.kt)
接收只含 `type` 和类型化字段值的创建草稿。它先检查当前权限，再检查必填字段和 `invalidProvidedFields`，生成身份后物化、
执行完整领域校验并写入 Repository。Autofill 创建复用同一纯规则，但使用自己的会话访问检查和写入端口。

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
| Login | `AddPasswordFormState` / `AddPasswordUiState`、`PasswordFormMapper.kt` | 网站写入 `PRIMARY_URL`，应用写入 `APPLICATION_IDS`；密码保留原值 |
| OTP | `OtpFormState` / `AddOtpCodeState`、`OtpFormMapper.kt` | `OtpConfigInput` 与扫码配置共用 Domain 校验 |
| Bank Card | `AddBankCardFormState` / `AddBankCardUiState`、`BankCardFormMapper.kt` | 卡号、有效期和可选银行资料使用各自规则 |
| Wi-Fi | `AddWifiFormState` / `AddWifiUiState`、`WifiFormMapper.kt` | 网络名原样保留并同时作为初始标题，安全类型、密码及隐藏标记一起转换；扫码种子只初始化一次 |

每类 ViewModel 暴露不可变 `uiState`，保留类型专属表单和 `Idle / Saving` 展示阶段。
共享的 `EditorWriteSession` 持有保存任务，驱动保存、失败恢复和清空回调；保存取点击时的表单快照，期间拒绝表单更新和重复保存。
成功清空表单并发送 `EditorSaveEffect.Saved`，失败回到可重试状态。当前成功 Effect 不携带条目 ID，不能按尚不存在的 `Saved(entryId)` 合同调用。

Route 按生命周期收集状态；Screen 接收状态和 Action。`EditorSaveEffectHandler` 消费保存结果并调用宿主回调或显示失败提示。
焦点、动画和展开状态留在 Compose；字段值、扫码结果和保存状态回到表单状态。敏感输入不进入导航参数或 SavedState。

`EditorWriteSession` 观察认证状态，将表单和保存尝试绑定到当前完整认证对象；锁定或认证归属变化先使尝试失效，再取消保存并清空表单。
迟到结果不能清空新表单或发出新保存效果。缓冲效果在消费时再次核对归属，新的编辑或重试会淘汰旧效果；只保留最近一次结果。
关闭 ViewModel 后协调器不能重新激活。取消继续传播，不生成失败效果；独立取消且认证仍有效时恢复可编辑状态。
取消或丢弃 UI 结果不撤销已经提交的数据库事务。类型专属的应用列表加载、扫码初始化与事件仍由对应 ViewModel 管理，不能把保存协调器当作整个表单的生命周期实现。

Login 的应用列表加载先经过编辑会话守卫，并在查询前后核对同一完整认证对象；查询返回后继续检查协程取消状态。
清空表单会取消持有的目录请求，迟到结果不能重建已清空缓存或覆盖新请求。正常加载失败仍保持可重试状态。

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

[`EditDetailEntryUseCase`](../../../app/src/main/java/com/aozijx/passly/feature/vault/detail/EditDetailEntryUseCase.kt)
在互斥区内读取最新条目，经 Domain `EntryEdit.applyTo()` 得到类型化结果，再提交 `EntryUpdate` 和预期版本。
无变化不写入；版本冲突最多重新读取并重试一次。失败返回明确错误，不把缺失字段补成默认凭据。

`EntryFieldEditor` 只校验本次替换字段，保留其他未揭示字段；不能对删去高敏值的详情模型执行整条创建校验。
Data 根据真实高敏行键维护能力标记，按 Preserve / Clear / Replace 合同处理敏感字段。
映射与存储边界见[数据映射](../../data/mapping.md)。

详情异步结果通过 `DetailWorkSession` 的独立访问凭据发布。内联草稿在提交后发生修改或重开时，旧完成保留当前编辑；
标签提交期间冻结草稿。图标资源另由 `DetailFaviconSession` 与引用检查回收端口管理，不能因页面关闭直接删除已提交文件。
取消图标裁剪时，即使详情 ViewModel 同时销毁，正在执行的临时输入删除也会在不可取消区完成；未完成的临时文件另由进程启动时的 staging 清理兜底。
这些都是现有详情机制，不代表四类创建页已经共享统一 Session。

详情复制会把当前访问凭据传到复制用例，并在异步读取结束后、剪贴板平台写入前再次核对。剪贴板写入器若需先挂起读取保留策略，必须在该读取后核对凭据，再执行同步的平台写入；已关闭或切换的详情不能把旧结果写入剪贴板。

## 四类完整表单编辑

详情页仅对 Login、OTP、Bank Card、Wi-Fi 展示完整表单编辑入口。路由只传 `entryId`；加载用例从 Repository
读取真实类型，目标不存在、类型不支持、授权拒绝、字段不完整或会话过期时不返回可保存表单，也不回退到其他类型。
Login、OTP 和 Bank Card 中实际存在的高敏字段会按字段存在集合一次性请求 `REVEAL` 授权；只有完整返回所有请求字段后
才回填表单，并记录一次 `VIEW` 活动。Wi-Fi 密码属于加密 bundle，由完整会话读取，不走高敏字段 reveal。

表单加载期间禁止编辑和保存。编辑器把加载结果绑定到当前完整认证对象；锁定、切换认证会话或 ViewModel 关闭时取消加载并清空表单。
保存使用 `EntryEdit.ReplaceForm`，更新用例重新读取最新条目、执行完整 Domain 校验，并通过预期版本写入及最多一次冲突重试提交。
表单不拥有的字段（包括图标、收藏、过期时间、域名关联、自定义字段及非表单关联）继续保留。失败不会清空用户编辑内容；成功后清空敏感表单并返回详情。

密码表单允许修改主关联应用，但回填和保存还会保留其他已有应用关联。Wi-Fi 编辑只使用编辑器回填，不触发扫码种子或创建初始化。
其余 EntryType 没有完整表单入口，仍使用上文的单逻辑字段编辑。

详情保存遇到非预期异常时，当前访问仍有效就转为可重试失败状态，避免界面永久停在 Saving；协程取消继续传播。异常也可能发生在数据库已提交、详情展示刷新失败之后，因此失败提示不等于保证事务回滚；重新打开详情会从持久化真相源重新加载。

## 创建入口与能力边界

[`VaultEditorDestinationRegistry`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/navigation/VaultEditorDestinationRegistry.kt)
目前只映射 Password、TOTP、Bank Card、Wi-Fi 四个导航目的地。不支持的请求返回 `UnsupportedDestination`，不回退到 Login。
注册表从目的地映射派生 `creatableTypes`，由列表 UI model 传入 FAB 和创建弹层；快捷项也与该列表取交集。
组件只渲染传入的创建选项，不自行遍历类型枚举。筛选继续使用独立的全量选项，读取、导入支持的类型不等于可创建类型。
不支持目的地的请求不会进入备用表单；旧的未渲染 `addType` 状态与分支已移除。

新增可创建类型应同时核对字段定义、强类型表单与映射、Domain 物化和校验、事务存储、详情读取及备份往返；
声明 Autofill、扫码等能力时，再验证对应集成。增加注册表只能解决实际的路由或组合需求，不能替代能力闭环。

## 当前缺口与验证

- 创建页已共享保存协调器，表单更新、校验和类型专属异步能力仍由类型 ViewModel 持有；没有泛型 `EntryEditorSession`、`EntryFormAdapter`、`EntryEditorRegistry` 或 `EntryMaterializerRegistry` 实现。
- 集中式物化器目前只有四个创建分支；需要扩展类型时按实际语义调整，不把每个分支强制包装成空转发类。
- 完整表单编辑目前只覆盖上述四种既有创建类型；其余 EntryType 仍由详情逐项编辑，不存在通用表单或类型插件注册系统。

验证以现有测试为准：`LoadEditableEntryUseCaseTest`、`EditDetailEntryUseCaseTest`、四类表单映射及 ViewModel 测试覆盖完整编辑；Domain 的
`EntryDraftMaterializerTest`、`EntryEditTest`、`EntryFormEditorTest`、`EntryFieldEditorTest` 覆盖纯规则；详情状态测试覆盖逐项更新与发布边界。
这些测试不证明未支持类型的完整编辑或真实设备交互已经完成。
`EditorWriteSessionTest` 覆盖保存去重、锁定与重新认证、迟到结果、缓冲效果淘汰、取消和销毁边界；Wi-Fi 创建测试通过实际用例复现锁定期间的保存窗口。
`VaultListUiMapperTest` 与 `VaultNavigationTest` 检查创建选项均有目的地、筛选能力独立保留；
`AddEntryBottomSheetTest` 提供按传入选项渲染和转发选择的仪器用例，编译通过不等于设备运行通过。
修改实现时按[测试要求](../../development/testing.md)运行相关模块、App 门禁及所需仪器测试。

## 代码位置

| 职责 | 类/函数 | 源码文件 |
| --- | --- | --- |
| 创建普通条目及 Autofill 条目 | `CreateEntryUseCase.invoke()`、`createFromAutofill()` | [`CreateEntryUseCase.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/entry/CreateEntryUseCase.kt) |
| 将新建草稿转换为可持久化条目 | `EntryDraftMaterializer.materialize()` | [`EntryDraftMaterializer.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/policy/EntryDraftMaterializer.kt) |
| 登记所有条目的字段定义并检查目录覆盖 | `EntryTypeDefinitions`、`buildEntryTypeDefinitionIndex()` | [`EntryTypeDefinitions.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/policy/EntryTypeDefinitions.kt)、[`EntryTypeDefinitionRegistry.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/policy/EntryTypeDefinitionRegistry.kt) |
| 将完整表单应用到已有条目并保留未管理状态 | `EntryFormEditor.apply()`、`EntryEdit.ReplaceForm` | [`EntryFormEditor.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/policy/EntryFormEditor.kt)、[`EntryEdit.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/policy/EntryEdit.kt) |
| 编辑详情条目 | `EditDetailEntryUseCase.edit()` | [`EditDetailEntryUseCase.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/detail/EditDetailEntryUseCase.kt) |
| 获授权读取完整编辑表单 | `LoadEditableEntryUseCase.invoke()` | [`LoadEditableEntryUseCase.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/detail/LoadEditableEntryUseCase.kt) |
| 按条目类型选择创建/完整编辑目标 | `VaultEditorDestinationRegistry.routeFor()`、`editRouteFor()` | [`VaultEditorDestinationRegistry.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/navigation/VaultEditorDestinationRegistry.kt) |
| 注册详情与四个编辑目的地 | `registerVaultGraph()`、`registerEditorEditDestination()` | [`VaultGraphRegistration.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/navigation/VaultGraphRegistration.kt) |
