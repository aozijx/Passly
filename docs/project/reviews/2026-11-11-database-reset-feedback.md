# Database Reset 成功反馈计时器审查

## 结果

Database Reset 成功后显示 3 秒完成状态。连续或重新进入页面后再次重置时，每次成功操作递增 feedback generation；旧计时任务只有在 generation 仍匹配时才能将状态复位为 `Idle`，因此不会提前清除后一次重置的完成反馈。

## 验收

`DatabaseResetViewModelTest.olderSuccessTimerCannotClearNewerResetFeedback` 使用虚拟时间推进覆盖两个成功操作的计时交错：前一次 timer 到期时第二次成功反馈仍为 `Completed`，第二次 timer 到期后回到 `Idle`。同批维护记录确认 `DatabaseResetViewModelTest` 全类及 `verifyArchitecture` 通过；本次文档清理未重跑这些命令。

当前重置会话及页面生命周期合同见[数据库与重置](../../platform/data/database.md#初始化失败与数据重置)。

## 代码位置

| 职责 | 类/测试 | 源码文件 |
| --- | --- | --- |
| 成功反馈代次与过期计时任务抑制 | `DatabaseResetViewModel.reset()` | [`DatabaseResetViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/database/presentation/reset/DatabaseResetViewModel.kt) |
| 反馈状态转换 | `DatabaseResetReducer.reduce()` | [`DatabaseResetReducer.kt`](../../../app/src/main/java/com/aozijx/passly/feature/database/presentation/reset/DatabaseResetReducer.kt) |
| 前后两次成功重置的计时交错回归 | `olderSuccessTimerCannotClearNewerResetFeedback()` | [`DatabaseResetViewModelTest.kt`](../../../app/src/test/java/com/aozijx/passly/feature/database/presentation/reset/DatabaseResetViewModelTest.kt) |
