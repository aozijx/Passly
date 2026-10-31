# Vault 列表、搜索与排序

状态：当前实现（代码核对：2026-10-18）。

列表只查询可展示摘要；密码、OTP secret、私钥等高敏值通过明确授权的窄入口按需读取。
修订历史是可恢复的版本快照，访问活动记录查看、复制和 Autofill 等行为，两者不是同一数据源；
具体合同见[条目修订历史](entry-revision-history.md)。备份、回收站和数据库维护不复用主列表的 Paging 配置，
分别遵守自己的事务和授权边界。创建表单、四种类型的完整编辑、详情逐项编辑及当前未覆盖的类型见[条目编辑器与写入架构](entry-editor-and-persistence.md)。

## 回收站读取

回收站单独订阅 `EntryListQueryRepository.deletedEntries`。加载失败保持明确的失败状态，显示重试入口，
不展示“回收站为空”；关闭操作错误也不能将读取失败改成空列表。`RetryLoad` 只在读取失败时重建订阅，
立即进入 Loading，重复点击不会创建重复订阅；恢复后继续以仓库 Flow 更新列表。
每次读取有独立任务票据，替换或销毁时取消旧 Job，迟到数据和错误不能发布。销毁后清理列表状态并拒绝重试。
重试只重建读取，不执行移动、恢复、永久删除或清空操作；回收站命令统一由 `TrashCommandUseCase` 执行会话门禁、授权和版本合同。移动操作在授权后读取当前条目版本，再以该版本写入，避免使用列表中的陈旧版本。
恢复、永久删除及清空在启动协程前同步占用操作状态，排队等待调度或授权时也会拒绝其他写入请求。
授权取消后回到 Idle，允许后续重新发起；ViewModel 销毁会取消尚未执行的写入协程，不继续访问仓库。
仓储或授权边界抛出的非取消异常按操作分别映射为“命令失败”或“清空失败”，并释放忙碌状态以允许重试；`CancellationException` 原样传播，不显示为操作失败。

## 查询

创建选项由 `VaultEditorDestinationRegistry` 的实际目的地派生，经列表 UI model 传给 FAB 与创建弹层；
没有创建目的地的类型不展示创建入口。筛选选项独立保留，不能因缺少创建页而隐藏已有或导入条目的类型筛选。

`EntryListQuery` 统一承载搜索、快捷筛选、标签分类、排序和层级模式。Room 在同一查询中完成过滤、排序和
分页，UI 不对已加载页面二次过滤或重排。

- 搜索覆盖标题、用户名、主网址、标签、域名和应用 ID；输入 trim/lowercase，并转义 `%`、`_`、反斜杠。
- 快捷筛选包括全部、密码、TOTP，按 capability flag 过滤，不按 EntryType 猜测能力。
- 标签分类通过 `json_each` 发现，忽略大小写并稳定排序；编辑与导入入口必须规范化标签空白。
- 有效搜索文本先去重，再防抖 250 ms；无关 UI 状态刷新及只改变首尾空白的输入不会重置搜索截止时间。
  分类、排序、层级和刷新参数继续即时传播，搜索等待期间沿用上次已生效的文本。查询变化通过 `flatMapLatest` 取消旧 Pager。

数据库整体由 SQLCipher 保护，因此摘要可参与 SQL 查询；高敏值不进入分页行。

## 排序

排序选择只发起持久化写入；页面选择和查询排序均随 Repository Flow 更新。写入失败发布本地化错误，
不提前修改查询，也不使用旧状态快照回滚，避免覆盖仓库已经确认的其他更新。
排序与交互设置共用 `writePreferenceWithFeedback` 的失败和取消处理；调用方持有协程作用域与反馈策略，
该函数不缓存偏好或拥有页面状态。成功回调只在写入返回且协程仍有效时执行，反馈回调异常不作为存储失败重报。

当前首次安装默认来自 `app_preferences.proto`：**创建时间降序、收藏置顶、Entry ID 最终兜底**。
Domain 的 `EntrySort` 要求显式传入主字段和方向；持久化读取由 Proto 默认实例提供。

排序优先级为收藏置顶（启用时）→ 主字段 → 次级字段 → Entry ID。主字段不得和 tie-breaker 相同。标题使用
SQLite `NOCASE`；从未使用的条目在最近使用升/降序中都排末尾；Entry type 按 Domain 声明顺序而非数据库字符串。

任何新增排序字段都必须同时定义 SQL 表达式、null 位置、稳定方向、Proto 映射和 UI 文案。最终 Entry ID 排序
不可省略，否则 Paging 失效刷新会产生重复或遗漏。

