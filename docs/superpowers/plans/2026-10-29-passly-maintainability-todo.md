# Passly Maintainability Follow-up Plan

**Goal:** Close the remaining Presentation workflow ownership and database-observation coverage findings with focused regression coverage.

**Source:** Current Passly maintenance review on 2026-10-29. Follow [Passly agent rules](../../../AGENTS.md), [state management](../../development/architecture-and-state.md), [maintenance boundaries](../../development/maintenance-boundaries.md), [sensitive-data access](../../security/sensitive-data-access.md), and [redundancy cleanup skill](../../../.agents/skills/passly-redundancy-cleanup/SKILL.md).

**Constraints:** Preserve Room/schema and wire-format versions. Keep behavior in its true owner and retain adapters only when they enforce a platform, persistence, security, or module boundary. Do not stage or commit. Project-local `AGENTS.md` and `.agents/skills/` remain local.

## Follow-up outcomes

**Completed audit:** See [the dated maintainability and redundancy review](../../reviews/2026-10-29-passly-maintainability-audit.md). Outcomes and verification for each follow-up are recorded below.

### Task 1: Audit multi-workflow Presentation owners without adding forwarding layers

**Files:**
- Review/refactor: `app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/DetailViewModel.kt`
- Related ownership: `DetailWorkSession.kt`, `DetailFaviconSession.kt`, `DetailQrSession.kt`, `history/DetailRevisionReducer.kt`, and `history/DetailRevisionRevealStore.kt`
- Tests: current focused Detail state/session tests; add `DetailViewModel` orchestration coverage before moving workflow ownership
- Docs: detail feature/state docs if ownership contracts change

- [x] Audit each `DetailUiAction` branch against its state owner, job handle, side effects, and caller. Separate only workflows that have an independent state/lifecycle and can own their own async work; do not extract a handler that only receives a large ViewModel callback bag or forwards actions back to the same ViewModel.
- [x] Evaluate moving the remaining revision-history async orchestration and/or favicon preparation/save orchestration beside its existing state model/session. A successful reduction must remove a cohesive set of ViewModel dependencies, job fields, or mutation branches while retaining one authoritative UI state projection and existing sensitive-value cleanup/session guards.
- [x] Add focused ViewModel-level tests for action-to-result coordination, entry replacement/session closure, and late async results before restructuring; the existing reducer/session tests do not currently exercise `DetailViewModel` as a whole.
- [x] Review current `DetailRoute` and `DetailScreen` boundaries: Route owns lifecycle, Activity Result, and host effects; Screen remains state-plus-actions UI. Avoid a duplicate Route/ViewModel state source.
- [x] Retain `CredentialResponseRoute` as a thin state/action/result host and the `CredentialResponseViewModel` as one Credential Manager request lifecycle unless new evidence shows unrelated ownership; it delegates request handling and keeps terminal Activity result delivery in the Activity.
- [x] Run focused presentation tests, relevant App test/lint/architecture gates, and `git diff --check` after implementation.

**Audit disposition:** `DetailRoute` is currently thin: it collects state, launches image picking, maps copy effects to Toast, loads the route entry, and clears sensitive state on disposal. `DetailScreen` receives presentation state and actions. `DetailViewModel` has 16 constructor dependencies and owns unrelated job families for entry/history, package picker, tag loading, revision observation/operations, and favicon processing; its action dispatcher covers entry editing, sensitive reveal/copy, tags, favicon, OTP/QR, and revision history. Some true owners already exist (`DetailWorkSession`, `DetailFaviconSession`, `DetailQrSession`, revision reducer/store), so the task is to close the remaining ownership gaps rather than move code mechanically. The new `DetailViewModelTest` now covers the restore action/result lifecycle and stale-result boundaries. By contrast, `CredentialResponseRoute` only sends the host action and delivers terminal state to its Activity callback; the ViewModel coordinates one request lifecycle across get/unlock/create and delegates the platform request/selection work to handlers. `AuthenticationRoute` is also thin; `UnlockViewModel` has only two collaborators and coordinates one authentication surface whose credential wiping, method availability, verification, and password setup share the same UI/session state, so retain it pending contrary call-graph evidence. A narrow exploratory review found the existing cleanup skill's general ownership criteria useful but no explicit Route/ViewModel audit rubric; the new local `AGENTS.md` rule now requires auditing state owner, independent lifecycle/job groups, and dependency closure rather than file length.

