# Passly Maintainability and Redundancy Audit

**Date:** 2026-10-29
**Scope:** Core, Domain, Runtime Session, Security, Data, App features, build logic, tests, and documentation.
**Method:** Classify candidates as remove, retain, or defer only after checking callers, DI, Android entry points, persistence, tests, and documentation.

## Review summary

The audit removed confirmed pass-through or unreferenced code, fixed reproduced cancellation and ownership defects, and updated affected topic documents. Similar names, declaration-only DI modules, test helpers, and state owners with distinct contracts were retained. Remaining device-only verification is tracked in the active verification plan.

## Detailed findings
### Baseline and rulings

- Baseline captured before implementation: 41 tracked paths already modified/deleted and 4 pre-existing untracked paths. The current revision-history/activity implementation, related docs, schema, tests, `AGENTS.md`, and related TODO edits are protected. No tracked or untracked user file has been reverted.
- Repository inventory: 7 Gradle modules and 1,003 production Kotlin files. Existing module docs explicitly prohibit no-op mappers/use cases and forwarding-only coordinators; use them as the decision rule rather than class names.
- Ruling: don't consolidate the two `TestLogger.kt` files. `core` provides only a no-op `Logger`; `app` adds a recording logger used by application behavior tests, and the Gradle setup does not expose the core test source as a fixture. The apparent duplicate has distinct test responsibilities; sharing it would add test-fixtures wiring without removing behavior.
- Ruling: keep both `ClipboardModule.kt` files. The `core` module binds the platform `SecureClipboard`; the app module binds separate Domain ports for controlled clipboard writes/cleanup. Names match, bindings do not.

### Core and Core Common

- Result: reviewed production source inventory, module dependencies, clipboard DI, test logger helpers, and shared error package. No production implementation met the confirmed-removal threshold.
- Similar filenames retained: `AddEntryTransition.kt` contains shared key/overflow constants while the editor package contains the transition modifier; these are consumed together and have distinct roles.
- Validation: read-only reference/registration audit; no source changed in these modules, so no module test was needed.
- Dependency aliases were reconciled during the build logic, dependency, test, and documentation review; no unused declaration was proven.

### Domain and Runtime Session

- `:domain`: retained the distinct field reader/editor, draft materializer, validators, revision comparator, and OTP/Wi-Fi policies; each has consumers with a separate domain rule or contract. No duplicate production source or clearly dead implementation was found in the targeted model/policy scan.
- Follow-up cross-module reference audit found stale, unreferenced domain pieces: `BackupSettings` and its private enum contract, `SessionPolicy`/`SessionPolicySource`, `StagedAttachment`, and the `CredentialCandidate` wrapper. Removed them; retained `CredentialMatch` and `MatchType`, which are consumed by Autofill.
- Attachment audit found `AttachmentRefMapper` and `EntryAttachment` had no callers; removed both. Retained `AttachmentStatus` because Room storage mapping and revision restoration use it, and moved it to `AttachmentStatus.kt` to keep the file name aligned with its surviving type.
- Core common audit found `NetworkError` had no callers; removed the unused exception model while retaining `NETWORK_ERROR`, which the shared UI error mapper still consumes.
- `:runtime:session` bug: `RuntimeSessionManager.unlock()` mapped a `CancellationException` from the suspending `SessionKeySource.copyKey()` to `KeyUnavailable`. Added a regression test; it failed before the fix. The first post-fix run exposed an overstrict exception-instance equality assertion because `withContext` may rewrap cancellation. The assertion now checks cancellation type and message.
- Follow-up bug: `SessionLeaseGate.open()` also converted cancellation from the suspending resource open into `OpenFailed`; `SessionLeaseGate.close()` swallowed cancellation while still discarding the handle. Both reproduced with focused regression tests.
- Fix: all three suspend boundaries rethrow `CancellationException` before mapping other `Throwable` values. Close still discards its handle in `finally`; state remains SEALED when open/close is cancelled. Updated `docs/architecture/runtime-flows.md` to document the key-copy wipe and cancellation contract.
- Verification: focused RED runs failed at the expected assertions; final `:runtime:session:test` — 9 tests passed after the fixes.
- Completed OTP key audit: `OtpConfig.toNewEntryDraft()` temporarily represents enum choices as text, and `EntryDraftMaterializer` exhaustively parses supported OTP keys. The resulting typed `OtpConfig` is persisted by explicit Data storage-key mappings; these text values are not an external format. No code change needed.
- Completed post-history review: the history feature is now committed in the current baseline. `EntryRevision` had no consumers beyond its declaration; the active repository uses `EntryRevisionMetadata` and `RedactedEntryRevision`, so the unused snapshot wrapper was removed. Retained `EntryRevisionId`, which is used by revision ports and UI actions.
- Validation: `:domain:test` passed after removing `EntryRevision`.

### Security

- Reviewed authentication manager exception handling, biometric candidate cleanup, KDF runner, session event tracing, and controller lock paths against current tests and `docs/development/kotlin-style.md` / `docs/security/authentication.md`.
- No dead or duplicate production implementation was proven; state restoration, durable candidate reconciliation, event tracing, and worker-secret cleanup have separate ownership.
- Fixed lock-cancellation bug: after acquiring the lock mutex, `VaultSessionController.lock()` now completes revocation, sensitive-key clear, session lock/seal, DEK wipe, and terminal-state publication inside `NonCancellable`; it then checks the original coroutine context so cancellation propagates to `SecuritySessionEventLogger` as `LockCancelled`. A regression test first failed because the DEK remained unlocked, then passed with assertions for wiped DEK, SEALED state, Locked auth state, and cancellation event.
- Verification: `:security:testDebugUnitTest` passed, including the new cancellation regression. `:app:testDebugUnitTest` and `:app:compileDebugKotlin` passed.
- Remaining security limitation: a real hardware biometric re-enrollment/Keystore invalidation event is still not verified by emulator or unit tests.
- Skill lesson: when cancellation meets security cleanup, keep cleanup complete and cancellation visible; test both resource wiping and terminal-state/event reporting.

### Data