## 层级

- 层级偏好只由 `VaultViewModel` 读取并进入 `EntryListQuery`；Vault 展示配置只包含卡片样式，不重复投影该偏好。
- `COLLAPSED`：隐藏已有有效 Account 归属的成员；
- `EXPANDED`：Account 根条目与成员连续分组；
- `SEPARATE`：隐藏 Account 根条目，只显示成员和其他条目。

搜索或分类时，COLLAPSED/EXPANDED 不隐藏匹配成员；SEPARATE 仍隐藏 Account 根条目。层级只用于“全部”筛选。
EXPANDED 先用根条目的收藏和排序值定位组，再在组内先显示根条目并排列成员。

## Paging 和会话

当前 `PagingConfig`：pageSize 30、initialLoadSize 60、prefetchDistance 10、placeholders false、maxSize 180。
每个可见快捷筛选拥有 `viewModelScope` 缓存的独立 Paging 流。

分页读取同时要求授权和数据库 `UNLOCKED`。`RoomEntryPagingStore` 在会话 `observeFlow` 租约内创建 Pager。
锁定/seal 使可读状态立即为 false，取消旧流并返回空数据；重新解锁才创建新 Pager。禁止在 factory 中用
`runBlocking` 取数据库或缓存跨会话 DAO/数据库实例。

## 搜索与菜单所有权

查询和“是否搜索”属于 `VaultViewModel`；焦点、下拉进度、键盘、返回收起和 Popup 动画属于 Compose
`VaultSearchStateHolder`。输入框只在显式进入 EDITING 后请求焦点，系统焦点恢复不能反向开启搜索。

下拉搜索只在列表顶部累计并限制位移；松手时仍达到阈值才开始编辑，反向滑动可取消。默认
`pull_to_search_enabled=false` 来自 Proto。

菜单状态固定在搜索动作锚点下，排序/筛选子页只改变菜单内容。扫描和设置通过一个 `VaultRouteRequest` 离开
UI 回调链。Scanner overlay 由 Route 持有；扫描出的 Wi-Fi seed 通过一次性非敏感 token 进入编辑页，不放入
Vault ViewModel 或可保存状态。

Vault 展示配置由 [`VaultDisplayViewModel`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/list/display/VaultDisplayViewModel.kt) 提供；层级筛选由 [`VaultViewModel`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/list/VaultViewModel.kt) 并入查询状态。

## 滑动默认值

列表错误事件持有 `UiErrorMessage` 资源键；移动到回收站失败不直接展示内部错误码，会话受限提示也由界面按当前语言解析。
无生产者的 `ShowToast` 事件已移除。列表与详情的复制标签共用 [`CopyFieldLabels.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/common/CopyFieldLabels.kt)，
只映射字段标识到文案资源，不读取或传递凭据值；未列字段保留通用“内容”标签。详情 OTP 复制使用独立 `OtpCopied`，
不再通过可空字段键区分。Toast 在消费时使用当前 `LocalResources` 解析标签和错误文案。

Proto 当前默认关闭滑动操作；启用后左滑复制密码、右滑删除，不再提供“详情”动作。显示和执行都必须读取
`InteractionSettings`，不能在 UI 重新定义另一套默认。

交互设置的滑动开关、下拉搜索开关及左右滑动动作共用写入失败处理。页面状态始终来自 Repository Flow，
不在写入前伪造成功；开关写入成功保持静默，动作选择成功保留保存提示，失败统一发布本地化资源键。
写入返回后及失败映射前检查协程仍有效；取消继续传播，已取消的慢写入即使延迟完成也不能补发成功或失败提示。

## 代码位置

| 职责 | 类/函数 | 源码文件 |
| --- | --- | --- |
| 列表查询条件与排序规则 | `EntryListQuery`、`EntrySort` | [`EntryListQuery.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/model/query/EntryListQuery.kt)、[`EntrySort.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/model/query/EntrySort.kt) |
| 构造 SQLite 分类与分页查询 | `buildEntryCategoryQuery()`、`buildEntryPagingQuery()` | [`EntryPagingQueryFactory.kt`](../../../data/src/main/java/com/aozijx/passly/data/local/database/query/EntryPagingQueryFactory.kt) |
| 列表界面动作与可见 OTP 订阅 | `VaultViewModel.onAction()`、`subscribeVisibleOtp()`、`unsubscribeVisibleOtp()` | [`VaultViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/list/VaultViewModel.kt) |
