# Passly 深度维护审查待办

**最近部分复核：2026-11-11。** 本轮将已验收的 Runtime Session lease 生命周期、DatabaseReset 成功反馈计时器，以及通知分发平台测试移出活动待办，并分别归档到正式主题页与审查记录；其余范围仍沿用先前全模块 Kotlin 静态盘点和生产调用闭包证据，不等同于全仓安全扫描或逐行证明。

## 执行顺序

保留原 Task ID，正文按批次重排。P0 表示工程门禁阻断，P1 为敏感/资源安全或具体运行流程缺口，P2 为恢复/交付合同，P3 为结构维护。**retained** 表示仍有证据支持的工作；**deferred/待验收** 表示已有机制但指定验证未完成；**removed** 仅移除过时缺陷，不代表整项已通过。编号不代表优先级。

| 顺序 | 执行入口 | 依赖与原因 |
| --- | --- | --- |
| 1 / P1 | [安全 Task 6](2026-11-06-passly-security-scan-followups.md#task-6)、[Session 与 DEK 审查记录](../../reviews/2026-11-07-session-close-and-dek-wipe.md)，本页 37、59 | 接收方身份、lease 关闭、撤权与提交事实；Runtime 自身 lease 排空已验收，继续闭合 Data admission 与 Security/reset 跨层终态 |
| 2 / P1 | 本页 42、40、32；[诊断 Tasks 1–3](2026-11-06-passly-diagnostics-reliability-followup.md) | seed/流资源、目录操作终态、分类查询与 flush；先固定失败结果再改 owner |
| 3 / P2 | 本页 46、51；[安全 Tasks 3、7、9](2026-11-06-passly-security-scan-followups.md)、OTP/搜索专题；Autofill Task 82（设置策略） | 46 接 59 的提交后恢复；Autofill 设置策略待办由专题清单维护 |
| 4 / 定向验收 | 本页 22、27、29、31、44、45、64；[Autofill 宿主验收](2026-11-07-passly-autofill-followups.md#task-21)、Scanner 验收 | 已有机制，不重复实现；JVM、Route/宿主和设备验证分别标记 |
| 5 / P3 | 本页 83 → 84/80，35、34、36、74、75、77 | 83 管 Detail 具体流程，84 管 owner 清单，80 管状态模式；77 先稳定诊断可靠性 |

安全 Task 1/5/10、Autofill 设置快照和非 ASCII 搜索仍有策略合同待裁定；本轮不臆定产品行为，不把候选升级为已验证漏洞。OTP 列表 HOTP 复制已由当前实现确定为支持，剩余 owner 级行为断言由 OTP 专题 TODO 跟踪。允许独立项并行，表中箭头表示必须先满足的前置条件。

## 专题 owner 索引

| 主题 | 唯一详细待办/验收来源 | 本页关联 |
| --- | --- | --- |
| 架构与并发 | [架构与并发 TODO](2026-11-06-app-architecture-concurrency-todo.md) | 37、51、80、83、84；具体缺陷先处理，广域提案随后 |
| Autofill 后续 | [唯一后续清单](2026-11-07-passly-autofill-followups.md) | 21 宿主验收、82 设置策略；其他已完成合同已归入主题文档 |
| Scanner | [输入与生命周期合同](../../../product/scanner/input-lifecycle.md)、[结果交接合同](../../../product/scanner/result-handoff.md)、[取景框计划](2026-11-06-scanner-entry-and-framing.md) | 42、51；Route/Activity Result 关闭交错与图片/坐标验收仍待验证 |
| Vault / OTP | [搜索 TODO](2026-11-06-passly-vault-search-review-followups.md)、[OTP TODO](2026-11-06-passly-otp-review-followups.md) | 27、32、34–36、42 |
| Backup / 诊断 | [Backup 格式说明](../../../product/backup/passly-archive-v1.md)、[数据端口迁移审查记录](../../reviews/2026-11-07-backup-data-port-migration.md)、[诊断可靠性 TODO](2026-11-06-passly-diagnostics-reliability-followup.md) | 40、73、74、77 |
| 安全与资源 | [安全遗留 TODO](2026-11-06-passly-security-scan-followups.md) | 37、44–46、59、64；Runtime lease 关闭验收归档在 Session 审查记录 |

<a id="task-37"></a>

### Task 37：验收认证撤权与数据库操作的线性化终态
- [ ] 增加可控挂起的跨层终态回归：在 Security lock 等待数据库 lease/resource 与敏感 key 清理时，从 runner、直接 Full Vault Repository 与 Autofill Repository 发起读写，验证线性化点之后新命令不触达 DAO；已进入事务准确报告提交/回滚/取消结果，敏感 permit、会话、DEK、key 与 lock state 一致。边界与真实 Repository 集成测试证明调用闭包后，不需为每个薄适配器复制同一测试。
- [ ] 在可用设备上验收真实 Data lease/Room DAO 与 Security reset/lock 交错终态；当前 `adb devices` 无连接设备，instrumentation runtime 尚未执行。恢复模式、Autofill session 结束和 idle timeout 的撤权次序仍需按本项统一验收。

Session 撤权顺序、数据库准入与 Room observation 的当前合同，以及已完成迁移的历史验证边界，见[数据库撤权线性化审查](../../reviews/2026-11-11-database-revocation-linearization.md)；权威运行规则见[数据库专题](../../../platform/data/database.md)和[Session/DEK 审查](../../reviews/2026-11-07-session-close-and-dek-wipe.md)。本 Task 仅保留跨层 owner 终态和设备交错验收。
<a id="task-59"></a>

### Task 59：闭合生物识别轮换的提交后恢复与启动对账

- [ ] 增加 `RotateBiometricKeyUseCase` + `DefaultAuthenticationMethodProvisioner` + 持久 Store 的 owner 级回归：`BiometricRotationCandidateTest.cleanup state read failure after committed success preserves success and requests retry` 已覆盖 helper 层 committed cleanup 失败；剩余需验证 Provisioner 合并 biometric cleanup 与 availability refresh pending 状态、持久 binding 保持成功，并确认维护重试不会盲目重复轮换。
- [ ] 用真实 Authentication Host/Provisioner/启动 owner 覆盖轮换 commit 前后异常、取消与 fatal、alias/journal 恢复和 host lease 释放。`BiometricRotationCandidateTest` 与 `BiometricRotationReconcilerTest` 已覆盖 helper/reconciler 级边界，不重复这些单元场景；manager 同快照与并发刷新由 Task 46 跟踪。
- [ ] 补齐 `AppPasswordSettingsInteractor` / `AppPasswordSettingsViewModel` 与真实 Provisioner/持久 Store 的 owner 联动：现有 VM 回归已覆盖操作挂起时 ViewModel 清理取消、Provisioner 提交前取消、持久状态已更新后的提交后取消、缓冲清理、`isEnabled` Flow、页面结果与重复提交；仍需验证真实持久 Store/Provisioner 交错时结果与页面状态一致。`AppPasswordProvisioningCommitTest` 覆盖 envelope 持久化后的取消传播。

**底层实现和测试证据：** 已归档于[认证提交后维护审查](../../reviews/2026-11-11-authentication-post-commit-recovery.md)，权威合同见[认证机制与会话](../../../platform/security/authentication.md)。剩余项仅要求跨 provisioner、持久 Store、ViewModel 与真实 Host 的 owner 联动验证。


<a id="task-42"></a>

### Task 42：验收完整表单编辑的 Route/NavHost 失败交接
- [ ] 在 Compose Route/NavHost 或设备宿主验收 Detail 预加载失败提示与重试、STOPPED 取消和 owner 替换、编辑导航失败提示与 seed 清理、无效/已消费 seed 返回重试，以及配置重建和迟到 callback。JVM 与 Session/Coordinator 测试不代替宿主交互；当前合同和已有验收证据见[完整表单编辑审查](../../reviews/2026-11-11-full-form-editing-boundaries.md)及[seed Coordinator owner 审查](../../reviews/2026-11-08-vault-presentation-boundaries.md#2026-11-12-entryeditorseedcoordinator-owner-审查)。


<a id="task-40"></a>

### Task 40：验收备份目录清除确认交互
- [ ] 在连接设备后执行 `BackupDirectoryClearDialogTest`，记录设备/API 与结果。此前 ViewModel/Interactor 单测、AndroidTest Kotlin 编译、`verifyArchitecture` 和 `git diff --check` 的实现合同与验收结果已迁入[目录设置失败处理审查](../../reviews/2026-11-08-backup-directory-settings-failure-feedback.md)；设备运行时验收仍未完成。

<a id="task-32"></a>

### Task 32：明确 Vault 列表 ViewModel 的工作流边界并补宿主行为覆盖
- [ ] 补齐授权撤销期间操作及其余 page owner 交接/迟到结果行为；`VaultListUiMapperTest` 与 `VaultQueryGenerationTest` 只覆盖纯列表映射和查询代际转换，不能代替 ViewModel 的协程和 `PageEffects` 所有权合同。
- [ ] 补齐 `VaultRoute` / NavHost 的实际 owner 转场验收：覆盖 Trash/Detail 转场、owner 替换，以及撤权时 effect collector 与 Paging/OTP 订阅不重复；设备运行与状态管理文档核对仍待完成。当前图级 ViewModel、列表行与 Detail 的状态及 OTP lease 合同见[专题页](../../../product/vault/otp-runtime.md)和[owner 审查记录](../../reviews/2026-11-11-vault-list-owner-followup.md)；已删除的旧测试与重复 ViewModel 用例不再作为待办描述。

当前列表状态、命令和图级 OTP lease 合同见[列表与搜索专题](../../../product/vault/list-and-search.md#代码位置)；实现与历史验证证据见[Vault 列表 owner 审查记录](../../reviews/2026-11-11-vault-list-owner-followup.md)。本 Task 只保留撤权/owner 交接与真实 Route/NavHost 转场验收。


## P2：恢复与交付合同

<a id="task-46"></a>

### Task 46：让认证方式 StateFlow 发布同一持久快照的最新解析结果

- [ ] 补真实 manager/resolver/store/provisioner 集成覆盖：同一持久快照解析、并发刷新、提交后刷新、调用方取消及恢复事件；同时为启动首次刷新失败提供可观察状态和重试投影。当前 `AuthenticationMethodsStateTest`、`AuthenticationAvailabilityRetryTest` 和 `AuthenticationMethodsMappingTest` 分别只验证状态协调、重试调度和纯 snapshot→methods 映射；测试源码中没有构造 `DefaultAuthenticationManager` 或 `AuthenticationAvailabilityResolver`，不能算 owner 集成覆盖。认证设置变更后的提交后刷新失败已有 Domain pending 状态与页面提示。

认证方式状态协调、manager 重试合同和状态 owner 测试见[认证专题](../../../platform/security/authentication.md#host-与生物识别)；本 Task 只保留 manager/store/provisioner 集成及启动失败反馈验收。

<a id="task-51"></a>

### Task 51：明确非 Scanner Route 一次性 effect 的前后台交付合同

- [ ] 逐项定义 Toast/Snackbar、导航、SAF picker、权限和平台交付在 STOPPED、恢复、离页、owner 替换后的有效期；短命反馈明确是否丢弃，待完成请求明确如何恢复。
- [ ] 补真实 Route/宿主测试，逐类覆盖转场、请求代际、锁定/关闭及 callback 异常；分别判断各 effect 应消费、丢弃、重试或恢复，不把不同 effect 塞入统一 handler 表。

共享 `PageEffects` 的 STARTED 收集、页面 owner 隔离及缓冲恢复合同见[状态管理专题](../../../engineering/development/architecture-and-state.md#页面一次性事件的可见性与-owner)。本 Task 只跟踪不同 effect 的有效期决策和 Route/宿主覆盖。


## P3：按 owner 分批整理

<a id="task-73"></a>

### Task 73：把官方 Android 检查纳入维护复核并处理已发现的质量项

- [ ] 按本轮 app/Autofill Lint 新报告裁定剩余项，不沿用旧数量；清理 EmptySuperCall、冗余 API、UseKtx、资源时保留清理/持久时序及中英文合同。
- [ ] 依赖升级独立验证：catalog 当前 palette 1.1.0、Material3 1.5.0-rc01、DataStore 1.3.0、SQLCipher 4.20.0 已更新；旧“14 条升级提示”不再整体列为未实施。
- [ ] APK Analyzer 保存交付/release APK 的 DEX、资源、native、assets 基线；历史 debug 体积不是 release 优化收益，设备/性能验收按功能 owner 执行。

<a id="task-83"></a>

### Task 83：复核 Repository 覆盖并把业务工作流移出大 ViewModel

- [ ] 继续审查 `DetailViewModel` 中的其余工作流；只有状态 owner、任务组和依赖闭包能一起迁走并实际减少 ViewModel 职责时才拆分。修订历史已有 metadata 首次失败重试、初始化失败重试、fatal 传播、关闭后重开时隔离旧观察者，以及持久 generation 初始化幂等/并发串行的测试。剩余需验证 metadata Flow 发出数据后再失败的重订阅、修订观察期间会话锁定、条目或 Detail visit owner 替换时的迟到结果，并核对其余 Detail owner。当前合同与测试覆盖边界见[修订历史审查记录](../../reviews/2026-11-08-vault-presentation-boundaries.md)与[修订历史专题](../../../product/vault/entry-revision-history.md)。
- [ ] 同一轮核对 Detail 的其余候选及最大 ViewModel：保留 `ActivityQueryRepository` 等简单只读 Flow 的页面订阅在 VM（包括异常态、重试 owner），已有 `EditEntryUseCase`、revision compare/reveal/restore、copy、QR 等完整动作不要再包一层；对 Backup、Credential Response、Diagnostics、Unlock 等大文件检查是否有跨端口业务决策或事务流程仍留在 VM，再依据证据决定提取或保留。只将有业务意图/不变量、复用价值、跨端口编排或独立取消/重试合同的流程归入专属 UseCase/工作流；UI 草稿、屏幕投影、Action 分发和生命周期任务仍由对应 Presentation owner 管理，防止造出上帝 UseCase。
- [ ] 对每个实际拆分补充 owner 级行为测试：验证旧 ViewModel 状态与依赖确有减少，UseCase 输入/结果不泄漏敏感值，并覆盖取消、失败重试、session/entry 替换后的迟到结果和资源收尾。同步必要的主题文档与本 TODO 证据；运行受影响测试、`verifyArchitecture` 与 `git diff --check`。本任务是待执行的维护审计与迁移方向，不表示所有大 ViewModel 都需要拆分。

<a id="task-84"></a>

### Task 84：全量审计 ViewModel 与 Coordinator 的职责归属并整理分层规范

- [ ] 对 `:app`、`:security`、`:feature:autofill` 中的生产 ViewModel 逐个完成调用闭包审计，记录保留、迁移、合并或继续调查的结论与证据；不按行数或类名自动判断。

  - Shell/宿主：`AppShellViewModel`、`AuthenticationHostOwnerViewModel`。
  - Vault：`VaultViewModel`、`TrashViewModel`、`DetailViewModel`、`ScannedOtpImportViewModel`、四种 `Add*ViewModel`。
  - Settings/Backup：`AppThemeSettingsViewModel`、`AutofillSettingsViewModel`、`SecuritySettingsViewModel`、`RecoveryCodeSettingsViewModel`、`PrivacySettingsViewModel`、`AppPasswordSettingsViewModel`、`InterfaceSettingsViewModel`、`AppearanceSettingsViewModel`、`PermissionManagementViewModel`、`InteractionSettingsViewModel`、`NotificationSettingsViewModel`、`DiagnosticsViewModel`、`DataManagementSettingsViewModel`、`BackupViewModel`、`StorageSettingsViewModel`。
  - Autofill：`AutofillFillViewModel`、`CredentialResponseViewModel`、`AccessibilityFillViewModel`，均属于 `:feature:autofill`。
  - Scanner/Unlock/Recovery/Database：`ScannerViewModel`、`UnlockViewModel`、`RecoveryModeViewModel`、`DatabaseResetViewModel`。

  同时核对每页 Route/Activity/Content 的实际消费者、DI 绑定、相关测试及调用的 UseCase/Interactor/Repository/Coordinator/Store，不只检查 ViewModel 文件。
- [ ] 在真实 Route/Activity 生命周期中验收 Recovery Code 确认后离页、Activity recreation 与 owner 替换时的 durable commit、disclosure 擦除及旧页面状态隔离。提交结果协调已确定保留在 `RecoveryCodeSettingsViewModel`：它将持久 marker 核对结果投影到当前页面状态；Interactor 已拥有 commit 与 durable marker，不再增加只有回调、不能拥有页面状态的应用操作转发层。单测源码覆盖关键路径但不证明真实宿主行为。当前合同见[认证专题](../../../platform/security/authentication.md#恢复码草稿)，决策与代码证据见[恢复码 owner 审查](../../reviews/2026-11-11-recovery-code-draft-owner.md)。
- [ ] 审查 `AppShellViewModel` 对外观/界面/安全设置流、认证状态、session lock 及 database failure/retry 的组合是否仍是 Shell 页面所需的屏幕投影和生命周期 owner。沿 `DatabaseSessionRecovery`、`DatabaseSessionRetryCoordinator`、`RecoverableRead` 到具体执行端检查是否有重复 retry 状态机、重复事实副本或可独立取消的子 owner；保留合法的应用 Shell 聚合，不为了减少构造器参数把状态拆进一对一转发 Coordinator。
- [ ] 对其余大/多流程候选逐项判断：`VaultViewModel` 的 Paging 查询、设置读取、OTP runtime 和条目命令；Credential Manager 的系统请求/结果交付与业务解析；Legacy/Accessibility Autofill 的授权、续接、选择和平台结果；Diagnostics 导出、清理与权限；Recovery Mode 的凭据设置；通知运行时权限的请求 owner。[Autofill 后续清单](2026-11-07-passly-autofill-followups.md)、Task 80 的统一架构审计和 Task 83 的 Detail 项应引用并更新状态，不重复复制缺陷清单；分支映射仅在同一 Presentation owner 确有重复、纯显示元数据时考虑合并。
- [ ] 沿具体 Route/宿主核对专项流程 owner：Scanner `Work.Active` 与 reducer/UI state、反馈消费确认及宿主暂停恢复/owner 替换/停止清理；Autofill Settings 的偏好与系统服务 Flow 独立失败恢复、页面退出后的旧 effect 丢弃及写入期间状态变化；Backup 文档选择 request ID 与页面 owner、退出重入/迟到回调/执行中关闭及凭据擦除/提交结果；Unlock 认证、尝试刷新和首次密码设置的 owner、取消/fatal 清理及页面反馈。相机识别、decoder、结果交付和 Recovery Code 提交恢复分别由 Scanner 专题、Autofill Task 21 与本 Task 前项维护，不复制范围。
- [ ] 对 Settings、Shell、Diagnostics、Database Reset 和 Recovery Mode 的 ViewModel 按状态真相源、合法状态、长 Flow 失败重订阅、任务取消、敏感清理及 Route effect 核对。
- [ ] 对每个 ViewModel 依赖分类并验证层边界：可观察的 Domain/Repository 数据、完整业务动作 UseCase/Interactor、窄应用端口、Presentation 状态 owner、平台/持久化实现。继续禁止 ViewModel 间依赖及直接 DAO/Room/Data Entity/具体文件实现；明确哪些 `feature.*.data` 或 `feature.*.internal` 类型实际是跨层实现泄漏，避免仅按 `internal` 包名搬移。核查 `verifyArchitecture` 当前 ViewModel 门禁与 `ViewModelDependencyVerifier` 的覆盖范围；若新增静态边界，只针对已证实错误依赖设计白名单/反例测试，不把合法 Repository Flow 或窄业务端口误禁。
- [ ] 将本轮的稳定规则同步到 `docs/platform/architecture/viewmodel-ownership.md`、`docs/platform/architecture/package-boundaries.md`、`docs/engineering/development/maintenance-boundaries.md` 及必要功能文档，并使本 TODO 保留未迁移清单与决策证据。迁移需要分批按 Feature 执行；每次必须减少原 ViewModel 的独立业务职责、状态、异步任务组或实现依赖，而不是只变短文件。覆盖取消/重试/离页迟到结果/配置重建/会话锁定/持久提交点/敏感清理；运行对应 ViewModel/Route/宿主测试、相关模块门禁、`verifyArchitecture` 和 `git diff --check`。

<a id="task-80"></a>

### Task 80：统一 Presentation 的 MVVM 与单向数据流契约

- [ ] 按真实 owner 审查持久数据、屏幕投影和用户草稿是否各有单一状态真相源；检查 Repository Flow 与手动赋值竞争、长期镜像状态及多个 owner 声称拥有同一字段，并继续核对 Vault 剩余编辑器、Trash 与导航状态的调用闭包和实现依赖。保留有明确提交/丢弃语义的草稿和 Paging 等独立状态；Route 与跨层实现 owner 的单项验收分别见 Task 32、42、83、84。
- [ ] 审查 Action、状态和 effect 的生产/消费路径，合并纯转发和重复状态；只有在带来明确状态转换保证时保留 MVI ceremony。系统 effect 的前后台有效期由 Task 51 覆盖，跨层工作流和 ViewModel 职责闭包由 Task 84 覆盖，Detail 候选由 Task 83 覆盖。
- [ ] 按 feature/owner 分批迁移状态或 effect 所有权；只在真实合同改变时同步正式文档，并以 Route/ViewModel/宿主行为测试验证。保留符合边界的页面，不为架构统一引入全局状态、万能 reducer 或转发层。当前 Detail/完整表单编辑器改动稳定后再作为基线；每批运行受影响门禁及 `git diff --check`。

正式分层与状态规则见[状态管理与 ViewModel](../../../engineering/development/architecture-and-state.md)和[ViewModel owner 边界](../../../platform/architecture/viewmodel-ownership.md)。Detail/List/完整编辑的具体 Route 与工作流验收分别归 Task 83、32、42；本 Task 只跟踪状态真相源、Action 与 effect 模式的剩余逐页核对。

<a id="task-35"></a>

### Task 35：验收 EntryLink 写入与端点删除竞态

- [ ] 以真实 Room AndroidTest 交错验证端点删除与 `EntryLinkRepository.upsert()`：允许 upsert 先提交后由删除级联清除，或删除先提交后 upsert 拒绝；最终不得留下指向缺失或已删除端点的 link。现有 `EntryLinkConstraintDaoTest.endpointTripleIsUnique_andDeletingEndpointCascadesLink` 已覆盖 DAO 唯一约束、同 ID 更新和级联删除，本项只补 Repository 事务竞态，不重复 DAO 断言。

<a id="task-34"></a>

### Task 34：验收 Activity 查询与修订恢复的 Room 行为
- [ ] 在设备上运行 `RoomObservationSessionTest` 与 `EntryUpdateAndRevisionRestoreTransactionTest`，记录设备/API 和 SQLCipher 行为。此前的安装失败证据见[测试逻辑清理审查记录](../../reviews/2026-11-11-test-logic-maintenance-review.md#task-34-36-device-install-attempt)。

实现调用闭包、Activity DAO 收敛及历史构建验证见[测试逻辑清理审查记录](../../reviews/2026-11-11-test-logic-maintenance-review.md#activity-与-revision-dao-收敛证据)；本项只跟踪尚未运行的 Room/修订恢复设备验收。

<a id="task-36"></a>

### Task 36：验收修订 DAO 生命周期与重建行为
- [ ] 在设备上运行 Revision DAO 专属 instrumentation：`EntryRevisionLifecycleDaoTest` 与 `EntryRevisionHistoryRebuilderTest`，记录设备/API 和结果。`RoomObservationSessionTest` 与 `EntryUpdateAndRevisionRestoreTransactionTest` 由 Task 34 跟踪；此前的安装失败证据见[测试逻辑清理审查记录](../../reviews/2026-11-11-test-logic-maintenance-review.md#task-34-36-device-install-attempt)。

Revision DAO 调用闭包、已完成代码收敛与初始化编排 JVM 验证见[测试逻辑清理审查记录](../../reviews/2026-11-11-test-logic-maintenance-review.md#activity-与-revision-dao-收敛证据)；本项只跟踪尚未运行的 Data instrumentation 设备验收。

<a id="task-74"></a>

### Task 74：按测试对象优化单测结构与重复装配
- [ ] Detail 生产 owner 稳定后再评估是否拆分 `DetailViewModelTest.kt`；本轮检查了 favicon/敏感字段、编辑准备、package picker、activity/revision history、snapshot 与 revision restore 工作流。相似的失败/重试、取消、fatal、session lock 与 entry replacement 保护不同 owner 和终态，未发现可安全删除的重复场景。由于当前 Detail owner 仍在变更，先不搬动宽 fixture；稳定后按独立工作流和生命周期职责决定，不以行数拆分。判断记录见[测试逻辑清理审查记录](../../reviews/2026-11-11-test-logic-maintenance-review.md#detailviewmodeltest-拆分评估)。
- [ ] 继续全仓 `src/test` 与 `src/androidTest` 逻辑审查，逐项追踪生产 owner、断言输入/状态/副作用及失败/取消清理。已审查模块、各测试保留/删除理由和验证记录见[测试逻辑清理审查记录](../../reviews/2026-11-11-test-logic-maintenance-review.md)；全仓审查仍未完成。
- [ ] 当前 App 生产编译恢复后，定向编译并运行 `VaultListUiMapperTest`、`OtpUiStateMapperTest`、`BackupArchiveServiceImplTest` 和 `DefaultBackupOperationGatewayTest`，验证最近 OTP 映射测试迁移及相关测试源集。此前编译失败与静态复核结论见[审查记录](../../reviews/2026-11-11-test-logic-maintenance-review.md)；源代码检查不能替代这项执行验证。

<a id="task-75"></a>

### Task 75：统一固定常量的文件位置与 owner，收敛重复合同值

- [ ] 继续按语义 owner 审查 companion 常量；仅将确属文件级的实现值移至文件顶部，类型/协议专属值归入准确 owner，不为位置变化改动算法或 wire 值。
- [ ] 验证新增/迁移后的可见范围、所有调用点和单一真相源；持久化 key、wire tag、格式版本不得改变，安全限制不得被放宽。补充/迁移对应常量与协议测试；运行受影响模块单测、`verifyArchitecture`、lint 和 `git diff --check`。同步更新 Kotlin 规范中的位置规则；不要建立无 owner 的全局 `Constants.kt`，也不要把资源文本、Gradle 构建值或运行时可调配置复制成 Kotlin 常量。

**验证缺口：** 相关定向单测、受影响模块编译和 `verifyArchitecture` 已分批通过；Favicon Store Android instrumentation 因无连接设备未执行。Core/Data/Security lint 已通过，App lint 在 `:app:lintAnalyzeDebug` 遇到 IntelliJ `AsyncExecutionService.getService must not return null`，没有有效结果，须修复工具环境后重跑。故本 Task 的完整验证仍开放。

<a id="task-77"></a>

### Task 77：核实诊断日志退出时的持久性与周期写入策略

- [ ] 稳定 flush 合同后调查离开/后台/移除 task/force-stop/强杀的日志损失窗口，记录是否等待 flush、最后持久批次及设备/API；任意终止不保证回调。
- [ ] 先完成[诊断 Tasks 1、2](2026-11-06-passly-diagnostics-reliability-followup.md)，再裁定周期/退出策略，不因独立模块或进程就宣称 durable。

**2026-11-07：deferred，设备持久性调查。** store 已有容量 256 单线程有界队列、50ms/16 条微批，每批 flush + fd.sync；onStop 仅异步排队 flush，不保证完成。移除旧“尚未批量写入/落盘”前提。

## 已实现机制：保留定向验收


<a id="task-22"></a>

### Task 22：验收通知权限重建与启动失败交接

- [ ] 在设备上验收 `AppCompatDelegate.setApplicationLocales()` 引发的 Activity 重建与系统授权/拒绝弹窗组合；确认新 Route/host 接管有效请求、旧 host/session 不产生重复提交或过期反馈，且 PermissionRequestHost arbiter lease 最终释放。
- [ ] 补 `NotificationsRoute` 与 `PermissionRequestHost` 联动行为覆盖：`Busy` / `HostUnavailable` 提示、未启动后的新 token 重试及 pending 清理。当前 ViewModel owner 与通用 session/lease 测试证据见[通知权限审查记录](../../reviews/2026-11-11-notification-permission-not-started.md)；这些测试不代替 Route/host 联动验收。

配置重建、token 与页面 owner 的当前实现见[通知权限审查记录](../../reviews/2026-11-11-notification-permission-not-started.md#配置重建与页面-owner)和[通知设置专题](../../../product/settings/notification-settings.md)；此 Task 只保留 locale/系统弹窗设备组合及 Busy/HostUnavailable Route/host 行为验收。

<a id="task-27"></a>

### Task 27：验收 OTP Runtime 配置失效的 Route 与 owner 交接
- [ ] 补充真实 Compose Route/NavHost/宿主回归：验证保存后返回 Detail 的显示与复制读取新 OTP 配置，取消/失败不触发成功刷新，以及列表/Detail owner 变化、锁定、离页和恢复时的 lease 与迟到结果。`AddOtpViewModelTest.fullEditLoadsOtpAndWritesUpdatedConfiguration` 于 2026-11-11 定向执行通过（1/1），证明 ViewModel 写入后通知活动 Runtime 重新加载；它不证明真实导航交互。实现和既有 owner 验收边界见[OTP Runtime owner 与失效审查](../../reviews/2026-11-11-otp-runtime-owner-and-invalidation.md)。

<a id="task-29"></a>

### Task 29：验收 App Password 草稿的真实宿主生命周期清理

- [ ] 在设备或 AVD 上运行 `AppPasswordDialogCoordinatorLifecycleTest` 并记录 API/结果；再通过真实 Activity 配置重建验证草稿按认证合同清空。当前 owner 合同、Compose 测试覆盖及其与 Activity recreation 的边界见[App Password 对话框生命周期审查](../../reviews/2026-11-11-app-password-dialog-lifecycle.md)。

<a id="task-31"></a>

### Task 31：验收 Room 观察在会话锁切换下的取消与重建

- [ ] 在真实 SQLCipher/设备执行已有 RoomObservationSessionTest 的 soft-lock、seal/reopen、DAO emission race、旧 lease 释放用例；验证普通错误可见、撤权清敏感状态、快速恢复不读旧 handle。

当前观察恢复实现、异常边界和 JVM/Room 测试证据见[测试逻辑清理审查记录](../../reviews/2026-11-11-test-logic-maintenance-review.md)与[数据库会话合同](../../../platform/data/database.md#session-生命周期)；本项只跟踪尚未完成的 SQLCipher/设备交错验收。

<a id="task-44"></a>

### Task 44：验收迟到 AuthenticationHost 密码回调的清理与隔离

- [ ] 为真实认证宿主补取消/替换/重建后的迟到密码 callback 测试，保留实际 CharArray 引用，验证错误 request、stale identity、CAS 失配和未接管路径擦除，且不提交给新请求。

迟到输入的当前清理合同、准确函数及 helper 测试见[认证专题](../../../platform/security/authentication.md)；本 Task 只保留真实认证 Host 取消、替换和重建交错下的迟到 callback 验收。

<a id="task-45"></a>

### Task 45：验收认证与生物识别轮换的 Host 并发所有权

- [ ] 以真实 Host/Provisioner 验收共享 UI lease 的生命周期交错：普通认证与生物识别轮换争用、等待取消、Host pause/destroy、旧 callback 与新 owner 隔离。journal 收尾和轮换提交结果由 Task 59 跟踪；本 Task 不重复这些事务验收。

共享 lease、单飞请求、request identity 及现有 helper 测试见[认证专题](../../../platform/security/authentication.md)；本项只跟踪真实 Host/Provisioner 交错，helper 单测不能代替 Activity 生命周期验收。


<a id="task-64"></a>

### Task 64：验收附件 GC 重试与提交后 owner 恢复

- [ ] 在真实 SQLCipher/文件系统执行 AttachmentResourceGarbageCollectorTest 多批排空、失败文件重试、worker 重建唤醒，验证撤权、当前/历史引用保护及最终回收。
- [ ] 沿 Vault `EditorWriteSession`、Detail revision owner 与 Autofill/Inbox 调用链验证 durable commit 后取消的结果映射、离页/撤权恢复与重试，特别确认普通创建不会重复生成 EntryId；当前只有静态链路证据，尚未验证 owner 行为。

附件 GC 与提交后清理的当前合同、实现函数及源码测试覆盖见[数据库专题](../../../platform/data/database.md#历史与附件生命周期)和[审查记录](../../reviews/2026-11-11-attachment-gc-postcommit-review.md)；本 Task 仅保留设备运行与 owner 级结果验收。
