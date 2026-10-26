# 条目编辑器与写入架构

状态：已批准，尚未实现（设计核对：2026-10-19）。

本文定义 Passly 内部条目类型的表单、校验、草稿转换与持久化边界。扩展方式限定为应用内部的编译期注册，
不支持由用户、备份文件或远端配置在运行时定义新的条目类型。

## 目标

- 每种条目保留强类型表单、专属交互和可独立演进的界面。
- 统一编辑会话、保存状态、认证生命周期、错误恢复和一次性 Effect。
- 复用有稳定业务语义的字段组，而不是复制输入控件和字段更新逻辑。
- 以 `EntryDraft` 作为 Presentation 与 Feature 写入层之间的唯一输入合同。
- 新增条目类型时，由编译期注册和覆盖测试保证编辑、物化、详情及集成能力完整。
- 创建和编辑能够复用同一套表单与 Draft 合同，但分别执行创建和乐观锁更新语义。

## 非目标

- 不创建由 `Map<FieldKey, Any?>` 或任意 JSON Schema 驱动的万能动态表单。
- 不把标签、键盘、布局、Compose 组件或平台对象放入 Domain。
- 不让字段组件、Screen 或 ViewModel 直接调用 DAO。
- 不为每个输入框创建独立的可写 `StateFlow`。
- 不通过反射、类名、枚举 ordinal 或未经约束的字符串寻找编辑器和物化器。
- 不把尚未闭环的类型显示在创建入口中。

## 当前实现与问题

当前实现已经具备正确的基础边界：

- `EntryTypeDefinitions` 定义字段类型、必填、访问级别和高敏存储键。
- 各编辑器拥有自己的强类型 `FormState` 和 `toEntryDraft()`。
- `CreateEntryUseCase` 检查 session、必填字段、领域校验并通过 Repository 写入。
- Domain 的 `EntryDraftMaterializer` 把 `EntryDraft` 和调用方提供的身份转成 `Entry`；Feature 保留创建校验、
  会话检查、身份生成和 Repository 写入。下文注册表与统一编辑会话仍为设计，不代表已实现。
- Login 创建页已经把网站地址映射到 `PRIMARY_URL`，把绑定应用映射到
  `APPLICATION_IDS`；两者是独立语义，编辑器不能再以“网站或应用地址”混用。

仍需解决的问题：

1. `AddPasswordViewModel`、`AddOtpViewModel`、`AddBankCardViewModel` 和 `AddWifiViewModel` 重复实现
   `mutate -> validate -> save -> clear -> effect`，生命周期修复容易遗漏某个类型。
2. `EntryDraftMaterializer` 使用持续扩大的 `when (EntryType)`；新增类型时，字段定义、编辑器和物化器可能
   处于不同完成度。
3. 多个凭据类型复用 `loginFields()`，但数据库凭据、服务器凭据、API Key 和加密钱包并不共享完整语义。
4. 相同字段组在不同 Screen 中容易重复布局、错误呈现和 Action 转换。
5. 创建入口、编辑器路由、物化、详情、Autofill 和备份缺少可验证的能力闭环。

## 总体边界

```text
Domain
  EntryTypeDefinitions
  EntryDraft / EntryDraftValue / FieldKey
  领域字段语义、必填、敏感等级和纯校验

Feature
  SaveEntryDraftUseCase
  EntryMaterializerRegistry
  创建或更新、实体物化、事务和 Repository 写入

Presentation
  EntryEditorRegistry
  EntryEditorSession<F>
  类型专属 FormState / Action / ViewModel / Screen
  可复用字段组状态与被动 Compose 组件
```

依赖方向始终是 Presentation -> Feature -> Domain。Domain 不知道编辑器、Material 组件、Android package 或
导航；Feature 不持有 Compose 状态；Presentation 不创建数据库实体。

## 强类型表单与字段组

### 单一状态源

每个编辑器只暴露一个不可变状态流。新建会话可以立即进入 `Ready`；编辑已有条目必须先通过授权读取并转换表单，
因此使用封闭的加载状态，而不是用可空 form 表达：

```kotlin
sealed interface EntryEditorUiState<out F : EntryFormState> {
    data object Loading : EntryEditorUiState<Nothing>
    data object Closed : EntryEditorUiState<Nothing>

    data class Ready<F : EntryFormState>(
        val form: F,
        val validation: EntryEditorValidation = EntryEditorValidation.Empty,
        val saveState: EntryEditorSaveState = EntryEditorSaveState.Idle,
    ) : EntryEditorUiState<F>

    data class LoadFailed(
        val failure: EntryEditorFailure,
    ) : EntryEditorUiState<Nothing>
}

data class EntryEditorFailure(
    val errorCode: String,
    val recoverable: Boolean,
)

sealed interface EntryEditorSaveState {
    data object Idle : EntryEditorSaveState
    data object Saving : EntryEditorSaveState
    data class Failed(val failure: EntryEditorFailure) : EntryEditorSaveState
}
```

