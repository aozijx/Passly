# Related Password Management Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` or `superpowers:executing-plans` to implement this plan task-by-task. Each task ends with its own verification and commit.

**Goal:** Present saved direct password links clearly in Detail and let users remove one by left-swiping a row and confirming, while keeping both password entries.

**Architecture:** Keep `EntryLink` storage unchanged and reuse `EntryLinkRepository.delete(linkId)`, which deletes the selected relationship row and retains its transaction, revision snapshots, and cleanup. Detail loading will preserve account-group entries, other existing relation rows, and eligible direct active LOGIN-to-LOGIN `RELATED_TO` links separately; one current-page-owned workflow will handle bind/unlink state, logging and refresh. Presentation owns the confirmation dialog and keeps existing relation navigation intact.

**Tech Stack:** Kotlin, Hilt, Coroutines/Flow, Jetpack Compose Material 3, Room, JUnit, Compose UI instrumentation tests.

**Spec:** [2026-11-08-related-password-management-design.md](../specs/2026-11-08-related-password-management-design.md)

## Global Constraints

- Direct password relations are `RELATED_TO` links whose two endpoints are active, saved `EntryType.LOGIN` entries.
- Unlink removes only the link; it does not delete either entry.
- Reuse `EntryLinkRepository.delete(linkId)` to delete the existing bind-link row by ID. `RoomEntryLinkRepository` already performs this in its transaction, snapshots both endpoints, and schedules/drains existing attachment cleanup; do not add a table/column, migration, DAO method, or change the repository contract.
- A left swipe opens confirmation. Dismissing confirmation leaves the link unchanged; confirming removes only the selected link.
- Bind/unlink work belongs to the current Detail entry and secure-session owner. Propagate cancellation, suppress stale results, log ordinary operation failures and keep a retry path.
- Use existing Material 3 components and swipe container; add no dependency or image asset. Update Chinese and English resources together.

## Review Focus

- **ACCOUNT group and direct password link share the same target:** preserve separate visible relationship rows so unlinking cannot appear ineffective; cover in `DetailSessionQueryTest` and the section instrumentation test.
- **A `RELATED_TO` edge has a non-LOGIN or inactive endpoint:** keep the existing row visible and navigable, but never expose unlink; cover in the query projection and section instrumentation tests.
- **Reversed endpoint order:** resolve the same `RELATED_TO` link in either direction; cover in `RelatedPasswordLinksTest`.
- **Target is deleted or no longer LOGIN before confirmation:** refuse to delete, refresh the projection, and close stale confirmation if the row is no longer eligible; cover in `RelatedPasswordLinksTest` and `DetailPasswordRelationsWorkflowTest`.
- **Repeated confirmation or a failed delete:** run one mutation at a time and keep retry feedback attached to the active dialog; cover in `DetailPasswordRelationsWorkflowTest`.
- **Page replacement or authentication revocation during unlink/refresh:** cancel the owned job and prevent stale state publication; cover in `DetailPasswordRelationsWorkflowTest`.

## File Structure

| Owner | Files | Responsibility after the change |
| --- | --- | --- |
| Detail query | `DetailSessionQuery.kt`, `DetailPresentationLoader.kt` | Read the link graph once, preserve account-group and other existing relation rows, and produce eligible direct-password summaries with link IDs. |
| Password links | `RelatedPasswordLinks.kt` | Own candidate filtering, bind/unlink validation, repository calls, and operation logging. |
| Detail workflow | `DetailPasswordRelationsWorkflow.kt`, Detail state/action/reducer/ViewModel files | Own request jobs, confirmation state, page/session cancellation, retry state, and refresh publication. |
| Detail UI | `RelatedEntriesSection.kt`, `RelatedPasswordPickerSheet.kt`, `DetailBody.kt`, UI models and bilingual resources | Render separate relationship groups, bind/picker states, swipe confirmation, progress, errors, and accessibility action. |
| Verification and records | focused App unit/Compose tests, feature documentation, old binding plan, Task 35 TODO | Pin the behavior and retire stale implementation notes without changing Data contracts. |

---

### Task 1: Separate account-group and direct-password projections

**Files:**
- Modify `app/src/main/java/com/aozijx/passly/feature/vault/detail/DetailSessionQuery.kt`
- Modify `app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/DetailPresentationLoader.kt`
- Modify `app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/DetailReducer.kt`
- Modify `app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/DetailUiState.kt`
- Modify `app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/DetailUiMapper.kt`
- Modify `app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/DetailPasswordBindingWorkflow.kt`
- Modify `app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/ui/DetailBody.kt`
- Modify `app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/ui/model/DetailUiModels.kt`
- Modify `app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/ui/model/DetailPresentationModels.kt`
- Modify `app/src/test/java/com/aozijx/passly/feature/vault/detail/DetailSessionQueryTest.kt`
- Modify `app/src/test/java/com/aozijx/passly/feature/vault/presentation/detail/DetailReducerTest.kt`
- Modify `app/src/test/java/com/aozijx/passly/feature/vault/presentation/detail/DetailUiMapperTest.kt`
- Replace `app/src/test/java/com/aozijx/passly/feature/vault/detail/DetailRelatedEntryIdsTest.kt` with coverage in `DetailSessionQueryTest.kt`

