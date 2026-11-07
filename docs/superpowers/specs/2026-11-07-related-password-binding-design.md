# Detail page password binding design

## Intent

Let a user bind two already saved password entries from an entry's Detail page, so their relationship appears with the existing related-entry content.

## Scope and rules

- The binding action is available only for an active, persisted `EntryType.LOGIN` entry.
- The picker contains only active, persisted `EntryType.LOGIN` entries. It excludes the current entry and entries already linked to it with `RELATED_TO`.
- Binding creates the existing symmetric `EntryRelationType.RELATED_TO` relation. `EntryLink.create()` remains the owner of canonical endpoint order; the repository transaction remains the owner of endpoint validation, revision snapshots, and post-commit cleanup.
- Candidate reads use `EntryListItem` summaries and never decrypt or expose password values.
- A bind request rechecks that both endpoints are active saved login entries before writing. Self-links and duplicate links cannot be persisted.
- Existing ACCOUNT, OTP, and recovery-code graph behavior stays intact. `RELATED_TO` neighbors are added to the Detail related-entry projection and loaded in one batch while preserving relation order and skipping entries deleted during the read.
- Failures remain visible and retryable; cancellation propagates, and ordinary failures are logged without including entry contents or credentials.
- No Room schema, stable storage key, or backup format changes.

## Design choice

Use a Detail-owned binding workflow backed by the existing `EntryLinkRepository` and a non-sensitive active-entry query. Keep generic relationship persistence in Data, where the current transaction already validates endpoints, captures revisions, and schedules attachment cleanup. Do not create a second relation store or a parallel linking format.

## Out of scope

Editing or removing existing links, linking non-login entry types from this button, and changing ACCOUNT/OTP/RECOVERY relation policy.

## Verification

Cover candidate filtering, active/login endpoint revalidation, symmetric duplicate handling, related-entry projection and batched load order, Detail owner cancellation/replacement, empty/loading/error/retry UI states, and English/Chinese labels. Run the affected App and Data tests plus `verifyArchitecture` and `git diff --check`.