**Detail follow-up disposition (2026-10-30):** Rechecked revision and favicon orchestration. Revision actions already reduce through `DetailRevisionReducer` into the single `DetailUiState.revisions` projection, and reveal payload lifecycle belongs to `DetailRevisionRevealStore`; moving the remaining jobs would still require a parent-session callback bag to coordinate entry replacement and presentation loading. Favicon preparation owns its async resource cleanup in `DetailFaviconSession`, while save remains coupled to `DetailWorkSession` entry/session guards. No extraction was made because neither candidate can yet own the full lifecycle while removing meaningful ViewModel dependencies or mutation branches. `DetailViewModelTest` now exercises restore action-to-result flow, back/dismiss, entry replacement, session closure, and late results; no second state projection or callback bag was introduced.

**Restore regression fix (2026-10-30):** A confirmed revision restore could be canceled by Back or sheet dismissal while repository success cleanup was suspended after the database transaction. The transaction could persist while the ViewModel skipped its success transition and detail refresh. The reducer now holds the active restore state against close/back, ViewModel action handlers do not cancel the restore job in that state, and the sheet disables Back and blocks drag/dismiss. Existing session/entry replacement still invalidates late UI delivery. The ViewModel regressions gate the post-transaction cleanup, then verify successful refresh or no stale write into a replacement/cleared state.

### Task 2: Close the broader Route/ViewModel ownership audit

**Files:**
- Review: `app/src/main/java/com/aozijx/passly/feature/vault/presentation/list/VaultRoute.kt` and `VaultViewModel.kt`
- Review: `app/src/main/java/com/aozijx/passly/feature/settings/presentation/security/RecoveryCodeSettingsViewModel.kt` and `ui/security/RecoveryCodeDetail.kt`
- Related contracts/tests: scanner import callback, `ImportScannedOtpUseCase`, recovery-code draft/interactor tests, and presentation action contracts

- [x] Trace the `VaultRoute -> vaultViewModel.importScannedOtp -> ImportScannedOtpUseCase` path with the scanner's suspend `onImport` callback. Decide whether the Route is the correct feature-composition owner because it must combine OTP import with Wi-Fi navigation and return `Applied/Rejected`, or whether async execution and result publication belong in the ViewModel. Do not expose a concrete UseCase property from the ViewModel or add a one-line forwarding method without a lifecycle/state ownership gain.
- [x] Check cancellation and overlay-disposal behavior while a scanned OTP is being persisted; if the owner changes, verify committed writes cannot produce a stale rejection or leave scanner import state stuck.
- [x] Evaluate Recovery Code generation/disclosure/commit and existing-code verification as separate state lifecycles. Split only if a new owner removes real state/job/dependency coupling and can own all `OwnedChars`, `SavedStateHandle` disclosure restoration, draft dismissal, and `close()` cleanup; otherwise retain the current single settings-page owner and document the evidence.
- [x] Add focused orchestration coverage only for behavior changed by a selected ownership move; current isolated draft/session/reducer tests do not exercise these Route/ViewModel handoffs end to end.
- [x] Run the affected App presentation tests and `git diff --check` after implementation.

**Expanded-scope disposition:** Retain `SettingsRoute` as the adaptive navigation host, `BackupViewModel` as one sealed import/export workflow (two injected collaborators), and the Legacy/Accessibility Autofill ViewModels as owners of one interaction lifecycle each (authorize → discover → select → terminal delivery/cleanup). Do not split these based on current file sizes.

