# Detail Password Binding Implementation Plan

> **For agentic workers:** Use this plan task-by-task in the current session. Each task has an independent test cycle and commit.

**Goal:** Let an active saved LOGIN entry bind another active saved LOGIN entry from Detail using the existing symmetric relationship contract.

**Architecture:** The Data query adapter returns non-sensitive active `EntryListItem` summaries by type. A Detail interactor filters current and already related entries and validates both saved endpoints before delegating to `EntryLinkRepository`. Detail presentation owns picker state and user feedback; existing `EntryLink` persistence and account grouping remain authoritative.

**Tech Stack:** Kotlin, Room, Hilt, Coroutines/Flow, Jetpack Compose, JUnit.

**Spec:** [2026-11-07-related-password-binding-design.md](../specs/2026-11-07-related-password-binding-design.md)

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
- [ ] Commit `feat(vault): query and bind saved password entries`.

**Task 1 evidence (2026-11-07):** `RelatedPasswordBindingTest` first failed to compile because the new interactor and `EntryListQueryRepository.getActiveByType` API were absent. The implementation now returns `EntryListItem` summaries from the active-by-type DAO without reading secret bundles, filters self/related candidates, revalidates active LOGIN endpoints before writing, canonicalizes symmetric `RELATED_TO` links, and treats an existing relation as idempotent. The four focused App tests pass; `:data:testDebugUnitTest` and `verifyArchitecture` pass; `git diff --check` passes.

### Task 2: Include direct related-password links in Detail loading

**Files:**
- Modify `domain/src/main/kotlin/com/aozijx/passly/domain/entry/port/EntryQueryRepository.kt`
- Modify `data/src/main/java/com/aozijx/passly/data/repository/entry/RoomEntryQueryRepository.kt`
- Modify `app/src/main/java/com/aozijx/passly/feature/vault/detail/DetailSessionQuery.kt`
- Update `EntryQueryRepository` fakes and Detail session tests

**Interfaces:**
- Add `suspend fun getByIds(entryIds: List<EntryId>): List<Entry>`; an empty input returns immediately without opening a database session. Data performs one full-vault read session and reorders found entries to match the requested IDs.

- [ ] Test empty input, result ordering, missing entries, mixed ACCOUNT and RELATED_TO relation resolution, and preservation of existing typed grouping relations.
- [ ] Run focused tests to establish the missing batch behavior.
- [ ] Load related entries through one batch query; merge ACCOUNT group members and direct RELATED_TO neighbors in stable order without duplicates.
- [ ] Run Detail and Data query tests, `verifyArchitecture`, and `git diff --check`.
- [ ] Commit `refactor(vault): batch related entry loading`.

### Task 3: Add the Detail binding action and picker

**Files:**
- Modify Detail state/action/owner files under `app/src/main/java/com/aozijx/passly/feature/vault/presentation/detail/`
- Modify `DetailTopBar.kt` or `RelatedEntriesSection.kt` and add one focused picker sheet component
- Modify `DetailRoute.kt` and the affected UI/ViewModel tests
- Modify `app/src/main/res/values/strings.xml` and `values-en/strings.xml`

- [ ] Add a Detail action visible only for active saved LOGIN entries and picker states for loading, candidates, saving, empty, and retryable failure.
- [ ] Test page-owner replacement, session revocation, picker dismissal, bind success/failure/cancellation, duplicate prevention, and source/target changes while work is suspended.
- [ ] Add a searchable lazy candidate sheet that shows title/username summaries, never credential fields, with localized labels and explicit bind affordance.
- [ ] On success, refresh the current Detail relationship projection; ordinary failures are logged and shown without closing the retry path.
- [ ] Run the focused Detail UI/ViewModel tests, full `:app:testDebugUnitTest`, `:data:testDebugUnitTest`, `verifyArchitecture`, and `git diff --check`.
- [ ] Commit `feat(vault): bind saved passwords from detail`.
