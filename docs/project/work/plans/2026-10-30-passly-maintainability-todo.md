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

### Task 37：为认证撤权与数据库操作定义同一线性化边界
- [ ] 增加可控挂起的跨层终态回归：在 Security lock 等待数据库 lease/resource 与敏感 key 清理时，从 runner、直接 Full Vault Repository 与 Autofill Repository 发起读写，验证线性化点之后新命令不触达 DAO；已进入事务准确报告提交/回滚/取消结果，敏感 permit、会话、DEK、key 与 lock state 一致。边界与真实 Repository 集成测试证明调用闭包后，不需为每个薄适配器复制同一测试。
- [ ] 在可用设备上验收真实 Data lease/Room DAO 与 Security reset/lock 交错终态；当前 `adb devices` 无连接设备，instrumentation runtime 尚未执行。恢复模式、Autofill session 结束和 idle timeout 的撤权次序仍需按本项统一验收。

Session 撤权顺序、数据库准入与 Room observation 的当前合同，以及已完成迁移的历史验证边界，见[数据库撤权线性化审查](../../reviews/2026-11-11-database-revocation-linearization.md)；权威运行规则见[数据库专题](../../../platform/data/database.md)和[Session/DEK 审查](../../reviews/2026-11-07-session-close-and-dek-wipe.md)。本 Task 仅保留跨层 owner 终态和设备交错验收。
<a id="task-59"></a>

### Task 59：闭合生物识别轮换的提交后恢复与启动对账

- [ ] 增加 `RotateBiometricKeyUseCase` + `DefaultAuthenticationMethodProvisioner` + 持久 Store 的 owner 级回归：binding 已提交后 candidate cleanup 或 availability refresh 普通失败必须保持成功结果并带准确的 pending maintenance，重试不能盲目重复轮换。
- [ ] 用真实 Authentication Host/Provisioner/启动 owner 覆盖轮换 commit 前后异常、取消与 fatal、alias/journal 恢复和 host lease 释放。`BiometricRotationCandidateTest` 与 `BiometricRotationReconcilerTest` 已覆盖 helper/reconciler 级边界，不重复这些单元场景；manager 同快照与并发刷新由 Task 46 跟踪。
- [ ] 在 `AppPasswordSettingsInteractor` / `AppPasswordSettingsViewModel` owner 覆盖提交前后取消：确认请求缓冲清理、`isEnabled` Flow 与页面结果一致、不会显示错误的成功/失败或允许重复提交。`AppPasswordProvisioningCommitTest` 已验证 envelope 持久化后的取消传播，但不覆盖设置页状态投影。

**底层实现和测试证据：** 已归档于[认证提交后维护审查](../../reviews/2026-11-11-authentication-post-commit-recovery.md)，权威合同见[认证机制与会话](../../../platform/security/authentication.md)。剩余项仅要求跨 provisioner、持久 Store、ViewModel 与真实 Host 的 owner 联动验证。


<a id="task-42"></a>

### Task 42：让共享条目编辑能力由条目编辑层持有
- [ ] 补齐并执行 Compose Route/NavHost 或设备宿主验收：Detail 预加载失败 Toast 的可见性、重复点击重试、STOPPED 时取消及条目/认证 owner 替换后丢弃迟到反馈；完整编辑导航失败后的提示与 seed 清理；编辑器无效/已消费 seed 的返回后重试；配置重建与迟到 callback。已有 JVM 行为、Session/Coordinator 测试不证明真实宿主交互。已完成实现合同及历史验收见[完整表单编辑边界审查](../../reviews/2026-11-11-full-form-editing-boundaries.md)。


<a id="task-40"></a>

### Task 40：让备份目录设置命令失败可见并保留授权状态
- [ ] 在连接设备后执行 `BackupDirectoryClearDialogTest`，记录设备/API 与结果。此前 ViewModel/Interactor 单测、AndroidTest Kotlin 编译、`verifyArchitecture` 和 `git diff --check` 的实现合同与验收结果已迁入[目录设置失败处理审查](../../reviews/2026-11-08-backup-directory-settings-failure-feedback.md)；设备运行时验收仍未完成。

<a id="task-32"></a>