**Interfaces:**
- Add `DetailRelatedPassword(linkId: EntryLinkId, targetEntryId: EntryId, title: String, username: String)` in `DetailSessionQuery.kt`; do not carry the target `Entry` or credential fields into Detail UI state.
- Change `DetailSessionData` and `DetailSessionSnapshot` to expose `accountGroupEntries: List<Entry>`, `otherRelatedEntries: List<Entry>`, and `relatedPasswords: List<DetailRelatedPassword>`; retain `detail` / `presentation` as they are.
- Replace `resolveRelatedEntryIds(...)` with `resolveAccountGroupEntryIds(...)`; direct `RELATED_TO` neighbors no longer enter the account-group list.
- Add `resolveOtherRelatedEntryIds(...)` for direct `RELATED_TO` neighbors not represented by an eligible active Login-to-Login password link, preserving existing rows for non-Login or inactive endpoints.
- Define one `DetailRelationsSnapshot(accountGroupEntries: List<Entry>, otherRelatedEntries: List<Entry>, relatedPasswords: List<DetailRelatedPassword>)` beside the query projection in `DetailSessionQuery.kt`; `DetailPresentationLoader.reloadRelations(entryId)` returns it directly without copying the snapshot model.
- Add `resolveDirectPasswordLinks(source: Entry, links: List<EntryLink>): List<EntryLink>`; it returns only incident `RELATED_TO` links when `source` is an active LOGIN. Filter loaded targets to active LOGIN entries before creating `DetailRelatedPassword` values.

- [x] **Step 1: Add failing tests.** `DetailSessionQueryTest` covers distinct one-batch projections, retained link ID/username, a target that is both an account member and direct password, reversed link endpoints, deleted/non-LOGIN source or target, unrelated relation kinds, and navigable ineligible direct relations. `DetailReducerTest` verifies all three projections update only for the current entry.
- [x] **Step 2: Run the focused query tests and confirm they fail** because the split projection does not yet exist.
- [x] **Step 3: Implement the split projection** from the existing single `getAll()` link snapshot. Build the union of account-group IDs and direct relation IDs for one batch read; filter removable direct rows to active LOGIN endpoints while retaining other existing `RELATED_TO` rows. Rename the old merged-ID helper and replace obsolete mixed-projection tests.
- [x] **Step 4: Carry the projections through Detail loading and state.** Update loader snapshots, mutations, reducer, `DetailUiState`, existing binding refresh, and UI mapping; keep existing account-group behavior and ordering.
- [x] **Step 5: Run the focused query, loader, mapper, reducer, binding workflow, and Detail ViewModel tests plus `git diff --check`; then commit** as `refactor(vault): separate account and password relations`.

### Task 2: Make the related-password service own binding and unlinking

**Files:**
- Rename `app/src/main/java/com/aozijx/passly/feature/vault/detail/RelatedPasswordBinding.kt` to `RelatedPasswordLinks.kt`
- Rename `app/src/test/java/com/aozijx/passly/feature/vault/detail/RelatedPasswordBindingTest.kt` to `RelatedPasswordLinksTest.kt`
- Modify the injection site in `app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/DetailViewModel.kt`

**Interfaces:**
- Rename class `RelatedPasswordBinding` to `RelatedPasswordLinks`; retain `candidates(sourceEntryId: EntryId)` and `bind(sourceEntryId: EntryId, targetEntryId: EntryId)` signatures.
- Add `suspend fun unlink(sourceEntryId: EntryId, targetEntryId: EntryId, linkId: EntryLinkId): AppResult<Unit>`; the service verifies the current edge has that ID, `RELATED_TO` type, and the same endpoints in either direction before calling `EntryLinkRepository.delete(linkId)`.
- Do not modify `EntryLinkEntity`, `EntryLinkCommandDao`, `EntryLinkRepository`, Room schema version, or backup representation for unlink; this operation removes one already-persisted relationship record through the existing repository path.

