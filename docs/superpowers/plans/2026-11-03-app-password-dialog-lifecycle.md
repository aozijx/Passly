# App Password Dialog Lifecycle Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Clear Security page App Password dialog drafts on navigation away and Activity recreation, and shorten the lifetime of coordinator-owned password material.

**Architecture:** Keep `AppPasswordDialogCoordinator` scoped to the `SecurityRoute` composition. Store its three owned drafts in wipeable `CharArray` holders, clear copies on replacement/dismiss/submit/dispose, and leave ViewModel request-array ownership unchanged. Bind coordinator cleanup to the same route composition lifetime and retain the existing page-visit owner checks for late effects.

**Tech Stack:** Kotlin, Jetpack Compose runtime, Android Compose instrumentation tests, JUnit, Gradle.

**Spec:** `docs/superpowers/specs/2026-11-03-app-password-dialog-lifecycle-design.md`

## Global Constraints

- SecurityRoute dispose, including Activity configuration recreation, clears all coordinator drafts and closes its active dialog.
- A replacement SecurityRoute starts with a new empty coordinator; drafts are not restored.
- Compose password inputs remain `String`-based; clearing owned arrays does not claim to erase immutable String copies already made by Compose or the platform.
- ViewModel submitted request arrays remain independently owned and cleared by existing completion/rejection paths.
- Preserve existing busy-operation and page-visit owner guards; do not cancel an already accepted settings write merely because the Route leaves composition.
- Preserve all pre-existing worktree changes and stage only this plan's implementation files if a commit is explicitly requested.

## Review Focus

- Replacement of a field must zero the previous owned buffer before taking ownership of the new one; a focused holder test must retain and inspect the previous array.
- Submit must create independent request arrays and zero coordinator arrays immediately; assert both request contents and coordinator-visible state.
- Action-dialog dismissal must clear stale Change drafts as well as close the active dialog; cover all dismissal events.
- Late field-change or confirm callbacks after dismissal must not recreate drafts or issue requests; verify the active-dialog guard.
- Busy input/submission/dismissal must remain rejected without changing the active state or accepting a new draft; preserve the existing test.
- Route removal and configuration-style dispose/remount must clear the old coordinator, while a new instance is empty; verify through the production Compose lifecycle binding used by SecurityRoute.

---

### Task 1: Wipe App Password drafts with the SecurityRoute lifecycle

**Files:**
- Modify: `app/src/main/java/com/aozijx/passly/feature/settings/presentation/security/AppPasswordDialogCoordinator.kt`
- Create: `app/src/main/java/com/aozijx/passly/feature/settings/presentation/security/SensitivePasswordDraft.kt`
- Modify: `app/src/main/java/com/aozijx/passly/feature/settings/presentation/main/navigation/core/SecurityRoute.kt`
- Test: `app/src/test/java/com/aozijx/passly/feature/settings/presentation/security/AppPasswordDialogCoordinatorTest.kt`
- Test: `app/src/test/java/com/aozijx/passly/feature/settings/presentation/security/SensitivePasswordDraftTest.kt`
- Test: `app/src/androidTest/java/com/aozijx/passly/feature/settings/presentation/security/AppPasswordDialogCoordinatorLifecycleTest.kt`
- Modify: `docs/security/authentication.md`
- Modify: `docs/superpowers/specs/2026-11-03-app-password-dialog-lifecycle-design.md`
- Modify: `docs/superpowers/plans/2026-10-30-passly-maintainability-todo.md`

**Interfaces:**
- Consumes: existing `AppPasswordDialogCoordinator`, `AppPasswordChangeRequest`, ViewModel page-owner lifecycle, and Compose `DisposableEffect` in `SecurityRoute`.
- Produces: `AppPasswordDialogCoordinator.clear()`; `SensitivePasswordDraft.replace(ownedValue: CharArray)`, `snapshot(): String`, `copy(): CharArray`, and `clear()`; `AppPasswordDialogCoordinatorOwner(coordinator)` as the Compose disposal binding used by `SecurityRoute` and the instrumentation test.

- [x] **Step 1: Write failing unit tests**

Add tests proving the draft holder zeros an owned array on replacement and clear; both Set and Change submits return independent request arrays while clearing their owned draft state; dismissing Action/Set/Change clears all drafts and closes the dialog; and explicit route cleanup is idempotent. Extend the busy-operation test to reject field changes as well as submit/dismiss events.

