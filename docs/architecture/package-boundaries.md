# 包边界

## 职责表

| 包          | 可以包含                                      | 不应包含                                  |
|------------|-------------------------------------------|---------------------------------------|
| `core`     | Android 平台适配、权限、共享 Compose UI、密码学与遥测能力          | DAO、Repository 实现、业务页面                |
| `domain`   | 领域模型、端口、策略、验证及设置契约                    | `android.*`、资源 ID、Entity、Feature UI 状态 |
| `data`     | Room、Proto DataStore、Mapper、Repository 实现 | Compose 页面、业务流程、备份协议与格式              |
| `core.crypto` | 无状态密码学原语、算法实现、加密格式                    | 认证流程、持久化、Android Keystore          |
| `security` | Android Keystore、认证方式、授权与安全会话编排                 | Room Entity、DataStore 具体实现、Feature UI |
| `feature/<name>` | app 内的业务流程、平台入口、能力合同与应用策略 | Data 实现；非 UI 代码不依赖 presentation |
| `app` 适配器 | Android Application、DI 与跨模块实现连接；Backup 快照可读取 Data | 页面状态、领域规则真相源 |
| `feature/<name>/presentation` | 对应业务的 Route、ViewModel、UI state/effect、Compose UI | DAO、Entity、Data Repository 实现 |
| `presentation/shared` 等 | 跨业务的纯展示组件与映射 | 具体业务流程与数据实现 |
| `service`  | Android Service 入口与系统生命周期桥接；与单一业务流程紧耦合的实现可放在对应 `feature/<name>` 下 | 独立业务真相源 |

真实模块还要求源码命名空间与职责一致：`:data` 只包含 `com.aozijx.passly.data.*`。`:core:common` 保持为
Domain 可依赖的纯 Kotlin 错误与通用能力；密码学、遥测、共享 Android 与 Compose 能力合并在 `:core`。
DEK、字段派生密钥和信封原语当前位于 `:core` 的 `security.dek/security.envelope` 包；Android Keystore、
锁状态和认证编排归独立的 `:security`。应用消息调度与全局框架桥归 `:app`。

每个业务只有一个 `feature/<name>` 入口；页面统一放在其 `presentation` 子包，包括 Recovery 和
Database Reset。根 `presentation` 仅保留应用组合入口及其 Activity 宿主、跨业务展示组件和展示映射。`core` 的共享指
跨业务使用的技术能力；包含业务语义的纯规则优先进入 Domain，不因被多个页面引用就放进 Core。

## 强制规则

```text
domain -X-> data | feature | android.*
core   -X-> data repository implementation
feature 非 UI -X-> presentation | data implementation
feature presentation -X-> data implementation
security-X-> data implementation
app feature -X-> 任意散落的 data implementation import
```

`:security` 可以依赖 Domain 的认证/会话端口、`:core` 的密码学能力和 `:runtime:session` 的资源无关状态机，
但不能依赖 Room、Data Repository 实现、Compose 页面或 Feature 状态。Feature 只能通过 Domain 端口请求认证、
授权和锁定，不直接导入 `security.authentication`、Keystore 或密钥实现。

App 通过 Gradle 依赖 `:data` 让 Hilt 聚合实现。Backup 的 `BackupSnapshotGateway` 是能力侧合同；
`app/database/backup` 的 Room Reader、Restorer、恢复文件事务、Gateway 实现和 Hilt binding 负责与 Data 集成。
恢复器写入 Room 附件状态时复用 Data 的显式存储键映射，不从 Domain enum 名称推断持久化值。
稳定键映射器位于 `feature/backup/internal/archive/mapper`，只依赖 Domain 与归档模型。
`SourceBoundaryPolicy` 对全部 `feature` 禁止 Data 实现 import，不为 Backup 另开例外。
Autofill 等 Android Service 需要打开页面时，由 Feature 中的启动契约与 `app` 适配器衔接，Feature 不引用 Activity 类。

分页应通过纯 Kotlin 契约传递；Android Paging 可留在 Data/UI 适配边界。Feature 需要条目数据时依赖
Entry Domain 端口，不直接构造 Entity 或具体 Repository。Autofill/Credential Manager 的平台流程和候选编排
留在 App；只有可复用的条目匹配语义进入 Entry 领域。卡号校验和 Wi-Fi 二维码载荷解析属于纯规则，
位于 `domain/entry/policy`；相机、扫码结果展示与导入编排仍在 App Feature。

`EntryFieldReader` 的默认实现也位于 `domain/entry/policy`，App 仅通过 DI 连接。读取按 `FieldKey`
穷尽映射，不按枚举名称或前缀猜分组；新增字段必须明确其读取语义。凭据不存在或高敏值未揭示时返回
无值，不编造 OTP 默认配置。读取器不授权、不访问仓库、不解密；复制与揭示流程继续在 Feature 中授权。
布尔值使用 `true` / `false`，时间戳保留毫秒值，列表沿用逗号加空格、恢复码使用换行分隔；展示层负责本地化。
`CUSTOM_FIELDS` 是带名称和类型的集合，不隐式拼成单个字符串，仍由专属流程处理。