`EntryEditorSession<F>` 是这个状态的唯一可写拥有者。类型 ViewModel、字段组件和 Sheet 不再各自保存一份可写
业务状态。需要局部观察时，可以从父状态生成只读派生 Flow，但不能形成第二个事实源。

焦点、下拉展开、动画进度和 Sheet 是否打开仍可由 Compose 保存，因为它们不是持久业务状态。字段值、选择结果、
扫描结果和保存状态必须回到编辑器状态。

### 字段组

只抽取跨类型具有相同含义和规则的字段组：

```kotlin
interface EntryFormState

data class CommonMetadataState(
    val title: String = "",
    val notes: String = "",
    val tags: List<String> = emptyList(),
)

data class AssociationState(
    val primaryUrl: String = "",
    val domains: List<String> = emptyList(),
    val applicationIds: List<String> = emptyList(),
)

data class LoginCredentialState(
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
)

data class LoginFormState(
    val metadata: CommonMetadataState = CommonMetadataState(),
    val credential: LoginCredentialState = LoginCredentialState(),
    val associations: AssociationState = AssociationState(),
) : EntryFormState
```

`passwordVisible` 等字段与敏感值本身属于表单会话，认证失效时一并清理。若某个 UI 标志只影响瞬时动画，不参与
业务规则，则留在 Compose。

字段组可提供三类复用能力：

1. 不依赖 Android 的纯更新函数和规范化规则；
2. 被类型 `EntryFormAdapter` 复用的 Draft 映射函数；
3. 接收状态和语义回调的被动 Compose 组件。

字段组不能自行写数据库、读取认证状态或持有 ViewModel。

## EntryEditorSession

`EntryEditorSession<F>` 通过组合被类型 ViewModel 持有，不作为纯参数转发包装层。它拥有所有编辑器共同的状态机：

- 原子更新表单并重新计算可见错误和完整校验结果；
- 保存期间阻止重复提交和不一致字段更新；
- 对保存时的表单快照调用 `SaveEntryDraftUseCase`；
- 发出 `Saved(entryId)` 或类型化失败 Effect；
- session 权限丢失时取消初始化和保存任务、擦除表单状态并进入 `Closed`；
- `ViewModel.onCleared()` 时擦除敏感状态；
- 保留 `CancellationException`，不把取消误报为保存失败。

建议合同：

```kotlin
class EntryEditorSession<F : EntryFormState>(
    private val initializer: EntryEditorInitializer<F>,
    private val validator: EntryFormValidator<F>,
    private val formAdapter: EntryFormAdapter<F>,
    private val saveEntryDraft: SaveEntryDraftUseCase,
) {
    val uiState: StateFlow<EntryEditorUiState<F>>
    val effects: Flow<EntryEditorEffect>

    fun update(transform: (F) -> F)
    fun save()
    fun clear()
}

sealed interface EntryEditorEffect {
    data class Saved(val entryId: EntryId) : EntryEditorEffect
    data class SaveFailed(val failure: EntryEditorFailure) : EntryEditorEffect
}
```

Coroutine scope 和认证状态由拥有它的 ViewModel 在构造时绑定。Session 不接收 `NavController`、Context 或
字符串资源。类型 ViewModel 仍负责其特有的异步能力，例如 Wi-Fi 扫码、OTP URI 初始化和已安装应用加载；这些
任务的结果通过 `session.update` 合并回单一表单状态。ViewModel 在权限丢失或清理时先取消这些类型专属任务，
再调用 `session.clear()`；Session 只取消自己拥有的初始化和保存任务。

`EntryEditorInitializer<F>` 返回 `EntryEditorInitial(form, target)`。新建时使用空白表单和
`EntryDraftTarget.New(type)`；编辑时由窄读取 UseCase 加载完整 Entry，再由 `EntryFormAdapter.fromEntry()` 转成
表单，并绑定包含 `entryId`、类型和预期版本的 `EntryDraftTarget.Existing`。初始化失败进入 `LoadFailed`，不能
渲染半空编辑器。进入 `Closed` 后不因认证状态再次变化自动恢复旧表单；宿主完成重新认证后必须建立新的编辑会话。

## 类型专属 ViewModel 和 Screen

