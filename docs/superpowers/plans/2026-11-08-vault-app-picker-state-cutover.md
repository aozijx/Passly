# Vault App Package Picker State Cutover Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Complete the in-progress Vault app picker state migration across Detail and Password editor, removing legacy booleans and adapters while preserving retry, cancellation, and stale-result behavior.

**Architecture:** Keep each page's async directory request in its existing ViewModel. Share only the immutable `AppPackagePickerUiState<T>` projection/search contract and the stateless BottomSheet. Use exhaustive state branches instead of list plus loaded/failed flags.

**Tech Stack:** Kotlin, Android ViewModel, Jetpack Compose, JUnit, kotlinx.coroutines test.

**Spec:** `docs/superpowers/specs/2026-11-08-vault-app-picker-state-design.md`; umbrella goal: `docs/superpowers/specs/2026-11-08-vault-list-boundaries-design.md`.

## Global Constraints

- Preserve the current Vault worktree and index; do not stage or commit.
- Keep `NotRequested`, `Loading`, `Ready`, and `Failed` mutually exclusive.
- Preserve Cancelled and fatal `Error` propagation; only ordinary `Exception` becomes recoverable `Failed`.
- Reject results after authentication/page/entry owner replacement or ViewModel clearing.
- Do not retain old loaded/failed flags, list-only adapters, or compatibility getters after all callers migrate.

## Review Focus

- A regular directory exception must publish retryable `Failed`, then a later request must reach `Ready`.
- Cancellation must not be mapped to `Failed`; fatal `Error` must escape.
- A result from an old authentication or Detail entry must not replace the current picker state.
- Empty catalog and non-empty catalog with no search matches must render distinct result states.
- Both Detail and Password callers must send query changes to their own state owner; no BottomSheet-local duplicate query remains.

---

### Task 1: Prove and complete the shared picker state contract

**Files:**
- Create: `app/src/test/java/com/aozijx/passly/feature/vault/presentation/common/AppPackagePickerUiStateTest.kt`
- Modify: `app/src/main/java/com/aozijx/passly/feature/vault/presentation/common/AppPackagePickerUiState.kt`
- Modify: `app/src/main/java/com/aozijx/passly/feature/vault/presentation/editor/password/AddPasswordContract.kt`
- Modify: `app/src/main/java/com/aozijx/passly/feature/vault/presentation/editor/ui/password/AddPasswordEditorScreen.kt`
- Test: `app/src/test/java/com/aozijx/passly/feature/vault/presentation/editor/password/AddPasswordViewModelTest.kt`
- Test: `app/src/test/java/com/aozijx/passly/feature/vault/presentation/detail/DetailReducerTest.kt`
- Test: `app/src/test/java/com/aozijx/passly/feature/vault/presentation/detail/DetailViewModelTest.kt`

**Interfaces:**
- Consumes: `AppPackagePickerUiState<T>`, `AppPackagePickerResults<T>`, `appPackagePickerReady(apps, label, packageName)`, `Ready<T>.search(query, label, packageName)`, and `Ready<T>.mapItems(transform)`.
- Produces: the single picker state contract for Detail and Password; all current consumers read `Ready.apps` or `Ready.results` directly and use exhaustive branches for loading/failure/empty presentation.

- [x] **Step 1: Add pure state contract tests**

  In `AppPackagePickerUiStateTest`, cover: empty app list creates `Ready` with `NoApps`; whitespace query preserves all apps as `Matches`; label and package-name search are case-insensitive and retain the raw query; unmatched query produces `NoMatches`; `mapItems` preserves query and maps both full apps and visible matches.

- [x] **Step 2: Run the pure state tests**

  Run: `./gradlew :app:testDebugUnitTest --tests "com.aozijx.passly.feature.vault.presentation.common.AppPackagePickerUiStateTest" --no-parallel --max-workers=2`

  Expected: PASS. Correct the shared state helper only if these contract assertions expose a mismatch.

- [x] **Step 3: Remove the Password list compatibility projection**

  Delete `AddPasswordUiState.availableAppItems`. In `AddPasswordEditorScreen`, derive the selected item directly from `state.availableApps as? AppPackagePickerUiState.Ready` and its `apps`; preserve null selection when the picker has not reached Ready.

- [x] **Step 4: Migrate Detail and Password behavior tests to sealed states**

  Replace assertions against `availableAppsLoaded`, `packagePickerAppsLoaded`, `packagePickerAppsLoadFailed`, and bare list values with exhaustive-state assertions. Cover locked request as `NotRequested`, successful and retried requests as `Ready`, ordinary failure as `Failed`, and replacement/cancellation behavior as not publishing stale `Ready`/`Failed` results. Keep the existing fatal-Error identity assertions.

- [ ] **Step 5: Run all affected ViewModel and Reducer tests**

  Run: `./gradlew :app:testDebugUnitTest --tests "com.aozijx.passly.feature.vault.presentation.common.AppPackagePickerUiStateTest" --tests "com.aozijx.passly.feature.vault.presentation.editor.password.AddPasswordViewModelTest" --tests "com.aozijx.passly.feature.vault.presentation.detail.DetailReducerTest" --tests "com.aozijx.passly.feature.vault.presentation.detail.DetailViewModelTest" --no-parallel --max-workers=2`

  Expected: PASS; confirm the XML reports contain all four classes.

- [ ] **Step 6: Verify production caller closure and Android source compilation**

  Search production and test sources for `availableAppsLoaded`, `packagePickerAppsLoaded`, `packagePickerAppsLoadFailed`, and `availableAppItems`; expected: no remaining references. Run `./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin verifyArchitecture --no-parallel --max-workers=2` and `git diff --check`.

  Expected: both Kotlin source sets and architecture checks pass; no whitespace errors. Do not stage or commit.
