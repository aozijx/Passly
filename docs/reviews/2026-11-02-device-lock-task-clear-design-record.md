# Device Lock Task Clear Design Record

**Date:** 2026-11-02
**Status:** Implemented; archived 2026-11-06

## Intent

Make the optional “lock on device flip and clear the task” behavior close the task that is actually in front, including Autofill activities, without dropping commands during Activity transitions or replaying an old close after authentication resumes.

## Goals

- A task-clear request is created only after a successful `FLIP` lock when both flip-lock and clear-task settings are enabled.
- The currently resumed Passly Activity owns delivery. This includes `MainActivity`, Credential Manager response, Legacy Autofill, and Accessibility Autofill activities.
- A request made while there is no resumed owner remains pending in process memory and can be claimed once by the next owner while the session remains locked.
- Activity recreation releases an unconsumed claim so the replacement Activity can claim the same request.
- Successful authentication cancels the pending request and any delayed close. The 1-second reminder delay must recheck request identity, owner identity, and locked session state before finishing the task.
- Each request is consumed at most once. Process death does not persist/replay a task-clear request.

## Non-goals

- Do not change which device events lock the Vault or the screen-off lock policy.
- Do not clear a task for `SCREEN_OFF`, even when the flip settings are enabled.
- Do not close a task when a flip arrives while already locked and the handler skips the request.
- Do not add a generic event-bus replay buffer or persist pending task commands.

## Behavior contract

1. `DeviceLockHandler` returns `shouldClearTask = true` only for a successful `FLIP` lock when both `isFlipToLockEnabled` and `isFlipExitAndClearStackEnabled` are true.
2. `DeviceLockController` creates a uniquely identified, process-local request after the lock operation completes.
3. The task-clear coordinator keeps at most one pending request. A new resumed Activity becomes the current owner; the prior owner is invalidated. An Activity claims the request once and holds a claim until it consumes the request or releases ownership.
4. No current owner means the request remains pending. Authentication state changing to `Authenticated` or `AutofillAuthenticated` clears it. `AwaitingHost`, `Authenticating`, and recovery states do not count as successful reauthentication.
5. When a failed or cancelled authentication attempt returns the session to `Locked`, the coordinator redelivers its still-pending claim to the current owner. A delivery or delay that expires while the session is temporarily outside `Locked` cannot consume the request; it is resumed after lock returns.
6. The current Activity immediately shows the existing close reminder and schedules the existing 1-second delay. On expiry it calls `consume(claim)`; consumption succeeds only when the same request and owner are current and the Vault remains locked. Only then does it call `finishAndRemoveTask()`.
7. `onPause` releases the Activity claim and cancels its delayed callback. A replacement Activity can then claim the pending request. Successful authentication invalidates the claim even if an old callback still runs.
8. If process death occurs, the in-memory request is lost; the task and its Activity stack are already gone, so there is no stale command to restore.

## Approaches

- **Chosen: one process-level current-Activity owner with claim/consume.** Activities register while resumed. A single coordinator retains one pending command, invalidates former owners, allows a replacement owner to reclaim an unconsumed request, and cancels it on successful authentication. This makes delivery task-aware without replaying an already consumed command.
- **Rejected: keep a non-replaying `SharedFlow`.** It drops a request when no Activity is collecting and the only current collector is `MainActivity`, so Autofill task owners cannot close themselves.
- **Rejected: add replay to the existing `SharedFlow`.** A later Activity can receive an old request after reauthentication, and replay does not identify the current task owner or provide claim/ack semantics.

## Implementation boundaries

- Keep the event/owner coordination in the existing app-level `ActiveTaskClearRequests` boundary; it owns request identity, pending state, claim/release/consume, and authentication invalidation.
- `DeviceLockController` remains the process-level trigger and supplies authentication state changes to the coordinator.
- Each of the four Activities owns only its lifecycle registration and delayed callback. The callback uses the coordinator claim as its validity check.
- Update the existing security/device-lock documentation and Task 28 evidence after implementation.

## Verification

- Unit tests prove screen-off never clears the task, flip settings gate the request, no-owner requests survive until claim, owner replacement invalidates stale claims, and successful reauthentication cancels pending and delayed requests.
- Activity owner tests prove `onPause` releases a claim and recreation can reclaim the same request; consumption is one-shot.
- Compile affected Android sources and instrumentation tests; run focused app tests, KtLint, `verifyArchitecture`, and `git diff --check`. Device behavior remains unverified until an Android device is connected.
