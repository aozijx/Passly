# 长函数与大文件职责审查

审查日期：2026-11-02。行数按本次工作区格式化后的 Kotlin 源码统计；行数仅用于定位，KtLint 的换行会显著改变物理行数。

## 审查结论

| 文件 | 行数 | 当前职责与 owner | 结论与依据 |
| --- | ---: | --- | --- |
| `security/src/main/java/com/aozijx/passly/security/authentication/DefaultAuthenticationManager.kt` | 332 | `authenticateInternal()` 统筹单飞互斥、会话复用、host lease、认证方式选择与执行、凭据擦除、认证状态提交/恢复。 | 保留为一次认证事务。各阶段共享 `requestMutex`、`hostLease`、`previousState` 与 credential `finally` 清理；拆成接收同一组状态的 helpers 不会产生独立 owner。外层 fatal `Throwable` 映射已由 Task 63 单独跟踪。 |
| `app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/DetailViewModel.kt` | 1,059 | Detail visit、主快照、活动、标签、favicon、复制/OTP/QR、reveals 和 revision UI 的页面生命周期 owner；已有 `DetailWorkSession`、`DetailFaviconSession`、`DetailQrSession` 与 OTP runtime。 | 不拆 392 行 `onAction()` 的穷尽式 sealed `when`，也不将分支搬进转发 helpers。Revision 与同一 `DetailWorkSession` token、敏感值清理及恢复后根快照刷新相连，当前拆出只会增加父 VM 回调桥。重复打开同一条目 revision 的取消 bug 已由 [Task 58 修复](../features/vault/entry-revision-history.md)。 |
| `security/src/main/java/com/aozijx/passly/security/authentication/VaultSessionController.kt` | 604 | 认证态、unlock/recovery、数据库会话重试、reset、soft-lock/lock 的串行转换 owner。 | 保留。状态写入、数据库 seal、密钥/授权清理和取消收尾共享同一会话 mutex；按函数搬移会拆散 Task 26/54/55 已覆盖的状态转换合同。 |
| `buildSrc/src/main/kotlin/passly/SourceBoundaryPolicy.kt` | 712 | `SourceBoundaryPolicy` 声明模块、package、Feature/API 和 import 方向约束，供 verifier 使用。 | 保留单一策略目录，避免规则拆散后由多份配置重复决定合法边界。规则族的测试拆分留给 Task 74。 |
| `core/src/main/kotlin/com/aozijx/passly/core/observability/LogStatements.kt` | 706 | 结构化日志事件的构造目录，内部按 Application、Database、Security、Autofill、Backup、Vault 子域分组。 | 保留集中目录以维持稳定事件/code/字段 schema 的单一来源；按文件拆分会分散同一 `LogStatement` 合同。 |
| `buildSrc/src/test/kotlin/passly/SourceBoundaryPolicyTest.kt` | 1,410 | 约 60 个边界测试，覆盖 package/layer matrix、FQN 与字符串模板扫描、Feature owner 和各层禁用依赖。 | 暂缓生产之外的测试拆分；场景已有具名测试，拆分需共用大量 source fixture。Task 74 已独立跟踪按 policy family 拆套件，避免重复建项。 |
| `data/src/main/java/com/aozijx/passly/data/observability/EncryptedLogRecordStore.kt` | 460 | 有界 writer queue、分页读/解密、flush/clear/close、崩溃与 overflow emergency write。 | 保留在 store owner：writer、emergency worker、key 和文件句柄共同受 `writeAcceptanceLock`/`writeLock` 及关闭时限约束。将读写拆开会跨对象暴露同一锁与文件生命周期。 |
| `app/src/main/java/com/aozijx/passly/feature/vault/otp/OtpCodeRuntime.kt` | 354 | OTP entry subscription/activation、session 与 entry epoch、fresh-auth pause、HOTP 消费、TOTP 调度和敏感态清理。 | 保留。调度表、epoch、订阅和生成状态共享 token 校验；拆 ticker/activation 会让取消与锁后失效合同跨 owner。 |
| `app/src/main/java/com/aozijx/passly/feature/autofill/presentation/credential/CredentialResponseViewModel.kt` | 337 | Credential Manager get/unlock/create 请求、平台交互取消、trace continuation 和一次性 result completion。 | 保留。三条 action 共用同一平台 completion、interaction 生命周期及结果交付；现有 helper 已按平台分支命名，进一步拆分会引入重复的 result bridge。 |
| `app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/ui/component/FaviconEditorSheet.kt` | 410 | Favicon 编辑 surface 与 source/library/crop/progress/error 的纯 Compose 展示。 | 保留。内容子项已由私有 Composable 分组，异步图片 owner 在 `DetailFaviconSession`；没有额外状态机或副作用可迁移。 |
| `app/src/main/java/com/aozijx/passly/feature/vault/presentation/trash/ui/TrashBottomSheet.kt` | 413 | Trash 列表、加载/错误/空内容、删除与清空确认弹窗。 | 保留。屏幕参数和回调来自上层状态 owner，sheet 内只有短期对话框选择状态；拆出仅按视觉块增加跨文件参数。 |
| `security/src/main/java/com/aozijx/passly/security/autofill/inbox/AutofillInboxRecordCodec.kt` | 361 | Inbox record header、认证加密、payload 二进制编码/解码、长度与枚举校验、临时明文擦除。 | 保留为同一加密格式 codec；seal/open 共用版本、AAD、二进制字段顺序和 wipe 规则，避免格式实现散落。 |
| `app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/format/bitwarden/BitwardenJsonImportAdapter.kt` | 372 | Bitwarden JSON identity/probe、JSON decode、item 字段归一化、OTP 拆分和稳定关联 ID 生成。 | 保留为外部格式 adapter；`toBackupRecords()` 属于该格式到内部 archive 的单向转换，内部私有辅助函数未形成其他 consumer。 |
| `app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/mapper/KeyedEntryArchiveMapper.kt` | 372 | Domain Entry 与 keyed archive record 的双向字段映射及稳定 key 校验。 | 保留。`toEntry()` 的 credential-kind `when` 与 `toArchiveFields()` 是同一稳定格式的逆向投影，穷尽式分支可在 EntryCredentialKind 扩展时由编译器提示。 |
| `security/src/main/java/com/aozijx/passly/security/authentication/SecuritySessionEventLogger.kt` | 343 | Lock/auth trace 生命周期、异常优先级与公开日志字段转换。 | 保留。两个 trace builder 使用同一事件记录与异常传播约束；简单拆文件不会改变 trace owner。事件目录仍由 `LogStatements` 拥有。 |
| `app/src/main/java/com/aozijx/passly/feature/autofill/legacy/service/builder/LegacyResponseFactory.kt` | 331 | Android Autofill `FillResponse`/`Dataset` 协议对象和 unlock/candidate/picker Authentication response。 | 保留为 Android 协议 builder。已有 `buildDataset`、unlock、candidate、picker helpers；拆成 facade 只会把 `Context`/`FillResponse` 继续转发。ViewModel framework-result ownership 由独立 Autofill 待办跟踪。 |
| `app/src/main/java/com/aozijx/passly/feature/backup/internal/archive/format/encrypted/EncryptedBackupContainerCodec.kt` | 269 | 加密容器 encrypt/decrypt、认证 header、magic/version/长度检查及 KDF 参数校验。 | 保留为单一 wire-format codec；parse/encode 与 AEAD 验证共享 header bytes 和协议失败语义。 |
| `app/src/main/java/com/aozijx/passly/feature/settings/presentation/ui/diagnostics/DiagnosticsScreen.kt` | 289 | Diagnostics 搜索/等级筛选展示与格式、清空、复制 dialogs。 | 保留为纯 UI 内容组合；筛选、导出和清空行为由 UiState/Action owner 提供，私有 Composable 已按区域拆分，无 UI 外状态流需迁移。 |
| `app/src/main/java/com/aozijx/passly/app/database/backup/RoomBackupSnapshotRestorer.kt` | 257 | Bundle 校验、APPEND/OVERWRITE DB transaction、Entry/Link/Attachment 映射与加密、文件 journal 提交/回滚及清理。 | 保留为 restore transaction owner。数据库事务与附件 journal 必须以同一成功/失败边界协作；`retainRestoredAttachmentFile()` 仅管理同事务中资源文件的保留与回滚，并非可独立提交的 facade。Task 65 单独覆盖一致性测试。 |
| `app/src/main/java/com/aozijx/passly/feature/unlock/presentation/ui/AuthenticationScreen.kt` | 319 | 解锁/recovery 的输入、错误提示和认证方式布局。 | 保留为无副作用 Compose 内容；实际提交、认证请求与生命周期由 Route/ViewModel 持有。UI 子块按功能命名，无须按行数另拆。 |

