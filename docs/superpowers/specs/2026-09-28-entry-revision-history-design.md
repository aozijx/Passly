# Entry Revision History Design

## Goal

Turn the existing encrypted `entry_revisions` snapshots into a real entry-version history feature. The Vault detail page must keep access activity and revision history as separate concepts, expose revision browsing and comparison in one bounded bottom sheet, and restore a selected revision through the same authorization and transactional boundaries as other sensitive Vault commands.

## User experience

The detail page keeps the existing activity timeline for views, copies, autofill, and audit events. A separate “Version history” row shows the available revision count and latest revision time. Selecting it opens one large, scrollable modal bottom sheet.

The sheet has two internal destinations:

1. **History list** — revisions are ordered newest first and identified by version, timestamp, and change kind. The list may contain up to the existing per-entry limit of 50 items and therefore uses lazy scrolling rather than expanding the detail page.
2. **Revision comparison** — selecting a revision replaces the list content inside the same sheet. It compares that snapshot with the current entry, groups changed fields by entry structure, and provides an in-sheet back action. It does not open another sheet or add an application navigation route.

Only changed fields are shown. Each difference has one of `ADDED`, `REMOVED`, or `CHANGED`. Ordinary values may show old and current text. High-sensitivity fields initially expose only the field label and change kind. A user may reveal an individual high-sensitivity comparison after fresh authorization; dismissing the sheet, selecting another revision, locking the Vault, or losing full access clears every revealed value.

“Restore this version” is available from the comparison destination. It requires an explicit confirmation followed by authorization when the affected snapshot contains current or historical high-sensitivity fields. Cancellation leaves the sheet and current entry unchanged. Success returns the sheet to a refreshed history list and refreshes the detail content.

## Terminology and UI ownership

- **Activity** is an audit or usage event. It remains represented by `EntryActivity` and `ActivityTimelineSection` and is never a recovery source.
- **Revision** is a complete recoverable entry state. It is represented by a revision-specific domain contract and never inferred from activity events.
- `DetailRoute` remains the navigation/platform-effect owner and passes one `DetailUiAction` sink into `DetailScreen`.
- `DetailScreen` owns presentation of the history entry point and the sheet overlay.
- `DetailViewModel` owns sheet destination, selection, loading, confirmation, and revealed-value lifetime as part of the detail feature state. Composables do not query repositories or perform authentication.
- Feature use cases own comparison, reveal, and restore orchestration. The ViewModel does not decode database entities or construct authorization scopes.
- The data module owns Room entities, snapshot codecs, attachment-reference replacement, and the atomic restore transaction.

The sheet is a semantic `RevisionHistorySheet`, not a generic callback-driven sheet. It receives immutable revision presentation state and emits `DetailUiAction` values.

## Domain contracts

Introduce an `EntryRevisionRepository` port with narrow operations rather than exposing Room DAOs:

- observe metadata for the revisions of one `EntryId`;
- load one revision as a redacted snapshot suitable for ordinary comparison;
- reveal selected high-sensitivity values only with a matching consumed permit;
- restore one revision with an expected current `EntryVersion` and, when needed, a matching restore permit.

Revision list metadata contains no decrypted entry values. The redacted snapshot contains the profile, low-sensitivity structure, relationships, attachment metadata, and the set of high-sensitivity keys that existed in that revision. It cannot contain high-sensitivity values.

The comparison use case compares a selected redacted revision against the current redacted entry and returns stable, field-keyed differences. Presentation maps field keys to localized labels and formatting. Comparison logic stays independent of Compose and string resources.

The reveal use case accepts exactly one selected revision and a non-empty set of high-sensitivity field keys. It authorizes `AuthorizationScope.SensitiveRevision` with action `REVEAL`, consumes the permit in the repository, and returns owned sensitive values only for the requested keys. The presentation layer holds those values in the existing non-saveable reveal-store pattern and clears them on lifecycle and session boundaries.

## Snapshot security boundary

The current content snapshot serializes a complete `EntrySecret` even though high-sensitivity values are also stored in `sensitiveFieldCipherSet`. That creates two encrypted facts for the same values and would let a generic content decoder expose them without a revision-specific permit.

The new snapshot contract removes that duplication:

- `entryContentCipher` contains `EntryProfile`, the low-sensitivity secret bundle, and relationships only;
- `sensitiveFieldCipherSet` is the sole historical source for high-sensitivity values;
- revision attachment references remain separate and continue sharing immutable attachment resources;
- ordinary history queries decode only the content snapshot and sensitive-key metadata;
- only the permit-consuming reveal or restore path may decrypt or install historical high-sensitivity fields.

