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
恢复先同步从 Confirming 占用 Restoring，再启动协程；重复确认、重复请求及迟到的对话框取消均不能重启执行。
对话框取消与授权取消使用不同事件，授权取消才从 Restoring 回到 Idle，允许重新确认和重试。
恢复执行期间 Back 与整张弹层的关闭/下滑手势均不可用；Reducer 也拒绝关闭和返回列表事件，避免数据库事务已成功、提交后清理仍挂起时取消 ViewModel job，造成数据已恢复而当前详情未刷新。切换条目或关闭敏感会话仍会使旧结果失效，禁止回写新的或已清空的页面状态。

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

## 内容修订与活动记录

内容重写由 `EntryUpdateTransaction` 在调用方的 Room 事务中完成：检查乐观锁、比较实际提交内容、更新字段与能力、
写入同版本快照和一条 UPDATE 活动。Executor 负责会话事务边界与提交后的资源清理。相同 Profile 与 bundle、
保留未揭示高敏字段、清空不存在的高敏行均不会新增修订；显式替换高敏值仍视为重写，不为比较而解密原值。
创建只记录 CREATE；关联关系实际变化也更新快照与活动，相同关系重复提交不新增修订。关联快照不解密高敏行。
关系 DAO 在事务中按主键更新、未命中才严格插入；端点唯一键冲突必须失败，不能将 0 行更新当作保存成功并制造修订。

历史恢复记录 VERSION_RESTORED，回收站恢复记录 RESTORE，软删除记录 DELETE。后两者改变生命周期与乐观锁版本，
不制造内容快照。内容活动使用 `EntryActivityContext.ContentRevision` 关联实际新版本，访问与生命周期活动分别使用
Usage / Lifecycle。活动只含类型、版本、条目标识与时间，不含字段值或任意来源文本；存储删除未使用的 source，
以 revisionVersion 表达可选修订关联，Room schema 保持 1，不兼容旧开发数据。活动不以外键绑定可被裁剪的修订行。

使用记录统一由 `EntryActivityWriter` 写入，拒绝通过 usage 入口写入内容事件；活动按时间及活动 ID 稳定倒序读取。
修订最多保留每条 50、全库 1000 份；活动不随修订裁剪删除，使用频次与最后使用时间继续基于已存使用记录统计。
界面直接使用 Domain 活动类型，按当前语言格式化日期和关联版本，不复制一份同名枚举。

## 历史弹层布局

弹层只在 Hidden / Expanded 间切换，不预留屏幕百分比高度。空列表、短列表和无差异比较按内容收缩；
长列表使用受剩余高度约束的 LazyColumn，比较页的恢复按钮位于滚动区域之外，由 M3 处理系统栏留白。
普通加密 bundle 与隐藏自定义字段只提示已变更；高敏字段只有在历史版本中确实存在时才提供揭示操作。
恢复确认或执行期间禁止启动揭示，开始新的揭示前取消旧操作，过期结果仍由访问会话擦除。

## 清理

关闭历史会取消观察和操作 job、擦除所有揭示值，并归位 Closed。Presentation 使用穷尽映射生成 sheet model，
不保留 `visible + loading + restoring + failure` 的布尔/nullable 组合。

历史快照和 Backup 是不同边界：Backup 默认不导出修订历史；修订清理策略也不得借 Backup 导入绕过。

相关决策见 [ADR-0015](../../decisions/ADR-0015-history-snapshot-strategy.md)。

## 代码位置

| 职责 | 类/函数 | 源码文件 |
| --- | --- | --- |
| 详情页历史状态和交互编排 | `DetailViewModel`、`DetailRevisionReducer` | [`DetailViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/DetailViewModel.kt)、[`DetailRevisionReducer.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/history/DetailRevisionReducer.kt) |
| 比较与恢复历史版本 | `CompareEntryRevisionUseCase`、`RestoreEntryRevisionUseCase` | [`CompareEntryRevisionUseCase.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/history/CompareEntryRevisionUseCase.kt)、[`RestoreEntryRevisionUseCase.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/history/RestoreEntryRevisionUseCase.kt) |
| 在事务中写入版本并恢复修订 | `EntryRevisionWriter`、`EntryRevisionRestoreTransaction` | [`EntryRevisionWriter.kt`](../../../data/src/main/java/com/aozijx/passly/data/repository/entry/command/EntryRevisionWriter.kt)、[`EntryRevisionRestoreTransaction.kt`](../../../data/src/main/java/com/aozijx/passly/data/repository/entry/EntryRevisionRestoreTransaction.kt) |
| 写入条目活动记录 | `EntryActivityWriter` | [`EntryActivityWriter.kt`](../../../data/src/main/java/com/aozijx/passly/data/repository/entry/command/EntryActivityWriter.kt) |
