# Proto 设置单一事实源设计

## 目标

Passly 的跨启动、可由用户修改的偏好只由 `app_preferences.proto` 与 `security_preferences.proto` 定义默认值。
Domain、ViewModel、Controller 和 Compose
不得再次声明同一设置的默认值。没有持久化记录时，应用使用 Proto 生成类的默认实例；DataStore 读取完成后，
同一状态流更新为用户已保存的值。

Proto 是持久化协议，不是业务规则容器。只有产品中已经存在读取与写入入口、确实需要跨启动保留的用户选择才进入
schema；校验、状态机、排序稳定性、运行时能力判断和一次性工作流参数仍由 Domain 或 Application 层负责。

本次允许重排消息和字段、重新分配字段号，不兼容旧的 `app_settings.pb` wire layout。普通偏好使用
`app_preferences.pb`，安全偏好使用 `security_preferences.pb`，都不会用新 schema 解析旧文件；旧字段不写运行时兼容分支，
新 schema 作为干净的设置格式重新开始。旧文件只包含非秘密偏好，升级后不再读取，也不在运行时添加专门删除逻辑。

## 设置边界

以下值需要跨启动保留，必须进入 Proto：

- 外观：主题模式、动态颜色、主题键、画布染色强度、语言、字体。
- 界面：系统栏、顶部栏和快捷筛选栏折叠、圆角。
- 安全：安全窗口、翻转锁定、翻转退出、后台锁定、锁定超时、生物识别密钥失效策略、敏感复制再认证、
  剪贴板清理开关和延迟。
- 交互：滑动开关、左滑动作、右滑动作、下拉搜索开关。
- 自动填充：总开关、展示方式、Credential Manager、认证要求、OTP、保存提示、不匹配建议和最大建议数。
- Vault 视图：排序字段、方向、收藏置顶和层级显示方式。
- 消息：总开关、系统通知开关以及每个固定主题的开关和最低等级。
- 备份：已由用户通过 SAF 选择的目录 URI。

以下值不是用户持久化设置，不进入 Proto：

- 系统自动填充服务、生物识别硬件和通知权限的实时可用状态。
- 页面加载、搜索输入、弹窗、sheet、待处理导出和一次性 effect。
- 缓存与日志占用、当前认证会话和恢复码临时展示状态。
- 密码、密钥、OTP secret、恢复码明文及其他敏感数据。
- 排序 tie-breaker、数值范围、状态转换、错误降级和自动填充执行策略等业务规则。
- 备份 sheet 当前选择的格式、导入模式、包含项和条目类型；这些是单次工作流参数，不是现有持久化偏好。
- 没有写入入口的卡片展示预留配置；不得为未来可能使用的能力提前扩张持久化协议。

## 存储文件边界

运行时涉及三个职责不同的 Proto DataStore 文件：

- `bootstrap.pb`：保持现有 `bootstrap.proto`、`BootstrapData`、运行时文件和 `ProtoVaultBootstrapStore` 不变；
  保存认证引导、密钥信封、验证标签、生物识别绑定和轮换日志，由认证流程按需读取，不是常驻设置流。
- `security_preferences.pb`：安全窗口、翻转锁定、后台锁定、锁定超时、生物识别密钥策略、敏感复制再认证和
  剪贴板清理偏好。
- `app_preferences.pb`：外观、界面、交互、下拉搜索、自动填充、Vault 视图、消息和备份偏好。

`revision_history_maintenance.pb` 只由条目修订历史维护任务按需读取，不属于启动设置。代码中不存在
`diagnostics_settings.pb`；文档中的旧引用删除，诊断日志继续使用自己的内部日志文件而不是设置 DataStore。

普通设置与安全设置分别拥有 serializer、DataStore accessor、Proto 根消息和 repository 实现。二者不能互相
读写字段，也不能通过一个聚合 builder 做跨文件更新。页面若同时需要两类状态，只在 presentation 层组合两个
`StateFlow`；单次写操作始终只落入一个文件。

## Proto schema

`AppPreferences` 按 appearance、interface、interaction、vault、message、backup 的稳定顺序组织；
`SecurityPreferences` 单独持有安全偏好。两个新 Schema 文件分别命名为 `app_preferences.proto` 和
`security_preferences.proto`，生成消息、运行时文件与 Store 类一一对应。消息和字段按语义连续编号，
不保留当前布局中的空洞。根消息不添加无读取、拒绝或迁移行为的 `version` 字段；将来真正需要迁移路由时，
再随迁移规则一起引入版本字段。

固定集合优先使用 Proto enum：

