# CARD Payload Adapter Implementation Plan

**Goal:** Route `EntryCredentialKind.CARD` payload conversion through a registered adapter, retaining the existing flat schema and all 13 card fields.

**Constraints:** Register by credential kind. Keep all existing field values, JSON names/defaults/order, and expected EntryType validation. Do not add reflection to production code or silently fall back after adapter failure. Preserve the in-progress LOGIN changes and unrelated workspace edits. The initial plan kept this batch uncommitted while LOGIN was in progress; the final combined commit is recorded below.

### Task 1: Migrate the CARD slot

1. Add a failing mapper test that resolves the CARD adapter and round-trips the full card credential through the BANK_CARD EntryType.
2. Run the focused mapper test and confirm it fails because CARD has no registered adapter.
3. Add `CardEntryPayloadAdapter`, register it by `EntryCredentialKind.CARD`, and delegate both sealed credential encoding and enum-kind decoding.
4. Run focused mapper tests, full `:data:testDebugUnitTest`, and `git diff --check`. The auto-populated all-fields test must catch any property omitted by either conversion direction.
5. Review this batch, preserving LOGIN and unrelated changes. Do not stage or commit.

## Execution record

- TDD red: `EntryCredentialKind.CARD` had no registered adapter.
- Added `CardEntryPayloadAdapter` and routed card encoding/decoding through the kind registry.
- The auto-populated nondefault-value test verifies all 13 payload fields and Domain round-trip.
- `:data:testDebugUnitTest`: 98 tests, 0 failures, 0 errors, 0 skipped.
- `git diff --check`: passed.
- Independent read-only review: no findings.
- Committed with the LOGIN and WIFI adapters in `54e478d4b refactor(data): register login card and wifi adapters`.
