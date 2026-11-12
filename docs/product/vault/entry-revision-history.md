# 条目修订历史

状态：当前实现。

详情页使用单一历史 Sheet 呈现内容修订与活动审计记录。修订快照提供比较和恢复数据；活动记录仅用于审计展示。
Activity 与 Revision 保持各自持久化、观察和恢复 owner，只有 Detail Presentation 将两者投影到一条时间线。

## 状态和所有权

`DetailRevisionWorkflow` 启动或取消观察、比较、揭示和恢复；`DetailRevisionReducer` 只处理纯状态转换。
`DetailViewModel` 路由页面 action 并投影修订状态。根状态只有
Closed 和 Open。Open 固定绑定一个 Entry ID，并分别持有列表与当前页：

`DetailRevisionWorkflow` 由 Detail 页创建并与页面共用 `DetailWorkSession` token。它拥有历史观察代次、观察与操作
Job，以及独立的历史敏感值存储；条目切换、会话撤权、关闭或返回列表时取消适用任务并擦除揭示值。恢复提交后，
工作流发布修订状态并通知 `DetailViewModel`；ViewModel 校验当前 token 和条目，再刷新根详情投影及 OTP runtime。
因此历史操作与敏感资源有独立 owner，页面特有的根 snapshot 和 OTP 副作用仍由 Detail 页负责。

- 列表：Loading / Ready / Failed；
- 当前页：列表或绑定 revision ID 的比较页；
- 比较：Loading / Ready / Failed；
- 恢复阶段只存在于比较 Ready：Idle / Confirming / Restoring。

异步结果必须同时匹配当前 Entry ID 和 revision ID，过期结果由 reducer 丢弃。关闭状态不能残留 selection，
列表页或尚未加载的比较页不能发起恢复。
修订元数据观察在初始化成功后发生普通异常时进入列表 Failed，并提供重试；重试重新初始化当前条目历史再订阅。
每次观察使用递增代际；关闭后重开时，旧 collector 的迟到值或异常不能改写新观察状态。
协程取消继续传播，会话锁定由会话生命周期清理，致命 Error 不转换为页面错误。
恢复先同步从 Confirming 占用 Restoring，再启动协程；重复确认、重复请求及迟到的对话框取消均不能重启执行。
对话框取消与授权取消使用不同事件，授权取消才从 Restoring 回到 Idle，允许重新确认和重试。
恢复执行期间 Back 与整张弹层的关闭/下滑手势均不可用；Reducer 也拒绝关闭和返回列表事件，避免数据库事务已成功、提交后清理仍挂起时取消 ViewModel job，造成数据已恢复而当前详情未刷新。切换条目或关闭敏感会话仍会使旧结果失效，禁止回写新的或已清空的页面状态。
同一条目历史弹层已经打开时，重复的 `OpenRevisionHistory` action 保持当前状态，不重置比较/恢复阶段，也不取消正在执行的恢复任务。

## 首次打开时的历史初始化

`DetailViewModel` 将打开和重试 action 转给 `DetailRevisionWorkflow`。工作流从当前详情条目取得 `entryId` 和 `DetailWorkSession` token，发布 Opened 后启动 metadata 观察。观察任务通过 `ObserveEntryRevisionMetadataUseCase` 先调用无参 `EntryRevisionRepository.initializeHistory()`；只有初始化成功后，用例才返回 `observeMetadata(entryId)` 的 Flow 供工作流收集。列表失败后的重试会用同一条目的当前 token 重走初始化检查和 metadata 订阅。每次执行使用递增 observation generation；条目切换、关闭或重试导致旧任务失效时，不发布迟到结果。

用例只负责初始化、观察 metadata 并加载可读的脱敏快照；历史版本排序、当前快照排除、无变化版本筛选和相邻版本字段标注由 Domain `EntryRevisionHistoryPolicy` 统一决定。单个快照读取失败时用例省略该快照，Domain 策略保留对应历史版本但不伪造变更字段。

