# 数据库

状态：当前实现（Room Schema v1）。本文描述当前代码与全新安装，不承诺旧开发 Schema 的迁移。
Room Schema 保持版本 1，当前没有旧开发库迁移；使用旧结构的本地开发数据库前，需清除应用数据或重新安装，再验证当前 Schema。

## 所有权与访问边界

`:data` 拥有 Room Entity、DAO、converter、Schema、SQLCipher provider、数据库 session adapter，以及
Entry、附件、历史等仓储实现。`:data` 不依赖 App 或 Feature。

```mermaid
flowchart TB
    Repository["Repository / command executor"] --> Session["AppDatabaseSession"]
    Session --> Provider["DatabaseProvider"]
    Provider --> Room["Room AppDatabase"]
    Room --> SQLCipher["SQLCipher passphrase"]
    Repository --> Mapper["Entity / payload / Domain mapper"]
    Repository --> DAO
    DAO --> Room
```

`DatabaseProvider` 使用 `SessionKeySource` 提供的会话密钥打开加密数据库；`AppDatabaseSession` 管理
租约、延迟打开和关闭。Repository 必须在 session adapter 提供的作用域内使用 DAO，不能长期缓存
Room 实例或越过锁定边界持有 DAO。

### 查询与事务的调用链

写命令通常从 `DatabaseTransactionRunner.write(operation, block)` 进入。它将
`sessionState::hasFullSecureSessionAccess` 与 `operation` 一并交给 `writeWithAccess(...)`；Autofill 专用命令
调用 `writeForAutofill(operation, block)`，门控谓词改为 `hasAutofillSessionAccess()`。Runner 通过
`AppResult.runSuspendCatching { ... }` 映射普通 `Exception` 为 `AppResult.Failure`，重抛
`CancellationException`；随后调用 `databaseSession.transaction { block() }`。失败结果再以相同的 `operation`
传给 `errorReporter.report(error, ErrorReportContext(operation = OperationCode(operation), category = DATABASE))`。

`AppDatabaseSession.transaction(block)` 将接收者闭包传给 `RuntimeSessionManager.write(block)`；Runtime 经
`SessionLeaseGate.write(block)` 调用 `acquireLease()`，在 `leaseMutex` 内确认状态为 `UNLOCKED` 并登记当前 coroutine
Job 和 active lease 数，再由 `resolveHandle()` 取得当前 Room handle。事务调用
`AppDatabaseSessionResource.transaction(handle, block)`，最终执行
`handle.withTransaction { handle.block() }`。读取对应地经过 `AppDatabaseSession.query(block)` →
`RuntimeSessionManager.read(block)` → `SessionLeaseGate.read(block)`，同样先取 lease/handle，但直接运行 `handle.block()`，
不包 Room 写事务。两条路径都在 `finally` 调用 `releaseLease(owner)`；DAO 和 Room 接收者不得逃出闭包。