**Vault scanner disposition (2026-10-30):** `VaultRoute` is the feature-composition owner: one scanner callback must either persist an OTP or navigate to the Wi-Fi editor, then map the operation to `Applied/Rejected`. `VaultViewModel` exposes the concrete use case, but moving the work to its scope would make the callback lose its suspend result; adding a suspend forwarding method would add no state or lifecycle ownership. Keep the callback in Route and consider replacing the concrete property only if a real lifecycle/state contract is introduced. The scanner's `ScannerImportSession` reserves `Importing`, returns to `Ready` on rejection, cancellation is propagated, and disposal closes the session. `ImportScannedOtpUseCase` checks full secure-session access before work and calls `ensureActive()` after `CreateEntryUseCase`; cancellation after a completed write therefore cannot become a stale `Rejected` result. Overlay disposal cancels the scanner route coroutine and closes its session, so no code move is needed to avoid a stuck visible scanner state. No production change was made for this audit.

**Verification:** No ownership move was selected, so no Route/ViewModel orchestration behavior changed. `:app:testDebugUnitTest --tests '*ScannerImportSessionTest'` passes; Recovery Code interactor and ViewModel tests are recorded under Task 3. `git diff --check` passes.

### Task 3: Make recovery-code draft commit single-flight and terminal

**Files:**
- Review/fix: `app/src/main/java/com/aozijx/passly/feature/settings/presentation/security/RecoveryCodeSettingsViewModel.kt`
- Review/fix: `app/src/main/java/com/aozijx/passly/feature/settings/presentation/security/RecoveryCodeSettingsContract.kt`
- Review/fix: `app/src/main/java/com/aozijx/passly/feature/settings/presentation/ui/security/RecoveryCodeSheet.kt`
- Review/fix: `app/src/main/java/com/aozijx/passly/feature/settings/presentation/ui/security/RecoveryCodeDetail.kt`
- Review/fix: `app/src/main/java/com/aozijx/passly/feature/settings/security/RecoveryCodeDraftInteractor.kt` only if commit ownership is moved or centralized there
- Tests: new focused `RecoveryCodeSettingsViewModelTest` plus existing draft interactor/security draft tests

- [x] Add a deterministic ViewModel regression that invokes confirm twice while the first draft commit is suspended; assert one commit executes and the eventual committed state cannot be overwritten by a later result from the same draft.
- [x] Add an explicit committing state or equivalent single-flight ownership and disable confirmation while committing. Ensure Generate, Dismiss, and sheet disposal cannot close/replace the same draft mid-commit or let stale outcomes overwrite a committed state.
- [x] Keep commit state and draft cleanup under one owner; preserve cancellation propagation, disclosure wiping, SavedStateHandle behavior, and retry only when the draft is still valid.
- [x] Run the focused App and Security tests, the relevant architecture gate, and `git diff --check` after implementation.

**Evidence:** `RecoveryCodeSettingsViewModel.confirmAndEnable()` starts an unguarded coroutine and leaves the draft in `Ready` while `draftInteractor.commit()` suspends. `RecoveryCodeSheet` renders the confirm `Button` without an `enabled` condition, and `RecoveryCodeDraftUiState` has no committing state. `RecoveryCodeDraftInteractor.commit()` snapshots `activeDraft` before suspending, so two callers can both invoke the same draft. The security-owned `SecureRecoveryCodeDraft.commit()` serializes those calls with a mutex; after the first successful save calls `close()` and destroys its envelope, the second call returns `ENVELOPE_CORRUPTED` failure. The first ViewModel coroutine can publish `Committed` and `hasRecoveryCode = true`, after which the second failure path calls `dismiss()` and replaces the visible draft with `Failed`. This leaves persisted recovery-code availability true while presenting a failed setup result. No ViewModel-level regression currently covers duplicate confirmation or terminal-state monotonicity.

**Implementation (2026-10-30):** Added `Committing(disclosure)` as the synchronous single-flight state before launching commit; duplicate confirmation is ignored, and Generate/Copy/Dismiss cannot replace or use the draft during commit. The sheet remains composed with the same disclosure and disables its controls. Success/failure transitions wipe the disclosure through the existing state owner; cancellation still propagates, and ViewModel teardown wipes both Ready and Committing disclosures and closes the interactor. Existing-code verification remains in this settings ViewModel because it uses the same page-owned authentication collaborator and sensitive-input lifecycle, and extraction would require transferring cleanup and page state without reducing the operation's dependencies.

