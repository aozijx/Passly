# Device Lock Task Clear Delivery Record

状态：已完成。本文保留实现任务与验收记录；当前行为以[认证与设备锁主题](../security/authentication.md)为准。

**Goal:** Deliver a flip-triggered task-clear request to the current Passly Activity exactly once, cancel it on successful reauthentication, and prevent stale delayed callbacks from closing a newer interaction.

**Architecture:** Replace the lossy SharedFlow signal with an app-scoped coordinator that owns one pending request, a current resumed-Activity owner, and a claim/consume contract. `DeviceLockController` creates requests only after eligible flip locks and forwards authentication changes; each Android Activity registers and manages its own delayed close callback.

**Tech Stack:** Kotlin, Android Activity lifecycle, StateFlow, coroutine tests.

**设计记录：** [设备锁任务清理设计](2026-11-02-device-lock-task-clear-design-record.md)

## Global Constraints

- Only a successful FLIP lock with both flip settings enabled creates a clear-task request.
- SCREEN_OFF never creates a clear-task request.
- Successful `Authenticated` or `AutofillAuthenticated` state cancels pending and delayed requests.
- Pending requests remain process-local and are consumed at most once.
- Activity registration/release is bound to onResume/onPause; unconsumed requests transfer during Activity recreation.
- Run Gradle tasks serially with `--no-parallel --max-workers=2`.

## Review Focus

- SCREEN_OFF must never clear the task, even when both flip settings are true.
- A request sent before an Activity resumes remains claimable while locked, but successful authentication invalidates it.
- A replacement owner must invalidate the old claim; an old delayed callback must not finish the replacement Activity/task.
- A delivery or 1-second delay that expires during an unsuccessful authentication attempt must be retried when the session returns to Locked; successful authentication cancels it.
- Claim release during Activity recreation must not lose the pending request or deliver it twice.
- Credential Manager, Legacy Autofill, Accessibility Autofill, and MainActivity all use the same exact lifecycle owner contract.

---

### Task 1: Flip-only eligibility and request coordinator

**Files:** `app/src/main/java/com/aozijx/passly/app/DeviceLockController.kt`, `app/src/main/java/com/aozijx/passly/app/ActiveTaskClearRequests.kt`, `app/src/test/java/com/aozijx/passly/app/DeviceLockHandlerTest.kt`, and a focused coordinator test in `app/src/test/java/com/aozijx/passly/app/`.

**Interfaces:** Replace the untyped event with `TaskClearRequestId`, `TaskClearOwnerId`, and `TaskClearClaim`. `ActiveTaskClearRequests.requestClear(): TaskClearRequestId?` creates a pending request only while authentication state is `Locked`; `registerOwner(ownerId, onRequest, onCancelled)` assigns the current owner and delivers an unclaimed request; `releaseOwner(ownerId)` releases its claim; `isCurrent(claim): Boolean` and `consume(claim): Boolean` validate request and owner identity. `onAuthenticationStateChanged(state)` cancels pending work on `Authenticated` or `AutofillAuthenticated`.

- [x] Add failing tests proving `SCREEN_OFF` does not request task clear while flip settings are enabled and FLIP still does when both are enabled.
- [x] Add failing tests for request-before-owner retention, one-shot consume, owner replacement/release, old-claim rejection, and successful-authentication cancellation.
- [x] Run focused tests and confirm they fail for the missing coordinator contract.
- [x] Implement the coordinator and update `DeviceLockHandler` eligibility; have `DeviceLockController` request only after successful eligible FLIP lock and feed authentication state to the coordinator.
- [x] Run focused tests, `:app:ktlintCheck`, `verifyArchitecture`, and `git diff --check`.

### Task 2: Bind the current Activity and its delayed callback

**Files:** Create `app/src/main/java/com/aozijx/passly/app/TaskClearActivityOwner.kt`; modify `app/src/main/java/com/aozijx/passly/presentation/MainActivity.kt`, `app/src/main/java/com/aozijx/passly/feature/autofill/presentation/credential/CredentialResponseActivity.kt`, `app/src/main/java/com/aozijx/passly/feature/autofill/presentation/legacy/AutofillFillActivity.kt`, and `app/src/main/java/com/aozijx/passly/feature/autofill/presentation/accessibility/AccessibilityFillActivity.kt`; add `app/src/test/java/com/aozijx/passly/app/TaskClearActivityOwnerTest.kt`.

**Interfaces:** `TaskClearActivityOwner.onResumed()` registers its Activity-specific owner token and `onPaused()` cancels callbacks and releases it. The helper receives `TaskClearClaim`, posts the existing 1-second delay, and calls `consume(claim)` immediately before `finishAndRemoveTask()`; its cancellation callback removes the pending Runnable for that request.

- [x] Add tests for owner recreation reclaiming an unconsumed request and old owner callback cancellation. The owner uses an injected callback scheduler, so these lifecycle races are deterministic JVM tests.
- [x] Add Activity lifecycle wiring for all four Activity types and remove the MainActivity-only Flow collector.
- [x] Run focused app tests and `:app:compileDebugAndroidTestKotlin`.

### Task 3: Document and verify the full contract

**Files:** `docs/security/authentication.md`, `docs/superpowers/plans/2026-10-30-passly-maintainability-todo.md`, and this plan.

- [x] Update authentication/device-lock behavior documentation, including the screen-off distinction, current Activity ownership, pending request, reauthentication cancellation, and process-death behavior.
- [x] Mark Task 28 items complete only when backed by tests; note that a device is connected but device behavior has not been run.
- [x] Run focused `DeviceLockHandlerTest`/coordinator tests, App KtLint, `verifyArchitecture`, `:app:compileDebugAndroidTestKotlin`, and `git diff --check` serially.
- [x] Review the complete diff, stage only Task 28 paths, and commit after review.

## Review Resolution

- P2 fixed: a delivery or delayed close that encounters a transient authentication state previously left its claim pending but inert. On return to `Locked`, the coordinator now re-delivers the still-pending claim; the Activity owner deduplicates a delivery/timer already in flight. Added regressions for both a delayed callback and initial delivery encountering `AwaitingHost`. The regression was observed failing before the fix and passing after it.
- Device-specific behavior was not run. `adb devices` reports a connected device, but no APK was installed or launched for this task.
