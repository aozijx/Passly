# Exhaustive Entry Materializer Dispatch

**Goal:** Make creation support decisions compile-time exhaustive over `EntryType` without adding a materializer strategy registry.

**Constraints:** Preserve behavior and exception message for unsupported creation types. Keep the four implemented materializers and all private type-specific rules unchanged. Preserve unrelated workspace edits.

### Task 1: Classify every entry type explicitly

1. Add a characterization test that a valid NOTE draft remains rejected for creation.
2. Remove the fallback branch and confirm Kotlin reports the unclassified `EntryType` values.
3. Add explicit rejection branches for currently unsupported values.
4. Run `:domain:test` and `git diff --check`; review and commit only scoped files.

## Execution Record

- The NOTE rejection characterization test passed before production changes.
- Removing the fallback produced the expected Kotlin compile error listing unclassified enum values.
- Added explicit rejection cases for the currently unsupported types while preserving the original exception message.
- Full `:domain:test`: passed (178 tests, 0 failures, errors, or skips).
- Independent read-only review: no Critical, Important, or Minor findings.
- `git diff --check`: passed; only LF-to-CRLF working-copy notices for unrelated files were emitted.
- Committed in `0c1758c64 refactor(domain): make materializer dispatch exhaustive`.