### 其余命中 300 行筛查阈值的文件

这些文件也已按测试对象或生产 owner 核对。测试只在多个独立行为组确实验证同一生产 owner 时保留在一个文件；Task 74 中已列出的拆分仍按该任务执行。

| 文件 | 行数 | owner / 结论 |
| --- | ---: | --- |
| `app/src/test/java/com/aozijx/passly/feature/vault/presentation/detail/DetailViewModelTest.kt` | 1,102 | Detail 页面状态/异步生命周期行为；当前覆盖 package、history 和恢复后迟到结果，保留为跨工作流集成套件。测试夹具拆分留 Task 74。 |
| `security/src/test/java/com/aozijx/passly/security/authentication/VaultSessionIdleTimeoutTest.kt` | 1,077 | Security 会话锁定、reset、取消、retry、idle timeout 与 inbox provisioning；一个文件混合多个 owner，明确拆分由 Task 74 处理。 |
| `runtime/session/src/test/kotlin/com/aozijx/passly/runtime/session/RuntimeSessionManagerTest.kt` | 862 | Runtime session 状态、租约、open/resume/seal/close 竞争及取消；共享同一 SessionResource 假实现和状态终态断言，暂保留。 |
| `app/src/test/java/com/aozijx/passly/feature/backup/internal/archive/BackupCodecTest.kt` | 746 | ZIP、加密容器、archive registry、JSON wire model 与资源校验混合；拆分到实际 codec/validator owner 由 Task 74 处理。 |
| `app/src/test/java/com/aozijx/passly/app/message/runtime/DefaultAppNoticeDispatcherTest.kt` | 564 | Dispatcher 的 route、重复事件、sink 失败和设置变化；共享 dispatcher 的顺序/交付合同，暂保留。 |
| `data/src/androidTest/java/com/aozijx/passly/data/local/database/session/RoomObservationSessionTest.kt` | 524 | Room session/invalidation 在真实数据库上的观察合同；保留为同一 Room/session 集成场景。 |
| `app/src/test/java/com/aozijx/passly/app/observability/ObservabilityExportServiceTest.kt` | 478 | ExportService 的分页、取消、格式与资源关闭；均经过同一导出资源 owner，暂保留。 |
| `app/src/test/java/com/aozijx/passly/feature/vault/detail/EditDetailEntryUseCaseTest.kt` | 464 | Detail edit transaction 的成功、冲突、隐藏字段保留和失败结果；属于单一 UseCase 写入合同，保留。 |
| `app/src/test/java/com/aozijx/passly/feature/vault/presentation/editor/wifi/AddWifiViewModelTest.kt` | 455 | AddWifi 草稿、扫码 seed、编辑初始化和保存 action；一页面状态 owner，保留。 |
| `data/src/androidTest/java/com/aozijx/passly/data/repository/entry/EntryRevisionRestoreTransactionTest.kt` | 452 | Revision restore transaction 的 DB 写入/rollback/invalidation；共享 transaction fixture，保留为一个集成 owner。 |
| `app/src/test/java/com/aozijx/passly/feature/autofill/credential/CredentialManagerFlowTest.kt` | 442 | Credential Manager 请求到一次性平台结果的主流程；同一协议交付生命周期，保留。 |
| `data/src/test/java/com/aozijx/passly/data/observability/EncryptedLogRecordStoreTest.kt` | 439 | Store 写队列、分页读取、flush/clear/close/emergency write；单一 store 并发合同，保留。 |
| `app/src/test/java/com/aozijx/passly/feature/vault/otp/OtpCodeRuntimeTest.kt` | 430 | OTP activation、订阅、fresh-auth、HOTP/TOTP、锁定清理；单一运行时 owner，保留。 |
| `app/src/test/java/com/aozijx/passly/feature/settings/presentation/main/diagnostics/DiagnosticsViewModelTest.kt` | 424 | Diagnostics ViewModel 搜索/分页/导出/清理；同一状态和 action owner，保留。 |
| `app/src/test/java/com/aozijx/passly/feature/vault/presentation/editor/otp/AddOtpViewModelTest.kt` | 424 | OTP 编辑器输入验证、扫描导入、初始化与保存；单一 editor owner，保留。 |
| `app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/DetailReducer.kt` | 421 | Detail 页面根状态协调及 tag/favicon/revision 子状态投影。 | 保留根 reducer；已有子 reducer 承担独立状态转换，不再将子逻辑塞回根 reducer。 |
| `app/src/test/java/com/aozijx/passly/feature/settings/backup/BackupDirectorySettingsInteractorTest.kt` | 406 | Backup directory grant、选取、撤销与持久状态；同一 settings interactor 合同，保留。 |
| `app/src/test/java/com/aozijx/passly/feature/vault/presentation/trash/TrashViewModelTest.kt` | 401 | Trash 查询、restore/delete/empty 与失败重试；单一 Trash VM 行为 owner，保留。 |
| `data/src/test/java/com/aozijx/passly/data/mapper/entry/EntrySecretMapperTest.kt` | 397 | Credential subtype 到敏感字段 mapper 的字段/清除合同；一个 production mapper，保留。 |
| `runtime/session/src/main/kotlin/com/aozijx/passly/runtime/session/SessionLeaseGate.kt` | 392 | Lease acquire/release、drain、seal 与资源 close 协调；锁和引用计数共享状态，保留为单一 gate。 |
| `app/src/test/java/com/aozijx/passly/feature/settings/presentation/security/RecoveryCodeSettingsViewModelTest.kt` | 388 | Recovery code issuance、确认和失败状态；单一设置 VM owner，保留。 |
| `app/src/test/java/com/aozijx/passly/feature/autofill/core/AutofillInteractionLifecycleTest.kt` | 383 | Interaction authorize/discover/select/deliver/finish 生命周期；跨操作的同一 interaction store 状态机，保留。 |
| `app/src/test/java/com/aozijx/passly/app/entry/creation/CreateEntryUseCaseTest.kt` | 369 | Entry creation、字段 materialize、policy 与 repository failure；单一创建用例 owner，保留。 |
| `app/src/test/java/com/aozijx/passly/feature/vault/presentation/detail/DetailWorkSessionTest.kt` | 360 | Visit token、stale async result、persist/reveal 和 wipe；对应一个 session owner，保留。 |
| `security/src/main/java/com/aozijx/passly/security/authentication/host/ActivityAuthUiHost.kt` | 353 | Activity-owned credential/bio prompts、request ownership 与取消/结果清理；一个宿主生命周期 owner，保留。 |
| `app/src/main/java/com/aozijx/passly/feature/settings/presentation/security/RecoveryCodeSettingsViewModel.kt` | 352 | Recovery code generation/acknowledgement 的 settings 状态和一次性敏感结果；保留页面 owner。 |
| `app/src/test/java/com/aozijx/passly/feature/vault/presentation/editor/password/AddPasswordViewModelTest.kt` | 344 | Add Password 字段输入、验证、编辑/保存 action；单一页面状态 owner，保留。 |
| `app/src/test/java/com/aozijx/passly/feature/settings/presentation/main/data/StorageSettingsViewModelTest.kt` | 340 | Storage settings 观察、清理 action 和状态错误；同一 ViewModel owner，保留。 |
| `app/src/test/java/com/aozijx/passly/feature/backup/internal/operation/DefaultBackupOperationGatewayTest.kt` | 338 | Backup 操作 gateway 的认证、读写、资源生命周期与失败传播；单一 gateway contract，保留。 |
| `app/src/main/java/com/aozijx/passly/feature/backup/presentation/BackupViewModel.kt` | 335 | Backup screen 状态、格式识别与一次性 document result；操作执行已委托 gateway/use case，保留 screen owner。 |
| `app/src/test/java/com/aozijx/passly/feature/settings/presentation/main/general/NotificationSettingsViewModelTest.kt` | 330 | Notification setting intent 与权限 result 状态；同一通知页面 owner，保留。 |
| `app/src/main/java/com/aozijx/passly/app/observability/DiagnosticsExportEncoders.kt` | 324 | Diagnostics JSONL/TSV/ZIP encoder 与 shared export manifest/row mapping。 | 保留输出格式 encoder 边界；各格式 helper 共用相同 field redaction/schema，不新增转发 facade。 |
| `buildSrc/src/test/kotlin/passly/EditorSourceBoundaryVerifierTest.kt` | 323 | Editor mapper/use-case source boundary 规则测试；对应同一 verifier policy，保留。 |
| `security/src/test/java/com/aozijx/passly/security/autofill/inbox/AutofillInboxKeyProvisionerTest.kt` | 321 | Key provisioner 成功、已有 key、失败和清理合同；单一 provisioner owner，保留。 |
| `app/src/test/java/com/aozijx/passly/feature/vault/presentation/detail/DetailFaviconSessionTest.kt` | 319 | Favicon preparation/save/cancel/resource cleanup；对应一个 session owner，保留。 |
| `data/src/main/java/com/aozijx/passly/data/observability/EncryptedLogFileManager.kt` | 314 | Encrypted log segment file create/open/count/rotate/retention；文件/key 生命周期 owner，与 RecordStore 通过窄 manager API 交互，保留分层。 |
| `domain/src/test/kotlin/com/aozijx/passly/domain/entry/policy/EntryEditTest.kt` | 312 | Domain EntryEdit typed mutation variants and projection; 单一 sealed edit contract，保留。 |
| `app/src/main/java/com/aozijx/passly/feature/settings/presentation/main/diagnostics/DiagnosticsViewModel.kt` | 301 | Diagnostics query/filter/export action 状态 owner；IO/encoder 已委托服务，保留。 |

