# Identity Payload Adapter Implementation Plan

**Goal:** Route `EntryCredentialKind.IDENTITY` payload conversion through the adapter registry while preserving the flat version-1 payload.

**Constraints:** Adapter owns only the identity slot. Preserve all identity fields and root schema/slot/type validation in `EntrySecretMapper`. Keep unrelated workspace changes intact.

### Task 1: Migrate the identity slot

1. Add a failing adapter lookup test that verifies all identity fields encode and decode.
2. Confirm the focused mapper test fails because IDENTITY is not registered.
3. Add and register `IdentityEntryPayloadAdapter`; delegate both mapper directions.
4. Run focused and full `:data:testDebugUnitTest`, then `git diff --check`.
5. Review the batch and commit only its implementation, test, and plan files.

## Execution Record

- Added an all-fields nondefault adapter test first; it failed because IDENTITY was not registered.
- Added `IdentityEntryPayloadAdapter` and delegated identity slot encode/decode through the kind registry.
- Focused `EntrySecretMapperTest`: passed.
- Full `:data:testDebugUnitTest`: passed (100 tests, 0 failures, errors, or skips).
- `git diff --check`: passed; Git reported only LF-to-CRLF working-copy notices.
- Independent read-only review: no Critical, Important, or Minor findings.
- Committed in `01c22d277 refactor(data): register identity payload adapter`.
