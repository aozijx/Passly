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
- Commit verified code and formal documentation by exact path; keep `AGENTS.md` and local skills outside commits.
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
- TODO: inspect dependency aliases for unused declarations during Task 7 after each module's source/API review.

### Task 3 — `:domain` and `:runtime:session`

- `:domain`: retained the distinct field reader/editor, draft materializer, validators, revision comparator, and OTP/Wi-Fi policies; each has consumers with a separate domain rule or contract. No duplicate production source or clearly dead implementation was found in the targeted model/policy scan.
- Follow-up cross-module reference audit found stale, unreferenced domain pieces: `BackupSettings` and its private enum contract, `SessionPolicy`/`SessionPolicySource`, `StagedAttachment`, and the `CredentialCandidate` wrapper. Removed them; retained `CredentialMatch` and `MatchType`, which are consumed by Autofill.
- Attachment audit found `AttachmentRefMapper` and `EntryAttachment` had no callers; removed both. Retained `AttachmentStatus` because Room storage mapping and revision restoration use it, and moved it to `AttachmentStatus.kt` to keep the file name aligned with its surviving type.
- Core common audit found `NetworkError` had no callers; removed the unused exception model while retaining `NETWORK_ERROR`, which the shared UI error mapper still consumes.
- `:runtime:session` bug: `RuntimeSessionManager.unlock()` mapped a `CancellationException` from the suspending `SessionKeySource.copyKey()` to `KeyUnavailable`. Added a regression test; it failed before the fix. The first post-fix run exposed an overstrict exception-instance equality assertion because `withContext` may rewrap cancellation. The assertion now checks cancellation type and message.
- Follow-up bug: `SessionLeaseGate.open()` also converted cancellation from the suspending resource open into `OpenFailed`; `SessionLeaseGate.close()` swallowed cancellation while still discarding the handle. Both reproduced with focused regression tests.
- Fix: all three suspend boundaries rethrow `CancellationException` before mapping other `Throwable` values. Close still discards its handle in `finally`; state remains SEALED when open/close is cancelled. Updated `docs/architecture/runtime-flows.md` to document the key-copy wipe and cancellation contract.
- Verification: focused RED runs failed at the expected assertions; final `:runtime:session:test` — 9 tests passed after the fixes.
- TODO: inspect enum-name persistence in OTP materialization during the wider `:data` stable-storage-key review; no change made yet.
- TODO: revisit unused `EntryRevision` after the active history implementation is finished; it is currently isolated in source search, but this audit explicitly protects the in-progress history work.

### Task 4 — `:security`

- Reviewed authentication manager exception handling, biometric candidate cleanup, KDF runner, session event tracing, and controller lock paths against current tests and `docs/development/kotlin-style.md` / `docs/security/authentication.md`.
- No dead or duplicate production implementation was proven; state restoration, durable candidate reconciliation, event tracing, and worker-secret cleanup have separate ownership.
- Deferred bug/TODO: `VaultSessionController.lock()` catches `Throwable` from suspend session lock/seal calls, records a failure, and continues wiping the DEK. This protects the cleanup path, but can turn caller cancellation into a lock failure and prevents `SecuritySessionEventLogger` from reporting the cancelled terminal. Define and test the required cancellation/cleanup contract before changing this security-sensitive path.
- Skill lesson: when cancellation meets security cleanup, keep cleanup complete and cancellation visible; don't replace a broad catch without a regression test that proves both resource wiping and terminal state.

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
- [x] Confirm the code and documentation commits contain only audited paths; preserve `AGENTS.md`, local skills, and unrelated worktree changes.
- [x] Leave remaining candidates and missing device checks as explicit TODOs.
