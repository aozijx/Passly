# Vault 列表 owner 收敛审查

## 当前实现

Vault 列表的查询、设置读取、分类恢复和 OTP 状态由图级 `VaultViewModel` 持有；列表行只请求激活可见 OTP，图 owner 幂等持有 lease，并在销毁或条目删除时释放。删除、字段复制和 OTP 复制由 `VaultEntryCommandWorkflow` 执行，异步反馈受当前页面 owner 与完整会话授权约束。正式行为和实现函数见[列表与搜索专题](../../product/vault/list-and-search.md#代码位置)。

已经移除列表删除确认死路径及 `VaultRoute.isEffectsPage` 无调用者开关。分类查询使用可恢复读取，空列表与读取失败分开表达；删除失败不投递给已退出页面，条目删除后清理其 OTP 状态。以上合同归专题页维护；活动计划只跟踪尚未完成的宿主验收。

## 验收证据

- 当前源码包含 `VaultViewModelTest` 的分类失败恢复、`sortPreferenceFailureShowsCurrentPageFeedbackWithoutChangingSelectedSort`、`deleteFailureCompletingAfterPageExitDoesNotEmitStalePageEffect`、`completedDeleteClearsRetainedOtp` 和 `retainOtpIsIdempotentAndGraphViewModelOwnsLeaseUntilCleared` 等用例；其他历史覆盖包括设置驱动排序、搜索/类型组合、删除单飞、当前页复制和锁定时拒绝复制。`TrashCommandUseCaseTest` 覆盖授权等待期间锁定后不查询或执行条目命令。`VaultEntryCommandWorkflow` 承载列表命令及页面 effect owner。
- 维护计划的历史记录报告：四个 Vault list 定向测试 28 项通过，多个分类、排序、删除及 OTP 用例分别通过；11 月 11 日 App JVM 测试 1188 项通过、AndroidTest 源码编译通过。上述是历史执行记录，本次只核对当前源文件，未重跑。
- 真实 Vault→Detail NavHost 转场、授权撤销期间命令和各 page owner 的迟到结果仍未由 Route/宿主行为测试证明；没有设备验收结论。

## 未闭合事项

[主维护计划 Task 32](../work/plans/2026-10-30-passly-maintainability-todo.md#task-32) 继续跟踪撤权与 owner 交接，以及 Vault→Detail/Trash 转场期间图级 collector 和 Paging/OTP 订阅行为。当前源代码与 JVM 测试不能代替真实导航宿主覆盖。

## Vault Paging 查询代际去重（历史修正）

2026-11-07 的维护记录发现，旧逻辑先按页面查询状态去重；当类型筛选已经启用时，层级偏好不改变有效 `EntryListQuery`，但会导致相同查询对应的 Pager 重启。当前实现先将筛选、排序与层级偏好映射为 `EntryListQuery`，仅在“全部”查询中应用层级模式，再按生成的查询去重。行为合同见[列表与搜索](../../product/vault/list-and-search.md)；生产入口为 [`buildRecoverableVaultQueryGenerations()` / `switchRecoverableQueryGenerations()`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/list/VaultQueryGeneration.kt)，当前测试源为 [`VaultQueryGenerationTest`](../../../app/src/test/java/com/aozijx/passly/feature/vault/presentation/list/VaultQueryGenerationTest.kt)。

原活动计划记录了修复时的红灯回归和 `VaultQueryStateTest` 8/8 结果；该测试名及旧 helper 名已不在当前源码中，本记录保留为历史执行证据，不代表本轮重跑。主维护计划 Task 84 的 VaultViewModel 调用闭包审计仍开放。
