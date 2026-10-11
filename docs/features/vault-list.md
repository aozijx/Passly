# 保险箱列表

状态：当前实现。

本文描述保险箱主列表的查询、排序与 Paging 行为。回收站、备份和数据库维护属于小规模或全量操作，
不复用主列表的分页配置。

## 功能分类

### 查询与筛选

`EntryListQuery` 是主列表查询的领域契约，统一承载搜索文本、快捷筛选、分类、排序和层级展示模式。
UI 只更新这些语义参数；筛选、排序和分页边界由 Room 在同一条查询中完成，不能在已加载页面上二次过滤或重排。

- 搜索覆盖标题、用户名、主网址、标签、域名和应用 ID；输入会先去除首尾空白并转换为小写，
  `%`、`_` 和反斜杠按 `LIKE` 转义，避免被当成通配符。
- 快捷筛选包括全部、密码和 TOTP，数据库按 capability flag 过滤，不依赖具体 EntryType 猜测能力。
- 分类来自 Entry 标签，使用 `json_each`、忽略大小写分组，并返回稳定的字母顺序；选择分类后对数据库中的
  标签原值进行精确、忽略大小写的匹配。
- 搜索输入延迟 250 ms 生效。搜索、分类、排序、层级模式或外部数据刷新发生变化时，
  `flatMapLatest` 会取消旧查询并创建新的分页流。

### 排序

`EntrySort` 是设置持久化和列表查询共用的排序模型，包含主字段、方向、收藏置顶开关和稳定次级字段。
默认规则为“最近使用、降序、收藏置顶”，稳定次级字段为 Entry ID。
排序字段和层级模式使用各自的稳定 key 持久化；读取时明确区分缺失、支持和不支持的 key，不能把未知值
静默替换成默认排序或折叠模式。

| 排序字段 | 数据来源 | 默认稳定方向 | 特殊规则 |
| --- | --- | --- | --- |
| 标题 | Entry 结构化摘要 | 升序 | SQLite `NOCASE` 比较 |
| 创建时间 | Entry 时间戳 | 降序 | 无 |
| 修改时间 | Entry 时间戳 | 降序 | 无 |
| 最近使用 | Usage Activity 的最大时间 | 降序 | 从未使用的条目在升序和降序时都放在末尾 |
| 使用频率 | Usage Activity 数量 | 降序 | 没有使用记录时按 0 处理 |
| Entry 类型 | Domain 中 `EntryType` 的声明顺序 | 升序 | 不按数据库字符串字母顺序排列 |
| Entry ID | Entry 主键 | 升序 | 作为最终稳定边界，保证跨页顺序确定 |

排序按以下优先级生成：收藏置顶（启用时）→ 主排序字段 → 次级字段 → Entry ID。
主字段不能与次级字段相同。即使主字段值相等，结果也必须保持确定顺序，否则 Room 失效刷新时可能出现跨页重复或遗漏。

层级展开模式下，Account 根条目及其成员使用根条目的收藏和排序值确定组位置，组内固定先显示根条目，
再按当前条目排序规则排列成员。这样一个 Account 组不会被 Paging 切成彼此无关的排序位置。

### 层级展示

层级投影在 SQL 排序和分页切片之前完成：

- `COLLAPSED`：普通列表隐藏已有有效 Account 归属的成员，只显示 Account 根条目和独立条目。
- `EXPANDED`：Account 根条目和成员组成连续分组。
- `SEPARATE`：隐藏 Account 根条目，单独显示成员和其他条目。

搜索或分类属于定向查找。此时 `COLLAPSED` 和 `EXPANDED` 不隐藏匹配成员，也不强制全局分组，
避免用户搜索到实际条目却看不到结果；`SEPARATE` 仍保持“不显示 Account 根条目”的语义。
层级模式只应用于“全部”快捷筛选，密码和 TOTP 页直接按能力查询。

### Paging

主列表使用 Room `PagingSource`。Data 层负责把 `EntryListQuery` 转成 SQL，并在每页中一次返回：

