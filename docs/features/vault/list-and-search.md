# Vault 列表、搜索与排序

状态：当前实现（代码核对：2026-10-18）。

列表只查询可展示摘要；密码、OTP secret、私钥等高敏值通过明确授权的窄入口按需读取。
修订历史是可恢复的版本快照，访问活动记录查看、复制和 Autofill 等行为，两者不是同一数据源；
具体合同见[条目修订历史](entry-revision-history.md)。备份、回收站和数据库维护不复用主列表的 Paging 配置，
分别遵守自己的事务和授权边界。已批准但尚未实现的编辑架构见[条目编辑器与写入架构](entry-editor-and-persistence.md)。

## 查询

`EntryListQuery` 统一承载搜索、快捷筛选、标签分类、排序和层级模式。Room 在同一查询中完成过滤、排序和
分页，UI 不对已加载页面二次过滤或重排。

- 搜索覆盖标题、用户名、主网址、标签、域名和应用 ID；输入 trim/lowercase，并转义 `%`、`_`、反斜杠。
- 快捷筛选包括全部、密码、TOTP，按 capability flag 过滤，不按 EntryType 猜测能力。
- 标签分类通过 `json_each` 发现，忽略大小写并稳定排序；编辑与导入入口必须规范化标签空白。
- 输入延迟 250 ms。查询参数变化通过 `flatMapLatest` 取消旧 Pager。

数据库整体由 SQLCipher 保护，因此摘要可参与 SQL 查询；高敏值不进入分页行。

## 排序

当前首次安装默认来自 `app_preferences.proto`：**创建时间降序、收藏置顶、Entry ID 最终兜底**。Domain 的
`EntrySort.DEFAULT` 当前保持同一值，用于构造完整查询模型，但持久化读取由 Proto 默认实例提供。

排序优先级为收藏置顶（启用时）→ 主字段 → 次级字段 → Entry ID。主字段不得和 tie-breaker 相同。标题使用
SQLite `NOCASE`；从未使用的条目在最近使用升/降序中都排末尾；Entry type 按 Domain 声明顺序而非数据库字符串。

任何新增排序字段都必须同时定义 SQL 表达式、null 位置、稳定方向、Proto 映射和 UI 文案。最终 Entry ID 排序
不可省略，否则 Paging 失效刷新会产生重复或遗漏。

## 层级

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

## 滑动默认值

Proto 当前默认关闭滑动操作；启用后左滑复制密码、右滑删除，不再提供“详情”动作。显示和执行都必须读取
`InteractionSettings`，不能在 UI 重新定义另一套默认。

相关实现：

- [`EntryListQuery`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/model/query/EntryListQuery.kt)
- [`EntrySort`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/model/query/EntrySort.kt)
- [`EntryPagingQueryFactory`](../../../data/src/main/java/com/aozijx/passly/data/local/database/query/EntryPagingQueryFactory.kt)
- [`VaultViewModel`](../../../app/src/main/java/com/aozijx/passly/presentation/feature/vault/list/VaultViewModel.kt)
