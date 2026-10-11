# 条目修订历史

详情页的修订历史与访问活动是两个独立功能。活动记录描述查看、复制和自动填充等行为；修订历史保存可比较、
可恢复的条目版本。列表、比较和恢复都留在同一个 `RevisionHistorySheet` 中，不新增应用导航目的地。

## 状态与职责

`DetailViewModel` 负责启动/取消修订观察和比较、揭示、恢复用例；`DetailRevisionReducer` 只处理纯状态转换。
修订状态只有关闭与打开两种根分支。打开后固定绑定一个 `EntryId`，并分别持有修订列表状态和当前页：

- 列表为 `Loading / Ready / Failed`；
- 当前页为列表或绑定一个 revision id 的比较页；
- 比较为 `Loading / Ready / Failed`；
- 恢复确认与执行只存在于比较 `Ready` 分支，阶段为 `Idle / Confirming / Restoring`。

因此关闭状态不能残留 selection，加载失败不能同时携带成功差异，恢复也不可能在列表页或比较尚未加载时启动。
异步结果必须同时匹配当前 entry id 和 revision id；过期结果由 reducer 丢弃。

## 敏感数据

普通比较值来自 redacted revision contract。高敏字段只显示字段名，用户逐项授权后才通过 revision reveal
用例读取。揭示值只进入非 saveable 的内存 store；关闭 sheet、切换修订、切换条目、锁定或失去完整会话访问时
立即清空。恢复由 Data 层在单事务中校验版本和引用、消费精确 permit、写入新版本并记录活动；UI 不提前宣告成功。

## 失败与清理

取消不是错误。缺失、陈旧、损坏、认证失败和意外错误映射为功能级失败状态。关闭历史会取消观察和操作 job，
清除修订揭示值，并把状态恢复为 `Closed`。Presentation 使用穷尽映射生成 sheet model，不保留旧的
`visible + loading + restoring + failure` 布尔/nullable 组合。
