# Detail Password Binding Implementation Plan

> **For agentic workers:** Use this plan task-by-task in the current session. Each task has an independent test cycle and commit.

**Goal:** Let an active saved LOGIN entry bind another active saved LOGIN entry from Detail using the existing symmetric relationship contract.

**Architecture:** The Data query adapter returns non-sensitive active `EntryListItem` summaries by type. A Detail interactor filters current and already related entries and validates both saved endpoints before delegating to `EntryLinkRepository`. Detail presentation owns picker state and user feedback; existing `EntryLink` persistence and account grouping remain authoritative.

**Tech Stack:** Kotlin, Room, Hilt, Coroutines/Flow, Jetpack Compose, JUnit.

**Spec:** [2026-11-07-related-password-binding-design.md](../specs/2026-11-07-related-password-binding-design.md)

> **Archived on 2026-11-08.** The durable user contract is now in [entry-editor-and-persistence.md](../../features/vault/entry-editor-and-persistence.md#详情页密码关联), and the active implementation/verification plan is [2026-11-08-related-password-management.md](2026-11-08-related-password-management.md). This file records the earlier binding implementation; its task descriptions are historical. The Room upsert race where an endpoint disappears during the write remains open under [maintainability TODO Task 35](2026-10-30-passly-maintainability-todo.md#task-35).

## Global Constraints

- Bind only active persisted `EntryType.LOGIN` entries through `EntryRelationType.RELATED_TO`.
- Do not read password plaintext to populate or validate picker candidates.
- Preserve repository transaction, revision snapshot, attachment cleanup, ACCOUNT grouping, and existing storage contracts.
- Keep cancellation observable, log ordinary failures without entry or credential contents, and bind results to the current Detail entry/session owner.
- Keep local `AGENTS.md` and `.agents/` unstaged.

## Review Focus

- Entry turns trashed or changes session while the picker or bind request is suspended: cancel/ignore stale results and never write after access revocation.
- Duplicate or reversed selection: keep one canonical symmetric relationship and avoid a second revision write.
- Link target disappears after selection: return a visible, retryable failure and keep current Detail state valid.
- Existing ACCOUNT, OTP, and recovery graph links: retain current projection and add RELATED_TO neighbors without duplicating items.
- Large candidate set: render a searchable lazy list without loading credential secrets.

---

### Task 1: Query and bind saved password summaries

**Files:**
- Modify `domain/src/main/kotlin/com/aozijx/passly/domain/entry/port/EntryListQueryRepository.kt`
- Modify `data/src/main/java/com/aozijx/passly/data/repository/entry/RoomEntryListQueryRepository.kt`
- Modify `app/src/main/java/com/aozijx/passly/feature/vault/detail/` with a focused interactor and tests
- Update the existing EntryListQueryRepository test fakes

**Interfaces:**
- Add `suspend fun getActiveByType(entryType: EntryType): List<EntryListItem>`; the Data implementation uses the existing active-by-type DAO and `EntryListItemMapper`.
- Add a Detail interactor that loads active LOGIN summaries, excludes self and existing direct RELATED_TO links, and binds only when both IDs still appear in the active LOGIN result.

- [x] Write tests proving active LOGIN-only candidates, self/existing-link exclusion, no plaintext reads, endpoint revalidation, and duplicate handling.
- [x] Run the interactor/query focused tests and confirm the new cases fail for the missing behavior.
- [x] Implement the summary query and bind interactor by reusing `EntryLinkRepository` and `EntryLink.create()`.
- [x] Run affected App/Data tests, `verifyArchitecture`, and `git diff --check`.
- [x] Commit `feat(vault): query and bind saved password entries` (`f790b5fac`).

**Task 1 evidence (2026-11-07):** `RelatedPasswordBindingTest` first failed to compile because the new interactor and `EntryListQueryRepository.getActiveByType` API were absent. The implementation now returns `EntryListItem` summaries from the active-by-type DAO without reading secret bundles, filters self/related candidates, revalidates active LOGIN endpoints before writing, canonicalizes symmetric `RELATED_TO` links, and treats an existing relation as idempotent. The four focused App tests pass; `:data:testDebugUnitTest` and `verifyArchitecture` pass; `git diff --check` passes.

### Task 2: Include direct related-password links in Detail loading

**Files:**
- Modify `domain/src/main/kotlin/com/aozijx/passly/domain/entry/port/EntryQueryRepository.kt`
- Modify `data/src/main/java/com/aozijx/passly/data/repository/entry/RoomEntryQueryRepository.kt`
- Modify `app/src/main/java/com/aozijx/passly/feature/vault/detail/DetailSessionQuery.kt`
- Update `EntryQueryRepository` fakes and Detail session tests

**Interfaces:**
- Add `suspend fun getByIds(entryIds: List<EntryId>): List<Entry>`; an empty input returns immediately without opening a database session. Data performs one full-vault read session and reorders found entries to match the requested IDs.

- [x] Add tests for mixed ACCOUNT and RELATED_TO resolution, batch invocation and stable order; Room query implementation handles empty input and missing rows without opening extra sessions.
- [x] The existing related-ID focused test passed during implementation.
- [x] Run the mixed session test in the detached verification worktree at `a1e20e5f6`; it passed as part of the full App suite (1035 tests, 0 failures).
- [x] Load related entries through one batch query; merge ACCOUNT group members and direct RELATED_TO neighbors in stable order without duplicates.
- [x] Run Data unit tests, production App compilation, `verifyArchitecture`, and `git diff --check`.
- [x] Commit `refactor(vault): batch related entry loading` (`d73fba54b`).

### Task 3: Add the Detail binding action and picker

**Files:**
- Modify Detail state/action/owner files under `app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/`
- Modify `DetailTopBar.kt` or `RelatedEntriesSection.kt` and add one focused picker sheet component
- Modify `DetailRoute.kt` and the affected UI/ViewModel tests
- Modify `app/src/main/res/values/strings.xml` and `values-en/strings.xml`

- [x] Add a Detail action visible only for active saved LOGIN entries and picker states for loading, candidates, saving, empty, and retryable failure.
- [x] Test candidate results after same-entry page replacement, session revocation, and picker dismissal; stale requests do not publish candidates.
- [x] Test binding write cancellation when replacing the Detail page; the in-flight link write is cancelled before persistence.
- [x] `RelatedPasswordLinksTest` covers valid saved endpoints, rejects an unsaved endpoint, and makes an existing relation idempotent.
- [x] `DetailPasswordRelationsWorkflowTest` covers bind-failure retry, success refresh, and suppression of duplicate in-flight taps. The separate Room upsert endpoint-deleted-during-write acceptance remains open in Task 35.
- [x] Add a searchable lazy candidate sheet that shows title/username summaries, never credential fields, with localized labels and explicit bind affordance.
- [x] On success, refresh only the current Detail relationship projection; ordinary failures are logged and shown without closing the retry path.
- [x] Run the focused Detail tests through the full `:app:testDebugUnitTest` suite, `:data:testDebugUnitTest`, `verifyArchitecture`, and `git diff --check`; App passed 1037 tests and Data passed 168 tests, with no failures. Data was up-to-date in the latest run; its test report is from the preceding full execution.
- [x] Commit `feat(vault): bind saved passwords from detail`.

**Superseded follow-up:** Unlink behavior, its exact-link deletion contract, confirmation UI, session ownership, and current test names are maintained only in the active plan linked above; do not extend this historical plan with another implementation path.
