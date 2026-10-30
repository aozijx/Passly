# Passkey Payload Adapter Implementation Plan

**Goal:** Route `EntryCredentialKind.PASSKEY` payload conversion through the adapter registry while preserving the flat version-1 payload.

**Constraints:** Adapter owns only the passkey slot and all five fields. Keep root schema/slot/type validation in `EntrySecretMapper` and preserve unrelated workspace changes.

### Task 1: Migrate the passkey slot

1. Add a failing adapter lookup test that checks all Passkey fields on encode/decode.
2. Confirm the focused mapper test fails because PASSKEY is not registered.
3. Add and register `PasskeyEntryPayloadAdapter`; delegate both mapper directions.
4. Run focused and full `:data:testDebugUnitTest`, then `git diff --check`.
5. Review the batch and commit only its implementation, test, and plan files.

## Execution Record

- Added an all-fields nondefault adapter test first; it failed because PASSKEY was not registered.
- Added and registered `PasskeyEntryPayloadAdapter`; mapper encode/decode now delegate through the kind registry.
- Added registry coverage asserting every `EntryCredentialKind` except `NONE` resolves to an adapter.
- Focused `EntrySecretMapperTest`: passed.
- Full `:data:testDebugUnitTest`: passed (103 tests, no failures, errors, or skips).
- `git diff --check`: passed; Git emitted only LF-to-CRLF working-copy notices.
- Independent read-only review: no Critical, Important, or Minor findings, including the registry coverage test.
- Committed in `a4ba05549 refactor(data): register passkey payload adapter`.
