# Entry Links Derived from Endpoint Types

## Intent

Use one generic `entry_links` relationship row for all associations between Vault entries, including account ownership. Do not persist a second OTP-specific `OTP_FOR` discriminator when the endpoint entry types already determine the relationship. This makes link endpoints the sole internal association record and establishes the link model as the path for future entry associations.

## Baseline evidence at proposal time

These bullets explain the original design decision; they describe the implementation before this cutover, not the current worktree. The current storage and backup behavior is documented in [database boundaries](../../platform/data/database.md) and the [Archive v1 contract](../../product/backup/passly-archive-v1.md).

- Room `entry_links` currently stores `linkId`, `sourceEntryId`, `targetEntryId`, `relationType`, and timestamps. There is no separate `otpFor` column in `entries`.
- `OTP_FOR` is a stored `relationType` value, used by policy, account projection, autofill and backup mapping.
- Generic links must remain available for every distinct Entry type pair. Endpoint types only classify known account, OTP or recovery combinations; all other pairs remain generic links.
- Passly Archive v1 currently has a `relationType` key in its link record. Its decoder uses `ignoreUnknownKeys = true`, so old archives can be imported after the field is removed; endpoint IDs and timestamps remain the recognized link data.

## Goals and boundaries

- Internal Room and Domain links are generic undirected endpoint pairs carrying link ID, two entry IDs and timestamps. Canonicalize endpoint IDs in stable lexical order so reversed insertion cannot create duplicate links; the database unique index covers only that pair.
- Infer the relationship from the two endpoint `EntryType` values through one exhaustive Domain policy:
  - one `ACCOUNT` endpoint means account ownership/membership;
  - one `OTP` endpoint paired with an eligible protected entry means OTP association;
  - one `RECOVERY_CODE` endpoint paired with an eligible protected entry means recovery-code association;
  - all other distinct endpoint pairs mean a generic related-entry association.
- Apply the rules in that order, giving each pair exactly one inferred meaning. A pair classified as OTP, recovery or account membership cannot also carry a second explicit related-entry meaning.
- Permit any existing distinct Entry endpoint pair and reject only self-links or missing/deleted endpoints. Relationship classification is a read projection, not a creation constraint.
- Remove `relationType` from Passly Archive link records without changing archive version 1. `BackupJson.ignoreUnknownKeys = true` ignores the old field on import; export writes only the generic endpoints, link ID and timestamps.
- Remove relation type from encrypted revision link snapshots and bump the internal snapshot prefix; old local development databases are reset as a whole.
- Keep Room schema version 1 as requested and document resetting old local development databases before opening the changed schema. Do not add a migration or destructive fallback.
- Update all link writers/readers, SQL projections, history snapshots, backup mapping, Autofill lookup, tests and formal docs in the same cutover. Remove the persisted `EntryRelationType` path and its Room converter/index only after callers move.

## Approaches

1. Keep `relationType` and `OTP_FOR`. This preserves current behavior but does not remove the duplicate internal discriminator the user identified.
2. Remove `relationType` from internal links and Archive v1 link records, deriving meaning from endpoint types. **Selected.** This matches the request to use generic links and avoid storing OTP semantics twice while retaining Archive version 1 and ignoring the removed legacy field on import.
3. Remove `OTP_FOR` alone while keeping a required `relationType` column. This requires a replacement value or nullable special case and still stores a discriminator, so it adds complexity without removing the redundant field.

## Acceptance checks

- All active link creation and query paths use canonical generic endpoint pairs and the endpoint-derived Domain policy; no Room or Domain link relation-type field remains.
- OTP, recovery, account membership and generic related-entry meanings are derived from endpoint types; all endpoint pairs remain linkable and self-links are rejected.
- Archive version remains 1; old archive `relationType` values are ignored by the configured decoder and generic link data round trips without that field.
- Room schema export for version 1 reflects the new link row/index; the documented manual reset procedure matches the configured provider behavior.
- Repository compile and `git diff --check` pass. Tests are updated but not run unless requested.