- 可直接展示的结构化 Entry 摘要；
- 使用次数与最近使用时间；
- Account 归属关系。

App 层拥有 UI 加载策略，当前 `PagingConfig` 为：

| 参数 | 值 | 目的 |
| --- | ---: | --- |
| `pageSize` | 30 | 常规加载批次 |
| `initialLoadSize` | 60 | 首屏和预滚动区域一次准备两页 |
| `prefetchDistance` | 10 | 距离页尾 10 项时预取 |
| `enablePlaceholders` | `false` | 不为未加载条目创建占位对象 |
| `maxSize` | 180 | 限制内存中保留的分页窗口 |

每个可见快捷筛选拥有独立且由 `viewModelScope` 缓存的 Paging 流，横向切换页面时可以保留已加载窗口。
Compose 使用 `LazyPagingItems` 消费数据：首次加载显示全页进度，首次加载失败显示重试，追加加载在列表末尾显示进度或重试；
条目以 Entry ID 作为稳定 key、以 EntryType 作为 content type。

### 会话生命周期

分页读取必须同时满足应用已授权和数据库会话为 `UNLOCKED`。`RoomEntryPagingStore` 在会话的
`observeFlow` 租约中创建并消费 Pager，使 PagingSource 的每次读取都受数据库生命周期约束。

锁定或 seal 时，可读状态立即变为 false，`flatMapLatest` 取消旧分页流并向 UI 提供空数据；重新解锁后创建新的 Pager。
不得在同步的 PagingSource factory 中使用 `runBlocking` 获取数据库，也不得缓存跨会话的 DAO 或数据库实例，
否则旧 PagingSource 可能在锁定后继续访问已关闭的 SQLCipher 数据库。

### 搜索交互所有权

搜索查询和“是否启用搜索”属于页面业务状态，由 `VaultViewModel` 通过 `VaultUiAction` 更新。搜索框焦点、
下拉展开进度、返回键收起和页面暂停后的视觉阶段属于瞬时 Compose 状态，由
`VaultSearchStateHolder` 单独持有。TopBar、筛选栏和列表 Section 直接发送页面级 Action，不再经过
`VaultListEvent` 或 TopBar 专属 Action 的一对一翻译层。

搜索焦点采用单向控制：点击搜索框或下拉手势先向 `VaultSearchStateHolder` 发送显式编辑请求，状态机进入
`EDITING` 后输入框才允许获得焦点并由唯一的焦点 Effect 请求键盘。浏览或结果阶段禁止输入框接受系统焦点
恢复；焦点回调只确认已请求的焦点或处理焦点丢失，不能反向启用搜索。这样重新进入应用时不会把残留焦点
解释成新的搜索操作。

浏览态用于接收“点击后进入搜索”的透明命中层只能使用 `BoxScope.matchParentSize()` 覆盖已经测量完成的
搜索输入框，不能用 `fillMaxSize()` 参与 TopBar title 的尺寸测量。否则 title 会被窗口最大约束撑高，搜索框
挤占列表，依附同一锚点的 More 按钮也会被错误地垂直居中到整屏。

下拉搜索只在列表顶部累计位移，并把累计值限制在启动阈值内，不保留 UI 无法表达的超额距离。达到阈值后
仍可在松手前上滑：反向位移会立即收回展开进度，低于阈值时松手取消搜索；抵消完已累计距离后，剩余上滑
继续交给列表滚动。只有松手时仍停在阈值才发送显式编辑请求。

