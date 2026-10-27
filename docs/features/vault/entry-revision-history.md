# 条目修订历史

状态：当前实现。

详情页的修订历史与访问活动是两个独立功能。修订历史保存可比较、可恢复的条目版本；列表、比较和恢复留在
同一个 `RevisionHistorySheet`，不新增导航目的地。

## 状态和所有权

`DetailViewModel` 启动或取消观察、比较、揭示和恢复；`DetailRevisionReducer` 只处理纯状态转换。根状态只有
Closed 和 Open。Open 固定绑定一个 Entry ID，并分别持有列表与当前页：

- 列表：Loading / Ready / Failed；
- 当前页：列表或绑定 revision ID 的比较页；
- 比较：Loading / Ready / Failed；
- 恢复阶段只存在于比较 Ready：Idle / Confirming / Restoring。

异步结果必须同时匹配当前 Entry ID 和 revision ID，过期结果由 reducer 丢弃。关闭状态不能残留 selection，
列表页或尚未加载的比较页不能发起恢复。

## 快照与敏感数据

普通比较读取 redacted revision。高敏字段默认只显示字段名，逐项取得授权后才由 revision reveal 用例读取。
揭示值只存在于不可保存的内存 store；关闭 sheet、切换 revision/entry、锁定或失去完整会话时立即清空。

比较携带实际 EntryType，按 Domain 字段定义和同一个 `EntryFieldReader` 读取逻辑字段，不把整份 credential 或
自定义字段对象转成调试文本。Wi-Fi 密码、安全回答以及任一侧为隐藏类型的自定义字段，只输出变更类型，
不向普通差异模型传递值；高敏行继续使用不透明指纹判断变化，并沿用原授权揭示入口。
公开字段按名称展示之前/现在，自定义字段按位置和名称区分；布尔、日期、附件与关联条目数量在界面按当前语言格式化，
不展示内部文件路径或对象字符串。UI 映射与差异行渲染分别位于 `DetailRevisionUiMapper` 和 `RevisionDifferenceRow`，
共享字段名称使用穷尽资源映射。

Revision snapshot 是不可变历史事实。恢复不是把旧 row 原地设为当前，而是在 Data 层单事务中：校验 Entry 和
revision、消费精确 permit、物化快照、写入新的当前版本并记录活动。UI 只有收到事务成功才显示完成。

恢复不得信任快照携带的外部权限或降低字段敏感度。缺失、陈旧、损坏、认证失败和意外错误映射为功能级失败；
用户取消不作为错误记录。

## 清理

关闭历史会取消观察和操作 job、擦除所有揭示值，并归位 Closed。Presentation 使用穷尽映射生成 sheet model，
不保留 `visible + loading + restoring + failure` 的布尔/nullable 组合。

历史快照和 Backup 是不同边界：Backup 默认不导出修订历史；修订清理策略也不得借 Backup 导入绕过。

相关决策见 [ADR-0015](../../decisions/ADR-0015-history-snapshot-strategy.md)。
