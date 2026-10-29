# SSH Payload Adapter Implementation Plan

**Goal:** Route `EntryCredentialKind.SSH` payload conversion through the adapter registry while preserving the flat version-1 payload.

**Constraints:** Adapter owns only the SSH slot and all three fields. Keep root schema/slot/type validation in `EntrySecretMapper` and preserve unrelated workspace changes.

### Task 1: Migrate the SSH slot

1. Add a failing adapter lookup test that checks all SSH fields on encode/decode.
2. Confirm the focused mapper test fails because SSH is not registered.
3. Add and register `SshEntryPayloadAdapter`; delegate both mapper directions.
4. Run focused and full `:data:testDebugUnitTest`, then `git diff --check`.
5. Review the batch and commit only its implementation, test, and plan files.

## Execution Record

- Added an all-fields nondefault adapter test first; it failed because SSH was not registered.
- Added `SshEntryPayloadAdapter` and delegated SSH slot encode/decode through the kind registry.
- Focused `EntrySecretMapperTest`: passed.
- Full `:data:testDebugUnitTest`: passed (101 tests, no failures, errors, or skips).
- `git diff --check`: passed; Git emitted only LF-to-CRLF working-copy notices.
- Independent read-only review: no Critical, Important, or Minor findings.
- Changes are ready for exact-path staging and a separate commit.