类型 ViewModel 只解释本类型 Action，不重复保存状态机：

```kotlin
class WifiEditorViewModel(
    private val editor: EntryEditorSession<WifiFormState>,
    private val scannerSeed: EntryEditorSeedSource,
) : ViewModel() {
    val uiState = editor.uiState
    val effects = editor.effects

    fun onAction(action: WifiEditorAction) {
        when (action) {
            is WifiEditorAction.SsidChanged -> editor.update { it.withSsid(action.value) }
            is WifiEditorAction.SecurityChanged -> editor.update {
                it.withSecurityType(action.securityType)
            }
            is WifiEditorAction.ApplyScan -> editor.update { it.applyScanned(action.payload) }
            WifiEditorAction.Save -> editor.save()
        }
    }
}
```

Screen 按业务语义组合字段组件，而不是遍历一个动态字段数组：

```kotlin
LoginEditorContent(
    state = state,
    onAction = viewModel::onAction,
) {
    CommonMetadataFields(state.form.metadata, onAction)
    LoginCredentialFields(state.form.credential, onAction)
    AssociationFields(state.form.associations, onAction)
}
```

字段组件不创建字段到 Action 的第二套映射接口；应接收少量语义回调或所属编辑器的 Action sink。复杂组可以定义
自己的 sealed `GroupAction`，再由类型 ViewModel 穷尽映射。

## 编译期编辑器注册

Presentation 使用封闭配置，而不是运行时 schema：

```kotlin
sealed interface EntryEditorConfig {
    val type: EntryType

    data class Login(
        override val type: EntryType,
        val showEmail: Boolean,
        val associations: AssociationMode,
    ) : EntryEditorConfig

    data class BankCard(
        override val type: EntryType = EntryType.BANK_CARD,
    ) : EntryEditorConfig

    data class Wifi(
        override val type: EntryType = EntryType.WIFI,
    ) : EntryEditorConfig
}
```

`EntryEditorRegistry` 显式保存支持创建的配置。导航根据 sealed config 做穷尽分派，因此不需要反射和不安全泛型
转换：

```kotlin
when (val config = EntryEditorRegistry.require(type)) {
    is EntryEditorConfig.Login -> LoginEditorHost(config)
    is EntryEditorConfig.BankCard -> BankCardEditorHost(config)
    is EntryEditorConfig.Wifi -> WifiEditorHost(config)
}
```

参数只控制同一语义族内的明确变体。例如 Login 可以决定是否显示 Email 和关联应用；数据库凭据需要数据库名、
主机和端口时，应使用独立 `DatabaseCredential` config 和表单，不继续扩展 Login 的布尔开关。API Key、服务器
凭据和加密钱包也必须按真实语义建模。

创建菜单只能从 `EntryEditorRegistry.creatableTypes` 生成。未注册完整编辑器的 `EntryType` 不显示，也不能通过
深链路进入半成品页面。

## 表单与 Draft 的双向适配

每种表单提供一个纯 `EntryFormAdapter<F>`，使已有条目的读取与保存使用同一套字段定义：

```kotlin
interface EntryFormAdapter<F : EntryFormState> {
    fun empty(): F
    fun fromEntry(entry: Entry): F
    fun toDraft(form: F, target: EntryDraftTarget): EntryDraft
}
```

Adapter 负责：

- 把完整授权读取的 `Entry` 拆成对应强类型表单；
- 规范化 UI 输入；
- 把字段组映射为类型安全的 `EntryDraftValue`；
- 忽略真正为空的可选字段；
- 保留密码、密钥等不能被 trim 的原始值；
- 设置 `EntryDraftTarget.New` 或 `EntryDraftTarget.Existing`。

Adapter 不负责授权读取、生成 ID、时间戳、版本递增、加密或数据库事务。字段是否属于该类型、值类型是否匹配，继续由
`EntryDraft.withValue()` 和 `EntryTypeDefinitions` 保证。

## 统一写入链路

目标写入链路：

```text
UiAction.Save
  -> EntryEditorSession 获取不可变表单快照
  -> EntryFormAdapter.toDraft(form, target)
  -> SaveEntryDraftUseCase
       -> session 权限检查
       -> required-field 检查
       -> EntryMaterializerRegistry.materialize(draft)
       -> EntryValidation
       -> New: EntryCommandRepository.createEntry
       -> Existing: EntryCommandRepository.updateEntry(expectedVersion)
  -> EditorEffect.Saved(entryId)
```