More 菜单的展开状态只属于稳定的搜索动作锚点；`DropdownMenu` 始终保留在该锚点下，仅由 `expanded`
控制显示，避免 Popup 被 More/Clear 动画层反复创建。菜单页和查询状态以展开会话为 key，关闭后自然重置。
菜单渲染数据集中在 `VaultMenuUiState`，`VaultSearchAction` 直接连接 `VaultDropdownMenu`，不保留只拆参数和转发
回调的中间组件。主菜单与排序、筛选子页使用 Material expressive 交叉淡化；目标页独立决定自然 intrinsic 尺寸，
淡出的旧页不参与容器尺寸，外层仅对目标页自然尺寸变化做动画，因此不会被撑到 Material 最大宽度或留下多余右侧空白。
固定锚点及 Popup 显隐不参与切页动画。
主菜单依次提供排序、筛选、TOTP 显隐、分隔线、扫描二维码和设置。扫描与设置都通过单一
`VaultRouteRequest` 离开 UI 回调链；菜单先关闭 Popup 再发出请求。`VaultRoute` 用显式 `VaultOverlay`
持有 Scanner overlay 状态，并把首页确认的 OTP 转成 `VaultUiAction.AddScannedOtp`。Wi-Fi 扫码结果交给
`VaultNavigation`，由 Vault 图作用域的一次性 seed 协调器生成不含敏感内容的 token，再进入 Wi-Fi 编辑页。
Scanner 状态和值不进入 `VaultViewModel`，设置仍由 `VaultNavigation` 连接。新增页面同样属于 Route 负责连接的导航出口。

列表与详情共用的 OTP runtime 只发布 `Pending / Ready / Failed` 三种状态；验证码和倒计时只存在于
`Ready`，错误只存在于 `Failed`。Presentation 对其做穷尽映射，不再用 `code == null`、`isLoading`
和 `error != null` 组合猜测阶段。回收站同样把内容加载状态与恢复/永久删除/清空操作状态分开，避免同一条目
同时处于多种操作或“加载完成但仍 loading”的非法组合。

## 注意事项

- SQL 的 `ORDER BY` 必须始终包含确定性的最终 Entry ID；新增排序字段时，要同时定义 SQL 表达式、
  稳定方向、设置序列化和 UI 文案。
- 新增可空排序字段时要显式规定 null 位置，不能依赖 SQLite 在升序和降序下不同的默认行为。
- 搜索、筛选、分类和层级投影必须在数据库查询中完成；对单个 `PagingData` 页面做内存过滤或排序会破坏全局结果。
- usage 与关系表参与查询并列入 Room 的 `observedEntities`；修改这些表后应由 Room 自动使 PagingSource 失效，
  普通增删改不需要手动刷新。
- 分类发现会对标签执行 `TRIM`，当前分类筛选只对标签原值执行 `LOWER`。正常编辑入口会去除标签首尾空格，
  但新增导入入口时必须继续规范化标签，或同步把筛选 SQL 改为 `LOWER(TRIM(value))` 并补充测试，
  否则带首尾空格的标签可能出现在分类列表中却筛选不到。
- `cachedIn(viewModelScope)` 应放在动态查询切换之后；放在查询组合之前会复用错误参数，完全省略则会在 UI 重组后重复加载。
- 摘要字段可被 SQL 查询是因为数据库文件整体受 SQLCipher 保护；密码、OTP Secret、私钥等高敏内容仍需独立加密，
  不进入列表分页行。
- 动态 SQL 变化时，应同步验证搜索、分类、usage/account 组合、最近使用 null 顺序、类型顺序和层级模式，
  尤其注意真实 Room 与 SQLite 的排序和失效行为。

## 相关实现

- [`EntryListQuery`](../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/model/query/EntryListQuery.kt)
- [`EntrySort`](../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/model/query/EntrySort.kt)
- [`EntryPagingQueryFactory`](../../data/src/main/java/com/aozijx/passly/data/local/database/query/EntryPagingQueryFactory.kt)
- [`RoomEntryPagingStore`](../../data/src/main/java/com/aozijx/passly/data/repository/entry/paging/RoomEntryPagingStore.kt)
- [`VaultViewModel`](../../app/src/main/java/com/aozijx/passly/presentation/feature/vault/list/VaultViewModel.kt)
- [`VaultScreen`](../../app/src/main/java/com/aozijx/passly/presentation/feature/vault/list/ui/VaultScreen.kt)
- [`VaultSearchStateHolder`](../../app/src/main/java/com/aozijx/passly/presentation/feature/vault/list/ui/search/VaultSearchStateHolder.kt)
