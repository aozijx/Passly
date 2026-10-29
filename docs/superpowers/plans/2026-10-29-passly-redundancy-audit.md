# Passly Redundancy Audit Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** Review every Passly module, remove only proven redundant implementation, record deferred findings, and improve the local agent and project maintenance skill from concrete findings.

**Architecture:** Audit production code against its callers, DI registrations, Android entry points, serialization/resource references, tests, and formal docs. Record each candidate as remove, retain, or defer; perform only evidence-backed removals, then update the authoritative topic docs and the local maintenance workflow.

**Tech Stack:** Kotlin, Android, Gradle, Room, Hilt, Compose, Markdown, Codex project skills.

**Request:** The user's repository-wide review and maintenance request in this task.

## Global Constraints

- Preserve all pre-existing worktree changes; do not overwrite or revert user edits.
- Exclude the existing uncommitted revision-history/activity work from this audit unless a candidate is already explicitly covered by those edits.
- Keep `AGENTS.md` and `.agents/skills/passly-maintenance/` local; never stage or commit them.
- Commit verified code and formal documentation by exact path; `AGENTS.md` and `.agents/skills/` remain local-only.
- Do not delete code based only on similar names, file size, absent textual references, or stylistic preference.
- Keep Room schema, internal protocols, and persistence format version at 1; document any incompatible development-data implications.
- Sync behavior changes to the relevant formal topic docs and record deferred work in this plan.

## Review Focus

- Hilt/Android/serialization/resource callers that ordinary Kotlin reference search misses: preserve or remove only after registrations and platform entry points are checked.
- Security-sensitive lifecycle cleanup: verify ownership and wipe/cancellation semantics before deleting helpers or state.
- Public contracts used across modules: do not collapse types where the boundary is consumed by another module or stable format.
- Existing dirty changes: compare against the initial status and inspect diffs before touching any shared file.
- Device-only behavior: record unresolved runtime verification as a TODO instead of inferring success from compilation.

## Audit Ledger

Append per-module findings to this file as work proceeds. Each finding includes evidence, decision, affected files, validation, and any follow-up TODO. Keep unresolved candidates explicitly deferred.

### Baseline and rulings

- Baseline captured before implementation: 41 tracked paths already modified/deleted and 4 pre-existing untracked paths. The current revision-history/activity implementation, related docs, schema, tests, `AGENTS.md`, and related TODO edits are protected. No tracked or untracked user file has been reverted.
- Repository inventory: 7 Gradle modules and 1,003 production Kotlin files. Existing module docs explicitly prohibit no-op mappers/use cases and forwarding-only coordinators; use them as the decision rule rather than class names.
- Ruling: don't consolidate the two `TestLogger.kt` files. `core` provides only a no-op `Logger`; `app` adds a recording logger used by application behavior tests, and the Gradle setup does not expose the core test source as a fixture. The apparent duplicate has distinct test responsibilities; sharing it would add test-fixtures wiring without removing behavior.
- Ruling: keep both `ClipboardModule.kt` files. The `core` module binds the platform `SecureClipboard`; the app module binds separate Domain ports for controlled clipboard writes/cleanup. Names match, bindings do not.

### Task 2 — `:core` and `:core:common`

- Result: reviewed production source inventory, module dependencies, clipboard DI, test logger helpers, and shared error package. No production implementation met the confirmed-removal threshold.
- Similar filenames retained: `AddEntryTransition.kt` contains shared key/overflow constants while the editor package contains the transition modifier; these are consumed together and have distinct roles.
- Validation: read-only reference/registration audit; no source changed in these modules, so no module test was needed.
- Dependency aliases were reconciled in Task 7; no unused declaration was proven, so this follow-up is closed.

### Task 3 — `:domain` and `:runtime:session`

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

### Task 4 — `:security`

- Reviewed authentication manager exception handling, biometric candidate cleanup, KDF runner, session event tracing, and controller lock paths against current tests and `docs/development/kotlin-style.md` / `docs/security/authentication.md`.
- No dead or duplicate production implementation was proven; state restoration, durable candidate reconciliation, event tracing, and worker-secret cleanup have separate ownership.
- Fixed lock-cancellation bug: after acquiring the lock mutex, `VaultSessionController.lock()` now completes revocation, sensitive-key clear, session lock/seal, DEK wipe, and terminal-state publication inside `NonCancellable`; it then checks the original coroutine context so cancellation propagates to `SecuritySessionEventLogger` as `LockCancelled`. A regression test first failed because the DEK remained unlocked, then passed with assertions for wiped DEK, SEALED state, Locked auth state, and cancellation event.
- Verification: `:security:testDebugUnitTest` passed, including the new cancellation regression. `:app:testDebugUnitTest` and `:app:compileDebugKotlin` passed.
- Remaining security limitation: a real hardware biometric re-enrollment/Keystore invalidation event is still not verified by emulator or unit tests.
- Skill lesson: when cancellation meets security cleanup, keep cleanup complete and cancellation visible; test both resource wiping and terminal-state/event reporting.

