# Passly 深度维护审查待办

**最近复核：2026-11-08。** 全模块 Kotlin 静态盘点，并沿生产调用闭包重点检查会话/认证、敏感清理、Flow 恢复、页面与平台宿主、备份、诊断及既有待办；不等同于全仓安全扫描或逐行证明。

## 执行顺序

保留原 Task ID，正文按批次重排。P0 表示工程门禁阻断，P1 为敏感/资源安全或具体运行流程缺口，P2 为恢复/交付合同，P3 为结构维护。**retained** 表示仍有证据支持的工作；**deferred/待验收** 表示已有机制但指定验证未完成；**removed** 仅移除过时缺陷，不代表整项已通过。编号不代表优先级。

| 顺序 | 执行入口 | 依赖与原因 |
| --- | --- | --- |
| 1 / P1 | [安全 Task 6](2026-11-06-passly-security-scan-followups.md#task-6)、[Session 与 DEK 审查记录](../../reviews/2026-11-07-session-close-and-dek-wipe.md)，本页 37→26、59 | 接收方身份、lease 关闭、撤权与提交事实；先完成 Data admission 与撤权线性化，再闭合 Runtime/reset 跨层终态 |
| 2 / P1 | 本页 42、40、32；[诊断 Tasks 1–3](2026-11-06-passly-diagnostics-reliability-followup.md) | seed/流资源、目录操作终态、分类查询与 flush；先固定失败结果再改 owner |
| 3 / P2 | 本页 46、51；[安全 Tasks 3、7、9](2026-11-06-passly-security-scan-followups.md)、OTP/搜索专题；Autofill Task 82（设置策略） | 46 接 59 的提交后恢复；备份端到端流式重构已完成；Autofill 设置策略待办由专题清单维护 |
| 4 / 定向验收 | 本页 22、23、27、29、31、44、45、64；[Autofill 宿主验收](2026-11-07-passly-autofill-followups.md#task-21)、Scanner 验收 | 已有机制，不重复实现；JVM、Route/宿主和设备验证分别标记 |
| 5 / P3 | 本页 83 → 84/80，35、34、36、74、75、77 | 83 管 Detail 具体流程，84 管 owner 清单，80 管状态模式；77 先稳定诊断可靠性 |

安全 Task 1/5/10、Autofill 设置快照、OTP HOTP 和非 ASCII 搜索仍有策略合同待裁定；本轮不臆定产品行为，不把候选升级为已验证漏洞。允许独立项并行，表中箭头表示必须先满足的前置条件。

## 专题 owner 索引

| 主题 | 唯一详细待办/验收来源 | 本页关联 |
| --- | --- | --- |
| 架构与并发 | [架构与并发 TODO](2026-11-06-app-architecture-concurrency-todo.md) | 26、37、51、80、83、84；具体缺陷先处理，广域提案随后 |
| Autofill 后续 | [唯一后续清单](2026-11-07-passly-autofill-followups.md) | 21 宿主验收、82 设置策略；其他已完成合同已归入主题文档 |
| Scanner | [审查 TODO](2026-11-06-passly-scanner-review-followups.md)、[取景框计划](2026-11-06-scanner-entry-and-framing.md) | 42、51；显式关闭/宿主暂停区分在专题 Task 4 |
| Vault / OTP | [搜索 TODO](2026-11-06-passly-vault-search-review-followups.md)、[OTP TODO](2026-11-06-passly-otp-review-followups.md) | 27、32、34–36、42 |
| Backup / 诊断 | [Backup 格式说明](../../features/backup/passly-archive-v1.md)、[数据端口迁移审查记录](../../reviews/2026-11-07-backup-data-port-migration.md)、[诊断可靠性 TODO](2026-11-06-passly-diagnostics-reliability-followup.md) | 40、73、74、77 |
| 安全与资源 | [安全遗留 TODO](2026-11-06-passly-security-scan-followups.md) | 26、37、44–46、59、64；非合作 lease 关闭验收归档在安全审查记录 |

## Autofill 重构后的清理

Autofill 的旧段落已从本页移出：宿主验收合并到 Task 21，系统状态观察合同已从完成计划清除，85 的形式改造也已移除。当前专题清单只保留 Task 21、82；其余当前合同见 Autofill 主题页。下方扫描数量与失败统计是历史采样记录。

## 此前扫描与验证记录

- 历次源码数量和失败数是并行工作区的时间点快照，已移除；当前验收以各 Task 未完成项为准。
- 旧 App/Autofill 单测与 lint 历史失败已关闭，不再作为当前待办。
- Autofill/Room/Auth/UI 合同修正已放入对应专题或功能文档；Domain 端口迁移证据见[迁移审查记录](../../reviews/2026-11-07-domain-cohesion-migration.md)。
- 真实设备、APK Analyzer、KDF/低内存基准和强杀持久性未由 JVM/静态验证替代，仍由相关 Task 跟踪。

<a id="task-37"></a>

### Task 37：为认证撤权与数据库操作定义同一线性化边界
- [ ] 增加可控挂起的跨层终态回归：在 Security lock 等待数据库 lease/resource 与敏感 key 清理时，从 runner、直接 Full Vault Repository 与 Autofill Repository 发起读写，验证线性化点之后新命令不触达 DAO；已进入事务准确报告提交/回滚/取消结果，敏感 permit、会话、DEK、key 与 lock state 一致。边界与真实 Repository 集成测试证明调用闭包后，不需为每个薄适配器复制同一测试。
- [ ] 在可用设备上验收真实 Data lease/Room DAO 与 Security reset/lock 交错终态；当前 `adb devices` 无连接设备，instrumentation runtime 尚未执行。恢复模式、Autofill session 结束和 idle timeout 的撤权次序仍需按本项统一验收。

**审查证据：** 原调用闭包中 Runner 与多个 Repository 各自只在获取 lease 前检查授权。当前 `AppDatabaseSession` 通过 Hilt `Provider<SecureSessionAccessState>` 获取当前 Security owner，并按 Full Vault/Autofill 语义提供 protected query/transaction；旧的无授权通用 command API 与逐调用谓词均已删除。普通命令入口均已迁移；maintenance API 只给数据库清理与测试 fixture 使用，GC 不使用旁路。Room observations 也由 `AppDatabaseSession.observeFullVaultWhenReadable()` 内部读取授权 Flow，再组合 Runtime lock/generation 重新订阅，Repository 不再注入或传递授权状态。当前边界测试用可控 access-state 验证第二次授权拒绝时 read/write DAO block 不运行；它还未验证真实锁定与 Repository/Autofill 并发入口的提交/取消终态，因此 Task 37 仍未完成。

**2026-11-07 Security 锁定顺序批次：** `VaultSessionController.lock()` 在进入可挂起清理前发布 `AuthenticationState.Locking`，保留变更前状态以判断 RecoveryMode；随后撤销 permit、调用 Runtime soft-lock/seal 排空 lease，再清敏感会话 key，sealed/fatal 路径在 key 清理后锁 DEK。可控 soft-lock 与 key-clear 屏障验证 Locking 已使 full access 失效，且 key clear 要等 Runtime soft-lock 返回；Security 150 项与 `verifyArchitecture` 通过。此批只记录撤权与 key 清理顺序；后续 Task 37 已另行收口 Data admission 与授权复核。

**2026-11-07 Data admission 批次：** `AppDatabaseSession` 通过惰性 Security provider 直接读取认证权限，并在 lease 内再次核验；Repository 与 Runner 不再传递授权谓词或持有重复访问状态，Room observation 也由 session 集中读取认证 Flow。Full Vault、Autofill 与 maintenance 入口语义显式区分。新增 Room observation instrumentation 回归，分别验证 read/write 在第二次授权核验拒绝且 DAO block 不执行；`:app:kspDebugKotlin`、Data 主代码与 AndroidTest 源集编译通过。`:security:testDebugUnitTest` 与 `:runtime:session:test` 强制重跑通过；`adb devices` 无可用设备，instrumentation 未运行。同步修正数据库映射和认证文档。Task 37 仍待真实 Security/Data 并发终态、Autofill/Recovery/idle/reset 次序验收和定向验证。\n\n**2026-11-07 Room 撤权/事务终态批次：** 新增真实 Room 事务屏障测试：Full Vault 事务获准并进入 DAO 后撤销认证，Runtime soft-lock 等待该 lease；线性化点后的新查询返回拒绝结果、不运行 DAO；释放原事务后确认提交并排空 lease。Data AndroidTest 源集编译通过。连接测试启动前设备在并发 App instrumentation 期间断开，Gradle 报 `No connected devices`，设备端用例仍未执行。

**2026-11-07 GC 授权回调收敛：** `AttachmentGarbageCollectionWorker` 不再接收 `hasFullAccess` 函数参数，改为在每个分页、条目与退避检查点直接查询 `SecureSessionAccessState`；生产调用点不再转发谓词，测试通过可变认证状态覆盖撤权边界。早期定向构建曾被旧 Core→Security 实现依赖阻断；该依赖已通过 Security 边界迁移移除。后续记录的 `:core:testDebugUnitTest`、`:security:testDebugUnitTest`、Data unit/AndroidTest 源码编译与 `verifyArchitecture` 均通过，`AttachmentGarbageCollectionWorker` 7/7 通过。因此旧 KSP 阻断不再是当前待办；Task 37 仍待更广的授权线性化、事务终态、Autofill/Recovery/reset 交错行为验收。

**2026-11-07 生命周期顺序复核：** idle/background 与 Autofill 结果完成后的释放都调用 `VaultSessionController.lock()`；RecoveryMode 锁定升级为 SEALED，Autofill 最后一个 borrower 释放受结果完成屏障保护。Reset 由 `SecureDatabaseResetCoordinator` 在同一 Security `sessionMutex` 下执行，先 SEALED，再按 Runtime lock state 恢复或撤销 permit/清 key；`security lock waits for database reset to finish reopening` 屏障测试验证 lock 等待 reset 完成。该测试使用可控 DatabaseSession fake，真实 Room lease/DAO 结果与 reset/lock 交错仍未联合验收，因此保留 Task 37。

**2026-11-07 Room 列表订阅收敛：** `DatabaseReadAccess` 在授权、lock state 与 lock generation 合流时采样当前 generation，避免快速重开时组合出重复的可读代际并重启 Room Pager 两次。`RoomObservationSessionTest` 现覆盖真实 SQLCipher PagingSource 的新增、编辑更新、过滤与重置恢复；`:data:testDebugUnitTest` 与设备 `RoomObservationSessionTest` 全类通过。Task 37 仍待真实 Security 撤权与 Data reset 并发终态验收。

**2026-11-08 JVM 验证批次：** Security 全量 179/179、Runtime Session 34/34、Data JVM 170/170 均通过；`:data:compileDebugKotlin`、`:data:compileDebugAndroidTestKotlin` 和 `verifyArchitecture` 通过。Security 锁定异常测试将跨协程边界的异常对象 identity 断言收敛为类型与 message，保留清敏、终态和 suppressed failure 断言。一次合并 Gradle 构建曾因 Core Hilt 聚合 wrapper 编译失败；随后独立强制 Data JVM 全量执行通过。`adb devices` 无设备，真实 Room instrumentation 与跨层 reset/lock 仍待验收。
<a id="task-26"></a>

### Task 26：明确 Runtime Session 生命周期转换的并发所有者
- [ ] 与 Task 37 完成 soft-lock、认证撤权和 Data Reset 的跨层终态验收：覆盖已有 lease 的读写/观察在排空与取消后的真实退出、迟到结果处理及 reset 与并发 lock 的交错。Data 命令 admission 已在 Task 37 统一；此项只保留仍缺的跨层终态测试。运行 Runtime、Security Session、Data Reset 定向测试。

**2026-11-07 Runtime lifecycle 串行化：** `RuntimeSessionManager.lifecycleMutex` 保护完整 `unlock`（close-failure 检查、resume、key copy、resource open）、`softLock` 与 `seal`；`VaultSessionController` 的会话互斥继续作为外层 owner，`SecureDatabaseResetCoordinator` 也使用该外层互斥。锁顺序为 Security/reset session mutex → Runtime lifecycle mutex → SessionLeaseGate 的 resource/lease mutex；请求 lease 与执行资源操作分阶段取 gate 锁，`resumeIfOpen` 唯一的嵌套顺序为 resource mutex → lease mutex。五项屏障用例先在旧实现失败，再在新 owner 下通过，覆盖两个首次 unlock、open/seal、seal drain/unlock、soft-lock/resume 和 seal 调用方取消。

**2026-11-07 soft-lock 排空批次：** Runtime 在 `softLock()` 发布 `SOFT_LOCKED` 后等待活动 lease；5 秒超时或调用方取消会请求登记 owner 取消，再在不可取消清理区等待实际退出，保留已打开的 handle。新增两项屏障回归，分别验证超时取消与调用方取消后仍等待、不关闭资源，且新 lease 被拒绝。Runtime 用例通过。Security 当前已先发布认证撤权、等待 Runtime soft-lock/seal 排空，再清敏感会话 key；Task 26 仅保留与 Task 37 共用的 Reset/跨层终态验收。

<a id="task-59"></a>

### Task 59：闭合生物识别轮换的提交后恢复与启动对账

- [ ] 统一轮换 durable commit 后结果：binding 已切换后，候选清理状态读取或 availability refresh 普通失败不得显示未提交/允许盲重试；按持久事实保留 pending 派生恢复。
- [ ] 覆盖 withBiometricRotationCandidate 正常返回后 loadBiometricState 失败、provisioner 在 rotate Success 后 refreshAvailability 失败；验证提交前/后异常、取消、fatal、alias/journal、启动恢复、敏感缓冲、host lease。
- [ ] 精确验收应用密码 durable commit 后的取消结果：`DefaultAuthenticationMethodProvisioner.setAppPassword(password)` 先由 `dekManager.rekeyWithKey(...)` / `initializeWithKey(...)` 持久化 APP_PASSWORD envelope，再执行 `reconcileSession()`；普通模式先调用 `refreshAvailabilityAfterCommit()` 再 `session.markAuthenticated(...)`，恢复模式调用 `finishRecoveryPasswordProvisioning(seal, refreshAvailability)`。会话协调中的刷新或认证态发布若取消，会从 `runAppPasswordProvisioning()` 直接传播，跳过普通异常分支的 `hasCommittedAppPasswordEnvelope(store, salt)`；`AppPasswordSettingsInteractor.execute(request)` 只在 `finally` 清理输入，`AppPasswordSettingsViewModel.runChange()` 没有收到 Completed 时不发成功 effect。用提交前/后取消的 owner 行为覆盖确认 `isEnabled` Flow 能否及时校正页面状态、是否留下可重试界面或造成重复提交；不可把普通异常的 envelope 读回保证外推到取消。
- [ ] 保留 reconciler 可重试清理及 fatal 传播，以真实 rotate/provisioner/启动 owner 测试闭合并同步认证文档。

**2026-11-07：retained，提交后窄残余已部分收敛。** `withBiometricRotationCandidate()` 清理阶段读取持久状态失败时保留已提交 Success，并携带 `BIOMETRIC_ROTATION_CLEANUP`；Security Manager 对认证方式解析普通失败启用 1s 起步、上限 60s 的指数退避，成功后发布最新快照。生物识别轮换、禁用生物识别与禁用应用密码在持久操作后刷新失败时返回成功并携带 `AUTHENTICATION_AVAILABILITY_REFRESH`；UI 以提示反馈，不显示提交失败。取消仍传播，fatal `Error` 不转换。Security 176 项/34 类、App 全量 998 项、`verifyArchitecture` 通过。剩余：启动时刷新失败的可见状态、provisioner/store 的真实 durable-commit 集成覆盖，以及取消/fatal/host lease 与启动恢复联合验收。

**2026-11-07 应用密码结果透传：** 设置/修改/禁用的 App interactor 不再把 Domain `Success` 压成无信息的 `Completed`；完成结果携带 `pendingMaintenance`，ViewModel 发出成功效果后再按 Domain 维护类型提示。应用密码持久提交后，可用性刷新失败被带回 Domain 结果并提示重试状态；恢复模式封存后刷新失败同样保留该语义。新增用例覆盖 Security 提交结果、恢复流程、Interactor 映射和 ViewModel 反馈。当前 Security 177 项/34 类、App 1010 项/179 类全通过，`verifyArchitecture` 通过。仍需用真实 provisioner/store 组合覆盖提交点前后及异常次序。

**2026-11-07 候选清理恢复：** `withBiometricRotationCandidate()` 在操作已返回结果后读取轮换状态/清理候选若遇普通 `Exception`，不再覆盖操作结果；成功结果标记 `BIOMETRIC_ROTATION_CLEANUP` 以使用已有恢复合同，失败或取消结果保留原结果。`CancellationException` 继续传播，fatal `Error` 不捕获。新增回归先按旧实现失败，修复后 Security 单测 175 项/33 类全部通过。仍待真实 rotation/provisioner 测试覆盖 durable commit 后刷新失败，以及提交前/后取消、启动恢复和 host lease。


<a id="task-42"></a>

### Task 42：让共享条目编辑能力由条目编辑层持有
- [ ] 按当前 owner 补齐并执行行为验收：`LoadEditableEntryUseCase` 与 `DetailViewModel.prepareEntryForEdit()` 覆盖 Repository/授权/activity-recording 普通异常、取消、认证撤销及条目版本变化；Detail Route 覆盖失败可见性和重新发起。`EditEntryUseCaseTest` 已从旧 `feature.vault.detail` 路径迁到 `feature.vault.entry`，同包引用新 owner；Detail/四类编辑 ViewModel 测试已改为调用新 owner，但仍需运行并核对版本冲突、取消和迟到结果合同；seed 消费测试覆盖无效/已消费 token 后需返回 Detail 重新 stage。另为 `EditorWriteSession` 覆盖 fatal Error 清理并继续重抛。运行相关 UseCase/ViewModel/Route 测试、`verifyArchitecture` 与 `git diff --check`。

**2026-11-07 Detail 预加载异常实现进度：** `prepareEntryForEdit()` 现对普通异常发出携带 `EntryId` 的 `EditPreparationFailed`，仅在同一详情条目、认证状态和完整访问仍有效时发送；Route 对过期条目事件静默丢弃，并复用本地化“无法加载条目”提示，用户再次点编辑即可重新发起。取消和 fatal `Error` 原样传播，没有新增兼容入口。`DetailViewModelTest` 27 项通过（含普通异常后重试、取消传播与 `LinkageError` 传播），六个编辑能力/页面测试合计 98 项通过，`verifyArchitecture` 与 `git diff --check` 通过。Route 的生命周期/Toast 可见性尚无独立宿主行为断言，因此上方完整验收仍未关闭。

**2026-11-07 EditorWriteSession fatal 清理：** `write()`/draft 构造抛 `Error` 且 attempt 仍为当前 owner 时，session 现在关闭为不可重激活终态、清空当前表单并停止 effects，再原样传播 fatal；清理异常仅作为 suppressed failure 附加，不把未知提交结果映射成 `Failed`。新增测试在 RED 阶段复现表单未清和 Saving 卡住；实现后 `EditorWriteSessionTest` 13 项通过，精确检查错误身份、表单清空、释放 saving 和 session 不可重开。随后一次无关增量断言重跑被工作区中的 `BackupChunkedContainerCodecTest.kt` 编译错误阻断；该断言已撤回，保留先前通过的测试版本。独立 `verifyArchitecture` 通过；全量相关 App 测试/Route 宿主验收仍待当前备份测试阻塞解除。

**2026-11-08 seed 导航失败实现：** `EntryEditorSeedCoordinatorTest` 与 `VaultNavigationTest` 定向共 12 项通过，覆盖普通导航异常的原始异常日志与失败结果、失败后 token 消失、旧调用失败不清理新 token、取消/fatal 清理后传播，以及扫码 Wi-Fi 成功路径仍使用 opaque token。`EntryEditorSeedStager` 与 `EntryEditorSeedStageResult` 无生产/测试引用。Detail Route 显示本地化编辑器打开失败提示；Wi-Fi 扫码导航失败映射到现有拒绝反馈（静态调用链复核）。相关 Kotlin 编译、`verifyArchitecture` 和本批 `git diff --check` 通过。本条只关闭 seed 导航补偿，不关闭 Task 42 的 Detail 宿主验收、敏感 String 生命周期评估和整体行为验证。

**2026-11-08 敏感 String 驻留评估：** `LoadEditableEntryUseCaseTest` 覆盖 reveal 拒绝/不完整结果与认证会话替换；`DetailViewModelTest` 覆盖预加载取消；`EntryEditorSeedStoreTest`、`EntryEditorSeedCoordinatorTest` 覆盖 seed 清除及锁定；`EditorWriteSessionTest` 覆盖取消、保存清理和锁定输入门禁。这些只证明授权/owner 生命周期与受控引用释放，不证明 JVM 擦除。`Entry`、Domain credential 和四类编辑 Form 均以 `String` 为值合同，局部改缓冲会引入重复模型且无法避免入口/Compose/写入处重新创建 String，因此本轮将其作为已评估的固有限制记录；如需更强擦除语义，应先整体迁移 Domain、表单和持久化写入边界。

**2026-11-08 Detail 与 seed 重试边界复核：** `prepareEntryForEdit()` 当前捕获普通 `Exception` 发出 effect，`DetailRoute` 仅对匹配当前 `entryId` 的 effect 显示本地化 Toast；用户可重新点 Edit 发起新读取。四个编辑 Route 用 opaque token 调用同步 `loadExistingEntry(result)`；Missing/AccessDenied 或类型/会话不匹配会清空表单并置为 `EditorLoadPhase.Failed`，`EditorLoadFailureEffectHandler` 给出返回后重试提示。此处不存在编辑器内 Repository 重读或复用已消费 token 的路径。上述实现项已闭合；整体 Detail Route 宿主行为和迟到结果过滤仍保持开放，不能用 ViewModel 单测替代。

**2026-11-08 Task 42 定向验证续记：** 对 `EditEntryUseCaseTest`、`LoadEditableEntryUseCaseTest`、Detail ViewModel/Reducer、四类编辑 ViewModel、`EditorWriteSessionTest`、`EditorSaveEffectHandlerTest` 与 `VaultNavigationTest` 分别执行定向单测，共 151 项、0 failures/errors；`verifyArchitecture` 通过。此证据覆盖当前 JVM 行为和生命周期 effect handler，不覆盖真实 Detail Route Toast/重复点击、编辑器 Route 重试显示或 Android Activity 重建，因此 Task 42 保持开放。

**历史调用闭包复核（2026-11-06，迁移前快照）：** `rg` 生产引用显示 `LoadEditableEntryUseCase` / `EditableEntryLoadResult` 只由 `DetailViewModel` 使用：`prepareEntryForEdit()` 以当前 `uiState.entry.id` 调 `loadEditableEntry(entryId)`，收到 `Loaded(entry, authentication)` 后核对 ID、类型、认证对象身份、完整访问权和当前详情条目，再交给 `VaultGraphRegistration.onEditFullForm` stage。四个 Route 从 token 调 `seedCoordinator.consume(token)`，随后同步调用各自 `loadExistingEntry(result)`；这四个函数不依赖或调用读取用例，只校验 seed 访问并把 `seed.entry` 映射成表单。迁移前 `EditDetailEntryUseCase.edit(entryId, edit)` 由 DetailViewModel 和 Password/OTP/BankCard/Wi-Fi 四个 ViewModel 共用，维护互斥、读取最新条目、`EntryEdit.applyTo()`、预期版本更新及一次冲突重试；旧 `feature.vault.detail` 包路径是共享能力命名/依赖泄漏候选。该候选已按上方记录迁到 `feature.vault.entry.EditEntryUseCase`，行为验证仍开放；loader 保留 Detail owner。
- [ ] 核实完整银行卡编辑能否保留已有 CardCredential.paymentPlatform：当前 editor form/mapper 未发现对应字段；沿 EntryEdit/materialization 写入链确认是否会丢失，若会则补齐回填/保留并覆盖回归。

<a id="task-40"></a>

### Task 40：让备份目录设置命令失败可见并保留授权状态
- [ ] 已增加 ViewModel/Interactor 单测与清除对话框仪器测试，覆盖异常、取消、fatal Error、授权保留/释放顺序及页面 owner；保留“先持久化新 URI、再释放旧 URI”顺序。完整 App 单测、AndroidTest Kotlin 编译、`verifyArchitecture`、`git diff --check` 已通过；需在连接设备后执行 `BackupDirectoryClearDialogTest` 再关闭本项。

**2026-11-08 失败反馈与日志批次：** `BackupDirectorySettingsInteractor` 将普通操作异常与取消/fatal Error 分开处理，补偿读取或授权释放中的 fatal 错误继续传播；提交后旧目录 grant 清理失败记录 `Backup.ResourceCleanupFailed`，不反转已提交结果。`DataManagementSettingsViewModel` 对选择/清除普通异常记录 `Backup.Error` 并发当前页失败 Effect，CancellationException 继续传播；页面退出后 selection 迟到结果仍被 owner gate 丢弃。清除 UI 等待成功后关闭，失败保留对话框重试；测试覆盖忙碌态按钮不可重复提交。定向 `BackupDirectorySettingsInteractorTest` 16/16、`DataManagementSettingsViewModelTest` 5/5 通过；完整 App 单测 XML 汇总 191 份、1064 项、0 failures/errors；`:app:compileDebugAndroidTestKotlin`、`verifyArchitecture`、`git diff --check` 通过。`adb devices` 无设备，仪器测试尚未运行，故 Task 40 保持未完成。

<a id="task-32"></a>

### Task 32：明确 Vault 列表 ViewModel 的工作流边界并补宿主行为覆盖
- [ ] 补齐授权撤销期间操作及其余 page owner 交接/迟到结果行为；纯 `VaultReducer`/`VaultQueryState` 单测仍不能代替 ViewModel 的协程和 `PageEffects` 所有权合同。
- [ ] 让 Route 消费与各 owner 对齐：`VaultRoute` 负责 Compose/导航/system effect，Content 只接收 immutable state/actions；确保图级 `VaultViewModel` 在 Trash/Detail 转场或目的地保留期间不会重复创建 effect collector 或重复订阅 Paging/OTP。给选定的 owners 增加 Route/NavHost 行为回归后同步状态管理文档，运行 Vault Presentation 定向测试、`verifyArchitecture` 和 `git diff --check`。

**当前证据（2026-11-08）：** `VaultViewModel` 保留列表查询、分页/筛选、设置排序、OTP Runtime 与同一 `VaultUiState`；分类读取由 `RecoverableRead` 管理并投影为 `Loading/Ready/Failed`。删除、字段复制和 OTP 复制由 `VaultEntryCommandWorkflow` 执行，它持有按条目删除单飞，并用当前 `PageEffects` owner 控制异步结果；VM 只分派当前页面动作，不再保留列表删除确认状态。`VaultViewModelTest` 已覆盖分类恢复、sort 写入失败、settings sort 驱动新 Paging generation、搜索与类型筛选组合、删除单飞、删除后 OTP 状态清理、退出页面后的删除失败抑制，以及离页/current-page copy；新增撤权期间锁定时不复制回归。`TrashCommandUseCaseTest` 现在验证授权等待期间锁定不会触达条目查询或命令。`VaultOtpSubscriptionEffectTest` 覆盖 OTP 行的组合生命周期订阅与退订。Route/NavHost 转场覆盖仍缺；`VaultGraphRegistration.vaultGraphViewModel()` 使用图级 VM owner，Route 的 destination 生命周期与 effect collector 仍需宿主验证。

**2026-11-08 Vault 列表删除确认死路径清理：** 调用闭包确认 `VaultScreen` 不消费 `dialogs.pendingDelete`，实际列表删除只由 `QuickDelete` 触发；移除无人调用的 `ItemToDeleteSelected` / `ConfirmDelete`、`pendingDelete` 状态/UI 投影、Reducer mutation 与 workflow 分支。保留真实的 Trash command、同条目单飞、当前 page effect 失败反馈和删除后的 OTP Runtime 清理。切换后全仓无 Vault 列表确认删除旧符号引用（Trash 页面自己的确认状态独立保留）；四个 Vault list 定向测试共 28 项通过，`verifyArchitecture` 通过。Task 32 的 OTP 可见行退订、撤权及 Route/NavHost 覆盖仍待处理。

**2026-11-08 Vault sort 写入失败回归：** 注入 `setSort` 普通异常，验证当前 page owner 收到 `vault_error_sort_failed`，而 `selectedSort` 继续等于设置 Flow 的已保存值。定向 `VaultViewModelTest.sortPreferenceFailureShowsCurrentPageFeedbackWithoutChangingSelectedSort` 通过（1 项）。

**2026-11-08 Vault 删除后 OTP 失效回归：** 先订阅并生成可见 OTP，确认删除完成后状态被清除；再模拟页面恢复刷新，验证旧订阅不会重新启用该条目。定向 `VaultViewModelTest.completedDeleteClearsVisibleOtpAndDoesNotReactivateItOnRefresh` 通过（1 项）。

**2026-11-08 Vault 迟到删除失败回归：** 删除授权等待期间退出页面，之后 Repository 返回失败；`PageEffects` 不向已退出 owner 投递错误。定向 `VaultViewModelTest.deleteFailureCompletingAfterPageExitDoesNotEmitStalePageEffect` 通过（1 项）。

**2026-11-08 Vault OTP 行订阅编译核对：** 既有 `VaultOtpSubscriptionEffectTest.subscriptionTracksVisibilityAndCompositionLifetime` 覆盖显示开关与组合销毁时的 unsubscribe；`:app:compileDebugAndroidTestKotlin` 通过。当前无连接设备，instrumentation 运行未验证。

**2026-11-08 VaultRoute 无效 owner 开关清理：** 全仓仅有 `VaultRoute` 自身声明和默认值为 `true` 的条件读取，没有调用方传入 `false`；删除 `isEffectsPage` 参数与不可达分支，保留 overlay 关闭时创建、overlay 打开时退出的现有 owner 生命周期。Route/NavHost 转场期间的 owner 行为仍待宿主验证。

**2026-11-07 分类 Flow 缺口（已处理）：** 旧实现由 `VaultViewModel` 直接 collect `availableCategories`，普通异常会结束 collector，空列表也无法表达加载或失败。现改由 `RecoverableRead` 管理分类订阅，并通过明确状态和 Retry action 暴露失败恢复；锁定/重开恢复由数据库 observation owner 管理，证据见下方。


## P2：恢复与交付合同

<a id="task-46"></a>

### Task 46：让认证方式 StateFlow 发布同一持久快照的最新解析结果

- [ ] 补真实 manager/store/provisioner 集成覆盖：同一持久快照解析、并发刷新、提交后刷新、调用方取消、恢复事件；状态对象单测不代替 manager 合同。
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

- [ ] 按本轮 app/Autofill Lint 新报告裁定剩余项，不沿用旧数量；清理 EmptySuperCall、冗余 API、UseKtx、资源时保留清理/持久时序及中英文合同。
- [ ] 依赖升级独立验证：catalog 当前 palette 1.1.0、Material3 1.5.0-beta02、DataStore 1.3.0、SQLCipher 4.20.0 已更新；旧“14 条升级提示”不再整体列为未实施。
- [ ] APK Analyzer 保存交付/release APK 的 DEX、资源、native、assets 基线；历史 debug 体积不是 release 优化收益，设备/性能验收按功能 owner 执行。

**2026-11-07 更正：** DetailViewModelTest 旧 CREATE/Lifecycle 夹具已改 AUTOFILL，实际 retry 并验证订阅两次及 Ready，移出待修。ScannerResultCard 的 Modifier 位置已调整，Lint 任务已在 entry 拆分后成功退出；当前测试失败已收敛并清零，不沿用 2026-11-02 统计。

<a id="task-83"></a>

### Task 83：复核 Repository 覆盖并把业务工作流移出大 ViewModel

- [ ] 以当前工作区版本审查 `DetailViewModel` 中的独立工作流，按“状态 owner/任务组/依赖闭包是否能一起迁走并实际减轻原 VM”决定拆分，不能只把函数搬到新文件或新建一一转发 UseCase。历史打开的全库初始化与当前条目 metadata 订阅顺序已迁入 `ObserveEntryRevisionMetadataUseCase`，ViewModel 保留页面订阅 generation、重试与 UI 失败投影；继续核对其余 Detail owner，并验证初始化失败重试、取消、会话锁定、metadata Flow 后续失败/重订阅及重复初始化幂等性。
- [ ] 评估 Detail favicon 编辑是否应从 `DetailViewModel` 提取为独立工作流 owner：当前 VM 同时发起 URI/URL staging、裁剪后图片处理、staged image promote、取消/过期结果回收及 `EntryEdit.SetIcon` 持久化；`DetailFaviconSession` 已拥有准备/保存 Job 与资源释放的一部分合同。按职责将图像处理/条目命令放在合适 UseCase，将裁剪输入、临时路径和页面生命周期放在 Feature 工作流 owner；不得把 Android/UI 会话状态塞进通用 UseCase，也不得拆出不能减少 ViewModel 依赖、分支或异步 owner 的薄包装。必须保持过期会话不回写、promote 后失败回收、取消/关闭时敏感与文件资源清理及提交结果一致性。
- [ ] 同一轮核对 Detail 的其余候选及最大 ViewModel：保留 `ActivityQueryRepository` 等简单只读 Flow 的页面订阅在 VM（包括异常态、重试 owner），已有 `EditEntryUseCase`、revision compare/reveal/restore、copy、QR 等完整动作不要再包一层；对 Backup、Credential Response、Diagnostics、Unlock 等大文件检查是否有跨端口业务决策或事务流程仍留在 VM，再依据证据决定提取或保留。只将有业务意图/不变量、复用价值、跨端口编排或独立取消/重试合同的流程归入专属 UseCase/工作流；UI 草稿、屏幕投影、Action 分发和生命周期任务仍由对应 Presentation owner 管理，防止造出上帝 UseCase。
- [ ] 对每个实际拆分补充 owner 级行为测试：验证旧 ViewModel 状态与依赖确有减少，UseCase 输入/结果不泄漏敏感值，并覆盖取消、失败重试、session/entry 替换后的迟到结果和资源收尾。同步必要的主题文档与本 TODO 证据；运行受影响测试、`verifyArchitecture` 与 `git diff --check`。本任务是待执行的维护审计与迁移方向，不表示所有大 ViewModel 都需要拆分。

**2026-11-08 修订历史订阅 owner 迁移（部分）：** `DetailViewModel` 已移除对 `EntryRevisionRepository` 初始化/观察的直接调用，改用 `ObserveEntryRevisionMetadataUseCase` 顺序执行历史初始化与当前条目 metadata 订阅；初始化失败不订阅，取消原样传播。UseCase 的顺序/失败/取消测试与 ViewModel 初始化失败后重试测试均通过，`DetailViewModelTest` 定向类通过。Task 83 仍开放：继续验证 session 锁定、替换后迟到结果及 Detail 其他 owner，再决定是否拆分 favicon 等职责。

**初始审计证据（2026-11-06）：** 静态盘点到 17 个 Repository 契约及对应 Hilt 绑定；暂未发现需要增补接口的证据。ViewModel 文件行数快照中 `DetailViewModel.kt` 为 1208 行，依赖同时包括两个 Repository、`EntryTagQuery`、`FaviconImageProcessor` 及多个既有 UseCase。已验证 `observeRevisionHistory()` 先直接调用 `entryRevisionRepository.initializeHistory()`，其 Data 实现会在事务中重建 revision history、安排 attachment GC，并进行 post-commit 图标/附件清理，属于应从大 ViewModel 归位的明确业务维护流程。Activity history 的 Repository Flow 收集是页面加载/错误投影，不据此要求抽 UseCase。Detail favicon 路径存在多个资源与生命周期阶段，已部分封装于 `DetailFaviconSession`，将其列为工作流边界审查而非预断必须全部迁入 UseCase。Backup（385 行）、CredentialResponse（364 行）、Diagnostics（350 行）、Unlock（344 行）只按快照列为扫描范围；尚未完成其业务职责判断。该段为 2026-11-06 静态审计历史；本轮构建/测试结果统一见扫描验证记录。

**修订历史调用闭包复核（2026-11-06）：** `openRevisionHistory()` 从 `_uiState.value.entry.id` 取 `entryId`，检查同条目历史已打开后取得 `workSession.current(entryId)`；随后取消旧的 `revisionObservationJob` / `revisionOperationJob`、清 reveal 并发布 `Revision.Opened(entryId)`，调用 `observeRevisionHistory(entryId, token)`。重试入口只接受当前 `DetailRevisionState.Open` 中 `page == List` 且 `revisions is Failed`，用同一 `state.entryId` 取当前 token、清 job、重发 Opened 后再次调用 `observeRevisionHistory(entryId, token)`。该函数增加 `revisionObservationGeneration`，在 `viewModelScope.launch` 中调用无参 `entryRevisionRepository.initializeHistory()`；`AppResult.Failure` 只有在 `workSession.isCurrent(token)` 且 generation 未变时才以 `mutateRevisionFailure(entryId, revisionId = null, Failed(error))` 投影，否则丢弃迟到结果；Success 才开始 `entryRevisionRepository.observeMetadata(entryId).collect`，每个列表仅在 token/generation 仍有效时转为 `Revision.MetadataChanged(entryId.value, revisions)`。取消重抛，`SessionLockedException` 由会话撤权路径清理，其他普通 `Exception` 在 owner 仍有效时映射为 `DetailRevisionFailure.UNEXPECTED`。Data `RoomEntryRevisionRepository.initializeHistory()` 无参数地委托 `historyInitializer.execute()`；该 initializer 先在 Mutex 内读取 `maintenanceStore.completedGeneration()`，若低于 `CURRENT_GENERATION = 1`，以 `databaseTransactions.write("entry_revision_history_initialize")` 执行 `rebuilder.rebuildInTransaction(db, clock.now())`；该重建器内部清空旧修订，再遍历未按 `deletedAt` 过滤的 `entryQueryDao().getAll()`（含回收站），并为每项复用同一个 `now` 调用 `EntryRevisionWriter.snapshotChanges(db, entryId, entryVersion, summary, secret, entryType, now)`；缺少 `STRUCT_BUNDLE` 时使用空 `EntrySecret``，对旧图标引用调用 `entryResourceCleaner.discardUnreferencedIcon()`，并 `attachmentGarbageCollector.scheduleInTransaction(db)`；transaction 成功后写 `maintenanceStore.markCompleted(1)`，再分别执行 `postCommitCleanup.run { attachmentGarbageCollector.drain() }` 与 `postCommitCleanup.run { entryResourceCleaner.drainPendingIcons() }`。因此跨端口的一次性初始化、数据库写事务和提交后资源维护不是简单读取，应评估移到具备启动/首次打开 owner 的应用流程；ViewModel 对 `entryId + token + generation` 的页面订阅、失败投影、重试和旧结果丢弃继续由页面 owner 持有。迁移需保留懒触发语义，并验证重建幂等、提交后 cleanup、锁定/取消及 metadata Flow 异常后的重订阅。

**2026-11-08 Vault 边界审查续记：** `DetailFaviconSession`、`DetailQrSession`、`DetailPasswordBindingWorkflow`、`DetailWorkSession` 均实际拥有任务、资源或访问页生命周期，保留；revision restore 与 Detail Reducer、OTP runtime、根快照刷新共享当前 visit token，暂留 Detail owner。历史初始化后订阅 metadata 的协议已进 `ObserveEntryRevisionMetadataUseCase`，并补了顺序/失败/取消测试。Vault 页面审查与调用闭包完成；历史验收证据见[审查记录](../../reviews/2026-11-08-vault-presentation-boundaries.md)。Task 83 的跨 Feature 维护审计仍开放。

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
- [ ] 明确单一数据真相源合同并审查潜在双份状态：持久业务数据以 Repository/Domain 为真相源，ViewModel 的屏幕状态是其可丢弃投影；用户编辑草稿、筛选输入、展开状态等可以是独立的屏幕/交互状态，但须有明确 owner、生命周期和提交/丢弃语义。排查从 Repository 读取后另存一份可独立修改的长期副本、Flow 与手动赋值竞争同一字段、多个 ViewModel/Compose state 同时声称拥有同一状态等情形；区分有意未提交草稿与意外镜像，禁止把 Action 另做一条并行业务状态真相。保留 Paging 等确有独立语义的输出，不为形式统一强塞到一个 State，也不引入全局 EventBus。
- [ ] 按统一契约审查所有页面并制定分批迁移清单：合并重复的 Action/状态/事件转发与纯桥接；移除没有带来明确状态转换保证或可测试性的 MVI ceremony；让真实多输入/异步工作流仍按 UDF 汇入屏幕状态，必要时用纯 Reducer。只在能明确减少职责、重复或依赖时改代码；不得以统一为由制造上帝 ViewModel、万能 Reducer/Effect handler、全局状态容器或新的转发层。复杂状态机、认证提交顺序、协议分支、敏感数据清理与平台生命周期合同不得表驱动化抹平。
- [ ] 对所有 ViewModel 做职责密度与依赖闭包审计，标出同时拥有多个独立业务流程/状态机、宽依赖集合、重复决策映射或不同取消/重试任务组的候选；按[ViewModel owner 边界](../../architecture/viewmodel-ownership.md)和[维护边界](../../development/maintenance-boundaries.md)，结合调用方、状态 owner、事务/安全边界决定保留或拆分，不能仅按行数拆。
- [ ] 为每个拆分候选确定逻辑 owner：纯且可复用的业务判断归 Domain Policy/规则；完整业务动作及验证、不变量、可复用流程或事务/跨端口编排归职责匹配的 UseCase/应用服务；简单单一 Repository CRUD 保持直接调用，不造一对一转发 UseCase；平台生命周期和系统 API 留在 Route/Activity。UseCase 也须单一职责，提取后验证原 ViewModel 的依赖、分支或异步职责实际减少。
- [ ] 对跨层流程追踪 `Presentation → Application/UseCase → Domain Port/Policy → Repository → Data` 的真实依赖方向，检查错误/取消/重试、durable commit、认证、敏感数据清理和调用方闭包；不要把规则搬进 ViewModel，也不要把大 ViewModel 原样搬进 God UseCase。拆分异步 owner 时增加 VM/Route/宿主行为覆盖，不以 Reducer 或纯 UseCase 单测代替。
- [ ] 逐页迁移确认实际合同变化后，按需更新正式文档代码示例与例外；统一术语和基础规则已完成，不再重复立项。
- [ ] 按 feature/owner 分阶段实施迁移；异步编排或状态 owner 变化须有 ViewModel/Route/宿主行为覆盖，验证 Flow 失败与重订阅、CancellationException/fatal Error、离页迟到结果、配置重建、Effect 前后台交付、Repository 提交后状态与敏感值擦除。保留无需迁移且符合模板的页面，并记录理由；每阶段运行受影响模块测试、`verifyArchitecture`、相关 lint/编译门禁与 `git diff --check`。当前 Detail/完整表单编辑器存在未提交 owner 重构，先以稳定后的代码为基线，保留并适配工作区改动。

**2026-11-07：retained，仅剩逐页审计/迁移。** architecture-and-state.md 已明确分层 MVVM + UDF、Repository 真相源、可选 Reducer 和 UseCase owner；viewmodel-ownership.md 已有对应边界。移除“正式规范仍分四套架构”的过期事实。Task 84 持有 ViewModel/Coordinator 调用闭包清单，Task 83 持有具体 Detail 候选，本 Task 只管状态生产、真相源及 action/effect 模式，不复制 owner 清单。

<a id="task-35"></a>

### Task 35：验收 EntryLink 写入与端点删除竞态

- [ ] 以真实 Room AndroidTest 交错验证端点删除与 `EntryLinkRepository.upsert()`：允许 upsert 先提交后由删除级联清除，或删除先提交后 upsert 拒绝；最终不得留下指向缺失或已删除端点的 link。覆盖既有 link ID 更新与 endpoint 唯一约束路径。

<a id="task-34"></a>

### Task 34：收窄未消费的 Activity 查询流
- [ ] 在可用设备上运行 `RoomObservationSessionTest` 与 `EntryUpdateAndRevisionRestoreTransactionTest`；当前未连接 Android 设备，instrumentation runtime 尚未验证。

**证据：** `ActivityQueryRepository.observeByEntryId()` 唯一生产调用位于 `DetailViewModel`，支持详情活动记录；`observeAll()` 与 `observeByType()` 没有生产 consumer。全仓搜索 `EntryActivityQueryDao` 的方法后，确认 usage/mutation 四类观察、四个 paging、`getById()`、`exists()`、`count()` 和两个 usage/mutation counts 只在 DAO 声明。`observeAll()` 由 `RoomObservationSessionTest` 用作 session gate 集成测试入口；`getByEntryId()`、`countByEntryId()`、`countByEntryIdAndType()` 在 `EntryRevisionRestoreTransactionTest` 中验证恢复行为，需要保留覆盖。`EntryActivityAnalyticsDao` 的四个聚合查询只在 DAO 声明及 `AppDatabase` accessor 出现，没有生产调用者；列表 usageCount/lastUsedAt 实际由 `EntryPagingQueryFactory` 内联 SQL 生成。现有 `DetailViewModelTest` fake 实现全量/类型方法仅为满足接口，没有行为断言；未发现正式文档声明这些额外 API 合同。

**2026-11-08 批次：** 删除 `ActivityQueryRepository.observeAll()` / `observeByType()`、Room adapter 对应实现及 DetailViewModelTest 的空 fake 方法。当前生产消费者只调用 `observeByEntryId()`；全仓引用扫描确认剩余接口引用仅为其生产注入与测试 fixture。未删除 `EntryActivityQueryDao.observeAll()`，因为 Room session gate 仪器测试直接覆盖该查询；其余 DAO 收敛留待下一批。此批次尚未运行编译或测试。

**2026-11-08 批次：** 删除未被引用的 `EntryActivityAnalyticsDao`、`AppDatabase` accessor 和聚合结果 DTO；精确符号扫描只找到 DAO 声明及 accessor，未发现 Kotlin/Java consumer。`EntryPagingQueryFactory` 已承担列表 usageCount/lastUsedAt 查询。未改数据库实体或 schema；未运行测试。

**2026-11-08 批次：** 从 `EntryActivityQueryDao` 删除全表/按类型与 usage/mutation 观察、全部 paging、单项 ID、exists、总数及分类 count 等无消费者方法；保留生产详情流和 revision restore 仪器测试消费的三个精确查询。session gate 用例改用 `observeByEntryId("entry-a")`，保留锁定/解锁订阅行为。`:data:compileDebugKotlin`、`:data:compileDebugAndroidTestKotlin`（61 项任务执行）、`verifyArchitecture`、目标文件 `git diff --check` 通过；全仓扫描未找到被删方法调用。`adb devices` 无设备，未运行 instrumentation tests。

<a id="task-36"></a>

### Task 36：按生产消费闭包收敛修订 Room DAO
- [ ] 在可用设备上运行受影响的 Data instrumentation tests；当前无连接设备，运行时数据库行为尚未验证。

**证据：** 当前生产 Repository 通过 `observeByEntryId()` 发布修订元数据，通过 `getById()` 加载、揭示和恢复目标修订；`EntryRevisionWriter`、永久删除、清空回收站与图标回收路径消费其余写入、裁剪及图标引用查询。`getByEntryId()`、`getLatest()`、`countAll()` 和 `insertAllStrict()` 仅被仪器测试使用，现已改用生产 Flow 和逐条严格插入；Import upsert 与 `deleteBefore()` 无调用点，且 [`docs/features/backup/backup-workflows.md`](../../features/backup/backup-workflows.md) 明确完整备份不含修订历史。更新保留历史上限、恢复后最新快照、清空回收站及附件/图标引用保护行为断言。

**2026-11-08 批次：** 删除修订 DAO 中无消费者的查询、Import 写入和过期清理方法；把所有测试专用 get/count/batch insert 调用迁到 `observeByEntryId()` 与逐条 `insertStrict()`，保留行为断言。未改表结构或 schema。`:data:compileDebugKotlin`、`:data:compileDebugAndroidTestKotlin`、`verifyArchitecture` 通过；目标文件 `git diff --check` 通过；instrumentation tests 因当前无连接设备未运行。

<a id="task-74"></a>

### Task 74：按测试对象优化单测结构与重复装配
- [ ] Detail 生产 owner 稳定后再评估是否拆分 `DetailViewModelTest.kt`；按独立工作流和生命周期职责决定，不以行数拆分，避免搬动仍在变化的 fixture 合同。
- [ ] 继续全仓 `src/test`、`src/androidTest`、`src/testFixtures` 的逻辑审查；逐项追生产 owner、输入/状态/副作用断言及失败/取消清理，再决定合并或删除。不同模块、平台生命周期、稳定协议键和安全策略保持各自 owner。详细扫描计数不在此维护，避免把动态快照误作完成证明。

<a id="task-75"></a>

### Task 75：统一固定常量的文件位置与 owner，收敛重复合同值

- [ ] 继续按语义 owner 审查 companion 常量；仅将确属文件级的实现值移至文件顶部，类型/协议专属值归入准确 owner，不为位置变化改动算法或 wire 值。
- [ ] 验证新增/迁移后的可见范围、所有调用点和单一真相源；持久化 key、wire tag、格式版本不得改变，安全限制不得被放宽。补充/迁移对应常量与协议测试；运行受影响模块单测、`verifyArchitecture`、lint 和 `git diff --check`。同步更新 Kotlin 规范中的位置规则；不要建立无 owner 的全局 `Constants.kt`，也不要把资源文本、Gradle 构建值或运行时可调配置复制成 Kotlin 常量。

**扫描与批次结论：** 初始 360 个 `const val` 的扫描只是历史快照；本任务已按消费者和语义 owner 完成多批归位/去重，已完成范围见本摘要。涉及的 owner 包括日志分页、Favicon 输入与裁剪、App 关闭提醒、Autofill Inbox 协议/算法、Security 与 Backup 加密标识、版本化日志格式，以及 Session/授权/认证实现私有值。迁移保留原安全边界、持久化 key、协议字段和 wire 值；测试期望保留独立字面量。

**验证缺口：** 相关定向单测、受影响模块编译和 `verifyArchitecture` 已分批通过；Favicon Store Android instrumentation 因无连接设备未执行。Core/Data/Security lint 已通过，App lint 在 `:app:lintAnalyzeDebug` 遇到 IntelliJ `AsyncExecutionService.getService must not return null`，没有有效结果，须修复工具环境后重跑。故本 Task 的完整验证仍开放。
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
- [ ] 为 Route 生命周期补宿主行为验证：OTP 编辑成功返回时 Detail 刷新当前条目，编辑取消/失败不会触发保存成功路径；Vault 列表恢复时只刷新仍可见订阅，离开后不重新激活无 owner 条目。当前无跨 ViewModel 全局广播桥。
- [ ] 增加 Route/编辑器宿主行为回归：OTP secret 保存成功后返回 Detail 时展示与 copy 使用新配置；离页/锁定期间迟到结果不得覆盖当前状态。运行 OtpCodeRuntime、DetailViewModel 与 Vault OTP 订阅相关测试、`verifyArchitecture` 和 `git diff --check`。

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

## 全项目测试逻辑清理进度（进行中）

- 范围覆盖所有模块的 `src/test`、`src/androidTest` 和 `src/testFixtures`。审查标准是生产 owner、行为合同和断言实际证明的结果；同名、同主题或文件短小本身都不是合并依据。
- 已在多个模块完成候选交叉核对和部分测试逻辑修正，但全项目审查未完成，尤其 App/Feature 仍需继续逐 owner 检查。此前逐轮源码数量会随并行改动变化，已从本计划删除，不再作为完成证据。
- 本轮 Detail mapper 复核为 identity、SSH、seed phrase、passkey 增加直接状态投影断言，并将 Wi-Fi 凭据断言迁到当前可空行模型；新增/调整测试尚未执行。相同文件名候选也按 owner 对照：AppBackup unit/androidTest 分别验证 restore 与 read，三个模块的 `TestLogger`、两个模块的 `IsolatedTestContext` 是源集/模块隔离的测试支撑，未发现可安全合并的重复行为测试。Data `SettingsDataStoreReadTest` 补了 fatal `Error` 原样传播断言（与 corruption 包装、I/O、取消保持不同合同）；本轮未运行测试。Unlock 的新“启动不读 attempt status”测试只覆盖无启动查询；静态追踪确认认证失败在 `finally` 清凭据时触发 `AppPasswordChanged`/`RecoveryCodeChanged`，reducer 随即清除失败态，需补 owner 行为覆盖并修正错误反馈交付。未关闭事项仍包括 Task 74 的全仓逐例复核与 Detail ViewModel 测试拆分评估、Task 42 的银行卡 `paymentPlatform` 编辑保留核实，以及 Data `InitializeEntryRevisionHistory` 的 generation 标记、事务失败重试和提交后清理 owner 测试。历史记录中 `CompareEntryRevisionUseCase` 的“无直接测试”说法已过时，当前 `EntryRevisionUseCasesTest` 已覆盖其版本选择及加载失败映射。
- 本轮没有运行测试；完成度须以尚未审查的测试 owner 与未关闭事项为准，`git diff --check` 只验证文本空白，不代表测试通过。