## 中型候选和长函数

- `DefaultAuthenticationManager.authenticateInternal()` 当前 208 行；它是一笔受 mutex 保护的认证事务。保留显式顺序与 credential wipe。没有新增 helper，因为当前阶段共享前置状态且尚无 manager 层行为测试；fatal 错误恢复由 Task 63 单独修复。
- `RoomBackupSnapshotRestorer.restore()` 当前 180 行；入口负责一个跨数据库与文件的原子恢复窗口，保留其协调职责，不重复 Task 65 的 file-journal 故障覆盖。
- `LegacyResponseFactory.buildResult()` 负责将 Android 请求和发现结果组装为协议结果；其候选、unlock、picker 分支已分为命名 helpers，保持在单一 Android Autofill 协议 owner。
- `EncryptedBackupContainerCodec.encrypt()`、`decrypt()`、`parse()` 分别负责 seal、open、header 解析；单一文件保持协议字段、长度限制、AAD 与 KDF 验证可一起审查。
- `BitwardenItem.toBackupRecords()` 按外部 item subtype 投影字段并生成关联 OTP/account records；不与内部 Entry mapper 合并，避免混淆外部 JSON 的字段与内部稳定 archive key。
- `DiagnosticsScreen()` 和 `AuthenticationScreen()` 是 UI 声明而非业务工作流；布局行数不构成新增 presentation owner 的理由。