- [x] **Step 2: Run the focused unit tests and verify they fail for the missing behavior**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.aozijx.passly.feature.settings.presentation.security.AppPasswordDialogCoordinatorTest" --tests "com.aozijx.passly.feature.settings.presentation.security.SensitivePasswordDraftTest" --no-parallel --max-workers=2`

Expected: FAIL because the wipeable holder/clear API and submit/dismiss cleanup behavior do not yet exist.

- [x] **Step 3: Implement wipeable coordinator draft ownership**

Add the internal holder and make the coordinator replace owned arrays safely, expose String snapshots only for the existing text-field model, copy separate request arrays on confirmation, then clear its own buffers. Make all dismissal paths and `clear()` close the dialog and wipe all fields. Keep busy guards ahead of event handling.

- [x] **Step 4: Run unit tests and verify they pass**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.aozijx.passly.feature.settings.presentation.security.AppPasswordDialogCoordinatorTest" --tests "com.aozijx.passly.feature.settings.presentation.security.SensitivePasswordDraftTest" --no-parallel --max-workers=2`

Expected: PASS, including zero-filled prior buffers and independently owned request arrays.

- [x] **Step 4a: Add failing tests for late input and confirm callbacks after dismissal**

After closing Set, send delayed field-change and confirm events. Assert the dialog stays closed, every snapshot remains empty, and no request is emitted.

- [x] **Step 4b: Implement active-dialog event guards and rerun focused unit tests**

Accept Set input/confirmation only while Set is active, Change input/confirmation only while Change is active, and Change/Disable actions only while Action is active. Re-run Coordinator and draft-holder tests; all must pass.

- [x] **Step 5: Write the failing Compose lifecycle test**

Add a Compose test that mounts the production `AppPasswordDialogCoordinatorOwner` binding with populated drafts, removes that route owner, and verifies the old coordinator is empty/closed. Remount with a new coordinator to represent Activity recreation and verify the replacement begins empty. The test must invoke the same production lifecycle binding that `SecurityRoute` uses.

- [x] **Step 6: Run the lifecycle test and verify the missing binding is exposed**

Run: `.\gradlew.bat :app:compileDebugAndroidTestKotlin --no-parallel --max-workers=2`

Expected: FAIL because `AppPasswordDialogCoordinatorOwner` is not yet defined. The test was first prototyped with the unit-test `Composition` API, but the Android local-test runtime calls unmocked `android.util.Log` on composition failure; the lifecycle test therefore uses the existing instrumentation Compose rule. `adb devices` currently reports no connected device and no Android emulator executable/AVD is available, so behavioral instrumentation cannot run in this environment.

- [x] **Step 7: Bind cleanup to SecurityRoute and update security documentation**

Have `SecurityRoute` own one remembered coordinator before its page `DisposableEffect` and mount `AppPasswordDialogCoordinatorOwner` while the route is composed. This binding clears the coordinator on both navigation away and configuration recreation; the page effect continues exiting both ViewModel owners. Document navigation/configuration-recreation clearing, immutable String limits, and the separate ViewModel request-buffer lifetime in `docs/security/authentication.md`.

- [x] **Step 8: Run focused unit tests, compile the lifecycle test, and attempt instrumentation**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.aozijx.passly.feature.settings.presentation.security.AppPasswordDialogCoordinatorTest" --tests "com.aozijx.passly.feature.settings.presentation.security.SensitivePasswordDraftTest" --tests "com.aozijx.passly.feature.settings.presentation.security.AppPasswordSettingsViewModelTest" --no-parallel --max-workers=2`

Run: `.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.aozijx.passly.feature.settings.presentation.security.AppPasswordDialogCoordinatorLifecycleTest --no-parallel --max-workers=2`

Expected: Unit tests PASS and Android-test sources compile. The instrumentation attempt is recorded but runtime remains unverified: no device/emulator is attached, and task configuration fails while applying Protobuf plugin 0.10.0 from the current worktree's modified version catalog. Do not claim route disposal/recreation behavior was executed on device.

- [x] **Step 9: Update task evidence and run final gates**

Mark Task 29 complete with code paths and verification results. Run `.\gradlew.bat verifyArchitecture --no-parallel --max-workers=2` and `git diff --check`.

- [x] **Step 10: Review, then commit only this batch**

Obtain an independent code review of the exact Task 29 diff, fix any correctness findings with regression tests, rerun affected gates, then stage only the files listed above and commit as `fix(settings): clear app password drafts on route exit`.