`SaveEntryDraftUseCase` 是目标合同。迁移期可在内部委托当前 `CreateEntryUseCase` 和未来的更新 UseCase，但最终不保留
两套校验、物化和错误映射。`EntryDraftTarget.Existing` 必须携带 `EntryId` 和预期 `EntryVersion`，更新冲突返回明确
错误，不静默覆盖。

创建、普通编辑和导入不能直接共享 Presentation Session，但可以共享 Draft 校验、物化与 Repository 命令。
Autofill 保存仍使用自己的授权与 sealed inbox 流程，只在已经拥有合法 Draft 后复用物化规则。

## EntryMaterializerRegistry

将当前集中式 `EntryDraftMaterializer.when(type)` 拆成按语义族注册的物化器：

```kotlin
interface EntryTypeMaterializer {
    val supportedTypes: Set<EntryType>
    fun materialize(draft: EntryDraft, identity: EntryIdentity): Entry
}

class LoginEntryMaterializer : EntryTypeMaterializer
class OtpEntryMaterializer : EntryTypeMaterializer
class BankCardEntryMaterializer : EntryTypeMaterializer
class WifiEntryMaterializer : EntryTypeMaterializer
```

实现可通过 Hilt Set multibinding 注入 `EntryMaterializerRegistry`。Registry 在构建索引时拒绝：

- 同一 `EntryType` 被多个物化器声明；
- `supportedTypes` 为空；
- 请求一个未注册的类型；
- Draft target type 与 Identity type 不一致。

各层保持独立注册表，不能创建一个同时依赖 Domain、Compose、备份和 Autofill 的全局万能注册表：

- Domain：`EntryTypeDefinitions`；
- Presentation：`EntryEditorRegistry`；
- Feature：`EntryMaterializerRegistry`；
- 其他能力：详情、备份和 Autofill 继续拥有自己的窄注册或映射。

跨层覆盖测试负责确认对外开放类型的能力闭环。

## 校验和错误呈现

校验分为三层：

1. 字段组即时校验：用于输入体验，可区分 `REQUIRED`、格式错误和不支持值；
2. Draft 结构校验：字段成员、值类型和必填字段；
3. Entry 领域校验：跨字段约束和最终实体不变量。

空白新表单可以禁止保存但不立即显示红色 `REQUIRED`；用户输入非空非法值时立即显示。提交后可选择显示全部必填
错误，但规则必须由统一的 validation policy 决定，不能由各 Screen 自行猜测。

UseCase 返回的 `AppError` 在 Presentation 边界转换成只含稳定 code 和可恢复性的 `EntryEditorFailure`；UI state
不长期持有 Throwable。WARN/ERROR 使用稳定 `error_code` 和 `reason_code`；Presentation 映射为用户可读资源。禁止把异常 message、密码、
密钥、用户名、域名或明文包名写入日志。协程取消必须重新抛出。

## 生命周期与敏感状态

- 完整 Vault session 权限丢失后，所有表单、密码显隐状态、选择结果和辅助缓存立即清空。
- ViewModel 清理时取消扫描、应用列表、图标和保存任务，再擦除编辑状态。
- 保存使用点击瞬间的不可变快照；`Saving` 期间不接受字段修改和第二次保存。
- 敏感表单不写入 `SavedStateHandle`、导航参数、Bundle 或磁盘缓存。
- 跨页面初始化只传递不含秘密的标识，或使用受 Vault 图生命周期约束的一次性内存 seed。
- 保存成功 Effect 携带 `EntryId`；过期 Effect 不得关闭后来打开的另一个编辑器。

## 新增条目类型的闭环

一个类型只有同时满足下列条件，才能加入创建菜单：

1. `EntryTypeDefinitions` 定义准确字段语义和敏感映射；
2. 专属或明确复用的强类型 `FormState`；
3. 类型 Action、ViewModel 和 Screen/字段组组合；
4. `EntryFormAdapter` 与双向映射单元测试；
5. `EntryTypeMaterializer` 与物化测试；
6. Domain 校验；
7. 详情读取和渲染；
8. 数据库存储与完整往返测试；
9. 备份导入导出映射；
10. 若声明支持，则补齐 Autofill、Credential Manager、扫码和搜索投影；
11. `EntryEditorRegistry` 注册，能力闭环测试通过。

没有完成闭环的枚举值可以保留在 Domain 规划中，但不得出现在用户入口，也不得静默回退为 Login。

## 迁移顺序

### 阶段 1：建立统一编辑会话

- 定义 `EntryEditorUiState`、`EntryEditorSaveState`、`EntryEditorEffect` 和 `EntryEditorSession`。
- 以 Login 为首个迁移类型，将 `AddPassword*` 收敛为同时支持创建和编辑的
  `PasswordEditor*`，验证状态、保存、认证清理和 Effect；不保留只负责创建的并行 ViewModel。
