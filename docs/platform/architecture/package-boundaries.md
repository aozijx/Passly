# 包边界

## 职责表

| 包          | 可以包含                                      | 不应包含                                  |
|------------|-------------------------------------------|---------------------------------------|
| `core`     | Android 平台适配、权限、共享 Compose UI、密码学与遥测能力          | DAO、Repository 实现、业务页面                |
| `domain`   | 领域模型、端口、策略、验证及设置契约                    | `android.*`、资源 ID、Entity、Feature UI 状态 |
| `data`     | Room、Proto DataStore、Mapper、Repository 实现 | Compose 页面、业务流程、备份协议与格式              |
| `core.crypto` | 无状态密码学原语、算法实现及 Data 使用的加密能力合同 | 密钥生命周期、认证流程、Android Keystore |
| `security` | Vault/DEK/字段/附件密钥生命周期、Android Keystore、认证、授权、锁定与会话编排 | Room Entity、DataStore 具体实现、Feature UI |
| `app/feature/<name>` | 多数 App 内业务流程、平台入口、能力合同与应用策略 | Data 实现；非 UI 代码不依赖 presentation |
| `:feature:autofill` | Autofill/Accessibility/Credential 服务、候选编排、结果交付与页面；Gradle 仅依赖 `:core`、`:domain`、`:runtime:session` | App、Data、Security 实现；App 只依赖其稳定宿主/启动合同 |
| `app` 适配器 | Android Application、DI 与跨模块实现连接；Backup 协议到 Domain 快照映射 | 页面状态、领域规则真相源、Room/文件持久化实现 |
| `app/feature/<name>/presentation` / Autofill Feature `presentation` | 对应业务的 Route、ViewModel、UI state/effect、Compose UI | DAO、Entity、Data Repository 实现 |
| `presentation/shared` 等 | 跨业务的纯展示组件、映射与展示契约；可依赖其展示投影直接需要的 Domain 契约（例如条目分类输入的 `EntryType`） | 具体业务流程与数据实现；与该展示无关的 Domain 模型 |
| `service`  | Android Service 入口与系统生命周期桥接；与单一业务流程紧耦合的实现可放在对应 `feature/<name>` 下 | 独立业务真相源 |

真实模块还要求源码命名空间与职责一致：`:data` 只包含 `com.aozijx.passly.data.*`。`:core:common` 保持为
Domain 可依赖的纯 Kotlin 错误与通用能力；通用密码学、遥测、共享 Android 与 Compose 能力合并在 `:core`。
Domain 只允许分页端口使用 `androidx.paging:paging-common` 的 `PagingConfig`/`PagingData`；不得依赖 Paging Compose、Lifecycle 或其他 UI API。
`SensitiveFieldCryptor` 与 `AttachmentContentProtector` 是供 Data 消费的 Core 能力合同；实现和所有受 Vault 会话控制的密钥
（DEK、字段密钥、附件密钥及 Envelope/VerificationTag）归 `:security`。Security 内部按
`authentication.flow`、`authentication.method.*`、`authentication.host`、`session`、`authorization`、`lock`、`dek` 和
`envelope` 分包；`:core` 不放 `com.aozijx.passly.security.*` 实现。日志加密和遥测散列的 Keystore key 仍由各自的 Data/App owner 管理。
应用消息调度与全局框架桥归 `:app`。

每个业务只有一个 Feature owner；多数 App 内业务以 `app/feature/<name>` 为入口，Autofill 由独立
`:feature:autofill` 模块拥有。页面放在对应 Feature 的 `presentation` 子包，包括 Recovery 和 Database Reset。
根 `presentation` 仅保留应用组合入口及其 Activity 宿主、跨业务展示组件和展示映射。`core` 的共享指
跨业务使用的技术能力；包含业务语义的纯规则优先进入 Domain，不因被多个页面引用就放进 Core。

根 `PasslyApp`/`PasslyAppNavigation` 组合入口负责注册 Vault 与 Settings 图，并持有 Activity 作用域的 OTP 导入
ViewModel。Shell 持有安全目的地状态与宿主合同，由 app 根提供 Vault、Recovery 和 Authentication 页面内容；Shell
不导入这些 Feature Presentation。各 Feature 仍负责自身图注册与页面状态。

## Feature 内的包与命名