授权检查发生在 `DatabaseTransactionRunner` 进入 session API 之前，session lease 检查则发生在获取 lease 时；二者目前
不是一个原子操作。所有直接调用 `AppDatabaseSession.query/transaction` 的 Repository 也不经过 Runner。认证撤权与排队
数据库操作的统一线性化合同及需审查的旁路见[维护 TODO Task 37](../superpowers/plans/2026-10-30-passly-maintainability-todo.md#task-37为认证撤权与数据库操作定义同一线性化边界)。

备份协议与交互属于 `app/feature/backup`；Feature 只能通过明确的快照适配边界接触数据库数据，不能把
Room Entity 当作可移植协议。

## 当前 Schema

`AppDatabase` 当前只注册以下八张表：

| 表 | 用途 |
|---|---|
| `entries` | Entry 身份、类型、能力位、OTP 类型、可查询的 Profile 元数据和时间戳 |
| `entry_secret_fields` | `entryId + fieldKey` 定位的密文行；高敏字段逐字段保存，低敏结构聚合在 `STRUCT_BUNDLE` |
| `entry_links` | Entry 之间的类型化关系；任一端点删除时级联删除 |
| `entry_revisions` | Entry Profile 与敏感字段集合的完整历史快照密文 |
| `entry_activities` | 查看、复制、使用等审计/统计事件，不用于恢复 |
| `attachment_resources` | keyed content ID 标识的不可变加密附件资源及其 GC 生命周期状态 |
| `attachment_refs` | 当前 Entry 对资源的引用、文件名、MIME 和顺序；每行都必须归属现存 Entry |
| `revision_attachment_refs` | 历史 Revision 对不可变附件资源的引用 |

Schema 的唯一事实源是 `AppDatabase`、Entity 和导出的 `data/schemas`。当前没有 `entry_drafts`、
`attachment_gc_queue` 或 Autofill Inbox 导入日志表。Inbox 文件队列独立于 SQLCipher；导入幂等性由 draft ID
派生的稳定 Entry ID 实现，不新增 Room 表。

## Entry 元数据与查询

`entries` 使用结构化列保存 Profile 元数据：`title`、`username`、`primaryUrl`、`domains`、
`applicationIds`、图标字段、`favorite`、`tags`、过期时间和创建/更新时间。当前表中没有
`summaryBlob` 或 `indexVersion`。

- `entryType` 是结构类型，不是用户分类。
- `capabilityFlags` 表示条目实际具有的能力，不能替代 `entryType` 或 `entry_links`。
- 当前没有 `vaultId` 或 `parentEntryId`；关联凭据保持原子 Entry，通过 `entry_links` 表达关系。
- 列表和搜索直接查询 `entries` 的结构化列及 JSON 集合。动态查询始终使用参数绑定；文本搜索当前使用
  SQLite `LIKE`，不会扫描高敏字段密文。
- Profile 由 `EntryProfileMapper.fromEntity()` 直接映射，不需要先解密摘要 Blob。

数据库列、密文字段键和 Revision 快照中的枚举值通过 `StorageEnumMappings` 显式映射为稳定键。
持久化协议不得依赖 ordinal；新增领域枚举时必须补齐双向映射与 round-trip 测试。Room converter
负责 SQL 边界的强类型转换，未知持久化值必须明确失败，不能静默替换成合法默认值。

## Secret 读取边界

高敏字段按 `entryId + fieldKey` 分行 AES-GCM 加密，AAD 绑定对应记录身份。`PASSWORD`、银行卡号、
CVV、支付 PIN、身份号码、助记词、恢复码、SSH 私钥/口令、Passkey 私钥引用和 OTP Secret 都不进入
`entries` Profile 列。

`STRUCT_BUNDLE` 保存低敏凭据结构、notes 和 custom fields，并把逐字段高敏值置为 `null`。因此：

- 普通详情读取 `entries` Profile 与 `STRUCT_BUNDLE`，不物化高敏字段；
- 单字段 reveal 只查询并解密对应 `fieldKey`；
- 完整凭据快照仅由 Backup 的 `RoomBackupSnapshotReader` 经 Data 内部 `SecretFieldStore.readAll()` 合并全部字段；
- 普通更新用 `Preserve / Clear / Replace` 区分未加载、主动清空和替换，避免为了写低敏结构而解密全部
  高敏字段。

`SummaryPayload` 仍用于 Revision 的 Profile 快照编码，不是 `entries` 当前存储格式。

## 历史与附件生命周期

- Revision 使用完整快照而非 diff。Profile 快照和高敏字段集合分别加密；附件正文不复制进 Revision，
  只通过 `revision_attachment_refs` 引用不可变资源。
- 单 Entry 最多保留 50 条 Revision，全库最多 1000 条。永久删除和清空回收站在数据库事务中同步
  删除关联历史。
- 附件正文位于 `filesDir/attachments/content/<resourceId>.enc`。`resourceId` 是 keyed content ID，
  文件名、可空 MIME 和显示顺序属于引用行。
- 当当前引用和历史引用都为 0 时，资源行改为 `PENDING_GC`；`enqueuedAt`、`attemptCount` 和
  `lastAttemptAt` 就是持久重试状态。文件删除成功后再删除资源行。没有单独 GC 队列表。
- 条目写事务只负责把无引用附件标为 `PENDING_GC`。提交后附件清理与图标路径回收分开执行；普通清理异常
  记录结构化日志，不把已提交的数据库结果改成失败。附件 GC 监听完整 Vault 会话恢复并重试持久的
  `PENDING_GC` 行；每页最多 64 条，按 `(enqueuedAt, resourceId)` 稳定翻页，同一次授权解锁会排空全部页面。
  文件失败先记录 attempt 并继续处理后续资源；一轮仍有待重试项或数据库抛普通 `Exception` 时，worker 在授权持续有效期间
  以 250 ms 起步、倍增到最多 30 s 的退避间隔重试。会话封存或取消会停止当前 drain；后续完整 Vault 会话恢复从持久队列继续。
  Fatal `Error` 不映射为普通清理失败，进程中断也不丢失附件待办。
- 若调用方在数据库提交后、清理完成前取消，取消仍向上传播且不会回滚已提交数据。重新进入页面时以 Room
  查询结果为准，不应仅凭取消结果盲目重放写入；附件 GC 可在会话重开后继续，图标回收保留队列请求并由图片处理器重建时的候选扫描补齐中断窗口。
- 外键和删除前引用计数阻止仍被引用的内容被回收。附件不做通用压缩。
- 每条附件引用都绑定现存 Entry 与 `ACTIVE` 资源。Backup 导入和修订恢复在对应写事务中创建引用；当前没有附件编辑/上传流程需要数据库保存草稿 owner。

## Session 生命周期

```mermaid
stateDiagram-v2
    [*] --> SEALED
    SEALED --> UNLOCKED: unlock / resource.open 成功
    UNLOCKED --> SOFT_LOCKED: softLock
    SOFT_LOCKED --> UNLOCKED: resumeIfOpen
    UNLOCKED --> SEALED: seal
    SOFT_LOCKED --> SEALED: seal
    SEALED --> SEALED: close 失败，保留失败句柄并拒绝新 lease
```

- 错误密钥或 Schema 不匹配必须返回明确错误，禁止自动删库。
- 锁定先把 session gate 转为 `SEALED`，拒绝新 lease，再等待 active lease 排空并关闭数据库，最后由 Security owner 擦除 DEK 与会话密钥。及时排空时，关闭发生在 active lease 归零之后。
- `seal(timeout = 5.seconds)` 排空超时时发出 `DRAIN_TIMEOUT`，并取消登记的 lease owner Job；这只是取消请求，不证明 Room transaction/read 或 native 操作已经退出。Runtime 在不可取消区继续等到 active lease 归零后才调用资源 `close()`；等待期间 session 保持 `SEALED` 并拒绝新 lease。没有资源 abort 合同，因此不会强制关闭仍在使用的句柄；open/resume/seal 生命周期转换的整体并发 owner 仍见[维护 TODO Task 26](../superpowers/plans/2026-10-30-passly-maintainability-todo.md#task-26明确-runtime-session-生命周期转换的并发所有者)。
- `SOFT_LOCKED` 会拒绝新 lease，但当前 `softLock()` 不等待或取消已经取得的 lease；在途 read/write 按其实际完成或取消结果结束。这一语义及锁定返回时机仍由维护 TODO Task 26 追踪。
- 覆盖导入、Revision 恢复和多表写入必须在事务中完成，失败整体回滚。
- 当前 Room Schema、Secret Payload 和 Revision 格式均为版本 `1`；备份协议版本独立。
关闭失败时保持 `SEALED` 并保留失败句柄；没有确认关闭成功前，不删除数据库文件或建立第二个 session。失败结果、撤权和重置边界见本页的会话关闭与重置合同。

Sealed Autofill Inbox 使用独立耐久文件队列，完整解锁后才写入 SQLCipher Entry。Room Schema 保持版本 1；
队列清理与幂等导入边界见 [Sealed Inbox](../features/autofill/sealed-inbox.md)。

## 初始化失败与数据重置

数据库初始化失败页只提供重试和关闭应用，不在故障路径提供清库。
`AppShellViewModel` 在协程调度前占用独立重试任务，连续点击只发起一次恢复；故障流发布新错误时仍保留在途状态。
任务在 `finally` 中释放重试状态，取消不产生失败提示。数据库可用性始终由故障流决定，不能仅凭一次 Ready 返回值
自行清除故障；故障流已经恢复或作用域销毁后，旧任务的迟到错误不得再次提示或改写界面。

“设置 → 数据管理”的永久重置必须经过确认和新鲜认证。它删除数据库及 WAL/SHM/journal、附件和
自定义图片，然后使用现有 Bootstrap DEK 创建空数据库；普通偏好、认证信封和密钥绑定仍由各自的
存储生命周期管理。

该操作不删除完整 Vault：Bootstrap DataStore、Autofill Inbox 密钥材料、pending/quarantine 文件队列都会保留。
因此数据库重置后，待导入 Inbox 草稿仍可在完整解锁后继续处理；Inbox 自身的过期和容量清理规则不变。

进入数据库封存后，重置会在不可取消区完成封存、删除数据库与托管资源并重新打开数据库，然后才向调用方传播取消。
尚未开始封存时收到的取消会阻止重置启动；执行期间的取消不能留下“已封存但未清理”的半完成状态。

如果底层数据库句柄关闭失败，Runtime 仍立即拒绝所有新访问，并在普通可恢复异常时通过 Kotlin `Result<Unit>` 失败结果返回原因；取消与 fatal `Error` 继续传播。Runtime 尽力发出 `CLOSE_FAILED`，Data logger 将普通关闭故障记录为 `DatabaseResourceCloseFailed`。该进程中的 session 保持 `SEALED`：保留失败句柄作为未关闭的资源事实，不重试关闭、不创建第二个 Room/SQLCipher 句柄；只有进程重启才能建立新 session。永久重置把成功关闭视为删库和删除托管资源的前置条件，因此关闭失败时跳过所有删除且不尝试重开，将错误返回给调用方。Security 协调器随后撤销授权、清除 DEK/敏感数据密钥并保持认证锁定。

普通 lease drain 超时会发出 `DRAIN_TIMEOUT` 取消请求事件并取消当前 lease owner；它不表示操作已经退出。Runtime 从此拒绝新访问，但继续持有 Room 句柄并在不可取消区等待活动 lease 归零，随后才关闭数据库。Security 锁定期间认证状态保持 `Locking`；lease 退出且关闭成功后再擦除 DEK 并发布 `Locked`。若操作一直不退出，锁定继续等待，不强制关闭正在使用的 Room 句柄。句柄关闭失败则按上段保持封存，进程重启后恢复。

数据库重置与认证锁定、解锁共用 Security 会话互斥。重置期间不报告完整 Vault 访问；重开成功后恢复会话，
重开失败则撤销授权、清除敏感数据密钥并转入锁定状态。活动查询流同时要求认证授权和数据库会话为 `UNLOCKED`；
Room 长期观察同时依赖认证授权和数据库 `UNLOCKED` 状态。认证撤权会立即切换到空结果并取消当前查询；数据库
`SOFT_LOCKED`/`SEALED` 也会取消旧 collector、释放 session lease、发出空结果，并在重新解锁后订阅新 Room Flow。
如果锁定恰好与 DAO emission 交错，`SessionLeaseGate` 的 `SessionLockedException` 只转换成空结果，不能终止外层可读状态观察；
SQLite/磁盘等其他查询异常仍原样传播，不能吞成空数据。Vault Paging 直接随数据库 session state 重建 Pager，不再从重置控制器另发列表刷新事件。
Session owner 同时发布单调递增的锁代次；观察 gate 把代次纳入可读状态，使快速 lock/reopen 即使被 `StateFlow` 合并，重开的状态仍会建立新的 Room 查询。

Database Reset Sheet 由其可见 Route owner 注册/退出。关闭 Sheet 或离开 Data Management 时，终态回到 `Idle`，
因此重新打开后可再次提交；如果退出时状态为 `Resetting`，ViewModel scope 继续完成已确认的重置，不因 dismiss/锁定收尾而取消。仍在执行时重新进入会继续显示进行中；完成后再次打开则从新一轮 `Idle` 开始。

## 代码位置

| 职责 | 类/函数 | 源码文件 |
| --- | --- | --- |
| Room schema 与数据库定义 | `AppDatabase` | [`AppDatabase.kt`](../../data/src/main/java/com/aozijx/passly/data/local/database/AppDatabase.kt) |
| 数据库依赖提供 | `DatabaseProvider` | [`DatabaseProvider.kt`](../../data/src/main/java/com/aozijx/passly/data/local/database/DatabaseProvider.kt) |
| 数据库命令读写入口与错误映射 | `DatabaseTransactionRunner.read(operation, block)`、`write(operation, block)`、`writeForAutofill(operation, block)` | [`DatabaseTransactionRunner.kt`](../../data/src/main/java/com/aozijx/passly/data/local/database/DatabaseTransactionRunner.kt) |
| Room 类型的 session API | `AppDatabaseSession.query(block)`、`transaction(block)`、`observeFlow(block)`、`unlock()`、`softLock()`、`seal()` | [`AppDatabaseSession.kt`](../../data/src/main/java/com/aozijx/passly/data/local/database/session/AppDatabaseSession.kt) |
| 通用 session lease / lock 状态机 | `RuntimeSessionManager.read(block)`、`write(block)`、`observe(block)`、`unlock()`、`softLock()`、`seal(timeout)`；`SessionLeaseGate.acquireLease()`、`releaseLease()`、`closeAfterDrainTimeout()`、`close()` | [`RuntimeSessionManager.kt`](../../runtime/session/src/main/kotlin/com/aozijx/passly/runtime/session/RuntimeSessionManager.kt)、[`SessionLeaseGate.kt`](../../runtime/session/src/main/kotlin/com/aozijx/passly/runtime/session/SessionLeaseGate.kt) |
| Room open、close 与事务适配 | `AppDatabaseSessionResource.open(key)`、`close(handle)`、`transaction(handle, block)` | [`AppDatabaseSessionResource.kt`](../../data/src/main/java/com/aozijx/passly/data/local/database/session/AppDatabaseSessionResource.kt) |
| Vault 数据库重置与可读观察 gate | `VaultDatabaseResetController.reset()`、`observeWhenReadable()` | [`VaultDatabaseResetController.kt`](../../data/src/main/java/com/aozijx/passly/data/local/database/port/VaultDatabaseResetController.kt)、[`AppDatabaseSessionObservations.kt`](../../data/src/main/java/com/aozijx/passly/data/local/database/session/AppDatabaseSessionObservations.kt) |
| 秘密字段替换、读取与 OTP 配置读取 | `SecretFieldStore.replaceAll()`、`readBundle()`、`readField()`、`readOtpConfig()` | [`SecretFieldStore.kt`](../../data/src/main/java/com/aozijx/passly/data/repository/entry/SecretFieldStore.kt) |
| 查找元数据策略决策 | [ADR-0018](../decisions/ADR-0018-lookup-metadata-strategy.md) | [`ADR-0018-lookup-metadata-strategy.md`](../decisions/ADR-0018-lookup-metadata-strategy.md) |