### Task 32：明确 Vault 列表 ViewModel 的工作流边界并补宿主行为覆盖
- [ ] 补齐授权撤销期间操作及其余 page owner 交接/迟到结果行为；纯 `VaultReducer`/`VaultQueryState` 单测仍不能代替 ViewModel 的协程和 `PageEffects` 所有权合同。
- [ ] 让 Route 消费与各 owner 对齐：`VaultRoute` 负责 Compose/导航/system effect，Content 只接收 immutable state/actions；确保图级 `VaultViewModel` 在 Trash/Detail 转场或目的地保留期间不会重复创建 effect collector 或重复订阅 Paging/OTP。给选定的 owners 增加 Route/NavHost 行为回归后同步状态管理文档，运行 Vault Presentation 定向测试、`verifyArchitecture` 和 `git diff --check`。

当前列表状态、命令和图级 OTP lease 合同见[列表与搜索专题](../../../product/vault/list-and-search.md#代码位置)；实现与历史验证证据见[Vault 列表 owner 审查记录](../../reviews/2026-11-11-vault-list-owner-followup.md)。本 Task 只保留撤权/owner 交接与真实 Route/NavHost 转场验收。


## P2：恢复与交付合同

<a id="task-46"></a>

### Task 46：让认证方式 StateFlow 发布同一持久快照的最新解析结果

- [ ] 补真实 manager/store/provisioner 集成覆盖：同一持久快照解析、并发刷新、提交后刷新、调用方取消及恢复事件；同时为启动首次刷新失败提供可观察状态和重试投影。状态对象单测不代替 manager 合同。认证设置变更后的提交后刷新失败已有 Domain pending 状态与页面提示。

认证方式状态协调、manager 重试合同和状态 owner 测试见[认证专题](../../../platform/security/authentication.md#host-与-生物识别)；本 Task 只保留 manager/store/provisioner 集成及启动失败反馈验收。

<a id="task-51"></a>

### Task 51：明确非 Scanner Route 一次性 effect 的前后台交付合同

- [ ] 逐项定义 Toast/Snackbar、导航、SAF picker、权限和平台交付在 STOPPED、恢复、离页、owner 替换后的有效期；短命反馈明确是否丢弃，待完成请求明确如何恢复。
- [ ] 补 PageEffects 及真实 Route/宿主测试，覆盖同页 STOPPED→STARTED、转场、请求代际、锁定/关闭及 callback 异常；不把不同 effect 塞入统一 handler 表。

共享 `PageEffects` 的 STARTED 收集、页面 owner 隔离及缓冲恢复合同见[状态管理专题](../../../engineering/development/architecture-and-state.md#页面一次性事件的可见性与-owner)。本 Task 只跟踪不同 effect 的有效期决策和 Route/宿主覆盖。


## P3：按 owner 分批整理

<a id="task-73"></a>

### Task 73：把官方 Android 检查纳入维护复核并处理已发现的质量项

- [ ] 按本轮 app/Autofill Lint 新报告裁定剩余项，不沿用旧数量；清理 EmptySuperCall、冗余 API、UseKtx、资源时保留清理/持久时序及中英文合同。
- [ ] 依赖升级独立验证：catalog 当前 palette 1.1.0、Material3 1.5.0-beta02、DataStore 1.3.0、SQLCipher 4.20.0 已更新；旧“14 条升级提示”不再整体列为未实施。
- [ ] APK Analyzer 保存交付/release APK 的 DEX、资源、native、assets 基线；历史 debug 体积不是 release 优化收益，设备/性能验收按功能 owner 执行。

**2026-11-07 更正：** DetailViewModelTest 旧 CREATE/Lifecycle 夹具已改 AUTOFILL，实际 retry 并验证订阅两次及 Ready，移出待修。ScannerResultCard 的 Modifier 位置已调整，Lint 任务已在 entry 拆分后成功退出；当前测试失败已收敛并清零，不沿用 2026-11-02 统计。

<a id="task-83"></a>

### Task 83：复核 Repository 覆盖并把业务工作流移出大 ViewModel

- [ ] 继续审查 `DetailViewModel` 中的其余工作流，只有状态 owner、任务组和依赖闭包能一起迁走并实际减少 ViewModel 职责时才拆分。验证修订历史 metadata Flow 后续失败/重订阅、会话锁定、条目或页面 owner 替换后的迟到结果及重复初始化幂等性；再核对其余 Detail owner。已完成迁移及其当前合同与验收证据见[修订历史审查记录](../../reviews/2026-11-08-vault-presentation-boundaries.md)与[修订历史专题](../../../product/vault/entry-revision-history.md)。
- [ ] 评估 Detail favicon 编辑是否应从 `DetailViewModel` 提取为独立工作流 owner：当前 VM 同时发起 URI/URL staging、裁剪后图片处理、staged image promote、取消/过期结果回收及 `EntryEdit.SetIcon` 持久化；`DetailFaviconSession` 已拥有准备/保存 Job 与资源释放的一部分合同。按职责将图像处理/条目命令放在合适 UseCase，将裁剪输入、临时路径和页面生命周期放在 Feature 工作流 owner；不得把 Android/UI 会话状态塞进通用 UseCase，也不得拆出不能减少 ViewModel 依赖、分支或异步 owner 的薄包装。必须保持过期会话不回写、promote 后失败回收、取消/关闭时敏感与文件资源清理及提交结果一致性。
- [ ] 同一轮核对 Detail 的其余候选及最大 ViewModel：保留 `ActivityQueryRepository` 等简单只读 Flow 的页面订阅在 VM（包括异常态、重试 owner），已有 `EditEntryUseCase`、revision compare/reveal/restore、copy、QR 等完整动作不要再包一层；对 Backup、Credential Response、Diagnostics、Unlock 等大文件检查是否有跨端口业务决策或事务流程仍留在 VM，再依据证据决定提取或保留。只将有业务意图/不变量、复用价值、跨端口编排或独立取消/重试合同的流程归入专属 UseCase/工作流；UI 草稿、屏幕投影、Action 分发和生命周期任务仍由对应 Presentation owner 管理，防止造出上帝 UseCase。
- [ ] 对每个实际拆分补充 owner 级行为测试：验证旧 ViewModel 状态与依赖确有减少，UseCase 输入/结果不泄漏敏感值，并覆盖取消、失败重试、session/entry 替换后的迟到结果和资源收尾。同步必要的主题文档与本 TODO 证据；运行受影响测试、`verifyArchitecture` 与 `git diff --check`。本任务是待执行的维护审计与迁移方向，不表示所有大 ViewModel 都需要拆分。

**2026-11-08 Vault 边界审查续记：** Vault 页面审查与调用闭包已归档；详情工作流 owner 的保留决策和验收证据见[审查记录](../../reviews/2026-11-08-vault-presentation-boundaries.md)。Task 83 的跨 Feature 维护审计仍开放。

<a id="task-84"></a>

### Task 84：全量审计 ViewModel 与 Coordinator 的职责归属并整理分层规范

- [ ] 对当前 `:app`、`:security`、`:feature:autofill` 中生产 ViewModel 逐个完成调用闭包审计，输出保留/迁移/合并/继续调查及其证据，不按行数或类名自动判断。清点范围：Shell/宿主（`AppShellViewModel`、`AppThemeSettingsViewModel`、`AppNoticeHostViewModel` 属于 App，`AuthenticationHostOwnerViewModel` 属于 Security）；Vault（`VaultViewModel`、`TrashViewModel`、`DetailViewModel`、`ScannedOtpImportViewModel`、四种 `Add*ViewModel`）；Settings/Backup（`AutofillSettingsViewModel`、`SecuritySettingsViewModel`、`RecoveryCodeSettingsViewModel`、`PrivacySettingsViewModel`、`AppPasswordSettingsViewModel`、`InterfaceSettingsViewModel`、`AppearanceSettingsViewModel`、`PermissionManagementViewModel`、`InteractionSettingsViewModel`、`NotificationSettingsViewModel`、`DiagnosticsViewModel`、`DataManagementSettingsViewModel`、`BackupViewModel`；`StorageSettingsViewModel` 已审查并保留，证据见[边界审查](../../reviews/2026-11-11-storage-settings-viewmodel-boundary.md)）；Autofill 工作流（`AutofillFillViewModel`、`CredentialResponseViewModel`、`AccessibilityFillViewModel`，均属于 `:feature:autofill`）；Scanner/Unlock/Recovery/Database（`ScannerViewModel`、`UnlockViewModel`、`RecoveryModeViewModel`、`DatabaseResetViewModel`）。同时覆盖每页 Route/Activity/Content 的实际消费者、DI 绑定、相关测试及下面被调用的 UseCase/Interactor/Repository/Coordinator/Store，而非只看 ViewModel 文件。
- [ ] 审查 Recovery Code 提交恢复的剩余 owner 边界：`RecoveryCodeDraftInteractor.commit()` / `reconcileCommit()` 已拥有草稿提交和 durable marker 核对；`RecoveryCodeSettingsViewModel.confirmAndEnable()` 仍在取消/fatal 路径通过 `reconcileInterruptedCommit()` 执行 `NonCancellable` 核对，并将结果投影到页面状态。判断这段提交结果协调是否应移入应用操作 owner，或保留为页面 owner 的 UI 收尾；保留 commit 前失败、持久化后普通异常/取消/fatal `Error`、离页和 disclosure 擦除合同。单测源码已覆盖关键路径，真实 Route/Activity 行为仍需单独验收。当前合同见[认证专题](../../../platform/security/authentication.md#恢复码草稿)，代码核对见[恢复码 owner 审查](../../reviews/2026-11-11-recovery-code-draft-owner.md)。
- [ ] 审查 `AppShellViewModel` 对外观/界面/安全设置流、认证状态、session lock 及 database failure/retry 的组合是否仍是 Shell 页面所需的屏幕投影和生命周期 owner。沿 `DatabaseSessionRecovery`、`DatabaseSessionRetryCoordinator`、`RecoverableRead` 到具体执行端检查是否有重复 retry 状态机、重复事实副本或可独立取消的子 owner；保留合法的应用 Shell 聚合，不为了减少构造器参数把状态拆进一对一转发 Coordinator。
- [ ] 对其余大/多流程候选逐项判断：`VaultViewModel` 的 Paging 查询、设置读取、OTP runtime 和条目命令；Backup 文档选择与备份操作；Credential Manager 的系统请求/结果交付与业务解析；Legacy/Accessibility Autofill 的授权、续接、选择和平台结果；Diagnostics 导出、清理与权限；Unlock 的认证和 App Password provision；Scanner 的相机/图片输入与解码；Recovery Mode 的凭据设置；通知运行时权限的请求 owner。[Autofill 后续清单](2026-11-07-passly-autofill-followups.md)、Task 80 的统一架构审计和 Task 83 的 Detail 项应引用并更新状态，不重复复制缺陷清单；分支映射仅在同一 Presentation owner 确有重复、纯显示元数据时考虑合并。
- [ ] 审查分散在层外/文件外的 Coordinator、Session、Store 和 helper：至少覆盖 `DatabaseSessionRetryCoordinator`、`SecureDatabaseResetCoordinator`、`EntryEditorSeedCoordinator`、`AppPasswordDialogCoordinator`、`PermissionRequestSession`、`AccessibilityFillSessionStore`、`DetailWorkSession`、`DetailQrSession`、`DetailFaviconSession`、`DetailRevealStore`、`DetailRevisionRevealStore`、`EditorWriteSession`、`ScannerCameraHost`、`ScannerResultSession`、`RecoverableRead` 与 `PageEffects`。为每项记录真实创建者、生命周期、共享者、状态真相源及所处 Gradle module；合并重复机制或移除纯桥接前，证明其职责未被配置重建、Route、系统 callback、认证/锁定 owner 或其他调用方需要。已确认四种编辑器共用 `EditorWriteSession`，七处 Settings `saveSetting` 共用 `writePreferenceWithFeedback`：审计为保留/扩展现有共享逻辑，不复制到新 Coordinator；`AppShell` 等用例须按调用图核实。
- [ ] 对每个 ViewModel 依赖分类并验证层边界：可观察的 Domain/Repository 数据、完整业务动作 UseCase/Interactor、窄应用端口、Presentation 状态 owner、平台/持久化实现。继续禁止 ViewModel 间依赖及直接 DAO/Room/Data Entity/具体文件实现；明确哪些 `feature.*.data` 或 `feature.*.internal` 类型实际是跨层实现泄漏，避免仅按 `internal` 包名搬移。核查 `verifyArchitecture` 当前 ViewModel 门禁与 `ViewModelDependencyVerifier` 的覆盖范围；若新增静态边界，只针对已证实错误依赖设计白名单/反例测试，不把合法 Repository Flow 或窄业务端口误禁。
- [ ] 将本轮的稳定规则同步到 `docs/platform/architecture/viewmodel-ownership.md`、`docs/platform/architecture/package-boundaries.md`、`docs/engineering/development/maintenance-boundaries.md` 及必要功能文档，并使本 TODO 保留未迁移清单与决策证据。迁移需要分批按 Feature 执行；每次必须减少原 ViewModel 的独立业务职责、状态、异步任务组或实现依赖，而不是只变短文件。覆盖取消/重试/离页迟到结果/配置重建/会话锁定/持久提交点/敏感清理；运行对应 ViewModel/Route/宿主测试、相关模块门禁、`verifyArchitecture` 和 `git diff --check`。

**全量清点证据（2026-11-06，工作区快照）：** 按生产 `src/main` 清点到 34 个 ViewModel；[`ViewModelDependencyVerifier.kt`](../../../../buildSrc/src/main/kotlin/passly/ViewModelDependencyVerifier.kt) 是架构检查器，不计作生产 ViewModel。文件长度范围为 8–1208 行；仅行数用于筛查，不作为拆分依据。代码摘要覆盖了每个 ViewModel 的构造依赖、公开/私有方法、`launch`/`collect` 与分支密度；并对 Shell、Storage Settings、Recovery Code、Unlock、Detail 及共享 Editor/Settings helper 做了方法级复核。初步确认 Recovery Code ViewModel 仍直接执行 durable commit 异常/取消/fatal 恢复协调；Storage Settings 同时操作 cache、diagnostics log 与记录模式；Shell 聚合设置、session 和 database retry 流；这些是优先复核项，是否迁层按实际 owner 合同完成闭包审计后决定。多个简单 Settings ViewModel、只投影配置的 Theme/Vault Display ViewModel、扫描导入 ViewModel 使用窄数据源/UseCase；四类编辑器已共享 `EditorWriteSession`，七个设置保存入口共用 `writePreferenceWithFeedback`，这些共享职责暂列保留核实，不提出机械新增层。当前 `viewmodel-ownership.md` 规定不得 ViewModel 相互依赖及不得直接依赖 DAO/Room/文件实现，`verifyArchitecture` 的 ViewModel 检查定位于跨 ViewModel 依赖；层间实现依赖和具体工作流归属仍需本任务逐页核对。此为全量生产文件清点与候选筛查，尚未逐项完成所有 Route/DI/测试的调用闭包验证，也未运行构建或测试。

**2026-11-07 Vault Paging 旧查询路径清理：** 生产只使用 `buildRecoverableVaultQueryStates()` 与 `switchRecoverableQueryGenerations()`；移除未被调用的 `buildVaultQueryStates()`、`switchQueryGenerations()`，将搜索/切换用例迁到实际生产 helper。红灯回归发现层级模式只对“全部”查询有效，但旧逻辑先按 `VaultQueryState` 去重，导致密码筛选下重启同一个 `EntryListQuery` 对应的 Pager；现改为先映射查询、再按 `EntryListQuery` 去重。定向红灯复现后 `VaultQueryStateTest` 8/8 通过；Task 84 的 VaultViewModel 行为审计仍待完成。

<a id="task-80"></a>

### Task 80：统一 Presentation 的 MVVM 与单向数据流契约

- [ ] 按真实 owner 审查持久数据、屏幕投影和用户草稿是否各有单一状态真相源；检查 Repository Flow 与手动赋值竞争、长期镜像状态及多个 owner 声称拥有同一字段。保留有明确提交/丢弃语义的草稿和 Paging 等独立状态。
- [ ] 审查 Action、状态和 effect 的生产/消费路径，合并纯转发和重复状态；只有在带来明确状态转换保证时保留 MVI ceremony。系统 effect 的前后台有效期由 Task 51 覆盖，跨层工作流和 ViewModel 职责闭包由 Task 84 覆盖，Detail 候选由 Task 83 覆盖。
- [ ] 按 feature/owner 分批迁移状态或 effect 所有权；只在真实合同改变时同步正式文档，并以 Route/ViewModel/宿主行为测试验证。保留符合边界的页面，不为架构统一引入全局状态、万能 reducer 或转发层。当前 Detail/完整表单编辑器改动稳定后再作为基线；每批运行受影响门禁及 `git diff --check`。

**2026-11-07：retained，仅剩逐页审计/迁移。** architecture-and-state.md 已明确分层 MVVM + UDF、Repository 真相源、可选 Reducer 和 UseCase owner；viewmodel-ownership.md 已有对应边界。移除“正式规范仍分四套架构”的过期事实。Task 84 持有 ViewModel/Coordinator 调用闭包清单，Task 83 持有具体 Detail 候选，本 Task 只管状态生产、真相源及 action/effect 模式，不复制 owner 清单。

**2026-11-10 Vault 状态清理进度：** Detail 标签编辑器删除 `DetailTagEditorUiModel.Hidden/Visible` 镜像和 `DetailTagValidationState`，Reducer 直接维护被 Route/Composable 消费的 `DetailTagEditorState`；Domain 校验错误只在 Presentation 转成复数资源展示状态，标签 chips 空/非空投影共用一个 owner。列表 OTP 行也已用非空 `VaultOtpKindUiModel` 替代 `hasOtp + nullable kind`，OTP 各等待/隐藏/失败状态非空表达。OTP 表单将 HOTP counter 字段和 STEAM 参数禁用能力投影为 `OtpFormState` 属性，Composable 不再重复判断类型。OTP 编辑器移除生产调用从未启用的 `reportFailure` 分支、失败 Toast/Snackbar 资源和单分支 `AddOtpEvent` wrapper，统一为受页面/认证 owner 校验的 `uriAppliedFeedback: Flow<Unit>`；页面状态改名 `AddOtpEditorUiState`。生产代码、单测源码、AndroidTest 源码编译及 `verifyArchitecture` 通过；测试未运行。整体 Vault 审计仍开放，后续继续检查其他编辑器、Trash 与导航状态的调用闭包、实现依赖和过时路径。

<a id="task-35"></a>

### Task 35：验收 EntryLink 写入与端点删除竞态

- [ ] 以真实 Room AndroidTest 交错验证端点删除与 `EntryLinkRepository.upsert()`：允许 upsert 先提交后由删除级联清除，或删除先提交后 upsert 拒绝；最终不得留下指向缺失或已删除端点的 link。覆盖既有 link ID 更新与 endpoint 唯一约束路径。

<a id="task-34"></a>

### Task 34：收窄未消费的 Activity 查询流
- [ ] 在可用设备上运行 `RoomObservationSessionTest` 与 `EntryUpdateAndRevisionRestoreTransactionTest`；当前未连接 Android 设备，instrumentation runtime 尚未验证。

实现调用闭包、Activity DAO 收敛及历史构建验证见[测试逻辑清理审查记录](../../reviews/2026-11-11-test-logic-maintenance-review.md#activity-与-revision-dao-收敛证据)；本项只跟踪尚未运行的 Room/修订恢复设备验收。

<a id="task-36"></a>

### Task 36：按生产消费闭包收敛修订 Room DAO
- [ ] 在可用设备上运行受影响的 Data instrumentation tests；当前无连接设备，运行时数据库行为尚未验证。

Revision DAO 调用闭包、已完成代码收敛与初始化编排 JVM 验证见[测试逻辑清理审查记录](../../reviews/2026-11-11-test-logic-maintenance-review.md#activity-与-revision-dao-收敛证据)；本项只跟踪尚未运行的 Data instrumentation 设备验收。

<a id="task-74"></a>

### Task 74：按测试对象优化单测结构与重复装配
- [ ] Detail 生产 owner 稳定后再评估是否拆分 `DetailViewModelTest.kt`；按独立工作流和生命周期职责决定，不以行数拆分，避免搬动仍在变化的 fixture 合同。
- [ ] 继续全仓 `src/test`、`src/androidTest`、`src/testFixtures` 的逻辑审查；逐项追生产 owner、输入/状态/副作用断言及失败/取消清理，再决定合并或删除。不同模块、平台生命周期、稳定协议键和安全策略保持各自 owner。已完成批次与审查边界见[测试逻辑清理审查记录](../../reviews/2026-11-11-test-logic-maintenance-review.md)，不在本计划维护扫描计数。

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
- [ ] 为通知权限 `Busy` / `HostUnavailable` 分支增加 Route/host 行为覆盖，验证提示、重试时机及 arbiter lease 收尾；ViewModel 的未启动与新 token 重试行为已有回归覆盖，结果见[未启动通知权限请求审查](../../reviews/2026-11-11-notification-permission-not-started.md)。

配置重建、token 与页面 owner 的当前实现见[通知权限审查记录](../../reviews/2026-11-11-notification-permission-not-started.md#配置重建与页面-owner)和[通知设置专题](../../../product/settings/notification-settings.md)；此 Task 只保留 locale/系统弹窗设备组合及 Busy/HostUnavailable Route/host 行为验收。

<a id="task-27"></a>

### Task 27：闭合 OTP Runtime 缓存失效与刷新故障恢复合同
- [ ] 补充真实 Compose Route/NavHost/宿主回归：验证保存后返回 Detail 的显示与复制读取新 OTP 配置，取消/失败不触发成功刷新，以及列表/Detail owner 变化、锁定、离页和恢复时的 lease 与迟到结果。`AddOtpViewModelTest.fullEditLoadsOtpAndWritesUpdatedConfiguration` 源码覆盖编辑成功后使活动 Runtime 重载配置，但未由本轮执行；它不证明真实导航交互。实现和既有 owner 验收边界见[OTP Runtime owner 与失效审查](../../reviews/2026-11-11-otp-runtime-owner-and-invalidation.md)。

<a id="task-29"></a>

### Task 29：在 Security 页面退出时清除 App Password 对话框草稿

- [ ] 在连接设备或 AVD 后运行 `AppPasswordDialogCoordinatorLifecycleTest`，记录 API 与结果；再以真实 Activity 配置重建验证草稿按认证文档规定清空。现有 Coordinator/ViewModel 单测覆盖迟到输入、授权和操作结果 owner；Compose 仪器测试覆盖 Route 从组合中移除后清空旧 Coordinator、重新组合后建立空状态，但尚未设备执行，也不模拟 Activity recreation。实现合同和覆盖边界见[App Password 对话框生命周期审查](../../reviews/2026-11-11-app-password-dialog-lifecycle.md)。

<a id="task-31"></a>

### Task 31：让 Room 长期观察在会话锁切换时可取消并可重建

- [ ] 在真实 SQLCipher/设备执行已有 RoomObservationSessionTest 的 soft-lock、seal/reopen、DAO emission race、旧 lease 释放用例；验证普通错误可见、撤权清敏感状态、快速恢复不读旧 handle。

当前观察恢复实现、异常边界和 JVM/Room 测试证据见[测试逻辑清理审查记录](../../reviews/2026-11-11-test-logic-maintenance-review.md)与[数据库会话合同](../../../platform/data/database.md#session-生命周期)；本项只跟踪尚未完成的 SQLCipher/设备交错验收。

<a id="task-44"></a>

### Task 44：迟到的 AuthenticationHost 密码提交必须擦除输入缓冲

- [ ] 为真实认证宿主补取消/替换/重建后的迟到密码 callback 测试，保留实际 CharArray 引用，验证错误 request、stale identity、CAS 失配和未接管路径擦除，且不提交给新请求。

迟到输入的当前清理合同、准确函数及 helper 测试见[认证专题](../../../platform/security/authentication.md)；本 Task 只保留真实认证 Host 取消、替换和重建交错下的迟到 callback 验收。

<a id="task-45"></a>

### Task 45：统一认证请求与生物识别策略轮换的 Host 并发所有权

- [ ] 以真实 Host/Provisioner 验收共享 UI lease 的生命周期交错：普通认证与生物识别轮换争用、等待取消、Host pause/destroy、旧 callback 与新 owner 隔离。journal 收尾和轮换提交结果由 Task 59 跟踪；本 Task 不重复这些事务验收。

共享 lease、单飞请求、request identity 及现有 helper 测试见[认证专题](../../../platform/security/authentication.md)；本项只跟踪真实 Host/Provisioner 交错，helper 单测不能代替 Activity 生命周期验收。


<a id="task-64"></a>

### Task 64：让附件 GC 解锁后完整排空并在失败后恢复

- [ ] 在真实 SQLCipher/文件系统执行 AttachmentResourceGarbageCollectorTest 多批排空、失败文件重试、worker 重建唤醒，验证撤权、当前/历史引用保护及最终回收。
- [ ] 沿 Vault `EditorWriteSession`、Detail revision owner 与 Autofill/Inbox 调用链验证 durable commit 后取消的结果映射、离页/撤权恢复与重试，特别确认普通创建不会重复生成 EntryId；当前只有静态链路证据，尚未验证 owner 行为。

附件 GC 与提交后清理的当前合同、实现函数及源码测试覆盖见[数据库专题](../../../platform/data/database.md#历史与附件生命周期)和[审查记录](../../reviews/2026-11-11-attachment-gc-postcommit-review.md)；本 Task 仅保留设备运行与 owner 级结果验收。