### Task 5 — `:data`

- Reference audit found `AttachmentRefMapper` had no callers; Room reference rows are created/queried through DAOs and current restoration logic directly because it also handles the revision snapshot contract. Removed the unused mapper and its unused `EntryAttachment` domain model. Kept `AttachmentStatus`, which is used by stable storage mapping and restore logic.
- No schema, persistent key, transaction boundary, or encryption behavior changed.
- Verification after the dead-code removals: `:core:common:test` (NO-SOURCE), `:domain:test`, `:data:test`, and `:app:compileDebugKotlin` all succeeded. `:runtime:session:test` passed independently with 9 tests.
- The cross-project declaration scan found no other unreferenced Data declarations. No Room or storage mapping changes were needed; the current dirty `StorageEnumMappings.kt` and history transaction files remain protected.

### Task 6 — `:app`

- Removed unused `AppErrorNoticeMapper`; all production error notices are selected by feature-specific results (for example, Backup's typed notice mapper). Updated `docs/architecture/error-handling.md` to remove the stale global mapping flow and obsolete `NetworkErrors.kt` path.
- Removed four unreferenced one-line argument constructors from `NoticeArgument.kt`; callers already construct typed `ArgumentValue` at their use site.
- Removed unreferenced `DetailOpenRequest`; retained `DetailLaunchMode`, which `DetailRoute` and `DetailViewModel` consume.
- Verification: `:app:testDebugUnitTest` and `:app:compileDebugKotlin` succeeded after the cleanup. Repository-wide search found no remaining references to removed symbols.
- The candidate sweep identified `ObservabilityModule`, `EntryPolicyModule`, and `BackupDispatchersModule` as declaration-only symbols; all are retained because Hilt installs them through `@Module` metadata. Existing `BackupNoticeMapper` has a live Route caller and tests. Protected detail history/activity files were not edited.

### Task 7 — dependencies, build logic, tests, and docs

- Reviewed all 68 `[libraries]` aliases in `gradle/libs.versions.toml` against Gradle/Kotlin sources; no unused alias was proven, so no dependency was removed.
- Reviewed `buildSrc` task registration: generator, verification, module boundary and metrics tasks are registered through the Gradle plugin and have distinct CLI/build/check responsibilities. No confirmed duplicate build task or implementation was removed.
- Updated `docs/architecture/error-handling.md` for current Feature-owned Notice selection and removed stale file/type references. Full docs-relative-link scan passed: `All relative Markdown links resolve.`
- Preserved the pre-existing `docs/superpowers/plans/2026-10-27-project-todos.md` work. The history Sheet bottom-space fix was committed separately in `1ddf8d7b`; remaining device checks stay in that plan.

### Task 8 — local agent and project skills

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

## Tasks

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
- 复验：`Pixel_8_Pro` API 36 AVD 的 Data 仪器测试 33 项、App 仪器测试 56 项全部通过。测试后通过 `adb emu kill` 关闭本轮启动的 AVD；当前没有连接设备。API 31 system image 未安装且没有 `sdkmanager`，硬件 Keystore 生物录入变更仍需真机验证，两项继续保留为未完成 TODO。
- 安全会话复审发现 `VaultSessionController.lock()` 和恢复回滚路径捕获 `sessionManager.softLock()/seal()` 的 `CancellationException` 后会当作普通封存失败记录并吞掉。新增封存与软锁取消回归，旧实现先红；改为在 NonCancellable 区完成钥匙清理、锁状态和 Locked 发布，再传播取消，由 Trace 记为 `LockCancelled`。更新认证文档明确内部取消合同。
- 验证：`VaultSessionIdleTimeoutTest`、完整 `:security:testDebugUnitTest`、`:app:testDebugUnitTest`、`verifyArchitecture`、`:security:lintDebug` 与 `git diff --check` 均通过。

### Task 1: Establish a protected baseline and live audit ledger

**Files:**
- Modify: `docs/superpowers/plans/2026-10-29-passly-redundancy-audit.md`
- Read: initial `git status`, tracked and untracked diffs, module list, project-specific agent rules.

- [x] Capture the initial worktree state and mark all pre-existing edits as protected.
- [x] Record the audit order and evidence rules in the ledger.
- [x] Recheck status before each edit to a previously dirty file.

### Task 2: Review `:core` and `:core:common`

**Files:**
- Review: production sources, module dependencies, test support, architecture docs, DI bindings.
- Modify only files with a confirmed duplicate/dead responsibility; update the matching formal doc and this plan.

- [x] Inspect callers, DI, Android platform registrations, and tests for every candidate.
- [x] Remove only proven redundant implementation; preserve separate production/test adapters and platform contracts.
- [x] Run relevant module verification for code changes and log exact commands/results.
- [x] Append retained/deferred candidates and TODOs to the audit ledger.

### Task 3: Review `:domain` and `:runtime:session`

**Files:** Domain contracts, policies, models, runtime session state machine, tests, and their consumers.

- [x] Check duplicate domain rules and pass-through contracts across module boundaries.
- [x] Preserve explicit state, cancellation, session ownership, and external stable-key behavior.
- [x] Remove confirmed redundant implementation, update formal docs, run relevant verification, and record TODOs.

### Task 4: Review `:security`

**Files:** authentication orchestration, key/session lifecycle, DI, tests, and security documentation.

- [x] Trace each candidate through authentication state ownership, callers, DI, cleanup, and security docs.
- [x] Remove only a duplicate or unreachable responsibility with complete lifecycle evidence.
- [x] Run relevant verification for changes and record device-only follow-up separately.

### Task 5: Review `:data`

**Files:** Room/DataStore implementations, DAOs, mappers, transactions, dependency declarations, tests, and data docs.

- [x] Compare mapper, transaction, repository, and serialization responsibilities against actual call graphs and schemas.
- [x] Preserve encryption boundaries, transaction atomicity, referential cleanup, stable storage keys, and Room version 1.
- [x] Remove confirmed redundancy, update current data docs, run relevant verification, and append unresolved TODOs.

### Task 6: Review `:app` and feature presentation

**Files:** app wiring, Android entry points, features, ViewModels/Routes/Reducers/Mappers, resources, tests, and feature docs.

- [x] Review by feature while excluding the protected in-progress revision-history changes.
- [x] Check Manifest, DI, navigation, resource IDs, SavedState, reflection, and platform callbacks before classifying code as unused.
- [x] Remove confirmed duplicate rules, empty mappings, or forwarding-only layers; keep lifecycle/session ownership intact.
- [x] Update formal docs, run relevant verification, and record device verification TODOs.

### Task 7: Reconcile dependencies, tests, and documentation

**Files:** Gradle catalogs/build files, docs index/topic pages, obsolete plans and tests.

- [x] Verify dependency use before deleting declarations and check generated/plugin use sites.
- [x] Remove obsolete tests/docs only when the replaced behavior and all links have been reconciled.
- [x] Update authoritative docs and links; preserve ADR history and the requested audit TODO.

### Task 8: Refine local agent and project maintenance skill

**Files:**
- Modify: `AGENTS.md` (local only)
- Create: `.agents/skills/passly-maintenance/SKILL.md` (local only)
- Create: `.agents/skills/passly-redundancy-cleanup/SKILL.md` (local only)
- Read: `docs/development/maintenance-boundaries.md`, `docs/development/documentation.md`, audit ledger.

- [x] Keep AGENTS concise: enduring repository constraints and links to authoritative docs only.
- [x] Split routine Passly maintenance/TODO guidance from focused redundancy-audit guidance.
- [x] Write skills from observed failure modes, concrete bug/TODO lessons, and the verified audit process.
- [x] Ensure skills avoid duplicating formal docs and require evidence before removing code.
- [x] Verify both local-only files are unstaged and report their Git state.

### Task 9: Final scope and workspace review

**Files:** all files changed by this task and the protected baseline.

- [x] Compare final changes against the initial status; confirm pre-existing work is intact.
- [x] Run `git diff --check` and all verification required by the affected module rules.
- [x] Confirm each commit contains only its audited paths; preserve `AGENTS.md`, local skills, and unrelated worktree changes.
- [x] Leave remaining candidates and missing device checks as explicit TODOs.

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
