# OTP Payload Adapter Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Route OTP payload conversion through a statically registered Data adapter and make root payload codecs validate schema version, slot count, and expected `EntryType` before mapping.

**Architecture:** Add one internal `EntryPayloadAdapter` registered by `EntryCredentialKind`; migrate OTP only, leaving other credential conversions in the existing mapper until their own steps. Keep notes/custom fields and flat `SecretPayload` assembly in the unified mapper. Thread the persisted `EntryType` through bundle and revision codec boundaries.

**Tech Stack:** Kotlin, kotlinx.serialization, `:data` unit tests, JUnit 4.

**Spec:** `C:\Users\lenovo\Documents\Codex\2026-10-29\passly-totp-compose-hilt-room-sqlcipher\outputs\Passly-entry-type-registration-draft.md`, sections 3 (Data adapters), 5 (OTP), 6 (P2), 7 (step 4), and 8 (payload validation and OTP round trip).

## Global Constraints

- Keep `SecretPayload` wire fields, defaults, order, `schemaVersion = 1`, and OTP slot representation unchanged.
- Register adapters by `EntryCredentialKind`, not `EntryType`; multiple entry types may share one credential kind.
- OTP is the only migrated credential adapter in this task; other kinds continue through existing explicit mapping branches.
- Reject unknown schema versions, multiple credential slots, and slot/type mismatches; do not silently fall back after an adapter decode failure.
- Keep notes and custom fields at the unified root mapping boundary; adapters may only encode/decode their own credential slot.
- Preserve Room, revision, and wire-format versions; do not change encryption, AAD, or archive mapping.
- Preserve unrelated workspace changes; stage only this implementation and its required payload DTO split. Commit only when the user requests it.

## Review Focus

- A schema version other than 1 fails before any credential conversion.
- Empty payload is accepted only for `EntryType` values whose `credentialKind` is `NONE`.
- Every present typed slot must match the expected `EntryType.credentialKind`; OTP must not decode as LOGIN or vice versa.
- More than one typed slot fails before a map/adapter can silently select one.
- OTP bundle redaction keeps `config.secret == null`; only the field-level secret row supplies the OTP secret.
- Revision encrypt/decrypt carries the same entry type so restored credentials satisfy the current entity contract.

---

### Task 1: Register the OTP adapter and validate typed payload boundaries

**Files:**
- Create: `data/src/main/java/com/aozijx/passly/data/mapper/entry/EntryPayloadAdapter.kt`.
- Create: `data/src/main/java/com/aozijx/passly/data/mapper/entry/EntryPayloadAdapterRegistry.kt`.
- Create: `data/src/main/java/com/aozijx/passly/data/mapper/entry/OtpEntryPayloadAdapter.kt`.
- Modify: `data/src/main/java/com/aozijx/passly/data/mapper/entry/EntrySecretMapper.kt` to use the OTP adapter and require an expected `EntryType` for encode/decode.
- Modify: `data/src/main/java/com/aozijx/passly/data/codec/entry/SecretBundleCodec.kt` and `data/src/main/java/com/aozijx/passly/data/repository/entry/SecretFieldStore.kt` to pass/resolve `EntryType`.
- Modify: `data/src/main/java/com/aozijx/passly/data/codec/revision/EntryContentSnapshotCodec.kt`, its repository/transaction/writer call sites, and focused tests to pass the expected type through revision encrypt/decrypt.
- Modify: direct `EntrySecretMapper` tests to pass their `EntryType`; add schema, slot-count, mismatch, adapter, and `NONE` cases.
- Test: `data/src/test/java/com/aozijx/passly/data/mapper/entry/EntrySecretMapperTest.kt` and `data/src/test/java/com/aozijx/passly/data/codec/revision/EntryContentSnapshotCodecTest.kt`.

**Interfaces:**
- Consumes: `EntryCredential`, `EntryCredentialKind`, `EntryType`, current flat payload DTOs, and the existing `OtpConfig` / `OtpCredential` model.
- Produces: `internal interface EntryPayloadAdapter { val kind: EntryCredentialKind; fun encode(credential: EntryCredential): SecretPayload; fun decode(payload: SecretPayload): EntryCredential }`; the registry rejects duplicate kinds and resolves adapters by kind.
- Produces: `EntrySecretMapper.toPayload(secret, expectedType)` and `toDomain(payload, expectedType)`; bundle and revision codec boundaries require the persisted `EntryType`.

- [x] **Step 1: Add failing mapper contract tests.** Cover unknown schema version, multiple typed slots, wrong expected type, missing credential for a non-`NONE` type, and a valid OTP round trip with secret kept out of the bundle form.
- [x] **Step 2: Run `EntrySecretMapperTest` and verify each new contract fails for the intended missing validation/adapter behavior.** Initial run failed only at unsupported schema version because current mapper accepted schema 2; existing OTP round trip passed.
- [x] **Step 3: Implement the typed adapter and registry.** The OTP adapter owns only the `otp` slot; the mapper owns root extensions, schema/slot/type checks, and explicit legacy branches for unmigrated kinds. Sealed credential encoding and enum-kind decoding are exhaustive `when` expressions.
- [x] **Step 4: Propagate expected `EntryType` through bundle and revision codecs.** Entity metadata is passed at repository/transaction boundaries, including App backup store calls; OTP paths validate entity state before using its actual `EntryType`.
- [x] **Step 5: Run focused mapper and revision snapshot tests.** OTP adapter round trip and redacted secret behavior pass; invalid schema/slots/types fail with `IllegalArgumentException`.
- [x] **Step 6: Run full `:data:testDebugUnitTest` and `git diff --check`.** All 96 Data tests pass; payload schema, AAD functions, and revision format prefix are unchanged.
- [x] **Step 7: Review call sites and diff.** Independent read-only review found and resolved the missing OTP entity validation path and strengthened the card field omission guard. All production decode/encode paths supply verified `EntryType`; unrelated dirty files are preserved. Do not stage or commit.

**Out of scope for this task:** migrating LOGIN/CARD/IDENTITY/SSH/WIFI/PASSKEY adapters, changing payload DTO wire fields, backup/archive mapping, Domain model changes, database migrations, and UI creation registrations. The prerequisite DTO file split was completed earlier and is included in this commit because it remains uncommitted in the shared checkout.