- 创建目标使用 `EntryDraftTarget.New(LOGIN)`；编辑目标只通过非敏感 `entryId` 导航参数建立，
  由授权读取获得 `EntryDraftTarget.Existing(entryId, LOGIN, expectedVersion)`。
- Login 表单必须完整往返 `TITLE`、`USERNAME`、`EMAIL`、`PASSWORD`、`PRIMARY_URL`、
  `DOMAINS`、`APPLICATION_IDS`、`NOTES` 和 `TAGS`。界面可以只突出一个主网站和一个主要绑定应用，
  但未被用户修改的多域名、多应用关联不得在保存时丢失。
- `PasswordEditor` 保存已有条目时只执行一次乐观锁更新。禁止复用详情页逐字段命令拼装完整保存，
  以免一次提交产生多条历史版本或留下部分更新状态。
- Draft 未表达的正交元数据（收藏、图标、到期时间和自定义字段）由写入层从当前 Entry 保留；
  Presentation 不用隐藏字段副本伪造这些数据。
- 详情页的“编辑”入口只有在 Login 编辑路由、授权加载、原子更新和冲突反馈全部闭环后才开放。
- 不同时迁移其他类型，避免一次重写全部编辑器。

### 阶段 2：抽取稳定字段组

- 抽取 metadata、associations 和 login credential 状态、纯规则和被动组件。
- 只抽真实出现两次以上且语义一致的字段组。
- 银行卡有效期、OTP 配置等保持专属组合，不强塞进通用 TextField。

### 阶段 3：迁移现有编辑器

- 依次迁移 OTP、Bank Card、Wi-Fi；每个类型独立可编译、测试和提交。
- 删除对应旧 ViewModel 保存模板和重复状态，不保留桥接兼容层。

### 阶段 4：拆分物化注册

- 提取四个现有物化器并建立 `EntryMaterializerRegistry`。
- `CreateEntryUseCase` 先消费 Registry；创建与更新统一后替换为 `SaveEntryDraftUseCase`。
- 删除集中式类型 `when`，不同时保留旧、新两套物化路径。

### 阶段 5：关闭类型能力缺口

- 为数据库、服务器、API Key、加密钱包等建立真实语义模型。
- 未实现闭环的类型从创建入口移除。
- 逐类型完成详情、备份和声明过的集成能力后再开放。

## 测试与验收

### EntryEditorSession

- 更新表单后校验与 `canSave` 在同一原子状态中变化；
- 保存期间重复 Save 只产生一次 Repository 写入；
- 保存使用点击时快照，后续 UI 事件不能改变该次 Draft；
- session 锁定和 `onCleared` 会取消任务并擦除敏感状态；
- `CancellationException` 不生成失败 Effect；
- 成功 Effect 携带实际 `EntryId`。

### 类型合同

- 每个 Adapter 覆盖空值、规范化、高敏原值保留和所有字段组；
- `fromEntry()` 与 `toDraft()` 对该类型的字段集合保持对称；
- Adapter 产生的 Draft 能被对应 Materializer 完整消费；
- Materializer 不读取未在 `EntryTypeDefinitions` 声明的字段；
- 创建后从 Repository 读回的 Entry 与预期一致；
- Existing target 使用预期版本并验证冲突。

### 注册覆盖

- `EntryEditorRegistry` 中 `EntryType` 唯一；
- `EntryMaterializerRegistry` 中 `EntryType` 唯一；
- 每个 `creatableType` 都具有 definition、editor、mapper、materializer、detail 和存储往返能力；
- 未完成闭环的类型不会出现在创建菜单；
- 未知或未注册类型明确失败，不回退到 Login。

### UI

- 字段组组件只渲染输入状态并发送语义动作；
- 空白初始表单不展示红色必填错误，但保存保持禁用；
- 类型参数只影响声明过的字段组和布局变体；
- 应用选择、扫码和 OTP 初始化结果都回写父编辑状态；
- 详情页读取与编辑页写入字段保持对称。

## 完成标准

- 四个现有编辑器不再重复保存、Effect 和 session 清理实现；
- 所有对外开放类型都通过能力闭环测试；
- 不存在动态 `Any` 表单、反射路由或运行时自定义类型；
- 不存在旧、新并行的保存或物化兼容路径；
- Domain、Feature 和 Presentation 注册边界保持单向依赖；
- 项目单元测试、AndroidTest 编译、Lint 和模块边界检查全部通过。
