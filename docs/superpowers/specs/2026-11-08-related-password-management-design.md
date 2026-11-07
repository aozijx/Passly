# Related password management in Detail

## Intent

Polish the related-password area in saved Login details and let users remove a password-to-password relation without deleting either password. Keep the current Detail screen, search flow, and persisted `RELATED_TO` contract.

## Goals

- Give direct active Login-to-Login related passwords their own clearly titled group, with each row showing the saved title and username summary; keep ACCOUNT-group members and any other existing direct relation rows visible so removing a password link has an unambiguous visible result.
- Put a compact bind action in the direct-related-password group header. Give the empty state a short explanation and the same primary action; refine the candidate sheet's search, loading, empty, and failure states using existing Material 3 components.
- Allow a user to remove a direct relation between two active saved Login entries.
- Keep relation writes and removals owned by the current Detail page/session; stale completions must not update a replacement visit.
- Keep bind/unbind failures logged at the operation owner, preserve cancellation, and keep a retry path.

## Non-goals

- Deleting or editing either password entry.
- Exposing a generic editor for ACCOUNT, OTP, recovery, or other `EntryLink` relations.
- Changing Room relation storage, backup serialization, or the meaning of existing relation types.

## Existing behavior and constraints

- `RelatedEntriesSection` renders a clickable list inside `InfoGroupCard` and an outlined bind button. `RelatedPasswordPickerSheet` is a searchable `ModalBottomSheet` with title/username rows.
- `DetailSessionQuery` merges account-group members and direct relation neighbors into one `relatedEntries` projection, losing the relation kind and link ID. `RELATED_TO` is not restricted to Login types by `EntryLinkPolicy`, so the UI needs a separate direct-password projection identifying only direct links whose two endpoints are active Login entries before exposing an unbind gesture. Other relation rows must remain visible and navigable.
- `EntryLinkRepository.delete(linkId)` already removes by ID in a transaction; Data snapshots both endpoints and schedules attachment cleanup. Unlink can reuse this contract after the related-password service resolves the direct `RELATED_TO` link. No Room schema or relation-storage change is needed.
- The shared `SwipeActionContainer` invokes its action when the swipe crosses its threshold; it does not remain open for a second tap. The chosen interaction therefore uses the swipe as an explicit request and opens a confirmation dialog before unlinking.

## Approaches considered

1. **Swipe to request, confirm in a dialog (selected).** Reuse `SwipeActionContainer` with its left action. The swipe opens a dialog that says the relation will be removed while both saved passwords remain. Confirm starts the current-page-owned unlink operation. This matches the selected left-swipe interaction and reuses the existing gesture contract without duplicating drag handling.
2. **Inline unlink icon with confirmation.** More discoverable and direct, but adds a persistent destructive control to every relation row and does not use the selected swipe interaction.
3. **Change the shared swipe container to reveal a button.** This would support swipe-then-tap, but changes a shared gesture contract while that component is being edited and requires its existing callers and tests to be reviewed together.

## Proposed behavior

- Keep ACCOUNT-group and non-password relation rows navigable as they are. Render eligible direct password links in a separate group as Material 3 list items with title, username summary, and trailing navigation affordance. Crossing the shared container's left-swipe threshold opens a confirmation dialog naming the related entry and stating that only the relationship will be removed. Both saved passwords stay in the vault. Dismissing the dialog leaves the relation unchanged. Expose the same action to accessibility services through a custom row action.
- On confirmation, revalidate the current source, target, and selected direct `RELATED_TO` link ID through the related-password service, then pass that ID to the existing repository delete operation. Disable repeated operations for that target and show progress while removal is active.
- After each completed unlink attempt, reload all relation projections. On success, close the dialog. If revalidation finds an endpoint is no longer an active LOGIN, do not delete the link; close the dialog when the refreshed projection no longer contains that eligible link. For other failures, keep the relationship and dialog available, show localized retry feedback, and log the failure once at its operation owner. `RelatedPasswordLinks` owns logging for bind and unlink operations; projection reload failures must be logged by the Detail workflow. Cancellation propagates; page/session replacement cancels the old operation and suppresses its result.
- Keep the compact bind action in the group header even when the direct-password group is empty; show a small link icon and concise empty-state text rather than an oversized placeholder. The candidate sheet has a clear title, full-width search field, and distinct loading, no-candidate, no-search-match, failure-with-retry, and ready states. Candidate rows expose only title and username summary, and the active write shows progress on its row. Preserve bilingual resources and use existing Material 3 components without new image assets or dependencies.

## Design decisions for implementation

- Split `DetailSessionData` into account-group entries, non-password direct relation entries, and eligible direct-password relations. The latter carries the `EntryLinkId` plus target title/username summary and is populated from the same link snapshot used to build the account graph; do not infer unlink eligibility from entry type alone or include ACCOUNT-group rows.
- Keep bind and unlink business rules together with the existing related-password application service; carry the selected `EntryLinkId` through confirmation so the service can verify and delete that precise edge. Keep confirmation visibility and screen rendering in Presentation.
- Reuse `EntryLinkRepository.delete(linkId)` and its existing transaction, revision snapshot, and cleanup behavior. Add no storage, schema, or repository-contract changes; the application service owns selecting an eligible direct `RELATED_TO` link, and Detail owns the refresh after deletion.
- Add focused tests for filtering eligible swipe rows, symmetric unlink lookup, idempotence/missing relation, failure retry, duplicate suppression, lifecycle cancellation, and Detail refresh behavior.

## Verification

Run the focused related-password query, service, and workflow tests; App and Data unit tests; `verifyArchitecture`; and `git diff --check`. Review the final staged paths to exclude unrelated user work and the locally maintained `AGENTS.md` / `.agents/` files.