初始化能力本身是全库且按 generation 一次性执行，不接收当前页面的 `entryId`：`RoomEntryRevisionRepository.initializeHistory()` 委托给 `InitializeEntryRevisionHistory.execute()`，它在 mutex 内读取 `RevisionHistoryMaintenanceStore.completedGeneration()`。若版本低于 `CURRENT_GENERATION = 1`，便通过 `DatabaseTransactionRunner.write("entry_revision_history_initialize")` 清除旧 revision 并重建当前全库基线；`EntryRevisionHistoryRebuilder.rebuildInTransaction(db, now)` 用同一个 `now` 遍历 `entryQueryDao().getAll()`。该 DAO 查询不筛选 `deletedAt`，所以回收站条目也参与基线重建。对每条目读取 `STRUCT_BUNDLE`，调用 `EntryRevisionWriter.snapshotChanges(db, entryId, entryVersion, summary, secret, entryType, now)` 写入基线，并为缺失 bundle 使用空 `EntrySecret`。事务中还会安排附件 GC 和旧图标引用清理。事务成功后标记 generation 完成，再执行附件队列与待清理图标的 post-commit drain。故首次从单条目历史页进入会触发全库一次性维护；后续打开只检查 generation，再读取当前条目 metadata。页面仍拥有按条目的观察、取消、失败投影和重试；初始化的全库事务与资源维护归 Data/application 工作流审计范围。

附件 GC 在删除引用的数据库事务内把未引用资源标记为 `PENDING_GC`；提交后 collector 在完整 Vault 会话可用时执行文件删除并完成数据库记录清理。批次按 `(enqueuedAt, resourceId)` 稳定分页；单行失败会记录并推进游标，避免后续文件饥饿。整轮失败从 250 ms 起按指数退避，最高 30 秒；锁定会停止 drain，解锁或数据库状态恢复会重新唤醒。取消继续传播，fatal `Error` 不映射为普通重试。
## 快照与敏感数据

普通比较读取 redacted revision。高敏字段默认只显示字段名，逐项取得授权后才由 revision reveal 用例读取。
揭示值只存在于不可保存的内存 store；关闭 sheet、切换 revision/entry、锁定或失去完整会话时立即清空。

比较携带实际 EntryType，按 Domain 字段定义和同一个 `EntryFieldReader` 读取逻辑字段，不把整份 credential 或
自定义字段对象转成调试文本。Wi-Fi 密码、安全回答以及任一侧为隐藏类型的自定义字段，只输出变更类型，
不向普通差异模型传递值；高敏行继续使用不透明指纹判断变化，并沿用原授权揭示入口。
公开字段按名称展示之前/现在，自定义字段按位置和名称区分；布尔、日期、附件与关联条目数量在界面按当前语言格式化，
不展示内部文件路径或对象字符串。UI 映射位于 `DetailRevisionUiMapper`；历史列表和差异行共用
`DetailRevisionFieldLabelUiState.resolveText()` 解析资源标签，映射分支穷尽覆盖资源标签和命名自定义字段。

Revision snapshot 是不可变历史事实。恢复不是把旧 row 原地设为当前，而是在 Data 层单事务中：校验 Entry 和
revision、消费精确 permit、物化快照、写入新的当前版本并记录活动。UI 只有收到事务成功才显示完成。
恢复成功后的根详情 snapshot 刷新由 Detail 独立持有，不跟随版本弹层关闭而取消。普通读取失败会明确显示
“恢复已保存、详情刷新失败”，重试只重读当前详情，不会再次执行恢复事务；条目替换或会话关闭仍会取消/丢弃旧刷新。

恢复不得信任快照携带的外部权限或降低字段敏感度。缺失、陈旧、损坏、认证失败和意外错误映射为功能级失败；
用户取消不作为错误记录。

## 内容修订与活动记录

内容重写由 `EntryUpdateTransaction` 在调用方的 Room 事务中完成：检查乐观锁、比较实际提交内容、更新字段与能力、
写入同版本快照和一条 UPDATE 活动。Executor 负责会话事务边界与提交后的资源清理。相同 Profile 与 bundle、
保留未揭示高敏字段、清空不存在的高敏行均不会新增修订；显式替换高敏值仍视为重写，不为比较而解密原值。
创建只记录 CREATE；关联关系实际变化也更新快照与活动，相同关系重复提交不新增修订。关联快照不解密高敏行。
关系 DAO 在事务中按主键更新、未命中才严格插入；端点唯一键冲突必须失败，不能将 0 行更新当作保存成功并制造修订。

EntryVersion 与修订存储继续从 1 开始，保证乐观锁、活动与归档版本合同不变；历史卡片将创建时的版本 1 显示为版本 0。
历史列表不显示最高版本的当前快照；该快照与当前条目内容一致，没有可供比较的变化。若较旧快照也与当前内容一致，同样不进入列表。其余历史元数据使用脱敏快照比较相邻修订，完全相同的较新快照不进入列表；无法读取任一快照时保留该版本，避免读取失败导致历史记录被隐藏。高敏字段只比较现有的不透明指纹，不揭示明文。比较页若得到空差异，显示无变更提示并禁用恢复操作。