`EntryDraftMaterializer` 位于同一 Domain 策略包，只将草稿和给定身份转换为条目；不生成 ID、不读取会话、
不记录日志或写入仓库。Feature 的 `CreateEntryUseCase` 负责校验、会话检查、身份生成与持久化，App DI 连接
纯转换实例。新增凭据字段时在 Domain 测试其转换，不能把持卡人等凭据值借用为 Profile 用户名。
`EntryDraft.invalidProvidedFields` 校验所有已提供字段的归属、值类型和空数字；创建用例与物化器共用该检查，
不能假定所有草稿都经由 `withValue` 构造。必填检查与完整凭据校验分别处理缺失字段和具体业务规则。

OTP 生成、密钥编解码和 `otpauth:` URI 协议位于 `domain/entry/otp`；App 负责扫码、编辑状态、授权与展示，
不另留 Android Uri 实现或仅用于测试的解析、格式化转发接口。
手动输入的原始数字与密钥通过 Domain 的 `OtpConfigInput` 解析；页面仅保留输入和显示错误，
不在草稿映射中用默认值掩盖无效输入。当前规则见[动态口令创建](../features/vault/otp-entry.md)。

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
页面的可选状态与年份选择范围留在 Presentation；[银行卡创建与有效期](../features/vault/card-entry.md)说明当前写入边界。

## 模型归属

- Entity：数据库结构，只在 Data 层。
- Domain model：业务语义与跨层契约。
- UI model/state：展示派生状态，只在 Feature presentation/UI。
- 设置策略枚举若展示语义与 Domain 完全相同，Feature UI 可直接使用 Domain 类型；只有展示结构或语义确实不同才增加 UI 映射。
- 应用偏好：设置契约模型与端口（`domain/settings`）在 Domain 统一表达；Proto 存储 DTO 与
  Repository 实现在 Data（`data/local/datastore`、`data/repository/settings`）。通知事件模型
  （`AppNotice`）属于 App 层，通知设置值属于 Domain 设置模型。
- 外部格式 model：备份模型归 `app/feature/backup/internal/archive`；Proto 或系统 API 模型放在各自适配边界。

Mapper 应显式处理缺失字段、版本和错误，不用强制类型转换掩盖边界问题。

## 页面协作职责核查

本轮按真实状态所有权核查安全设置、编辑器、详情历史、Autofill 与备份，不按类名增删层级：

| 位置 | 保留的职责与调整 |
|---|---|
| ViewModel | 生命周期、观察、UI 状态及事件；安全设置从已提交的配置 Flow 派生，详情异步发布仍由 `DetailWorkSession` 校验访问凭据。没有把页面 jobs 下沉到 Domain。 |
| Route | 生命周期收集、导航、系统权限和 UI effect；通知权限结果与请求身份相关，保留在 Route。业务写入仍交给应用服务或端口。 |
| Mapper | 只有实际的结构、格式或展示转换才保留；历史比较单独归 `DetailRevisionUiMapper`，字段行负责本地化。删除活动类型的同名 UI 枚举与逐项照抄映射。备份 Mapper 保留稳定外部格式边界。 |
| Coordinator | `EntryEditorSeedCoordinator` 持有导航数据的认证会话所有权；`AppPasswordDialogCoordinator` 持有临时对话框与草稿，两者均有实际 UI 生命周期职责。 |
| 历史应用服务 | 比较、揭示、恢复具有授权或业务编排，保留；删除仅转发初始化与 Flow 的 `ObserveEntryRevisionsUseCase`，页面直接使用 Domain 修订端口。 |
| Data 事务 | `EntryUpdateTransaction` 只做原子内容重写、快照与活动；Executor 负责事务入口和提交后资源回收，不让数据库事务持有页面状态。 |

页面不因直接使用纯 Domain 枚举或端口而增加一份空壳 UI 类型或 use case。长文件可以继续保留关联的生命周期工作，
拆分必须形成可辨认的状态、资源或业务所有权，不能仅把调用移到另一个同参转发类。

## 自动检查

`verifyArchitecture` 通过 `verifyModuleBoundaries` 扫描项目依赖和源码 import，并包含以下禁止边：

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
带别名或通配符的 import 仍执行同一规则。受审查的精确文件例外保留在规则中。

## 代码位置

| 职责 | 类/函数 | 源码文件 |
| --- | --- | --- |
| 检查源码包边界 | `SourceBoundaryPolicy` | [`SourceBoundaryPolicy.kt`](../../buildSrc/src/main/kotlin/passly/SourceBoundaryPolicy.kt) |
| 检查 Gradle 模块依赖白名单 | `verifyModuleBoundaries` 任务 | [`ModuleBoundariesPlugin.kt`](../../buildSrc/src/main/kotlin/passly/ModuleBoundariesPlugin.kt)、[`VerifyModuleBoundariesTask.kt`](../../buildSrc/src/main/kotlin/passly/VerifyModuleBoundariesTask.kt) |
| 展示 Domain 到 Data 的条目映射 | `EntryProfileMapper.fromEntity()`、`EntrySecretMapper` | [`EntryProfileMapper.kt`](../../data/src/main/java/com/aozijx/passly/data/mapper/entry/EntryProfileMapper.kt)、[`EntrySecretMapper.kt`](../../data/src/main/java/com/aozijx/passly/data/mapper/entry/EntrySecretMapper.kt) |
