# Wi-Fi Payload Adapter Implementation Plan

**Goal:** Route `EntryCredentialKind.WIFI` payload conversion through a registered adapter while preserving the flat version-1 payload.

**Constraints:** Adapter owns only the wifi slot. Preserve SSID/password/hidden fields and the canonical stored security string. Reject missing and unsupported stored security types; do not default malformed data. Keep root extensions and schema/slot/type validation in `EntrySecretMapper`. Preserve LOGIN/CARD in-progress edits and unrelated workspace changes; do not stage or commit.

### Task 1: Migrate the Wi-Fi slot

1. Add a failing test for resolving the WIFI adapter and round-tripping a WIFI entry, retaining canonical security storage.
2. Run the focused mapper tests and confirm the missing adapter registration fails.
3. Add `WifiEntryPayloadAdapter`, move security type stored-value parsing into that adapter, register by `EntryCredentialKind.WIFI`, and delegate both mapper directions.
4. Keep tests for missing/unsupported stored security and run focused plus full `:data:testDebugUnitTest`; run `git diff --check`.
5. Review the batch and preserve unrelated changes. The final commit status is recorded below.

## Execution Record

- Added a registry lookup and WIFI payload round-trip test first; it failed because no WIFI adapter was registered.
- Added `WifiEntryPayloadAdapter` for the Wi-Fi slot and security-type parsing, registered it, and delegated mapper encoding/decoding to it.
- The existing missing and unsupported security-type tests remain in place.
- Focused `EntrySecretMapperTest`: passed.
- Full `:data:testDebugUnitTest`: passed.
- `git diff --check`: passed; Git reported only existing LF-to-CRLF working-copy notices.
- Committed with the LOGIN and CARD adapters in `54e478d4b refactor(data): register login card and wifi adapters`.