**Verification:** The new `RecoveryCodeSettingsViewModelTest` was first run against the old implementation and failed at the duplicate-call assertion (`commitCalls` was 2); after the fix, it passes. `:app:testDebugUnitTest --tests '*RecoveryCodeDraftInteractorTest'`, `:security:testDebugUnitTest --tests '*DefaultRecoveryCodeDraftFactoryTest'`, and root `verifyArchitecture` pass. The exact focused ViewModel test also passes. Diff check is recorded after the ledger update.

### Task 4: Verify live Room observations across database-only seal and reopen

**Files:** affected activity, link, and revision Room query repositories plus focused Data repository/instrumentation tests.

- [x] Exercise active collectors against a real `AppDatabaseSession`; verify a database-only seal cancels each query source and emits an empty result, then successful reopen re-subscribes without an authentication transition.
- [x] Keep Room access inside `AppDatabaseSession.observeFlow`; do not add a production-only seam solely for the test.
- [x] Run the focused Data test and `git diff --check`.

**Implementation and cleanup (2026-10-30):** Added `AppDatabaseSession.observeWhenReadable`, which owns the shared authorization-plus-`UNLOCKED` gate, empty fallback, and `observeFlow` lease for secure query streams. Migrated activity, link, revision, entry-list, and paging readers to this entry point, removing their duplicated `canRead` fields, `flatMapLatest` branches, and empty-flow fallbacks. This consolidates existing production behavior; Room access remains inside `AppDatabaseSession.observeFlow`, and no test-only production seam or database/schema change was added.

**Verification:** `RoomObservationSessionTest` uses the real SQLCipher-backed `DatabaseProvider` and `AppDatabaseSession` with active Activity, Link, and Revision DAO collectors. On database-only seal, all three Room sources complete and emit empty lists; reopening re-subscribes all three and restores their seeded rows while the authenticated state remains unchanged. The emulator report records 35 Data instrumentation tests with zero failures. `:data:testDebugUnitTest`, `:data:compileDebugKotlin`, `:data:compileDebugAndroidTestKotlin`, root `verifyArchitecture`, and `git diff --check` pass. Scoped source search confirms the old repeated gate/flow wrappers remain only inside the shared helper, not in the migrated repositories. No additional production bug was exposed by this coverage.

**Current revalidation (2026-10-30):** Re-ran `RoomObservationSessionTest` on the connected Pixel 8 Pro API 36 emulator; the real-session seal/empty/reopen/resubscribe regression passed. `:data:testDebugUnitTest`, `:data:lintDebug`, and `verifyArchitecture` also completed successfully; the focused unit task was up to date.

### Task 5: Make diagnostics log clearing report queue-drain failure consistently

  - [x] Confirm `EncryptedLogRecordStore.clear()` cannot report success after `flush(500)` times out while an earlier `WriteTask` is still pending or blocked.
  - [x] Define one result contract for `LogRecordStore.clear()` / `DiagnosticsLogStore.clear()` and use it in both `StorageSettingsViewModel` and `DiagnosticsViewModel`; both surfaces reach an explicit cleared or failed state.
  - [x] Serialize the clear barrier with write acceptance so writes accepted before clear cannot recreate old records after the user receives success; preserve records legitimately emitted after the clear boundary.
  - [x] Add deterministic writer-blocking regressions for timeout, clear outcome, and post-clear queue behavior; test both presentation callers' terminal state.
  - [x] Update diagnostics/storage docs and run affected Data/App tests, architecture, lint, and `git diff --check`.

**Evidence:** `LogClearResult` now carries clear success or failure across the Core store and feature store contracts. `EncryptedLogRecordStore.clear()` holds the write acceptance lock while draining normal and emergency writers, returns `Failed` on the 500 ms timeout without deleting existing records, and deletes under the storage lock only after a successful drain. Writers accepted after the clear caller acquires the barrier wait until deletion finishes and remain queued afterward. Writer-blocking tests cover timeout retention and post-clear ordering; Storage Settings and Diagnostics both surface explicit failure. Full `:data:testDebugUnitTest` and `:app:testDebugUnitTest`, `verifyArchitecture`, `:data:lintDebug`, and `:app:lintDebug` passed.