### 约 80 行以上函数筛查

下列候选来自源码函数跨度扫描，再按真实代码体核对；跨度含格式化、嵌套 lambda 和布局声明，不能当作逻辑语句数。处理结论针对是否存在可以独立拥有状态、资源或失败合同的职责。

| 函数/区域 | 扫描跨度（约） | 审查结论 |
| --- | ---: | --- |
| `DetailViewModel.onAction()` | 392 行 | sealed action 的穷尽分派；子工作流已有 session/runtime/reducer owner。保持 action→现有 owner 的显式路由，不加转发层。恢复竞态已在 Task 58 的独立修复中处理。 |
| `DetailReducer.reduce()` | 310 行 | 根页面状态协调，favicon/tag/revision 子状态转换已有专属 reducer；保留根协调职责，未发现可迁移的独立 owner。 |
| `PrivacyPolicy.sanitize()` | 约 247 行 | 集中字段脱敏及递归值处理的安全策略目录；分散规则会削弱同一输入净化合同的审查，保留集中实现。 |
| `AuthenticationScreen()` | 约 235 行 | 纯 Compose 输入、错误和认证方式布局；无业务状态提交或 IO，私有内容函数已经按 UI 区域分组。 |
| `DefaultEntryFieldReader.getFieldValue()` | 约 226 行 | `FieldKey` 到 Domain 标量值的穷尽投影；保留 sealed/enum `when`，避免用 Map/反射失去新增字段编译提示。 |
| `DetailBody()`、`AutofillSettingsSection()` | 各约 220/212 行 | Compose 内容函数，接收不可变状态与窄回调；无额外生命周期或持久状态，不为布局块增加跨文件参数边界。 |
| `BitwardenItem.toBackupRecords()` | 约 201 行 | 外部 JSON item 到内部备份记录的格式转换，含 OTP/账户关联规则；维持 adapter 私有转换闭包。 |
| `TrashBottomSheet()`、`TagEditorSheet()` | 各约 190/186 行 | UI 内容与局部对话框/编辑状态；删除、保存等持久化行为由上层 owner 提供，继续保持内容边界。 |
| `VaultRoute()` | 约 185 行 | Compose navigation/lifecycle composition owner，包含 scanner overlay 与结果回传；不抽出隐藏 ViewModel/lifecycle 依赖的 helper。 |
| `RoomBackupSnapshotRestorer.restore()` | 约 180 行 | DB 与文件 journal 的一个 restore transaction 协调窗口；Task 65 已覆盖一致性失败行为，不额外复制 transaction/facade owner。 |
| `ScannerResultCard()`、`AddBankCardEditorScreen()` | 各约 181/179 行 | UI 展示/表单内容，action 通过窄接口交回 ViewModel；无混入业务 IO 的证据。 |
| `VaultGraphRegistration.registerVaultGraph()` | 约 165 行 | 集中导航图注册点，保留 destination/host wiring 单一入口，避免分散注册目录。 |
| `buildEntryPagingQuery()` | 约 157 行 | 查询谓词、排序和绑定参数组成一个 SQL query；同一 builder 负责语义与参数顺序，未发现独立 query owner。 |
| `EntryRevisionRestoreTransaction.restore()` | 约 146 行 | revision 写入、事务边界与结果映射属于同一原子数据操作；保留在 Data transaction owner。 |
| `SecuritySessionEventLogger.traceAuthentication()` | 约 138 行 | 认证 trace、异常优先级与脱敏字段属于同一安全日志事件合同；不拆成转发 logger。 |

扫描未发现需要本 Task 再抽出的独立职责。这里记录“保留”是本轮按现有边界审查后的结论，不表示这些实现没有其他待办；认证 fatal 映射、backup journal 覆盖及 Detail 测试套件归属仍由计划中的 Task 63/65/74 跟进。

## 后续工作边界

本轮只对职责归属和已定位的 Detail 恢复竞态作结论。Task 59/60/63/65/74 中已列的状态恢复、fatal Error、文件 journal 与测试结构工作保持其原 owner，不在本次重复实现。当前表是 2026-11-02 格式化后的筛查快照，代码变更后需重新统计；既有待办不因本表被视为已验证。
