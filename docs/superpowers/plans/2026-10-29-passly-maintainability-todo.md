# Passly Maintainability Follow-up Plan

**Goal:** Close the remaining Presentation workflow ownership and database-observation coverage findings with focused regression coverage.

**Source:** Current Passly maintenance review on 2026-10-29. Follow [Passly agent rules](../../../AGENTS.md), [state management](../../development/architecture-and-state.md), [maintenance boundaries](../../development/maintenance-boundaries.md), [sensitive-data access](../../security/sensitive-data-access.md), and [redundancy cleanup skill](../../../.agents/skills/passly-redundancy-cleanup/SKILL.md).

**Constraints:** Preserve Room/schema and wire-format versions. Keep behavior in its true owner and retain adapters only when they enforce a platform, persistence, security, or module boundary. Do not stage or commit. Project-local `AGENTS.md` and `.agents/skills/` remain local.

## Open work

**Completed audit:** See [the dated maintainability and redundancy review](../../reviews/2026-10-29-passly-maintainability-audit.md). Only open follow-up work remains below.

### Task 1: Reduce multi-workflow Presentation owners without adding forwarding layers

**Files:**
- Review/refactor: `app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/DetailViewModel.kt`
- Related ownership: `DetailWorkSession.kt`, `DetailFaviconSession.kt`, `DetailQrSession.kt`, `history/DetailRevisionReducer.kt`, and `history/DetailRevisionRevealStore.kt`
- Tests: current focused Detail state/session tests; add `DetailViewModel` orchestration coverage before moving workflow ownership
- Docs: detail feature/state docs if ownership contracts change

- [ ] Audit each `DetailUiAction` branch against its state owner, job handle, side effects, and caller. Separate only workflows that have an independent state/lifecycle and can own their own async work; do not extract a handler that only receives a large ViewModel callback bag or forwards actions back to the same ViewModel.
- [ ] Evaluate moving the remaining revision-history async orchestration and/or favicon preparation/save orchestration beside its existing state model/session. A successful reduction must remove a cohesive set of ViewModel dependencies, job fields, or mutation branches while retaining one authoritative UI state projection and existing sensitive-value cleanup/session guards.
- [ ] Add focused ViewModel-level tests for action-to-result coordination, entry replacement/session closure, and late async results before restructuring; the existing reducer/session tests do not currently exercise `DetailViewModel` as a whole.
- [ ] Review `DetailRoute` and `DetailScreen` again after any extraction: Route should own lifecycle, Activity Result, and host effects; Screen should remain state-plus-actions UI. Avoid a duplicate Route/ViewModel state source.
- [ ] Retain `CredentialResponseRoute` as a thin state/action/result host and the `CredentialResponseViewModel` as one Credential Manager request lifecycle unless new evidence shows unrelated ownership; it delegates request handling and keeps terminal Activity result delivery in the Activity.
- [ ] Run focused presentation tests, relevant App test/lint/architecture gates, and `git diff --check` after implementation.

**Audit disposition:** `DetailRoute` is currently thin: it collects state, launches image picking, maps copy effects to Toast, loads the route entry, and clears sensitive state on disposal. `DetailScreen` receives presentation state and actions. `DetailViewModel` has 16 constructor dependencies and owns unrelated job families for entry/history, package picker, tag loading, revision observation/operations, and favicon processing; its action dispatcher covers entry editing, sensitive reveal/copy, tags, favicon, OTP/QR, and revision history. Some true owners already exist (`DetailWorkSession`, `DetailFaviconSession`, `DetailQrSession`, revision reducer/store), so the task is to close the remaining ownership gaps rather than move code mechanically. Existing tests cover reducers, sessions, and projections but there is no `DetailViewModelTest`. By contrast, `CredentialResponseRoute` only sends the host action and delivers terminal state to its Activity callback; the ViewModel coordinates one request lifecycle across get/unlock/create and delegates the platform request/selection work to handlers. `AuthenticationRoute` is also thin; `UnlockViewModel` has only two collaborators and coordinates one authentication surface whose credential wiping, method availability, verification, and password setup share the same UI/session state, so retain it pending contrary call-graph evidence. A narrow exploratory subagent pass found the existing cleanup skill's general ownership criteria useful but no explicit Route/ViewModel audit rubric; the new local `AGENTS.md` rule now requires auditing state owner, independent lifecycle/job groups, and dependency closure rather than file length.

### Task 2: Close the broader Route/ViewModel ownership audit

**Files:**
- Review: `app/src/main/java/com/aozijx/passly/feature/vault/presentation/list/VaultRoute.kt` and `VaultViewModel.kt`
- Review: `app/src/main/java/com/aozijx/passly/feature/settings/presentation/security/RecoveryCodeSettingsViewModel.kt` and `ui/security/RecoveryCodeDetail.kt`
- Related contracts/tests: scanner import callback, `ImportScannedOtpUseCase`, recovery-code draft/interactor tests, and presentation action contracts