历史恢复记录 VERSION_RESTORED，回收站恢复记录 RESTORE，软删除记录 DELETE。后两者改变生命周期与乐观锁版本，
不制造内容快照。内容活动使用 `EntryActivityContext.ContentRevision` 关联实际新版本，访问与生命周期活动分别使用
Usage / Lifecycle。活动只含类型、版本、条目标识与时间，不含字段值或任意来源文本。Room 行保存活动类型、
条目标识、时间和可选的 `revisionVersion`，Room schema 保持 1，不兼容旧开发数据。
活动不以外键绑定可被裁剪的修订行。

使用记录统一由 `EntryActivityWriter` 写入，拒绝通过 usage 入口写入内容事件；活动按时间及活动 ID 稳定倒序读取。
修订最多保留每条 50、全库 1000 份；活动不随修订裁剪删除，使用频次与最后使用时间继续基于已存使用记录统计。
界面直接使用 Domain 活动类型，按当前语言格式化日期和关联版本，不复制一份同名枚举。
详情页在 entry 成功打开后才开始活动观察；加载状态明确区分 Loading、Ready 与 Failed。普通查询异常可在当前
entry visit 内重订阅，失败提示提供重试；若此前已有记录，则失败期间仍显示已取得的记录。正常空结果才显示空历史。
会话锁定不显示为普通查询错误，取消继续传播，致命 Error 保持未捕获。

## 详情快照加载与恢复后刷新

详情主体快照与修订列表分别管理加载状态。`DetailSnapshotState` 区分首次加载、已加载、条目不存在、加载失败、恢复后刷新中及刷新失败。
首次加载失败时 Route 提供重试；条目不存在时返回详情入口。修订历史的重试只重新初始化并订阅修订元数据，不代替详情快照重试。恢复后的快照刷新语义见上文。

## 历史弹层布局

详情页只有一个历史入口。Sheet 在列表页提供全部、内容变更、生命周期和使用记录筛选；分类穷尽映射 `ActivityType` 的领域语义，操作图标区分创建、编辑、版本恢复、删除、回收站恢复、查看、揭示、复制、自动填充、导入和导出。活动描述、字段标签和日期仍提供文字语义，图标不是唯一信息。

Activity 的 `ContentRevision.version` 只与同版本且仍保留的 revision metadata 关联。匹配时合并成一行并沿用活动操作图标；没有活动匹配的保留快照单独显示。活动记录可以比快照保留得更久，因此对应快照已裁剪时仍保留活动行，但不显示比较/恢复入口。当前内容快照不作为历史项；只有 `DetailHistoryRevisionReference.Linked` 才能打开现有比较与恢复流程。Activity 日志不重放成删除撤销或回收站恢复等生命周期操作。

活动和修订来源的加载、失败与重试分别呈现；某一来源失败时保留另一来源的条目。仅在两个来源都 Ready 且筛选后的项目为空时显示空结果。弹层只在 Hidden / Expanded 间切换，不预留屏幕百分比高度；空列表、短列表和无差异比较按内容收缩，长列表使用受剩余高度约束的 LazyColumn，比较页的恢复按钮位于滚动区域之外，由 M3 处理系统栏留白。
普通加密 bundle 与隐藏自定义字段只提示已变更；高敏字段只有在历史版本中确实存在时才提供揭示操作。
恢复确认或执行期间禁止启动揭示，开始新的揭示前取消旧操作，过期结果仍由访问会话擦除。

## 清理

关闭历史会取消观察和操作 job、擦除所有揭示值，并归位 Closed。Presentation 使用穷尽映射生成 sheet model，
不保留 `visible + loading + restoring + failure` 的布尔/nullable 组合。

历史快照和 Backup 是不同边界：Backup 默认不导出修订历史；修订清理策略也不得借 Backup 导入绕过。

相关决策见 [ADR-0015](../../project/decisions/ADR-0015-history-snapshot-strategy.md)。

## 代码位置