- [x] **Step 1: Add failing unlink tests.** In `RelatedPasswordLinksTest`, add `unlinkDeletesSelectedSymmetricRelatedToLink`, `unlinkIsIdempotentWhenLinkIsAbsent`, `unlinkRejectsMismatchedLinkIdOrEndpoints`, `unlinkRejectsInactiveOrNonLoginEndpoints`, `unlinkLeavesUnrelatedLinkTypesUntouched`, and `unlinkLogsRepositoryFailureAndPropagatesCancellation`.
- [x] **Step 2: Run `RelatedPasswordLinksTest` and confirm the new unlink cases fail.**
- [x] **Step 3: Rename the service and test file; implement `unlink`.** Revalidate both endpoints using the existing active LOGIN query, resolve the exact link ID from `getLinks(sourceEntryId)`, verify its `RELATED_TO` type and symmetric source/target endpoints, then call existing `delete(linkId)`. If the requested edge is already absent, return success without deleting; if the same endpoints still have a link under a different ID, reject the stale request. Log failures once using the service's existing logger boundary; rethrow `CancellationException`.
- [x] **Step 4: Run `:app:testDebugUnitTest --tests "com.aozijx.passly.feature.vault.detail.RelatedPasswordLinksTest"` and commit** as `refactor(vault): centralize related password links`.

### Task 3: Own confirmation and unlink lifecycle in the Detail relationship workflow

**Files:**
- Rename `app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/DetailPasswordBindingWorkflow.kt` to `DetailPasswordRelationsWorkflow.kt`
- Rename `app/src/test/java/com/aozijx/passly/feature/vault/presentation/detail/DetailPasswordBindingWorkflowTest.kt` to `DetailPasswordRelationsWorkflowTest.kt`
- Rename the existing binding-state owner `DetailPasswordBindingUiState.kt` to `DetailPasswordRelationsUiState.kt` and keep binding/unlink state together there.
- Modify `app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/DetailUiAction.kt`
- Modify `app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/DetailUiState.kt`
- Modify `app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/DetailReducer.kt`
- Modify `app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/DetailViewModel.kt`
- Modify `app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/DetailPresentationLoader.kt`
- Modify `app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/ui/model/DetailUiModels.kt`
- Modify `app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/ui/model/DetailPresentationModels.kt`

**Interfaces:**
- Rename `DetailPasswordBindingWorkflow` to `DetailPasswordRelationsWorkflow`; retain candidate loading/bind behavior and add `requestUnlink(targetEntryId: EntryId, linkId: EntryLinkId)`, `dismissUnlink()`, and `confirmUnlink()`.
- Add `currentRelatedPasswords: () -> List<DetailRelatedPassword>` to the workflow so a swipe request must match the current Detail projection before confirmation can open. Use one `linkMutationJob: Job?` to serialize bind and unlink writes.
- Add `RelatedPasswordUnlinkUiState` in `DetailPasswordRelationsUiState.kt` with `Closed` and `Confirming(targetEntryId: String, linkId: String, targetTitle: String, phase: Phase)`; `Phase` is `Ready`, `Removing`, or `Failed`.
- Add `DetailUiAction.RequestPasswordUnlink(targetEntryId: String, linkId: String)`, `DismissPasswordUnlink`, and `ConfirmPasswordUnlink`; retry dispatches `ConfirmPasswordUnlink` again.
- Add `RelatedPasswordUiModel(linkId: String, targetEntryId: String, title: String, username: String)` in `DetailUiModels.kt`.

- [x] **Step 1: Add failing workflow tests** in `DetailPasswordRelationsWorkflowTest`: `requestedUnlinkCanBeDismissedWithoutDeleting`, `successfulUnlinkRefreshesCurrentRelations`, `inactiveTargetBeforeConfirmationRefreshesAndClosesDialog`, `failedUnlinkKeepsConfirmationRetryable`, `duplicateUnlinkConfirmationStartsOneDelete`, `unlinkResultIsIgnoredAfterPageReplacementOrSessionRevocation`, and `relationRefreshFailureIsLoggedAndLeavesRetryState`, and `unlinkRequestMustMatchCurrentProjectionAndSession`.
- [x] **Step 2: Run `DetailPasswordRelationsWorkflowTest` and confirm the new lifecycle cases fail.**
- [x] **Step 3: Rename and extend the workflow.** Keep one current-entry/session owner, serialize bind and unlink with `linkMutationJob`, disable repeated actions while `Removing`, and update state only while the captured `DetailWorkSession` token and authentication are current. Cancellation propagates. Pass the confirmed target and link ID to the service. After every completed unlink attempt, reload all relation projections; close on success or when the refreshed projection no longer contains that exact eligible link, otherwise keep the dialog in `Failed` for retry.
- [x] **Step 4: Log Detail projection reload failures** at the workflow boundary using `Logger.error(Events.Vault.Error, ErrorCodes.Security.VaultOperationFailed, Reasons.UnknownFailure, failure)`; do not log the already-logged service failure twice. Leave failed unlink confirmation open and retryable.
- [x] **Step 5: Run `:app:testDebugUnitTest` for `DetailPasswordRelationsWorkflowTest`, `DetailViewModelTest`, and `DetailReducerTest`; all 66 tests passed with 0 failures/errors. Commit** as `feat(vault): own password unlink lifecycle`.

