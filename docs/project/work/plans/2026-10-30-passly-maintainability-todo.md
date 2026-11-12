# Passly 深度维护审查待办

**最近部分复核：2026-11-12。** 已验收的 Runtime Session lease 生命周期、DatabaseReset 成功反馈计时器和通知分发平台测试已移出活动待办。Task 84 中 Scanner 相机/结果 Session、数据库重试/设置读取及共享 PageEffects 的 owner 候选已分别复核并归档；尚未完成的 ViewModel、Route 和宿主级验收仍保留。其余范围沿用先前全模块 Kotlin 静态盘点和生产调用闭包证据，不等同于全仓安全扫描或逐行证明。

## 执行顺序

保留原 Task ID，正文按批次重排。P0 表示工程门禁阻断，P1 为敏感/资源安全或具体运行流程缺口，P2 为恢复/交付合同，P3 为结构维护。**retained** 表示仍有证据支持的工作；**deferred/待验收** 表示已有机制但指定验证未完成；**removed** 仅移除过时缺陷，不代表整项已通过。编号不代表优先级。

| 顺序 | 执行入口 | 依赖与原因 |
| --- | --- | --- |
| 1 / P1 | [安全 Task 6](2026-11-06-passly-security-scan-followups.md#task-6)、[Session 与 DEK 审查记录](../../reviews/2026-11-07-session-close-and-dek-wipe.md)，本页 37、59 | 接收方身份、lease 关闭、撤权与提交事实；Runtime 自身 lease 排空已验收，继续闭合 Data admission 与 Security/reset 跨层终态 |
| 2 / P1 | 本页 42、40、32；[诊断 Tasks 1–3](2026-11-06-passly-diagnostics-reliability-followup.md) | seed/流资源、目录操作终态、分类查询与 flush；先固定失败结果再改 owner |
| 3 / P2 | 本页 46、51；[安全 Tasks 3、7、9](2026-11-06-passly-security-scan-followups.md)、OTP/搜索专题；Autofill Task 82（设置策略） | 46 接 59 的提交后恢复；Autofill 设置策略待办由专题清单维护 |
| 4 / 定向验收 | 本页 22、27、29、44、45、64；[Autofill 宿主验收](2026-11-07-passly-autofill-followups.md#task-21)、[Scanner 行为与设备验收](2026-11-06-scanner-entry-and-framing.md) | 已有机制，不重复实现；JVM、Route/宿主和设备验证分别标记 |
| 5 / P3 | 本页 83 → 84/80，35、34、36、74、75、77 | 83 管 Detail 具体流程，84 管 owner 清单，80 管状态模式；77 先稳定诊断可靠性 |

安全 Task 1/5/10、Autofill 设置快照和非 ASCII 搜索仍有策略合同待裁定；策略确认前不将候选行为升级为已验证漏洞。OTP 列表 HOTP 复制由当前实现确定为支持，Vault ViewModel 的消费/交付/撤权单元断言已记录在测试逻辑审查；OTP 专题只继续跟踪 Runtime/Detail/Route/NavHost 的 owner 交接与设备验收。允许独立项并行，表中箭头表示必须先满足的前置条件。

## 专题 owner 索引

| 主题 | 唯一详细待办/验收来源 | 本页关联 |
| --- | --- | --- |
| 架构与并发 | [架构与并发 TODO](2026-11-06-app-architecture-concurrency-todo.md) | 37、51、80、83、84；具体缺陷先处理，广域提案随后 |
| Autofill 后续 | [唯一后续清单](2026-11-07-passly-autofill-followups.md) | 21 宿主验收、82 设置策略；其他已完成合同已归入主题文档 |
| Scanner | [唯一行为与设备验收计划](2026-11-06-scanner-entry-and-framing.md)、[输入与生命周期合同](../../../product/scanner/input-lifecycle.md)、[结果交接合同](../../../product/scanner/result-handoff.md) | [并发计划 Task 5](2026-11-06-app-architecture-concurrency-todo.md#task-5) 只跟踪 Camera Host 资源 lifecycle；Scanner 计划跟踪 picker/Route、解码器、几何和设备验收 |
| Vault / OTP | [搜索 TODO](2026-11-06-passly-vault-search-review-followups.md)、[OTP TODO](2026-11-06-passly-otp-review-followups.md) | 27、32、34–36、42 |
| Backup / 诊断 | [Backup 格式说明](../../../product/backup/passly-archive-v1.md)、[数据端口迁移审查记录](../../reviews/2026-11-07-backup-data-port-migration.md)、[诊断可靠性 TODO](2026-11-06-passly-diagnostics-reliability-followup.md) | 40、73、74、77 |
| 安全与资源 | [安全遗留 TODO](2026-11-06-passly-security-scan-followups.md) | 37、44–46、59、64；Runtime lease 关闭验收归档在 Session 审查记录 |

<a id="task-37"></a>

### Task 37：验收认证撤权与数据库操作的线性化终态
- [ ] 增加可控挂起的跨层终态回归：在 Security lock 等待数据库 lease/resource 与敏感 key 清理时，从 runner、直接 Full Vault Repository 与 Autofill Repository 发起读写，验证线性化点之后新命令不触达 DAO；已进入事务准确报告提交/回滚/取消结果，敏感 permit、会话、DEK、key 与 lock state 一致。边界与真实 Repository 集成测试证明调用闭包后，不需为每个薄适配器复制同一测试。
- [ ] 在可用设备上验收真实 Data lease/Room DAO 与 Security reset/lock 交错终态；instrumentation runtime 尚未执行。恢复模式、Autofill session 结束和 idle timeout 的撤权次序仍需按本项统一验收。

Session 撤权顺序、数据库准入与 Room observation 的当前合同，以及已完成迁移的历史验证边界，见[数据库撤权线性化审查](../../reviews/2026-11-11-database-revocation-linearization.md)；权威运行规则见[数据库专题](../../../platform/data/database.md)和[Session/DEK 审查](../../reviews/2026-11-07-session-close-and-dek-wipe.md)。本 Task 仅保留跨层 owner 终态和设备交错验收。
<a id="task-59"></a>

### Task 59：闭合认证设置变更的提交后恢复与启动对账

- [ ] 补 `RotateBiometricKeyUseCase`、`DefaultAuthenticationMethodProvisioner` 与持久 Store 的集成回归：验证 cleanup/availability refresh pending 时已提交的 biometric binding 仍报告成功，维护重试不会重复轮换。
- [ ] 用真实 Authentication Host、Provisioner 和启动 owner 验收轮换提交前后的异常/取消/fatal、alias/journal 恢复及 host lease 释放。helper/reconciler 测试边界见下方审查记录；manager 同快照与并发刷新由 Task 46 跟踪。
- [ ] 通过真实 `AppPasswordSettingsInteractor`、Provisioner 与持久 Store 验收提交前后取消：结果、维护重试状态和页面 `isEnabled` 必须反映持久提交事实，不能把已提交的更改显示为失败。

  [`AppPasswordSettingsViewModelTest`](../../../../app/src/test/java/com/aozijx/passly/feature/settings/presentation/security/AppPasswordSettingsViewModelTest.kt) 已覆盖敏感输入清理、页面 owner/生命周期取消、提交前后取消的页面状态与反馈；这些用例使用 fake Provisioner/availability Flow，不代替真实持久链路验收。测试范围见[认证提交后维护审查](../../reviews/2026-11-11-authentication-post-commit-recovery.md)。

**底层实现、测试边界与代码位置：** 见[认证提交后维护审查](../../reviews/2026-11-11-authentication-post-commit-recovery.md)；权威合同见[认证机制与会话](../../../platform/security/authentication.md)。本 Task 只跟踪上列 owner 集成验收。


<a id="task-42"></a>

### Task 42：验收完整表单编辑的 Route/NavHost 失败交接
- [ ] 补 Route/NavHost 行为覆盖：Detail 预加载失败后显示提示并可重试；编辑导航失败或 seed 无效/已消费时显示提示并可重试；Route STOPPED 或 owner 替换后不交付旧结果。另在真实 Activity 配置重建和平台 callback 交错下验收 seed 清理与旧 owner 隔离。

  Coordinator/Store 的 token 补偿、取消和 fatal 清理已有 JVM 覆盖；它们不证明 Route 提示/重试或真实宿主生命周期行为。合同与历史验收边界见[完整表单编辑审查](../../reviews/2026-11-11-full-form-editing-boundaries.md)及[seed Coordinator owner 审查](../../reviews/2026-11-08-vault-presentation-boundaries.md#2026-11-12-entryeditorseedcoordinator-owner-审查)。


<a id="task-40"></a>

### Task 40：验收备份目录清除确认交互
- [ ] 在设备或 AVD 上运行 [`BackupDirectoryClearDialogTest`](../../../../app/src/androidTest/java/com/aozijx/passly/feature/settings/presentation/ui/data/BackupDirectoryClearDialogTest.kt)，记录设备/API 和结果；确认清除期间确认/关闭操作被禁用，且只有用户确认后才调用清除。Interactor/ViewModel 的失败补偿与页面反馈范围见[目录设置失败处理审查](../../reviews/2026-11-08-backup-directory-settings-failure-feedback.md)。

<a id="task-32"></a>

### Task 32：明确 Vault 列表 ViewModel 的工作流边界并补宿主行为覆盖
- [ ] 补齐授权撤销期间的 Vault/Trash 操作和页面 owner 交接：已退出页面或旧 owner 收到的迟到结果不得产生新反馈或执行敏感副作用。
- [ ] 用真实 `VaultRoute` / NavHost 验收 Vault→Detail/Trash 转场与 owner 替换；确认 effect collector、Paging 和 OTP 订阅不重复，撤权后旧页面不会继续执行操作。

已有 `VaultViewModelTest` 与 `TrashCommandUseCaseTest` 的覆盖范围和 Route/宿主缺口见[Vault 列表 owner 审查记录](../../reviews/2026-11-11-vault-list-owner-followup.md)；列表命令和图级 OTP lease 的当前合同见[列表与搜索专题](../../../product/vault/list-and-search.md#代码位置)与[OTP Runtime 专题](../../../product/vault/otp-runtime.md)。


## P2：恢复与交付合同

<a id="task-46"></a>

### Task 46：让认证方式 StateFlow 发布同一持久快照的最新解析结果

- [ ] 补 `DefaultAuthenticationManager`、`AuthenticationAvailabilityResolver`、持久 Store 与 Provisioner 的 owner 集成回归：验证同一持久快照解析、并发/提交后刷新、调用方取消，以及启动首次刷新失败可观察且能重试。

现有 [`AuthenticationMethodsStateTest`](../../../../security/src/test/java/com/aozijx/passly/security/authentication/flow/AuthenticationMethodsStateTest.kt)、[`AuthenticationAvailabilityRetryTest`](../../../../security/src/test/java/com/aozijx/passly/security/authentication/flow/AuthenticationAvailabilityRetryTest.kt) 与 [`AuthenticationMethodsMappingTest`](../../../../security/src/test/java/com/aozijx/passly/security/authentication/flow/AuthenticationMethodsMappingTest.kt) 分别覆盖状态协调、重试调度和纯映射；集成缺口与测试边界见[测试逻辑清理审查](../../reviews/2026-11-11-test-logic-maintenance-review.md#认证可用性测试范围校正)，正式行为合同见[认证专题](../../../platform/security/authentication.md#host-与生物识别)。

<a id="task-51"></a>

### Task 51：明确非 Scanner Route 一次性 effect 的前后台交付合同

- [ ] 逐项定义 Toast/Snackbar、导航、SAF picker、权限和平台交付在 STOPPED、恢复、离页、owner 替换后的有效期；短命反馈明确是否丢弃，待完成请求明确如何恢复。
- [ ] 补真实 Route/宿主测试，逐类覆盖转场、请求代际、锁定/关闭及 callback 异常；分别判断各 effect 应消费、丢弃、重试或恢复，不把不同 effect 塞入统一 handler 表。

共享 `PageEffects` 的 STARTED 收集、页面 owner 隔离及缓冲恢复合同见[状态管理专题](../../../engineering/development/architecture-and-state.md#页面一次性事件的可见性与-owner)。本 Task 只跟踪不同 effect 的有效期决策和 Route/宿主覆盖。


## P3：按 owner 分批整理

<a id="task-73"></a>

### Task 73：复核 Android 质量门禁与交付基线

- [ ] 取得有效的 App/Autofill Lint 报告后，按当前 findings 逐项裁定 EmptySuperCall、冗余 API、UseKtx 与资源项；保留清理/持久时序及中英文合同，不沿用旧报告数量。此前 App lint 在 `:app:lintAnalyzeDebug` 遇到 IntelliJ `AsyncExecutionService.getService must not return null`，没有生成有效报告。
- [ ] 验证 [version catalog](../../../../gradle/libs.versions.toml) 的实际升级：Palette 1.0.0→1.1.0（`:app`）、Material3 1.5.0-beta01→1.5.0-rc01（`:app`、`:core`、`:security`、`:feature:autofill`）、Room 2.8.4→2.8.5（`:data`）、DataStore 1.2.1→1.3.0 与 SQLCipher 4.19.1→4.20.0（`:data`）、Biometric 1.4.0-alpha07→1.4.0-beta01（`:app`、`:security`）、Guava 33.7.2-android→33.8.0-android（`:domain`）；另核对新增的 `paging-common` Domain API 依赖解析与编译。记录受影响模块的依赖解析、编译、必要测试和 SQLCipher 现有加密数据重开兼容性证据及命令。旧“14 条升级提示”只作历史背景，不作为当前未完成数量。
- [ ] APK Analyzer 保存交付/release APK 的 DEX、资源、native、assets 基线；历史 debug 体积不是 release 优化收益，设备/性能验收按功能 owner 执行。

<a id="task-83"></a>

### Task 83：复核 DetailViewModel 剩余流程与修订历史 owner

- [ ] 继续审查 `DetailViewModel` 的其余工作流并记录调用闭包；只有整体迁移确实减少原 owner 的职责或依赖时才拆分，判断标准遵循[维护边界](../../../engineering/development/maintenance-boundaries.md)。

- [ ] 补 `DetailRevisionWorkflow` owner 回归：metadata Flow 发出数据后失败，应进入可重试状态且重试会重新订阅；Session 锁定时 observer 清理，重新建立访问后不复用旧 visit；条目或 Detail visit 替换后，旧 metadata/snapshot 结果不得更新当前页。源码已有失败映射、reset 清理和 token/generation 检查，审查记录说明尚无对应 owner 测试；见[修订历史审查记录](../../reviews/2026-11-08-vault-presentation-boundaries.md)与[修订历史专题](../../../product/vault/entry-revision-history.md)。
- [ ] 对每个实际拆分补充 owner 级行为测试：验证旧 ViewModel 状态与依赖确有减少，UseCase 输入/结果不泄漏敏感值，并覆盖取消、失败重试、session/entry 替换后的迟到结果和资源收尾。同步必要的主题文档与本 TODO 证据；运行受影响测试、`verifyArchitecture` 与 `git diff --check`。本任务是待执行的维护审计与迁移方向，不表示所有大 ViewModel 都需要拆分。

<a id="task-84"></a>

### Task 84：全量审计 ViewModel 与 Coordinator 的职责归属并整理分层规范

- [ ] 对 `:app`、`:security`、`:feature:autofill` 中的生产 ViewModel 逐个完成调用闭包审计，记录保留、迁移、合并或继续调查的结论与证据；不按行数或类名自动判断。

  - Shell/宿主：`AppShellViewModel`、`AuthenticationHostOwnerViewModel`。
  - Vault：`VaultViewModel`、`TrashViewModel`、`DetailViewModel`、`ScannedOtpImportViewModel`、四种 `Add*ViewModel`、`EntryEditorSeedCoordinator`（职责审查归本项，Route/宿主交接验收归 Task 42）。
  - Settings/Backup：`AutofillSettingsViewModel`、`SecuritySettingsViewModel`、`RecoveryCodeSettingsViewModel`、`PrivacySettingsViewModel`、`AppPasswordSettingsViewModel`、`InterfaceSettingsViewModel`、`AppearanceSettingsViewModel`、`PermissionManagementViewModel`、`InteractionSettingsViewModel`、`NotificationSettingsViewModel`、`DiagnosticsViewModel`、`DataManagementSettingsViewModel`、`BackupViewModel`、`StorageSettingsViewModel`。
  - Autofill：`AutofillFillViewModel`、`CredentialResponseViewModel`、`AccessibilityFillViewModel`，均属于 `:feature:autofill`。
  - Scanner/Unlock/Recovery/Database：`ScannerViewModel`、`UnlockViewModel`、`RecoveryModeViewModel`、`DatabaseResetViewModel`。

  同时核对每页 Route/Activity/Content 的实际消费者、DI 绑定、相关测试及调用的 UseCase/Interactor/Repository/Coordinator/Store，不只检查 ViewModel 文件。

- [ ] 在真实 Route/Activity 生命周期中验收 Recovery Code 确认后离页、Activity recreation 与 owner 替换时的 durable commit、disclosure 擦除及旧页面状态隔离。现有 JVM 测试不代替宿主验收；当前合同与 owner 决策见[恢复码 owner 审查](../../reviews/2026-11-11-recovery-code-draft-owner.md)和[认证专题](../../../platform/security/authentication.md#恢复码草稿)。
- [ ] 继续核对 `AppShellViewModel` 对外观/界面/安全设置与认证/session lock 状态的聚合是否仍只承担 Shell 所需的屏幕投影和生命周期 owner；重点审查可独立取消的子 owner。保留合法的 Shell 聚合，不为减少构造器参数拆出一对一转发 Coordinator。
- [ ] 对其余大/多流程候选逐项判断：`VaultViewModel` 的 Paging 查询、设置读取、OTP runtime 和条目命令；Credential Manager 的系统请求/结果交付与业务解析；Legacy/Accessibility Autofill 的授权、续接、选择和平台结果；Diagnostics 导出、清理与权限；Recovery Mode 的凭据设置。[Autofill 后续清单](2026-11-07-passly-autofill-followups.md)、Task 80 的统一架构审计和 Task 83 的 Detail 项应引用并更新状态，不重复复制缺陷清单；分支映射仅在同一 Presentation owner 确有重复、纯显示元数据时考虑合并。
- [ ] 沿具体 Route/宿主核对剩余专项流程 owner：Autofill Settings 的偏好与系统服务 Flow 独立失败恢复、页面退出后的旧 effect 丢弃及写入期间状态变化；Backup 文档选择 request ID 与页面 owner、退出重入/迟到回调/执行中关闭及凭据擦除/提交结果；Unlock 认证、尝试刷新和首次密码设置的 owner、取消/fatal 清理及页面反馈。
- [ ] 对 Settings、Shell、Diagnostics、Database Reset 和 Recovery Mode 的 ViewModel 按状态真相源、合法状态、长 Flow 失败重订阅、任务取消、敏感清理及 Route effect 核对。
- [ ] 对每个 ViewModel 依赖分类并验证层边界：可观察的 Domain/Repository 数据、完整业务动作 UseCase/Interactor、窄应用端口、Presentation 状态 owner、平台/持久化实现。继续禁止 ViewModel 间依赖及直接 DAO/Room/Data Entity/具体文件实现；明确哪些 `feature.*.data` 或 `feature.*.internal` 类型实际是跨层实现泄漏，避免仅按 `internal` 包名搬移。核查 `verifyArchitecture` 当前 ViewModel 门禁与 `ViewModelDependencyVerifier` 的覆盖范围；若新增静态边界，只针对已证实错误依赖设计白名单/反例测试，不把合法 Repository Flow 或窄业务端口误禁。
- [ ] 将经验证且需要跨页面复用的稳定规则同步到 `docs/platform/architecture/viewmodel-ownership.md`、`docs/platform/architecture/package-boundaries.md`、`docs/engineering/development/maintenance-boundaries.md` 及必要功能文档，并使本 TODO 保留未迁移清单与决策证据。迁移需要分批按 Feature 执行；每次必须减少原 ViewModel 的独立业务职责、状态、异步任务组或实现依赖，而不是只变短文件。覆盖取消/重试/离页迟到结果/配置重建/会话锁定/持久提交点/敏感清理；运行对应 ViewModel/Route/宿主测试、相关模块门禁、`verifyArchitecture` 和 `git diff --check`。

<a id="task-80"></a>

### Task 80：统一 Presentation 的 MVVM 与单向数据流契约

- [ ] 按真实 owner 审查持久数据、屏幕投影和用户草稿是否各有单一状态真相源；检查 Repository Flow 与手动赋值竞争、长期镜像状态及多个 owner 声称拥有同一字段，并继续核对 Vault 剩余编辑器、Trash 与导航状态的调用闭包和实现依赖。保留有明确提交/丢弃语义的草稿和 Paging 等独立状态；Route 与跨层实现 owner 的单项验收分别见 Task 32、42、83、84。
- [ ] 审查 Action、状态和 effect 的生产/消费路径，合并纯转发和重复状态；只有在带来明确状态转换保证时保留 MVI ceremony。系统 effect 的前后台有效期由 Task 51 覆盖，跨层工作流和 ViewModel 职责闭包由 Task 84 覆盖，Detail 候选由 Task 83 覆盖。
- [ ] 按 feature/owner 分批迁移状态或 effect 所有权；只在真实合同改变时同步正式文档，并以 Route/ViewModel/宿主行为测试验证。保留符合边界的页面，不为架构统一引入全局状态、万能 reducer 或转发层；每批运行受影响门禁及 `git diff --check`。

正式分层与状态规则见[状态管理与 ViewModel](../../../engineering/development/architecture-and-state.md)和[ViewModel owner 边界](../../../platform/architecture/viewmodel-ownership.md)。Detail/List/完整编辑的具体 Route 与工作流验收分别归 Task 83、32、42；本 Task 只跟踪状态真相源、Action 与 effect 模式的剩余逐页核对。

<a id="task-35"></a>

### Task 35：验收 EntryLink 写入与端点删除竞态

- [ ] 以真实 Room AndroidTest 交错验证端点删除与 [`RoomEntryLinkRepository.upsert()`](../../../../data/src/main/java/com/aozijx/passly/data/repository/entry/RoomEntryLinkRepository.kt)：无论 upsert 或删除先提交，最终都不得留下指向缺失或已删除端点的 link。DAO 唯一约束、同 ID 更新和级联删除已有[`EntryLinkConstraintDaoTest`](../../../../data/src/androidTest/java/com/aozijx/passly/data/local/database/EntryLinkConstraintDaoTest.kt)覆盖；本项只追踪 Repository 事务竞态，审查结论见[测试逻辑清理审查](../../reviews/2026-11-11-test-logic-maintenance-review.md)。

<a id="task-34"></a>

### Task 34：验收 Room 会话观察与修订恢复
- [ ] 在设备上运行 [`RoomObservationSessionTest`](../../../../data/src/androidTest/java/com/aozijx/passly/data/local/database/session/RoomObservationSessionTest.kt) 和 [`EntryUpdateAndRevisionRestoreTransactionTest`](../../../../data/src/androidTest/java/com/aozijx/passly/data/repository/entry/EntryUpdateAndRevisionRestoreTransactionTest.kt)，记录设备/API、SQLCipher 结果，并确认会话撤权、旧 DAO emission、恢复订阅和修订恢复事务行为。此前的安装失败证据见[测试逻辑清理审查记录](../../reviews/2026-11-11-test-logic-maintenance-review.md#task-34-36-device-install-attempt)。

<a id="task-36"></a>

### Task 36：验收修订 DAO 生命周期与重建行为
- [ ] 在设备上运行 [`EntryRevisionLifecycleDaoTest`](../../../../data/src/androidTest/java/com/aozijx/passly/data/local/database/EntryRevisionLifecycleDaoTest.kt) 和 [`EntryRevisionHistoryRebuilderTest`](../../../../data/src/androidTest/java/com/aozijx/passly/data/repository/entry/EntryRevisionHistoryRebuilderTest.kt)，记录设备/API 和结果；确认历史上限、恢复后最新快照、回收站清空及附件/图标引用保护行为。Room 会话观察和修订恢复事务由 Task 34 跟踪；此前的安装失败证据见[测试逻辑清理审查记录](../../reviews/2026-11-11-test-logic-maintenance-review.md#task-34-36-device-install-attempt)。

<a id="task-74"></a>

### Task 74：按测试对象优化单测结构与重复装配
- [ ] Task 83 的 Detail owner 审查与迁移收敛后，按独立工作流和生命周期职责重新评估是否拆分 `DetailViewModelTest.kt`；不按文件长度拆分。Task 83 仍开放时保留现有 fixture。审查范围与当前拆分结论见[测试逻辑清理审查记录](../../reviews/2026-11-11-test-logic-maintenance-review.md#detailviewmodeltest-拆分评估)及[Task 83](#task-83)。
- [ ] 继续全仓 `src/test` 与 `src/androidTest` 逻辑审查，逐项追踪生产 owner、断言输入/状态/副作用及失败/取消清理。已审查模块、各测试保留/删除理由和验证记录见[测试逻辑清理审查记录](../../reviews/2026-11-11-test-logic-maintenance-review.md)；全仓审查仍未完成。
- [x] 2026-11-12 在当前工作树定向编译并运行以下 JVM 测试，验证 OTP 映射测试迁移及 Backup owner 覆盖能通过生产与测试源集编译：
  - [`VaultListUiMapperTest`](../../../../app/src/test/java/com/aozijx/passly/feature/vault/presentation/list/VaultListUiMapperTest.kt)
  - [`OtpUiStateMapperTest`](../../../../app/src/test/java/com/aozijx/passly/feature/vault/presentation/list/OtpUiStateMapperTest.kt)
  - [`BackupArchiveServiceImplTest`](../../../../app/src/test/java/com/aozijx/passly/feature/backup/internal/archive/BackupArchiveServiceImplTest.kt)
  - [`DefaultBackupOperationGatewayTest`](../../../../app/src/test/java/com/aozijx/passly/feature/backup/internal/operation/DefaultBackupOperationGatewayTest.kt)

  使用 `--no-build-cache --no-configuration-cache --no-daemon --no-parallel --max-workers=2` 执行；四类测试共 48 项，0 failures、0 errors、0 skipped。此前的缓存/daemon 与 Data 可见性门禁问题及修正记录见[审查记录](../../reviews/2026-11-11-test-logic-maintenance-review.md)。本项 JVM 定向验证完成，不代表全仓逻辑审查、Route/宿主或设备验收完成。

<a id="task-75"></a>

### Task 75：验收 Favicon 暂存、提交与启动恢复

- [ ] 在设备或 AVD 上运行 [`FaviconImageStoreTest`](../../../../app/src/androidTest/java/com/aozijx/passly/app/entry/favicon/FaviconImageStoreTest.kt)，记录设备/API，并确认暂存晋级、失败输入清理、路径拒绝、过期暂存清理和启动恢复结果。

Favicon 资源安全、文件 owner 与清理合同见[远端内容与图标](../../../platform/security/remote-content.md)；本 Task 只跟踪该 instrumentation 的设备/AVD 执行。App lint 有效报告由 Task 73 跟踪。

<a id="task-77"></a>

### Task 77：核实诊断日志退出时的持久性与周期写入策略

- [ ] 稳定 flush 合同后调查离开/后台/移除 task/force-stop/强杀的日志损失窗口，记录是否等待 flush、最后持久批次及设备/API；任意终止不保证回调。
- [ ] 先完成[诊断 Tasks 1、2](2026-11-06-passly-diagnostics-reliability-followup.md)，再裁定周期/退出策略，不因独立模块或进程就宣称 durable。

当前 flush 与生命周期调用链见[运行时与加密存储](../../../platform/architecture/observability/runtime-and-storage.md)；本 Task 只跟踪设备上的退出/终止窗口和最后持久批次。

## 已有机制：剩余定向验收


<a id="task-22"></a>

### Task 22：验收通知权限重建与启动失败交接

- [ ] 在设备上验收 `AppCompatDelegate.setApplicationLocales()` 引发的 Activity 重建与系统授权/拒绝弹窗组合；确认新 Route/host 接管有效请求、旧 host/session 不产生重复提交或过期反馈，且 PermissionRequestHost arbiter lease 最终释放。
- [ ] 补 `NotificationsRoute` 与 `PermissionRequestHost` 联动覆盖：验证 `Busy` / `HostUnavailable` 映射为对应提示，并将未启动结果交给当前请求 owner，不误报系统拒绝或写入开启偏好。

  新 request token、pending 清理和配置重建重绑已有 `NotificationSettingsViewModelTest` 覆盖；其范围与未覆盖的 Route/host 分支见[通知权限审查记录](../../reviews/2026-11-11-notification-permission-not-started.md)。

<a id="task-27"></a>

### Task 27：验收 OTP Runtime 配置失效的 Route 与 owner 交接
- [ ] 补真实 Compose Route/NavHost/宿主回归：验证保存后返回 Detail 的显示与复制读取新 OTP 配置，取消/失败不触发成功刷新，以及列表/Detail owner 变化、锁定、离页和恢复时的 lease 与迟到结果。

  ViewModel 写入后通知 Runtime 失效旧配置的实现与历史测试结果见[OTP Runtime owner 与失效审查](../../reviews/2026-11-11-otp-runtime-owner-and-invalidation.md)；它不证明真实导航交互。

<a id="task-29"></a>

### Task 29：验收 App Password 草稿的真实宿主生命周期清理

- [ ] 在设备或 AVD 上运行 [`AppPasswordDialogCoordinatorLifecycleTest`](../../../../app/src/androidTest/java/com/aozijx/passly/feature/settings/presentation/security/AppPasswordDialogCoordinatorLifecycleTest.kt) 并记录设备/API 与结果；再通过真实 Activity 配置重建验证草稿按认证合同清空。Compose 测试覆盖边界见[App Password 对话框生命周期审查](../../reviews/2026-11-11-app-password-dialog-lifecycle.md)。

<a id="task-44"></a>

### Task 44：验收迟到 AuthenticationHost 密码回调的清理与隔离

- [ ] 在真实 `ActivityAuthUiHost` 生命周期中取消、替换或重建认证请求后触发迟到密码 dialog callback；保留 callback 的实际 `CharArray` 引用，验证旧 identity/CAS 失配时输入被擦除且不会提交给新 request。

`SecretDialogSubmissionTest` 已覆盖无请求、非活动请求、有效接管与 identity 失配时的输入处理；它不覆盖真实 Activity/Host 生命周期与 `ActivityAuthUiHost.submitSecret()` 的 CAS 失配交错。准确函数、合同与 helper 测试见[认证专题](../../../platform/security/authentication.md)。

<a id="task-45"></a>

### Task 45：验收认证与生物识别轮换的 Host 并发所有权

- [ ] 以真实 Host/Provisioner 验收共享 UI lease 的生命周期交错：普通认证与生物识别轮换争用、等待取消，以及 Host pause/destroy 后 lease 释放和后续 owner 接管。迟到 callback 隔离由 Task 44、journal 收尾和轮换提交结果由 Task 59 跟踪。

共享 lease、单飞请求、request identity 及现有 helper 测试见[认证专题](../../../platform/security/authentication.md)；本项只跟踪真实 Host/Provisioner 交错，helper 单测不能代替 Activity 生命周期验收。


<a id="task-64"></a>

### Task 64：验收附件 GC 重试与提交后 owner 恢复

- [ ] 在真实 SQLCipher/文件系统设备运行 [`AttachmentResourceGarbageCollectorTest`](../../../../data/src/androidTest/java/com/aozijx/passly/data/repository/attachment/AttachmentResourceGarbageCollectorTest.kt) 的多批排空、文件删除失败重试和解锁后恢复持久待办用例；另验收撤权、当前/历史引用保护与最终回收，并记录设备/API 结果。
- [ ] 沿 Vault `EditorWriteSession`、Detail revision owner 与 Autofill/Inbox 调用链验证 durable commit 后取消的结果映射、离页/撤权恢复与重试，特别确认普通创建不会重复生成 EntryId；当前只有静态链路证据，尚未验证 owner 行为。

附件 GC 与提交后清理的当前合同、实现函数及源码测试覆盖见[数据库专题](../../../platform/data/database.md#历史与附件生命周期)和[审查记录](../../reviews/2026-11-11-attachment-gc-postcommit-review.md)；本 Task 仅保留设备运行与 owner 级结果验收。
