# App Password Dialog Draft Lifecycle Design

**Date:** 2026-11-03
**Status:** Implementation in progress

## Intent

Clear App Password dialog input whenever the Security page leaves composition, including Activity configuration recreation, and reduce how long coordinator-owned password material remains live.

## Goals

- Treat one `SecurityRoute` composition as the sole owner of its dialog coordinator and draft.
- Clear all draft fields and close the active dialog from the route's `DisposableEffect.onDispose` path. This intentionally clears on both navigation away and Activity recreation; the replacement route starts empty.
- Keep page-effect ownership separate: results from authentication or an operation started by an old route visit must not open a dialog in a later visit.
- Store coordinator drafts in wipeable `CharArray` buffers, replace and clear old buffers on edits, and clear all buffers on dismissal, submit, successful completion, and route disposal.
- Keep the ViewModel's submitted request as a separate owner. It already clears request arrays on completion and rejects results from an exited page visit; preserve those contracts if the route is disposed while persistence is in flight.
- Keep busy-state guards so the coordinator rejects input and repeated submission while an operation is active.

## Behavior contract

1. `SecurityRoute` uses a composition-scoped disposal binding for the coordinator alongside its existing ViewModel page-owner effect; both are disposed whenever the route leaves composition.
2. The coordinator owns only its current dialog state and three draft buffers. It does not survive a route composition or save input in `SavedStateHandle`.
3. A configuration recreation disposes the old route, wipes its buffers, and creates a new empty coordinator. No draft restoration is attempted.
4. Password UI APIs currently exchange immutable `String` values. The coordinator converts incoming values into owned buffers and exposes short-lived `String` snapshots for rendering. Clearing the coordinator buffers limits retained copies but cannot erase Strings already created by Compose or the platform; documentation and tests must not claim memory-zeroization.
5. On submit, the coordinator creates the ViewModel-owned request arrays and immediately clears its own draft buffers. The busy dialog remains disabled until the operation finishes. If it fails, the user can enter the values again.
6. A dismiss event clears the active dialog and all buffers. This includes dismissing the action dialog, so switching away cannot retain stale change-password fields.
7. `AppPasswordSettingsViewModel` remains responsible for the lifetime of its request arrays and its page-visit effect owner. Route disposal invalidates the old owner; late authentication and operation results are dropped and cannot reopen a dialog after re-entry. Existing tests cover request cleanup and late operation results; add focused coverage for late authorization.
8. Field-change and confirm events are accepted only by their matching active dialog. A delayed callback from a dismissed dialog cannot repopulate a cleared draft or submit a new request.

## Approaches

- **Chosen: route-scoped coordinator, clear on every disposal.** Simple ownership follows the existing `remember` lifetime and matches the requested policy for both departure and Activity recreation.
- **Rejected: preserve drafts across configuration recreation.** It requires a longer-lived state owner and explicit handling of sensitive state restoration, contrary to the selected policy.
- **Rejected: retain coordinator `String` drafts and only set them to empty on exit.** This leaves immutable coordinator copies until garbage collection and cannot wipe the owned backing storage.

## Implementation boundaries

- `AppPasswordDialogCoordinator` owns and wipes the mutable draft buffers; Compose-facing models remain strings because existing text-field APIs require them.
- `SecurityRoute` mounts the coordinator's disposal binding for exactly the route composition lifetime; its existing `DisposableEffect` continues to exit the ViewModel page owners.
- Keep ViewModel page ownership, operation cancellation behavior, request-array cleanup, and authentication contracts unchanged except for tests proving old-owner effects are ignored.
- Update `docs/security/authentication.md` and Task 29 evidence after implementation.

## Verification

- Coordinator unit tests cover buffer replacement/clear behavior through observable state, every dismissal path, submit cleanup, and busy rejection.
- Route lifecycle tests cover route disposal and recreation clearing drafts, plus late old-visit authorization/results not reopening a dialog after re-entry. Reuse the existing ViewModel request-cleanup and late-operation tests instead of duplicating them.
- Run focused App Password Coordinator/ViewModel and Route tests, `verifyArchitecture`, and `git diff --check`.