This project is still on the development schema contract. The change keeps the Room database version unchanged and does not implement compatibility decoding for snapshots written by earlier development builds. The revision payload format identifier changes so unsupported old payloads fail explicitly rather than being interpreted ambiguously. Existing development installations must clear application data, consistent with the documented schema policy.

## Restore transaction

Restoring a revision is a command, not an editor update and not a replay of activity events. The repository implementation performs one write transaction:

1. Load the current entry and require its version to equal the caller’s expected version.
2. Load and validate the selected revision belongs to that entry.
3. Determine the union of high-sensitivity fields present in the current and historical states.
4. If the union is non-empty, consume a `SensitiveRevision(RESTORE)` permit whose entry, revision, action, and field set match exactly. A permit is never accepted when the union is empty.
5. Replace profile metadata and low-sensitivity structure with the historical values while preserving the entry identity.
6. Replace the current high-sensitivity field rows from the historical cipher set, including deletion of fields absent from the target revision.
7. Replace entry relationships and committed attachment references with the historical sets, validating referenced entries and immutable attachment resources.
8. Increment the current entry version and update its timestamp.
9. Write the restored state as the new latest revision with `RevisionChange.VERSION_RESTORED`.
10. Record one `ActivityType.RESTORE` audit event.

Any validation, optimistic-version, authorization, reference, codec, or database failure rolls back all ten steps. Attachment garbage collection is scheduled in the transaction and drained only after success. Replaced custom icon cleanup follows the same post-commit resource-cleanup rule as an ordinary entry update.

The restore operation does not mutate or reuse the selected historical revision. It creates a new current version, so restoring is itself reversible through later history.

## State and error handling

Revision history uses explicit state rather than nullable lists and unrelated booleans:

- sheet visibility and destination (`LIST` or `COMPARISON`);
- history load (`Loading`, `Ready`, `Failed`);
- selected comparison (`Loading`, `Ready`, `Failed`);
- restore confirmation and restore progress;
- transient revealed high-sensitivity values outside saveable state.

Opening the sheet starts observation for the currently loaded entry. Closing it cancels history collection and clears selection, confirmation, and revealed values. A new entry load always resets the entire revision state.

User cancellation is not shown as an error. Missing revisions, stale current versions, corrupt snapshots, invalid attachment references, and database failures produce feature-level failures with retry or dismissal. Restore is disabled while loading or while another restore is running. The UI never claims success until the transaction returns success and the refreshed current entry has been loaded.

## Testing and verification

### Domain and feature tests

- Comparison reports added, removed, and changed ordinary fields and omits unchanged fields.
- Redacted comparison cannot contain high-sensitivity values.
- Reveal requests only the selected revision and requested non-empty key set, and cancellation clears/no-ops.
- Restore authorization covers the exact union of current and historical high-sensitivity keys.
- ViewModel transitions list → comparison → list inside one sheet and clears revealed values on close, entry change, and access loss.
- Restore success refreshes detail/history; failure preserves the selected comparison and reports a retryable failure where appropriate.

### Data tests

- The new content codec round-trips only low-sensitivity content and rejects the previous format identifier.
- Revision metadata queries do not decrypt snapshot content.
- Reveal consumes an exact revision permit and cannot reveal another entry, revision, action, or field set.
- Restore atomically replaces profile, low-sensitivity structure, high-sensitivity rows, links, and attachment references; increments the version; writes `VERSION_RESTORED`; and records RESTORE activity.
- Optimistic-version, invalid-reference, and authorization failures leave every table unchanged.
- Restoring a restored revision remains reversible and retention limits still apply.

### UI and architecture tests

- Detail UI has distinct activity and revision sections.
- The history list is lazy and the comparison replaces content within one sheet.
- `DetailRoute` does not translate revision callbacks or access revision repositories.
- Revision data contracts do not depend on Compose, Room, Android resources, or presentation models.

Each implementation batch runs focused tests first, followed by `:buildSrc:test`, `:app:testDebugUnitTest`, `:app:compileDebugAndroidTestKotlin`, `:app:assembleDebug`, `:app:lintDebug`, `verifyModuleBoundaries`, and `git diff --check`. Data transaction behavior additionally runs the relevant instrumented Room tests.

## Delivery order

1. Make the snapshot boundary safe and add redacted revision query contracts.
2. Add comparison, authorized reveal, and atomic restore use cases/data implementation.
3. Add detail state/actions and the single-sheet history UI.
4. Update database, sensitive-access, and Vault-detail documentation, then run the full verification gate.

Each independently compiling batch is committed separately. The existing architecture implementation plan remains untracked and is not staged with this design.