先按业务 Feature 和页面/工作流 owner 归属，再按需要建立子包。`presentation` 可直接容纳同一页面的 Route、ViewModel、Action、Effect、UiState、Reducer 和 Mapper；只有一组文件有共同 owner、稳定的依赖方向，且独立目录能让调用关系更清楚时才建 `state`、`result`、`history` 等子包。`ui` 放被动 Compose 内容，不拥有 Repository、认证或页面异步任务。不要把 `logic`、`utils`、`common` 当成不同职责的收纳包，也不要求全项目具有相同的子包层级；包名与目录一致，但同一 Gradle module 内改包名不形成编译隔离。

文件和类型用业务语义加职责命名：`XxxUiState` 表示可观察页面状态，`XxxUiModel` 表示展示投影，`XxxMutation` 表示纯转换输入，`XxxReducer` 只计算新状态，`XxxUiMapper`/`XxxMapper` 只转换模型，`XxxPolicy` 表示无副作用的准入或规则，`XxxSession` 表示有明确创建、关闭和迟到结果合同的会话 owner。名称应指出被转换的目标或所约束的动作，例如 Screen 投影和 Screen Action；不要以模糊的 `Logic`、`Helper`、`Manager` 代替职责。单主要类型文件同名；多声明文件用其共同职责命名。具体 Kotlin 文件组织规则见 [Kotlin 规范](../../engineering/development/kotlin-style.md)。

Mapper 与 Reducer 可以在同一页面并存：Reducer 以当前状态和 Mutation 计算合法的新状态；Mapper 从现有状态、Domain 值或其他明确输入生成 UI 投影，不发起状态迁移或副作用。两者按消费方和输出模型放在所属页面附近，不为后缀分别创建只有一两个文件的全局 `mapper`/`reducer` 包。若 Mapper 的判断已变成持久业务规则，应移到 Domain Policy；若转换需要授权、I/O、取消或资源清理，应由 ViewModel、Route 或独立工作流 owner 编排。

## 强制规则

```text
domain -X-> data | feature | android.*
core   -X-> data repository implementation
feature 非 UI -X-> presentation | data implementation
feature presentation -X-> data implementation
security-X-> data implementation
app feature -X-> 任意散落的 data implementation import
```

## 官方架构建议与 Passly 适配