| 职责 | 类/函数 | 源码文件 |
| --- | --- | --- |
| 详情页历史操作任务与敏感 reveal 生命周期 | `DetailRevisionWorkflow`、`DetailRevisionRevealStore` | [`DetailRevisionWorkflow.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/history/DetailRevisionWorkflow.kt)、[`DetailRevisionRevealStore.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/history/DetailRevisionRevealStore.kt) |
| 详情页历史 action 路由、状态投影与恢复后根 snapshot/OTP 刷新 | `DetailViewModel`、`DetailRevisionReducer` | [`DetailViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/DetailViewModel.kt)、[`DetailRevisionReducer.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/history/DetailRevisionReducer.kt) |
| 首次查看时检查并初始化全库历史 generation，再订阅当前条目 metadata | `ObserveEntryRevisionMetadataUseCase`、`initializeHistory()`、`InitializeEntryRevisionHistory.execute()` | [`ObserveEntryRevisionMetadataUseCase.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/history/ObserveEntryRevisionMetadataUseCase.kt)、[`EntryRevisionRepository.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/port/EntryRevisionRepository.kt)、[`RoomEntryRevisionRepository.kt`](../../../data/src/main/java/com/aozijx/passly/data/repository/entry/RoomEntryRevisionRepository.kt)、[`InitializeEntryRevisionHistory.kt`](../../../data/src/main/java/com/aozijx/passly/data/repository/entry/command/InitializeEntryRevisionHistory.kt) |
| 对脱敏快照应用历史可见性规则并标注相邻版本的变更字段 | `EntryRevisionHistoryPolicy.summarize()` | [`EntryRevisionHistoryPolicy.kt`](../../../domain/src/main/kotlin/com/aozijx/passly/domain/entry/policy/EntryRevisionHistoryPolicy.kt) |
| 清旧修订并为全库当前条目写入基线 | `rebuildInTransaction(db, now)`、`snapshotChanges(db, entryId, entryVersion, summary, secret, entryType, now)` | [`EntryRevisionHistoryRebuilder.kt`](../../../data/src/main/java/com/aozijx/passly/data/repository/entry/command/EntryRevisionHistoryRebuilder.kt)、[`EntryRevisionWriter.kt`](../../../data/src/main/java/com/aozijx/passly/data/repository/entry/command/EntryRevisionWriter.kt) |
| Detail 根 snapshot 加载与恢复后刷新 | `DetailSnapshotState`、`DetailPresentationLoader`、`retryDetailLoad()`、`retrySnapshotRefresh()` | [`DetailUiState.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/DetailUiState.kt)、[`DetailPresentationLoader.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/DetailPresentationLoader.kt)、[`DetailViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/DetailViewModel.kt)、[`DetailRoute.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/DetailRoute.kt) |
| 活动记录映射与 Activity/Revision 时间线合并 | `EntryActivity.toDetailHistoryItemUiModel()`、`toHistorySheetUiModel()` | [`DetailActivityUiMapper.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/DetailActivityUiMapper.kt)、[`DetailRevisionUiMapper.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/history/DetailRevisionUiMapper.kt) |
| 唯一历史入口与 Sheet 的分类筛选、图标和恢复交互 | `DetailHistorySection()`、`DetailHistorySheet()` | [`DetailHistorySection.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/ui/component/DetailHistorySection.kt)、[`DetailHistorySheet.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/ui/component/DetailHistorySheet.kt) |
| 活动历史状态、筛选状态与重试 action owner | `DetailActivityHistoryState`、`DetailHistoryFilter` | [`DetailUiState.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/DetailUiState.kt)、[`DetailViewModel.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/DetailViewModel.kt) |
| 比较与恢复历史版本 | `CompareEntryRevisionUseCase`、`RestoreEntryRevisionUseCase` | [`CompareEntryRevisionUseCase.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/history/CompareEntryRevisionUseCase.kt)、[`RestoreEntryRevisionUseCase.kt`](../../../app/src/main/java/com/aozijx/passly/feature/vault/history/RestoreEntryRevisionUseCase.kt) |
| 在事务中写入版本并恢复修订 | `EntryRevisionWriter`、`EntryRevisionRestoreTransaction` | [`EntryRevisionWriter.kt`](../../../data/src/main/java/com/aozijx/passly/data/repository/entry/command/EntryRevisionWriter.kt)、[`EntryRevisionRestoreTransaction.kt`](../../../data/src/main/java/com/aozijx/passly/data/repository/entry/EntryRevisionRestoreTransaction.kt) |
| 写入条目活动记录 | `EntryActivityWriter` | [`EntryActivityWriter.kt`](../../../data/src/main/java/com/aozijx/passly/data/repository/entry/command/EntryActivityWriter.kt) |
| 安排及执行附件资源回收 | `AttachmentResourceGarbageCollector.scheduleInTransaction()`、`drain()`、`runAttachmentGcDrain()` | [`AttachmentResourceGarbageCollector.kt`](../../../data/src/main/java/com/aozijx/passly/data/repository/attachment/AttachmentResourceGarbageCollector.kt)、[`AttachmentGarbageCollectionWorker.kt`](../../../data/src/main/java/com/aozijx/passly/data/repository/attachment/AttachmentGarbageCollectionWorker.kt) |
