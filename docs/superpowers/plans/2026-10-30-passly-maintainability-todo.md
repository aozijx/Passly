# Passly 深度维护审查待办

**最近复核：2026-11-08。** 全模块 Kotlin 静态盘点，并沿生产调用闭包重点检查会话/认证、敏感清理、Flow 恢复、页面与平台宿主、备份、诊断及既有待办；不等同于全仓安全扫描或逐行证明。

## 执行顺序

保留原 Task ID，正文按批次重排。P0 表示工程门禁阻断，P1 为敏感/资源安全或具体运行流程缺口，P2 为恢复/交付合同，P3 为结构维护。**retained** 表示仍有证据支持的工作；**deferred/待验收** 表示已有机制但指定验证未完成；**removed** 仅移除过时缺陷，不代表整项已通过。编号不代表优先级。

| 顺序 | 执行入口 | 依赖与原因 |
| --- | --- | --- |
| 1 / P1 | [安全 Tasks 6、2](2026-11-06-passly-security-scan-followups.md)，本页 37→26、59 | 接收方身份、lease 关闭、撤权与提交事实；先完成 Data admission 与撤权线性化，再闭合 Runtime/reset 跨层终态；Task 43 已完成，安全 Task 8 已完成 |
| 2 / P1 | 本页 38、42、40、41、32；[诊断 Tasks 1–3](2026-11-06-passly-diagnostics-reliability-followup.md) | 保存期输入、seed/流资源、操作终态、分类查询、flush；先固定失败结果再改 owner |
| 3 / P2 | 本页 46、51；[安全 Tasks 3、4、7、9](2026-11-06-passly-security-scan-followups.md)、OTP/搜索专题；Autofill Task 82（设置策略） | 46 接 59 的提交后恢复；备份端到端流式重构已完成；Autofill 设置策略待办由专题清单维护 |
| 4 / 定向验收 | 本页 22、23、27、29、31、44、45、64；[Autofill 宿主验收](2026-11-07-passly-autofill-followups.md#task-21)、Scanner 验收 | 已有机制，不重复实现；JVM、Route/宿主和设备验证分别标记 |
| 5 / P3 | 本页 83 → 84/80，39、35、34、36、78、74、75、77 | 83 管 Detail 具体流程，84 管 owner 清单，80 管状态模式；39/77 先稳定诊断可靠性 |

安全 Task 1/5/10、Autofill 设置快照、OTP HOTP 和非 ASCII 搜索仍有策略合同待裁定；本轮不臆定产品行为，不把候选升级为已验证漏洞。允许独立项并行，表中箭头表示必须先满足的前置条件。

## 专题 owner 索引

| 主题 | 唯一详细待办/验收来源 | 本页关联 |
| --- | --- | --- |
| 架构与并发 | [架构与并发 TODO](2026-11-06-app-architecture-concurrency-todo.md) | 26、37、51、80、83、84；具体缺陷先处理，广域提案随后 |
| Autofill 后续 | [唯一后续清单](2026-11-07-passly-autofill-followups.md)；[模块实施记录](2026-11-06-autofill-feature-module.md)、[BottomSheet 实施记录](2026-11-06-autofill-bottom-sheet-auth-contract.md) | 21 宿主验收、82 设置策略；81 授权异常与 33 系统观察已完成，不再作为当前待办 |
| Scanner | [审查 TODO](2026-11-06-passly-scanner-review-followups.md)、[取景框计划](2026-11-06-scanner-entry-and-framing.md) | 42、51、86；显式关闭/宿主暂停区分在专题 Task 4 |
| Vault / OTP | [搜索 TODO](2026-11-06-passly-vault-search-review-followups.md)、[OTP TODO](2026-11-06-passly-otp-review-followups.md) | 27、32、34–36、42、43 |
| Backup / 诊断 | [Backup 格式说明](../../features/backup/passly-archive-v1.md)、[数据端口迁移审查记录](../../reviews/2026-11-07-backup-data-port-migration.md)、[诊断可靠性 TODO](2026-11-06-passly-diagnostics-reliability-followup.md) | 39–41、73、74、77、86 |
| 安全与资源 | [安全遗留 TODO](2026-11-06-passly-security-scan-followups.md) | 26、37、43–46、59、64；安全 Task 2 统一持有非合作 lease 关闭清单 |

## Autofill 重构后的清理

Autofill 的 7 个旧段落移出本页：9 合入 21、52 合入 33、85 移除；21/33/81/82 由[专题清单](2026-11-07-passly-autofill-followups.md)唯一维护。当前 Autofill 清单只把 21、82 列为未完成；81、33 保留为已完成证据。下方扫描数量与失败统计是历史采样记录。

## 此前扫描与验证记录

- 扫描开始时清点 8 个 Gradle 业务/能力模块及 buildSrc：生产 main 1,072 个 Kotlin 文件（含 buildSrc 13 个），测试源 377 个。尺寸只用于筛查，不按行数拆分。39 个既有主任务逐项复核，新增验证基线 Task 86。
- 修正已迁移的 Autofill、Room 观察、认证输入、通知快照、GC 和正式 UDF 规范等过时描述；补分类查询恢复、导出流所有权及 Scanner 输入资格，保留各专题 owner，不复制第二份修复清单。
- **2026-11-07 文档/调用合同续扫：** removed：Vault 创建页曾把 Autofill 专用 `createFromAutofill(draft, inboxDraftId)` 误记为通用 `EntryCreation`，现按 `EntryCreation.invoke(draft)`、`AutofillEntryCreation.createFromAutofill(...)`、App 双绑定与 `observeCreate(...)` 的实参更新权威功能页；Domain 端口迁移及保留边证据见[迁移审查记录](../../reviews/2026-11-07-domain-cohesion-migration.md)。deferred：Create/Update/Revision Restore 的事务后 GC 取消会传播到写入调用者，是否令已提交结果表现为未完成及可重试尚缺 owner 级行为证据，详见 Task 64；不复制第二份详细待办。
- 串行运行 `gradlew.bat -p buildSrc test --no-parallel --max-workers=2`：通过，实际执行 121 项；随后 `verifyArchitecture`：通过，含 module/error-code 门禁。
- 运行 `gradlew.bat :app:testDebugUnitTest :feature:autofill:testDebugUnitTest --continue --no-parallel --max-workers=2`：首轮 App 主/测试源码编译通过，987 项中 9 失败；Autofill test 任务 UP-TO-DATE。详见 Task 86。
- 收尾运行 `gradlew.bat :app:testDebugUnitTest :feature:autofill:testDebugUnitTest :app:lintDebug :feature:autofill:lintDebug --continue --no-parallel --max-workers=2 --quiet`：App **984 项/9 失败**，仍为诊断 8 项与 Scanner 1 项；Lint 为 app **2 errors/27 warnings**、Autofill **5 errors/1 warning**，构建退出 1。Autofill 单测 XML 131/0，任务复用缓存。
- 执行期间有其他工作推进 Autofill/Scanner/Backup 和测试；本页保留这些并发改动及 Task 21 的同期纠正。统计与验证绑定各次工作区采样，不保证后续变化；新备份端口计划由其原 owner 继续维护。
- 2026-11-07 首轮仅更新待办与本地维护规则，未修改生产/测试实现；后续安全 Task 8 批次完成敏感数组清理和状态一致性修复并单独记录验证。保留既有和并发改动；设备仪器、APK Analyzer、KDF/低内存基准、强杀持久性未执行，不沿用历史通过记录代替。

## P0：当前无工程门禁阻断

<a id="task-86"></a>

### Task 86：恢复本轮扫描发现的验证基线（已完成）

- [x] 修正 `AutofillServiceStatusObservationTest` 5 个过强 identity 断言为异常类型和 message 断言；source 异常合同保持不变。
- [x] 重跑 App 与 Autofill 单测：App 991 项、Feature Autofill 135 项均通过。App/Autofill Lint 已在 Autofill entry 拆分后的工作区运行成功。

**2026-11-07 完成：** 旧 984 项/9 失败和 7 个 lint errors 是历史快照。诊断异常断言、Scanner 停止输入分支与相关 Lint 项已由 owner 修正。状态观察异常在 `callbackFlow` 的 coroutine stacktrace recovery 边界被复制，5 处 `assertSame` 失败实际/期望类型和 message 相同；收窄为稳定语义断言后，过滤用例 7/7 通过，随后 App 991/991、Feature Autofill 135/135 通过。Lint 使用此前同一 entry 拆分源码状态的成功结果，不把当前全量单测命令描述成运行过 Lint。


## P1：敏感数据、并发与用户流程缺口

<a id="task-37"></a>

### Task 37：为认证撤权与数据库操作定义同一线性化边界

- [x] 明确锁定从请求发起到 Data 禁止新命令进入 DAO 的线性化点。`VaultSessionController.lock()` 先发布 `AuthenticationState.Locking`；`AppDatabaseSession` 直接查询 Security-owned `SecureSessionAccessState`，在 Runtime lease 内、DAO block 前重新核验授权。lock 请求前通过 admission 的操作持有 lease，由 Runtime drain 等待真实退出。
- [x] 审计并收口数据库命令入口。`DatabaseTransactionRunner`、Entry/Link/Sensitive/OTP Repository、Autofill Candidate/Credential Resolver、Backup Store、Activity Recorder 与附件 GC 均迁移到 Full Vault 或 Autofill 专用 session API；删除 Repository/runner 自己的授权检查与重复注入。GC 仍在文件副作用前核验 Full Vault。数据库清理使用单独命名的 maintenance API，仍受 Runtime lease/lock gate 保护。没有无授权的通用 `query/transaction` 重载。
- [x] 统一 `DatabaseTransactionRunner.read()`、`write()` 与 `writeForAutofill()` 的 lease 内授权复核；直接 Full Vault 与 Autofill 命令使用同一个 `AppDatabaseSession` admission 边界。新增边界回归分别让 read/write 的授权第一次为真、lease 内复核时为假，并断言 DAO block 不运行。
- [ ] 增加可控挂起的跨层终态回归：在 Security lock 等待数据库 lease/resource 与敏感 key 清理时，从 runner、直接 Full Vault Repository 与 Autofill Repository 发起读写，验证线性化点之后新命令不触达 DAO；已进入事务准确报告提交/回滚/取消结果，敏感 permit、会话、DEK、key 与 lock state 一致。边界与真实 Repository 集成测试证明调用闭包后，不需为每个薄适配器复制同一测试。
- [x] 串行执行当前 Security、Runtime Session、Data JVM 单测与 `verifyArchitecture`：Security 179/179、Runtime Session 34/34、Data 170/170；目标变更 `git diff --check` 通过。
- [ ] 在可用设备上验收真实 Data lease/Room DAO 与 Security reset/lock 交错终态；当前 `adb devices` 无连接设备，instrumentation runtime 尚未执行。恢复模式、Autofill session 结束和 idle timeout 的撤权次序仍需按本项统一验收。

**审查证据：** 原调用闭包中 Runner 与多个 Repository 各自只在获取 lease 前检查授权。当前 `AppDatabaseSession` 通过 Hilt `Provider<SecureSessionAccessState>` 获取当前 Security owner，并按 Full Vault/Autofill 语义提供 protected query/transaction；旧的无授权通用 command API 与逐调用谓词均已删除。普通命令入口均已迁移；maintenance API 只给数据库清理与测试 fixture 使用，GC 不使用旁路。Room observations 也由 `AppDatabaseSession.observeFullVaultWhenReadable()` 内部读取授权 Flow，再组合 Runtime lock/generation 重新订阅，Repository 不再注入或传递授权状态。当前边界测试用可控 access-state 验证第二次授权拒绝时 read/write DAO block 不运行；它还未验证真实锁定与 Repository/Autofill 并发入口的提交/取消终态，因此 Task 37 仍未完成。

**2026-11-07 Security 锁定顺序批次：** `VaultSessionController.lock()` 在进入可挂起清理前发布 `AuthenticationState.Locking`，保留变更前状态以判断 RecoveryMode；随后撤销 permit、调用 Runtime soft-lock/seal 排空 lease，再清敏感会话 key，sealed/fatal 路径在 key 清理后锁 DEK。可控 soft-lock 与 key-clear 屏障验证 Locking 已使 full access 失效，且 key clear 要等 Runtime soft-lock 返回；Security 150 项与 `verifyArchitecture` 通过。此批只记录撤权与 key 清理顺序；后续 Task 37 已另行收口 Data admission 与授权复核。

**2026-11-07 Data admission 批次：** `AppDatabaseSession` 通过惰性 Security provider 直接读取认证权限，并在 lease 内再次核验；Repository 与 Runner 不再传递授权谓词或持有重复访问状态，Room observation 也由 session 集中读取认证 Flow。Full Vault、Autofill 与 maintenance 入口语义显式区分。新增 Room observation instrumentation 回归，分别验证 read/write 在第二次授权核验拒绝且 DAO block 不执行；`:app:kspDebugKotlin`、Data 主代码与 AndroidTest 源集编译通过。`:security:testDebugUnitTest` 与 `:runtime:session:test` 强制重跑通过；`adb devices` 无可用设备，instrumentation 未运行。同步修正数据库映射和认证文档。Task 37 仍待真实 Security/Data 并发终态、Autofill/Recovery/idle/reset 次序验收和定向验证。\n\n**2026-11-07 Room 撤权/事务终态批次：** 新增真实 Room 事务屏障测试：Full Vault 事务获准并进入 DAO 后撤销认证，Runtime soft-lock 等待该 lease；线性化点后的新查询返回拒绝结果、不运行 DAO；释放原事务后确认提交并排空 lease。Data AndroidTest 源集编译通过。连接测试启动前设备在并发 App instrumentation 期间断开，Gradle 报 `No connected devices`，设备端用例仍未执行。

**2026-11-07 GC 授权回调收敛：** `AttachmentGarbageCollectionWorker` 不再接收 `hasFullAccess` 函数参数，改为在每个分页、条目与退避检查点直接查询 `SecureSessionAccessState`；生产调用点不再转发谓词，测试通过可变认证状态覆盖撤权边界。早期定向构建曾被旧 Core→Security 实现依赖阻断；该依赖已通过 Security 边界迁移移除。后续记录的 `:core:testDebugUnitTest`、`:security:testDebugUnitTest`、Data unit/AndroidTest 源码编译与 `verifyArchitecture` 均通过，Task 43 也记录 `AttachmentGarbageCollectionWorker` 7/7 通过。因此旧 KSP 阻断不再是当前待办；Task 37 仍待更广的授权线性化、事务终态、Autofill/Recovery/reset 交错行为验收。

**2026-11-07 生命周期顺序复核：** idle/background 与 Autofill 结果完成后的释放都调用 `VaultSessionController.lock()`；RecoveryMode 锁定升级为 SEALED，Autofill 最后一个 borrower 释放受结果完成屏障保护。Reset 由 `SecureDatabaseResetCoordinator` 在同一 Security `sessionMutex` 下执行，先 SEALED，再按 Runtime lock state 恢复或撤销 permit/清 key；`security lock waits for database reset to finish reopening` 屏障测试验证 lock 等待 reset 完成。该测试使用可控 DatabaseSession fake，真实 Room lease/DAO 结果与 reset/lock 交错仍未联合验收，因此保留 Task 37。

**2026-11-07 Room 列表订阅收敛：** `DatabaseReadAccess` 在授权、lock state 与 lock generation 合流时采样当前 generation，避免快速重开时组合出重复的可读代际并重启 Room Pager 两次。`RoomObservationSessionTest` 现覆盖真实 SQLCipher PagingSource 的新增、编辑更新、过滤与重置恢复；`:data:testDebugUnitTest` 与设备 `RoomObservationSessionTest` 全类通过。Task 37 仍待真实 Security 撤权与 Data reset 并发终态验收。

**2026-11-08 JVM 验证批次：** Security 全量 179/179、Runtime Session 34/34、Data JVM 170/170 均通过；`:data:compileDebugKotlin`、`:data:compileDebugAndroidTestKotlin` 和 `verifyArchitecture` 通过。Security 锁定异常测试将跨协程边界的异常对象 identity 断言收敛为类型与 message，保留清敏、终态和 suppressed failure 断言。一次合并 Gradle 构建曾因 Core Hilt 聚合 wrapper 编译失败；随后独立强制 Data JVM 全量执行通过。`adb devices` 无设备，真实 Room instrumentation 与跨层 reset/lock 仍待验收。
<a id="task-26"></a>

### Task 26：明确 Runtime Session 生命周期转换的并发所有者

- [x] 定义 open/resume/soft-lock/seal/close 的统一生命周期 owner：RuntimeSessionManager 串行化转换；保留 VaultSessionController 会话级串行化并明确锁顺序。
- [x] 屏障测试覆盖两个首次 unlock 并发 open、open 与 seal、seal drain 中 unlock 不能提前 resume、soft-lock/resume；不得覆盖未关闭句柄，终态与生命周期 Mutex 顺序一致。
- [x] 定义 seal 等待 drain 时取消的清理 owner、结果与恢复：一旦发布 SEALED，Runtime 捕获调用方取消、请求 lease 取消并等待实际 drain/close，再传播取消，不留下无人回收的句柄。
- [x] 非合作 lease 超时关闭的详细清单由[安全 Task 2](2026-11-06-passly-security-scan-followups.md#task-2)持有；Task 2 已实现实际 drain 后关闭。
- [ ] 与 Task 37 完成 soft-lock、认证撤权和 Data Reset 的跨层终态验收：覆盖已有 lease 的读写/观察在排空与取消后的真实退出、迟到结果处理及 reset 与并发 lock 的交错。Data 命令 admission 已在 Task 37 统一；此项只保留仍缺的跨层终态测试。运行 Runtime、Security Session、Data Reset 定向测试。

**2026-11-07 Runtime lifecycle 串行化：** `RuntimeSessionManager.lifecycleMutex` 保护完整 `unlock`（close-failure 检查、resume、key copy、resource open）、`softLock` 与 `seal`；`VaultSessionController` 的会话互斥继续作为外层 owner，`SecureDatabaseResetCoordinator` 也使用该外层互斥。锁顺序为 Security/reset session mutex → Runtime lifecycle mutex → SessionLeaseGate 的 resource/lease mutex；请求 lease 与执行资源操作分阶段取 gate 锁，`resumeIfOpen` 唯一的嵌套顺序为 resource mutex → lease mutex。五项屏障用例先在旧实现失败，再在新 owner 下通过，覆盖两个首次 unlock、open/seal、seal drain/unlock、soft-lock/resume 和 seal 调用方取消。

**2026-11-07 soft-lock 排空批次：** Runtime 在 `softLock()` 发布 `SOFT_LOCKED` 后等待活动 lease；5 秒超时或调用方取消会请求登记 owner 取消，再在不可取消清理区等待实际退出，保留已打开的 handle。新增两项屏障回归，分别验证超时取消与调用方取消后仍等待、不关闭资源，且新 lease 被拒绝。Runtime 用例通过。Security 当前已先发布认证撤权、等待 Runtime soft-lock/seal 排空，再清敏感会话 key；Task 26 仅保留与 Task 37 共用的 Reset/跨层终态验收。

<a id="task-43"></a>

### Task 43：保证条目与修订敏感揭示异常时擦除部分结果

- [x] 为 `RevealEntryFieldsUseCase.reveal()` 建立局部敏感值所有权交接，并为 `RevealRevisionFieldsUseCase` 补齐授权门在 Repository 返回后失败/取消时的清理。异常退出时擦除已累计值，成功返回才转交；Detail ViewModel 收到结果后再交给 `DetailWorkSession`，History ViewModel 对迟到结果 wipe、对当前结果交给 revision reveal store。
- [x] Data producer 使用同一局部 owner 构造 `RoomSensitiveFieldRepository.revealMany()` 与 `RoomEntryRevisionRepository.reveal()` 结果；DAO/decrypt/assembly 失败时擦除部分结果后原样重抛。可控后续字段失败测试已覆盖 builder 的部分累积清理。
- [x] 覆盖读取/创建中途失败、授权后置取消、记录异常/取消、组装失败与调用方交接。低敏字段测试确认前一字段已读取后第二字段异常原样传播；该分支与高敏字段共用同一局部 owner/finally 清理，不额外注入只用于测试的 OwnedChars factory。实际 DAO/decrypt 调用位于共享 builder 的 create block 中，后续创建失败由同一行为测试覆盖。Detail/History 的发布、迟到结果 wipe 与离页清理合同按 ViewModel、工作会话和两个 reveal store 复核。

**证据：** `RevealEntryFieldsUseCase` 在 authorize 调用前跟踪低敏 `OwnedChars`，并跟踪授权 block 内返回的高敏结果；`finally` 只把实际放入成功返回 map 的值交给调用方，异常/取消时擦除全部已持有值。`RevealEntryFieldsUseCaseTest` 7/7 通过，覆盖第二个低敏字段读取异常传播、activity recording 异常/取消、授权 suspend 边界取消及既有成功/拒绝行为；擦除断言覆盖 activity 异常/取消和授权 block 完成后的取消。Data 新增 `buildSensitiveResults`，两个 Room Repository 都经此 helper 创建/组装结果；后续 create/decrypt/DAO 或 assemble 失败时擦除已累积值后重抛，`RoomEntryRevisionRepositoryBoundaryTest` 4/4 通过，分别验证后续 create 与 assemble 失败。History UseCase 暂持 Repository 的成功 reveal 结果，只有 `RevisionOperationResult.Succeeded` 才转移所有权；gate 在 block 返回后取消与成功转移测试已执行，`EntryRevisionUseCasesTest` 6/6 通过。GC 授权回调收敛后 worker 测试 7/7 通过。`DetailViewModelTest` 24/24、`DetailWorkSessionTest` 18/18、`DetailRevealStoreTest` 2/2、`DetailRevisionRevealStoreTest` 1/1 通过。`:data:compileDebugAndroidTestKotlin`、根 `verifyArchitecture`、定向 `git diff --check` 均通过。调用方能在 store 接管成功结果；viewmodel 离页/替换时取消工作并清除 store，`RevealRevisionFieldsUseCase` 对授权边界无法返回的值兜底擦除。此 task 完成。

<a id="task-59"></a>

### Task 59：闭合生物识别轮换的提交后恢复与启动对账

- [ ] 统一轮换 durable commit 后结果：binding 已切换后，候选清理状态读取或 availability refresh 普通失败不得显示未提交/允许盲重试；按持久事实保留 pending 派生恢复。
- [ ] 覆盖 withBiometricRotationCandidate 正常返回后 loadBiometricState 失败、provisioner 在 rotate Success 后 refreshAvailability 失败；验证提交前/后异常、取消、fatal、alias/journal、启动恢复、敏感缓冲、host lease。
- [ ] 精确验收应用密码 durable commit 后的取消结果：`DefaultAuthenticationMethodProvisioner.setAppPassword(password)` 先由 `dekManager.rekeyWithKey(...)` / `initializeWithKey(...)` 持久化 APP_PASSWORD envelope，再执行 `reconcileSession()`；普通模式先调用 `refreshAvailabilityAfterCommit()` 再 `session.markAuthenticated(...)`，恢复模式调用 `finishRecoveryPasswordProvisioning(seal, refreshAvailability)`。会话协调中的刷新或认证态发布若取消，会从 `runAppPasswordProvisioning()` 直接传播，跳过普通异常分支的 `hasCommittedAppPasswordEnvelope(store, salt)`；`AppPasswordSettingsInteractor.execute(request)` 只在 `finally` 清理输入，`AppPasswordSettingsViewModel.runChange()` 没有收到 Completed 时不发成功 effect。用提交前/后取消的 owner 行为覆盖确认 `isEnabled` Flow 能否及时校正页面状态、是否留下可重试界面或造成重复提交；不可把普通异常的 envelope 读回保证外推到取消。
- [ ] 保留 reconciler 可重试清理及 fatal 传播，以真实 rotate/provisioner/启动 owner 测试闭合并同步认证文档。

**2026-11-07：retained，提交后窄残余已部分收敛。** `withBiometricRotationCandidate()` 清理阶段读取持久状态失败时保留已提交 Success，并携带 `BIOMETRIC_ROTATION_CLEANUP`；Security Manager 对认证方式解析普通失败启用 1s 起步、上限 60s 的指数退避，成功后发布最新快照。生物识别轮换、禁用生物识别与禁用应用密码在持久操作后刷新失败时返回成功并携带 `AUTHENTICATION_AVAILABILITY_REFRESH`；UI 以提示反馈，不显示提交失败。取消仍传播，fatal `Error` 不转换。Security 176 项/34 类、App 全量 998 项、`verifyArchitecture` 通过。剩余：启动时刷新失败的可见状态、provisioner/store 的真实 durable-commit 集成覆盖，以及取消/fatal/host lease 与启动恢复联合验收。

**2026-11-07 应用密码结果透传：** 设置/修改/禁用的 App interactor 不再把 Domain `Success` 压成无信息的 `Completed`；完成结果携带 `pendingMaintenance`，ViewModel 发出成功效果后再按 Domain 维护类型提示。应用密码持久提交后，可用性刷新失败被带回 Domain 结果并提示重试状态；恢复模式封存后刷新失败同样保留该语义。新增用例覆盖 Security 提交结果、恢复流程、Interactor 映射和 ViewModel 反馈。当前 Security 177 项/34 类、App 1010 项/179 类全通过，`verifyArchitecture` 通过。仍需用真实 provisioner/store 组合覆盖提交点前后及异常次序。

**2026-11-07 候选清理恢复：** `withBiometricRotationCandidate()` 在操作已返回结果后读取轮换状态/清理候选若遇普通 `Exception`，不再覆盖操作结果；成功结果标记 `BIOMETRIC_ROTATION_CLEANUP` 以使用已有恢复合同，失败或取消结果保留原结果。`CancellationException` 继续传播，fatal `Error` 不捕获。新增回归先按旧实现失败，修复后 Security 单测 175 项/33 类全部通过。仍待真实 rotation/provisioner 测试覆盖 durable commit 后刷新失败，以及提交前/后取消、启动恢复和 host lease。


<a id="task-38"></a>

### Task 38：让密码配置状态机在保存期间拒绝迟到输入

- [x] 在 `UnlockViewModel` 与 `RecoveryModeViewModel` 的状态所有者/Reducer 闭合 `Saving` 状态：提交启动后到达的新密码、确认密码、重新打开或关闭对话框命令不能结束/替换当前请求，也不能重新开放第二次提交。不能只依靠 Compose `enabled = false`，因为 ViewModel 是状态和命令合同的最终所有者。
- [x] 以单飞状态与页面 owner 保证操作期间不能重开或重复提交，旧结果不能改写之后的密码草稿；覆盖 Changed/Reopened/Dismissed/重复提交以及 Success/Failure/Cancelled/普通异常次序，断言调用数量、对话框状态和反馈归属、敏感值清理。
- [x] 检查 App Password Settings 和恢复码设置的等价 guard；只修复确认存在的漏洞，不引入跨功能通用状态框架。运行 Unlock/Recovery ViewModel 与 reducer 定向测试、`verifyArchitecture` 和 `git diff --check`。

**完成证据（2026-11-07）：** `UnlockViewModel` 与 `RecoveryModeViewModel` 现在以 owner 内单飞标志拒绝 Saving 期间的编辑、重开、关闭和第二次提交；Reducer 也拒绝 Saving 状态变更，并擦除被丢弃的敏感输入。恢复页面退出会丢弃当前草稿，旧请求完成不能向重新进入的页面投递结果。两组新增 ViewModel 测试覆盖成功、结构化失败、取消后重试、普通异常映射、并发命令抑制、离页/重入和 `OwnedChars`/局部数组清理。App Password Settings 已有操作进行态与页面 owner guard；Recovery Code Settings 已有 `commitInFlight` 和 Committing dismiss guard，且页面退出后通过提交核对恢复持久事实，不需重复增加锁。定向 Unlock/Recovery ViewModel 与 Reducer 测试以及 `verifyArchitecture` 通过；最终 `git diff --check` 通过。生产依赖仍通过 Domain `AuthenticationMethodProvisioner`，没有引入实现类依赖或兼容桥接。

**2026-11-07 测试语义校正：** 全量 App 单测发现认证输入清理后仍有旧断言把 Saving/Accepted 状态当作持有原 `OwnedChars`，取消重试也未重新输入。测试改为提交前保留 owned-buffer 引用并断言提交后状态为空、原缓冲已擦除；重试前显式重新输入。Unlock、Recovery Mode、Recovery Code Settings 定向测试通过；完整 `:app:testDebugUnitTest` 1024 项通过。

<a id="task-42"></a>

### Task 42：让共享条目编辑能力由条目编辑层持有

- [x] 按实际调用闭包迁移共享条目更新能力：`EditEntryUseCase.edit(entryId, edit)` 位于 `feature.vault.entry`，Detail 与 Password/OTP/BankCard/Wi-Fi ViewModel 均已改用新 owner，旧 `EditDetailEntryUseCase` 生产实现已从 `feature.vault.detail` 删除；更新逻辑和 `EntryEdit` 契约保留。读取型 `LoadEditableEntryUseCase` / `EditableEntryLoadResult` 仍只由 `DetailViewModel.prepareEntryForEdit()` 消费，保留 Detail owner，不机械搬入编辑层。此为当前工作树的源码迁移结果，门禁与行为验证仍见下列待办。
- [x] 按新 owner 收敛包依赖与架构门禁：编辑能力只依赖 Domain/Core 端口与规则，禁止 `presentation/editor/*` 反向依赖 Detail presentation 或仅为搬包新增同签名转发层。迁移保留认证代际检查、敏感字段揭示与 wipe、编辑版本/冲突合同、统一日志及成功/失败行为；若发现该能力确实拥有 Detail 专属宿主状态，先拆清该状态 owner，不把页面导航/状态搬入 UseCase。
- [x] 为 Detail 预加载补齐可恢复异常反馈与重试合同：`DetailViewModel.prepareEntryForEdit()` 只映射普通 `Exception` 为当前条目 `EditPreparationFailed` effect，继续传播 `CancellationException` 与 fatal `Error`，不会伪装成 `NotFound`/`AccessDenied`。`DetailRoute` 过滤过期条目 effect 并显示本地化失败 Toast；保留编辑入口，用户再次点击即可重新预加载。ViewModel 的普通异常后重试、取消和 fatal 传播有定向测试；Route 的实际 Toast/重复点击宿主行为仍由最后一项整体验收跟踪。
- [x] 完成完整表单 reveal 的 JVM 敏感值驻留评估：`SensitiveValue.toTransientString()` 通过 `String(chars)` 创建不可变明文副本，值随后进入 Domain `Entry`/credential、一次性 seed 和 Password/OTP/BankCard/Wi-Fi 的 String 表单状态。当前领域及表单合同都要求 String；局部换成可变缓冲会造成第二套模型且不能消除构造 Entry、Compose 输入值和持久化写入时的 String 副本。现有路径可在 `finally` 覆盖临时 `CharArray`、wipe 被 reveal 的 `SensitiveValue`，并在 seed 消费/丢弃/锁定及表单清理时释放可控引用；这些操作不能保证擦除不可变 String。保持现有单一 String 模型，不声称可靠 JVM 内存擦除；若未来要改变此边界，需先为 Domain、表单与写入链路设计统一可控缓冲合同，不能单改表单字段。
- [x] 分开定义 Detail 预加载失败与编辑器 seed 失败的重试路径：Detail 读取失败由详情页显示提示，再次点击 Edit 会启动新的预加载；编辑器 `loadExistingEntry(result)` 只校验并消费 seed，不重新查询条目。Missing/无效 seed 进入 `EditorLoadPhase.Failed`，Route 通过 `EditorLoadFailureEffectHandler` 提示“返回后重试”；没有添加已消费 token 的虚假重试。导航暂存失败按精确 token 补偿已在上方关闭。条目/认证替换、迟到结果及 Route 宿主可见性仍纳入整体行为验收。
- [x] 闭合 seed 暂存后的导航失败补偿：完整编辑与扫码 Wi-Fi 都改由 `EntryEditorSeedCoordinator.navigateWithSeed()` 统一持有 stage、route 构造、导航和精确 token cleanup；移除 `EntryEditorSeedStager` 双路径。普通 `Exception` 清理本次 token、记录原始 Throwable 并返回可见失败结果；取消和 fatal `Error` 清理后原样传播。导航回调重入并暂存新 token 时，旧调用的失败补偿不删除新 token。单槽仍只释放受控引用，不声称擦除不可变 String；消费、认证撤销和 graph 销毁语义保留。
- [x] 闭合共享 `EditorWriteSession` 的 fatal `Error` 路径：当前保存协程对 `CancellationException` 和普通 `Exception` 有显式转换，但 fatal `Error` 会原样逃逸且不会调用 `onIdle()`/`onClear()`；若宿主在错误传播后仍存活，session 可能卡在 `Saving` 且敏感表单仍保留。定义窄清理路径擦除表单并释放忙碌状态，同时继续传播原始 `Error`，不能将未知提交结果映射为可重试 `Failed`，避免可能的重复写入。
- [ ] 按当前 owner 补齐并执行行为验收：`LoadEditableEntryUseCase` 与 `DetailViewModel.prepareEntryForEdit()` 覆盖 Repository/授权/activity-recording 普通异常、取消、认证撤销及条目版本变化；Detail Route 覆盖失败可见性和重新发起。`EditEntryUseCaseTest` 已从旧 `feature.vault.detail` 路径迁到 `feature.vault.entry`，同包引用新 owner；Detail/四类编辑 ViewModel 测试已改为调用新 owner，但仍需运行并核对版本冲突、取消和迟到结果合同；seed 消费测试覆盖无效/已消费 token 后需返回 Detail 重新 stage。另为 `EditorWriteSession` 覆盖 fatal Error 清理并继续重抛。运行相关 UseCase/ViewModel/Route 测试、`verifyArchitecture` 与 `git diff --check`。

**2026-11-07 Detail 预加载异常实现进度：** `prepareEntryForEdit()` 现对普通异常发出携带 `EntryId` 的 `EditPreparationFailed`，仅在同一详情条目、认证状态和完整访问仍有效时发送；Route 对过期条目事件静默丢弃，并复用本地化“无法加载条目”提示，用户再次点编辑即可重新发起。取消和 fatal `Error` 原样传播，没有新增兼容入口。`DetailViewModelTest` 27 项通过（含普通异常后重试、取消传播与 `LinkageError` 传播），六个编辑能力/页面测试合计 98 项通过，`verifyArchitecture` 与 `git diff --check` 通过。Route 的生命周期/Toast 可见性尚无独立宿主行为断言，因此上方完整验收仍未关闭。

**2026-11-07 EditorWriteSession fatal 清理：** `write()`/draft 构造抛 `Error` 且 attempt 仍为当前 owner 时，session 现在关闭为不可重激活终态、清空当前表单并停止 effects，再原样传播 fatal；清理异常仅作为 suppressed failure 附加，不把未知提交结果映射成 `Failed`。新增测试在 RED 阶段复现表单未清和 Saving 卡住；实现后 `EditorWriteSessionTest` 13 项通过，精确检查错误身份、表单清空、释放 saving 和 session 不可重开。随后一次无关增量断言重跑被工作区中的 `BackupChunkedContainerCodecTest.kt` 编译错误阻断；该断言已撤回，保留先前通过的测试版本。独立 `verifyArchitecture` 通过；全量相关 App 测试/Route 宿主验收仍待当前备份测试阻塞解除。

**2026-11-08 seed 导航失败实现：** `EntryEditorSeedCoordinatorTest` 与 `VaultNavigationTest` 定向共 12 项通过，覆盖普通导航异常的原始异常日志与失败结果、失败后 token 消失、旧调用失败不清理新 token、取消/fatal 清理后传播，以及扫码 Wi-Fi 成功路径仍使用 opaque token。`EntryEditorSeedStager` 与 `EntryEditorSeedStageResult` 无生产/测试引用。Detail Route 显示本地化编辑器打开失败提示；Wi-Fi 扫码导航失败映射到现有拒绝反馈（静态调用链复核）。相关 Kotlin 编译、`verifyArchitecture` 和本批 `git diff --check` 通过。本条只关闭 seed 导航补偿，不关闭 Task 42 的 Detail 宿主验收、敏感 String 生命周期评估和整体行为验证。

**2026-11-08 敏感 String 驻留评估：** `LoadEditableEntryUseCaseTest` 覆盖 reveal 拒绝/不完整结果与认证会话替换；`DetailViewModelTest` 覆盖预加载取消；`EntryEditorSeedStoreTest`、`EntryEditorSeedCoordinatorTest` 覆盖 seed 清除及锁定；`EditorWriteSessionTest` 覆盖取消、保存清理和锁定输入门禁。这些只证明授权/owner 生命周期与受控引用释放，不证明 JVM 擦除。`Entry`、Domain credential 和四类编辑 Form 均以 `String` 为值合同，局部改缓冲会引入重复模型且无法避免入口/Compose/写入处重新创建 String，因此本轮将其作为已评估的固有限制记录；如需更强擦除语义，应先整体迁移 Domain、表单和持久化写入边界。

**2026-11-08 Detail 与 seed 重试边界复核：** `prepareEntryForEdit()` 当前捕获普通 `Exception` 发出 effect，`DetailRoute` 仅对匹配当前 `entryId` 的 effect 显示本地化 Toast；用户可重新点 Edit 发起新读取。四个编辑 Route 用 opaque token 调用同步 `loadExistingEntry(result)`；Missing/AccessDenied 或类型/会话不匹配会清空表单并置为 `EditorLoadPhase.Failed`，`EditorLoadFailureEffectHandler` 给出返回后重试提示。此处不存在编辑器内 Repository 重读或复用已消费 token 的路径。上述实现项已闭合；整体 Detail Route 宿主行为和迟到结果过滤仍保持开放，不能用 ViewModel 单测替代。

**2026-11-08 Task 42 定向验证续记：** 对 `EditEntryUseCaseTest`、`LoadEditableEntryUseCaseTest`、Detail ViewModel/Reducer、四类编辑 ViewModel、`EditorWriteSessionTest`、`EditorSaveEffectHandlerTest` 与 `VaultNavigationTest` 分别执行定向单测，共 151 项、0 failures/errors；`verifyArchitecture` 通过。此证据覆盖当前 JVM 行为和生命周期 effect handler，不覆盖真实 Detail Route Toast/重复点击、编辑器 Route 重试显示或 Android Activity 重建，因此 Task 42 保持开放。

**历史调用闭包复核（2026-11-06，迁移前快照）：** `rg` 生产引用显示 `LoadEditableEntryUseCase` / `EditableEntryLoadResult` 只由 `DetailViewModel` 使用：`prepareEntryForEdit()` 以当前 `uiState.entry.id` 调 `loadEditableEntry(entryId)`，收到 `Loaded(entry, authentication)` 后核对 ID、类型、认证对象身份、完整访问权和当前详情条目，再交给 `VaultGraphRegistration.onEditFullForm` stage。四个 Route 从 token 调 `seedCoordinator.consume(token)`，随后同步调用各自 `loadExistingEntry(result)`；这四个函数不依赖或调用读取用例，只校验 seed 访问并把 `seed.entry` 映射成表单。迁移前 `EditDetailEntryUseCase.edit(entryId, edit)` 由 DetailViewModel 和 Password/OTP/BankCard/Wi-Fi 四个 ViewModel 共用，维护互斥、读取最新条目、`EntryEdit.applyTo()`、预期版本更新及一次冲突重试；旧 `feature.vault.detail` 包路径是共享能力命名/依赖泄漏候选。该候选已按上方记录迁到 `feature.vault.entry.EditEntryUseCase`，行为验证仍开放；loader 保留 Detail owner。




<a id="task-40"></a>

### Task 40：让备份目录设置命令失败可见并保留授权状态

- [x] 在 `DataManagementSettingsViewModel` 为目录选择与清除命令建立完整失败反馈：协程取消继续传播；普通异常记录 Backup 错误日志并转为当前页面 owner 的失败 Effect，不得从 `viewModelScope.launch` 无处理逸出，也不得把未完成操作呈现为成功。
- [x] 收窄 `BackupDirectorySettingsInteractor` 的补偿异常边界：补偿读取与 NonCancellable grant release 不再吞 fatal `Error`；普通异常依持久状态补偿，已提交设置下旧 grant 清理失败保留 Saved，并记录资源清理告警。
- [x] 目录清除失败时保留持久 URI 与仍被配置引用的 Android grant，向用户呈现明确失败；目录选择失败时只在设置确定没有引用新 URI 后释放新 grant。`BackupRoute` 仅在清除成功后关闭确认框，失败时保留重试入口并防止重复提交。
- [ ] 已增加 ViewModel/Interactor 单测与清除对话框仪器测试，覆盖异常、取消、fatal Error、授权保留/释放顺序及页面 owner；保留“先持久化新 URI、再释放旧 URI”顺序。完整 App 单测、AndroidTest Kotlin 编译、`verifyArchitecture`、`git diff --check` 已通过；需在连接设备后执行 `BackupDirectoryClearDialogTest` 再关闭本项。

**2026-11-08 失败反馈与日志批次：** `BackupDirectorySettingsInteractor` 将普通操作异常与取消/fatal Error 分开处理，补偿读取或授权释放中的 fatal 错误继续传播；提交后旧目录 grant 清理失败记录 `Backup.ResourceCleanupFailed`，不反转已提交结果。`DataManagementSettingsViewModel` 对选择/清除普通异常记录 `Backup.Error` 并发当前页失败 Effect，CancellationException 继续传播；页面退出后 selection 迟到结果仍被 owner gate 丢弃。清除 UI 等待成功后关闭，失败保留对话框重试；测试覆盖忙碌态按钮不可重复提交。定向 `BackupDirectorySettingsInteractorTest` 16/16、`DataManagementSettingsViewModelTest` 5/5 通过；完整 App 单测 XML 汇总 191 份、1064 项、0 failures/errors；`:app:compileDebugAndroidTestKotlin`、`verifyArchitecture`、`git diff --check` 通过。`adb devices` 无设备，仪器测试尚未运行，故 Task 40 保持未完成。

<a id="task-41"></a>

### Task 41：保证 Backup Gateway 异常能结束操作状态

- [x] 让 `DefaultBackupOperationGateway` 的公开操作遵守统一终态合同：`AuthorizationGate`、备份目录设置读取、SAF storage port 或 archive service 抛出的普通异常都变成 `BackupExecutionResult.Failure` 并通过 `AppErrorReporter` 记录；`CancellationException` 和 fatal `Error` 继续传播，提交/清理与敏感密码清零顺序保持不变。Gateway 映射后 ViewModel 不会重复处理同一异常。
- [x] 覆盖 `BackupViewModel.execute()` 的异常终态：漏出的普通异常复位 Executing 状态、报告错误并经当前 page owner 发布失败 effect；密码仍在 `finally` 清除，可再次使用失败工作流；取消继续传播。
- [x] 扩展 `DefaultBackupOperationGatewayTest`、`BackupViewModelLifecycleTest` 与 `BackupViewModelAccessTest`，注入目录 `Flow.first()`、AuthorizationGate、storage/check、archive service 和 ViewModel 普通异常，并保留协程取消、目标清理及敏感密码清零断言。Backup 包定向单测与 `verifyArchitecture`、`git diff --check` 已通过；一次性目录读取异常与 `RecoverableRead` 长期订阅恢复分别由不同 owner 验证。

**2026-11-08 Backup 异常终态批次：** `DefaultBackupOperationGateway` 在授权回调边界接住授权/操作普通异常，设置目录 `Flow.first()`、export target 创建、可写性检查和 archive service 的直接异常均映射到 `BackupExecutionResult.Failure` 并通过 `AppErrorReporter` 记录；取消与 fatal `Error` 原样传播，部分 export 清理优先级及操作密码 `finally` 清零保留。`BackupViewModel.execute()` 作为兜底边界将漏出的普通异常报告并映射到既有失败 mutation/notice，当前页面可重试；Gateway 已映射的结果不会再被 ViewModel 重复捕获。新增授权 gate、目录 Flow、storage check、import service 与 VM 抛错回归，包含报告次数、页面失败终态和密码清零。`:app:testDebugUnitTest --tests "com.aozijx.passly.feature.backup.*" verifyArchitecture` 通过；生成测试 XML 汇总 Backup package 119 项、0 failures/errors；`git diff --check` 通过。

<a id="task-32"></a>

### Task 32：明确 Vault 列表 ViewModel 的工作流边界并补宿主行为覆盖

- [x] `VaultViewModelTest` 覆盖分类首次/发值后异常与重试、空/非空更新、设置 sort 更新触发 Paging generation、搜索和类型筛选组合进入 Paging query、同条目删除单飞、离页后 field/OTP copy 丢弃以及当前页 copy 成功。
- [x] `VaultViewModelTest` 覆盖 sort 设置写入失败：保留当前持久化排序，并仅向活动 page owner 发出本地失败反馈。
- [x] `VaultViewModelTest` 覆盖可见 OTP 订阅后条目移入回收站会清除 OTP 状态，后续页面刷新不会通过残留订阅重新激活该条目。
- [x] `VaultViewModelTest` 覆盖删除失败在页面退出后才返回时不向已退出的 page owner 发出错误 effect。
- [x] `VaultOtpSubscriptionEffectTest` 覆盖 OTP 显示开启时订阅、关闭及离开组合时退订；AndroidTest 源码编译通过，设备运行仍待有可用设备时验证。
- [x] 收口 Vault 命令的授权撤销竞态：Trash 授权闭包内再次查询 `SecureSessionAccessState`，锁定后不进入条目查询/命令；field/OTP copy 的交付谓词同时要求 page owner 有效及完整会话仍获准。`TrashCommandUseCaseTest.sessionLockedWhileAuthorizationIsPendingDoesNotRunCommand` 与 `VaultViewModelTest.sessionRevocationWhileAuthorizationIsPendingSuppressesCopy` 覆盖认证等待期间锁定；定向 Trash 命令/映射测试通过。
- [ ] 补齐授权撤销期间操作及其余 page owner 交接/迟到结果行为；纯 `VaultReducer`/`VaultQueryState` 单测仍不能代替 ViewModel 的协程和 `PageEffects` 所有权合同。
- [x] 依实际依赖拆出 `VaultEntryCommandWorkflow`：它持有删除单飞并执行 trash、field/OTP copy 与当前 page effect 检查；列表筛选、Paging 和 OTP 可见订阅仍由单一 `VaultViewModel`/graph owner 管理，避免再拆出共享 `VaultUiState` 的协调 ViewModel。数据库重置后的 Paging 重订阅继续由 `AppDatabaseSession.observeFullVaultWhenReadable()` 管理，不另造 reload 桥或一一转发类。
- [ ] 让 Route 消费与各 owner 对齐：`VaultRoute` 负责 Compose/导航/system effect，Content 只接收 immutable state/actions；确保图级 `VaultViewModel` 在 Trash/Detail 转场或目的地保留期间不会重复创建 effect collector 或重复订阅 Paging/OTP。给选定的 owners 增加 Route/NavHost 行为回归后同步状态管理文档，运行 Vault Presentation 定向测试、`verifyArchitecture` 和 `git diff --check`。

**当前证据（2026-11-08）：** `VaultViewModel` 保留列表查询、分页/筛选、设置排序、OTP Runtime 与同一 `VaultUiState`；分类读取由 `RecoverableRead` 管理并投影为 `Loading/Ready/Failed`。删除、字段复制和 OTP 复制由 `VaultEntryCommandWorkflow` 执行，它持有按条目删除单飞，并用当前 `PageEffects` owner 控制异步结果；VM 只分派当前页面动作，不再保留列表删除确认状态。`VaultViewModelTest` 已覆盖分类恢复、sort 写入失败、settings sort 驱动新 Paging generation、搜索与类型筛选组合、删除单飞、删除后 OTP 状态清理、退出页面后的删除失败抑制，以及离页/current-page copy；新增撤权期间锁定时不复制回归。`TrashCommandUseCaseTest` 现在验证授权等待期间锁定不会触达条目查询或命令。`VaultOtpSubscriptionEffectTest` 覆盖 OTP 行的组合生命周期订阅与退订。Route/NavHost 转场覆盖仍缺；`VaultGraphRegistration.vaultGraphViewModel()` 使用图级 VM owner，Route 的 destination 生命周期与 effect collector 仍需宿主验证。

**2026-11-08 Vault 列表删除确认死路径清理：** 调用闭包确认 `VaultScreen` 不消费 `dialogs.pendingDelete`，实际列表删除只由 `QuickDelete` 触发；移除无人调用的 `ItemToDeleteSelected` / `ConfirmDelete`、`pendingDelete` 状态/UI 投影、Reducer mutation 与 workflow 分支。保留真实的 Trash command、同条目单飞、当前 page effect 失败反馈和删除后的 OTP Runtime 清理。切换后全仓无 Vault 列表确认删除旧符号引用（Trash 页面自己的确认状态独立保留）；四个 Vault list 定向测试共 28 项通过，`verifyArchitecture` 通过。Task 32 的 OTP 可见行退订、撤权及 Route/NavHost 覆盖仍待处理。

**2026-11-08 Vault sort 写入失败回归：** 注入 `setSort` 普通异常，验证当前 page owner 收到 `vault_error_sort_failed`，而 `selectedSort` 继续等于设置 Flow 的已保存值。定向 `VaultViewModelTest.sortPreferenceFailureShowsCurrentPageFeedbackWithoutChangingSelectedSort` 通过（1 项）。

**2026-11-08 Vault 删除后 OTP 失效回归：** 先订阅并生成可见 OTP，确认删除完成后状态被清除；再模拟页面恢复刷新，验证旧订阅不会重新启用该条目。定向 `VaultViewModelTest.completedDeleteClearsVisibleOtpAndDoesNotReactivateItOnRefresh` 通过（1 项）。

**2026-11-08 Vault 迟到删除失败回归：** 删除授权等待期间退出页面，之后 Repository 返回失败；`PageEffects` 不向已退出 owner 投递错误。定向 `VaultViewModelTest.deleteFailureCompletingAfterPageExitDoesNotEmitStalePageEffect` 通过（1 项）。

**2026-11-08 Vault OTP 行订阅编译核对：** 既有 `VaultOtpSubscriptionEffectTest.subscriptionTracksVisibilityAndCompositionLifetime` 覆盖显示开关与组合销毁时的 unsubscribe；`:app:compileDebugAndroidTestKotlin` 通过。当前无连接设备，instrumentation 运行未验证。

**2026-11-08 VaultRoute 无效 owner 开关清理：** 全仓仅有 `VaultRoute` 自身声明和默认值为 `true` 的条件读取，没有调用方传入 `false`；删除 `isEffectsPage` 参数与不可达分支，保留 overlay 关闭时创建、overlay 打开时退出的现有 owner 生命周期。Route/NavHost 转场期间的 owner 行为仍待宿主验证。

**2026-11-07 分类 Flow 缺口（已处理）：** 旧实现由 `VaultViewModel` 直接 collect `availableCategories`，普通异常会结束 collector，空列表也无法表达加载或失败。现改由 `RecoverableRead` 管理分类订阅，并通过明确状态和 Retry action 暴露失败恢复；锁定/重开恢复由数据库 observation owner 管理，证据见下方。

- [x] 分类查询由 `RecoverableRead` 持有并映射为 `VaultCategoriesState.Loading/Ready/Failed`，失败可通过 `RetryCategories` 重试；ViewModel 测试覆盖首次及发值后普通异常、重试恢复、空列表与后续分类更新。UI 和 ViewModel 已删除旧 `availableCategories` 列表状态别名，读取明确状态。
- [x] 锁定/重开由 Data observation owner 的 `flatMapLatest` 取消旧 Room Flow、锁定期间发占位值并在可读后重订阅；`DatabaseObservationRecoveryTest` 与 `RoomObservationSessionTest` 覆盖锁定竞态和重开，`RecoverableReadTest` 验证取消不会伪装成失败，`VaultViewModelTest` 验证空/非空分类投影。分类 Flow 属于 graph-scoped VM，不是 page 请求；无需再造 page token 或重复的迟到结果桥接层。Paging 查询仍由 `RecoverableRead` + `buildRecoverableVaultQueryStates()` 组合。


## P2：恢复与交付合同

<a id="task-46"></a>

### Task 46：让认证方式 StateFlow 发布同一持久快照的最新解析结果

- [ ] 补真实 manager/store/provisioner 集成覆盖：同一持久快照解析、并发刷新、提交后刷新、调用方取消、恢复事件；状态对象单测不代替 manager 合同。
- [x] 为普通刷新失败建立 manager-owned 指数退避重试（1s 起步、最高 60s），不创建重复 retry job；成功刷新只由现有 generation/revision owner 发布，取消继续传播，fatal `Error` 不吞并。
- [ ] 为启动时首次刷新失败提供可观察状态，并补真实 manager/store/provisioner 同一持久快照、并发刷新、提交后刷新、调用方取消和恢复事件行为覆盖。认证设置变更后的提交后刷新失败已有 Domain pending 状态与页面提示，但启动空状态仍无独立失败/重试投影。

**2026-11-07：retained，收窄剩余合同。** resolver 已读统一 credentials snapshot，AuthenticationMethodsState 已有 generation、串行解析、recovery revision，刷新由 manager 长期 scope 托管；测试覆盖状态对象竞争/取消/失败 observer。移除旧“分别解析过时快照”的描述。

<a id="task-51"></a>

### Task 51：明确非 Scanner Route 一次性 effect 的前后台交付合同

- [ ] 逐项定义 Toast/Snackbar、导航、SAF picker、权限和平台交付在 STOPPED、恢复、离页、owner 替换后的有效期；短命反馈明确是否丢弃，待完成请求明确如何恢复。
- [ ] 补 PageEffects 及真实 Route/宿主测试，覆盖同页 STOPPED→STARTED、转场、请求代际、锁定/关闭及 callback 异常；不把不同 effect 塞入统一 handler 表。

**2026-11-07：retained，机制已接入，剩语义/宿主验收。** Detail、Vault、BackupOperation 等已用 collectPageEffects，PageEffects:61–69 已 repeatOnLifecycle(STARTED)。PageEffectLifecycleTest:21–59 明确同页恢复补发缓冲事件，剩余是区分 effect 有效期，不是笼统添加 wrapper；旧直接后台 collector 清单移除。


## P3：按 owner 分批整理

<a id="task-73"></a>

### Task 73：把官方 Android 检查纳入维护复核并处理已发现的质量项

- [x] Autofill entry API guard/context、Scanner camera opt-in 与 SecurityRoute 配置感知资源访问已按当前实现修正。
- [x] App 与 Autofill Lint 在 entry 拆分后的工作区复跑退出 0；历史 warning/error 计数不再作为当前清理目标。

- [ ] 按本轮 app/Autofill Lint 新报告裁定剩余项，不沿用旧数量；清理 EmptySuperCall、冗余 API、UseKtx、资源时保留清理/持久时序及中英文合同。
- [ ] 依赖升级独立验证：catalog 当前 palette 1.1.0、Material3 1.5.0-beta02、DataStore 1.3.0、SQLCipher 4.20.0 已更新；旧“14 条升级提示”不再整体列为未实施。
- [ ] APK Analyzer 保存交付/release APK 的 DEX、资源、native、assets 基线；历史 debug 体积不是 release 优化收益，设备/性能验收按功能 owner 执行。

**2026-11-07 更正：** DetailViewModelTest 旧 CREATE/Lifecycle 夹具已改 AUTOFILL，实际 retry 并验证订阅两次及 Ready，移出待修。ScannerResultCard 的 Modifier 位置已调整，Lint 任务已在 entry 拆分后成功退出；当前测试失败由 Task 86 收敛并已清零，不沿用 2026-11-02 统计。

<a id="task-83"></a>

### Task 83：复核 Repository 覆盖并把业务工作流移出大 ViewModel

- [ ] 以当前工作区版本审查 `DetailViewModel` 中的独立工作流，按“状态 owner/任务组/依赖闭包是否能一起迁走并实际减轻原 VM”决定拆分，不能只把函数搬到新文件或新建一一转发 UseCase。历史打开的全库初始化与当前条目 metadata 订阅顺序已迁入 `ObserveEntryRevisionMetadataUseCase`，ViewModel 保留页面订阅 generation、重试与 UI 失败投影；继续核对其余 Detail owner，并验证初始化失败重试、取消、会话锁定、metadata Flow 后续失败/重订阅及重复初始化幂等性。
- [ ] 评估 Detail favicon 编辑是否应从 `DetailViewModel` 提取为独立工作流 owner：当前 VM 同时发起 URI/URL staging、裁剪后图片处理、staged image promote、取消/过期结果回收及 `EntryEdit.SetIcon` 持久化；`DetailFaviconSession` 已拥有准备/保存 Job 与资源释放的一部分合同。按职责将图像处理/条目命令放在合适 UseCase，将裁剪输入、临时路径和页面生命周期放在 Feature 工作流 owner；不得把 Android/UI 会话状态塞进通用 UseCase，也不得拆出不能减少 ViewModel 依赖、分支或异步 owner 的薄包装。必须保持过期会话不回写、promote 后失败回收、取消/关闭时敏感与文件资源清理及提交结果一致性。
- [ ] 同一轮核对 Detail 的其余候选及最大 ViewModel：保留 `ActivityQueryRepository` 等简单只读 Flow 的页面订阅在 VM（包括异常态、重试 owner），已有 `EditEntryUseCase`、revision compare/reveal/restore、copy、QR 等完整动作不要再包一层；对 Backup、Credential Response、Diagnostics、Unlock 等大文件检查是否有跨端口业务决策或事务流程仍留在 VM，再依据证据决定提取或保留。只将有业务意图/不变量、复用价值、跨端口编排或独立取消/重试合同的流程归入专属 UseCase/工作流；UI 草稿、屏幕投影、Action 分发和生命周期任务仍由对应 Presentation owner 管理，防止造出上帝 UseCase。
- [ ] 对每个实际拆分补充 owner 级行为测试：验证旧 ViewModel 状态与依赖确有减少，UseCase 输入/结果不泄漏敏感值，并覆盖取消、失败重试、session/entry 替换后的迟到结果和资源收尾。同步必要的主题文档与本 TODO 证据；运行受影响测试、`verifyArchitecture` 与 `git diff --check`。本任务是待执行的维护审计与迁移方向，不表示所有大 ViewModel 都需要拆分。

**2026-11-08 修订历史订阅 owner 迁移（部分）：** `DetailViewModel` 已移除对 `EntryRevisionRepository` 初始化/观察的直接调用，改用 `ObserveEntryRevisionMetadataUseCase` 顺序执行历史初始化与当前条目 metadata 订阅；初始化失败不订阅，取消原样传播。UseCase 的顺序/失败/取消测试与 ViewModel 初始化失败后重试测试均通过，`DetailViewModelTest` 定向类通过。Task 83 仍开放：继续验证 session 锁定、替换后迟到结果及 Detail 其他 owner，再决定是否拆分 favicon 等职责。

**初始审计证据（2026-11-06）：** 静态盘点到 17 个 Repository 契约及对应 Hilt 绑定；暂未发现需要增补接口的证据。ViewModel 文件行数快照中 `DetailViewModel.kt` 为 1208 行，依赖同时包括两个 Repository、`EntryTagQuery`、`FaviconImageProcessor` 及多个既有 UseCase。已验证 `observeRevisionHistory()` 先直接调用 `entryRevisionRepository.initializeHistory()`，其 Data 实现会在事务中重建 revision history、安排 attachment GC，并进行 post-commit 图标/附件清理，属于应从大 ViewModel 归位的明确业务维护流程。Activity history 的 Repository Flow 收集是页面加载/错误投影，不据此要求抽 UseCase。Detail favicon 路径存在多个资源与生命周期阶段，已部分封装于 `DetailFaviconSession`，将其列为工作流边界审查而非预断必须全部迁入 UseCase。Backup（385 行）、CredentialResponse（364 行）、Diagnostics（350 行）、Unlock（344 行）只按快照列为扫描范围；尚未完成其业务职责判断。该段为 2026-11-06 静态审计历史；本轮构建/测试结果统一见扫描验证记录。

**修订历史调用闭包复核（2026-11-06）：** `openRevisionHistory()` 从 `_uiState.value.entry.id` 取 `entryId`，检查同条目历史已打开后取得 `workSession.current(entryId)`；随后取消旧的 `revisionObservationJob` / `revisionOperationJob`、清 reveal 并发布 `Revision.Opened(entryId)`，调用 `observeRevisionHistory(entryId, token)`。重试入口只接受当前 `DetailRevisionState.Open` 中 `page == List` 且 `revisions is Failed`，用同一 `state.entryId` 取当前 token、清 job、重发 Opened 后再次调用 `observeRevisionHistory(entryId, token)`。该函数增加 `revisionObservationGeneration`，在 `viewModelScope.launch` 中调用无参 `entryRevisionRepository.initializeHistory()`；`AppResult.Failure` 只有在 `workSession.isCurrent(token)` 且 generation 未变时才以 `mutateRevisionFailure(entryId, revisionId = null, Failed(error))` 投影，否则丢弃迟到结果；Success 才开始 `entryRevisionRepository.observeMetadata(entryId).collect`，每个列表仅在 token/generation 仍有效时转为 `Revision.MetadataChanged(entryId.value, revisions)`。取消重抛，`SessionLockedException` 由会话撤权路径清理，其他普通 `Exception` 在 owner 仍有效时映射为 `DetailRevisionFailure.UNEXPECTED`。Data `RoomEntryRevisionRepository.initializeHistory()` 无参数地委托 `historyInitializer.execute()`；该 initializer 先在 Mutex 内读取 `maintenanceStore.completedGeneration()`，若低于 `CURRENT_GENERATION = 1`，以 `databaseTransactions.write("entry_revision_history_initialize")` 执行 `rebuilder.rebuildInTransaction(db, clock.now())`；该重建器内部清空旧修订，再遍历未按 `deletedAt` 过滤的 `entryQueryDao().getAll()`（含回收站），并为每项复用同一个 `now` 调用 `EntryRevisionWriter.snapshotChanges(db, entryId, entryVersion, summary, secret, entryType, now)`；缺少 `STRUCT_BUNDLE` 时使用空 `EntrySecret``，对旧图标引用调用 `entryResourceCleaner.discardUnreferencedIcon()`，并 `attachmentGarbageCollector.scheduleInTransaction(db)`；transaction 成功后写 `maintenanceStore.markCompleted(1)`，再分别执行 `postCommitCleanup.run { attachmentGarbageCollector.drain() }` 与 `postCommitCleanup.run { entryResourceCleaner.drainPendingIcons() }`。因此跨端口的一次性初始化、数据库写事务和提交后资源维护不是简单读取，应评估移到具备启动/首次打开 owner 的应用流程；ViewModel 对 `entryId + token + generation` 的页面订阅、失败投影、重试和旧结果丢弃继续由页面 owner 持有。迁移需保留懒触发语义，并验证重建幂等、提交后 cleanup、锁定/取消及 metadata Flow 异常后的重订阅。

**2026-11-08 Vault 边界审查续记：** `DetailFaviconSession`、`DetailQrSession`、`DetailPasswordBindingWorkflow`、`DetailWorkSession` 均实际拥有任务、资源或访问页生命周期，保留；revision restore 与 Detail Reducer、OTP runtime、根快照刷新共享当前 visit token，暂留 Detail owner。历史初始化后订阅 metadata 的协议已进 `ObserveEntryRevisionMetadataUseCase`，并补了顺序/失败/取消测试。原记录中的 Settings 编译错误和测试阻塞均为短暂工作区状态；之后修复了过期的 `PasswordStrengthUiStateTest` 调用，Vault 全量 472 项单测、AndroidTest Kotlin 编译、`verifyArchitecture` 与 `git diff --check` 均通过。Vault 本轮职责审计与调用闭包完成，但 Task 83 的跨 Feature 维护审计仍未完成，勿将此局部证据视作整个 Task 83 完成。详见 [`2026-11-08-vault-presentation-boundaries.md`](2026-11-08-vault-presentation-boundaries.md)。

<a id="task-84"></a>

### Task 84：全量审计 ViewModel 与 Coordinator 的职责归属并整理分层规范

- [ ] 对当前 `:app`、`:security`、`:feature:autofill` 中 34 个生产 ViewModel 逐个完成调用闭包审计，输出保留/迁移/合并/继续调查及其证据，不按行数或类名自动判断。清点范围：Shell/宿主（`AppShellViewModel`、`AppThemeSettingsViewModel`、`AppNoticeHostViewModel` 属于 App，`AuthenticationHostOwnerViewModel` 属于 Security）；Vault（`VaultViewModel`、`VaultDisplayViewModel`、`TrashViewModel`、`DetailViewModel`、`ScannedOtpImportViewModel`、四种 `Add*ViewModel`）；Settings/Backup（`AutofillSettingsViewModel`、`SecuritySettingsViewModel`、`RecoveryCodeSettingsViewModel`、`PrivacySettingsViewModel`、`AppPasswordSettingsViewModel`、`InterfaceSettingsViewModel`、`AppearanceSettingsViewModel`、`PermissionManagementViewModel`、`InteractionSettingsViewModel`、`NotificationSettingsViewModel`、`DiagnosticsViewModel`、`DataManagementSettingsViewModel`、`StorageSettingsViewModel`、`BackupViewModel`）；Autofill 工作流（`AutofillFillViewModel`、`CredentialResponseViewModel`、`AccessibilityFillViewModel`，均属于 `:feature:autofill`）；Scanner/Unlock/Recovery/Database（`ScannerViewModel`、`UnlockViewModel`、`RecoveryModeViewModel`、`DatabaseResetViewModel`）。同时覆盖每页 Route/Activity/Content 的实际消费者、DI 绑定、相关测试及下面被调用的 UseCase/Interactor/Repository/Coordinator/Store，而非只看 ViewModel 文件。
- [ ] 将执行职责从页面状态协调中归位：已确认的 Detail 修订历史初始化候选引用 Task 83，避免重复建项；重点审查 `RecoveryCodeSettingsViewModel.confirmAndEnable()` 与 `reconcileInterruptedCommit()` 中的提交后事实核对、`NonCancellable` 恢复、取消与 fatal `Error` 分支。这些负责 durable commit 协调的流程应由 `RecoveryCodeDraftInteractor` 或 Security 应用操作 owner 统一定义结果；ViewModel 保留页面状态、披露草稿和 owner 有效性投影。任何迁移都须保持 commit 前失败、持久化后异常、取消、fatal `Error`、离页与敏感值擦除合同，并通过 ViewModel/Interactor 行为测试证明状态不会错误回滚或允许重复提交。
- [ ] 审查 `StorageSettingsViewModel` 同时直接协调 cache 清理/大小读取、诊断日志清理/大小读取和 log-recording 偏好写入的边界：分别核对 `AppCacheStore`、`DiagnosticsLogStore`、`DiagnosticsLogSettingsRepository` 的所属层和所有消费者，区分简单单仓库读写、跨能力业务动作、格式化展示和页面错误状态；只有行为契约/事务/复用确有 owner 时才提取 UseCase 或应用服务，并让刷新/清理任务的取消、锁定、失败反馈仍由一个清楚的页面或工作流 owner 管理。
- [ ] 审查 `AppShellViewModel` 对外观/界面/安全设置流、认证状态、session lock 及 database failure/retry 的组合是否仍是 Shell 页面所需的屏幕投影和生命周期 owner。沿 `DatabaseSessionRecovery`、`DatabaseSessionRetryCoordinator`、`RecoverableRead` 到具体执行端检查是否有重复 retry 状态机、重复事实副本或可独立取消的子 owner；保留合法的应用 Shell 聚合，不为了减少构造器参数把状态拆进一对一转发 Coordinator。
- [ ] 对其余大/多流程候选逐项判断：`VaultViewModel` 的 Paging 查询、设置读取、OTP runtime 和条目命令；Backup 文档选择与备份操作；Credential Manager 的系统请求/结果交付与业务解析；Legacy/Accessibility Autofill 的授权、续接、选择和平台结果；Diagnostics 导出、清理与权限；Unlock 的认证和 App Password provision；Scanner 的相机/图片输入与解码；Recovery Mode 的凭据设置；通知运行时权限的请求 owner。[Autofill 后续清单](2026-11-07-passly-autofill-followups.md)、Task 80 的统一架构审计和 Task 83 的 Detail 项应引用并更新状态，不重复复制缺陷清单；分支映射仅在同一 Presentation owner 确有重复、纯显示元数据时考虑合并。
- [ ] 审查分散在层外/文件外的 Coordinator、Session、Store 和 helper：至少覆盖 `DatabaseSessionRetryCoordinator`、`SecureDatabaseResetCoordinator`、`EntryEditorSeedCoordinator`、`AppPasswordDialogCoordinator`、`PermissionRequestSession`、`AccessibilityFillSessionStore`、`DetailWorkSession`、`DetailQrSession`、`DetailFaviconSession`、`DetailRevealStore`、`DetailRevisionRevealStore`、`EditorWriteSession`、`ScannerCameraHost`、`ScannerResultSession`、`RecoverableRead` 与 `PageEffects`。为每项记录真实创建者、生命周期、共享者、状态真相源及所处 Gradle module；合并重复机制或移除纯桥接前，证明其职责未被配置重建、Route、系统 callback、认证/锁定 owner 或其他调用方需要。已确认四种编辑器共用 `EditorWriteSession`，七处 Settings `saveSetting` 共用 `writePreferenceWithFeedback`：审计为保留/扩展现有共享逻辑，不复制到新 Coordinator；`AppShell` 等用例须按调用图核实。
- [ ] 对每个 ViewModel 依赖分类并验证层边界：可观察的 Domain/Repository 数据、完整业务动作 UseCase/Interactor、窄应用端口、Presentation 状态 owner、平台/持久化实现。继续禁止 ViewModel 间依赖及直接 DAO/Room/Data Entity/具体文件实现；明确哪些 `feature.*.data` 或 `feature.*.internal` 类型实际是跨层实现泄漏，避免仅按 `internal` 包名搬移。核查 `verifyArchitecture` 当前 ViewModel 门禁与 `ViewModelDependencyVerifier` 的覆盖范围；若新增静态边界，只针对已证实错误依赖设计白名单/反例测试，不把合法 Repository Flow 或窄业务端口误禁。
- [ ] 将本轮的稳定规则同步到 `docs/architecture/viewmodel-ownership.md`、`docs/architecture/package-boundaries.md`、`docs/development/maintenance-boundaries.md` 及必要功能文档，并使本 TODO 保留未迁移清单与决策证据。迁移需要分批按 Feature 执行；每次必须减少原 ViewModel 的独立业务职责、状态、异步任务组或实现依赖，而不是只变短文件。覆盖取消/重试/离页迟到结果/配置重建/会话锁定/持久提交点/敏感清理；运行对应 ViewModel/Route/宿主测试、相关模块门禁、`verifyArchitecture` 和 `git diff --check`。

**全量清点证据（2026-11-06，工作区快照）：** 按生产 `src/main` 清点到 34 个 ViewModel；`buildSrc/ViewModelDependencyVerifier.kt` 是架构检查器，不计作生产 ViewModel。文件长度范围为 8–1208 行；仅行数用于筛查，不作为拆分依据。代码摘要覆盖了每个 ViewModel 的构造依赖、公开/私有方法、`launch`/`collect` 与分支密度；并对 Shell、Storage Settings、Recovery Code、Unlock、Detail 及共享 Editor/Settings helper 做了方法级复核。初步确认 Recovery Code ViewModel 仍直接执行 durable commit 异常/取消/fatal 恢复协调；Storage Settings 同时操作 cache、diagnostics log 与记录模式；Shell 聚合设置、session 和 database retry 流；这些是优先复核项，是否迁层按实际 owner 合同完成闭包审计后决定。多个简单 Settings ViewModel、只投影配置的 Theme/Vault Display ViewModel、扫描导入 ViewModel 使用窄数据源/UseCase；四类编辑器已共享 `EditorWriteSession`，七个设置保存入口共用 `writePreferenceWithFeedback`，这些共享职责暂列保留核实，不提出机械新增层。当前 `viewmodel-ownership.md` 规定不得 ViewModel 相互依赖及不得直接依赖 DAO/Room/文件实现，`verifyArchitecture` 的 ViewModel 检查定位于跨 ViewModel 依赖；层间实现依赖和具体工作流归属仍需本任务逐页核对。此为全量生产文件清点与候选筛查，尚未逐项完成所有 Route/DI/测试的调用闭包验证，也未运行构建或测试。

**2026-11-07 Vault Paging 旧查询路径清理：** 生产只使用 `buildRecoverableVaultQueryStates()` 与 `switchRecoverableQueryGenerations()`；移除未被调用的 `buildVaultQueryStates()`、`switchQueryGenerations()`，将搜索/切换用例迁到实际生产 helper。红灯回归发现层级模式只对“全部”查询有效，但旧逻辑先按 `VaultQueryState` 去重，导致密码筛选下重启同一个 `EntryListQuery` 对应的 Pager；现改为先映射查询、再按 `EntryListQuery` 去重。定向红灯复现后 `VaultQueryStateTest` 8/8 通过；Task 84 的 VaultViewModel 行为审计仍待完成。

<a id="task-80"></a>

### Task 80：统一 Presentation 的 MVVM 与单向数据流契约

- [ ] 全量盘点 `:app` 的 Route、ViewModel、UI state/action/effect、Reducer、Repository Flow 与 Compose state holder，按真实状态 owner 和任务生命周期分类；至少覆盖设置、列表/分页、详情、编辑器、备份、扫描、认证/恢复、权限与平台宿主。记录每页当前模式、依赖闭包、异步任务组、取消/重试与 effect 交付合同，标明保留、迁移或需进一步调查及证据；不要按文件名、分支数或是否存在 `Reducer` 推断架构。
- [x] 正式规范已统一为一套架构契约：分层 MVVM + 单向数据流（UDF）。ViewModel 是屏幕业务状态 owner，暴露不可变、可观察的屏幕 `UiState` 并接收 UI 操作；Repository Flow、异步操作结果和离散用户输入都是状态生产过程的输入，不是两种不同架构。语义方法或类型化 `UiAction` 按表达清晰度选择；Reducer 仅是复杂转换时可选的纯实现，Effect 也只是受生命周期/owner 约束的宿主交互出口。静态 Content 与短暂视觉状态无需 ViewModel，不构成另一套架构。
- [ ] 明确单一数据真相源合同并审查潜在双份状态：持久业务数据以 Repository/Domain 为真相源，ViewModel 的屏幕状态是其可丢弃投影；用户编辑草稿、筛选输入、展开状态等可以是独立的屏幕/交互状态，但须有明确 owner、生命周期和提交/丢弃语义。排查从 Repository 读取后另存一份可独立修改的长期副本、Flow 与手动赋值竞争同一字段、多个 ViewModel/Compose state 同时声称拥有同一状态等情形；区分有意未提交草稿与意外镜像，禁止把 Action 另做一条并行业务状态真相。保留 Paging 等确有独立语义的输出，不为形式统一强塞到一个 State，也不引入全局 EventBus。
- [ ] 按统一契约审查所有页面并制定分批迁移清单：合并重复的 Action/状态/事件转发与纯桥接；移除没有带来明确状态转换保证或可测试性的 MVI ceremony；让真实多输入/异步工作流仍按 UDF 汇入屏幕状态，必要时用纯 Reducer。只在能明确减少职责、重复或依赖时改代码；不得以统一为由制造上帝 ViewModel、万能 Reducer/Effect handler、全局状态容器或新的转发层。复杂状态机、认证提交顺序、协议分支、敏感数据清理与平台生命周期合同不得表驱动化抹平。
- [ ] 对所有 ViewModel 做职责密度与依赖闭包审计，标出同时拥有多个独立业务流程/状态机、宽依赖集合、重复决策映射或不同取消/重试任务组的候选；按[ViewModel owner 边界](../../architecture/viewmodel-ownership.md)和[维护边界](../../development/maintenance-boundaries.md)，结合调用方、状态 owner、事务/安全边界决定保留或拆分，不能仅按行数拆。
- [ ] 为每个拆分候选确定逻辑 owner：纯且可复用的业务判断归 Domain Policy/规则；完整业务动作及验证、不变量、可复用流程或事务/跨端口编排归职责匹配的 UseCase/应用服务；简单单一 Repository CRUD 保持直接调用，不造一对一转发 UseCase；平台生命周期和系统 API 留在 Route/Activity。UseCase 也须单一职责，提取后验证原 ViewModel 的依赖、分支或异步职责实际减少。
- [ ] 对跨层流程追踪 `Presentation → Application/UseCase → Domain Port/Policy → Repository → Data` 的真实依赖方向，检查错误/取消/重试、durable commit、认证、敏感数据清理和调用方闭包；不要把规则搬进 ViewModel，也不要把大 ViewModel 原样搬进 God UseCase。拆分异步 owner 时增加 VM/Route/宿主行为覆盖，不以 Reducer 或纯 UseCase 单测代替。
- [ ] 逐页迁移确认实际合同变化后，按需更新正式文档代码示例与例外；统一术语和基础规则已完成，不再重复立项。
- [ ] 按 feature/owner 分阶段实施迁移；异步编排或状态 owner 变化须有 ViewModel/Route/宿主行为覆盖，验证 Flow 失败与重订阅、CancellationException/fatal Error、离页迟到结果、配置重建、Effect 前后台交付、Repository 提交后状态与敏感值擦除。保留无需迁移且符合模板的页面，并记录理由；每阶段运行受影响模块测试、`verifyArchitecture`、相关 lint/编译门禁与 `git diff --check`。当前 Detail/完整表单编辑器存在未提交 owner 重构，先以稳定后的代码为基线，保留并适配工作区改动。

**2026-11-07：retained，仅剩逐页审计/迁移。** architecture-and-state.md 已明确分层 MVVM + UDF、Repository 真相源、可选 Reducer 和 UseCase owner；viewmodel-ownership.md 已有对应边界。移除“正式规范仍分四套架构”的过期事实。Task 84 持有 ViewModel/Coordinator 调用闭包清单，Task 83 持有具体 Detail 候选，本 Task 只管状态生产、真相源及 action/effect 模式，不复制 owner 清单。

<a id="task-39"></a>

### Task 39：拆开诊断运行时生命周期与设置日志读取适配

- [x] 收窄 `ObservabilityRuntimeController` 的所有权：保留进程级 crash handler 安装/恢复、日志策略监听、上一进程退出记录、生命周期 flush 与 shutdown；让独立的 app 层适配器直接实现 `DiagnosticsLogStore`，负责 `LogRecordStore` 的行读取/编码、清理和存储大小查询。适配器应直接持有必要端口，不要再经控制器加一层纯转发。
- [x] 让 `DiagnosticsLogStore` 的 feature-facing `DiagnosticsLogLine` 转换留在该端口的适配边界，不再让进程运行时控制器同时成为 Settings 功能存储实现。DI 继续共享同一个受策略包装的 `LogRecordStore`；验证功能依赖不会绕过记录策略或获得未经脱敏的原始记录。
- [x] 新增只读 `DiagnosticsLogSettingsSource`，由 `DiagnosticsLogSettingsRepository` 扩展，并在同一 `ProtoAppPreferencesStore` 上绑定；将 runtime controller 改为依赖只读 source，保留 `StorageSettingsViewModel` 通过 repository 写模式。当前它是设置 Source/Repository 分离族中唯一缺少 read-only port 的能力，运行时不应获得未使用的设置写权限。
- [x] 将覆盖分成真实职责：运行时控制器测试继续验证 crash handler、启动监听、flush/shutdown 次序；新增 store adapter 测试验证限量分页、TSV 编码字段、清理结果和 storageBytes 直传；保留 Storage/Diagnostics ViewModel 对 `DiagnosticsLogStore` 的替身测试，并运行 `verifyArchitecture`、相关定向测试与 `git diff --check`。若拆分后新增的对象只是把方法逐个委托给控制器、未减少双方依赖或职责，则不采用该拆分。

**证据：** `ObservabilityRuntimeController` 同时实现 `DiagnosticsLogStore`，并由 `PasslyApplication` 调用 `start()`、由 `AppLifecycleObserver` 调用 `flush()`/`shutdown()`；同一实例因此跨进程生命周期与 Settings 读取/清理/容量查询两类调用方。它唯一读取 `DiagnosticsLogSettingsRepository.diagnosticsLogRecordingMode`，不调用 setter；`StorageSettingsViewModel` 则同时读写该 Repository。与 Interaction、Message、Security、Library View、Backup Directory 已有 `*Source` + Repository 的端口分离对照，Diagnostics 缺少只读 source 使 runtime 注入了未用写能力。该控制器还把 `SanitizedLogRecord` 转换成 `feature.settings.diagnostics.DiagnosticsLogLine`，承担 feature 合同的展示 DTO 编码，同时持有 `Logger`、crash capture、previous-exit source、crash handler registry、process terminator、event aggregator、设置 repository 和 recording policy。Hilt 将其直接绑定为 `DiagnosticsLogStore`。现有 `ObservabilityRuntimeControllerTest` 主要覆盖 handler 安装、上一进程记录、crash、flush 与 shutdown；Settings ViewModel 测试使用独立 store 替身，没有独立验证生产 adapter 的分页/编码/清理实现。该耦合没有证据表明造成运行时错误；本项是按实际端口调用闭包收窄职责并固定 adapter 行为，实施时必须保持同一个策略包装 store 和 crash capture 顺序，不按类长度机械拆分。

**2026-11-08 实施与验证：** 新增 `DiagnosticsLogStoreAdapter`，直接从 DI 获取同一 `@Singleton LogRecordStore` 策略包装实例，持有分页、TSV 映射、clear 与容量查询；`ObservabilityRuntimeController` 只保留进程生命周期/策略监听职责。`DiagnosticsLogSettingsRepository` 扩展只读 `DiagnosticsLogSettingsSource`，Proto store 同一实例分别绑定读写端口。新增 adapter 用例覆盖 500/1 分页、行字段/TSV 编码、clear 结果与 storageBytes；controller 11/11、adapter 3/3 通过，强制 App 全量 JVM 1089/1089 通过，`verifyArchitecture` 与本批 `git diff --check` 通过。

<a id="task-35"></a>

### Task 35：按真实消费收窄 EntryLinkRepository 与详情关系查询

- [x] 按 [Detail 密码绑定设计](../specs/2026-11-07-related-password-binding-design.md) 实现已保存 LOGIN 条目之间的 `RELATED_TO` 绑定。详情页仅在活动 LOGIN 项显示绑定入口；候选是可搜索的非敏感标题/用户名摘要。
- [x] 候选只通过 `EntryListItem` 查询活动 LOGIN 条目；写入前重验两端存在且未删除，Room link transaction 再次原子校验端点活动状态、保留修订快照/GC，并按 `RELATED_TO` 对称合同唯一写入。没有新增兼容分支或 fallback。
- [x] 保留 `DetailSessionQuery` 的全量 link 图查询：`EntryAccountGraph` 需要跨 MEMBER_OF_ACCOUNT、OTP_FOR、RECOVERY_FOR 闭包找同组成员，直接按 entry 查询会漏项；详情结果现在再并入直接 `RELATED_TO` 邻项。
- [x] 收敛关联条目加载的 N+1 查询：Domain `EntryQueryRepository.getByIds()` 复用 `EntryQueryDao.getByIds()`，空输入不打开 session，Room 在一次可读 session 中按输入顺序装配结果，忽略查询时已不存在的 ID；详情关系一次批量装载。
- [x] 保留关系 CRUD、Domain link model/policy、历史快照、Backup 稳定关系键和 `EntryLinkCommandDao`；绑定与解绑均复用既有持久化合同，Room schema/internal format 未改。
- [x] 关系管理实现：按[详情页密码关系设计](../specs/2026-11-08-related-password-management-design.md)与[实施计划](2026-11-08-related-password-management.md)拆分账户组/其他关系与活动 LOGIN↔LOGIN `RELATED_TO` 投影；完成绑定、左滑确认解绑、双语 UI、错误日志、失败重试、会话/页面生命周期守卫和解绑后详情刷新。解绑复用现有 `EntryLinkRepository.delete(linkId)`，只删除指定 link，保留两个条目及现有事务快照/清理；数据库结构、Repository 契约和备份格式未改。分批提交：`0b144c032`、`22453f02c`、`73646d530`、`62a9c1305`。
- [x] 在隔离的 Android 36 x86_64 临时 AVD 上运行 `RelatedEntriesSectionTest` 与 `RelatedPasswordPickerSheetTest`，两类均通过。Picker 测试发现并修正同一用例重复调用 `setContent` 的问题，现以单次 composition 更新状态验证空候选和无搜索匹配。

**2026-11-07 实现证据：** 批量关系解析测试已通过；详情绑定 ViewModel/UI 测试在临时隔离一份未提交的权限测试文件后编译，56 个定向测试中 55 个通过，唯一失败是新混合关系测试 fixture 把 LOGIN 与 NONE 凭据组合，已修正为合法 LOGIN/OTP 凭据。之后重跑被工作区持续变化中的权限设置代码阻塞：`PermissionsRoute`/`PermissionManagementScreen` 引用了缺失的 `settings_permissions_*` 资源。Data unit tests、`:app:compileDebugKotlin`、`verifyArchitecture`、`git diff --check` 在权限改动进入该次构建前通过；权限设置文件均保留原样。修正后的混合关系测试待阻塞解除后重跑。

**实现前审查证据（2026-11-06）：** 当时生产调用只有 `DetailSessionQuery.getAll()`；Repository 的 `getLinks()`、`upsert()`、`delete()` 与两种观察方法没有 App consumer。`getAll()` 将全部 links 交给 `EntryAccountGraph` 计算 ACCOUNT 分组，不能直接改为按单个 entry 查询，否则 OTP_FOR 会漏掉该账户其他 login/recovery/OTP 成员。Detail 随后逐个 `getById()` 读取关联项，闲置 `EntryQueryDao.getByIds()` 并为每项重新获取数据库 session。`RoomObservationSessionTest` 用 `EntryLinkQueryDao.observeAll()` 检查通用 session gate，删除 DAO API 前需保留此覆盖。此段只记录改动前快照，不代表当前消费状态。

**实现后消费闭包（2026-11-08）：** `DetailSessionQuery` 仍保留 `getAll()` 图查询供 `EntryAccountGraph` 计算账户组，并在同一 link snapshot 上区分账户组、其他关系和可解绑的 LOGIN↔LOGIN `RELATED_TO` link。`RelatedPasswordLinks` 消费 `getLinks()`、`upsert()` 与 `delete(linkId)`；`delete()` 现在由确认解绑流程实际调用，继续复用现有 Room 事务、端点修订快照和附件清理。生产 App 当前未消费 `observeAll()` / `observeLinks()`。候选仍来自活动 LOGIN 的非敏感 `EntryListItem` 批量查询；详情目标通过 `getByIds()` 一次装配。没有兼容分支、Schema 变更或备份格式变更。

**2026-11-08 详情密码关系实现与验证：** Task 1-4 分别提交为 `0b144c032`、`22453f02c`、`73646d530`、`62a9c1305`。Task 1 的 80 项 query/loader/reducer/UI mapper/绑定工作流定向单测、Task 2 的 46 项 links/workflow/ViewModel 定向单测、Task 3 的 66 项 workflow/ViewModel/reducer 定向单测通过；Task 4 的 `DetailUiMapperTest` 通过，`:app:compileDebugKotlin` 通过。Android 测试源码编译曾在临时补上工作区另一处 `VaultTopBarMenuTest` 缺失的 `VaultCategoriesState` import 后通过，临时改动已撤销；当前工作树该顶栏测试仍有此独立编译问题。`adb devices -l` 无设备且无已配置 AVD，因此两组 Compose instrumentation 尚未运行。Room upsert 端点在事务写入期间删除的竞态仍作为独立验收项保留，不因 unlink 的事务复用而关闭。

<a id="task-34"></a>

### Task 34：收窄未消费的 Activity 查询流

- [x] 从 `ActivityQueryRepository` 移除没有生产消费者的 `observeAll()` 与 `observeByType(activityType)`，并同步删除 `RoomActivityQueryRepository` 实现和 Detail 测试替身中的空实现；`EntryActivityQueryDao` 继续只保留真实消费路径。
- [x] 删除无调用者的 `EntryActivityAnalyticsDao`、`AppDatabase.entryActivityAnalyticsDao()` accessor 和其局部 DTO。引用闭包只到 DAO 声明及 accessor；列表 usage stats 由 `EntryPagingQueryFactory` 的 Paging SQL 提供。保留 `RoomObservationSessionTest` 使用的 Activity/Link `observeAll()` session gate 查询；这次不扩展到其余 Activity DAO 方法。
- [x] 清理未消费的 Activity DAO 查询。符号闭包确认无反射、Hilt 多绑定或生成消费者；将 Room session gate 集成用例从全表观察迁移到生产用的按条目观察。保留修订恢复对 `getByEntryId()`、`countByEntryId()`、`countByEntryIdAndType()` 的行为断言，不制造符号不存在测试。
- [ ] 在可用设备上运行 `RoomObservationSessionTest` 与 `EntryUpdateAndRevisionRestoreTransactionTest`；当前未连接 Android 设备，instrumentation runtime 尚未验证。

**证据：** `ActivityQueryRepository.observeByEntryId()` 唯一生产调用位于 `DetailViewModel`，支持详情活动记录；`observeAll()` 与 `observeByType()` 没有生产 consumer。全仓搜索 `EntryActivityQueryDao` 的方法后，确认 usage/mutation 四类观察、四个 paging、`getById()`、`exists()`、`count()` 和两个 usage/mutation counts 只在 DAO 声明。`observeAll()` 由 `RoomObservationSessionTest` 用作 session gate 集成测试入口；`getByEntryId()`、`countByEntryId()`、`countByEntryIdAndType()` 在 `EntryRevisionRestoreTransactionTest` 中验证恢复行为，需要保留覆盖。`EntryActivityAnalyticsDao` 的四个聚合查询只在 DAO 声明及 `AppDatabase` accessor 出现，没有生产调用者；列表 usageCount/lastUsedAt 实际由 `EntryPagingQueryFactory` 内联 SQL 生成。现有 `DetailViewModelTest` fake 实现全量/类型方法仅为满足接口，没有行为断言；未发现正式文档声明这些额外 API 合同。

**2026-11-08 批次：** 删除 `ActivityQueryRepository.observeAll()` / `observeByType()`、Room adapter 对应实现及 DetailViewModelTest 的空 fake 方法。当前生产消费者只调用 `observeByEntryId()`；全仓引用扫描确认剩余接口引用仅为其生产注入与测试 fixture。未删除 `EntryActivityQueryDao.observeAll()`，因为 Room session gate 仪器测试直接覆盖该查询；其余 DAO 收敛留待下一批。此批次尚未运行编译或测试。

**2026-11-08 批次：** 删除未被引用的 `EntryActivityAnalyticsDao`、`AppDatabase` accessor 和聚合结果 DTO；精确符号扫描只找到 DAO 声明及 accessor，未发现 Kotlin/Java consumer。`EntryPagingQueryFactory` 已承担列表 usageCount/lastUsedAt 查询。未改数据库实体或 schema；未运行测试。

**2026-11-08 批次：** 从 `EntryActivityQueryDao` 删除全表/按类型与 usage/mutation 观察、全部 paging、单项 ID、exists、总数及分类 count 等无消费者方法；保留生产详情流和 revision restore 仪器测试消费的三个精确查询。session gate 用例改用 `observeByEntryId("entry-a")`，保留锁定/解锁订阅行为。`:data:compileDebugKotlin`、`:data:compileDebugAndroidTestKotlin`（61 项任务执行）、`verifyArchitecture`、目标文件 `git diff --check` 通过；全仓扫描未找到被删方法调用。`adb devices` 无设备，未运行 instrumentation tests。

<a id="task-36"></a>

### Task 36：按生产消费闭包收敛修订 Room DAO

- [x] 移除没有调用者的 `EntryRevisionQueryDao.getByVersion()`、`getByEntryId()`、`getLatest()`、`countAll()`；测试改为消费生产 `observeByEntryId()` Flow，并继续断言历史顺序、总量和恢复后最新快照。
- [x] 移除无消费者的 `EntryRevisionCommandDao.upsertForImport()`、`upsertAllForImport()`、`deleteBefore(timestamp)` 与测试专用 `insertAllStrict()`；生命周期测试改为逐条 `insertStrict()`。完整备份合同排除修订历史。
- [x] 保留有实际事务消费者的方法：普通修订写入、恢复、删除、按条目/全局裁剪、清空回收站修订和旧图标引用回收；保留 `countByIconCustomReference()` 等图标引用保护查询。未改 Room Schema/internal format 版本 1，也未把修订持久化职责搬到 App/Presentation。
- [x] 将 `RoomObservationSessionTest`、EntryRevision restore/history rebuilder、历史裁剪和图标引用回收断言保留并迁移到生产读取路径；内部 DAO API 收敛不改变正式修订历史文档合同。Data 主源码、AndroidTest 源码编译、`verifyArchitecture`、`git diff --check` 通过。
- [ ] 在可用设备上运行受影响的 Data instrumentation tests；当前无连接设备，运行时数据库行为尚未验证。

**证据：** 当前生产 Repository 通过 `observeByEntryId()` 发布修订元数据，通过 `getById()` 加载、揭示和恢复目标修订；`EntryRevisionWriter`、永久删除、清空回收站与图标回收路径消费其余写入、裁剪及图标引用查询。`getByEntryId()`、`getLatest()`、`countAll()` 和 `insertAllStrict()` 仅被仪器测试使用，现已改用生产 Flow 和逐条严格插入；Import upsert 与 `deleteBefore()` 无调用点，且 [`docs/features/backup/backup-workflows.md`](../../features/backup/backup-workflows.md) 明确完整备份不含修订历史。更新保留历史上限、恢复后最新快照、清空回收站及附件/图标引用保护行为断言。

**2026-11-08 批次：** 删除修订 DAO 中无消费者的查询、Import 写入和过期清理方法；把所有测试专用 get/count/batch insert 调用迁到 `observeByEntryId()` 与逐条 `insertStrict()`，保留行为断言。未改表结构或 schema。`:data:compileDebugKotlin`、`:data:compileDebugAndroidTestKotlin`、`verifyArchitecture` 通过；目标文件 `git diff --check` 通过；instrumentation tests 因当前无连接设备未运行。

<a id="task-78"></a>

### Task 78：让完整表单可编辑能力查询不伪造 seed token 参数

- [x] 将 `DetailUiMapper.detailHeaderUiModel()` 对可编辑类型的探测从 `editRouteFor(entry.type, entry.id.value) != null` 改为只接受 `EntryType` 的能力查询，或采用同样不会要求虚构路由参数的类型注册表接口。确认详情展示与实际导航仍由同一组支持类型决定，且不生成/消费 seed、不改变路由字符串。
- [x] 核对 `VaultEditorDestinationRegistry` 是否还存在其他只为探测 capability 而传入占位 `seedToken` 的调用；保留真正创建路由时的 `editRouteFor(type, token)`。增加针对四种受支持类型和所有不支持类型的映射覆盖后运行相关 Detail UI Mapper 测试、`verifyArchitecture` 与 `git diff --check`。

**证据（2026-11-06 源码复核）：** `toDetailPresentationModel(state, otp, revisionReveals)` 调用 `detailHeaderUiModel(entry, state)`；后者只需要 Boolean `canEditFullForm`，却以 `VaultEditorDestinationRegistry.editRouteFor(entry.type, entry.id.value) != null` 探测能力并丢弃返回的 route。注册表签名 `editRouteFor(type: EntryType, seedToken: String): String?` 对 LOGIN/OTP/BANK_CARD/WIFI 分别调用 `AppRoute.EditPassword/EditOtp/EditBankCard/EditWifi.createRoute(seedToken)`，其余 EntryType 返回 `null`，因此 `entry.id.value` 被放在 route 的 seedToken 槽位；当时的调用不导航、不创建或消费 seed，能力结果实际只由 type 分支决定。唯一另一处生产消费者在 `VaultGraphRegistration` 的真实导航闭包：先以 `seedCoordinator.stage(editableEntry)` 获取 `staged.token.value`，再调用 `editRouteFor(editableEntry.entry.type, staged.token.value)`，route 为 null 时执行 `seedCoordinator.discard(staged.token)`，否则 `navigateToRoute(route)`。当时全仓检索未发现其它占位参数探测；测试也未覆盖详情能力投影。此项属于路由构造的多余依赖与参数语义误用，而非已证实的用户可见故障；整改与验证见下方完成记录。

**完成（2026-11-07）：** `VaultEditorDestinationRegistry` 现在以唯一的 `EntryType -> route factory` 注册表同时回答 `supportsFullFormEdit(type)` 和用真实 staged token 构造路由；`DetailUiMapper` 不再把 entry ID 塞进 `seedToken` 参数，也不生成并丢弃 route。实际导航仍使用 `editRouteFor(type, staged.token.value)`。全量 `EntryType` 覆盖确认 capability 与 route 一致，路由字符串原样断言保留；Detail mapper 覆盖支持/不支持投影。定向 `VaultEditorDestinationRegistryTest` + `DetailUiMapperTest`、`verifyArchitecture` 和 `git diff --check` 通过。

<a id="task-79"></a>

### Task 79：移除二维码会话仅供旧调用签名使用的授权回调桥

- [x] 移除仅由测试调用的次级构造器 `(Entry) -> EntryQrExportResult`；生产 `DetailViewModel` 原本已使用带 `onAuthorized` 的主构造器，没有需要兼容的生产 owner。
- [x] 将 `DetailQrSessionTest` 全部迁到主构造器并显式控制授权回调，覆盖授权前 Authorizing、授权后 Loading、拒绝/取消隐藏、generation 过期 payload 擦除及锁定/关闭清理；不更改 `ExportEntryQrUseCase.invoke(entry, onAuthorized)` 的授权边界。

**2026-11-07 完成：** 生产调用唯一一处 `DetailViewModel` 已使用显式授权回调；全仓生产引用没有单参数 consumer。新增状态回归用例在旧构造器下先实际失败（授权尚未回调就进入 Loading），再将 10 个 session 用例统一迁到双参数主构造器并删除次级构造器。定向 `DetailQrSessionTest` 10 项通过；完整 App 测试显式按 `com.aozijx.passly.*` 执行，XML 汇总 992 项、0 失败、0 error。第一次 full-task 汇总曾被并行定向测试的 11 项报告覆盖，因此不作为全套证据；最终验证使用显式包过滤并在同一命令结束时汇总 XML。


<a id="task-74"></a>

### Task 74：按测试对象优化单测结构与重复装配

- [x] 拆分 `SourceBoundaryPolicyTest.kt`：原 70 例按 package/layer、reference scanner、module dependency、presentation、module ownership 拆成 5 个测试类/文件，最大文件 495 行。按方法名逐项与 HEAD 原文件比对，70/70 方法均保留且 body 完全相同，未增删断言；`uiSource` 移至同 package test helper，`LayerCase` 留在 layer policy owner。静态 `git diff --check` 通过；未运行测试。
- [x] 拆分 `VaultSessionControllerLifecycleTest.kt`：锁/重置及取消清理保留 19 例，后台超时移至 `VaultSessionControllerBackgroundLockTest`（4 例），Inbox key provisioning 移至 `VaultSessionControllerInboxProvisioningTest`（1 例）；共享 `runMainTest`/dispatcher 与 fixtures 收敛到同包 `VaultSessionControllerTestSupport.kt`。对照 HEAD 逐项比对 24/24 方法名及方法体，均一致（含既有取消断言语义调整）；静态 `git diff --check` 通过，未运行测试。
- [x] 按 owner 拆分 `app/src/test/java/com/aozijx/passly/feature/backup/internal/archive/BackupCodecTest.kt`：当前文件为 134 行、4 个 ZIP/encrypted-archive 合同；容器头/分块加密、JSON streaming、Keyed mapper round-trip、registry/validator、Bitwarden adapter 各有独立测试 owner（含 `BackupContainerHeaderTest`、`ChunkedBackupStreamsTest`、`JsonBackupStreamTest`、`KeyedEntryArchiveRoundTripTest`、`BackupArchiveKeyRegistryTest`、`BackupBundleValidatorKeyedTest` 与 Bitwarden tests）。提交 `9c08fe979` 的结构差异显示旧 815 行文件拆出 10 个路径，测试断言迁移后未发现同一 owner 的重复组；LimitedInputStream 边界用例现归入专属测试；本轮未运行测试。
- [ ] 在 Detail 生产 owner 重构完成后重新评估 `DetailViewModelTest.kt`（当前 1,570 行、28 个 `@Test`，工作树有未提交改动）：其用例组覆盖 package picker 失败/取消/迟到结果、Activity/Revision Repository Flow 异常与取消、restore 提交后离页/换条目的迟到结果，单文件私有 fixture 同时手写 `MutableEntries`、`BlockingRevisionRepository`、敏感字段库和 session/access fake。可按真实工作流拆测试类并只抽共享构造器；先按[ViewModel owner 边界](../../architecture/viewmodel-ownership.md)与[修订历史生命周期合同](../../features/vault/entry-revision-history.md)裁定职责，避免测试拆分把仍在演进的生产边界固化成重复 fixture。当前源文件有用户未提交改动，后续迁移须保留并适配。
- [ ] 对全仓当前 67 个恰含一个 `@Test` 的文件和 146 个“含 1–3 个 tests 且不超过 100 行”的文件继续做 owner 对照后再决定是否合并；Android 平台/生命周期合同、不同映射文件、稳定协议键与安全策略即使只有一个测试也保持独立。特别保留 `LibraryProtoMappingsTest`、`InteractionProtoMappingsTest`、`DiagnosticsLogRecordingProtoMappingsTest`、`ClipboardPolicyProtoMappingsTest` 的分组，它们分别对应不同生产 Proto mapper 文件和不同持久化默认/拒绝语义，不因同 package 或文件小而并到单个 settings mega-suite。当前已核对部分候选；以本计划后续续扫记录为进度证据，整项仍待完成。

**2026-11-07 扫描开始时盘点：** 377 个 Kotlin test/androidTest 源文件（含 helper）、2,030 个 @Test 声明，均为静态注解数而非执行数；14 个文件超过 500 行。除上述候选外，RuntimeSessionManagerTest 920/26、SourceBoundaryViewModelTest 874/27、OtpCodeRuntimeTest 824/26、ObservabilityExportServiceTest 728/21、RoomObservationSessionTest 699/6、DiagnosticsViewModelTest 688/15、EncryptedLogRecordStoreTest 612/21、DefaultAppNoticeDispatcherTest 610/12（行/注解）。平台/资源/会话场景先保留完整合同，Detail owner 稳定后再拆。旧 351 文件/1,669 声明及 5 个超 500 行统计移除；执行期间测试文件有并发调整，以上尺寸/注解数绑定扫描开始快照；收尾执行数见 Task 86。失败先按 Task 86 处理，结构整理随后。

**2026-11-07 续扫：** 当前源码树为 372 个 Kotlin test/androidTest 文件、1,941 个 @Test 声明、15 个 500 行以上测试文件；静态计数包含独立声明，不代表执行通过。复核 DefaultAppNoticeDispatcherTest 的 12 个用例：settings 加载/恢复、mandatory 路由、系统投递失败 fallback、eventId 去重、sink 返回失败与抛异常分别走不同生产分支，未发现可确认重复或过时用例。相似的失败重试断言保留，因为返回 SinkResult.Failed 与 sink 抛异常分别覆盖正常失败分支和异常释放 claim 分支。单用例文件的 owner 对照还核实了 Detail favicon 异常映射与 state-to-UI 投影、Security 与 Privacy 两个 settings mapper、Vault OTP provider 与 Compose 可见性/组合生命周期各自验证不同生产边界，保留独立。未运行测试。

继续复核 `data/src/test/java/com/aozijx/passly/data/observability/EncryptedLogRecordStoreTest.kt`（21 个用例）：过期清理、按日/数量限制、按大小/总字节限制对应 EncryptedLogFileManager.cleanup 的不同条件；队列优先级两例验证不同被淘汰等级；普通 key unwrap 异常在 nextFileCursor 被跳过，fatal Error 继续传播；返回失败和抛异常分别验证 dedup/异常收尾。各组契约不同，未发现可确认冗余。

**2026-11-07 会话层重复性复核：** `RuntimeSessionManagerTest` 直接验证 `SessionLeaseGate` / `SessionResource` 的 unlock key ownership、lease drain/timeout、观测取消、资源关闭与 runtime transition telemetry；`VaultSessionControllerLifecycleTest` 通过控制器组合验证 AuthenticationState、DEK/permit 清理、数据库 reset/reopen、后台计时及 inbox key provisioning。两层都出现 seal/取消/fatal 字样，但断言 owner 和失败后的责任不同，不合并。

**2026-11-07 导出测试逻辑复核：** `ObservabilityExportServiceTest` 的 21 个用例分别覆盖授权前置、读取分页/排序去重、URI 与 share 文件生命周期、普通失败/取消/fatal 的传播及 cleanup failure 优先级；与 `EncryptedLogRecordStoreTest` 的加密存储、队列、retention 合同分层。share 的返回失败与抛错、设置删除失败与取消/Fatal 保留共享文件，分别走不同异常边界；未发现确认冗余。

**2026-11-07 More 菜单 UI 测试复核：** 全部 test/androidTest 仅有一个 more menu 用例 `VaultTopBarMenuTest.moreMenuKeepsPopupsMeasurableAndDispatchesRoutes`；它覆盖 More popup 尺寸、Scanner/Settings route dispatch、单帧动画可测性及 Sort 子菜单，不存在重复的 More 测试文件或同名行为用例。不同 popup/帧断言分别保护不同 UI 阶段，保留。

**2026-11-07 Wi-Fi 测试 owner 对照：** `WifiQrPayloadParserTest` / `WifiQrPayloadFormatterTest` 验证线格式、转义和协议拒绝；`WifiFormMapperTest` 验证 draft/UI 字段与扫码应用；`AddWifiViewModelTest` 验证初始化 owner、锁/保存竞争和用户可见状态。相同凭据/空密码字样来自 parser、formatter、mapper、ViewModel 不同层次合同，不合并。

**2026-11-07 全量续扫：** 当前树有 368 个包含 `@Test` 的 Kotlin test/androidTest 文件、1,953 个静态测试声明；模块分布为 app 197/1,022、buildSrc 8/124、core 22/80、data 47/213、domain 35/192、feature:autofill 29/146、runtime:session 1/26、security 29/150（文件/声明）。`OtpCodeRuntimeTest` 对照 `OtpCodeRuntime` 的订阅、授权暂停/恢复、ticker、HOTP 消耗、session lock 与 epoch 过期路径后，删除仅断言 fresh-auth 失败时状态为空的用例；该状态由 fresh-auth 恢复用例断言，且 ticker 不重试用例覆盖同一失败路径，删除后保留的断言更完整。另将两处 moving-factor 用例改用注入的固定时钟，消除墙钟周期边界依赖；未运行测试。`SensitiveRevisionFingerprintTest` 的稳定/变化断言已迁入同一 `SensitiveRevisionSnapshotCodecTest`，codec 生产 owner 未改变。备份 wire-model 序列化断言合入 `BackupCodecTest.fieldArchiveV1_wireFieldNamesAreStableAndIndependentFromDatabasePayloads`；银行卡 mapper 的 round-trip 与稳定键断言合并后仍保留完整 `assertEquals(entry, mapper.toEntry(decoded))` 和关键字段核对。Detail favicon/tag editor 删除的 StateModel/Presentation 测试断言分别在 Reducer/DetailReducer 与 `DetailUiMapperTest` 中找到对应覆盖；迁移保留了脏状态确认、保存失败草稿保留、关闭后忽略输入及 UI 字段映射。这些删除/合并未发现已丢失的独立断言。以上为当前未提交改动的静态逻辑复核。

**2026-11-07 OTP 导入用例分层复核：** `ImportScannedOtpUseCaseTest` 检查配置到完整 EntryDraft 的持久化、返回值等待存储确认、失败/异常、锁定/无密钥和取消传播；`ScannedOtpImportViewModelTest` 检查 ViewModel task owner 越过 Scanner caller 退出、认证会话在任务启动前变化与写入开始后撤权、重建 Scanner 后并发确认只写一次，以及 ViewModel 对成功/失败结果的等待与传递。两个文件都有 deferred 写入断言，但分别约束 UseCase 的存储事实和 ViewModel 的任务所有权/返回合同，保留。

**2026-11-07 备份资源缓冲测试复核：** 新增的 `VaultBackupResourceAccumulatorTest` 覆盖超出累计资源上限时擦除被拒内容，以及后续失败时擦除已累计内容并清零计数；`RoomVaultBackupStoreTest` 只验证真实 Room/文件资源的读写回环和回滚，不重复缓冲对象的敏感字节清理合同。保留该单测。

**2026-11-07 buildSrc 边界测试重叠复核：** `SourceBoundaryPolicyTest` 对 `SourceBoundaryVerifier` 的通用规则断言 ruleId 和精确 evidence；`EditorSourceBoundaryVerifierTest` 走路径归属包装器，检查文件归入 `presentation.ui` 后聚合报告的 owner 与规则标签；`SourceBoundaryViewModelTest` 则覆盖 ViewModel 声明范围、类型别名及可执行模板引用。虽有相同禁用 import/ViewModel 情景，调用入口和负责规则不同，当前没有可删的重复逻辑用例；大文件按 Task 74 另做 policy-family 拆分评估。

**2026-11-07 全量续扫（Scanner 生命周期）：** 当前静态盘点为 369 个含 `@Test` 的 Kotlin/Java 测试文件、1,956 个 `@Test` 声明；按模块 app 197/1,022、buildSrc 8/124、core 22/80、data 48/216、domain 35/192、feature 29/146、runtime 1/26、security 29/150（文件/声明）。复核 `ScannerReducerTest`、`ScannerViewModelTest`、`ScannerRouteEffectsTest` 及 Route/VM owner：Reducer 测状态转移，ViewModel 测取消失效/显式停止/宿主暂停与照片 picker 结果，RouteEffects 测 Lifecycle STARTED 收集、背景反馈丢弃与恢复后新反馈；跨层相似断言保护不同 owner/边界，没有可确认重复项。`StopScanning`/Route dispose 终止 owner，`HostStopped` 保留可恢复页面状态并让 picker 结果接管；当前用例覆盖旧 decode 的非协作取消迟到结果。未运行测试；此前 Task 74 的 368/1,953 是旧快照，以上为本次刷新值。

**2026-11-07 全量续扫（UI/仪器测试）：** 刷新后共 369 个含 `@Test` 文件、1,958 个声明；app 197/1,023、buildSrc 8/124、core 22/80、data 48/217、domain 35/192、feature 29/146、runtime 1/26、security 29/150。`app/src/androidTest` 有 23 个文件、43 个声明。复核 `InternalInputAutofillTest` 与 `SensitiveFieldVisibilityTest`、`TagEditorSheetTest` 与 reducer 测试、`RevisionHistorySheetTest` 的 UI 行为/状态边界后，当前相似覆盖分别属于 autofill 排除、无障碍语义、真实 Compose 控件禁用态、纯状态转移或滚动/尺寸合同，没有确认重复。另发现工作区正在修改的 `AndroidAutofillLaunchTargetTest` 新增 `accessibilityHostCreatesOnlyItsModeViewModel`，却断言 Activity ViewModelStore 恰有 1 项；当前 `AuthenticationHost` 与 `AutofillFeatureEntry` 分别请求 owner、legacy、accessibility、credential 四个 Activity 级 ViewModel，故该断言与现有生产路径不符。之前读取时的重复 import 在随后重新读取时已消失，未触碰该并行编辑文件。保留为 Autofill 变更中的待核实测试/生产合同冲突，不擅自删除或改写；未运行测试。

**2026-11-07 备份清理测试交叉复核：** `BackupWriteTestCleanupTest` 直接验证 `enforceWriteTestCleanup()` 对目录探测临时文件的删除失败及异常优先级；`BackupCreatedDocumentCleanupTest` 验证新建 SAF 文档在后续操作失败时的 owner 清理；`DefaultBackupOperationGatewayTest` 验证通过 `BackupStorageSupport.deleteDocument()` 回收导出目标并保留 cancellation/fatal 语义。生产 owner 分别是目录写入探测、文档创建到交接之间的生命周期、备份操作网关；异常类型虽相似，但调用边界不同，不能合并成一个 helper 测试，也未发现可删除断言。仅静态对照生产实现，未运行测试。

**2026-11-07 Revision History UI 测试逻辑整理：** `RevisionHistorySheetTest.captureSheet()` 原本在 `waitForIdle()` 和 500ms Compose 动画等待后，额外按屏幕顶部/底部 RGB 差值判断空历史截图有效，并最多 `Thread.sleep(100)` 轮询 20 次；长列表截图则直接接受首帧。该启发式只对其中一个状态生效，节点可见性与 sheet 尺寸已有 Compose 断言负责行为验证。删除像素探测/墙钟轮询，保留 Compose 等待、截图文件输出和 Bitmap 回收；未运行仪器测试，`git diff --check` 通过。

**2026-11-07 编辑器保存测试重叠复核：** `EditorWriteSessionTest.duplicateSaveAndEditsAreRejectedWhileTheAttemptIsRunning` 验证共享 owner 在 saving phase 拒绝第二次保存/编辑；`AddWifiViewModelTest.duplicateSaveIsSuppressedWhilePersistenceIsInFlight` 经过实际 Wi-Fi `Save` action 和仓储调用，验证该表单接线不重复提交并保持 Saving。一个是共享 session 单元合同，一个是唯一代表性的 ViewModel 集成合同，不能互相替代；Bank Card 的相邻用例测 CVV 校验/保存门禁，没有第二份相同 in-flight 重复保存断言。保留，无需合并。

**2026-11-07 合并 App Backup Snapshot Gateway 重复覆盖：** 新增的 unit `AppBackupSnapshotGatewayTest` 已覆盖 restore 时稳定 icon key 转为 Domain resource key、字节数组 owner 转移，以及兼容过滤后空内容的非空来源禁止 OVERWRITE 且不调用 store。删除 `app/src/androidTest/.../AppBackupSnapshotGatewayTest.overwriteRejectsCompatibilityFilteredNonemptySourceBeforeCallingStore` 的同义断言；该 `restore` 路径不访问 Android `Context`，unit 中 `ContextWrapper(null)` 已足够。保留 androidTest 的真实 `PackageManager` 读取映射/资源限制覆盖。刷新静态清点为 370 个含 `@Test` 文件、1,959 个声明；app 198/1,024、buildSrc 8/124、core 22/80、data 48/217、domain 35/192、feature 29/146、runtime 1/26、security 29/150。未运行测试。

**2026-11-07 跨全仓测试名扫描：** 解析到 1,953 个 `@Test` 方法，没有重复的方法名；该结果只作为检索线索，不能替代按行为与生产 owner 的覆盖比较。备份数据层 `VaultBackupResourceAccumulatorTest` 与应用层 `BackupSnapshotArchiveMapperTest` 都断言失败时擦除缓冲区，但前者覆盖累计上限/接收失败，后者覆盖快照到 archive 的重复资源 ID 校验失败；清理边界各自属于不同 owner，保留。

**2026-11-07 Autofill scope 拒绝复核：** `ResolveAutofillSelectionTest` 检查核心解析器在 scope mismatch 时不调用 credential resolver；`CredentialManagerFlowTest` 通过 API 流程检查映射成 `NotAvailable` 并在失败分支取消 interaction。相同拒绝输入保护不同边界和交互清理，不合并。

**2026-11-07 通知分发链测试复核：** `DefaultNoticeRouterTest` 单测路由计划，`DefaultNoticeDeduplicatorTest` 单测 claim/TTL，`DefaultInAppNoticeSinkTest` 验证容量/队列保持；`DefaultAppNoticeDispatcherTest` 验证它们在 direct dispatch/有界 channel 下的端到端组合。虽然都覆盖 mandatory、重复事件和失败重试，相应断言分别保护策略纯逻辑、claim 状态机、sink buffer 与收据/实际交付，不是可删重复。Sink 返回 `Failed` 与抛 `Exception` 的重试合同也保留为不同分支。当前工作区对恢复场景的 `firstSettingsRead` latch 改成不论 Loading/Failed 都释放，能避免首读直接 Failed 时测试卡在等待；未运行测试。

**2026-11-07 异步测试等待复核：** `SensitiveDataKeyManagerTest` 的 100ms 等待对应真实 monotonic TTL（配置 20ms），`AuthenticationAttemptSupportTest` 的 1ms delay 运行在 `runTest` 虚拟时钟中，用来让 NonCancellable 恢复跨过挂起点；两者不是墙钟轮询重复。`RoomObservationSessionTest` 的 250ms 是 Room/Paging 负向观察窗口，用于检查 reset 后无额外 emission。`AttachmentResourceGarbageCollectorTest` 的 100ms 则在 SEALED 初始状态后给后台 wakeup worker 调度机会，再验证资源文件仍在；当前类没有可观察的“已处理 SEALED wakeup”同步点，属于较弱但有明确目的的负向边界等待，暂未机械替换。未运行仪器测试。

**2026-11-08 Encrypted log queue overflow fixture correction:** 合并 DEBUG→INFO 和 INFO→WARN 队列淘汰场景后，WARN 样例由无异常的 `sampleRecord()` 直接改 level，违反 `LogRecordCodec` 对 WARN 记录必须携带异常快照的合同；同一 `@Test` 中还复用了加密日志目录，第二个 store 读到了前一个场景写入的记录。改用 `errorRecord()` 生成 WARN 样例，并为每个优先级场景分配独立目录；定向用例和整个 `EncryptedLogRecordStoreTest` 均通过（21/21）。

<a id="task-75"></a>

### Task 75：统一固定常量的文件位置与 owner，收敛重复合同值

- [ ] 继续按语义 owner 审查 companion 常量；仅将确属文件级的实现值移至文件顶部，类型/协议专属值归入准确 owner，不为位置变化改动算法或 wire 值。
- [x] `SessionLeaseGate.kt` 的 lease drain 轮询间隔和纳秒换算从 companion 移至文件级 `private const val`；原值与调用不变。
- [x] `DefaultAuthorizationGate.kt` 的 permit TTL 移至文件级 `private const val`；10 秒有效期与授权流程不变。
- [x] `CredentialAttemptLimiter.kt` 的尝试次数上限与 lockout 时长改为文件级 `private const val`；仅该实现消费，测试使用独立阈值期望，不保留 companion 常量 API。
- [x] `ObservabilityRuntimeController.kt` 的纳秒换算、生命周期 flush、crash flush 和 emergency flush 常量移至 imports 后的文件级 `private const val`；原值与可见范围不变。
- [x] `ObservabilityExportService.kt` 的单文件导出上限和 flush timeout 移到文件级 `private const val`；删除对 `DiagnosticsExportFileRetention.MAX_AGE_MS` 的本地转接常量，直接引用所属 retention owner。
- [x] Favicon 下载与暂存共享 10 MiB 输入上限；已集中到 `FaviconImageLimits.MAX_INPUT_BYTES`，删除两类重复值。
- [x] 诊断页 adapter、导出 service 与 `EncryptedLogRecordStore` 对 `LogRecordStore.readPage()` 使用/执行相同的 500 条单页上限；统一到 Core 端口的 `LogRecordStore.MAX_PAGE_RECORDS`。
- [x] `TaskClearActivityOwner` 与 `AppShell` 都发布 `APP_CLOSE_REMINDER` 并延迟关闭 task 1 秒；时长统一到 App 生命周期策略 `APP_CLOSE_REMINDER_DELAY_MS`，两处 owner 的取消与认证生命周期仍各自管理。
- [x] `AutofillInboxKeyMaterialCodec` 与 `AutofillInboxRecordCodec` 共用 RSA algorithm 与 SHA-256 digest 名称；已集中到 Security Inbox 的 `AutofillInboxCryptoAlgorithms`。Record 专属 RSA-OAEP transformation 与 MGF1 随格式合同归入 `AutofillInboxProtocol.Record`。
- [x] Inbox key material 与 record/payload 的 magic、版本、字段宽度和编码限额集中到 `AutofillInboxProtocol`；key ID/GCM tag 长度只定义一次。算法 provider 名称继续归 `AutofillInboxCryptoAlgorithms`，总记录/队列大小继续归 Domain 的 `AutofillInboxLimits`。
- [x] `EnvelopeManager` 与 `VerificationTag` 复用 Core 的 `CryptoConfig.ALGORITHM`，移除重复的 AES/GCM transformation 字符串；各自的 IV/tag 编码合同保留在 Security owner。
- [x] Backup V1 的 `CIPHER_TRANSFORMATION` 已归属 `BackupContainerV1Spec`，`BackupRecordCrypto` 复用该协议 owner；保留 cipher ID 和序列化格式。Favicon 裁剪缩放上下限已统一到共享 `FaviconCropRequest` 契约；Data 日志记录直接引用其版本化格式 owner，不保留本地别名。
- [ ] 验证新增/迁移后的可见范围、所有调用点和单一真相源；持久化 key、wire tag、格式版本不得改变，安全限制不得被放宽。补充/迁移对应常量与协议测试；运行受影响模块单测、`verifyArchitecture`、lint 和 `git diff --check`。同步更新 Kotlin 规范中的位置规则；不要建立无 owner 的全局 `Constants.kt`，也不要把资源文本、Gradle 构建值或运行时可调配置复制成 Kotlin 常量。

**约定完成与扫描证据（2026-11-02）：** `docs/development/kotlin-style.md` 新增“常量与固定值归属”规则，`AGENTS.md` 加入简短入口约束；常量按文件私有实现、跨文件同 owner、协议/算法语义、资源/Gradle/运行期配置分别归属。生产源码静态扫描命中 360 个 `const val` 声明，多个声明位于文件尾部 companion；精确字面量聚合发现一批待语义核验的重复（例：observability page size、应用关闭延迟、Favicon 输入上限、Autofill Inbox RSA/SHA 标识）。该扫描同时发现相同数字在不同文件中表达不同策略，故本任务要求核对消费者和行为合同后再集中；本轮只更新规则与 TODO，未移动常量或更改生产代码。

**2026-11-08 LogRecordStore 分页上限批次：** Task 39 已将诊断读操作迁到 `DiagnosticsLogStoreAdapter`，旧待办中的 `ObservabilityRuntimeController` owner 描述过时。现确认 Viewer adapter、`ObservabilityExportService` 都通过相同的 `LogRecordStore.readPage(cursor, limit)` 读取；Data 的 `EncryptedLogRecordStore` 也把每次 `readPage()` 限制在 1..500，并在 `readBetween()` 里以 500 分段读取。这是端口的真实分页合同，将 `MAX_PAGE_RECORDS` 放进 `LogRecordStore`，三个生产调用点共享它并删除原私有副本。Data 测试先因接口常量缺失而编译失败，迁移后定向 store 测试通过；App adapter/export 测试、`verifyArchitecture` 与 `git diff --check` 通过。Viewer、Export、Observability 架构和错误处理文档已按当前 owner 更新。关闭 task 的提醒延迟与 Inbox RSA/SHA 标识已分别在后续批次归一。

**2026-11-08 Favicon 上限批次：** `DefaultFaviconImageProcessor.stageHttpsUrl()` 先经 `FaviconImageDownloader.download()` 读取受限响应，再把字节交给 `FaviconImageStore.stage()`；用户上传也直接经过同一暂存输入边界。因此下载和暂存对同类 favicon 输入施加同一个 10 MiB 上限，属于同一 Feature 合同。将值集中到 `FaviconImageLimits.MAX_INPUT_BYTES`，删除 Downloader 与 Store 上的重复类常量；定向下载测试改用独立 10 MiB 期望值，边界行为不变。`:app:testDebugUnitTest --tests "com.aozijx.passly.app.entry.favicon.FaviconImageDownloaderTest"`、`verifyArchitecture` 通过，`git diff --check` 通过；Store 的 Android instrumentation 未运行（当前无连接设备）。Task 75 的其他常量族与 Store 的设备运行验证仍开放。

**2026-11-08 App 关闭提醒时长批次：** `TaskClearActivityOwner` 和 `AppShell` 都先发布 `NoticeCode.APP_CLOSE_REMINDER` 再等待 1 秒关闭 task；同一提醒使用同一时长。将重复值放入 `app.lifecycle.APP_CLOSE_REMINDER_DELAY_MS`，保留两处 owner 各自的任务取消、认证撤销和 Compose/Activity 生命周期处理。更新待办 owner 名称以匹配 `TaskClearActivityOwner`。`TaskClearActivityOwnerTest`、App 编译和 `verifyArchitecture` 通过；关闭时序、延迟值不变。

**2026-11-08 Autofill Inbox 算法标识批次：** `AutofillInboxKeyMaterialCodec` 与 `AutofillInboxRecordCodec` 位于同一 Security Inbox owner；公钥生成/类型校验共用 `RSA` provider 标识，fingerprint 与 OAEP 参数共用 `SHA-256` provider 名称。集中到 `AutofillInboxCryptoAlgorithms` 并删除两个 codec 的重复声明。仅共享重复标识，RecordCodec 使用的 RSA-OAEP transformation 和 MGF1 参数保持本地；测试中的 provider/协议字符串保持独立字面期望。`AutofillInboxKeyMaterialCodecTest`、`AutofillInboxRecordCodecTest` 及 `verifyArchitecture` 通过，协议字节格式不变。

**2026-11-08 Security AES/GCM transformation 去重：** `EnvelopeManager` 和 `VerificationTag` 的 `Cipher.getInstance()` 原读取各自声明的 `AES/GCM/NoPadding`，与 Core `CryptoConfig.ALGORITHM` 完全相同；Security 已依赖 Core。两处改用现有 Core 算法标识并删除局部副本，保留 Envelope/VerificationTag 各自的 IV、tag、AAD 与字节格式合同。Data 日志文件以 `EncryptedLogFileManager` 版本化格式 owner 保留本地值；Backup 的 V1 cipher ID/记录合同另行处理。定向 Envelope、VerificationTag 测试与 `verifyArchitecture` 通过。

**2026-11-08 Backup V1 cipher transformation 去重：** `BackupRecordCrypto.crypt()` 原在 `Cipher.getInstance()` 写死 `AES/GCM/NoPadding`；此算法属于 Backup V1 容器格式，移入 `BackupContainerV1Spec.CIPHER_TRANSFORMATION`，由记录加解密实现引用同一来源。`CIPHER_AES_256_GCM_ID` 仍为 1，header 字段和加密记录格式不变；只合并同协议 owner 内的固定算法标识，不与 Security/Data 的算法格式常量共用。定向 Backup codec/分块流测试及 `verifyArchitecture` 通过。

**2026-11-08 Data log transformation alias 清理：** `EncryptedLogFileManager` 是版本化日志容器格式 owner 并保存唯一 `TRANSFORMATION` 字面量；`EncryptedLogRecordStore` 两处加解密直接引用该值，删除同文件 companion 的私有转接常量。`FILE_VERSION`、cipher 参数与日志字节格式均未改变；文件管理和记录写入的职责继续分开。

**2026-11-08 ObservabilityRuntimeController 常量归位：** 将该控制器尾部 companion 中仅在类内使用的纳秒换算及三种 flush timeout 移到 imports 后的文件级 private 区域；数值和调用点不变。`ObservabilityRuntimeControllerTest` 与 `verifyArchitecture` 通过。

**2026-11-08 ObservabilityExportService 常量归位：** 将 `MAX_EXPORT_RECORDS` 与 `FLUSH_TIMEOUT_MS` 从类尾 companion 移到文件级 `private const val`，删除 `DELETE_DELAY_MS` 转接并直接读取 `DiagnosticsExportFileRetention.MAX_AGE_MS`。导出数量、flush 时间和清理时间行为不变；定向 `ObservabilityExportServiceTest` 与 `verifyArchitecture` 通过，App Kotlin 编译通过。

**2026-11-08 SessionLeaseGate 常量归位：** 将 `DRAIN_POLL_MS` 与 `NANOS_PER_MILLISECOND` 从类尾 companion 移至文件级 `private const val`，保留 50 ms 轮询与 1,000,000 纳秒换算。`:runtime:session:test`、`verifyArchitecture` 和 `git diff --check` 通过。

**2026-11-08 DefaultAuthorizationGate 常量归位：** 将 `PERMIT_TTL_MS` 从类尾 companion 移到文件级 `private const val`，授权许可的 10 秒有效期与 revoke 时机不变。`:security:testDebugUnitTest` 179 项通过，`verifyArchitecture` 与 `git diff --check` 通过。

**2026-11-08 CredentialAttemptLimiter 策略常量收口：** `MAX_ATTEMPTS` 与 `LOCKOUT_MS` 的生产引用全部位于 limiter 内，跨文件引用只有单测；将其改为文件级 `private const val`，测试改用独立阈值 5。`Context.currentAttemptTime()` 移到文件级 private extension 并删除不再需要的 companion。`CredentialAttemptLimiterTest` 与 `verifyArchitecture` 通过。

**2026-11-08 Autofill Inbox protocol owner 收口：** key envelope 与 record/payload 格式常量移至单一 `AutofillInboxProtocol` 专用文件，并按 KeyMaterial/Record 分组；共享 key ID 与 GCM tag 字节长度去重。Codec 删除类尾 protocol companion；provider 算法名称和 Domain 总体存储限额继续留在各自 owner。magic、版本、编码和密码参数原值不变；两个 codec 定向测试、独立 record/payload magic 与版本字节断言、`verifyArchitecture` 与 `git diff --check` 通过，测试期望使用独立格式字面量。

**2026-11-08 Task 75 lint 验证：** 运行 Core、Data、Security、App `lintDebug` 与 `verifyArchitecture`；前三个模块 lint 和架构检查完成。App 在 `:app:lintAnalyzeDebug` 分析 `EntryEditorFields.kt` 时触发 Lint/IntelliJ 内部异常 `AsyncExecutionService.getService must not return null`，未产出有效 App lint 结果。该异常不是源码通过证据；Task 75 最终验证保持开放，需在工具异常消除后重跑 App lint。

**2026-11-08 Favicon crop zoom bounds 去重：** UI `FaviconCropTransform` 和 `FaviconBitmapTransformer` 都将 zoom 限制为 1..6，分别保护手势状态与对 processor 输入的最终裁剪；二者共享 Core `FaviconCropRequest` 契约。将 MIN/MAX 放入请求模型 companion，默认 zoom 与两处 clamp 复用同一边界，测试期望保持独立数值。原边界不变；Data 日志 transformation alias 已在同一 Task 75 的独立批次清理，版本化格式 owner 保持不变。


<a id="task-77"></a>

### Task 77：核实诊断日志退出时的持久性与周期写入策略

- [ ] 稳定 flush 合同后调查离开/后台/移除 task/force-stop/强杀的日志损失窗口，记录是否等待 flush、最后持久批次及设备/API；任意终止不保证回调。
- [ ] 先完成[诊断 Tasks 1、2](2026-11-06-passly-diagnostics-reliability-followup.md)，再裁定周期/退出策略，不因独立模块或进程就宣称 durable。

**2026-11-07：deferred，设备持久性调查。** store 已有容量 256 单线程有界队列、50ms/16 条微批，每批 flush + fd.sync；onStop 仅异步排队 flush，不保证完成。移除旧“尚未批量写入/落盘”前提。

## 已实现机制：保留定向验收


<a id="task-22"></a>

### Task 22：保留通知权限请求在 Activity 重建后的用户意图

- [ ] 设备验收 `AppCompatDelegate.setApplicationLocales()` 配置重建与系统授权/拒绝弹窗组合，并确认 PermissionRequestHost arbiter lease 最终释放。分别断言 grant 经 `completeRuntimePermissionRequest(request)` 依据当前 pending owner 处理，deny 经 `onAction(RuntimePermissionDenied(request))` 按 token/`enableWhenGranted` 处理；验证新 Route 重绑 owner、旧 host/session 关闭后不会产生重复提交或过期反馈。现有 `PermissionRequestHostTest` 只覆盖宿主替换和旧结果拒绝，不能代替真实 Activity recreation。
- [ ] 区分真实 Activity Result `Denied` 与 `permissionHost.request(...)` 未启动时的 `Busy` / `HostUnavailable`：`NotificationsRoute` 当前把后二者转成 `RuntimePermissionDenied(request)`，导致 ViewModel 清除 pending 并可能显示“权限已拒绝”。为未启动请求定义保留/清除开启意图、用户反馈和重试时机；增加 Route/host 行为覆盖，证明忙碌或无 host 不会伪装成系统拒绝，并验证 permission arbiter lease 已释放。

**实现证据（2026-11-01，提交 `ca0e02602`）：** Route 在 Activity 配置变化销毁时通过 `isChangingConfigurations` 保留 ViewModel 中相同的 `NotificationPermissionRequest` 与开通意图；新页面 owner 进入后重绑并重发请求。真正离页会清除其 owner 的待处理请求，迟到的旧 token grant/deny 不能影响新 owner。`NotificationSettingsViewModelTest` 已覆盖重建后同一 token 恢复并只写一次、真实离页后旧结果作废，以及新 owner 请求不被旧 owner dispose 清理。源码分支上 grant 由 `completeRuntimePermissionRequest(request)` 校验活动 owner；deny 由 `onAction(RuntimePermissionDenied(request))` 校验 token 并依赖离页时清空 pending 拦截过期结果。通知设置文档已记录此差异。实际 locale recreation 与系统弹窗仍缺设备级组合验证。

**旧证据更正：** 早期记录的无 owner `CancelRuntimePermissionRequest` 分支已删除。当前 Route 只调用 `onPageExited(owner, preservePendingPermissionRequest=...)`；ViewModel 先验证 owner 仍 active，再只修改该 owner 持有的 pending request。`AnimatedContent` 交错目前有 ViewModel owner 回归，但动画实际 dispose 顺序仍未由 Compose 导航测试验证。

<a id="task-23"></a>

### Task 23：闭合通知分发在设置快照未就绪时的事件所有权

- [ ] 给 AndroidSystemNotificationGateway 增加平台注入测试，覆盖 channel 创建、文案解析和 notify() 的普通异常、取消及 fatal Error；验证失败结果、异常类型和 dispatcher dedup claim 释放。

**2026-11-07：retained，仅平台测试缺口。** 快照 provider 已使用 RecoverableRead 发布 Loading/Ready/Failed 并重订阅；dispatcher 对 mandatory 在 Loading/Failed 时继续路由。provider/dispatcher 测试已有首次失败、旧快照失效、publisher 启动竞态、恢复和取消覆盖。等级比较已用显式 severity/meetsMinimum，Snackbar 时长由 longToast 表达。移除旧“永久 Loading/Ready、mandatory 被抑制、ordinal 决策”的实现待办。

<a id="task-27"></a>

### Task 27：闭合 OTP Runtime 缓存失效与刷新故障恢复合同

- [x] 成功保存 `EntryType.OTP` 的 `FieldKey.OTP_SECRET` 或成功恢复包含不同 OTP 配置的历史修订后，立即使当前 Detail 工作流中的 OTP schedule/code 失效，并基于已持久化的新配置重新生成；在 OTP 周期边界到达前，展示和复制都不得继续使用旧配置对应的 TOTP。保存失败、无效编辑、同值编辑和页面/认证 owner 替换分别保留正确的缓存与取消行为。
- [ ] 为 Route 生命周期补宿主行为验证：OTP 编辑成功返回时 Detail 刷新当前条目，编辑取消/失败不会触发保存成功路径；Vault 列表恢复时只刷新仍可见订阅，离开后不重新激活无 owner 条目。当前无跨 ViewModel 全局广播桥。
- [ ] 增加 Route/编辑器宿主行为回归：OTP secret 保存成功后返回 Detail 时展示与 copy 使用新配置；离页/锁定期间迟到结果不得覆盖当前状态。运行 OtpCodeRuntime、DetailViewModel 与 Vault OTP 订阅相关测试、`verifyArchitecture` 和 `git diff --check`。
- [x] 将 Vault 列表 `copyField()` 与 `copyOtp()` 的剪贴板写入绑定到 page owner：退出 Route 后授权恢复不得写入剪贴板或发出旧页面事件；仍有效 owner 可正常复制。

**实现边界：** Detail 与 Vault List 通过独立 Runtime 持有各自 OTP state。OTP 编辑页保存成功返回前台时，Route 刷新本 Runtime 当前仍请求的条目；Detail 的成功历史修订恢复分支也立即刷新当前条目。`entryChanged()` 保留 active/subscription owner，只撤销旧 epoch、schedule 与验证码，再对仍有 owner 的条目异步重载；不会把临时列表订阅提升成永久 active entry。激活读取由 entry epoch 去重，但显式 owner/config 变化可以替换未完成读取的 epoch，旧结果及其认证/锁定异常不能覆盖新配置。Detail ViewModel 现在覆盖前台重载与 restore 成功/失败；未覆盖真实 Route lifecycle、编辑成功返回、锁定或离页期间晚到结果。
**Vault 列表复制 owner 修复（2026-11-07）：** `VaultViewModel.copyField()` / `copyOtp()` 向 copy use case 传递 `pageEffects.isActive(owner)` 作为 `canDeliver`，并移除两个 use case 的默认 `{ true }`，使调用方必须明确提供交付条件。宿主行为测试覆盖授权等待期间离页后字段与 OTP 都不写剪贴板、活动 owner 字段复制仍成功并发出事件；`VaultViewModelTest` 3 项与 `CopyEntryFieldUseCaseTest` 7 项测试通过。唯一详细验收留在本 Task；OTP 产品行为另见[OTP Task 1](2026-11-06-passly-otp-review-followups.md#task-1)。

**2026-11-07 测试订阅修正：** `VaultViewModelTest.routeExitWhileOtpCopyWaitsForAuthorizationPreventsClipboardWrite` 原先未收集生产 `WhileSubscribed` 的 `totpStatesFlow`，断言 OTP 永远未进入 Ready。测试现由 `UnconfinedTestDispatcher` collector 启动真实订阅并在 `finally` 取消；修正前全量执行 998 项中该用例失败，修正后强制全量 App 单测通过。
**ticker 验证：** `start()` 现使用可替换的毫秒时钟（生产默认 `System.currentTimeMillis`），测试通过受控时钟与 coroutine test scheduler 驱动周期边界。回归验证一个条目普通读取异常清除旧码、其他条目同轮继续刷新、失败条目下一轮恢复；显式 `refreshStates(nowSeconds)` 测试仍保留，用来验证具体移动因子和 copy 状态。

<a id="task-29"></a>

### Task 29：在 Security 页面退出时清除 App Password 对话框草稿

- [ ] 增加 Coordinator/Compose Route 生命周期回归，验证输入后真实离开 Security 页面会清空对话框与草稿、迟到输入/授权或操作结果不能在重进后重新写入旧状态或打开旧对话框、配置重建遵守选定的恢复策略；保留现有提交期间页面 owner 和 `CharArray` `finally` 清理合同。同步认证/安全文档，运行 App Password Coordinator/ViewModel 与 Route 行为测试、`verifyArchitecture` 和 `git diff --check`。（Compose 生命周期仪器测试已编写；待设备连接后执行。）

<a id="task-31"></a>

### Task 31：让 Room 长期观察在会话锁切换时可取消并可重建

- [ ] 在真实 SQLCipher/设备执行已有 RoomObservationSessionTest 的 soft-lock、seal/reopen、DAO emission race、旧 lease 释放用例；验证普通错误可见、撤权清敏感状态、快速恢复不读旧 handle。

**2026-11-07：deferred，机制/竞态测试已接入，待仪器执行。** AppDatabaseSessionObservations 已用 lock generation 取消旧流并处理 SessionLockedException；DatabaseObservationRecoveryTest 覆盖恢复、旧 emission 取消和普通错误不吞空。RoomObservationSessionTest:402、470 已有屏障测试，移除“无恢复机制/无屏障测试”的旧描述。

<a id="task-44"></a>

### Task 44：迟到的 AuthenticationHost 密码提交必须擦除输入缓冲

- [ ] 为真实认证宿主补取消/替换/重建后的迟到密码 callback 测试，保留实际 CharArray 引用，验证错误 request、stale identity、CAS 失配和未接管路径擦除，且不提交给新请求。

**2026-11-07：deferred，防护已实现待宿主验收。** ActivityAuthUiHost:270–283 已擦除请求身份/类型不符及 CAS 失败输入；SecretDialogSubmissionTest 已覆盖空请求、错误请求、inactive、接管及 stale identity，不再列为当前缺少基础 wipe。

<a id="task-45"></a>

### Task 45：统一认证请求与生物识别策略轮换的 Host 并发所有权

- [ ] 以真实 Host/Provisioner 覆盖普通认证与轮换争用、等待取消、pause/destroy、迟到 callback、journal 收尾和重试；同一 UI lease 只服务一个请求，旧 owner 不影响新 owner。

**2026-11-07：deferred，机制已实现待整链路验收。** AuthenticationHostRegistry:73–92 已有可取消 UI mutex；provisioner 与普通认证共用 lease/finally release。ActivityAuthUiHost 使用 request-owned prompt/identity/CAS，HostRegistry/RequestOwnedSlot 已有竞争/取消单测。旧 requestMutex.tryLock 描述移除，当前为 AuthenticationRequestSlot。


<a id="task-64"></a>

### Task 64：让附件 GC 解锁后完整排空并在失败后恢复

- [ ] 在真实 SQLCipher/文件系统执行 AttachmentResourceGarbageCollectorTest 多批排空、失败文件重试、worker 重建唤醒，验证撤权、当前/历史引用保护及最终回收。
- [ ] 验证条目写事务已提交后，调用方取消是否会把成功写入呈现为未完成并允许重复提交：`CreateEntryExecutor.execute()`、`UpdateEntryExecutor.execute()`、`RestoreEntryRevisionExecutor.execute()` 在数据库写入返回后调用 `PostCommitCleanup.afterCommit(...)`；其 `run()` 将普通清理异常记录后吞掉，却继续传播 `CancellationException`，而 `AttachmentResourceGarbageCollector.drain()` 也传播取消。追踪 Vault `EditorWriteSession`、Detail revision owner 与 Autofill/Inbox 调用者的结果映射和重试入口，按每个 durable commit point 定义取消语义；覆盖事务已提交但 GC 被取消、普通清理异常、fatal `Error`、调用方离页/认证撤权和重试，特别确认普通创建的随机 EntryId 不会因“结果未知”被再次创建。静态链路已确认，尚无 owner 级行为证据，不先把它描述成已复现缺陷。

**2026-11-07：deferred，实现/测试已接入待仪器执行。** worker 已跨页、越过失败行并有界退避；单测有跨批/锁定/退避/fatal/取消，仪器源码有跨批/文件重试/重建唤醒。移除旧“仅一批、无重试 owner”实现待办。

**2026-11-07 全量续扫（设置/备份/会话锁）：** 当前静态清点为 370 个含 `@Test` 文件、1,959 个声明。Interface settings mapper 直接投影六个 UI 字段与半径范围，ViewModel 用例另测恢复读取、写失败、仓储发射与 page owner；映射与异步写入职责不同，保留。备份 wire model 的单独测试在当前工作区删除，完整 format/version/revision、keyed value、旧字段排除断言已并入 `BackupCodecTest` 的稳定 wire test；银行卡序列化测试把原 round-trip 与 stable-key 用例合为一例，仍检查全部 Entry round-trip、重要 wire keys 与业务字段，合并后无已识别断言缺口。`VaultSessionControllerLifecycleTest` 中同样的 fatal permit revoke 场景分别从 `lock()` 和 exclusive reset 收尾，分别约束两个公开生产入口，保留。SourceBoundaryPolicy 的 66 例逐条保护规则、归属或证据；与 ViewModel declaration scanner 有相似输入，但执行的 verifier/规则不同，暂无可删重复，Task 74 的按 policy family 拆分仍待做。上一批 Revision History 截图等待清理已由 `894703e73` 提交；本轮没有提交、没有运行测试。
**2026-11-07 Autofill Activity ViewModel 断言复核更正：** 先前把“只创建所选模式的 Autofill ViewModel”误读为 Activity `ViewModelStore` 总数必须等于 1。当前 `AutofillFeatureEntry` 用 `when (launch)` 进入独立 Legacy/Accessibility/Credential composable，各分支仅调用本模式的 `hiltViewModel(activity)`；`AuthenticationHost` 自己创建 `AuthenticationHostOwnerViewModel`，`PersistedAppTheme` 也有其主题 ViewModel，但测试只按三个 Autofill ViewModel 的类名检查所选存在、其余两种不存在。因此 `accessibilityHostCreatesOnlyItsModeViewModel` 与当前生产结构相符，旧的“计数冲突”结论撤销。尚未执行仪器测试；测试只覆盖 Accessibility 的分支，若要覆盖三类互斥创建，可后续为 Legacy/Credential 补对称用例。
**2026-11-07 停用测试全仓静态筛查：** 对 `app/buildSrc/core/data/domain/feature/runtime/security` 的 Kotlin 测试源码搜索 `@Ignore`、`@Disabled`、测试 TODO/FIXME 与 `skip()`，当前均无匹配；该筛查只排除显式停用标记，不能替代逐用例逻辑复核。
**2026-11-07 小型测试文件 owner 对照（App/Shell）：** `SessionActivityTouchDispatchTest` 两例分别验证 ACTION_DOWN 在内容收到事件前报告 session activity，以及 MOVE 只委派且保持内容处理结果，属于同一策略的正反分支，作为同一测试类已合并表达，未见重复。`AppShellDestinationTest` 覆盖失败优先级、session mode 和 AuthenticationState 到 typed destination 的状态映射；Android `SecureShellDestinationTest` 验证 Compose 切换目的地时 Vault 敏感内容无需等待动画即 dispose。二者分别约束纯状态映射和真实 Compose 生命周期，不合并。当前只完成该候选子集，Task 74 其余单/小测试文件仍待 owner 对照。
**2026-11-07 认证策略小型测试交叉复核：** `AuthenticationAvailabilityResolverTest` 验证设备可用性只门控已配置的 Biometric，`AppPasswordProvisioningCommitTest` 另验证持久化后即使刷新报告异常，已写凭据仍被解析为可用；与 `AuthenticationSessionReusePolicyTest` 的会话复用判断不是同一规则。后者分别覆盖 Locking 拒绝、Authenticated 允许、AutofillAuthenticated 按 purpose 隔离；但 `canReuseSession()` 还有 `storageUnlocked=false` 与 `requiresFreshAuthentication=true` 两个显式拒绝门，当前直接单测未分别断言。记录为逻辑覆盖待补，现有用例保留；本轮未新增测试或运行测试。
**2026-11-07 Detail 银行卡 Reveal All 过时分支清理：** `DetailEntryTypeUiMapper` 是 `DetailBankCardUiModel` 的唯一生产构造点，并始终设置 `cardholderRevealed=true`；`BankCardSection` 将 cardholder 作为普通可见字段渲染，敏感 reveal selection 只由 card number/CVV/payment PIN 的存在与 revealed 状态决定。旧 `fieldsToReveal()` 仍将 cardholder 加入 Reveal All，只有测试构造的不可达状态能触发。删除该选择分支，并把测试 fixture 改为生产状态、预期只选择已存在且未揭示的 card number；未改测试数量，未运行测试。
**2026-11-07 Autofill 设置页不可达失败态清理：** `RecoverableReadState.Loading/Failed` 不携带旧 preferences；`AutofillRoute` 仅在 `availableValue != null` 时调用 settings mapper/Section，加载或读取失败走独立不可用 UI。因此 Section 内 `preferencesRefreshing`、`preferencesFailed`、`canEditPreferences` 的只读/重试分支无法由生产路径构造；Compose 用例 `failedPreferencesRemainVisibleButReadOnlyUntilRetry` 与 ViewModel“失败不暴露旧设置”合同冲突。删除该仪器测试，移除 UiModel/mapper/Route/Section 中不可达状态字段及对应刷新提示资源，保留 Route 的 retry/load failure UI 和 system-service failure 分支；Section 测试从 4 例减为 3 例。当前全仓静态清点 370 个测试文件、1,958 个声明；静态引用搜索与 `git diff --check` 通过，未运行测试。
**2026-11-07 消息等级小型测试对照：** `NoticeLevelTest` 只覆盖 App 通知的 Toast 长短显示策略（ERROR/CRITICAL 长显示），`MessageLevelTest` 穷举 domain `MessageLevel` 的所有实际/最低等级组合，验证 `severity >= severity` 的门槛函数；前者不涉及分级排序，后者不涉及 Toast 时长，生产 owner 与合同不同。当前实现均显式使用 `longToast`/`severity`，测试中未发现旧 `ordinal` 比较，保留两例。
**2026-11-07 认证复用拒绝门覆盖已补：** 针对上条复核发现的 `canReuseSession()` 缺口，在已有 Authenticated 正向测试中加入 `storageUnlocked=false` 和 `requiresFreshAuthentication=true` 两个负例，分别验证生产 helper 的两个全局早退条件；没有新增 `@Test` 或改生产代码。该待补项现已闭合；静态 `git diff --check` 通过，测试仍未运行。

**2026-11-07 KDF runner 测试失败路径清理：** `SingleThreadKdfRunnerTest` 的 worker 由 `CountDownLatch` 阻塞；旧清理只在全部断言通过后释放 worker 并关闭 runner，早期 assertion failure 会遗留已启动的 runner/secret。将 worker 释放、job join、secret close 与 runner close 移入 `finally`。同时删除 `resultDiscarded.await(50ms)`：result 只在 release latch 后才创建，release 前该负断言无法区分任何可行生产行为；保留 cancel 后 callback 未继续、释放 worker 后迟到 discard 必须到达的有效合同。主路径和 finally 都 `countDown`，保证失败时 worker 可退出。静态检查待本轮结束复核，未运行测试。

**2026-11-07 测试方法重名与 Runtime session 逻辑复核：** 全仓 test/androidTest 源码清点为 378 个 Kotlin/Java 源文件、370 个含 `@Test` 文件、1,959 个声明；64 个单测试文件、139 个不超过 100 行且含 1–3 个测试的文件。注解方法名扫描到 1,959 个方法名且无重名；仅能排除字面重名，不能证明行为不重复。`RuntimeSessionManagerTest` 的 26 例按 key 获取/擦除、资源 open 与失败回滚、事件失败语义、soft lock/resume、lease drain/timeout、dispatcher 与终态重新打开分别核对；取消/fatal 组合测试覆盖转换时机和结果，不与普通异常 telemetry 用例重复，现有 lease 关闭和 cleanup 阶段断言亦有独立生产分支。保留该类用例，Task 74 仍需对剩余单/小测试文件按 owner 继续筛查。当前静态 diff 检查通过，无测试执行、无提交。

**2026-11-07 Revision history rebuilder 单例用例逻辑核实：** `EntryRevisionHistoryRebuilderTest` 的单个 Room transaction 合同一次构造 active 与 soft-deleted entry，并覆盖删除两者的旧 revision、当前 entry version 不被修改、同一时间的当前内容基线解密和最终 revision 总数；步骤共同验证一次性重建后的数据库事实，不拆成互不独立的断言用例。旧名称 `VersionOneBaselines` 容易被读作把 entry revision 改成 1，实际断言保留 entry version 8/3，改名为 `rebuild_discardsObsoleteHistoryAndCreatesCurrentBaselinesForActiveAndDeletedEntries`。全仓测试引用只找到 Rebuilder 的这一个直接 Room 用例；`InitializeEntryRevisionHistory.execute()` 的 generation marker、transaction 失败重试和 post-commit cleanup 尚无直接测试覆盖，记为待补逻辑覆盖；本轮未新增测试。

**2026-11-07 数据库 schema 不匹配测试逻辑修正：** `DatabaseSchemaCompatibilityTest` 旧名声称“不删数据”，但 fixture 未插入数据，且用 `assertNotNull` 接受任何打开异常。现插入哨兵 `EntryEntity`，制造 attachment_refs 旧列结构后验证 Room 以 `IllegalStateException` 拒绝打开、DB 文件保留、旧列结构仍在且哨兵标题仍可从原表读回；因此实际覆盖 fail closed 与数据保留，而不只是文件存在。单例仍验证同一故障事务前后的完整数据库合同，不拆分。未运行仪器测试。

**2026-11-07 Flow 异常传播测试捕获范围收紧：** 全仓 test Kotlin 检查到 `SettingsDataStoreReadTest` 用 `catch(Throwable)` 暂存 protobuf corruption、IOException 和 CancellationException，再断言结果。生产 `tagCorruptSettingsData()` 只转换 `CorruptionException`、其他错误原样重抛；按预期类型分别捕获 `CorruptSettingsDataException`、`IOException`、`CancellationException` 并继续检查原始对象，unexpected `Error` 不再被测试吞成断言结果。没有新增测试；静态检查待本轮结束复核，未运行测试。

**2026-11-07 全仓宽异常断言复扫：** 全仓仍有 5 个 `catch(Throwable)` 测试位置：`RuntimeSessionManagerTest` 两处遍历 Cancellation/Error 矩阵并分别核对身份/清理状态；`ObservabilityExportServiceTest` 一处使用 Cancellation/Error 的计划删除异常矩阵；两个 Favicon 测试位置是泛型 `assertSuspendThrows<T>` helper，捕获后检查 `T` 并返回原实例。都对应 fatal/cancellation 传播合同或类型化 helper，不收紧。JUnit/Kotlin 对 `Exception`、`Throwable`、`RuntimeException` 作宽期望类型的全仓正则扫描无匹配。`SettingsDataStoreReadTest` 的三处宽捕获已换为具体 expected type。当前四个测试文件的 `git diff --check` 通过，未运行测试或暂存。
**2026-11-07 Settings 读失败状态二次交叉核对：** 复查 Autofil 失败态清理边界后，未发现同类不可达失败 UI 测试遗漏。`PermissionManagementScreen` 直接接收 VM 的 `RecoverableReadState`，Retry action 回到同一 VM；`DataManagementSettingsViewModel` 仍由独立 `BackupRoute` 使用，Failed/Retry 用例覆盖该 backup-directory owner；主 Data Management 页面使用 `StorageSettingsViewModel`，其 `logRecordingMode` Failed/Retry 由对应 VM 测试覆盖且 Route 分支可达。相似“settings read failed”文案属于多个真实状态 owner，没有可合并或删除的同义用例。
**2026-11-07 Notification Settings ViewModel 逻辑复核：** 17 个用例逐项对照 `NotificationSettingsViewModel` 状态机。三个相似的 permission owner 测试分别覆盖正常离页后的旧 grant/deny 拒绝、配置重建复用同一 request identity 并提交，以及旧 owner 的迟到 dispose 不得撤销新 owner request；另有用户禁用时拒绝晚到 grant、重复 enable 共用 pending request、撤销后 denial 忽略并可重试，输入时序不同且状态责任独立。读取失败/失败态拒绝写入/Loading、平台权限申请与平台开关复核、消息和 topic 写入、普通保存失败与 grant 后保存失败各守不同路径。暂未发现重复或过时用例；本轮只静态复核，未运行测试。
**2026-11-07 Security/Privacy settings 逻辑复核：** Security 与 Privacy 用例共享底层 `SecuritySettingsRepository`，但分别验证认证策略操作（Biometric 轮换、last-method、operation owner）与隐私/剪贴板设置；读失败/重试也各验证独立 ViewModel 接线，没有合并候选。发现 `PrivacySettingsAction.ClearClipboardNow` 只有生产分支、没有测试调用；新增一个参数化结果顺序用例，在同一 Page owner 内验证清理器返回 owns=true/false 分别映射到 `ClipboardCleared`/`ClipboardNotCleared`，同时确认各调用一次。静态声明数因此增加 1；`git diff --check` 通过，未运行测试。
**2026-11-07 Permission Screen 测试 fixture 状态修正：** `PermissionManagementScreen` 只有 `backupDirectoryReadState=Ready` 才渲染备份目录管理 row；Compose action 测试原先只设置 `backupDirectory` 展示数据，read state 沿用 `Loading` 默认值，却点击 Ready 分支才有的“管理” action。将该测试状态设为 `RecoverableReadState.Ready`，使 action 断言对应真实可达 UI；用例数不变。静态 diff 检查通过，未运行仪器测试。
**2026-11-07 Compose settings group/editor focus 复核：** `SegmentedSettingsGroupTest` 的 anchored selection 用例验证用户选择更新外部状态；slider 居中用例覆盖 `sliderSettingsGroupItem` 的几何/语义边界，而该组件仍由 `InterfaceScreen` 调用，Autofill 页面移除 slider 不影响它，两例保留。`EntryFocusBehaviorTest` 检查密码编辑器向下传递 screen/save modifiers，并通过 IME 验证标题→账号→密码焦点顺序；`NextFocusTextField` 也用于银行卡、Wi-Fi、OTP 编辑器，目前没有重复焦点测试，保留该代表性 UI 合同。本轮静态查看，未运行仪器测试。
**2026-11-07 Revision History Compose 测试副作用清理：** `RevisionHistorySheetTest` 已通过 semantics 断言空态/restore 可用性、50 条历史与 30 项差异的滚动选择、敏感字段 reveal gating，以及恢复中禁止返回/下滑关闭；另有两处 `captureSheet()` 只在测试结束时把屏幕截图写入外部文件，没有 golden 比较或后续读取，且重复等待/推进时钟。删除截图写入 helper 与调用，保留布局尺寸和交互断言。`git diff --check` 通过，未运行仪器测试。
**2026-11-07 动画测试容差逻辑修正：** `AnimatedColorSchemeComposeTest.assertColorNear` 原先将 expected 与 actual 的中点和 expected 比较，实际容差因此被放宽约一倍；改为直接比较 RGBA 四通道并移除无用 `lerp` 导入。保留同一重定向/平滑过渡用例；`git diff --check` 通过，未运行仪器测试。

**2026-11-07 More 菜单与墙钟等待交叉搜索：** 全仓只发现一条 Compose `More` 菜单交互测试；`VaultNavigationTest` 的 `OpenSettings` 是纯导航状态映射，与 UI action 不重复。测试中的扫描/设置路由、popup 尺寸和 sort 子菜单断言属于不同职责但同一 top bar owner，暂留在单例用例。全仓匹配到 20ms TTL 后等待 100ms、sealed GC worker 启动观察 100ms、Room observer reset 后等待 250ms 的无额外 emission 窗口；它们分别约束时钟过期、启动恢复与异步 observer 静默，没有将其当作可删除的相同 sleep。后续若要消除墙钟等待，需要相应 owner 暴露可控时钟/启动屏障或可判定的观察窗口；未改这些合同。

**2026-11-07 失败类型断言逻辑复核：** `EntryRevisionRestoreTransactionTest.staleVersion_rollsBackWithoutChangingCurrentEntry` 用 expectedVersion=1 对 version=2 的记录调用 restore；生产首个版本检查明确抛 `Conflict`，旧断言只确认任意 failure 非空，允许错误异常类型通过。改为断言 `Conflict` 并附实际异常信息，同时保留回滚后条目/修订/活动记录断言。静态 `git diff --check` 通过，未运行仪器测试。

**2026-11-07 KeyStore fatal 错误传播断言加强：** `BiometricAliasDeletionTest` 旧 fatal 用例比较 message，无法证明 `Error` 原实例穿过仅捕获 `Exception` 的 helper 传播；改用 `assertSame(fatal, actual)`，保留普通异常返回 retryable false 与成功 true 两个相邻分支。静态 `git diff --check` 通过，未运行测试。

**2026-11-07 Autofill launch UI 采样时序修正：** `AndroidAutofillLaunchTargetTest` 原先在 ActivityScenario 达到 RESUMED 后立刻 close，并只在 `onActivityPaused` 读取 ViewModelStore；Compose 首次组合可能仍待主线程/首帧调度，导致把尚未创建 VM 当成路由失败。close 前加入 `InstrumentationRegistry.waitForIdleSync()`，再由 pause callback 取样；只检查三类 Autofill workflow VM 的存在/互斥，不限制 AuthenticationHost 或主题 owner。静态 `git diff --check` 通过，未运行仪器测试。

**2026-11-07 Autofill launch target 参数合同补齐：** `AndroidAutofillLaunchTargetTest` 为 Legacy 与 Credential 各传入了 request ID，但未读取返回 Intent 的 `REQUEST_ID` extra；测试此前只证明路由目标/action。新增两条 extra 值断言，覆盖 target 将调用参数写入平台 Intent 的合同。`git diff --check` 通过，未运行仪器测试。

**2026-11-07 Backup JSON 格式错误断言收紧：** `BackupCodecTest.documentValidator_acceptsAccountHierarchy_andRejectsMixedPayload` 把合法导出 JSON 的唯一 `format` 值改成旧格式后导入；执行路径到 `BackupBundleValidator` 的 format `require`，合同是 `IllegalArgumentException`。旧 `assertThrows(Exception::class.java)` 会让解码/IO 等无关异常也通过，已收紧到该类型。容器密码/密文认证测试仍保留宽异常断言，因为 AES-GCM provider 认证失败的具体异常类不属于稳定跨平台合同。静态 `git diff --check` 通过，未运行测试。

**2026-11-07 异常类型断言二次核实更正：** 初审认为 AES-GCM provider 认证失败应保持宽异常；沿调用链确认相关测试直接返回 `Cipher.doFinal`，JCA 对 AEAD tag 校验失败合同为 `AEADBadTagException`。因此收紧 `BackupCodecTest` 三个错误密码/密文/header 认证断言、`EncryptedLogRecordStoreTest` 文件篡改、`AutofillInboxKeyMaterialCodecTest` 错误 DEK 与 `VerificationTagTest` 错误 DEK；非法 UTF-8 则收紧为 `CharacterCodingException`。先前“容器认证保留宽异常”的记录由本条更正。`git diff --check` 通过，未运行测试。

**2026-11-07 全仓宽异常期望类型扫描：** Kotlin `*Test.kt` 对 JUnit `assertThrows(Exception|Throwable|RuntimeException)` 与 Kotlin `assertThrows/assertFailsWith` 的全仓搜索当前无匹配；本轮已将可由本地调用链证明的格式拒绝、GCM tag 失败和严格 UTF-8 解码改为具体异常。此静态规则不覆盖 catch 后手写判定或自定义 expectFailure helper，后续逻辑复核仍需查看调用点。修改路径 `git diff --check` 通过，未运行测试。

**2026-11-07 会话测试过时名称清理：** 将文件与类从 `VaultSessionIdleTimeoutTest` 重命名为 `VaultSessionControllerLifecycleTest`，准确反映其主要生产 owner；未移动/合并用例或改断言，23 个 `@Test` 保持。对照 `RuntimeSessionManagerTest` 后确认相似 seal/cancel/fatal 场景分属资源运行时与安全控制器责任，保留两组。旧名称全仓搜索无匹配；未运行测试。

**2026-11-07 Domain Wi-Fi/card validation 测试 owner 对齐：** `EntryValidationTest` 中 open 密码拒绝、WEP 长度、WPA 长度/hex 与空 SSID 四例直接调用 `WifiCredentialValidation`，其余覆盖 `EntryValidation` 投影与委托；全仓测试引用未找到这些规则的其他副本。将四例移动到新增 `WifiCredentialValidationTest`，`EntryValidationTest` 保留真正通过 `Entry` 构造调用的集成验证；不删除断言、不增加 `@Test`。相邻 `WifiSecurityTypeTest` 检查的是持久化字符串解析/稳定值，不是同一规则。CardNumberValidator 直接测试算法结果；EntryValidation 的同组卡号 fixture 则验证把 checksum/characters 结果映射到稳定 `EntryViolation` 且接受 editor 格式输入，保持独立，并将方法名改为说明其 mapping 合同。静态 diff 与未跟踪新文件尾随空格检查通过，未运行测试。

**2026-11-07 Autofill 状态观察异常断言更正：** 首次把 7 个 observer 生命周期用例中的 5 处断言强化为异常对象 identity；执行后确认 `callbackFlow` 的 coroutine stacktrace recovery 会复制 throwable，实际/期望的具体类型和 message 一致。恢复为类型与 message 语义断言；7 个用例及 App/Autofill 全量单测现均通过。之前记录的“改 assertSame、未运行”已由本条实测更正。

**2026-11-07 测试源码计数刷新：** 当前工作树含 380 个 test/androidTest Kotlin/Java 源文件、372 个含 `@Test` 的文件、1,966 个声明；其中 `domain` 仍为 192。相较上一快照，四个 Wi-Fi 用例仅移动到 owner 文件、声明数不变；并行新增的 `AutofillServiceStatusObservationTest` 贡献 1 个测试文件和 7 个声明。后续按当前工作树继续刷新，避免把并行功能测试归入本次移动。

**2026-11-08 Entry draft/form/edit 测试合同复核：** `EntryDraftMaterializerTest` 将宽 `IllegalArgumentException` 期望收紧到 `InvalidEntryDraft.fields` 精确字段，并区分 `UnsupportedEntryDraft`；`EntryFormEditorTest` 为 OTP/card/Wi-Fi 完整表单替换补充身份、图标、收藏、过期时间、自定义字段与关联保留断言，并精确校验必填 violation；`EntryEditTest` 精确校验标题、标签、应用 ID 与 Wi-Fi 名称的字段及错误码。三类测试各自对应 draft 构造、完整表单替换、聚合局部编辑，不重复，保留原测试数。`git diff --check` 通过；未运行测试，未提交。

**2026-11-08 全项目测试基线与 More 菜单复核：** 当前可见源码含 407 个 `src/test` / `src/androidTest` Kotlin/Java 文件，394 个文件有 `@Test`，共 2,083 个 `@Test` 标注；其中 `buildSrc` 有 8 个测试源码文件、128 个标注，不能遗漏。全仓 More 菜单路由/排序弹窗行为只命中 `VaultTopBarMenuTest` 一处；该 Compose 测试同时覆盖弹窗测量、Scanner/Settings 路由和排序菜单展开，未发现可合并的重复用例。全量逻辑复核仍在进行；未运行测试，未提交。

**2026-11-08 Wi-Fi QR 与 revision snapshot 测试逻辑复核：** Parser 用例覆盖输入分类、企业网络/畸形结构、别名与解码转义；Formatter 用例覆盖稳定输出格式、共享凭据规则与 formatter→parser 往返。往返测试验证双边协议性质，不与单向用例重复，保留。发现 `SensitiveRevisionSnapshotCodecTest` 的“round trip”输入顺序原本与 codec 的排序输出一致，且只检查第一项 ciphertext/keyVersion；现将输入改为逆序并断言两项 ciphertext 与版本，确保排序和完整恢复合同；fingerprint 测试补充仅 keyVersion 变化时 token 也变化。另将同时涵盖 `EntryUpdateTransaction` 与 `EntryRevisionRestoreTransaction` 的仪器测试类/文件改名为 `EntryUpdateAndRevisionRestoreTransactionTest`，6 个用例保留，前 3 个 update、后 3 个 restore。Restore 复核再补足“可逆最新修订”对 secret notes 与敏感 ciphertext/keyVersion 的快照断言，让 stale-version rollback 比较完整 EntryEntity 和所有 secret 行内容/版本/更新时间；update notes 用例补验当前 secret bundle 和 revision snapshot 均写入新 notes。未增加或删除测试。静态 diff 检查通过，未运行测试，未提交。

**2026-11-08 Revision history rebuild 测试逻辑复核：** `EntryRevisionHistoryRebuilder` 清除旧历史后委托 `EntryRevisionWriter` 为每个条目重建内容及敏感字段 baseline；原唯一仪器用例覆盖活动/已删除条目与当前 notes/title，但没有验证敏感密文迁移。现为活动条目插入 password sentinel ciphertext，断言 baseline 含精确 key、ciphertext、key version；已删除条目无敏感字段时断言快照为空。仍是同一用例，未增测试数；`git diff --check` 通过，未运行测试，未提交。

**2026-11-08 Favicon/cache/repository 仪器测试隔离复核：** `FaviconBitmapTransformerTest` 只覆盖 bitmap 解码/裁剪/编码，`FaviconImageStoreTest` 覆盖 staging/promote/recovery 路径，不重复。发现 Favicon Store 测试 `@After` 原会递归删除真实 `context.filesDir/vault_images`，Store 构造也会清理其中 staging；改为使用 `TemporaryFolder` 上的隔离 Context。继续沿全仓 `deleteDatabase`/资源目录清理扫描，确认 `RoomVaultBackupStoreTest` 删除生产 DB 与全部附件/图标目录，`AttachmentResourceGarbageCollectorTest` 和 `RoomObservationSessionTest` 删除生产 DB，`AndroidAppCacheStoreTest` 在每例前清空整个 app cache；另一个图标回收测试会在真实 cache 下递归删同名目录。新增 app/data androidTest `IsolatedTestContext`，把这些测试的 DB、files、cache 与资源根限制在每例临时目录；`DatabaseSchemaCompatibilityTest` 的旧 schema fixture也改为临时 DB。基于临时目录每例为空，再删除 Cache 测试多余的全缓存前清理/marker 后清理，以及 Favicon recovery 对临时根的多余递归删除；`@Test` 数不变。受影响的 7 个仪器测试类共 29 个 `@Test` 数与 HEAD 相同；`git diff --check` 通过，新隔离 Context 文件无尾随空格。未运行仪器测试，未提交。

**2026-11-08 全仓异常传播候选续核：** `ChunkedBackupStreamsTest` 损坏 GCM record 直接经过 `BackupRecordCrypto.crypt()` 调用 JCA `Cipher.doFinal`，stream 只清理后重抛；将宽 `Exception` 期望收紧为 `AEADBadTagException`。Recovery rollback 与 Attachment GC worker 的 fatal/cancellation 均直接重抛注入对象；与 `AuthenticationAttemptSupport` 的 fatal restore 一样，改用对象 identity 断言，并保留 suppressed failure、重试次数与不报告断言。未增删用例；未运行测试、未提交。

**2026-11-08 Security 密钥异常断言逻辑复核：** `AttachmentContentCryptoTest` 与 `SensitiveDataKeyManagerTest` 的随机源/存储 fake 注入固定异常实例，原断言只检查 `IllegalStateException` 或 cancellation/fatal 的大类；沿生产路径可确认原对象直接传播，现改为 identity 断言，同时保留所有随机缓冲区擦除检查。敏感密钥未授权读取改断言专用 `FreshAuthenticationRequiredException`；`DekManagerTest` 的锁定日志与派生取消同样断言注入对象原样传播，并把 FieldKeyManager 锁定检查收紧为精确异常类型/message。用例数不变。全仓静态 `git diff --check` 通过；未运行测试，未提交。

**2026-11-08 Security 小型测试 owner 对照：** `DeviceKdfPolicyTest` 覆盖慢/小设备基线、快速大内存档位和 moderate baseline 的 250ms 边界；最后一例在现有类中并列断言 250ms iterations=4 与 251ms iterations=3，补齐此前静态发现的策略分支缺口。`AuthenticationKdfParametersTest` 原先的全零参数先被 version 校验拦截，不能证明其他限制；改为每个非法样例均使用其余合法值，分别触发 version、memory 档位、iterations 上下界及高内存组合、parallelism、key length 约束。`BiometricPromptErrorClassifierTest` 分开覆盖平台不可用与 lockout/crypto 分类；`AuthenticationEnvelopeContextTest` 验证 KDF/type AAD 绑定及 IV 前置校验；`BiometricAliasDeletionTest` 验证普通失败可重试、fatal 传播、删除成功。生产 owner 和异常边界各异，无可确认重复；测试声明增加 1，未运行测试。

**2026-11-08 全仓测试源码计数刷新：** 当前可见 `src/test` / `src/androidTest` Kotlin/Java 源文件 410 个，其中 395 个含 `@Test`，共 2,093 个标注；按模块为 app 214/1,095、buildSrc 8/128、core 18/71、data 49/223、domain 37/197、feature 32/165、runtime 2/34、security 35/180（含测试文件/标注数）。相较上一快照增长 3 个源码文件、10 个 `@Test` 标注；增量属于当前工作树整体变化，尚未逐项归属本轮清理。该计数是清点基线，不代表全部用例逻辑已审核。

**2026-11-08 SourceBoundaryPolicy 测试族拆分后计数：** 全仓当前 415 个 `src/test` / `src/androidTest` Kotlin/Java 源文件，399 个含 `@Test` 文件，仍为 2,093 个标注；buildSrc 现在 13 个测试源码、12 个含测试文件、128 个标注。结构拆分增加 5 个源文件和 4 个含测试文件，不改变断言总数。未运行测试、未提交。

**2026-11-08 后续源码计数复核：** 当前仍为 415 个测试源码、399 个含测试文件；总标注变为 2,094，app 为 1,096（较拆分后刷新时增加 1），buildSrc 保持 12 个含测试文件/128 个标注。增量出现在当前 app 工作树，未纳入本次 buildSrc 拆分；未暂存或提交。

**2026-11-08 VaultSessionController 取消传播复核：** `VaultSessionControllerLifecycleTest` 的软锁恢复、recovery unlock、sealed lock 与 soft-lock cancellation 分别经 `commitUnlock()`、recovery rollback、security trace/non-cancellable cleanup 的公开路径，断言撤权/收尾状态和日志，保护不同调用入口，保留。由于部分路径经过 `withContext(NonCancellable)` 与 logger span，不能仅凭 fake 注入就要求 throwable identity；统一用辅助断言核对精确 `CancellationException` 类型与注入 message。大类仍有会话锁/重置、后台 timeout 与 Inbox provisioning 不同 owner，按 Task 74 待后续拆类；当前仍 24 个 `@Test`。未运行测试、未提交。

**2026-11-08 VaultSessionController 测试按 owner 拆类：** 锁/重置主类 19 例、后台锁 4 例、Inbox provisioning 1 例，三类均由原 24 例按生产责任分组；`runMainTest` 在共享 helper 中仍用同一测试 scheduler 并在 finally 复位 Main。相对原文件，逐方法名与方法体比较为 24/24 一致；没有测试增删。静态 `git diff --check` 通过，未运行测试。

**2026-11-08 DetailViewModel 测试 owner 对照：** 当前类 28 例分属 related-password binding（1）、edit preparation（3）、OTP restore/detail（3）、package picker（4）、activity/revision observers（6）、detail snapshot（8）、revision restore 的提交/离页生命周期（5）；各组覆盖不同 action、owner 或失败/取消/迟到结果合同，未找到可确认重复。`saved login can bind another...` 与 `DetailPasswordBindingWorkflowTest.successfulBind...` 结果相似，但前者通过 ViewModel action 验证 workflow 装配、`DetailUiState` 候选投影及写入后 relatedEntries 更新；后者直接保护 workflow 的副作用与刷新回调，因此保留两层。Detail 生产 owner 当前仍有未提交的跨文件重构，按 Task 74 的先决条件暂不拆该类。

**2026-11-08 全仓测试源码计数刷新：** `rg --files` 扫描当前工作树得到 419 个 Kotlin/Java `src/test` / `src/androidTest` 源文件，402 个含 `@Test` 的文件，共 2,097 个标注；按模块为 app 222/1,099、buildSrc 13/128、core 18/71、data 52/223、domain 37/197、feature 35/165、runtime 2/34、security 40/180（源文件/标注）。相比前次快照增 4 个源文件、3 个含测试文件、3 个标注；增量属于当前全工作树，未全部归因于本轮清理。此统计只作扫描基线，不证明逻辑审核完成。

**2026-11-08 OtpCodeRuntimeTest 逻辑复核：** 当前 25 例按 HOTP 单次消费/失败、TOTP 窗口刷新与 ticker 继续/重试、fresh-auth 与 session-lock 暂停恢复、visible/detail subscription 计数，以及 config/generation 迟到结果隔离逐组对照 `OtpCodeRuntime`。相似的失败分支分别从 `activate`、ticker refresh、HOTP consume 进入；release 在 config load/generation 两阶段、session lock 与 unlock/resubscribe 世代切换也各有不同 owner token 生命周期，没有可确认重复项。将 2 处空状态断言从 `Map<String, Any>` 收紧到 `Map<String, OtpCodeState>`，与 production `StateFlow<Map<String, OtpCodeState>>` 一致；测试数量保持 25。静态 `git diff --check` 通过，未运行测试。

**2026-11-08 Autofill settings 测试 owner 对照：** 单例 `AutofillSettingsUiMapperTest` 断言 settings/platform availability 到 UI model 的所有字段；`AutofillSettingsViewModelTest` 的 8 例覆盖偏好流初始/失败/重试、系统状态流独立失败/重试、subscriber 生命周期、单设置写入与错误 effect，职责与 mapper 投影不同，不合并。全仓静态搜索 `assertThrows(Exception|RuntimeException|Throwable)` 与 `assertFailsWith<Exception|RuntimeException|Throwable>` 无命中；该结果仅说明未发现这几种宽类型断言，不替代具体异常路径审核。

**2026-11-08 全项目计数复核：** 当前工作树为 419 个测试源码、402 个含 `@Test` 文件和 2,103 个测试标注；模块为 app 222/1,105、buildSrc 13/128、core 18/71、data 52/223、domain 37/197、feature 35/165、runtime 2/34、security 40/180。较前次扫描总标注增加 6，增量都在 app 工作树，尚未归因到清理批次；计数仅是索引，不代表全量逻辑审查完成。

**2026-11-08 Settings navigation 测试 owner 对照：** `SettingsDetailRouteResolverTest` 验证 detail 转场方向、单窗层级和 retained route 选择；`SettingsDestinationTest` 验证 main route key 映射、detail 路由注册/稳定键及 SettingsGroup 顺序。虽然同 package，直接生产对象与合同分别是 transition resolver、route catalog 和 UI group 定义；当前没有重复方法可合并。

**2026-11-08 全仓测试盘点刷新：** 当前有 420 个 Kotlin/Java 测试源码文件、403 个含 `@Test` 的文件、2,107 个标注；模块为 app 223/1,109、buildSrc 13/128、core 18/71、data 52/223、domain 37/197、feature 35/165、runtime 2/34、security 40/180。单测试文件 69 个，含 1–3 例且不超过 100 行的文件 148 个。相较上个快照，文件和测试数仍随 app 工作树变化；以上只用于候选索引，逐 owner 审查继续进行。

**2026-11-08 page effects / notice queue owner 对照：** `PageEffectsTest` 验证页面 owner token 对排队效果的过滤、旧 composition 的迟到退出和 trySend 满队结果；`PageEffectLifecycleTest` 验证 `Lifecycle.STARTED` 的收集/恢复及页面退出时停止态缓存不重放。退出后丢弃的结果相似，但后者额外覆盖 stopped host 恢复时的 owner 与 lifecycle 交互，保留两层。`DefaultNoticeDeduplicatorTest` 的 event-id claim/TTL/release 与 `DefaultInAppNoticeSinkTest` 的 AppNotice 队列满时失败结果及已排队通知保留，是不同生产组件；同为 64 容量的 Channel 不构成可合并的行为合同，dispatcher 另覆盖 sink 失败后的 claim retry，均保留。

**2026-11-08 Autofill launch 小型测试 owner 对照：** `AutofillPendingIntentPolicyTest` 保护 PendingIntent mutable/update flags 与 request identity/cancellation key；`AutofillIntentExtrasTest` 保护 credential-provider Intent action 的稳定外部值。两者虽同属 platform launch 且同目录，分别约束 PendingIntent 身份与 Intent action 协议；调用链会共同使用，但不存在重复断言或可安全删减覆盖，保持各自直接 owner 测试。

**2026-11-08 Autofill runtime 小型测试 owner 对照：** `AutofillServiceCleanupTest` 唯一直接调用 `launchAutofillServiceCleanup`，验证 service scope 已取消时 cleanup 仍完成且 afterCleanup 被抑制；交互生命周期测试保护 store/lease/timeout 的授权与关闭，不重复该 service cleanup 合同。`AutofillWebConfirmationTest` 在 `confirmWebEntry` 阶段验证 recipient approval、候选与 live interaction/time 绑定；`AutofillInteractionLifecycleTest.storeRejectsCandidateThatWasNotPresented` 在 `beginSelection` 阶段验证未展示候选不进入 selection/auth。两处同为 `CANDIDATE_NOT_PRESENTED`，但拦截不同状态转换，均保留。

**2026-11-08 Diagnostics clear reducer 用例合并：** `DiagnosticsReducerTest` 中两例都调用 `LogsCleared` 并断言清空 retained/visible rows、恢复 Ready 与清除确认状态，唯一差别是初始 loadState 为 Ready 或 RefreshFailed；合为一个表驱动用例遍历两种起态并保留两例断言并集，类内 `@Test` 从 5 减为 4。`DiagnosticsViewModelTest` 仍独立验证异步 clear 失败、去重、重入和迟到结果 owner，不与 reducer 投影重复。静态 `git diff --check` 通过；未运行测试。

**2026-11-08 Diagnostics 迟到成功清理逻辑修正：** 重新对照 Route owner 生命周期、`ObservabilityRuntimeController.clear()` 的持久化存储操作和文档后，发现 `late successful clear cannot erase lines loaded by a reentered page` 的预期与“清理成功清空 retained rows”契约相反：清理不随页面退出取消，已成功清空全局日志存储后，仍显示重入前已读入的行会与持久化状态不一致。将该用例改为断言迟到成功清空重入页行，并移除成功结果上的旧 owner 门控；失败结果及 `ClearFailed` 仍受发起页 owner 限制。同步更新诊断 viewer 文档。静态 `git diff --check` 通过，未运行测试、未提交。

**2026-11-08 认证可用性小测试 owner 与 scope 复核：** `AuthenticationAvailabilityResolverTest` 的纯凭据快照→可用方法投影，与 `AuthenticationAvailabilityRetryTest` 的延迟/退避调度分属 resolver 和 retry 两个生产 owner；即使同 package、各只有一例也不合并。Retry 用例原先自行创建 `SupervisorJob` scope 并只在断言后 cancel，断言失败时 job 不会由 `runTest` 接管；改用测试自带、结束时自动取消的 `backgroundScope`，保持 scheduler 与时间断言。未运行测试。

**2026-11-08 Recovery credential fatal 清理用例复核：** `DefaultRecoveryCredentialGeneratorTest` 是唯一直接构造生产 generator 的用例，验证 KDF fatal `Error` 原样传播且 `finally` 已关闭 `SecretChars`；`DefaultRecoveryCodeDraftFactoryTest` 的 Ready/Failed/Cancelled 用例通过接口 fake 验证 factory 组合与会话 owner，不重复 generator 清理合同。将“copy 后任意失败都算清理成功”的宽断言收紧为 `copyForWorker()` 必须因 secret 已销毁而抛 `IllegalStateException`；无增删测试。全批静态 `git diff --check` 通过，未运行测试、未提交。

**2026-11-08 小型测试 owner 续审（密码对话框/强度 UI）：** `AppPasswordDialogCoordinatorLifecycleTest` 检查 `DisposableEffect` 离开页面时调用 clear、重建 route 得到全新 coordinator；`AppPasswordDialogCoordinatorTest` 的直接 `clear()` 用例验证该生产操作本身关闭状态、清空 secret draft 且可重复调用，owner 接线与状态操作不同层，不合并。`PasswordStrengthUiStateTest` 穷举五个 `PasswordStrengthLevel` 到主题色和本地化资源的映射，domain evaluator 并未复制该 app 投影；只有一个 mapper 生产 owner，保留独立。以上未发现过时或重复断言；未运行测试。

**2026-11-08 Permission management UI/VM 小测试交叉复核：** ViewModel 单例覆盖 backup directory source 首次发射失败后 `Retry` 重新订阅并恢复 Ready，且 unavailable 状态与目录 reader 的“未配置”结果保持区分；Compose 单例覆盖 camera row 的 action dispatch、backup manage action、manifest capability row 不可点击。Route 的 Retry/selected/open-settings 分别路由到 VM、系统权限 launcher 与 SAF settings；两个测试断言无重复，Ready read state 也使备份管理 action 的 fixture 可达。保留；未运行测试。

**2026-11-08 Backup 单例映射测试交叉复核：** `BackupExportFormatMappingTest` 核对三个导出选择到稳定 archive `BackupFormatId` 的映射及 enum 完整性；`BackupSheetUiMapperTest` 验证配置 sheet 将敏感口令、目录、格式、资源选项、Entry 类型与 submit readiness 投影到 UI model。`BackupExportOptionsTest` 测 options 默认值、排除项和空 Entry type 的构造拒绝，分别属于内部 ID adapter、UI mapper 与选项值对象，字段/格式相似不代表重复；保留三个 owner 测试。未运行测试。

**2026-11-08 App theme 单例 owner 交叉复核：** `AppThemeSettingsViewModelTest` 覆盖 corrupt appearance Flow 进入 Failed 后显式 retry 重新订阅并 Ready，保障设置读取状态；`AnimatedColorSchemeComposeTest` 通过手动 Compose 时钟验证动画中连续 retarget 从当前可见色平滑衔接并最终抵达目标色。二者没有共享断言和生产 owner，不合并；测试分别走 ViewModel repository 与 Compose animation，保留。

**2026-11-08 Android Keystore 单例测试跨模块复核：** App 的 `AndroidKeystoreIdentifierHasherTest` 验证真实 AndroidKeyStore HMAC identifier 对相同输入稳定、不同 namespace 隔离且不泄露原始 package；Data 的 `AndroidKeystoreLogKeyWrapperTest` 验证 AES wrapping key 下的日志数据 key/AAD round-trip。二者测试平台相同但算法、数据类型与安全合同不同，不合并；hasher 用例的随机 alias 在 `finally` 删除，wrapper 用例以随机 data key 验证字节精确往返，未发现陈旧断言。

**2026-11-08 Permission request host 生命周期复核：** `PermissionRequestHostTest` 的单场景按真实 Compose owner 替换、旧 host 再请求、迟到旧结果、当前 host 成功和最终 dispose 顺序，验证 session 关闭、callback 过滤及全局 arbiter lease 交接；`PermissionRequestSessionTest` 直接覆盖 lease 回收/历史和 launcher 失败/重试，`DefaultPermissionRequestArbiterTest` 覆盖互斥原语。三层分别是 Compose launcher host、session 生命周期与原子 lease，无重复可删断言；主机 Activity 配置重建下的授权弹窗终态仍是独立未完成设备验收项，见该计划现存 follow-up。未运行测试。

**2026-11-08 Domain 小型测试合同补全：** `AuthenticationKdfParametersTest` 各非法构造现在独立通过其他字段校验，再触发对应参数约束；`DeviceKdfPolicyTest` 新覆盖 moderate baseline 250/251ms 的 iterations 阈值。`AppCornerRadiusConstraintsTest` 原来只断言常量，未保护生产 `normalize()`；在原用例内补充下界钳制、区间内保留与上界钳制。`EntryAccountGraphTest` 原单例只测 OTP→login→account，扩展同一图合同覆盖直接成员、recovery 直接归属、OTP 间接归属及三者成员投影，仍不新增测试方法。`SensitiveFieldPresenceTest` 仅测 key membership，而“不含值”的保证由当前模型只暴露 entryId/keys 的类型结构提供；`MessageLevelTest` 穷举 severity 阈值组合。未发现跨 owner 重复；静态复核实现一致，测试未运行。

**2026-11-08 编辑器保存能力投影复核：** `AddEntrySaveCapabilityTest` 与 AddPassword/OTP ViewModel save action 测试分别保护纯 UiState 的 `canSave` 投影和真实 action/persistence 编排，保留直接投影用例。检查生产 getter 发现除 form validity 与 Saving 外还拒绝 `EditorLoadPhase.Loading/Failed`；现有用例未覆盖该 gate，故在既有测试中对密码和 OTP 两种 state 各增加 Loading/Failed 断言，没有新增测试方法。未运行测试。

**2026-11-08 Trash/List mapper 相似字段复核：** `TrashUiMapper` 与 `VaultListUiMapper` 都投影标题、关联域/应用及图标，并独立组装同一 typed visual-classifier 输入；但 Trash 额外拥有 deleted time/version/回收站展示 model，List 负责 favorite、能力、OTP、类别与排序/导航 UI，不能合并测试或 owner。`TrashUiMapperTest` 原来未验证 title/username/iconName、转为本地 file URI 的 `iconImageModel` 和 classifier 结果；在唯一 mapper 用例中补齐对每个输出字段的断言。未新增测试方法或运行测试。

**2026-11-08 无效 Entry type label 测试删除：** `EntryTypeUiLabelsTest` 唯一断言是所有 `EntryType.labelRes != 0`。生产 `EntryType.labelRes` 是穷尽 `when`，新增 enum 值若未映射会编译失败；每个分支引用真实 `R.string.*`，缺失资源同样编译失败，生成资源 ID 非零是构建期事实。该运行时断言没有可独立保护的合同，删除该单例文件；不影响 detail/trash 对实际标签映射的使用。未运行测试。

**2026-11-08 图标颜色 token 存储测试复核：** `VaultIconPaletteTest` 把五个持久化颜色 token 做 round-trip，并确认 null/未知值回退到 automatic；相邻 Favicon mapper 测试保护 `EntryIcon.color` 与 BuiltIn 来源转换，而 Compose 颜色解析在 `iconColorForStorageToken`，不是同一规则。token 是持久化字符串协议，未发现可合并或过时断言，保留单例。

**2026-11-08 Preference write helper 单例复核：** `PreferenceWriteFeedbackTest` 唯一直接测试共享 helper，验证 `write()` 未完成时不提前发布成功，完成后才回调。8 个设置/Vault 调用者的 ViewModel 测试还覆盖各自失败错误映射、page owner、锁定拒写或取消效果；这些装配合同不同于 helper 的写入完成时序，没有同义重复。异常类型转换由错误映射 owner 另测，保留当前单例。

**2026-11-08 文本备份渲染单例复核：** `TextVaultRendererTest` 原有普通字段可读输出与 `favorite` 内部元数据排除检查保留；新增两例覆盖 `includeTechnicalInfo` 同时门控 ID/version 和附件元数据，以及 notes 截断、text-list/boolean/integer/long/custom-field 输出。避免断言时区敏感的动态时间字符串；这些测试都直接调用 renderer，没有与 exporter 重复。未运行测试。

**2026-11-08 LimitedInputStream 重复边界合并：** `BackupCodecTest` 和专属 `LimitedInputStreamTest` 都直接断言 exact-limit 接受与下一字节超限拒绝；前者不属于 ZIP codec 合同，且未覆盖另一文件的 zero-length-at-limit 边界。将 exact/oversize 与 zero-length 场景并为 `LimitedInputStreamTest` 的一个 helper-owner 合同，保留 single-byte/bulk-read 和空读断言；从 `BackupCodecTest` 删除重复用例及 import，类收敛至 4 例、134 行。测试声明净减 1，未运行测试。

**2026-11-08 Core formatter 单例逻辑复核：** `ByteSizeFormatterTest` 和 `UriDisplayNameFormatterTest` 分别保护二进制存储单位格式与 SAF URI 展示名，职责不同，不合并。前者原先缺少生产 `GB` 分支断言，补入 1 GiB 输出；后者只测普通最终 URI segment，补入长文件名按默认 32 字符截断且保留 extension 的断言。未运行测试。

**2026-11-08 Entry list mapper 跨层重复性复核：** Data 的 `EntryListItemMapperTest` 验证持久化 Entity 中的 `OtpType.STEAM` 作为 typed metadata 进入 Domain `EntryListItem`，全仓没有第二个 `EntryListItemMapper.assemble()` 直接测试；App `VaultListUiMapperTest` 再将 Domain OTP type 投影成 `VaultOtpKindUiModel`，同时覆盖 UI capability/preview，属于后续层。两者属于同一数据流的不同映射 owner，不合并。保留当前工作区中 `VaultListUiMapperTest` 的既有变更；未运行测试。

**2026-11-08 DetailSessionQuery 单例交叉复核：** `DetailSessionQueryTest` 验证 `EntryAccountGraph` 关系组加 direct `RELATED_TO` 一次批量加载、结果顺序及不重复读取当前 entry；`DetailPresentationLoaderTest` 通过同一 query 验证 loader 完整 detail session、敏感字段快照与缺失条目早退；DetailViewModel 用例另测 UI owner 和迟到状态。分属查询性能/关系组合、页面装配和生命周期，不合并；唯一直接 query 测试保留。

**2026-11-08 Card style Compose 单例复核：** `CardStyleSettingsSectionTest` 沿一条真实用户交互验证 section 初始折叠、按条目类型显示相应 preview、preview 高度一致、Option 点击映射为持久化 key；`CardStyleRegistryTest` 测 list renderer 对 `EntryCardPresentation` 的 registry/default fallback。UI 交互与 registry 映射是不同 owner；Compose 方法共享一个 `InterfaceDetail` fixture 和连续展开/选择状态，不拆成重复装配的孤立快照测试，保留。

**2026-11-08 FreshDatabase smoke 重复测试删除：** `FreshDatabaseTest` 只经 `TestDatabaseFactory.inMemoryDatabaseBuilder()` 打开新库、执行空 transaction 并断言 writable DB 已 open。至少 9 个同源 Room DAO/事务仪器测试也使用同一 factory，并执行真实查询、插入、约束或回滚；它们必然经过同一 open/schema 初始化路径。没有独立迁移或 schema assertion，删除该单例 smoke 文件；保留旧 schema fail-closed 测试和 DAO schema 行为合同。测试声明 -1；未运行仪器测试。

**2026-11-08 EntryLink 约束测试断言复核：** `EntryLinkConstraintDaoTest` 原先只要求重复 endpoint triple upsert “失败”，任意 DB 打开、fixture 或查询错误也可能满足该断言。对照 `EntryLinkEntity` 的唯一索引和 `EntryLinkCommandDao.upsert()` 的 update-then-insert 路径后，改为沿异常 cause 链断言 SQLite 报告 `entry_links(sourceEntryId, targetEntryId, relationType)` 唯一约束冲突；后续既有行内容、相同 linkId 更新 `updatedAt` 与 endpoint 删除级联断言保持不变。没有增删测试；未运行仪器测试。

**2026-11-08 Attachment reference 数据库约束断言复核：** `AttachmentReferenceLifecycleDaoTest` 的三处 `runCatching().isFailure` 分别改为检查异常 cause 链包含实际 owner 错误：删除仍被引用的资源和引用不存在的 Entry 要求 SQLite foreign-key failure，引用 `PENDING_GC` 资源要求 `AttachmentReferenceConstraintCallback` 的 `invalid attachment ref state` trigger。共享本类断言 helper 不改变已有资源保留/回收状态检查；没有增删测试，未运行仪器测试。

**2026-11-08 DataStore 映射小测试 owner 复核：** `AuthenticationCredentialSnapshotMappingTest` 用 envelope 加 active alias 推导认证方法可用快照；`BiometricConfigurationMappingTest` 将已提交 binding 的 invalidate-on-enrollment 策略投影为 `BiometricConfiguration`，并忽略尚未提交的 rotation journal；二者针对不同映射和状态源，保留独立。`SettingsDataStoreReadTest` 只为 DataStore flow 标记 protobuf corruption，同时保证 IO 与 cancellation 原样传播，不与上述快照读取重复。

**2026-11-08 Android Keystore 单例仪器测试复核：** `AndroidKeystoreLogKeyWrapperTest` 是唯一实际使用 `AndroidKeyStore` 的 wrapper round-trip；`EncryptedLogRecordStoreTest` 注入软件 fake，覆盖分页解密失败与文件生命周期，无法替代平台 Keystore 合同。该单例不可删除；发现固定 alias 在仪器环境持久化会让后续运行跳过创建分支，因此测试前清理旧 key、`@After` 删除测试 key，并将 alias 常量仅在模块内开放给测试清理。未运行仪器测试。

**2026-11-08 Core MemoryCleaner 与 Domain OwnedChars 测试归属校正：** `MemoryCleanerTest` 的第二例只调用 `OwnedChars.toCharArray()/wipe()`，不触及 `MemoryCleaner`，且全仓原先没有 `OwnedChars` 直接测试。将其原断言迁至 `domain/.../sensitive/OwnedCharsTest`；Core 测试仅保留验证 `MemoryCleaner` 覆盖调用方 byte/char array 的方法。行为断言总数不变，未运行测试。

**2026-11-08 Permission request history 重复键测试删除：** `request history keys remain explicit and stable` 只直接比较 CAMERA/POST_NOTIFICATIONS 两个字符串；同类 `markRequested()` 测试已经经生产 `requestHistoryKey` 写入并断言这两个精确偏好键，旧键读取测试亦用同名存储值。删除重复单例，并移除首次写入测试中无写次数观察、无状态差异断言的重复 `markRequested(CAMERA)`；保留首次写入、另一权限不受影响和 legacy read 合同。测试声明净减 1；未运行测试。

**2026-11-08 Core 小型测试 owner 续核：** `ExceptionMapperTest` 保护错误码/原因/Throwable-family 分类，`ExceptionSnapshotFactoryTest` 保护诊断栈帧和 cause 脱敏，输出相邻但职责不同；`FieldEncryptorTest` 覆盖随机 nonce、AAD 验证和畸形密文拒绝；`InputActionButtonTest` 约束提交动作与输入折叠顺序；`DefaultPermissionRequestArbiterTest` 约束租约独占及旧租约重复 release，均未发现同 owner 重复。Core `MemoryCleanerTest` 中 `OwnedChars` 测试已迁至其 Domain owner。

**2026-11-08 AppPasswordPolicy 测试归属与合并：** `app` 模块中的 `AppPasswordPolicyTest` 实际直接测 Domain 策略 `acceptsLength()`；`AuthenticationTest` 也混入同一策略的 `validate()` 断言。将长度下界/接受边界和完整 violation 集合归并到 `domain.access.policy.AppPasswordPolicyTest`，从 `AuthenticationTest` 移除外部策略用例，并删除 app 模块的错位副本。三个原有 `@Test` 断言均保留，认证模型测试 owner 收敛。

**2026-11-08 Domain codec/presence 测试逻辑校准：** `OtpSecretCodecTest` 原先仅直接断言内部 BASE32 encode/decode 和内部 malformed decode；在既有用例中补入公开 `canonicalBase32()` 的 BASE32 idempotence、有效 BASE64→BASE32 以及两种编码非法 secret 传播断言，不增加测试方法。`SensitiveFieldPresenceTest` 的断言只检查声明键成员/非成员，不观察对象结构本身；将方法名收窄到实际验证的 key membership。未运行测试。

**2026-11-08 Vault AddType 路由断言去重：** `VaultListUiMapperTest` 通过 mapper 后的 creation/filter option 字段验证 UI 投影；其 route-present/route-null 检查在 `VaultAddTypeUiModelTest` 已对全部 `AddType` 穷举相同的 `VaultEditorDestinationRegistry.routeFor()` 映射，并验证 creatable set。删除 mapper 用例中的重复 route 查验与精确列表后的非空断言，保留 creation option 的精确顺序以及完整 filter option/selection projection；既有 category-search 与 typed `featureType` 工作区改动一并保留。测试声明不变。

**2026-11-08 Vault editor seed/navigation 小测试交叉复核：** `EntryEditorSeedStoreTest` 直接测 token store 的一次性消费、替换失效与 clear；`EntryEditorSeedCoordinatorTest` 覆盖 secure-session/reauthentication/VM-clear ownership、导航异常清理日志、旧导航失败不删除新 token 及取消/fatal 传播，分属 store 和协调器 owner。`VaultNavigationTest` 测 Request 到单次导航路径及 Wi-Fi seed 不进入路由，`VaultAddTypeUiModelTest` 测全部 AddType 的显示/过滤/路由合同，`VaultEditorDestinationRegistryTest` 测 full-form edit route 能力，调用边界不同。Trash reducer 的 operation/content 独立状态与 `TrashUiMapper` 的领域条目投影也不重复。导航协调器工作区改动已提交到 `efed93b4e`；本次路由断言去重仍未暂存。

**2026-11-08 App card/tag/sensitive 小测试 owner 续核：** Domain `CardExpiryTest` 验证持久化 expiry 解析和构造约束；App `CardExpirySelectionTest` 验证 UI year picker 可选范围排除过期年份，保留不同边界。`DetailTagSuggestionsTest` 直接保护排序/大小写/已选项过滤，Reducer 测试另保护 mutation 输入到 state suggestions 的 wiring；`SensitivePasswordDraftTest` 的 Compose-owned buffer 与 Domain `OwnedChars` 虽均擦除字符并提供副本，但生命周期 owner 与持有模型不同，不合并。App shapes 仅投影归一半径到 Compose shape 尺度，Domain 约束测试只管输入规范化，分层保留。

**2026-11-08 Security DEK 测试 fixture 清理：** `VerificationTagTest` 的随机 DEK（以及已创建 verification tag）此前仅在断言成功后自然离开作用域；三例现将 fixture 清零放入 `finally`。`AutofillInboxKeyMaterialCodecTest`、`AutofillInboxRecordCodecTest` 与 Android provider 集成测试的固定 DEK 改由 JUnit `@After` 清理，每个测试实例都独立生成 fixture；不改变测试合同或数量。JVM codec 测试保护编码/格式/认证错误分支，Android test 仍保留真实 provider key generation/decrypt 与 record round-trip，二者不重复。

**2026-11-08 Security session/DEK 小测试 owner 交叉复核：** `VerificationTagTest` 直接检查 GCM 标记正确 key 可验证、错误 key 触发 tag failure、截断输入提前拒绝；`DekManagerTest` 通过 bootstrap/session 入口验证密钥管理、恢复与日志，保留底层原语测试。`DatabaseSessionRetryCoordinatorTest` 保护重试编排顺序与锁状态，`SessionOwnershipPolicyTest` 测 AuthenticationPurpose→state/lock policy，`AuthenticationSessionReusePolicyTest` 测储存解锁和 fresh-auth 条件下的复用决定，三个生产 owner 不同。未发现可确认重复项；未运行测试。

**2026-11-08 buildSrc verifier 正例合并与规则精度：** `PresentationUiApiVerifierTest` 三个正例分别只替换 feature/shared/settings UI 路径和函数名，重复验证同一“business parameter > 8”报告；合并为一个三源样例，继续覆盖全部三个扫描作用域及 settings Compose plumbing 不计入业务参数，测试方法净减 2。`DiOwnershipPolicyTest` 的 feature presentation/Data binding 样例原只断言 `PRESENTATION_DI_MODULE`，输入中的 Data import 违规未被断言；改为同时精确断言 `LAYER_FEATURE_PRESENTATION` 与 `PRESENTATION_DI_MODULE`，保留与纯 UI `@Module` 样例不同的层级边界覆盖。不运行测试。

**2026-11-08 Sensitive revision restore exact-set 边界补齐：** `SensitiveRevisionRestorePolicy.isExactAuthorization()` 的实现要求授权集合与 current/historical affected-field union 完全相等；原用例只保护缺字段拒绝与精确集合接受。新增同一用例对额外 `PASSWORD` 授权字段的拒绝断言，覆盖“不得多授权”，不增加测试方法。

**兼容/冗余清理复核更正（2026-11-08）：** 随后调用闭包审计发现 `isExactAuthorization()` 只被其独立测试调用，生产恢复路径并不依赖它；真实授权范围由 `RestoreEntryRevisionUseCase` 使用 `affectedFields()` 形成，并已有用例断言准确字段集合以及空集合跳过授权。故删除未接入生产的 helper 与重复 policy test file；上条记录是当时工作区状态，不能视为当前 API/覆盖事实。

**2026-11-08 FieldEncryptor fixture 清理：** `FieldEncryptorTest` 两个用例原先在断言全部通过后才擦除测试持有的原始 key 数组；将 key 擦除放入 `finally`，确保断言失败时也清理 fixture。生产 `FieldEncryptor` 已分别在加/解密的 `finally` 擦除 provider 返回的 key 副本和明文缓冲；测试不改变合同、数量不变。

**2026-11-08 App notice / page-effect 小测试 owner 交叉复核：** `LocalAlertPolicyTest` 测连续数据库失败聚合与十分钟失效；`NoticeLevelTest` 是 toast 时长映射；`DefaultInAppNoticeSinkTest` 验证队列溢出错误及已排队通知保留；`DefaultNoticeDeduplicatorTest` 验证 claim 去重/释放/过期。`PageEffectsTest` 检查页面 visit owner 与有界非阻塞发射，`PageEffectLifecycleTest` 经过真实 Lifecycle 检查 STARTED 可见性、暂停续发和退出时 stale effect 丢弃；虽然存在相似的旧 owner 丢弃断言，后一例覆盖 lifecycle 收集闭包，不是重复状态单测。`ObservabilityAppErrorReporterTest` 则覆盖错误 ID 不外泄和边界上报折叠，与 Core 的分类/脱敏快照分别处于报告、分类和快照层。均保留；未运行测试。

**2026-11-08 全仓扫描基线刷新：** 当前 419 个 Kotlin/Java test/androidTest 源文件、402 个含 `@Test` 文件、2,113 个标注；模块文件/标注为 app 214/1,115、buildSrc 12/126、core 18/70、data 48/222、domain 39/200、feature 32/165、runtime 2/34、security 37/181。相较前次可见快照，`OwnedCharsTest` 迁移和 `AppPasswordPolicyTest` owner 合并使 app/domain 测试源码数量净变更 -1/+2；permission history 删除重复用例后总标注 -1，随后 AppPasswordPolicy 用例迁移保持总标注不变；本轮 buildSrc verifier 重复正例合并减少 2 个标注。单测文件 67 个、≤3 例且≤100 行文件 146 个。此数字仅是候选索引，逻辑审查继续中。

**2026-11-08 Autofill 输入与小型策略测试逻辑复核：** `AutofillInputTypeClassifierTest`、`InlineSuggestionSlotsTest` 与 `AccessibilityTargetPolicyTest` 分属 Android input bitmask 分类、legacy inline slot 算法、accessibility capture 的 package/window/age 限定；断言语义不同，保留。输入分类器测试原只覆盖普通文本密码，补为普通/可见/web 三种 variation 并逐个断言名称和密码识别，不增加测试方法。目标策略测试补齐 30,000ms 接受边界及未来 capture 时间拒绝，仍在原测试中。`AccessibilityFillLaunchTest` 的启动前取消走 completion cleanup，启动后取消走协程 catch 与 completion 的幂等 cleanup；另保留普通 launch exception 报告路径，三种生命周期/错误入口相异，不合并。未运行测试。

**2026-11-08 Autofill result/traversal/inbox 测试 owner 续核：** `AutofillActivityResultClaimTest` 分别覆盖平台提交前释放与提交后终态；在既有提交前失败用例补充 `AssertionError` 原样传播且释放 claim，覆盖 production `catch(Throwable)` 的 fatal 分支，不新增测试方法。`BoundedPreOrderTraversalTest` 的前序顺序、节点预算和深度预算断言对应独立算法合同；`AutofillInboxImportOutcomeMappingTest` 穷尽 Saved/InvalidDraft/Failed 到 Imported/Invalid/Retry 的映射，无重复。两个 Android 测试分别实测 credential PendingIntent 的系统身份/可变性与测试攻击 Activity 经 Android 系统送达结构的行为；后者不覆盖 Passly production service 的 provider 绕过认证合同，按测试实际边界保留且不作更强安全结论。未运行测试。

**2026-11-08 Core permission history 冗余断言清理：** `SharedPreferencesPermissionRequestHistoryTest` 的 legacy-key 读取用例把两个历史值预置为 `true` 后，又对 CAMERA 重写 `true` 并读回相同状态；该段不验证新行为，且另一首次写入用例已验证写入与 POST_NOTIFICATIONS 独立性。删除冗余 mark/read 断言并将测试名收窄为读取已存历史键，保留旧 SharedPreferences key 的读取保护；测试方法数不变。未运行测试。

**2026-11-08 Core permission/clipboard/privacy/metrics 逻辑交叉复核：** `RuntimePermissionAccessResolverTest` 对 granted、not-applicable、首次拒绝、可 rationale 拒绝和需设置拒绝覆盖 resolver 的不同分支；`ClipboardClearSchedulerTest` 分开保护延时、替换、显式取消、并发发布时旧 timer 替换，以及 clear 已开始时新 clip 发布串行化；`PrivacyPolicyTest` 区分字段/值拒绝、identifier HMAC/失效、namespace mismatch、throw-vs-reject、错误快照脱敏与 pending model 字符串脱敏。`EventAggregatorTest` 的聚合维度、LRU/window、采样豁免、sink 重入和 unchanged-state 限频分别保护不同状态更新路径。与相邻测试 owner 不重复，均保留；未运行测试。

**2026-11-08 Core observability/gesture owner 续核：** `LogContractTest` 约束标识格式、`LogValue` 对外构造形态、warn/error 签名和异常符号边界；`DefaultLoggerTest` 覆盖 sanitizer 单次调用/多 sink 同记录、trace correlation 与 typed statement 映射；`TracePropagationTest` 分别覆盖嵌套/dispatcher 上下文、span 终结、token expiry/session/capacity/单次消费与 lifecycle sink 故障；`SwipeActionContainerTest` 覆盖 reveal/trigger 比例、可用方向、原始 drag、视觉阻尼和 progress clamp。它们按 API contract、logger、trace continuation、gesture 算法分属不同 owner；未发现同合同重复。Core 当前 18 个测试源码均已按直接生产 owner 交叉复核；未运行测试。

**2026-11-08 Domain link/entry 小型测试范围校准：** `EntryLinkPolicyTest` 的 RELATED_TO 实现允许任意 endpoint type pair，原用例只证明 ACCOUNT→OTP 一个组合；在原用例中改为穷举全部 EntryType 配对，未增加方法。`EntryTest` 正例仅构造了一个 LOGIN credential，方法名原称“exactly one”超出断言可证明范围，收窄为接受 login credential。`EntryLinkTest` 仍分别保护 RELATED_TO 端点 canonicalization、方向关系保持和同 ID self-link 拒绝；`EntryAccountGraphTest` 测图查询，`StoredQueryPreferenceParsingTest` 测持久化键 parse 三态，owner 不重叠。Card CVV ASCII/长度/空值规则与 Entry 构造校验、Activity revision/lifecycle 合同分别保留。未运行测试。

**2026-11-08 Data 存储小测试 owner 交叉复核：** `StorageEnumMappingsTest` 轮询所有 enum round-trip；未知键失败断言补齐 EntryType、EntryRelationType、RevisionChange decoder，不新增方法。Diagnostics recording mapper、Clipboard policy proto mapper、authentication attempt DataStore store 分别保护独立持久化字段/默认和并发更新合同，不合并。`EntryContentSnapshotCodecTest` 对应 general snapshot 的结构保留、secret redaction 和 format ID；`SensitiveRevisionSnapshotCodecTest` 对应 opaque sensitive ciphertext/keyVersion、keys-only 与 fingerprint，数据含义和格式 owner 不同。`DatabaseReadAccessTest` 的 Unconfined 全状态投影与调度器下快速 lock/reopen generation 观察分别验证状态组合和 StateFlow conflation 下恢复版本；`BestEffortBackupCleanupLoggingTest` 分别保护普通 logger failure 被吞、取消与 fatal Error 传播。未发现重复；未运行测试。

**2026-11-08 App Favicon/search 小型 UI 测试 owner 对照：** `DetailFaviconPresentationTest` 测编辑器状态到组件 UI model 的投影，`DetailFaviconMappingsTest` 测底层处理异常到稳定 UI 错误的映射，输入/输出和调用位置不同，不合并。`VaultSearchStateHolderTest` 覆盖顶栏 focus/request/clear 交互，`VaultQueryStateTest` 覆盖页面查询的 settings failure、规范化、generation cancellation、hierarchy 和 debounce；前者是 UI interaction owner，后者是 ViewModel query Flow owner，保留两层。未运行测试。

**2026-11-08 App Trash/permission UI 小测试 owner 对照：** 删除后 `TrashReducerTest` 仍只验证 busy/error/content 状态转换，`TrashUiMapperTest` 验证 Domain EntryListItem 到 UI-only Trash item 的字段投影；与 Data `TrashDaoTest`/revision lifecycle 持久化终态分层。`RuntimePermissionCatalogTest` 验证 Android manifest permission 名与 API 33 applicability，Core `RuntimePermissionAccessResolverTest` 计算 granted/denied/settings-required policy，二者不是重复映射。`PermissionManagementUiMapperTest` 只投影运行时、目录和 manifest 信息；`PermissionManagementViewModelTest` 则验证 DataStore 失败可见与 retry 重新订阅，保留。未运行测试。

**2026-11-08 App nested scroll/FAB gesture 逻辑补验：** `PrioritizedNestedScrollConnectionTest` 原只验证 onPreScroll 的优先消费；现增加一个用例验证 onPostScroll、onPreFling、onPostFling 都将 priority 消费后的剩余量及累计 consumed 值传给 secondary，测试方法 +1。`FabVisibilityGestureStateTest` 原覆盖阈值两侧的重复方向事件，在现有用例加入恰好 ±1f 不触发可见性变化。两者分别拥有 scroll-chain 调度与 FAB visibility 状态，不合并；未运行测试。

**2026-11-08 App Wi-Fi QR 仪器测试 owner 对照：** `EntryQrCodeTest.unicodeNetworkCodeDecodesFromRealBitmapWithIndependentWifiParser` 实际通过 Android bitmap encoder、ZXing QR reader 和 Wifi parser 验证端到端像素/转义 round-trip；Domain formatter/parser 用例只验证 payload 文本协议，不重复此平台编码合同。另一例验证 DetailBody 对已保存 Wi-Fi 条目显示 QR action 并发出 `ShowEntryQr`；生产 UI wiring owner 独立，均保留。未运行仪器测试。

**2026-11-08 App AddEntry BottomSheet UI owner 对照：** `AddEntryBottomSheetTest` 通过 Compose 验证仅渲染传入选项，并验证选择回调和 dismiss；`VaultAddTypeUiModelTest` 直接穷举类型 metadata/filter/registry，`VaultNavigationTest` 验证 navigation request 到 route 的单次跳转及 opaque Wi-Fi seed。UI rendering、type registry 和导航编排是不同生产边界，不合并。未运行仪器测试。

**2026-11-08 Backup notice 参数映射断言补全：** `BackupNoticeMapperTest.successfulImportMapsAllSummaryCountsToSafeNoticeArguments` 原只检查 imported 与 pruned-resource 两项，实际题名所称“all”未被证明。对照 mapper 的六个参数键，将断言改为完整 map equality，覆盖 existing、skipped、ignored-field、pruned-link 和 pruned-resource 值，并能发现多出或遗漏参数；测试方法数不变。未运行测试。

**2026-11-08 Backup byte-limit stream owner 交叉复核：** `LimitedInputStreamTest` 与 `LimitedOutputStreamTest` 都有 exact/over-limit 样例，但分别保护读取端允许探测下一字节以识别超限、写入端必须先拒绝整次超限 buffer，生产算法和副作用合同不同，保留两套。`LimitedOutputStream.write(int)` 此前未直接覆盖；在原有两个测试中补入单字节 exact-limit 与超限不写入断言，测试方法数不变。未运行测试。

**2026-11-08 Backup keyed Wi-Fi mapper 逻辑补验：** `KeyedEntryArchiveMapperTest` 原 Wi-Fi export 正例只断言 WpaPersonal→`WPA`，虽然实现读取 `WifiSecurityType.storedValue`，未证明 Open/WEP 键。扩展原用例遍历所有 enum，逐项核对 string fields 与稳定 security key，并用支持的 `WPA2` archive alias 验证导入归一为 WpaPersonal；缺失及未知 security 的拒绝用例保持。与 `BackupSnapshotArchiveMapperTest` 的资源缓冲清理/重复 resource ID owner 不同，不合并；测试方法数不变，未运行测试。

**2026-11-08 Favicon crop/bitmap 测试 owner 复核：** `FaviconCropTransformTest` 验证用户手势的 zoom/pan 约束与归一化 crop request；`FaviconBitmapTransformerTest` 验证 Android 图片解码后的实际尺寸、EXIF、GIF 首帧、透明度及像素裁剪，输入阶段/输出性质不同，不合并。未运行测试。

**2026-11-08 全仓测试计数刷新：** 当前 419 个 Kotlin/Java test/androidTest 源文件、402 个含 `@Test` 文件、2,119 个标注；按模块为 app 221/1,122、buildSrc 13/126、core 18/70、data 51/221、domain 39/200、feature 35/165、runtime 2/34、security 40/181。相较上一快照 app +2；本轮未增添测试方法，暂无法归因这两项工作树增量，扫描继续。

**2026-11-08 AddType filter 与 settings mapper 小测试对照：** `AddTypeFilterTest` 保护 PASSWORD→五种 LOGIN credential EntryType 和 ID_CARD→三类证件 EntryType 的领域分组；`VaultAddTypeUiModelTest` 测 AddType 选择投影/filter toggle，类型层不同。Security 与 Privacy settings mapper 虽同包，但分别投影认证/锁屏状态及剪贴板/隐私 flags，UI model、Route 消费方和字段均不同，不机械合并。未运行测试。

**2026-11-08 全仓测试计数刷新：** 当前 419 个 Kotlin/Java test/androidTest 源文件、402 个含 `@Test` 文件、2,117 个标注；模块 app 221/1,120、buildSrc 13/126、core 18/70、data 51/221、domain 39/200、feature 35/165、runtime 2/34、security 40/181。相较上次快照 app 增 1 个标注；本轮只改了既有测试断言，没有新增 `@Test`，该增量未归因，保留为工作树变化。全量逻辑审查继续中。

**2026-11-08 全仓测试计数刷新：** 当前工作树为 419 个 Kotlin/Java test/androidTest 源文件、402 个含 `@Test` 文件、2,116 个标注；按模块为 app 221/1,119、buildSrc 13/126、core 18/70、data 51/221、domain 39/200、feature 35/165、runtime 2/34、security 40/181。相较本页前次 2,114 快照，app +3（其中本轮新建的 nested-scroll 回调覆盖 +1，其余 +2 仍未归因）、data -1（删除 Trash 空回收站重复用例）；净增 2。此计数仅为索引，全量逻辑审查继续中。

**2026-11-08 全仓扫描计数复核：** 当前仍有 419 个 Kotlin/Java test/androidTest 源文件、402 个含 `@Test` 文件，共 2,114 个 `@Test` 标注；模块文件/标注为 app 221/1,116、buildSrc 13/126、core 18/70、data 51/222、domain 39/200、feature 35/165、runtime 2/34、security 40/181。相较本轮前一次计数，唯一可见变化为 app 标注 +1，非本轮修改文件；保留为工作树变化，不归因于清理。扫描与逻辑复核继续中。

**2026-11-08 Data trash DAO 重复用例删除：** `TrashDaoTest.emptyTrash_keepsActiveEntries` 与 `EntryRevisionLifecycleDaoTest.emptyTrashHistoryDelete_keepsActiveEntryHistory` 均调用 `deleteAllDeleted()` 并验证活动 Entry 保留；后者还核对被删 Entry 的 revision 清理与活动 Entry 的 revision 保留，覆盖更完整的 DAO/事务终态。删除较弱的 Trash 用例；该类保留 soft-delete 的旧版本拒绝、restore、永久删除路径，测试声明净减 1。未运行仪器测试。

**2026-11-08 Password form 与错误本地化测试复核：** `PasswordFormMapperTest.fromEntryRestoresFieldsAndRetainsAdditionalAssociatedAppsInTheUpdateDraft` 的测试名覆盖恢复字段，但原断言仅验证 password、username、application IDs 和 notes；现于原用例补齐 title、primary URL、tags、选中的/保留的应用 ID、隐藏密码状态及完整 Draft 字段，测试方法数不变。`UiErrorLocalizationTest` 直接验证同一资源 ID 在英语与简体中文配置下解析文本；`UiErrorMessageTest` 验证错误到资源 ID 的代码映射，两者覆盖资源解析和映射两个边界，保留。未运行测试。

**2026-11-08 Settings UI mapper 测试逻辑复核：** `InterfaceUiMapperTest` 与 `NotificationSettingsUiMapperTest` 输入状态/输出模型不同，分别映射外观偏好与通知 topic，不是同一映射的重复覆盖。通知 mapper 测试原先只验证通知开关、topic 顺序和剪贴板禁用；现断言 optional messages flag，并逐项比对所有 topic 的 enabled 值与输入策略，测试方法数不变。未运行测试。

**2026-11-08 OTP form mapper 回填逻辑复核：** `OtpFormMapperTest.fromEntryRestoresCompleteOtpConfigurationForEditing` 原只断言 secret、type、counter、issuer；现同一用例覆盖全部 OtpConfig 表单字段、编辑时清空过期 URI 文本，以及重新生成 Draft 的字段（含 HOTP 无 period、long counter、标签和 notes），测试方法数不变。与 `OtpConfigInputTest` 的表单字符串校验/规范化、`OtpAuthUriCodecTest` 的协议解析导入合同不同，均保留。未运行测试。

**2026-11-08 Bank card save-capability 重复用例合并：** `AddBankCardViewModelTest.validCardNumberEnablesSaveAndKeepsValidationOutsideMapper` 已在有效输入触发真实 ViewModel 后断言 canSave=true；相邻 `saveCapabilityIsDerivedFromValidationAndSavePhase` 独立构造同类有效卡表单，再次断言 idle=true 与 saving=false。将 saving=false 合入 ViewModel 用例并删除较弱的重复方法，保留卡号校验 owner 与 save phase 两种断言；测试方法净减 1。未运行测试。

**2026-11-08 Bank card mapper 回填逻辑复核：** `BankCardFormMapperTest.fromEntryRestoresProtectedAndOptionalCardFields` 原只断言卡号、CVV、PIN、到期日；现补齐 mapper 实际拥有的全部 form/draft 字段和从可见状态切回默认隐藏的断言，测试方法数不变。ViewModel 的 protected-secret 解封与持久化测试覆盖另一 owner，保留。复核发现 `CardCredential.paymentPlatform` 仍存在于 Domain model/type definition/read path/backup，但银行卡 editor form/mapper 没有字段，编辑后可能丢失旧值；作为生产逻辑缺口待后续确认与处理，不用测试把丢失行为固化。未运行测试。

**2026-11-08 Card expiry 测试用例合并：** `CardExpirySelectionTest` 两个用例对同一 `currentYear=2026` 范围分别断言未来上限和过期年份排除，合并为一条边界合同（当前年、+20 年、范围内及过期排除），测试方法净减 1。Domain `CardExpiryTest` 覆盖值构造、规范化序列化与严格解析，和 UI picker 的年份范围不是重复，保留。未运行测试。

**2026-11-08 Wi-Fi editor mapper 测试去重与回填复核：** `WifiFormMapperTest` 完整表单映射用例已断言同一 SSID 同时生成 TITLE/WIFI_SSID；独立 `ssid is the single source for wifi title` 对单个样例重复相同字段合同，删除并由原完整映射用例承接，测试方法净减 1。回填用例补齐表单/草稿的 SSID、security、隐藏/密码、敏感显示重置、notes 和 tags。Open 安全清密码的手工选择与扫描导入是两个独立入口，分别保留；Domain credential validation、QR payload parser/formatter 与 mapper 输入/输出边界不同，保留。未运行测试。

**2026-11-08 Vault list item mapper 断言补齐：** `entryMappingPreservesEveryRenderedValue` 原名声称覆盖每个渲染值，但漏断言 icon local URI model、visual category、password secondary/supporting text。现针对同一 EntryListItem 补齐 mapper 输出断言；分类器本身由独立 `EntryVisualCategoryClassifierTest` 覆盖，卡片组件消费由 CardStyle 测试覆盖，owner 不合并。未增测试方法，未运行测试。

**2026-11-08 Vault list mapper 测试命名与 owner 复核：** creation route 存在性仍由 `VaultAddTypeUiModelTest.allTypesCanBeFilteredButOnlyFourHaveCreationDestinations` 穷举 registry，mapper 测试中的重复路由断言已从现有工作区变更中移除，保留 registry 的单一合同 owner。将 `readingAndFilteringTypesDoNotRequireACreationRoute` 改名为 `allEntryTypesAreAvailableAsFilters`（其只断言选项与选择投影），将 `defaultSortShowsNewestCreatedEntries` 改名为 `createdAtDescendingSortMapsToNewestFirst`（测试输入由 helper 显式构造，并非默认值）。未增方法，未运行测试。

**2026-11-08 Vault swipe copy 路由测试合并：** `fieldCopyCarriesTheDomainEntryType` 与 `passwordCopyTargetsOtpWhenTheEntryHasOtp` 均只覆盖 `resolveCopyRequest` 的单一路径，合并为一条合同并断言普通密码字段、OTP 条目密码改走 OTP request、OTP 条目用户名仍携带 EntryType/FieldKey。dispatch 测试补齐 COPY_USERNAME，并分别断言 copy 与 delete side effects；DISABLED 无副作用用例保留。测试方法净减 1；Entry copy use case 测试验证数据读取/持久化和活动记录，owner 不同。未运行测试。

**2026-11-08 Card style registry 持久化键测试补全：** `CardStyleRegistry.entryTypeKey` 映射所有 17 个 `EntryType`，键被设置 UI 持久化。测试原只固定 LOGIN/BANK_CARD/OTP 三项；现以穷举 map equality 固定全部 keys，并检查 expected map 覆盖全 enum，未增加测试方法。Settings Compose section 测试验证预览渲染/点击回调，和注册表兼容性选择逻辑不同，保留。未运行测试。

**2026-11-08 Vault TopBar More 菜单测试重复核对：** 全仓搜索确认仅 `VaultTopBarMenuTest` 有 UI More 菜单用例；`VaultNavigationTest` 测 destination request 到路由的策略，owner/输入输出不同，保留。 `VaultTopBarMenuTest` 的一次 Compose 场景验证 popup 可测量、scan/settings route callback 及 sort submenu，未找到重复菜单 UI 测试。

**2026-11-08 Vault OTP provider 状态映射断言补全：** `VaultOtpStateProviderTest` 原仅验证 Ready 状态和最近订阅回调；现扩展同一 StateFlow 覆盖 Pending、Failed、缺失 ID→null，以及 Ready 的 code/progress，方法数不变。`VaultOtpSubscriptionEffectTest` 用 fake provider 验证 Compose 可见条件与 Composition 生命周期订阅/退订，和 provider 映射不同层，保留。未运行测试。

**2026-11-08 Data attachment/revision/schema DAO owner 复核：** `AttachmentReferenceLifecycleDaoTest` 验证 SQL 引用约束与 revision attachment 保留；`EntryRevisionLifecycleDaoTest` 验证 per-entry/global retention、软删、永久删除和 empty trash 的历史终态；`DatabaseSchemaCompatibilityTest` 重建旧 attachment table 并验证 Room 拒绝打开时数据库/哨兵 entry 保留。测试操作对象、事务及失败边界不同，未发现可合并的重复用例。`EntryLinkPolicyTest` 判定 relation/type 组合可否创建；`EntryLinkConstraintDaoTest` 则验证 SQLite triple unique、同 link upsert 与外键 cascade，分别保留。未运行仪器测试。

**2026-11-08 Vault session Security 测试 owner 复核：** Background lock 的 timeout cancel、seal 已启动后不可取消、后台锁关闭时 idle timeout 保留、idle 自身锁定不误报分别验证不同触发窗口/operation owner，Lifecycle fatal/cancellation cleanup 与 Observability terminal event/assertions 也不重复。Inbox provisioning 只对 `UNLOCK_VAULT` 创建 key；有效的 `AUTOFILL` 会话不创建，其他 purpose 不适用于普通会话发布。取消异常断言按协程可恢复异常身份检查 message 与 suppressed operation failure；Recovery credential fatal-cleanup 测试保持 JUnit `void` 返回签名。Security 179 项全量单测通过，`verifyArchitecture` 通过。

**2026-11-08 Security biometric recovery 与认证可用性测试逻辑复核：** `BiometricRotationCandidateTest` 验证运行中 rotation 的 commit/rollback 与 caller error priority；`BiometricRotationReconcilerTest` 验证启动恢复 journal 与 deferred cleanup，阶段与 owner 不同，保留。`CredentialAttemptLimiterTest` 持久化 attempt reservation/backoff，Data Proto store 测试只验证序列化存储边界，保留。`AuthenticationAvailabilityResolverTest` 原覆盖 configured biometric 的硬件开关和其他方法保留；现加入硬件可用但所有 credential 未配置时仍返回空集，补齐 `biometricAvailable && biometricConfigured` 短路条件，不增方法数。Retry backoff test 负责独立 scheduler owner。未运行测试。

**2026-11-08 Secret dialog late-input 测试合并：** `SecretDialogSubmissionTest.lateSecretInputIsClearedWhenThereIsNoSecretRequest` 与 `secretInputIsClearedWhenDialogHasChooseMethodRequest` 都验证 `submitSecretInput` 的 `activeRequest == null` 拒绝分支，只是 request=null 与 request 为 ChooseMethod 两种输入。现合并到原测试并保留两种场景，同时断言 ChooseMethod continuation 不会被错误恢复；测试方法净减 1。inactive continuation 与 stale identity 分属另外的有效 Secret request gate，仍保留。未运行测试。

**2026-11-08 BiometricRotationCandidate cleanup-priority 分支补验：** `withBiometricRotationCandidate` 对 operation failure 与 cleanup failure 有三类优先级：普通 cleanup exception 被作为 suppressed 保留原失败、cleanup cancellation 优先且抑制原失败、fatal Error 优先并抑制原失败。原测试仅直接覆盖普通异常与 fatal Error；把 cleanup cancellation 分支补入同一测试方法，断言取消对象身份及原操作失败被抑制，不增加 `@Test` 数量。`BiometricRotationReconcilerTest` 负责启动恢复/journal 延后清理，不是 candidate helper 的相同调用阶段；保留。未运行测试。

**2026-11-08 buildSrc 源码边界扫描测试交叉复核：** `SourceBoundaryReferenceScannerTest` 直接保护 `SourceBoundaryVerifier` 的无 import 限制引用、精确 allowlist、注释/普通字符串屏蔽、字符串模板可执行表达式、跨行证据定位、转义 import 归一化及跨 Feature owner allowlist。与 `EditorSourceBoundaryVerifierTest` 的编辑器专属 mapper/被动 UI 目录规则及 `SourceBoundaryPresentationPolicyTest` 的业务源码 marker 约束输入路径和规则不同；`SourceBoundaryLayerPolicyTest` 的模块层矩阵则检查 package edge，不与扫描 tokenizer 重复。逐方法比对未发现可确认重复或失效断言；未运行测试。

**2026-11-08 buildSrc App-to-Security 重复策略用例合并：** `SourceBoundaryPolicy` 中 `APP_ADAPTER_SECURITY_IMPLEMENTATION_IMPORT` 是同一条 `/app/src/main/` 规则，唯一合法前缀是 AuthenticationHost contract，且 `PasslyApp`、`AutofillAuthActivity` 是唯二路径豁免。`SourceBoundaryModuleDependencyPolicyTest` 与 `SourceBoundaryModuleOwnershipPolicyTest` 原有四个用例分别重复覆盖该规则的两个合法路径及四个非法 app 调用点；合并为一个表驱动合同用例，保留全部输入，并精确断言规则 ID、每个违规 path/evidence 与两条合法路径，测试方法净减 3。未运行测试。

**2026-11-08 buildSrc Backup Feature 依赖测试合并：** 同一 `DefaultBackupOperationGateway.kt` 路径下，`featureImplementationCannotImportPresentation` 与 `backupFeatureCannotImportDataImplementation` 都走 `LAYER_FEATURE` 规则，分别拒绝 Feature→Presentation 与 Feature→Data 依赖；合并为一个方法，同时断言两条输入的 rule ID、path 和 evidence，测试方法净减 1。没有合并跨 Feature allowlist 或 Backup 专属 internal API 规则测试，因为其 owner/路径条件不同。未运行测试。

**2026-11-08 buildSrc Runtime Session 层级测试去重：** `runtimeSessionRejectsPlatformPersistenceAndPresentationImports` 使用 `LAYER_RUNTIME_SESSION` 的 generalRules 验证 Android、AndroidX、Data、Security、Feature/Presentation 五个禁止前缀；`packageLayerMatrixAllowsOnlyReviewedEdges` 已对同一 `runtime.session` 源路径通过 layerRules 穷举这些相同禁止前缀（另含 app/core）并验证 owner/path/evidence。generalRules 由 layerRules 加应用专属规则组成，直接项重复且没有额外分支；删除专用方法，保留矩阵覆盖，测试方法净减 1。未运行测试。

**2026-11-08 buildSrc Presentation UI 禁止依赖测试去重：** `presentationUiRejectsExactForbiddenImportPrefixes` 与 `packageLayerMatrixAllowsOnlyReviewedEdges` 在同一 presentation.ui 层级规则中重复验证 VaultUiState、Domain Entry、Data EntryRepositoryImpl 三个完全相同 import；矩阵对每项还断言 rule ID、owner、path 和导入证据。将旧用例中唯一额外的 Feature AddType import 移入矩阵，删除重复方法，保留四个依赖样例及更完整断言，测试方法净减 1。未运行测试。

**2026-11-08 FreshDatabaseTest 过时仪器测试核对：** 工作区删除的 `FreshDatabaseTest.freshSchemaOpensAndRunsTransaction` 只在 in-memory Room DB 中执行空事务并断言 writable DB 打开；现存 `DatabaseSchemaCompatibilityTest.schemaMismatchFailsOpenAndPreservesExistingEntryAndLegacyAttachmentSchema` 先经生产 `DatabaseProvider.open()` 创建新 SQLCipher 数据库、插入并读回哨兵 Entry，再重建旧 attachment 表并验证 Room 拒绝不匹配结构且保留数据库/数据，已覆盖更完整的 fresh-open 和写读路径。`TestDatabaseFactory` 也由其他 DAO 仪器测试实际使用并验证可用 schema。未发现 FreshDatabaseTest 还有独立生产合同；支持保留删除，不恢复该用例。未运行仪器测试。

**2026-11-08 Core ByteSizeFormatter 边界断言补全：** `ByteSizeFormatterTest` 原覆盖 B/KB/MB/GB 的代表值，但未断言 0、1023 B、1 KiB 与 1 MiB 单位切换；在现有用例加入这些边界，不增加测试方法。Backup `TextVaultRenderer.formatSize` 虽共享二进制单位，输出精度、单位上限和文本导出合同不同，继续由 renderer owner 测试，未抽成共用测试或生产格式。未运行测试。

**2026-11-08 ByteSizeFormatter caller层测试 owner 核对：** `StorageSettingsViewModelTest` 通过页面进入/清理后的真实 ViewModel 状态验证 size 投影和刷新；`ByteSizeFormatterTest` 是纯格式单位边界，两者不重复，保留调用层覆盖。

**2026-11-08 TraceContinuationStore 日志断言校准：** `TracePropagationTest.continuation store consumes an opaque callback key without logging it` 原用不接收日志的 empty sinks，却在测试名声称 opaque key 不会被日志记录。为该场景接入 recording `LogSink` 并断言所有实际输出记录均不含 callback key；保持 token 恢复与一次消费断言，测试方法数不变。直接 `TraceTokenRegistry` 的错误 process session/过期/未知/容量及 Logger 的 single-use 映射是不同 owner/接口层，保留；未运行测试。

**2026-11-08 URI display-name 与持久化枚举测试逻辑复核：** `UriDisplayNameFormatterTest` 的 final-segment 与 long-name/extension 用例对应不同算法分支，无可合并项；全仓仅 `BackupRoute` 使用默认 `maxLength`。自定义短长度配合长扩展名时实现可能返回超过参数长度的结果，因无当前调用方且最小长度/扩展名优先级合同未定义，登记为生产合同待确认，不添加固化行为的测试。`StorageEnumMappingsTest` 原 round-trip 穷举只能证明编码/解码成对一致，只有三种 OTP 键固定字面值；现对全部 8 种持久化 enum 补充显式 enum→stable-key 表，并在同一用例断言覆盖完整、key 唯一、正反映射一致。未知键拒绝覆盖全部 8 个 decoder，保留独立失败合同。未运行测试。

**2026-11-08 Domain EntryDraftMaterializer 重复非法枚举用例合并核对：** `unsupportedTextualValuesAreExcludedFromFailureMessages` 对 OTP_TYPE、OTP_ALGORITHM、OTP_ENCODING、WIFI_SECURITY 均断言 `InvalidEntryDraft.fields` 精确等于对应键，并断言非法输入 marker 不出现在异常文本。它已覆盖 `materializeOtp_rejectsUnsupportedAlgorithmInsteadOfUsingSha1` 与 `materializeWifi_rejectsUnsupportedSecurity` 的同一解析拒绝分支；MD5/vendor-private 仅是替换非法文本值。删除两条专用重复用例，方法净减 2。`EntryFormEditorTest` 处理 existing Entry 的编辑/保留未拥有状态；`EntryDraftMaterializerTest` 创建 materialization，不因共享 Draft DTO 合并两组 owner。未运行测试。

**2026-11-08 Domain Wi-Fi QR parser/formatter 逻辑交叉复核：** `WifiQrPayloadParserTest` 的反向输入协议兼容性、重复/畸形字段和 Unsupported/Malformed/NotWifi 分类，与 `WifiQrPayloadFormatterTest` 的序列化转义、open 密码清理及凭据校验门禁分属解析和格式化方向。formatter 的 special-character round-trip 是跨两者集成断言，不替代 parser 对 field-order/alias/error 类别的直接合同；无重复可删用例。`WifiCredentialValidationTest` 直接拥有底层凭据规则，formatter 只验证拒绝格式输出，保留边界覆盖。未运行测试。

**2026-11-08 EntryFieldEditor 拒绝结果断言精度补全：** `EntryFieldEditorTest` 原以 `result is not Updated` 判断卡号/CVV/登录密码/Wi-Fi 密码拒绝，若实现错误返回 `Unsupported` 仍会通过。对照 `EntryValidation.validateEditedField` 将用例改为精确比较 `EntryEditResult.Invalid`、FieldKey 和 REQUIRED/INVALID_LENGTH/INVALID_FORMAT，覆盖卡号空值、长度、校验和、非法字符，以及各凭据密码错误。方法数不变。`EntryEditTest` 经 `EntryEdit.SetFieldValue` 验证 command→editor 委托、Entry 更新及其余状态保留，属于调用层合同，保留；`WifiCredentialValidationTest` 直接测凭据规则，与 EntryFieldEditor 的错误结果映射分层。未运行测试。

**2026-11-08 Domain EntryTagPolicy 与 EntryEdit 标签合同复核：** `EntryTagPolicyTest` 的五个方法分别验证 separators/blank 清理、大小写去重保留首写、排序、count 与 length 限制，无同一输出合同的重复项。将数量/长度测试各扩展至恰好 20/32 的接受边界，并验证大小写重复在 count limit 前去重，未增加测试方法。`EntryEditTest.tagsEditPreservesIconAndAssociations` 检查单字段命令及无关聚合字段保留；`EntryFormEditorTest` 检查整表单替换；与 normalization policy 区别保留。调用图显示当前唯一生产调用 `DetailViewModel.SaveTags` 先 normalize 再 `EntryEdit.SetTags`，而 `EntryEdit.applyTo(SetTags)` 只拒绝 blank，未自带 count/length 约束；把 Domain 入口对超限 tag 的不变量归属记为待确认，不用测试提前固化未决策略。未运行测试。
**2026-11-08 Domain card validation test logic review:** `CardNumberValidatorTest` owns Luhn/length/ASCII/spacing/network inference, `CardCvvValidatorTest` owns optional 3/4-digit ASCII policy, and `EntryValidationTest` owns stable field/code mapping plus independent per-field aggregation; similar invalid values exercise different production owners and are retained. Tightened the formatted valid card case to assert the complete empty violation set and the multi-field invalid card case to assert the exact three violations, so extra/missing violations fail. Test method count unchanged; no tests run.
**2026-11-08 CardNumberValidator 断言逻辑收紧：** 非法字符测试原只检查返回任意 `Invalid`，长度或校验和误分类也会通过；改为精确断言 `InvalidCharacters`，保持同一测试方法覆盖 ASCII 字母、标点和全角数字。方法数不变，未运行测试。
**2026-11-08 EntryLinkPolicy 关系矩阵复核：** `EntryLinkPolicyTest` 覆盖 MEMBER_OF_ACCOUNT 的方向、RELATED_TO 的全类型组合，以及 OTP_FOR/RECOVERY_FOR 的因子来源合同；生产仓储与备份导入均调用该策略。原因子用例只验证一个允许目标，未覆盖拒绝 ACCOUNT/OTP/RECOVERY_CODE 作为受保护目标；改为共享表驱动检查两类因子的允许 LOGIN 和三个禁止目标，并移除被表驱动重复覆盖的 recovery→ACCOUNT 断言。保留三个独立关系合同及 `EntryAccountGraphTest` 的图投影测试（owner 不同）；方法数不变，未运行测试。
**2026-11-08 EntryTypeDefinitions 测试交叉核对：** 注册表的全类型/title/sensitive mapping、构造时重复/缺失拒绝、各凭据字段语义、OTP long counter 与 `EntryDraft` 字段/值类型合同由不同断言负责。`EntryDraft.withValue` 的 WIFI_HIDDEN 文本输入专门覆盖已支持字段的值类型拒绝，CARD_NUMBER 输入覆盖不支持字段拒绝；`invalidProvidedFields` 覆盖绕过 withValue 的映射入口。与 app `CreateEntryUseCaseTest` 创建编排及表单 mapper 测试的调用层断言不合并。此批未发现能证实的冗余用例，未改测试、未运行测试。
**2026-11-08 ACCOUNT 关系图跨层重复样例收敛：** `EntryAccountGraphTest` 已完整断言 direct/recovery/OTP 三种成员解析。App `DetailSessionQueryTest.account group id projection retains graph semantics` 另需覆盖 `resolveAccountGroupEntryIds` 的入口类型分支与排除当前条目的投影；删去该 App 用例重复的 recovery edge，仅保留 login/account/OTP 最小图，精确验证 account 与 login 两种入口投影及非成员空结果。保留 Domain 图算法覆盖和 App 投影合同两个 owner，方法数不变，未运行测试。
**2026-11-08 Entry aggregate and activity model tests:** `EntryTest.login accepts a login credential` only exercised the positive counterpart of `entry rejects a credential that does not match its type`; combined it into the same type/credential invariant test so one contract owns rejection and acceptance, reducing one test method. Kept ACCOUNT secret rejection separate because it is an independent constructor invariant. `EntryActivityTest` checks activity classification (RESTORE vs VERSION_RESTORED), required revision context for content events, and rejects usage-context misuse; these cover separate flags/context branches and have no duplicate owner in mapper/writer tests, so retained. No tests run.
**2026-11-08 EntryRevisionComparator 测试逻辑复核：** 9 个用例分属敏感字段 redaction、custom-field 从 hidden 转 public 的历史保护、公开 typed value、aggregate/attachment 差异、以及敏感指纹变更类别；同样的“不泄漏”断言分别覆盖 Wi-Fi 凭据、security answer、hidden custom field 与单独 protected difference 构造，不是同一执行路径。与 App revision use case 的授权/擦除/恢复委托、Data revision repository 的持久化边界分层；未发现可确认重复用例，此批保留。未运行测试。
**2026-11-08 Revision reveal/restore 层间测试交叉复核：** `RevealRevisionFieldsUseCase` 负责授权后的临时值 wipe/所有权转移，`DetailRevisionRevealStore` 负责页面 owner 替换/清空时 wipe，`DetailRevisionReducerTest` 负责 restore 状态并发与 stale action；`RestoreEntryRevisionUseCase` 测授权字段并集、当前版本和空敏感字段绕过授权。各测试输入和断言合同沿真实调用边界区分，无重复可合并项。未改测试、未运行测试。
**2026-11-08 Sensitive revision codec fixture 对齐持久化合同：** `SecretFieldStore` 写入 `SensitiveFieldKey.toStorageKey()`，codec 读取 stable storage key；`SensitiveRevisionSnapshotCodecTest` 原用 enum `.name` 生成 `EntrySecretFieldEntity.fieldKey`，当前名称虽然碰巧与显式 storage key 一样，但会把 fixture 绑到 Kotlin enum 命名。改用 schema 字符串 `CARD_NUMBER`/`CARD_CVV`/`PASSWORD`，保留工作区已有排序及 key-rotation 断言。此举与 `StorageEnumMappingsTest` 的字面 key 合同一致；未运行测试。
**2026-11-08 Revision snapshot codec 测试对照：** `EntryContentSnapshotCodecTest` 覆盖加密后的 content1 格式、低敏感 bundle 重建、敏感字段剔除和 relation 序列化；`SensitiveRevisionSnapshotCodecTest` 覆盖 ciphertext 二进制帧、field presence、keyVersion 参与 fingerprint。虽然都 round-trip revision snapshot，它们不是同一 payload/owner；版本前缀与 binary FORMAT_VERSION 也是不同持久化合同，分别保留。未运行测试。
**2026-11-08 EncryptedLogRecordStore 队列优先级重复测试合并：** DEBUG→INFO 与 INFO→WARN 两个用例重复执行同一阻塞 writer/单队列槽/淘汰低优先级/接收新记录流程；合为一个表驱动用例保留两个优先级边界，并统一精确断言写入 event、drop level 计数及计数 drain 后清零，测试方法净减 1。修复异步测试清理：release latch 与 store.close 移入 finally；WARN 输入改用普通 sample record，避免无关错误 payload。未运行测试。
**2026-11-08 LogFields sanitized boundary test 归属调整：** `LogRecordCodecTest.sanitized record boundary refuses pending unhashed identifiers` 没有调用 `LogRecordCodec`，只调用 Core `LogFields.fromSanitized`; 因此从 Data codec 测试移入 `core/observability/LogContractTest`，保留同一拒绝断言，不改变全项目测试方法总数。Data codec 测试现在只覆盖实际编码/解码合同。未运行测试。
**2026-11-08 LogRecordCodec 协议断言补全：** persisted enum 测试标题声称稳定 wire IDs，但原只固定三个 enum 的首尾值；改为固定 LogLevel/LogCategory/ExceptionKind 全量映射并逐项检查 `fromWireId` 反解，方法数不变。round-trip 用例原检查编码数据不含 `DGN-`，而 fixture 与全文件都没有该 marker，属于无效旧断言，移除并删除唯一 `assertFalse` import；记录值等价 round-trip 保留。未运行测试。
**2026-11-08 Log store retention test separation：** `retention removes expired log` 验证过期时间删除；`rotates by date and enforces file count retention` 验证日 bucket 切换后 maxFiles 保留；`rotates by size and enforces total byte retention` 验证 maxFileBytes 与 maxTotalBytes。它们作用于不同清理触发条件，暂不合并。后续可进一步核对 date/size 用例的保留记录数量断言是否足以证明边界；当前未改这些用例，未运行测试。
**2026-11-08 Log file-count retention 边界断言：** `rotates by date and enforces file count retention` 每次推进一天、写入 5 个小记录；当前配置 maxFiles=3、maxTotalBytes=3 MiB 且 5 天未触发过期删除，生产 cleanup 应保留恰好 3 个日文件。将 `<= 3` 收紧为 `== 3`，与保留记录数恰好 3 的断言共同验证日期切换与文件数淘汰；方法数不变，未运行测试。
**2026-11-08 Vault More 菜单 UI 测试逻辑补全：** 当前全仓只有一个 `VaultTopBarMenuTest` Compose 用例；它与 `VaultListUiMapperTest.sortPresetsContainCreatedAtOnlyOnce`/`VaultViewModelTest` 分别测试 popup/路由 UI、排序 mapper 和持久化状态，不是重复层。UI 用例原只打开 sort 子菜单后检查 label，且 `onAction` 为空，未验证选择是否派发；现接入 action recorder、点击 Last used 并精确断言 `SortOptionSelected(EntrySort(LAST_USED_AT, DESC))`，同时重命名反映 routes 与 sort action。方法数不变，未运行 AndroidTest。
**2026-11-08 Detail Compose 测试交叉盘点：** `RelatedEntriesSectionTest` 的 swipe 与 accessibility custom action 分别覆盖手势和无障碍解绑确认入口；dialog dismiss、confirm/retry、account/direct 分区和 account 行无 unlink action 断言不重复。Picker 的 empty/no-match、load-failure retry、active-row progress 分别对应不同 UI state；TagEditor save freeze 与 reducer、EntryQrCode bitmap/parser vs visible action 测试属组件交互/核心逻辑不同 owner。本轮只盘点，不改这些用例。
**2026-11-08 Vault sort tests 层间去重核对：** `VaultListUiMapperTest.createdAtDescendingSortMapsToNewestFirst` 验证 Domain→UI 投影，`VaultTopBarMenuTest` 验证 UI→`SortOptionSelected(EntrySort)` action，`VaultViewModelTest` 验证设置监听/写失败的状态和 paging generation；三者方向与 owner 不同。`sortPresetsContainCreatedAtOnlyOnce` 独立防止 UI 菜单重复展示 CreatedAt，未与另两项合并。未改上述既有工作区断言。

**2026-11-08 全量续扫（小型/单例测试复核）：** 按全部 `src/test`、`src/androidTest`、`src/testFixtures` Kotlin/Java 源统计，当前共 420 个文件，其中 403 个含 `@Test`、共 2,132 个声明；含测试文件按模块为 app 216、buildSrc 12、core 18、data 48、domain 38、feature 32、runtime 2、security 37。67 个文件恰有 1 个测试，其中 60 个不超过 100 行。静态计数绑定当前 dirty worktree，不代表执行结果。复核 `EntryLinkConstraintDaoTest`：端点 triple 唯一约束、相同 linkId upsert 更新时间、删除端点级联是同一 SQLite schema/integrity owner 的不同合同；`TrashDaoTest` 的 stale expectedVersion 拒绝、恢复后版本前进及永久删除验证单条 optimistic trash 生命周期；`BackupSnapshotArchiveMapperTest` 验证 archive mapping 失败时擦除所有 Domain resource buffers，未与 resource accumulator 或 gateway 的不同所有权边界重复。未发现可确认冗余/过时项，未改测试代码，未运行测试。全仓逐例复核仍进行中。

**2026-11-08 权限管理 Compose 测试逻辑补齐：** `PermissionManagementScreenTest` 原验证 runtime permission action、备份设置 action 和 capability 行不可点击；对应 ViewModel 测试覆盖读取失败及 retry 重新订阅，但 Compose 的失败态 Retry 按钮没有验证事件接线。现将同一 Compose 场景切到 `RecoverableReadState.Failed` 并精确断言 `RetryBackupDirectoryRead`；使用 `runOnIdle` 更新状态以遵守 Compose UI 线程边界，不增测试方法。与 ViewModel 的重订阅行为合同不同。未运行 AndroidTest。

**2026-11-08 Card style preview 尺寸合同补全：** `CardStyleSettingsSectionTest` 标题声称 preview frame 等尺寸，原只比较了三种预览的高度；生产代码对选项卡统一固定宽度，并让预览 frame 填满卡片。现对 Login Password、OTP Default、OTP TOTP 三个 frame 同时精确比较 width 和 height，测试方法数不变。未运行 AndroidTest。

**2026-11-08 编辑器焦点测试 save action 逻辑复核：** `EntryFocusBehaviorTest` 的标题覆盖 screen/save modifier，但原只断言 save tag 存在并可见，没有确认 modifier 最终仍落在可点击的 Save FAB 上。当前状态 `title` 与 `password` 非空且 save phase idle，`AddPasswordUiState.canSave` 为 true；为该 tag 补充 `assertHasClickAction()`，与 IME title→username→password 焦点链独立，不增测试方法。`SensitiveFieldVisibilityTest` 仅验证 CVV 无障碍可见状态与银行卡三项字段命名，不重复焦点/save 合同。未运行 AndroidTest。

**2026-11-08 Database reset adapter 单例测试断言收紧：** 原用例只用 `is Success`/`is Failure` 检查 controller nullable error 的外层结果，coordinator 是不留痕的直通 lambda，不能证明适配器确实在独占重置边界执行，也没固定错误分类。现将同一用例改为断言成功结果精确等于 `Success(Unit)`、coordinator/controller 都各执行一次；失败使用 `IOException`，并断言映射为 `FileIOError` 且记录 `IOException` 类型。不增测试方法；UseCase 的授权范围与 VM 的页面反馈是不同 owner。未运行测试。

**2026-11-08 Autofill origin 仪器测试清理恢复核验：** `AutofillOriginAttackInstrumentedTest` 用测试 Activity + capture service 证明系统交给 service 的 structure 可带有无 DAL 关联的 domain；它不经过 Passly 生产 service 的认证/收件人确认，按测试名限定为 platform structure 行为。该用例会替换 `secure/autofill_service`；finally 原来恢复旧值但未验证，且将空值也当作“未设置”删除。现在仅对 shell 返回的字面 `null` 执行 delete，其余值（含空串）按原值写回，并查询后断言精确恢复。未运行仪器测试。

**2026-11-08 Interface UI mapper 单例复核：** `InterfaceUiMapperTest` 逐项固定 Ready → `InterfaceUiModel` 的 7 个字段，包括 preference 值、entry-card presentations 和 Domain corner-radius 范围；`InterfaceSettingsViewModelTest` 另测 repository state/read retry/write result 生命周期，不重复此纯映射合同。字段现已全部有明确断言，未发现过时或重复断言；不合并到多状态的 ViewModel 测试。未改测试代码，未运行测试。

**2026-11-08 Detail action marker 测试/类型清理：** `DetailUiActionBoundaryTest` 是唯一 `DetailTagAction` 消费者；它只把一组 tag action 强转为 `DetailUiAction` 后断言 marker 类型，生产 Route/ViewModel/reducer 无一使用该分组接口。对 Entry/Sensitive/Favicon/Revision 的四个同类 marker 做全仓搜索也无消费者。因此将具体 action 改为直接实现 `DetailUiAction`，删除五个无调用者的 marker 接口及唯一只测 tag marker 的单例测试。保留 `DetailUiAction.kt` 当前工作区里 `DetailField`、`DetailActivityFilter` 等已有迁移内容。测试源静态刷新为 419 文件、402 个含 `@Test`、2,132 个声明（app 215、buildSrc 12、core 18、data 48、domain 38、feature 32、runtime 2、security 37）；测试总量可能受并行工作区变化影响，计数绑定本快照。未运行编译或测试，`git diff --check` 通过。

**2026-11-08 Observability 小型测试逻辑校准：** `DiagnosticsExportFileRetention.staleFiles()` 用 `age >= MAX_AGE_MS`，但测试名说 older 且没有等界样本；补入恰好 10 分钟的 stale 文件，并验证更年轻/未来/无关前缀/同前缀目录均不返回，按集合比较避免依赖 `File.listFiles()` 顺序，方法数不变。`AndroidTraceSinkTest` 的 fixed name/cookie 用例原只启动一次，现重复同一 span start 再验证仍只 begin/end 一次；原 failure 测试仅触发 begin 异常，新增 end 异常用例覆盖独立 cleanup catch 分支。移除仅将常量 `42` 与自身比较的断言，保留 begin 异常不逃逸的测试合同；该类净增 1 个方法。未运行测试。

该批后全量静态快照为 419 个 test/androidTest/testFixtures Kotlin/Java 源文件，其中 402 个含 `@Test`、共 2,133 个声明；相较上条 Detail marker 清理快照净增 1 个声明，对应上面的独立 end failure 分支。

**2026-11-08 Database reset 授权分支复核：** `ResetDatabaseUseCaseTest` 的 allow/cancel/recovery-restricted 分别约束授权 scope、取消后无写入和受限 session 前置拒绝；`DatabaseResetGatewayAdapterTest` 约束独占 session coordinator 与 nullable persistence error 映射；`DatabaseResetViewModelTest` 约束 UI busy/error/reentry owner。三层无同 owner 重复。UseCase 的 `AuthorizationResult.Denied` 分支原无直接用例，补一例验证映射到 `AuthorizationDenied` 且不调用 gateway；与 cancellation 和 recovery restriction 输出不同，不合并。未运行测试。

该用例后当前全量静态计数为 419 个测试源码、402 个含 `@Test` 文件、2,134 个声明；本轮净增 1 个声明，对应 Denied 授权分支。

**2026-11-08 Vault revision history 逻辑交叉复核：** `ObserveEntryRevisionMetadataUseCaseTest` 断言 initialize 先于 metadata flow、初始化失败不订阅、初始化抛出的同一 CancellationException 原样传播；`DetailViewModelTest` 分别验证 metadata 收集异常重试，以及初始化失败时零订阅、重试后只订阅一次。前者是 UseCase 边界，后者是页面工作流恢复合同，不能合并。`DetailRevisionReducerTest` 原有两个测试分别断言活动 restore 下 BackToList/Closed 与 Opened 无效；它们构造同一状态并保护相同导航不变量，现合并为 `activeRestoreIgnoresNavigationMutations`，一次断言三种 mutation，测试方法净减 1。restore 取消后可重试和完整 sheet 状态转换保留；ViewModel 另覆盖持久化 restore 的提交/刷新生命周期。`RevisionHistorySheetTest` 的 Compose 测试覆盖空态/内容尺寸、长列表滚动与选择 action、高敏感字段 reveal affordance，以及 restore 中拖拽/返回拦截；这些验证组件呈现和事件接线，与 Reducer 的纯状态转换不同，不合并。

同组覆盖缺口记录：`CompareEntryRevisionUseCase` 当前没有直接单测，`DetailViewModelTest` 的 revision 用例侧重观察重试与 restore，没有验证 compare 对 metadata 中最高 version 的选择及 historical/current load failure 映射。`RevealRevisionFieldsUseCase` 已覆盖 key/scope、取消、授权边界取消后擦除和成功转移，但没有直接断言授权 Denied 时 repository 返回的敏感值被擦除；生产 `finally` 目前覆盖该路径。列为后续测试逻辑审查项，本轮不新增测试、不运行测试。

截至本次复核，重新扫描当前 `src/test`、`src/androidTest`、`src/testFixtures` Kotlin/Java 源：419 个文件、402 个含 `@Test`、2,134 个注解；这是静态源码计数。全仓逐例审查仍未完成；本轮仅更新待办记录，未提交。

**2026-11-08 buildSrc 与单例测试续扫：** `SourceBoundaryPresentationPolicyTest` 中 8 个 legacy/global settings access 用例都调用同一 `SourceBoundaryVerifier.verify(..., generalRules)`，差异仅为合成 consumer path/import 与对应 rule ID；现合并为带场景名的表驱动测试，保留全部 8 组输入及预期 ID，并保留 security runtime 场景的恰好 1 条 violation 断言，方法净减 7。`DetailFaviconMappingsTest` 测异常到稳定错误分类，`DetailFaviconPresentationTest` 测 editor state 到 UI model；`PrivacySettingsUiMapperTest` 与 `SecuritySettingsUiMapperTest` 分别投影不同页面合同，未合并。未运行测试。

合并后全仓静态计数刷新为 419 个测试源码、402 个含 `@Test` 文件、2,126 个注解；仅为源码注解计数，不是实际执行数。全量逻辑扫描继续进行，本轮未提交。

**2026-11-08 buildSrc module ownership 测试合并：** `appBackupGatewayCannotImportDataImplementation` 与 `appBackupGatewayMayImportDomainSnapshotPort` 使用同一 `AppBackupSnapshotGateway.kt` 路径，分别验证禁止 data implementation、允许 Domain snapshot port；合为一个正反例用例，保留相同 rule ID 与无 violation 断言，方法净减 1。合并后全仓当前为 419 个测试源码、402 个含 `@Test` 文件、2,125 个注解。未运行 buildSrc 测试，未提交。

**单例测试 owner 对照：** `DetailFaviconMappingsTest` 的 FaviconProcessingException → 稳定页面错误分类与 `DetailFaviconPresentationTest` 的 editor state → UI model 是两个映射边界；`PrivacySettingsUiMapperTest` 与 `SecuritySettingsUiMapperTest` 分别覆盖隐私页完整 flags/options 和安全页 lock/authentication projections。用例虽各只有一个 `@Test`，输入/输出类型与生产 owner 不同，不合并。

**2026-11-08 buildSrc policy/scanner 交叉复核：** `SourceBoundaryPresentationPolicyTest` 内重复路径的 SettingsRoute（导航本地状态/数据库能力）、VaultEntryGrid（modifier 透传/row 渲染 ownership）及 DataManagementSettingsViewModel（settings global port/trash ownership）对应不同 rule ID 与生产不变量，保留独立场景。`SourceBoundaryReferenceScannerTest` 的 fully-qualified package/layer reference 扫描与 `SourceBoundaryViewModelTest` 的 ViewModel 声明/类型依赖解析虽都含字符串模板样例，但前者验证包引用违规与源行证据，后者验证 ViewModel 类型解析跨模板插值；生产路径分别为 `SourceBoundaryVerifier` 与 `ViewModelDependencyVerifier`，不合并。该 buildSrc 家族未运行测试。

**2026-11-08 Autofill result delivery 相邻用例合并：** `AutofillResultDeliveryTest` 原有“平台结果交付后才关闭临时解锁”与“成功结果先过期 abandoned interaction 再关闭解锁”重复构造同一 delivery/lease/完成路径。合并为一个用例：平台 callback 提交前断言未锁；`RecordingLockController` 的回调检查选中 interaction 已 Completed、abandoned interaction 已 Expired 后才记录锁定；最后再核对 lock reason/trigger。精确保留两项时序合同并增加顺序断言，方法净减 1。`AutofillUnlockLeaseTest` 直接验证 lease 的 borrower/retry 状态机，owner 不同，保留。全仓静态计数现为 419 个测试源码、402 个含 `@Test` 文件、2,124 个注解。未运行测试、未提交。

**2026-11-08 Autofill save policy 分支合并：** `LegacySaveInfoPolicyTest` 的 username-only 与 unknown+OTP-only 都输入无 PASSWORD 的 role map，验证同一 `passwordIds.isEmpty()` 拒绝分支；改为带场景名的两例循环，保留 USERNAME、UNKNOWN、OTP 三种非密码角色，方法净减 1。`AutofillSaveDraftExtractorTest` 验证实际值候选选择/歧义拒绝，`LegacySaveFlowTest` 验证设置门禁、Inbox 排队与持久化结果映射，和 SaveInfo UI eligibility policy 不是重复 owner。当前全仓静态计数为 419 个测试源码、402 个含 `@Test` 文件、2,123 个注解。未运行测试、未提交。

**2026-11-08 Autofill credential save 拒绝用例合并：** `SaveAutofillCredentialTest` 的 blank password 与 invalid website domain 都以公开 `InvalidDraft` 结果拒绝并断言 application creation port 零调用；合为一个具名输入表，两个 guard 和副作用断言均保留，方法净减 1。它与 extractor 对 role-based value extraction 的拒绝结果不同，保存用例继续保护 validation 到 persistence port 的边界。当前全仓静态计数为 419 个测试源码、402 个含 `@Test` 文件、2,122 个注解。未运行测试、未提交。

**2026-11-08 Autofill title/matching 用例逻辑复核：** `AutofillCredentialTitleResolverTest` 分别保护 app/domain association、友好 app label、web title 优先级、package-like label/title 的隐私回退和密码原值保留，输入路径不同，保留。`HeuristicMatchStrategyTest` 的 search resource ID 带/不带 namespace 都保护同一早期 search 排除；合为一个具名案例循环，并显式断言无 credentials 且 roleMap 为空。OTP postcode/country-code 负例与 verification-code 正例共同固定 OTP pattern 的边界，合为一例并保留角色断言。两组各净减 1，合计净减 2。当前全仓静态计数为 419 个测试源码、402 个含 `@Test` 文件、2,120 个注解。未运行测试、未提交。

**2026-11-08 Autofill discovery preflight 用例合并与逻辑补断言：** `DiscoverAutofillCandidatesTest` 的 invalid domain 与 blank app id 都映射 `INVALID_SCOPE`，ambiguous origin 映射单独的 `AMBIGUOUS_WEB_ORIGIN`；三例均保证 query 不发生。合为具名三场景表，精确保留三个结果映射，并补断言 `AutofillInteractionStore.activeIds()` 为空，匹配测试名声称的 interaction 未启动合同；方法净减 2。`CompleteAutofillSelectionTest` 的 ActivityRecorder 入参/一次性记录合同与 `AutofillResultDeliveryTest` 的 Android 提交屏障/租约终态是直接完成 owner 与平台交付 owner，不重复。当前全仓静态计数为 419 个测试源码、402 个含 `@Test` 文件、2,118 个注解。未运行测试、未提交。

**2026-11-08 Autofill web-origin resolver 场景合并：** `AutofillWebOriginResolverTest` 的五个输入输出场景现由一个具名 case table 完整覆盖：等价 URL canonicalization、同 registrable domain 的不同 host、valid+invalid claim 冲突、无 web claim 的 truncated native tree、truncated tree 带 claim。每个场景仍精确固定 `domain` 和 `ambiguous`，整个列表一次比较可同时显示全部实际解析差异；测试方法净减 4。全仓静态计数为 419 个测试源码、402 个含 `@Test` 文件、2,114 个注解。未运行测试、未提交。

**2026-11-08 Autofill presentation reducer/event 测试合并与时序修正：** `AutofillFillReducer` 对 `ResultDelivered` 的 Result/Error 状态显式共用一个分支；两例合为具名状态表，两个 terminal 输入仍分别映射到 Initial，方法净减 1。`CredentialResponseResultEventsTest` 的 route recreation buffer 和取消 collector 后保留 completion 合为同一 Channel owner 生命周期用例；取消场景改用 `CoroutineStart.UNDISPATCHED` 并断言 collector 仍等待，确保它确实进入订阅后再取消，修正原测试可能在 collector 尚未启动时就取消的问题；单次消费/不重播断言保留，方法净减 1。全仓静态计数为 419 个测试源码、402 个含 `@Test` 文件、2,112 个注解。未运行测试、未提交。

**2026-11-08 Legacy Autofill response test overlap removal:** LegacyResponseFactoryInstrumentedTest.responseBuildResultCarriesOneSemanticDiscoveryObservation and the Disabled branch of disabledUnsupportedAndFailedOutcomesAdvertiseNothing both built the same AutofillDiscoveryOutcome.Disabled with an empty ParsedStructure, asserting null response; the standalone test additionally asserted Unavailable(SessionRestricted). Moved that semantic observation assertion into the Disabled branch and removed the duplicate method, preserving unsupported/failed null-response assertions. Method count net -1. Bottom-sheet versus system-inline authentication targets and password versus username no-candidate SaveInfo outcomes remain distinct contracts. No tests run, not committed.

**2026-11-08 Legacy unavailable response case precision:** Extended the merged unavailable-outcome test into a named case table and now assert the distinct production reason for Disabled (SessionRestricted), UnsupportedFields (NoFillableFields), and Failed (DatabaseUnavailable), in addition to null response. This preserves each mapping while preventing a wrong unavailable reason from passing. Net method count remains -1 for the duplicate removal. No tests run, not committed.

**2026-11-08 Encrypted log retention assertion review:** Date rotation already exercised the independent file-count limit, but its count-only read assertion could pass while retaining the wrong dates; it now asserts exactly the three newest event IDs. The prior size/total-byte test used the default three-file cap and a 2,000-byte budget, so its loose at-most-three assertions did not prove byte cleanup. It now sets maxFiles=10 and a one-byte total budget while size-rotating each write, then asserts cleanup removes prior files and keeps only the newest active record. The active file is intentionally exempt from cleanup and can exceed the aggregate budget, so the test fixes that behavior explicitly. No methods added or removed; tests not run, not committed.

**2026-11-08 Revision reveal authorization gap reclassified:** The earlier note about wiping repository-returned values on Authorization Denied described an impossible ordering under the production contract: DefaultAuthorizationGate returns Denied before issuing a permit or invoking the block. The existing after-block cancellation test exercises the path that can leave values owned by the use case and verifies wiping. Folded direct Denied result mapping and zero repository reads into the existing cancellation test, keeping method count unchanged. The CompareEntryRevisionUseCase selection/error mapping gap remains for a later focused pass. No tests run, not committed.

**2026-11-08 CompareEntryRevisionUseCase missing owner coverage closed:** Added one focused use-case test covering unordered metadata with highest-version selection, comparison output values, historical/current load failure mapping, and an emitted empty metadata list with zero revision reads. Extended the existing repository fake to emit metadata and capture requested revision IDs; this replaces the earlier emptyFlow fake for this owner, which did not represent the production empty-list case. Also folded Authorization Denied mapping/zero reads into the existing reveal cancellation case; denial occurs before the authorization block in DefaultAuthorizationGate, while after-block cancellation remains covered by the existing wipe assertion. Net method count +1 for the newly covered compare owner. No tests run, not committed.

**2026-11-08 Attachment GC worker test owner review:** AttachmentGarbageCollectionWorkerTest exercises pure stable-cursor paging, per-item retry continuation, access loss checkpoints, capped backoff, fatal Error propagation, and cancellation; AttachmentResourceGarbageCollectorTest exercises Android/Room/file lifecycle, authorization, references, and persisted retry state. They call different production owners and their assertions are not duplicates. The cross-suite scan also found no repeated @Test method names; this is only a candidate screen, not proof that behavior bodies are non-overlapping. Current static suite count: 419 Kotlin/Java test sources, 402 containing @Test, 2,112 annotations. Full logical review continues; tests not run, not committed.

**2026-11-08 Data Autofill resolver/mapping test owner review:** AutofillCredentialResolutionTest covers typed credential resolution, rejection, linked OTP lookup, and conditional OTP generation; AutofillCandidateMappingTest covers canonical/registrable-domain matching, filtered mapping, metadata/capability projection, and secret-free candidate shape; AutofillEntryQueryDaoTest covers SQL prefilter eligibility and exact-hit retrieval. These assert separate production owners and remain separate. Renamed the late-candidate mapping test because its pure helper does not execute the repository's candidate cap; its old title claimed cap ordering without exercising it. No direct repository-level test of sort/filter/take order was found; DAO and mapper coverage do not prove that pipeline. No methods added or removed. No tests run, not committed.

**2026-11-08 Unlock reducer/ViewModel test logic review:** UnlockViewModelTest owns provisioning single-flight, typed failure/retry, and restored attempt countdown; AuthenticationReducerTest owns pure state transitions, so their setup/failure checks are not duplicate. The reducer reset test previously initialized password/code as already empty and did not verify preserved availability state despite its broad title. It now starts with populated sensitive inputs, a failed verification phase, KDF result, available method, and attempt status; asserts the exact cleared fields and that KDF/method/attempt state remain unchanged. No test methods added or removed. Existing local edits in this test were preserved. No tests run, not committed.

**2026-11-08 Proto/bootstrap mapping test overlap:** ProtoSerializerTest's App/Security defaults, round trips, and corrupt-input checks cover two preference serializers; AutofillInboxBootstrapMappingTest covers a separate crypto bootstrap message, so no cross-file merge applies. Within the bootstrap mapper, the default-instance absent-key test and clear-key test asserted the same unprovisioned result; merged them while retaining both the default and cleared inputs plus preservation of bootstrap version/verification tag. The malformed-present and valid round-trip cases remain distinct. Method count net -1. A repository-wide scan for literal tautology patterns (assertTrue(true), assertFalse(false), identical primitive literals) found no matches. No tests run, not committed.

**2026-11-08 Full suite static count refresh:** Current worktree has 419 Kotlin/Java test/androidTest/testFixtures sources, 402 files containing @Test, and 2,109 annotations. Module breakdown: app 1,150; buildSrc 112; core 71; data 219; domain 195; feature 148; runtime 34; security 180. These are source counts, not executed test results. Compared with the prior 2,112 snapshot, the current count changed by three; only one removal in this batch is attributed here, so the other two changes are left unattributed pending the next source/diff audit. Full scan continues; not committed.

**2026-11-08 Deleted-test owner revalidation:** The deleted AppPasswordPolicy test's two length cases exist unchanged in the Domain policy owner; validation coverage is there as well, and AuthenticationTest no longer owns policy assertions. SensitiveRevisionRestorePolicyTest covered isExactAuthorization, which was removed from production; remaining affectedFields behavior is exercised through RestoreEntryRevisionUseCase's exact authorized union and empty-sensitive-field bypass. EntryTypeUiLabelsTest only asserted nonzero resource IDs for an exhaustive when; current localized labels are rendered by the AddEntry UI instrumentation tests. These deletions do not leave a confirmed uncovered live contract. No tests run, not committed.

**2026-11-08 Trash DAO duplicate rationale correction:** Rechecked the claim that `EntryRevisionLifecycleDaoTest.emptyTrashHistoryDelete_keepsActiveEntryHistory` fully subsumed the deleted `TrashDaoTest.emptyTrash_keepsActiveEntries`. The lifecycle test previously asserted only that the active entry's revision rows remained; it did not query whether the active Entry itself survived `deleteAllDeleted()`. Added direct active-entry-present and deleted-entry-absent assertions to the richer lifecycle transaction case, so the two cases now share the active-row predicate and revision-retention owner without leaving the Entry contract implicit. Static suite count remains 419 Kotlin/Java test sources, 402 containing `@Test`, 2,109 annotations. `git diff --check` passed; tests not run and no commit made.

**2026-11-08 Entry paging search logic review:** Traced the raw SQL search predicate and found the existing paging tests exercised only one secondary search field, leaving username, title, primary URL, tags, domains, application IDs, and LIKE escaping weakly demonstrated as a group. Added two DAO cases: one table-driven case checks each of the six SQL search fields in isolation with case normalization, and one checks literal `%`, `_`, and backslash handling against wildcard decoys. Review caught and corrected an expected-value type mismatch in the new table assertion before verification. Static suite count is now 419 Kotlin/Java test sources, 402 containing `@Test`, 2,111 annotations. `git diff --check` passed; tests not run and no commit made.

**2026-11-08 Entry paging sort logic review:** The SQL factory has independent title, created/updated timestamp, last-used, usage-frequency, entry-type and ID ordering, optional favorite pinning, and canonical tie-breaker direction. The previous DAO cases proved only last-used ordering in both directions, entry-type ascending, and hierarchy ordering by ID. Added DAO assertions for title, created/updated time and usage-frequency in both directions, entry-type descending, ID with favorite pinning, stable secondary ordering and exclusion of mutation activities from usage counts. One focused tie-breaker case verifies CREATED_AT uses its canonical descending tie direction. Static suite count is now 419 Kotlin/Java test sources, 402 containing `@Test`, 2,113 annotations. Tests not run and no commit made.

**2026-11-08 Worktree verification scope:** `git diff --check` passes for the current audit test files and this ledger. The whole dirty worktree check currently reports one unrelated existing issue at `app/src/main/java/com/aozijx/passly/feature/recovery/presentation/ui/RecoveryPasswordDialog.kt:127` (new blank line at EOF); left untouched to preserve that separate production change. No tests run and no commit made.

**2026-11-08 Entry paging linked-account resolution:** `EntryPagingQueryFactory` derives grouping for direct MEMBER_OF_ACCOUNT, RECOVERY_FOR, and OTP_FOR→MEMBER_OF_ACCOUNT links, but the DAO suite only asserted the direct path. Added one Room DAO case using domain-valid LOGIN, OTP, RECOVERY_CODE and ACCOUNT endpoints; it fixes the current projection contract as LOGIN→ACCOUNT, OTP→ACCOUNT through its protected LOGIN, and RECOVERY_CODE→protected LOGIN. Static suite count is now 419 Kotlin/Java test sources, 402 containing `@Test`, 2,114 annotations. The changed paging test's diff check passes; tests not run and no commit made.

**2026-11-08 SourceBoundaryPolicy split revalidation:** Compared the deleted HEAD suite (70 annotated tests) with the five current replacement files (56 annotated tests). The 14-method net reduction comes from recorded table merges: eight global-settings consumer cases into one (−7), two AppBackupGateway cases into one (−1), and four app/security import cases into one table (−3), plus three policy cases folded into the package/layer matrix and broader feature implementation test. Verified the matrix still covers the presentation forbidden imports and runtime/session forbidden edges, while the merged tests retain exact expected rule IDs and evidence/paths where the old cases asserted them. This revalidation is based on current source bodies; tests not run and no commit made.

**2026-11-08 Autofill candidate pipeline gap (superseded):** At this point `AutofillEntryQueryDaoTest` covered SQL prefilters and `AutofillCandidateMappingTest` covered the pure mapper, but there was no direct repository test. The initial note incorrectly described simultaneous source merge/deduplication: the current `DiscoverAutofillCandidates` caller selects one source, and the repository suppresses the application source when a domain exists. Repository ordering and bounded results remained untested at that time. Corrected by the later owner recheck and repository test below.

**2026-11-08 Autofill candidate search owner recheck (superseded):** `DiscoverAutofillCandidates` passes `packageName` only when `webDomain == null`; current app callers therefore query one source at a time. The repository's output ordering and limit behavior had no direct test at this point. See the follow-up below for repository-level coverage and the remaining prefetch-bound observation.
**2026-11-08 Post-commit duplicate-name screen:** Recounted the current Kotlin/Java test tree: 419 source files, 402 containing `@Test`, 2,116 annotated methods. Parsed each annotation to its following Kotlin/Java test declaration, including backticked and same-line Kotlin names; all 2,116 mapped, with zero duplicate test method names. This only screens names and does not establish body-level behavioral uniqueness. No test files changed or tests run; broader logic review remains open.

**2026-11-08 Test filename-index candidate review (partial):** A basename-to-ledger scan produced 105 candidates, which is only an index and includes false positives. Reviewed the 13 Data and 9 Domain candidates against their production owners; no confirmed obsolete, misleading, weak, or duplicate contracts were found. The `TestCryptoCapabilities.kt` entry is a support fixture, not a test class. Reviewed 1 Core, 1 Runtime, 10 Security, and 3 buildSrc candidates; no confirmed cleanup or coverage-loss issues were found. App/Feature candidates remain under review. No tests run and no test files changed.
**2026-11-08 Autofill settings presentation action coverage:** The settings Compose case previously asserted that presentation menu labels remained visible without Credential Manager support but did not select one. It now selects `BOTTOM_SHEET` and asserts the exact `SetPresentation` action. `AutofillSettingsViewModelTest` previously covered only strict-matching success; the existing writer test now also dispatches `SetPresentation(BOTTOM_SHEET)` and verifies its repository write, without adding a test method. These are distinct UI wiring and ViewModel persistence owners. The ViewModel action maps to `setAutofillPresentation` in production. `git diff --check` passed for both test files; tests not run and no commit made.
**2026-11-08 Trash test owner recheck:** `TrashCommandUseCaseTest` exercises authorization, entry lookup, optimistic version forwarding, and the access-state recheck inside the authorized command boundary. `TrashViewModelTest` exercises page-level busy/reentry state, presentation of failures, retry, cancellation, and recoverable list observation. These are distinct use-case and page-workflow owners; their overlapping command names do not duplicate the behavior assertions. No test changes or test runs.

**2026-11-08 Autofill candidate repository logic review:** Corrected the earlier gap description: `DiscoverAutofillCandidates` passes package name only when the web domain is absent, and `RoomAutofillCandidateRepository` also nulls the application scope when a domain exists, so its current caller path never merges both DAO sources; the earlier claim that both sources are merged was inaccurate. Added `RoomAutofillCandidateRepositoryTest` in AndroidTest to exercise the repository through the real encrypted Room session: Autofill-only session access and revocation, application-ID normalization and favorite/recency order under caller limits (including lower-bound clamp), plus domain filtering, deletion/type exclusion, favorite/recency order and the global 10-result cap. The bounded app-ID prefetch factor (`limit * 4`) remains an internal optimization whose exact boundary is not asserted by these output-level tests. Static count is 420 Kotlin/Java test sources, 403 containing `@Test`, 2,128 annotations. Test logic was reviewed against the repository, session gate, DAO and mapper; `git diff --check` passes for the new test and this note. Tests were not run; full App/Feature logic review remains open, and no commit was made.

**2026-11-08 Current dirty-test logic review:** AppTheme and AppShell retry tests previously asserted only that retry reached `Ready`, despite names/behavior depending on restored settings; they now supply non-default settings and compare the full `RecoverableReadState.Ready` projection, without adding methods. Reviewed the encrypted stream cases against record framing: empty and missing terminal, sequence order, record types/lengths, authenticated terminal totals/tag, and truncation protect distinct stream branches; the service-level missing-terminal test additionally asserts `BackupFailed(EOFException)` and no restore, and clears its password and byte buffers in `finally`. The test KDF returns `keyLengthBits / Byte.SIZE_BITS` bytes, matching the production 256-bit key contract. Header-length corruption is also included in pre-KDF fixed-field rejection. Current static count is 420 sources, 403 with `@Test`, 2,127 annotations. `git diff --check` passes for reviewed test paths; tests were not run and no commit was made by this scan. Full project logic review remains open.

**2026-11-08 Settings state projection and duplicate failure follow-up:** Removing `InterfaceUiMapperTest` with the obsolete mapper left its all-fields projection assertion without an owner; migrated the complete non-default `InterfaceUiModel` equality into the existing `InterfaceSettingsViewModelTest` retry case, including the 0..48 radius range and values from both repositories. In `InteractionSettingsViewModelTest`, merged the standalone swipe-right failure case into the existing preference failure case (toggle failures plus swipe-right action, same resource error owner), and strengthened successful action coverage to assert both left/right writer arguments and feedback. Net test method count decreases by 1 from this follow-up. Current static count is 419 Kotlin/Java test sources, 402 with `@Test`, 2,126 annotations. `git diff --check` passes for current reviewed paths; no tests run and no commit made by this scan. Full project logic review remains open.

**2026-11-08 Settings projections and recovery verification logic follow-up:** Replaced type-only retry assertions in Privacy, Security, Notification, and Vault Display ViewModel tests with exact failure classification and/or complete non-default ready projections; the Data Management state migration now also asserts `ReadFailure.IO`. In Recovery Code tests, tracing `verify()` through `replaceVerification()` showed that both rejection and ordinary failure wipe the input before page exit, so the old pair of “wiped when page exits” cases attributed cleanup to the wrong boundary. Merged them into a two-scenario case table, asserting each exact terminal result, immediate wipe, and the exact empty Editing state after exit. Recovery Mode reducer and ViewModel tests were compared; their pure transition assertions and page/async lifecycle contracts have separate owners, so retained them. Fresh tracked-source inventory: 418 Kotlin/Java test sources, 401 files with `@Test`, 2,124 annotations; earlier 420/403/2,128 snapshot was stale. Scoped `git diff --check` passed; tests were not run. These six test-file changes are now present in shared commit `a09dd045d`; this agent did not execute the commit command. Full App/Feature logic review remains open.

**2026-11-08 Detail entry-type projection coverage gap:** `DetailEntryTypeUiMapper` contains separate card, identity, SSH, seed-phrase and passkey projections, but repository search found assertions only for card and Wi-Fi mapping; no test directly covers identity, SSH, seed-phrase, or passkey mapping branches. The latest passkey state change distinguishes `Absent`, `Hidden`, and `Revealed`, and `PasskeySection` renders them differently; the existing `DetailSectionResolverTest` only checks section inclusion. Record these as missing detail mapping/render-state contracts for the owning test suite, not duplicate or obsolete tests. No test added or run during this audit.

**2026-11-08 Backup ViewModel access/lifecycle owner review:** Compared `BackupViewModelAccessTest` with `BackupViewModelLifecycleTest` and the reducer suite. Access cases stop at authenticated/recovery-mode admission and recheck at submit; lifecycle cases own picker token freshness, running-operation persistence, sensitive input lifetime, and bounded notice behavior. `BackupReducerTest` already fixes the full export option transition, so type-only `ConfigureExport` assertions in the admission tests serve as branch witnesses rather than duplicate projection contracts. No confirmed duplicate or stale test found; no test change or run.

**2026-11-08 Notification state-wrapper retirement review:** The shared refactor now maps `MessageSettings` directly to `NotificationSettingsUiModel` and exposes `RecoverableReadState`; repository search found no remaining `NotificationSettingsUiState` or refresh-action references. ViewModel retry coverage asserts the full ready projection. The pure mapper test overlapped that projection for complete topic maps and omitted its missing-topic default branch, so this audit changed it to a partial map and exact expected model with stable order/default-enabled topics. Scoped diff check passed; tests were not run and this audit did not commit.

**2026-11-08 Detail tag validation model migration review:** The current test changes replace duplicate Feature error enums with Domain `EntryTagValidationError`; the Domain policy and Detail reducer still own separate normalization and user-input transition behavior. The Detail mapper assertion now covers both TooMany and TooLong resource/quantity mappings in one scenario table. `TagEditorSheetTest` still used deleted `validationError`/`TagEditorOverlayUiState` fields, so migrated its existing save-disabled UI fixture to `validationMessage` and `DetailTagEditorOverlay`; global test-source search found no remaining references to the removed model types. Scoped diff check passed; tests were not run and no commit was issued by this audit.