- [ ] Trace the `VaultRoute -> vaultViewModel.importScannedOtp -> ImportScannedOtpUseCase` path with the scanner's suspend `onImport` callback. Decide whether the Route is the correct feature-composition owner because it must combine OTP import with Wi-Fi navigation and return `Applied/Rejected`, or whether async execution and result publication belong in the ViewModel. Do not expose a concrete UseCase property from the ViewModel or add a one-line forwarding method without a lifecycle/state ownership gain.
- [ ] Check cancellation and overlay-disposal behavior while a scanned OTP is being persisted; if the owner changes, verify committed writes cannot produce a stale rejection or leave scanner import state stuck.
- [ ] Evaluate Recovery Code generation/disclosure/commit and existing-code verification as separate state lifecycles. Split only if a new owner removes real state/job/dependency coupling and can own all `OwnedChars`, `SavedStateHandle` disclosure restoration, draft dismissal, and `close()` cleanup; otherwise retain the current single settings-page owner and document the evidence.
- [ ] Add focused orchestration coverage only for behavior changed by a selected ownership move; current isolated draft/session/reducer tests do not exercise these Route/ViewModel handoffs end to end.
- [ ] Run the affected App presentation tests and `git diff --check` after implementation.

**Expanded-scope disposition:** Retain `SettingsRoute` as the adaptive navigation host, `BackupViewModel` as one sealed import/export workflow (two injected collaborators), and the Legacy/Accessibility Autofill ViewModels as owners of one interaction lifecycle each (authorize → discover → select → terminal delivery/cleanup). Do not split these based on current file sizes. Defer the Vault scanner-usecase exposure and Recovery Code dual-workflow question to the call-site/lifecycle checks above; neither has been changed in this review.

### Task 3: Make recovery-code draft commit single-flight and terminal

**Files:**
- Review/fix: `app/src/main/java/com/aozijx/passly/feature/settings/presentation/security/RecoveryCodeSettingsViewModel.kt`
- Review/fix: `app/src/main/java/com/aozijx/passly/feature/settings/presentation/security/RecoveryCodeSettingsContract.kt`
- Review/fix: `app/src/main/java/com/aozijx/passly/feature/settings/presentation/ui/security/RecoveryCodeSheet.kt`
- Review/fix: `app/src/main/java/com/aozijx/passly/feature/settings/presentation/ui/security/RecoveryCodeDetail.kt`
- Review/fix: `app/src/main/java/com/aozijx/passly/feature/settings/security/RecoveryCodeDraftInteractor.kt` only if commit ownership is moved or centralized there
- Tests: new focused `RecoveryCodeSettingsViewModelTest` plus existing draft interactor/security draft tests

- [ ] Add a deterministic ViewModel regression that invokes confirm twice while the first draft commit is suspended; assert one commit executes and the eventual committed state cannot be overwritten by a later result from the same draft.
- [ ] Add an explicit committing state or equivalent single-flight ownership and disable confirmation while committing. Ensure Generate, Dismiss, and sheet disposal cannot close/replace the same draft mid-commit or let stale outcomes overwrite a committed state.
- [ ] Keep commit state and draft cleanup under one owner; preserve cancellation propagation, disclosure wiping, SavedStateHandle behavior, and retry only when the draft is still valid.
- [ ] Run the focused App and Security tests, the relevant architecture gate, and `git diff --check` after implementation.

**Evidence:** `RecoveryCodeSettingsViewModel.confirmAndEnable()` starts an unguarded coroutine and leaves the draft in `Ready` while `draftInteractor.commit()` suspends. `RecoveryCodeSheet` renders the confirm `Button` without an `enabled` condition, and `RecoveryCodeDraftUiState` has no committing state. `RecoveryCodeDraftInteractor.commit()` snapshots `activeDraft` before suspending, so two callers can both invoke the same draft. The security-owned `SecureRecoveryCodeDraft.commit()` serializes those calls with a mutex; after the first successful save calls `close()` and destroys its envelope, the second call returns `ENVELOPE_CORRUPTED` failure. The first ViewModel coroutine can publish `Committed` and `hasRecoveryCode = true`, after which the second failure path calls `dismiss()` and replaces the visible draft with `Failed`. This leaves persisted recovery-code availability true while presenting a failed setup result. No ViewModel-level regression currently covers duplicate confirmation or terminal-state monotonicity.

### Task 4: Verify live Room observations across database-only seal and reopen

**Files:** affected activity, link, and revision Room query repositories plus focused Data repository/instrumentation tests.

- [ ] Exercise active collectors against a real `AppDatabaseSession`; verify a database-only seal cancels each query source and emits an empty result, then successful reopen re-subscribes without an authentication transition.
- [ ] Keep Room access inside `AppDatabaseSession.observeFlow`; do not add a production-only seam solely for the test.
- [ ] Run the focused Data test and `git diff --check`.

**Evidence:** Production repositories now share the tested authorization-plus-`UNLOCKED` read projection. The projection unit test and source wiring cover the gate, but this checkout has not yet run a Room-backed collector cancellation/resume test.
