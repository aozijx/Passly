# LOGIN Payload Adapter Implementation Plan

**Goal:** Route `EntryCredentialKind.LOGIN` payload conversion through the internal adapter registry while preserving the flat version-1 JSON shape.

**Constraints:** Keep the login slot (`email`, `password`) and all defaults/order unchanged. Register by credential kind so LOGIN, API_KEY, DATABASE_CREDENTIAL, SERVER_CREDENTIAL, and CRYPTO_WALLET reuse one adapter. Keep EntryType/schema/slot validation in `EntrySecretMapper`; do not add fallback after adapter errors. No codec/callsite, Domain, Room, AAD, archive, or UI changes.

### Task 1: Migrate the LOGIN slot

1. Add a failing test that resolving `EntryCredentialKind.LOGIN` returns a working adapter, encodes only the login slot, and round-trips credentials for at least two EntryType values sharing the kind.
2. Run the focused mapper test and confirm it fails because LOGIN has no registered adapter.
3. Add `LoginEntryPayloadAdapter`, register it by kind, and route both sealed credential encoding and enum-kind decoding through the registry.
4. Run focused tests and full `:data:testDebugUnitTest`; verify JSON golden remains unchanged and run `git diff --check`.
5. Review the diff and preserve unrelated changes. The final commit status is recorded below.

## Execution record

- TDD red: the new shared-kind test failed because `EntryCredentialKind.LOGIN` had no adapter registration.
- Added `LoginEntryPayloadAdapter`, registered by `EntryCredentialKind.LOGIN`, and delegated both sealed credential encoding and enum-kind decoding through the adapter registry.
- Verified LOGIN and API_KEY EntryTypes share the adapter and round-trip the same login slot.
- `:data:testDebugUnitTest`: 97 tests, 0 failures, 0 errors, 0 skipped.
- `git diff --check`: passed.
- Independent read-only review: no Critical, Important, or Minor findings.
- Committed with the CARD and WIFI adapters in `54e478d4b refactor(data): register login card and wifi adapters`.