[Android Domain layer 指南](https://developer.android.com/topic/architecture/domain-layer)中的 Domain layer 是可选的 UI 与 Data 之间层，
用于复杂业务逻辑或被多个 ViewModel 复用的逻辑；该指南也提醒，不同架构中的 “domain layer” 含义可能不同。
Passly 的纯 Kotlin `:domain` 则是依赖倒置后的领域模型、规则与能力合同模块，不按 Android 指南的 UseCase 层定义。
仅在完整业务动作有复杂编排、策略或真实复用时建立 UseCase/应用服务，简单 Repository 调用不机械包装。

[Android 架构建议](https://developer.android.com/topic/architecture/recommendations)将建议标为指导而非强制要求，
应按应用需要调整；其中 UseCase 的推荐场景是跨多个 ViewModel 复用业务逻辑或简化复杂页面逻辑。
[Android 模块化模式](https://developer.android.com/topic/modularization/patterns)没有唯一模块方案，
建议追求高内聚、低耦合、明确职责和最小公开 API，并在有理由时分离抽象与实现。
Passly 据此先审查 Domain 包的合同 owner 和真实消费者；只有所有权、变更隔离或构建收益足以覆盖配置与依赖成本时，
才把包提升为独立 Gradle module。包级 verifier 可限制同一模块内的源码依赖，但不提供编译隔离。
编译依赖、运行时调用、Hilt 绑定和层间数据/错误流向的完整约定见[层间契约与通信](layer-interactions.md)。

`:security` 可以依赖 Domain 的认证/会话端口、`:core` 的密码学能力和 `:runtime:session` 的资源无关状态机，
但不能依赖 Room、Data Repository 实现、Compose 页面或 Feature 状态。Feature 只能通过 Domain 端口请求认证、
授权和锁定，不直接导入 `security.authentication`、Keystore 或密钥实现。
App 内的业务 Feature 同样只通过 Domain 端口使用认证、凭据配置、授权和会话能力；App 作为 Hilt 组合根在 Gradle 图中引入 Security 实现。源码层只允许 `PasslyApp` 和 `AutofillAuthActivity` 组合公开的 `AuthenticationHost` 平台宿主，其他 App 主源码禁止导入 Security。该宿主承载 Activity/Compose/BiometricPrompt 生命周期，不属于纯 Kotlin Domain 合同。
`:runtime:session` 只拥有资源无关的状态机、lease 与生命周期合同，不依赖 Domain。`:data` 和 `:security`
各自显式依赖 Domain 与 Runtime Session，Domain 合同不会通过会话模块传递给下游。
Core 保留通用 `PasswordStrengthIndicator`。App 的 `presentation.mapping` 将 Domain 强度结果映射为
`PasswordStrengthUiState`；共享 `PasswordFields` 接收该 UI 状态，不直接依赖 Domain。

App 通过 Gradle 依赖 `:data` 让 Hilt 聚合实现。Backup 的 `BackupSnapshotGateway` 保留归档与兼容合同，
由 App `AppBackupSnapshotGateway` 映射 App 归档模型和 Domain `VaultBackupStore`。Domain 只定义持久化中立的
Entry/关系/图标与附件快照、读取筛选、恢复模式和结果；Data 负责 Room 映射、密钥字段、Vault 资源文件、
文件 journal 及同一数据库事务。App Backup 不直接导入 Data 实体、Mapper、Session、GC 或 Repository。
稳定键映射器位于 `feature/backup/internal/archive/mapper`，只依赖 Domain 与归档模型；协议键和兼容过滤仍归 App。
`SourceBoundaryPolicy` 精确禁止 Backup Gateway 导入 Data 实现，不为 Backup 留数据库适配例外。
Autofill 等 Android Service 需要打开页面时，由 Feature 中的启动契约与 `app` 适配器衔接，Feature 不引用 Activity 类。

分页应通过纯 Kotlin 契约传递；Android Paging 可留在 Data/UI 适配边界。Feature 需要条目数据时依赖
Entry Domain 端口，不直接构造 Entity 或具体 Repository。Autofill 服务、候选编排和候选 UI 归属
`:feature:autofill`；App 仅拥有 Autofill Android 平台宿主 Activity、结果回调集成及平台适配器。认证 UI 与宿主注册由 `:security` 拥有，App Activity 只组合 Security 的认证宿主。候选 UI 使用
metadata-only 的 `AutofillCandidateSummary`，不依赖 App 的 Vault 图标组件，取不到目标应用图标时不展示图标。
Autofill 策略通过专属 `AutofillSettingsSource` / `AutofillSettingsRepository` Domain 端口读取与更新；
通用 `InteractionSettings` 仅描述滑动与下拉搜索，避免 Autofill 与 Vault 手势共用无关设置快照。
只有可复用的条目匹配语义进入 Entry Domain。卡号校验和 Wi-Fi 二维码载荷解析属于纯规则，位于
`domain/entry/policy`；相机、扫码结果展示与导入编排仍在 App Feature。

`EntryFieldReader` 的默认实现也位于 `domain/entry/policy`，App 仅通过 DI 连接。读取按 `FieldKey`
穷尽映射，不按枚举名称或前缀猜分组；新增字段必须明确其读取语义。凭据不存在或高敏值未揭示时返回
无值，不编造 OTP 默认配置。读取器不授权、不访问仓库、不解密；复制与揭示流程继续在 Feature 中授权。
布尔值使用 `true` / `false`，时间戳保留毫秒值，列表沿用逗号加空格、恢复码使用换行分隔；展示层负责本地化。
`CUSTOM_FIELDS` 是带名称和类型的集合，不隐式拼成单个字符串，仍由专属流程处理。

`EntryDraftMaterializer` 位于同一 Domain 策略包，只将草稿和给定身份转换为条目；不生成 ID、不读取会话、
不记录日志或写入仓库。`domain.entry.port.EntryCreation` 只表达 Vault 与扫码共用的普通条目创建能力；
Autofill 依赖 `domain.autofill.port.AutofillEntryCreation`，因为它需要 Autofill 会话授权和可选 Inbox 重试身份。
`domain.autofill.port.AutofillInboxRecordStore` 与 Autofill Inbox 模型共同归属 `domain.autofill`。
App 根组合中的 `CreateEntryUseCase` 可实现两项能力并复用纯创建规则，但分别执行相应会话门禁和持久化端口；
App DI 将实现绑定到两项能力合同。新增凭据字段时在 Domain 测试其转换，不能把持卡人等凭据值借用为 Profile 用户名。
`EntryDraft.invalidProvidedFields` 校验所有已提供字段的归属、值类型和空数字；创建用例与物化器共用该检查，
不能假定所有草稿都经由 `withValue` 构造。必填检查与完整凭据校验分别处理缺失字段和具体业务规则。

OTP 生成、密钥编解码和 `otpauth:` URI 协议位于 `domain/entry/otp`；App 负责扫码、编辑状态、授权与展示，
不另留 Android Uri 实现或仅用于测试的解析、格式化转发接口。
手动输入的原始数字与密钥通过 Domain 的 `OtpConfigInput` 解析；页面仅保留输入和显示错误，
不在草稿映射中用默认值掩盖无效输入。当前规则见[动态口令创建](../../product/vault/otp-entry.md)。

银行卡编辑器将原始输入交给 `CardNumberValidator`，由它一次性提取数字并执行长度、Luhn 与卡组织识别，
页面保留用户输入的分隔格式。Mastercard 2 系列包含 222100–272099，边界和前缀末位为 0 的情况由单测覆盖；
范围依据 [Mastercard 官方说明](https://www.mastercard.us/content/dam/mccom/en-us/documents/merchant-2-series-BIN-impact-checklist-aug-2016.pdf)。
通过校验但未识别的卡组织使用 `CardNetworkMatch.Unknown` 表达。

详情编辑命令为 Domain 的 `EntryEdit`，其 `applyTo` 返回 `EntryEditResult.Updated / Invalid / Unsupported`，
不以 null 合并失败原因。空标题、空白标签和应用 ID 在构造模型前返回字段错误；命令字符串表示隐藏输入。
普通标题命令仅更新 Profile；Wi-Fi 详情的网络名称编辑由 Presentation 显式选择 `SetWifiNetworkName`，
同时更新标题与 SSID，保留原有界面行为。纯规则与测试位于 Domain，Feature 只编排会话、重试与写入。

详情标题的显示、开始编辑、未修改判断和保存后刷新统一使用 `detailDisplayTitle`，Wi-Fi 对应 SSID。
`planDetailTitleEdit` 统一保存决策：网络名按原文比较和写入，保留首尾空格；普通标题仍去除首尾空白。
未修改网络名取消编辑，不因 Profile 的个人标签不同而重写标题或 SSID。
`EntryPresented` 仅携带是否保留标题编辑的意图，不携带标题草稿快照；Reducer 从当前状态保留活跃草稿，
否则从新条目派生显示名。迟到刷新不能重新打开已取消的标题编辑。

详情访问的异步发布由 `DetailWorkSession` 约束：每次打开生成独立会话凭据，关闭立即使其失效；
ViewModel 观察认证状态，完整访问权限撤销时清空页面并关闭凭据，重新解锁后仍须建立新会话。
离开后返回同一条目 ID 也不接受旧凭据。保存流程在写入前、写入返回后及展示加载返回后核对凭据和完整访问权限，
过期结果不能刷新页面、重新揭示字段或覆盖保存状态。此边界限制 UI 发布，不撤销已完成的数据库事务。
普通与修订字段揭示复用会话的发布入口；过期、取消或发布失败时擦除未保留的缓冲，异常继续传播。
详情加载、历史、应用列表、复制反馈及修订回调也核对访问凭据；清理会话同时取消详情加载和观察。
标签编辑的候选查询也核对访问凭据与协程状态，关闭、切换条目或新请求会取消待打开的查询；同 ID 的新访问不接受旧结果。
打开时读取当前条目的最新标签，已打开的编辑器不因重复打开事件重置草稿。
标签提交期间，Reducer 拒绝标签编辑与关闭事件；UI 禁用输入、标签操作及无障碍移除动作，并阻止底部弹层滑动关闭。
成功后关闭编辑器；失败保留提交时的草稿并恢复编辑，不允许保存期间的新输入被成功回调丢弃。
未提交的脏草稿在滑动关闭后仍需确认；选择继续编辑会重新展开底部弹层。
图标保存保留独立资源所有权边界，不能把过期发布等同于删除可能已持久化的图标文件。
保存期间不允许重新选择或准备图片。关闭图标编辑器先取消保存，等保存协程完成写入或回滚后再释放文件，
清理回调不依赖已经取消的页面协程作用域。
图片下载与裁剪由 `DetailFaviconSession` 分配独立准备凭据，选择新来源、重开编辑器或关闭会话会使旧凭据失效。
结果发布同时核对凭据与协程状态：迟到的成功结果释放其暂存文件，迟到的失败不改写新编辑器。
接收新的裁剪输入或图标来源时释放被替换的资源；修改 URL 文本不改变正在运行的工作状态。

已提升的图标通过 Domain `EntryIconResourceCleaner` 请求回收，App 不直接删除。
Data 在附件资源互斥锁和数据库写事务内核对当前条目及历史修订引用，再删除受管目录内的无引用文件；
备份恢复使用同一资源互斥锁。锁定或存储失败时保留请求；回收队列同时观察认证状态与数据库会话，
因此认证状态不变时数据库单独封存并重开也会触发重试。
请求合并按每次通知的身份确认完成，检查期间的新通知不能被旧检查移除。
修订裁剪、永久删除、历史重建及图标替换重新提交失去引用的路径；仍有引用的文件继续保留。
`FaviconImageStore` 初始化时快照已有的生成文件，恢复进程退出遗留的清理；本进程新提升的编辑资源不参加该快照。

单字段凭据更新由 Domain `EntryFieldEditor` 校验字段归属并复制目标值，保留无关与未揭示字段；
随后仅校验本次替换字段，复用创建规则中的卡号、CVV、登录/Wi-Fi 密码校验。无效输入以
`Invalid(violations)` 返回字段与错误码；不能对未揭示 bundle 直接执行整条创建校验。
Feature 的详情编辑用例负责会话、互斥、重试、日志和 Repository 写入。

详情内联文本保存只记录提交后草稿是否发生过变化（`Unchanged / Changed`），不复制输入值；重复提交同值输入不算变化。
同字段的修改、取消或重开会使旧保存完成保留当前草稿；未变化的草稿才在成功后关闭，失败继续保留。
`EntryPresented` 只刷新已存储条目，不关闭标题编辑；标题完成后通过 `detailDisplayTitle` 同步实际显示值。
网址与应用关联使用独立完成标记，保存应用关联不能关闭未保存的网址草稿。

银行卡年月模型 `CardExpiry` 位于 `domain/entry/model/credential`，统一有效期解析与 `MM/YYYY` 格式化。
页面的可选状态与年份选择范围留在 Presentation；[银行卡创建与有效期](../../product/vault/card-entry.md)说明当前写入边界。

## 模型归属

- 条目标签拆分、去重、稳定排序和数量/长度约束由 domain/entry/policy/EntryTagPolicy 持有；详情编辑器消费校验结果并维护输入、建议和页面错误状态。图标分类与详情分区解析只影响展示，留在 Presentation，不因其包含纯 Kotlin 规则就提升为业务 Domain。
- Entity：数据库结构，只在 Data 层。
- Domain model：业务语义与跨层契约。
- UI model/state：展示派生状态只属于 Presentation，可由 Feature presentation/UI 或 `presentation/shared` 中的共享展示契约承载；不得反向依赖 Data、Security 或其他 Feature 的实现。
- 设置策略枚举若展示语义与 Domain 完全相同，Feature UI 可直接使用 Domain 类型；只有展示结构或语义确实不同才增加 UI 映射。
- 应用偏好：设置契约模型与端口（`domain/settings`）在 Domain 统一表达；Proto 存储 DTO 与
  Repository 实现在 Data（`data/local/datastore`、`data/repository/settings`）。通知事件模型
  （`AppNotice`）属于 App 层，通知设置值属于 Domain 设置模型。
- 外部格式 model：备份模型归 `app/feature/backup/internal/archive`；Proto 或系统 API 模型放在各自适配边界。

Mapper 应显式处理缺失字段、版本和错误，不用强制类型转换掩盖边界问题。

## 页面协作职责

页面协作按状态、任务和资源的真实 owner 划分，不按文件长度或类名增删层级：

| 位置 | 职责边界 |
|---|---|
| ViewModel | 拥有页面状态和页面异步任务；详情页通过 `DetailWorkSession` 校验异步结果的访问凭据。Domain 不持有页面 job。 |
| Route | 负责生命周期收集、导航、系统权限和 UI effect；业务写入交给应用服务或端口。 |
| Mapper | 仅在存在结构、格式或展示转换时保留。历史比较使用 `DetailRevisionUiMapper`；备份映射维护稳定外部格式边界。 |
| Coordinator | 仅在对象确实持有跨回调的会话、任务或临时状态时建立；例如 `EntryEditorSeedCoordinator` 管理编辑器种子的认证会话归属。 |
| 历史操作 | 比较、揭示、恢复保留在具有授权或业务编排的应用能力中；修订元数据由详情页通过 Domain Repository 观察。 |
| Data 事务 | `EntryUpdateTransaction` 原子写入内容、快照与活动；Executor 管理事务入口和提交后资源回收，不持有页面状态。 |

页面不因直接使用纯 Domain 枚举或端口而增加一份空壳 UI 类型或 use case。长文件可以继续保留关联的生命周期工作，
拆分必须形成可辨认的状态、资源或业务所有权，不能仅把调用移到另一个同参转发类。

## 自动检查

`verifyArchitecture` 通过 `verifyModuleBoundaries` 扫描项目依赖、源码 import 和代码中的完全限定名引用，并包含以下禁止边：

```text
domain -> com.aozijx.passly.data
domain -> com.aozijx.passly.feature
feature（含页面） -> com.aozijx.passly.data
feature 非 UI -> com.aozijx.passly.feature.<任意业务>.presentation
security -> *.data.repository / *.data.local
```

应用内备份的数据库快照适配器是已知例外；允许清单必须精确到文件或接口并附理由，不能通过全局排除关闭检查。

`SourceBoundaryPolicy` 按 `feature/<业务>/presentation` 的单层业务路径和对应包名识别页面；新增业务无需
登记名称。非 UI 源码不得反向导入任意业务的 Presentation，页面仍受 Data 实现导入禁令约束。
匹配按完整路径段和包名段处理，`presentation_models` 等相似名称不作为页面。Windows 与 Unix 路径统一处理；
带别名或通配符的 import、使用反引号标识符的包段及未导入的完全限定名引用仍执行同一规则；Kotlin 点号续行按完整引用检查并将诊断定位到引用起始行。注释和字符串字面量文本不作为依赖，字符串模板 `${...}` 内的可执行表达式继续检查完全限定名。受审查的精确文件例外保留在规则中。

### 跨 Feature 集成

生产 Feature 源码不能直接依赖另一个 Feature；Route、系统 Activity 和窄能力合同等确有 owner 的协作，必须在 `SourceBoundaryPolicy.featureIntegrationAllowances` 按源文件、目标 Feature、允许包前缀、owner 和理由逐项登记。测试源码不受该生产依赖规则限制。当前登记为：

| 依赖边 | 允许内容 | Owner / 边界 |
| --- | --- | --- |
| Vault → Scanner | `feature.scanner.presentation.integration` 中的扫描覆盖层与结果处理合同 | Scanner 封装摄像头、识别和结果展示；Vault Route 只提供领域解释/宿主操作并管理可见性。禁止导入 Scanner 的 Route、状态、相机与 ViewModel 实现包。 |
| Settings → Vault | `CardStyleRegistry`，仅限 `InterfaceScreen.kt` | 设置页组合已注册的 Vault 样式预览，并持久化稳定 presentation key；其他 Vault API 仍禁止 |
| Settings → Backup | `BackupOperationRoute` | Settings 持有页面和 SAF Activity Result launcher；Backup 内部存储实现不可导入 |
| Settings → Database | `DatabaseResetRoute` | Settings 持有 Data Management 宿主；Sheet owner 出入时清理终态，但不取消 Resetting 中的确认操作 |

每一条登记都只允许指定源码文件中的指定包前缀；新增业务实现依赖或未经登记的页面集成会使 `verifyArchitecture` 失败。App composition root 连接 Feature 不算 Feature 之间的依赖。

全局 `AppRoute` 与导航 callback 合同 `ShellNavigationContext` 由 `app.navigation` 拥有。Shell NavHost 使用该合同承载控制器，Vault/Settings 只定义并注册各自的 destination graph；它们不再导入 Shell presentation 包。全局图仍由 App 根组合连接。

`AppTheme`、`PersistedAppTheme`、`LocalAppearanceSettings` 及主题设置状态 owner 由 `app.theme` 拥有；App 主入口和 Autofill 系统 Activity 使用同一主题组合，Settings 读取中立的当前外观 CompositionLocal。`PasslyApp` 用 `AppShellViewModel.settings` 驱动主 Activity 主题；`AutofillAuthActivity` 通过 `PersistedAppTheme` 使用 Activity-scoped `AppThemeSettingsViewModel` 读取外观与圆角。两者服务不同 Activity/ViewModelStoreOwner，不互相依赖或镜像同一个页面状态。Shell 不再拥有跨应用使用的主题实现。

| 主题入口 | 类/函数 | 源码文件 |
| --- | --- | --- |
| 主 Activity 将 Shell 状态中的设置投影应用到 AppTheme | `PasslyApp()`、`AppShellViewModel.uiState.settings` | [`PasslyApp.kt`](../../../app/src/main/java/com/aozijx/passly/presentation/PasslyApp.kt)、[`AppShellViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/shell/presentation/AppShellViewModel.kt) |
| Autofill 认证 Activity 读取持久外观并应用 AppTheme | `AutofillAuthActivity.onCreate()`、`PersistedAppTheme()`、`AppThemeSettingsViewModel` | [`AutofillAuthActivity.kt`](../../../app/src/main/java/com/aozijx/passly/app/autofill/presentation/AutofillAuthActivity.kt)、[`PersistedAppTheme.kt`](../../../app/src/main/java/com/aozijx/passly/app/theme/PersistedAppTheme.kt)、[`AppThemeSettingsViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/app/theme/AppThemeSettingsViewModel.kt) |

Autofill 的 Android service、服务 Manifest 声明、领域编排适配、平台协议、收件箱导入和主要 Presentation 实现在 `:feature:autofill`；Feature manifest 提供三个服务组件与其权限、metadata、配置资源。App 仍是最终 Manifest 合并/Hilt root，并保留 `app.autofill.presentation.AutofillAuthActivity` 平台宿主与需要 App 能力的 Android 适配器。Activity 源码与包名均归属 App，避免把组合根伪装成 Feature 实现，也让 import 边界规则能完整审查它。最终 APK 合并 Manifest 中包含 Feature 的服务和 App 的唯一 Autofill 平台宿主 Activity。Activity 负责 App 主题、窗口、应用任务生命周期和最终 Android 结果回调；Security 提供统一的 `AuthenticationHost` 并在自身模块内解析认证宿主依赖，认证 UI/流程归 Security 所有；Feature 的 `AutofillFeatureEntry` 只解析 launch 并分派到 Legacy、Accessibility、Credential 专属 entry，各模式 entry 分别拥有自己的 ViewModel、生命周期取消与平台结果交付。App Activity 只通过稳定 Feature 宿主 API 进入 Autofill，不直接依赖实现类型。系统服务启用状态只供 App 的 Settings 页面展示，因此其观察接口和 Android 实现均留在 App Autofill 适配器；设置偏好通过 Domain 端口读取，Autofill inbox importer 由 Feature 自己绑定到 Domain 端口，禁止反向依赖 `:app`。

## 代码位置

| 职责 | 类/函数 | 源码文件 |
| --- | --- | --- |
| 检查源码包边界 | `SourceBoundaryPolicy` | [`SourceBoundaryPolicy.kt`](../../../buildSrc/src/main/kotlin/passly/SourceBoundaryPolicy.kt) |
| 检查 Gradle 模块依赖白名单 | `verifyModuleBoundaries` 任务 | [`ModuleBoundariesPlugin.kt`](../../../buildSrc/src/main/kotlin/passly/ModuleBoundariesPlugin.kt)、[`VerifyModuleBoundariesTask.kt`](../../../buildSrc/src/main/kotlin/passly/VerifyModuleBoundariesTask.kt) |
| 展示 Domain 到 Data 的条目映射 | `EntryProfileMapper.fromEntity()`、`EntrySecretMapper` | [`EntryProfileMapper.kt`](../../../data/src/main/java/com/aozijx/passly/data/mapper/entry/EntryProfileMapper.kt)、[`EntrySecretMapper.kt`](../../../data/src/main/java/com/aozijx/passly/data/mapper/entry/EntrySecretMapper.kt) |