- `SwipeActionPreference`
- `AutofillPresentationPreference`
- `VaultSortFieldPreference`
- `SortDirectionPreference`
- `EntryHierarchyDisplayModePreference`
- 已经存在的主题和消息等级 enum

每个新 enum 的 0 值均为语义不可用的 `*_UNSPECIFIED`，使用 enum 的字段必须通过 `[default = ...]`
显式指定一个合法默认项。Domain mapper 穷尽处理合法项；收到显式 `UNSPECIFIED` 时抛出
`IllegalArgumentException`，不得静默映射到另一个合法偏好。Proto2 的未来未知 enum 数值保留在 unknown field 中，
当前生成 getter 将字段视为未设置并返回 schema 的显式默认值。

可扩展或天然字符串的值继续使用 string，例如主题键、BCP-47 语言标签和目录 URI。

固定消息主题不再依赖 domain 中的 `defaultMessageTopicSettings()`。Proto 为 clipboard、app lifecycle、backup、
security 和 database 分别提供具名字段，各字段默认值只表达用户可配置的开关和最低等级。目录 URI 使用 Proto
空字符串表达“尚未选择”。

`pull_to_search_enabled` 与左右滑动作直接属于 `InteractionPreferences`。Vault 的 `sort` 即使未显式写入，
也通过 `VaultSortPreference.getDefaultInstance()` 获得用户可选的主排序、方向和收藏置顶偏好，不再回退到
`EntrySort.DEFAULT`。稳定 tie-breaker 固定由 Domain 查询规则补充，不持久化到 Proto。

## 运行时数据流

两个 repository 直接映射各自 `DataStore.data`，沿用现有 Domain `Flow<T>` 契约。DataStore 在文件不存在时通过
Serializer 的 `defaultValue` 发出 Proto 默认实例，因此不需要为每个设置属性创建应用级 eager `StateFlow`、专用
CoroutineScope 或常驻 collector。页面和 Controller 在自己的既有生命周期中收集所需流，不新增第二套状态缓存。

`DeviceLockController` 只收集 `SecuritySettingsSource.security`；AppShell、设置 ViewModel、Vault 查询和自动填充策略
继续收集各自 source。业务层不得在流映射中写 `?: 60000`、`EntrySort.DEFAULT` 等持久化回退值，默认快照由
Serializer 与 Proto 字段默认自然发出。

## Domain 与 presentation

Domain 保留现有 `Flow` 端口、合法范围、稳定排序、状态转换、业务类型和校验函数。只删除实际读取链路中与 Proto
重复的持久化回退，不为本任务全面重写 data class 构造方式。解析 Proto enum 必须覆盖合法项；遇到不可用值时抛错，
不得猜测另一个合法偏好。

Presentation 可以保留纯 UI 初始值，例如空搜索词、关闭的对话框和 Loading。持久化设置在现有生命周期中收集
repository Flow；本任务不引入同步 `StateFlow.value` API，也不把无关 UI 状态初始化一并重构。

`EntrySort.DEFAULT` 和用于表达“默认用户排序”的 UI 常量删除。创建时间降序是 Proto 配置值，不是 Domain 全局常量；
排序菜单用实际排序值判断选中项。`EntrySort` 仍由 Domain 保证稳定 tie-breaker 和字段合法性。

## 写入与错误处理

写操作只更新对应文件中的 Proto 子消息，其他字段由 builder 保留。边界值在写入前按 domain 约束验证或规范化；
损坏的 Proto 输入继续由 Serializer 抛出 `CorruptionException`，不静默重置整个设置文件。

因为 schema 明确不兼容旧 wire layout，不增加旧字段读取、旧字符串动作映射或版本桥接代码。切换到新的
`app_preferences.pb` 与 `security_preferences.pb` 后，首次启动直接由各自 Proto 默认实例建立偏好；Vault 数据库、密码与
其他业务数据不受影响。

## 验证

测试关注可观察行为，不增加源码防回退测试：

- Proto 默认实例映射为完整 domain 设置，包含下拉搜索关闭、创建时间降序、左滑复制密码和右滑删除。
- 每个设置 repository 首次发出值与对应 Proto 默认实例一致。
- 保存一个设置只改变对应 Proto 字段，并保留同消息的其他值。
- `DeviceLockController` 从安全偏好流取得 Proto 默认或已保存值，不在 Controller 中声明持久化回退。
- 消息五个固定主题均由 Proto 默认实例形成完整 map。
- 现有 ViewModel、Vault 查询、自动填充与安全生命周期测试改为传入完整设置模型。

最终执行 domain、data、app 单元测试，`verifyModuleBoundaries`、`lintDebug` 和 `assembleDebug`。