- Reference audit found `AttachmentRefMapper` had no callers; Room reference rows are created/queried through DAOs and current restoration logic directly because it also handles the revision snapshot contract. Removed the unused mapper and its unused `EntryAttachment` domain model. Kept `AttachmentStatus`, which is used by stable storage mapping and restore logic.
- No schema, persistent key, transaction boundary, or encryption behavior changed.
- Verification after the dead-code removals: `:core:common:test` (NO-SOURCE), `:domain:test`, `:data:test`, and `:app:compileDebugKotlin` all succeeded. `:runtime:session:test` passed independently with 9 tests.
- The cross-project declaration scan found no other unreferenced Data declarations. No Room or storage mapping changes were needed; the current dirty `StorageEnumMappings.kt` and history transaction files remain protected.

### App

- Removed unused `AppErrorNoticeMapper`; all production error notices are selected by feature-specific results (for example, Backup's typed notice mapper). Updated `docs/architecture/error-handling.md` to remove the stale global mapping flow and obsolete `NetworkErrors.kt` path.
- Removed four unreferenced one-line argument constructors from `NoticeArgument.kt`; callers already construct typed `ArgumentValue` at their use site.
- Removed unreferenced `DetailOpenRequest`; retained `DetailLaunchMode`, which `DetailRoute` and `DetailViewModel` consume.
- Verification: `:app:testDebugUnitTest` and `:app:compileDebugKotlin` succeeded after the cleanup. Repository-wide search found no remaining references to removed symbols.
- The candidate sweep identified `ObservabilityModule`, `EntryPolicyModule`, and `BackupDispatchersModule` as declaration-only symbols; all are retained because Hilt installs them through `@Module` metadata. Existing `BackupNoticeMapper` has a live Route caller and tests. Protected detail history/activity files were not edited.

### Build logic, dependencies, tests, and documentation

- Reviewed all 68 `[libraries]` aliases in `gradle/libs.versions.toml` against Gradle/Kotlin sources; no unused alias was proven, so no dependency was removed.
- Reviewed `buildSrc` task registration: generator, verification, module boundary and metrics tasks are registered through the Gradle plugin and have distinct CLI/build/check responsibilities. No confirmed duplicate build task or implementation was removed.
- Updated `docs/architecture/error-handling.md` for current Feature-owned Notice selection and removed stale file/type references. Full docs-relative-link scan passed: `All relative Markdown links resolve.`
- Preserved the in-progress history and UI delivery work. The history Sheet bottom-space fix was committed separately in `1ddf8d7b`; the delivery record is now archived at [the history and UI review](2026-10-27-history-ui-delivery.md), and the remaining hardware check is tracked in the [Keystore verification plan](../superpowers/plans/2026-10-29-hardware-keystore-verification.md).

### Local agent rules and project skills

- Condensed `AGENTS.md` to stable rules and authoritative doc links; removed task-specific TODOs from the agent file.
- Created two project-local skills: `passly-maintenance` for agent/docs/TODO upkeep and durable lessons; `passly-redundancy-cleanup` for evidence-led code audits. The latter treats token counts as leads, checks platform/DI/persistence callers, and separates remove/retain/defer decisions.
- Incorporated the reproduced session cancellation defect and the unresolved lock-cleanup TODO into the skills without duplicating full architecture docs.
- Validation: skill-creator `quick_validate.py` reported `Skill is valid!` for both skill folders. `AGENTS.md` and `.agents/skills/` remain untracked and unstaged.

### Final review

- Renamed `CredentialCandidate.kt` to `CredentialMatch.kt` after removing the unused wrapper, so the source filename matches the remaining public types.
- `git diff --check` passed; Git only printed its expected LF-to-CRLF conversion notices for the current Windows checkout.
- The audit changes were later committed by exact path: session cancellation and its documentation in `f81108d3`, then unused contracts, mapper cleanup and error-handling documentation in `d79b5714`. `AGENTS.md` and `.agents/skills/` remain unstaged and uncommitted.
- Final verification after the filename change: `:domain:test` and `:app:compileDebugKotlin`; full affected module commands and session tests are recorded above. Android instrumentation/device behavior remains unverified.
- Self-review followed the repository cleanup evidence rules and the code-review checklist; no independent reviewer was dispatched.

## Follow-up findings and fixes

### 第二轮根因与桥接复审（2026-10-29）

- 静态候选扫描覆盖当前 1,298 个 Kotlin/Java 源文件，并复核广泛 `Throwable` 捕获、forwarder、跨 feature/Data 接口及 SourceBoundary 白名单。发现 `VaultDatabaseResetController.reset()` 在 `DatabaseSessionLifecycle.seal()` 的可挂起边界吞掉取消；取消发生在 seal 已开始后时，路径会跳过数据库/资源删除，直接尝试 unlock，导致重置被拆成半个状态转换。
- RED：`VaultDatabaseResetControllerTest.cancellationDuringSealFinishesResetAndReopensBeforePropagating` 失败，实际顺序为 `seal-start, unlock`，缺少 seal 完成和删除。
- Fix：控制器改为依赖已存在的 `DatabaseSessionLifecycle` 端口。操作启动前检查取消；一旦开始 seal，就在 `NonCancellable` 内完成封存、删除、重开与刷新，最后再传播调用方取消。重置失败与重开失败同时发生时保留两个异常。
- 删除 `VaultDataAdaptersTest`：它只验证两个无逻辑的 pass-through 转发及 Flow 身份，没有额外行为覆盖；同时删除为了该测试额外加入的 SourceBoundary 特例白名单。两个生产适配器保留在 App composition root，因为它们维持 feature 不依赖 Data 的边界；`DatabaseResetGatewayAdapter` 也保留，其负责线程切换和 Throwable 到 AppResult 的真实映射。
- 备份复审发现 `BackupOperationInteractor` 的导入/导出把 `CancellationException` 转成普通失败；导出取消时部分目标的清理又在已取消上下文执行，可能清理失败。改为保留取消，导出目标存在时使用不可取消清理，清理失败附在原取消异常后再传播。另将 SAF URI 字符串作为流程合同，Android `Uri` 只留在 UI 和存储实现边界；移除只消费一次的 `ExportTarget` 包装及未消费的文件名/tree URI 字段。
- 备份取消回归测试先红后绿，覆盖导出期间取消后的部分文档清理、导入取消传播，以及 storage `Result` 携带取消时的原样传播。纯转发测试/特例删除后无符号残留。
- 验证：重置 RED 如上；GREEN 定向回归通过。完整 `:data:testDebugUnitTest` 为 74 项通过、0 失败；`:buildSrc:test`、`verifyArchitecture`、`:app:compileDebugKotlin`、`:data:lintDebug` 和 `git diff --check` 均通过。备份编排定向单测通过。
- 续扫挂起边界：逐项复核认证管理器、恢复凭据草稿、锁定事件记录器、附件回收和 SAF 授权/可写检查中的宽泛捕获。安全路径在可挂起操作前显式传播取消；需要完成的密钥/资源清理由 `finally` 或 `NonCancellable` 承担；附件回收失败保留 `PENDING_GC` 供下次幂等重试。当前没有证据支持再删层或改异常映射。
- 保留的边界：`BackupStorageSupport` 内的 `runCatching` 包住的是同步 SAF 查询/创建；可挂起的存储入口在返回 `Result` 后由编排器显式恢复 `CancellationException`。`AndroidBackupDirectoryAccess.acquire/release` 对授权释放做尽力清理，释放失败不会撤销已经确定的业务状态。
- 最新验证：备份编排定向单测通过；`verifyArchitecture :app:lintDebug --no-parallel --max-workers=2` 通过（161 项动作，3 项执行）；`git diff --check` 通过。
- 继续追踪 SAF 授权调用方时发现 `BackupDirectorySettingsInteractor.persistGrantedDirectory()` 把“保存新 URI”和“释放旧授权”包在同一个异常范围中。已有代码先正确保存新值，但旧授权释放失败会进入补偿逻辑并释放新授权，形成偏好引用失效 grant 的根因。
- RED：新增 `failure releasing previous grant does not revoke newly saved directory`，旧实现失败，证明会返回 `Unavailable` 并尝试释放新 URI。
- Fix：将设置写入和旧授权回收分成两个阶段；设置写入失败时在 `NonCancellable` 尽力释放新授权，写入成功后在 `NonCancellable` 尽力释放旧授权但保留已提交的新 URI，再传播调用方取消。文档同步说明旧授权释放失败只会暂留多余授权，不破坏当前授权。
- GREEN：`:app:testDebugUnitTest --tests com.aozijx.passly.feature.settings.backup.BackupDirectorySettingsInteractorTest --no-parallel --max-workers=2` 通过。
- 同一合同也修正 `clear()`：改为先清偏好，再在 `NonCancellable` 内尽力释放已选 grant，随后传播调用方取消。新增清理顺序和释放失败回归；两项断言先红，修复后设置 Interactor 测试集通过。
- 后续补充“偏好已写入但挂起调用随后取消”的回归：旧补偿会释放已被新偏好引用的 grant，测试先红。修复后在不可取消区读取当前偏好，只有确认未引用新 URI 才回收新授权；读取失败时保留授权。设置 Interactor 定向 9 项、完整 App 868 项单测均通过。
- 最终验证：Data 74 项单测、`:buildSrc:test`、`verifyArchitecture`、App/Data Lint 和 `git diff --check` 均通过。数据库重置修复提交于 `3fa50221`，备份取消与授权所有权修复提交于 `26e26762`；无逻辑转发测试与对应 SourceBoundary 特例单独清理。
- HOTP 提交后复核发现 `RoomOtpConfigRepository` 和消费事务通过 `readAll()` 解密同一条目的全部敏感字段；用损坏 PASSWORD 行证明该无关数据会使 HOTP 失败。改为只读 OTP bundle 与 `OTP_SECRET`，回归先红后绿；API 36 Data 仪器测试 33 项、Data/App 单测、`verifyArchitecture`、Data lint 与 `git diff --check` 通过。
- API 36 AVD 复验再次发现 `AutofillLaunchTargetFactoryTest` 仍引用已在 `7877c569` 删除的 Legacy Save Activity/PendingIntent 工厂，导致 App 仪器测试编译失败。删除过期的 Legacy Save 启动断言，保留仍存在的 legacy fill、Credential Manager 与无障碍显式 Activity 验证；新保存路径由 `LegacySaveFlowTest` 覆盖。
- 该审计检查点：`Pixel_8_Pro` API 36 AVD 的 Data 仪器测试 33 项、App 仪器测试 56 项通过；API 31 模拟器镜像当时未安装。后续 API 31 真机复验已在[交付审查](2026-10-27-history-ui-delivery.md)记录通过；目前仅硬件 Keystore 生物识别录入变更仍待验收，见[活动计划](../superpowers/plans/2026-10-29-hardware-keystore-verification.md)。
- 安全会话复审发现 `VaultSessionController.lock()` 和恢复回滚路径捕获 `sessionManager.softLock()/seal()` 的 `CancellationException` 后会当作普通封存失败记录并吞掉。新增封存与软锁取消回归，旧实现先红；改为在 NonCancellable 区完成钥匙清理、锁状态和 Locked 发布，再传播取消，由 Trace 记为 `LockCancelled`。更新认证文档明确内部取消合同。
- 验证：`VaultSessionIdleTimeoutTest`、完整 `:security:testDebugUnitTest`、`:app:testDebugUnitTest`、`verifyArchitecture`、`:security:lintDebug` 与 `git diff --check` 均通过。

### 后续批次：诊断导出取消边界（2026-10-29）

- 生产路径核对：`ObservabilityExportService.export()` 通过 `withContext(IO)` 执行同步日志读取与编码；内部 `runCatching` 会吞掉依赖抛出的 `CancellationException`，分享与本地文档分支也会将取消转换为 `Result.failure`。同步操作不会自动响应已取消 Job，若不在阶段边界检查，取消后仍可能继续打开文档或启动分享。
- RED：新增日志读取抛取消、分享阶段抛取消、本地文档写入抛取消三项回归；旧实现全部返回普通失败。另新增在 flush 与临时文件创建期间真实取消 Job 的回归，证明旧实现会继续导出/分享。
- 修复：独立传播取消；在授权、flush、读取、临时输出创建与编码之后检查 Job；取消时尽力删除临时明文或 SAF 文档目标，清理异常作为 suppressed 保留原始结果。
- 文档：更新诊断导出主题页，说明同步编码中的取消会在返回后检查，并明确清理和传播合同。
- 验证：`:app:testDebugUnitTest --tests com.aozijx.passly.app.observability.ObservabilityExportServiceTest` 定向通过；`:app:testDebugUnitTest verifyArchitecture :app:lintDebug --no-parallel --max-workers=2` 通过（App 单测 862 项）；`git diff --check` 通过。

### 后续批次：分享导出保留期限（2026-10-29）

- 生产路径核对：分享文件成功后安排 10 分钟删除，但下一次分享的 `clearStaleExports()` 原先删除所有 `passly_logs_` 文件，不检查年龄；连续发起第二次分享会移除仍有效的第一次分享文件。
- RED：旧式“清理所有同前缀文件”使保留窗口测试失败；测试同时确认不匹配前缀的文件不受影响。近期文件和超期文件按精确边界选取。
- 修复：抽出纯文件保留策略，清理只选择同前缀、普通文件且修改时间至少超过 10 分钟的导出；系统时间回拨时保守保留。
- 文档：明确下次分享只清理超过保留期的导出，仍在分享窗口内的文件保持可读。
- 验证：保留策略两项单测通过；`:app:testDebugUnitTest verifyArchitecture :app:lintDebug --no-parallel --max-workers=2` 通过，`git diff --check` 通过。

### 后续批次：无障碍启动取消清理（2026-10-29）

- 生产路径核对：`ManualAccessibilityFillService.requestFill()` 在 `withContext(Main)` 外捕获所有 `Exception`，会把 `CancellationException` 当普通启动失败记录并吞掉。
- RED：在协程已运行并挂起后取消，旧式宽泛捕获使取消清理用例失败；断言 trace continuation 与暂存请求均需恰好清理一次，普通启动错误则需清理并记录。
- 修复：取消路径先清除 trace 与单次会话请求，再重抛原始取消；清理异常仅作为 suppressed 保留，不覆盖原始异常。普通异常继续记录为启动失败。
- 文档：补充无障碍服务取消时清理短生命周期状态并传播取消的合同。
- 验证：`AccessibilityFillLaunchTest` 定向通过；`verifyArchitecture` 与 `:app:lintDebug` 通过，`git diff --check` 通过。

### 后续批次：无障碍请求启动前取消（2026-10-29）

- 生产路径复核发现：`sessions.capture()` 在启动协程前写入短时请求；若 Service 随即销毁，协程可能在执行第一行前就被取消，协程体内的异常清理器不会运行。此前 `withRootTrace` 也位于清理器之外。
- RED：新增“启动函数体尚未运行即取消”回归，旧实现完成后请求清理和 trace 丢弃计数均为 0。
- 修复：启动助手现在持有并返回 Job，在任务取消完成回调中幂等清理；协程体运行后仍由内层捕获取消并重抛。把 trace 初始化、开始日志与主线程 Activity 启动一并纳入该边界。
- 文档：明确协程启动前及执行期间取消均清除暂存请求与 continuation。
- 验证：启动前取消、运行中取消和普通启动失败三项定向用例、架构检查、Lint 与 `git diff --check` 通过。

### 后续批次：详情图标裁剪取消清理（2026-10-29）

- 生产路径核对：`DetailViewModel.CancelFaviconCrop` 先清除草稿中的 pending path，再由 `DetailFaviconSession.cancelCrop()` 在 ViewModel scope 启动可取消 IO 删除；导航销毁可能在删除完成前取消任务。`FaviconImageStore` 只在进程启动时清理 staging，因此文件会滞留到下次启动。
- RED：新增在删除已挂起期间取消 owner scope 的回归；旧实现因任务未进入或被取消而没有删除该路径。
- 修复：清理从调用点立即开始，并在 `NonCancellable` 内完成文件删除；页面生命周期不会中断已开始的临时资源回收。
- 文档：补充裁剪取消时的资源释放合同及进程启动 staging 清理的兜底边界。
- 验证：`DetailFaviconSessionTest` 定向测试、`verifyArchitecture`、`:app:lintDebug` 与 `git diff --check` 通过。

### 后续批次：Legacy Autofill 服务销毁清理（2026-10-29）

- 生产路径核对：`onDisconnected()` 把活动交互过期排入 service scope；服务快速销毁时 `onDestroy()` 取消该 scope，清理协程可能在开始前就被取消，活动交互及解锁租约只能等待超时回收。
- RED：新增 scope 在清理任务调度前被取消的回归；普通 `scope.launch` 实现未运行清理体，断言失败。
- 修复：生命周期回调使用 `UNDISPATCHED` 立即进入清理，再在 `NonCancellable + IO` 段过期 Legacy 来源活动交互；销毁时先触发清理再取消普通请求 scope。重复调用由交互终态转换保证幂等。
- 文档：说明断连/销毁时的活动交互过期和租约释放。
- 验证：`LegacyInteractionExpiryTest` 定向测试通过；`verifyArchitecture`、`:app:lintDebug` 与 `git diff --check` 通过。

### 后续批次：详情保存异常恢复（2026-10-29）

- 生产路径核对：`DetailViewModel.persistEntryEdit()` 先发布 Saving，再调用 `DetailWorkSession.persist()`；该协调器只处理 `AppResult.Failure`。`EditDetailEntryUseCase` 对意外存储异常会记录后重抛，而 presentation 刷新也可能因数据库/平台查询异常失败；两个异常路径均无 SaveFailed，界面会永久保持 Saving。
- RED：新增意外写入异常用例，旧实现直接抛出并且不发出失败状态；再覆盖写入已返回成功、展示刷新失败的路径。
- 修复：在保存协调器中将非取消异常映射为不暴露内部信息的 `UNEXPECTED_FAILURE`，仅当原访问凭据和协程仍有效时发出 SaveFailed；CancellationException 继续传播。
- 文档：说明可重试失败与异常可能发生在数据库提交后的边界，重新进入详情会按持久化真相源加载。
- 验证：`DetailWorkSessionTest` 定向测试、`verifyArchitecture`、`:app:lintDebug` 与 `git diff --check` 通过。

### 后续批次：HOTP 旧会话结果丢弃（2026-10-29）

- 生产路径核对：详情页在授权闭包中调用 `OtpCodeRuntime.codeForCopy()`；HOTP 消费会挂起等待 Data 事务。页面关闭、切换条目或锁定会清空运行时令牌，但 `generateHotpCode()` 发现旧令牌失效后仍返回成功结果，授权闭包会继续把旧码写入剪贴板。
- RED：新增事务挂起期间关闭运行时的回归，旧实现仍返回验证码且测试失败。
- 修复：消费事务返回后令牌失效即返回失败结果，不发布或交付验证码；已完成的 HOTP 数据事务不回滚，计数器保持已推进。
- 文档：说明关闭/切换/锁定期间已消费但未交付的验证码会丢弃，用户重新授权即可继续。
- 验证：`OtpCodeRuntimeTest` 定向测试、`verifyArchitecture`、`:app:lintDebug` 与 `git diff --check` 通过。

### 后续批次：详情复制绑定访问会话（2026-10-29）

- 生产路径核对：`CopyEntryFieldUseCase` 在授权闭包内读取公开字段或解密单字段并直接调用剪贴板；`DetailViewModel.copyField()` 只在复制返回后检查访问令牌。页面关闭/切换发生在查询或解密挂起期间时，旧访问仍能写剪贴板。剪贴板控制器还会先挂起读取保留策略，因此仅在仓库读取后核对仍留有窗口。
- RED：两项延迟查询/解密回归在旧逻辑下失败；控制器延迟保留策略用例在旧式先检查再挂起实现下失败。
- 修复：详情复制用例把令牌和调用协程 Job 绑定到最终写入；Clipboard writer 增加条件写入合同，Android 控制器在读取策略后、同步写入前重新核对。OTP 复制也使用同一条件写入边界。
- 文档：明确详情复制的访问令牌检查点与剪贴板写入器条件合同。
- 验证：`CopyEntryFieldUseCaseTest` 与 `ClipboardCopyControllerTest` 共 12 项定向测试、`verifyArchitecture`、`:app:lintDebug` 与 `git diff --check` 通过。

### 后续批次：恢复码与扫码复制的会话归属（2026-10-29）

- 生产路径核对：`RecoveryCodeDraftInteractor.copy()` 在保留策略读取挂起期间不再确认草稿仍活动；`ScannerViewModel.copyText()` 的工作作用域只过滤旧完成提示，不能阻止旧文本继续写剪贴板。
- RED：恢复码草稿在条件写入等待期间关闭、Scanner 在非取消写入等待期间重扫，两项旧路径回归均失败。
- 修复：恢复码复制使用活动草稿对象身份作为条件写入守卫；Scanner 使用当前扫描工作对象守卫。共同由 Android ClipboardCopyController 在策略读取后重新校验，再执行同步平台写入。
- 文档：更新认证与 Scanner 对复制所有权的边界描述。
- 验证：`RecoveryCodeDraftInteractorTest` 与 `ScannerViewModelTest` 定向测试通过；`verifyArchitecture`、`:app:lintDebug` 与 `git diff --check` 通过。

### 后续批次：诊断分享清理调度失败（2026-10-29）

- 生产路径核对：分享 Intent 启动成功后，`scheduleDeletion()` 若抛出异常会落入整体失败处理并立即删除接收方仍需读取的文件；Android `Handler.postDelayed()` 还可能直接返回 false 表示任务未入队。
- RED：模拟成功分享后清理调度异常，旧实现返回失败且删除已分享文件。
- 修复：存储边界返回主线程删除任务是否入队；调度失败或抛错时保留文件，将其交给超过 10 分钟的陈旧文件清理兜底。分享成功状态不再被后置清理失败反转。
- 文档：明确延迟删除未入队时保留接收文件，并使用年龄清理兜底。
- 验证：`ObservabilityExportServiceTest` 14 项通过，含任务拒绝与调度异常；`verifyArchitecture`、`:app:lintDebug` 与 `git diff --check` 通过。

### 后续批次：敏感剪贴板发布与过期竞态（2026-10-29）

- 生产路径核对：剪贴板发布、计时任务取消/替换原先是三个独立操作；计时器先校验 token 再清剪贴板也没有与新复制串行化，两个顺序都允许新旧 owner/timer 不匹配。
- 回归覆盖：新增交错复制与旧计时器所有权检查期间发生新复制的屏障测试，验证替换操作互斥、旧回调完成后新内容及其计时任务仍生效。
- 修复：`ClipboardClearScheduler.replace()` 在同一监视器内执行平台发布和计时替换；计时回调只在仍是活动 Job 时，在同一监视器内完成 token 校验/清除。主动清除也在该边界中完成，继续返回既有清除结果。
- 文档：记录发布、计时替换和所有权清除共用串行边界。
- 验证：`:core:testDebugUnitTest`、`verifyArchitecture`、`:core:lintDebug` 与 `git diff --check` 通过。

### 后续批次：移除剪贴板调度的非原子入口（2026-10-29）

- 复核：`ClipboardClearScheduler.schedule()` 可绕过 `replace()` 的“发布 clip + 替换计时器”原子边界；当前生产调用已全部使用 `replace()`。
- 修复：删除多余的公开调度入口，现有调度操作只能通过原子替换进入；顺序计时测试改为覆盖该合同。
- 验证：`ClipboardClearSchedulerTest` 5 项、`:core:testDebugUnitTest`、`verifyArchitecture`、`:core:lintDebug` 与 `git diff --check` 通过。

### 后续批次：过时测试与 UI 测试核对（2026-10-29）

- 范围：核对各模块测试对本轮审计已移除生产符号的引用，并检查代表性的 Compose 仪器测试是否仍绑定当前页面、组件与 Android 入口。
- 结果：没有发现测试继续引用 `AppErrorNoticeMapper`、`RecordAutofillUsageUseCase`、`DetailOpenRequest`、已移除的 Domain 模型/Mapper、`VaultDataAdaptersTest` 或已移除的 Legacy Save 启动入口；这些历史失配项已在对应实现批次中清理。
- 保留：`AutofillLaunchTargetFactoryTest` 仍验证当前 Legacy Fill、Credential Manager 与 Accessibility 显式启动目标，生产目标类及 Manifest 注册仍存在。`RevisionHistorySheetTest`、`InternalInputAutofillTest`、`EntryFocusBehaviorTest`、`SensitiveFieldVisibilityTest`、`VaultTopBarMenuTest` 与 `SegmentedSettingsGroupDropdownTest` 均直接覆盖当前生产 UI/API 的可见行为，没有发现测试替身取代生产节点或已删除交互的证据。
- 决策：本轮只做只读核对，不删除有效覆盖；静态关键字/符号检查不能证明断言与运行时行为失配，因此未把“名称含 legacy”、测试文件名不等于生产文件名或断言风格当作删除依据。
- 验证边界：未运行单测、仪器测试或构建；此项是引用与生产声明的静态审查。后续如发现具体失败/失配用例，应以当前生产合同复现后再移除或改写。

### 后续批次：合并重复的新增条目保存能力测试（2026-10-29）

- 证据：密码条目和 OTP 编辑器分别在各自 ViewModel 测试中重复断言同一 UI 合同：有效表单在 Idle 可保存，Saving 时不可保存。两个状态各自实现该投影，但无需在两个长 ViewModel 测试套件中重复保存阶段断言。
- 处理：合并到 `AddEntrySaveCapabilityTest`，同一测试并列覆盖两个表单状态；删除两个原测试位置的重复用例。表单有效性及 ViewModel 工作流测试仍留在各自套件。
- 验证：未运行测试；`git diff --check` 通过。

### 后续批次：Autofill 使用记录转发层移除（2026-10-29）

- 生产路径核对：`RecordAutofillUsageUseCase` 只把条目 ID 转成 String 并固定 `ActivityType.AUTOFILL` 后转发至 Domain `ActivityRecorder`；唯一调用方 `CompleteAutofillSelection` 已拥有“完成点选并记录使用”的应用职责。
- 决策：移除 forwarding-only 用例；`CompleteAutofillSelection` 直接依赖 Domain 端口并显式传入 Autofill 类型。Room/Data 映射与活动记录规则仍留在原有 Domain/Data 边界。
- 测试：保留选择完成、访问租约释放和 Trace 生命周期覆盖；补充断言记录类型为 `AUTOFILL`。
- 验证：`CompleteAutofillSelectionTest`、`AutofillResultDeliveryTest`、`AutofillTraceLifecycleTest` 定向通过；`verifyArchitecture`、`:app:lintDebug` 与 `git diff --check` 通过。

### 后续批次：Autofill Service 归属边界核验（2026-10-29）

- 证据：Android Manifest 声明三个 Autofill Service。Accessibility Service 管理浮动按钮、系统回调与投递；Credential Provider Service 处理 Credential Manager 的创建/清除回调和取消；Legacy Autofill Service 解析 Fill/Save 请求、调用候选发现和保存流程并完成系统回调。
- 决策：保留在 `feature/autofill`。这些类是 feature 专属 Android 入口，包含真实生命周期与平台协议职责；移入通用 `service` 包会新增转发边界，不会减少平台实现。
- 文档：包边界允许单一业务流程紧耦合的 Service 实现归属对应 feature。
- 验证：核对 Manifest 三项声明、生产 Service 实现与 `git diff --check`；未改运行代码。

### 后续批次：Credential Manager 清除请求的服务取消边界（2026-10-29）

- 生产路径核对：`onClearCredentialStateRequest()` 将 `expireAll(CREDENTIAL_MANAGER)` 放在 Service scope 普通 `launch`；取消信号或 Service 销毁可能在协程体启动前取消它，未释放的交互/临时 unlock lease 只能等进程级超时。Legacy 的同类生命周期清理已使用立即启动的不可取消清理。
- 生命周期边界：不在 Modern Credential Service `onDestroy()` 过期全部交互，因为系统返回的 CredentialEntry PendingIntent 可能在 Service 销毁/重建后才被用户选择；由进程级 60 秒超时回收放弃的交互。
- 修复：抽取 Autofill 级服务清理 helper，Legacy 断连/销毁与 Credential Manager clear-state 共用；清理立即进入 `NonCancellable`，请求/父 scope 取消后仍完成，成功回调只在清理完成且请求未取消时发送。
- 文档：明确 clear-state 清理取消合同及 PendingIntent 跨 Service 生命周期的时限边界。
- RED：新增父 scope 在清理挂起期间取消的回归；首轮发现清理虽完成但取消后的成功回调仍执行，加入 `ensureActive()` 检查后回归通过。
- 验证：`AutofillServiceCleanupTest` 与 `AutofillTraceLifecycleTest` 共 5 项通过；`verifyArchitecture`、单 worker `:app:lintDebug` 与 `git diff --check` 通过。

### 后续批次：数据库重置与认证会话协调（2026-10-29）

- 根因：数据库重置已封存并删除文件后，重开失败会由 Data 控制器返回错误，但若认证状态只看 Security 锁状态，仍可能报告 Vault 已解锁；活动 Room 查询也只看认证授权，无法随数据库会话单独封存而停止。
- 修复：新增 Domain `DatabaseResetSessionCoordinator` 合同，由 Security 会话协调器与锁/解锁共用互斥区。重置成功后按数据库会话状态恢复；重开失败时保留错误、撤销授权、清除敏感数据密钥并发布 Locked。`isUnlocked()` 与 `isLocked()` 同时反映数据库会话状态。
- 查询：统一用授权状态和数据库 `UNLOCKED` 状态派生可读 Flow，并用于活动、关系、修订、列表及分页查询。封存时取消 Room 观察并发出空值，成功重开后重新订阅。
- 回归：Data 覆盖删除完成后重开失败的错误传播、封存状态和不发布刷新；Security 覆盖失败后锁定与重试、重置期间锁定请求等待；Data 共享投影测试覆盖授权及封存/重开转换。
- 文档：同步更新数据库重置、会话和活动查询边界。
- 验证：完整 `:data:testDebugUnitTest`、`:security:testDebugUnitTest`、`verifyArchitecture` 与 `git diff --check` 通过。并发批量执行时 Data 测试曾一次以 `EOFException` 退出；单独重跑完整 Data 套件通过。
- 限制：未运行基于真实 Room 实例的活动 collector 集成测试；当前通过共享可读 Flow 单测和各 Repository 的生产接线覆盖其门控合同。

### 后续批次：备份目录授权操作串行化（2026-10-29）

- 根因：目录选择在挂起取得新 SAF grant 前快照旧 URI；并发选择可能各自覆盖并释放同一个旧 grant，选择与清除交错时清除还可能依据 UI 旧快照释放错误 URI。
- 修复：`BackupDirectorySettingsInteractor` 在一个互斥区内执行 grant 获取、设置写入和旧 grant 回收；清除也在同一互斥区中读取最新持久化 URI，再清设置并释放实际被移除的 URI。ViewModel 不再把 UI 快照作为清除参数。Android SAF grant 与设置 Repository 的边界保持不变。
- 回归：新增 deferred grant 屏障测试，覆盖 select/select 与 select/clear 顺序，并断言每个被替换或清除的 URI 正确释放。
- 文档：同步更新备份 SAF 目录授权合同。
- 验证：`BackupDirectorySettingsInteractorTest` 13 项通过；`verifyArchitecture` 与 `git diff --check` 通过。

### 后续批次：恢复码提交、Room 观察与诊断日志清理（2026-10-30）

- 恢复码提交：为 `RecoveryCodeSettingsViewModel` 增加同步进入的 `Committing` 状态，阻止并发确认、草稿替换和关闭；ViewModel 回归覆盖提交暂停、重复确认与终态不被覆盖。完整 App 单测、Security 定向测试和 `verifyArchitecture` 通过。
- Room 观察：新增 `AppDatabaseSession.observeWhenReadable` 并迁移活动、链接、修订、列表和分页查询，移除各仓库重复的授权/数据库可读门控。真实 SQLCipher `AppDatabaseSession` collector 仪器测试覆盖封存、空结果和重开重订阅；35 项 Data 仪器测试通过，另有完整 Data 单测与架构门禁通过。此结果关闭本审查早期“缺少真实 Room collector 测试”的限制。
- 诊断日志清除：`LogRecordStore.clear()` 通过 `LogClearResult` 明确表示成功或失败；加密 store 在写入接受边界内等待普通和 emergency writer 排空，超时保留原文件并报告失败，成功删除后再接受清除边界之后的写入。Data 写入屏障回归及 Storage Settings、Diagnostics 两个 ViewModel 的清理失败状态测试通过。完整 Data/App 单测、`verifyArchitecture`、Data/App lint 与 `git diff --check` 通过。
- 诊断日志等级：新增持久化采集等级设置，写入过滤留在 `LogRecordStore` 边界并等待 DataStore 初始策略加载；设置只作用于本地加密记录，不影响 `LocalAlertPolicy`、Trace 或崩溃 handler。移除独立设置目的地，将设置放入 Data Management 日志区；完成 Proto 映射、策略、Storage Settings 和导航测试。Data/App 单测、`verifyArchitecture`、`:app:lintDebug` 通过。

### 后续批次：Detail 修订恢复取消竞态（2026-10-30）

- 根因：恢复事务已提交、仓储提交后清理仍挂起时，修订历史 Back 或 sheet dismiss 会取消 ViewModel job；恢复数据已经落库，但成功状态和详情刷新来不及执行。
- 修复：Reducer 在 Restoring 时拒绝 Back/Closed；ViewModel 的 Back、Dismiss 和 revision 切换不再取消活动恢复；Compose sheet 禁用 Back 并拒绝关闭/下滑隐藏。
- 覆盖：`DetailViewModelTest` 通过非取消清理屏障验证恢复完成后的详情刷新，以及条目替换、敏感会话关闭后的迟到结果不写回。`RevisionHistorySheetTest` 在 Pixel 8 Pro API 36 上验证 Back 和拖动关闭；5 项 reducer 测试、完整 App 单测、`verifyArchitecture`、`:app:lintDebug`、AndroidTest 编译和定向仪器测试通过。`git diff --check` 通过。
- 结构审查：没有把 revision/favicon 工作流搬到新的 callback bag 或第二份状态源；现有 reducer/store/session 继续各自拥有纯状态、敏感揭示值和资源清理。

### 后续复核：图标回收漏掉数据库单独重开（2026-10-30）

- RED：数据库处于 `SEALED` 时图标回收请求被保留；随后数据库单独重开为 `UNLOCKED`、认证状态保持 `Authenticated`，旧队列不会收到重试信号，断言失败。
- 修复：`IconResourceCleanupQueue` 同时监听认证状态与数据库会话 `lockStateFlow`；封存期间访问被拒后，数据库重开事件会触发再次 drain。`EntryResourceCleaner` 直接连接现有 `AppDatabaseSession`，未增加 Domain 契约或转发层。
- 验证：`IconResourceCleanupQueueTest` 全部通过；完整 `:data:testDebugUnitTest`、`:data:lintDebug`、`:app:compileDebugKotlin`、`verifyArchitecture` 通过。

### 后续复核：恢复码验证中的迟到输入事件（2026-10-30）

- 根因：验证时 Compose 输入框已禁用，但 ViewModel 仍无条件接受 `VerificationInputChanged`。迟到事件会擦除正在验证的 `OwnedChars` 并替换 UI 输入；挂起的验证结束后仍可能把旧结果写入已变化的验证状态。
- RED：增加 deferred 验证测试，验证进行中发送输入变化事件，旧实现会替换当前状态，断言失败。
- 修复：ViewModel 在 `Verifying` 状态拒绝输入变化事件；现有单一状态持有者继续管理敏感输入与验证结果，没有增加策略层或状态副本。
- 文档：认证主题页明确验证期间禁用输入及 ViewModel 对迟到事件的防护。
- 验证：`RecoveryCodeSettingsViewModelTest` 定向测试先失败后通过；完整 `:app:testDebugUnitTest` 通过。

### 后续复核：资源创建后取消导致会话句柄泄漏（2026-10-30）

- RED：`SessionResource.open()` 创建句柄后取消当前 Job；旧路径未检查资源返回边界的协程状态，外层 `withContext(IO)` 最后抛出取消，但句柄仍保持打开。
- 修复：资源返回后与状态发布后检查 Job；失败时在 `NonCancellable` 中关闭新句柄、撤销仍由本次 open 持有的句柄并保持 SEALED，清理异常保留为原始取消的 suppressed 异常。
- 验证：新增 `RuntimeSessionManagerTest` 回归先因资源未关闭而失败；修复后 `:runtime:session:test` 与 `verifyArchitecture` 通过，提交 `4f093f71b`。

### 后续复核：诊断复制绕过敏感剪贴板策略（2026-10-30）

- 根因：`DiagnosticsRoute` 自行调用 `ClipboardManager.setPrimaryClip()` 并设置 Android 敏感标记，但没有经过 `SensitiveClipboardWriter`，所以绕过隐私设置的自动清理策略；缓冲的复制 effect 也可能在页面离开后才被消费。
- RED：更新 ViewModel 回归要求逐行/Trace 复制交给共享端口，并覆盖挂起写入在页面退出后的所有权校验；旧 ViewModel 缺少剪贴板端口与页面退出动作，测试编译失败。
- 修复：`DiagnosticsViewModel` 直接通过 `writeSensitiveIf()` 发布，页面退出取消刷新和复制任务并使所有权 token 失效；删除 `CopyText` effect 与 Route 的直接剪贴板桥接。
- 文档：诊断查看页明确敏感标记、用户保留策略和页面退出取消行为。
- 验证：`DiagnosticsViewModelTest` 定向通过；完整 `:app:testDebugUnitTest` 896 项、`verifyArchitecture`、`:app:lintDebug`、`:app:compileDebugAndroidTestKotlin` 与 `git diff --check` 均通过。

### 后续复核：App Password 请求归属与单飞（2026-10-31）

- 根因：页面退出后的迟到 `SubmitChange` 会在 ViewModel 因无 owner 而提前返回，绕过 Interactor 唯一的 `finally` 清理；设置、修改和禁用请求也没有共同的进行中状态，重复设置可能并发执行 KDF 和凭据配置。
- 修复：ViewModel 无 owner 或已有操作时立即清零新请求；操作开始前同步占用单飞状态，并在协程完成回调中再次清理缓冲区、恢复忙状态，覆盖协程未启动即被取消。Route 将忙状态传入弹窗，操作期间禁用编辑、确认、取消和动作选择；旧 owner 的完成结果仍由 `PageEffects` 过滤。
- 回归：覆盖离页后迟到请求、ViewModel 启动前销毁、重复设置请求、页面重新进入时旧结果隔离及对话框忙碌事件拒绝。
- 验证：App Password ViewModel/协调器定向单测、完整 `:app:testDebugUnitTest`、`verifyArchitecture` 与 `git diff --check` 通过。

### 后续批次：移除未使用的 DEK 回调和全量清除链（2026-10-31）

- 根因：`DekManager.setLockCallback()` 没有注册方；`deleteVault()` 与 `DekState.Deleting` 无外部调用者，并成为 `VaultBootstrapStore.clear()`、Inbox 全 root `clear()` 的唯一入口。该入口不是当前数据库重置流程。
- 清理：移除上述 dormant API、Data/Domain 清除端口与实现及测试替身。保留 `VaultSessionController` 锁定、`DatabaseResetController` 数据库重置、Inbox pending/quarantine 生命周期、bootstrap Proto 字段和独立的 Inbox key-material 清理。旧删除事件键保留在 EventCatalog 供历史日志识别，移除不再可调用的 LogStatements 生产器。
- 文档：密钥管理、数据库和 Sealed Inbox 主题页明确当前没有完整 Vault 删除操作；永久数据库重置清理 SQLCipher 数据库和托管资源，但保留 bootstrap、Inbox key material 与 Inbox 文件队列。
- 验证：`:core:test`、`:domain:test`、`:data:testDebugUnitTest`、`:security:testDebugUnitTest` 与 `verifyArchitecture` 通过；`git diff --check` 通过。

### 后续批次：收窄 Vault 展示配置投影（2026-10-31）

- 根因：`entryHierarchyDisplayMode` 同时被 `VaultViewModel` 用于实际列表查询，又被 `VaultDisplayViewModel` 复制到只读取卡片呈现配置的样式对象中；重复投影导致与 UI 无关的层级变化触发 Ready 状态变化。
- 修复：从 `VaultStyleConfig` 和 `VaultDisplayViewModel` 映射移除层级字段，层级偏好继续由列表查询唯一消费。
- 回归：新增投影测试，切换层级模式时断言 Vault 展示配置不变；预期旧实现失败。
- 文档：列表与搜索页说明层级偏好和展示配置的单一消费边界。
- 验证：`VaultDisplayViewModelTest` 新增用例在旧实现上失败、收窄投影后通过；完整 `:app:testDebugUnitTest`、`verifyArchitecture` 与 `git diff --check` 通过。

### 后续批次：将 Autofill Inbox 队列契约归入 Domain（2026-10-31）

- 根因：`AutofillInboxQueue` 只接受和返回 Domain 模型，却声明在 Security 包，导致 App Feature 直接依赖 Security 命名空间。
- 修复：将接口移至 `domain.autofill.port`；`LegacySaveFlow`、`CredentialManagerFlow` 与 Security 实现均依赖 Domain port，Security 认证 Hilt 模块继续绑定具体 writer。Legacy/Credential Manager 各自的开关、来源、结果及取消处理未改。
- 文档：保存流程、Sealed Inbox 和维护边界页标明能力端口与加密/耐久实现的归属。
- 验证：`:domain:test`、`:security:testDebugUnitTest`、`:app:testDebugUnitTest`、`verifyArchitecture` 与 `git diff --check` 通过；全仓检索未发现旧 Security 契约引用。

### 后续评估：保留 Detail 修订历史所有权（2026-10-31）

- 评估：修订历史的观察、比较、授权揭示、恢复状态和敏感值清理都绑定当前 `DetailWorkSession` 条目令牌；认证撤权或切换 Entry 会同步关闭根详情状态。恢复事务成功后还需通过 `DetailPresentationLoader` 重载根投影。
- 结论：不拆成独立 ViewModel。新所有者仍需父 ViewModel 提供访问/条目 token，并回调恢复后的根投影刷新；这会增加状态同步和桥接，不能形成独立生命周期。现有 `DetailRevisionReducer` 已单独承担纯状态转换。
- 文档：修订历史主题页记录保留当前所有者的生命周期依据和拆分条件。
- 验证：本项为只读边界评估和文档更新；未改变运行时代码。

### 后续复核：Trash 命令异常与取消处理（2026-10-31）

- 根因：`TrashViewModel` 的恢复/永久删除/清空命令只在 `finally` 复位忙状态；`TrashCommandUseCase` 的 Repository 或授权端口抛出的非取消异常会逃出协程，不映射到页面失败状态。
- 修复：用一个私有操作执行函数共用“同步占用 → 执行 → 异常映射 → 释放”生命周期；各操作保留独立失败文案，取消原样传播。
- 回归：覆盖 Repository 异常、授权异常后可重试，以及执行中销毁和命令取消不变成失败提示。
- 文档：Vault 列表主题页记录失败映射、忙碌复位和取消合同。
- 验证：`TrashViewModelTest` 与 `TrashCommandUseCaseTest` 定向测试通过；完整 `:app:testDebugUnitTest`、`verifyArchitecture` 和 `git diff --check` 通过。