### Task 4: Polish Detail relationship and picker UI

**Files:**
- Modify `app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/ui/component/RelatedEntriesSection.kt`
- Modify `app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/ui/component/RelatedPasswordPickerSheet.kt`
- Modify `app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/ui/DetailBody.kt`
- Modify `app/src/main/res/values/strings.xml`
- Modify `app/src/main/res/values-en/strings.xml`
- Add `app/src/androidTest/java/com/aozijx/passly/feature/vault/presentation/detail/ui/component/RelatedEntriesSectionTest.kt`
- Add `app/src/androidTest/java/com/aozijx/passly/feature/vault/presentation/detail/ui/component/RelatedPasswordPickerSheetTest.kt`

**Interfaces:**
- Extend `RelatedEntriesSection` to receive `RelatedEntryUiModel` account-group rows, `RelatedPasswordUiModel` direct-password rows, bind eligibility, and `RelatedPasswordUnlinkUiState`; dispatch existing open/bind actions plus the new request/dismiss/confirm actions.
- Keep the shared `SwipeActionContainer` API unchanged. Direct-password rows supply its left action with the existing theme's error-container colors and an existing link-removal icon; crossing its swipe threshold opens confirmation because the container invokes the action instead of remaining open.

- [x] **Step 1: Add Compose UI tests.** In `RelatedEntriesSectionTest`, add `swipingDirectPasswordRequestsConfirmation`, `dismissingConfirmationLeavesRelationAndRow`, `confirmingAndRetryingUnlinkDispatchesActions`, `accountGroupAndDirectRelationRenderSeparately`, `accountGroupRowHasNoUnlinkAccessibilityAction`, and `accessibilityCustomActionRequestsConfirmation`. In `RelatedPasswordPickerSheetTest`, add `emptyCandidatesAndNoSearchMatchesHaveDifferentMessages`, `loadFailureOffersRetry`, and `activeBindShowsProgressOnItsRow`.
- [ ] **Step 2: Run each targeted class using `:app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=<fully-qualified-class>` on an available emulator/device and confirm the new interactions fail.** Pending: no device is attached and no AVD is configured.
- [x] **Step 3: Refine the existing section.** Keep account-group and other relation rows navigable; render direct password links in a distinct titled group with title, username, trailing navigation, compact header bind action, and a concise icon-based empty state. Use the shared swipe container to open—not execute—the unlink confirmation after the swipe threshold.
- [x] **Step 4: Implement the confirmation dialog and picker states.** Name the target, state that the link alone is removed and both saved entries remain, support cancel/confirm, disable dismissal and repeat confirmation while removal is active, show removal progress, and keep retry feedback in the dialog. Preserve distinct picker states for loading, no candidates, no search matches, load failure/retry, ready, and bind progress.
- [x] **Step 5: Add Chinese and English strings and the row accessibility custom action; use existing Material 3 components only.**
- [ ] **Step 6: Run the targeted Compose test and `:app:testDebugUnitTest --tests "com.aozijx.passly.feature.vault.presentation.detail.DetailUiMapperTest"`, then commit** as `feat(vault): add confirmed password unlink UI`.

### Task 5: Close the documentation and verification batch

**Files:**
- Update `docs/features/vault/entry-editor-and-persistence.md` with the user-visible Detail relationship contract.
- Update `docs/superpowers/plans/2026-11-07-related-password-binding.md` to remove its stale unchecked binding test items and point to the current relationship behavior.
- Update Task 35 in `docs/superpowers/plans/2026-10-30-passly-maintainability-todo.md` with implementation and verification evidence; remove the temporary “design pending review” marker.

- [x] **Step 1: Reconcile the old binding plan with current tests.** `DetailPasswordRelationsWorkflowTest` covers bind-failure retry, success refresh, and duplicate-tap suppression; retain the Room upsert endpoint-deleted-during-write acceptance as an open item in Task 35.
- [x] **Step 2: Transfer the durable binding/unlink user contract** to `entry-editor-and-persistence.md`; archive the old plan with links to that contract and this plan.
- [x] **Step 3: Run `:app:testDebugUnitTest`, `:data:testDebugUnitTest`, `verifyArchitecture`, and `git diff --check`.** App: 1085 tests, 0 failures/errors. Data: 170 tests, 0 failures/errors. Architecture and diff checks passed. Compose instrumentation was not run because no device or AVD is available.
- [x] **Step 4: Review the final diff and staged paths** to ensure the shared `SwipeActionContainer.kt`, unlock/authentication changes, `AGENTS.md`, `.agents/`, and other unrelated user work are excluded.
- [x] **Step 5: Commit the documentation and final TODO evidence** as `docs(vault): record related password management`.
