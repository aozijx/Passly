# Autofill Sealed Save Inbox Design

Date: 2026-10-18

Status: Approved design direction; implementation pending

## Objective

Allow both traditional Autofill and Credential Manager to accept a new credential while the Vault
is locked without showing a Passly authentication prompt. Locked-state saving must be a genuine
write-only capability: the process may seal a new credential, but it must not be able to decrypt the
pending credential, query the Vault, or retain the Vault DEK.

The change must preserve the existing behavior when the Vault is already writable: save directly,
return the platform result, and do not create a pending record.

## Why the Existing Flow Authenticates

The current save flow is not validating the Android save action. It authenticates only because the
formal Entry write path and SQLCipher transaction require an open Autofill or full Vault session.
SQLCipher receives one symmetric database key; a connection that can write can also read. Hiding
query methods behind a Kotlin interface would be a source-code restriction, not a cryptographic
write-only boundary.

Earlier Passly versions avoided a save-time prompt in two different ways:

- the oldest implementation encrypted captured fields with a bidirectional, authentication-free
  Keystore AES key (`vault_silent_key_v1`); this also permitted locked-state decryption and must not
  be restored;
- a later implementation retained an already-authorized fill session until the save callback; this
  did not support an independent save after manually entering a new credential and extended the
  lifetime of Autofill access.

The target design does not reuse either behavior.

## Android Platform Boundary

`SaveInfo.FLAG_DELAY_SAVE` is not a durable pending-save facility. Android uses it to combine
`FillContext` values from later fill/save requests in the same task, primarily for multi-screen
forms. Passly cannot actively retrieve that context after a later Vault unlock, and Credential
Manager create requests do not share the Legacy `SaveInfo` contract. The flag may still be used for
genuine multi-screen forms, but it is not part of this storage design.

Legacy `SaveCallback`, Legacy Dataset/`FillResponse`, and Credential Manager create results remain
separate Android contracts. They share only the platform-neutral sealed-save coordinator.

## Security Model

### Required properties

While the Vault is sealed:

1. Passly can encrypt a new `AutofillSaveDraft` using public material.
2. Passly cannot decrypt any pending draft.
3. No Vault DEK, Inbox private key, username, password, package name, or domain is written in
   plaintext outside the normal unlocked write transaction.
4. Within the 24-hour retention period and fixed capacity limits, process death and device restart
   do not lose an accepted pending save.
5. Pending records cannot be replayed into duplicate Vault entries after a crash.

After a Vault session is opened:

1. Passly can unwrap the Inbox private key with a key derived from the Vault DEK.
2. Pending records are imported through the same typed Entry materialization and Autofill writer as
   direct saves.
3. Successfully imported records are removed; corrupt records never cause an infinite retry loop.

This protects against filesystem inspection and against ordinary locked-state application paths.
It does not attempt to protect secrets from arbitrary code execution inside the Passly process.

### Key hierarchy

Each Vault owns one stable Inbox RSA key pair:

```text
Vault DEK
  -> HMAC-SHA256("passly-autofill-inbox-private-wrap-v1")
  -> Inbox private-key wrapping key

Inbox public key (X.509 encoded)
  -> available while locked; encryption only

Inbox private key (PKCS#8 encoded)
  -> AES-256-GCM envelope under the derived wrapping key
  -> available only while the Vault DEK is loaded
```

RSA-3072 with OAEP SHA-256 is selected for the Inbox key envelope because it is available on the
minimum supported Android version without adding another cryptographic provider. RSA encrypts only
a random 256-bit content key; it never encrypts a credential payload directly.

The private-key envelope has one fixed version 1 encoding:

```text
magic[14]                  ASCII "PASSLYINBOXKEY"
format_version             u8 = 1
key_id[16]                 random Vault-generation ID
public_key_sha256[32]      SHA-256 of the exact stored X.509 public-key bytes
nonce[12]                  random
ciphertext_length          u32, big endian
ciphertext                 AES-256-GCM(PKCS#8 private-key bytes)
tag[16]                    GCM authentication tag
```

Its AAD is exactly the encoded
`magic || format_version || key_id || public_key_sha256` prefix. The versioned magic is the domain
separator; there is no second implicit label. All length fields are bounded before allocation. All
temporary private-key, content-key, and plaintext byte arrays are wiped on a best-effort basis.

This is not an Android Keystore authentication-window key. Device authentication windows do not
model Passly's App Password session, and Android user-authentication restrictions do not restrict
public-key operations.

### Provisioning and existing Vaults

New Vault initialization provisions the Inbox key pair before the initial session is closed.
Existing Vaults provision it lazily on the first successful normal or Autofill unlock after the
update. Provisioning is mutex-protected and idempotent.

`bootstrap.pb` keeps its current name and all existing data. `BootstrapData` appends one message
field at number 9; no existing field number changes. `bootstrap.pb` is an app-private DataStore that
is available before SQLCipher opens, so both the public metadata and the DEK-wrapped private
envelope remain independent of the database.

### Proto schema

The schema stores one atomic cryptographic-material snapshot rather than four unrelated top-level
byte fields:

```proto
message BootstrapData {
  int32 version = 1;
  repeated EnvelopeEntry envelopes = 2;
  bytes verification_tag = 3;
  BiometricBinding biometric_binding = 4;
  BiometricRotationJournal biometric_rotation = 5;
  repeated string biometric_cleanup_aliases = 6;
  bytes sensitive_key_envelope = 7;
  bytes attachment_key_envelope = 8;
  AutofillInboxKeyMaterial autofill_inbox_key = 9;
}

message AutofillInboxKeyMaterial {
  uint32 format_version = 1;
  bytes key_id = 2;
  bytes public_key = 3;
  bytes public_key_sha256 = 4;
  bytes private_key_envelope = 5;
}
```

`format_version` must be exactly `1`; zero and unknown values are unsupported rather than defaults.
`key_id` must be 16 bytes, `public_key_sha256` must be 32 bytes, `public_key` must decode as an
RSA-3072 X.509 public key, and `private_key_envelope` must pass the fixed envelope bounds before use.
The Proto message carries opaque key material only. Key generation, RSA parsing, fingerprinting,
envelope authentication and rotation remain Core/Security responsibilities rather than Proto or
DataStore business logic.

The key ID is a random 128-bit Vault-generation identifier. The private-key envelope AAD binds its
format version, key ID, and public-key SHA-256 fingerprint. After unwrapping, Passly derives the RSA
public key from the private key and verifies its modulus and public exponent against the stored
public key, then verifies the fingerprint. Any mismatch stops the whole drain and moves active
pending records for that key ID into quarantine without attempting an Entry write.

`VaultBootstrapStore` exposes the aggregate as one nullable domain value:

```text
loadAutofillInboxKey(): AutofillInboxKeyMaterial?
saveAutofillInboxKey(material)
clearAutofillInboxKey()
```

`ProtoVaultBootstrapStore.saveAutofillInboxKey()` performs one `DataStore.updateData` that replaces
the complete nested message. It never exposes setters for individual child fields. Reading maps to
exactly one of these states:

- message absent: an existing Vault has not been provisioned yet;
- complete valid v1 message: usable locked-write material;
- message present but incomplete, malformed, zero-version or unknown-version: integrity failure;
  do not treat it as absent and do not automatically overwrite it.

Existing `bootstrap.pb` files naturally have no field 9 and therefore take the lazy-provision path
after a successful Vault unlock. There is no file rename, reset, compatibility copy, or separate
Inbox Proto DataStore.

If a locked existing Vault has not yet been provisioned, a save cannot be sealed. The platform
operation returns a typed unavailable result without launching authentication. The next normal
unlock provisions the key, after which locked-state saves work. Passly must not claim that such a
credential was saved.

Deleting the Vault clears the bootstrap fields, import-log rows, and the entire Inbox directory.
Changing an App Password or biometric binding only rewraps the stable Vault DEK, so it does not
rotate or rewrap the Inbox pair. A future operation that actually rotates the Vault DEK must decrypt
the private-key envelope with the old DEK-derived key, rewrap it with the new DEK-derived key, and
atomically replace the envelope before committing the DEK rotation. It must roll back the rotation
if that replacement fails.

If public metadata is absent or malformed, locked-state saving fails without authentication. If the
private envelope cannot be authenticated after a successful Vault unlock, pending files are retained
in quarantine and a redacted error is emitted; they are not deleted or treated as imported. Passly
does not currently promise an automatic recovery path for a destroyed Inbox private key.

### Key lifecycle

| Event | Required action |
| --- | --- |
| New Vault | Generate RSA-3072 pair, key ID and fingerprint; wrap PKCS#8 private key with a DEK-derived AES-256-GCM key; commit one complete nested bootstrap snapshot atomically. |
| Existing Vault first unlock | Perform the same provisioning lazily and idempotently before accepting later locked saves. |
| App Password or biometric change | No Inbox rotation; only the authentication envelope around the unchanged Vault DEK changes. |
| Actual Vault DEK rotation | Rewrap the Inbox private key under the new DEK-derived key in the same rotation protocol. |
| Public key missing or damaged | Reject locked save; never fall back to authentication or an unauthenticated symmetric key. |
| Public/private mismatch | Stop drain, quarantine records for the key ID, and emit a redacted integrity error. |
| Private envelope unwrap failure | Retain/quarantine pending ciphertext and require a later recovery decision. |
| Vault deletion | Remove bootstrap key material, pending/quarantine directories and SQLCipher import-log rows. |

## Pending Record Format and Storage

Pending records live under an application-internal directory dedicated to Autofill save ciphertext,
not in preferences, the SQLCipher database, cache, logs, Intent extras, or saved UI state. The
directory is `filesDir/vault/autofill-inbox/<key-id>/`, with `pending/` and `quarantine/`
subdirectories. The random key ID isolates one Vault generation from stale files left by an
interrupted deletion or recreation. This is a runtime data store and does not add another
startup-loaded settings `.pb` file.

Passly's normal Vault export and backup do not include the pending directory, quarantine directory,
public key, or private-key envelope. Android Auto Backup and device-to-device transfer must also
exclude `filesDir/vault/autofill-inbox/` in full, including `pending/`, `quarantine/`, state sidecars
and temporary files. Inbox fields in `bootstrap.pb` follow the same non-transferable lifecycle and
must not be restored independently on another device.

The current manifest already sets `android:allowBackup="false"`, and `data_extraction_rules.xml`
excludes the complete root for both cloud backup and device transfer. Implementation must retain
both protections and add a verification test/check for the Inbox path and `bootstrap.pb`; a future
change that enables system backup must add explicit path exclusions before it can land. This design
promises survival across process death and device restart on the same installation only; it does
not promise cross-device migration, Android device transfer, or restoration from an exported Vault
backup.

Each record is an independently authenticated, versioned binary envelope:

```text
magic[11]                  ASCII "PASSLYINBOX"
format_version             u8 = 1
key_id[16]                 random Vault-generation ID
draft_id[16]               random UUID bytes
created_at_ms              i64, big endian
wrapped_key_length         u16, big endian
wrapped_content_key        RSA-OAEP-SHA256(random 256-bit AES key)
payload_nonce[12]          random
ciphertext_length          u32, big endian
payload_ciphertext         AES-256-GCM ciphertext
payload_tag[16]            GCM authentication tag
```

All length fields are checked before allocation. Version 1 AAD is the exact concatenation of the
encoded file-header bytes `magic || format_version || key_id || draft_id || created_at_ms` followed
by `public_key_sha256` loaded from `bootstrap.pb`. The fingerprint is external AAD and is not stored
in the pending record header. The encrypted payload contains:

```text
draft_id
target_entry_id
source                     LEGACY or CREDENTIAL_MANAGER
package_name
web_domain
page_title
username
password
captured_at_ms
```

The decrypted `draft_id` and header `draft_id` must match. No target scope or secret value is exposed
in the filename or plaintext header. The filename is `<draft-id>.bin`; it never contains a package,
domain, username, timestamp, or Entry ID. `created_at_ms` and filesystem modification time disclose
the approximate save time. This limited metadata leak is accepted because locked expiry and capacity
cleanup must work without a Vault key.

Writes use a temporary file in the same directory, `fsync`, and atomic rename. Startup removes
orphan temporary files. The store is serialized by one mutex and applies these fixed limits:

- at most 8 active pending records across the entire `autofill-inbox/` root, not per key ID;
- at most 64 KiB ciphertext per record;
- at most 256 KiB total active pending storage across all key-ID directories;
- 24-hour retention from `created_at_ms`;
- oldest expired records are removed before capacity is evaluated;
- a full Inbox rejects the new save instead of silently deleting a still-valid credential.

Locked and unlocked cleanup may delete expired active records using only bounded header/file
metadata; it never opens SQLCipher for expiry. Quarantine is separately bounded to 8 records and
256 KiB across all key-ID directories and retains records for at most 7 days before deleting the
oldest ciphertext. Active records, quarantine, sidecars and temporary files have a final 512 KiB
hard cap for the complete `autofill-inbox/` root. Admission fails closed if cleanup cannot bring the
root below these limits. These limits are implementation constants, not user preferences.

When complete current bootstrap key metadata is available, startup and every successful unlock
remove every non-current `<key-id>/` directory before calculating capacity. The current key ID's
quarantine remains until its 7-day retention expires. Vault deletion removes the whole root before
a new Vault generation can provision a key. A partial/corrupt bootstrap record is an integrity
failure: Passly does not silently provision a new key over existing pending records, because that
would make them permanently unreadable while hiding the cause.

Each active record may have a non-secret atomic state sidecar containing only `retry_count` and
`next_retry_at_ms`. A database/session-wide outage aborts the drain and does not increment every
record. A record-specific transient materialization or transaction failure uses bounded exponential
backoff and moves the record to quarantine after 3 failed attempts. Permanent format or
authentication failures enter quarantine immediately and are never retried automatically.

The public key is necessarily readable and replaceable by an attacker who already has arbitrary
write access to Passly's private files. Such replacement could redirect credentials sealed before
the next unlock. Android app-private storage is the preventive boundary; the key-ID, private/public
match and fingerprint checks provide detection at unlock, not prevention after that boundary is
fully compromised. This residual risk is recorded explicitly rather than claiming that a locked
public key is self-authenticating.

## Shared Save Flow

Introduce one platform-neutral coordinator with explicit outcomes:

```text
AcceptAutofillSave(draft, source)
  -> Saved(entryId)
  -> Queued(pendingId)
  -> InvalidDraft
  -> Disabled
  -> InboxUnavailable(reason)
  -> Failed(errorCode)
```

The coordinator owns policy ordering:

1. validate the source-specific feature switch and typed draft;
2. if `hasAutofillSessionAccess()` is true, write directly through `SaveAutofillCredential`;
3. otherwise seal the draft through `AutofillSaveInbox` without starting an interaction or
   requesting authentication;
4. complete the source-specific Android result only after the direct write or durable atomic Inbox
   write succeeds.

`SaveAutofillCredential` remains the single Entry materialization path. The Inbox does not grow a
parallel repository or a second credential model.

`Queued` means **durably accepted into the sealed Inbox**, not imported into the Vault. Platform
success is returned because the platform contracts have no later result channel after a durable
queue write. Internally, state, logs and metrics always distinguish `queued` from `imported`; only a
committed SQLCipher import is `Saved`/`Imported`. If Android's own save UI describes the completed
callback as “saved” and offers no customizable post-result wording, Passly accepts that platform
wording but does not mirror it into internal state or user-visible Vault contents before import.

### Legacy Autofill

`LegacyAutofillService.onSaveRequest()` continues to parse the latest `FillContext`, rerun field
classification, and recheck the Legacy save policy. It then calls the shared coordinator.

- `Saved` and `Queued` call `SaveCallback.onSuccess()` exactly once.
- invalid, unavailable, full, oversized, or failed Inbox outcomes call `onFailure()` exactly once
  with a non-sensitive message. They never silently discard an older record and never launch
  authentication.
- it no longer returns an authentication `IntentSender` for locked-state saving.
- `LegacySaveActivity`, its navigation route/ViewModel, `LegacySavePendingIntentFactory`, the
  plaintext `LegacySaveDraftStore`, `LEGACY_SAVE` interaction source, and their obsolete tests are
  deleted after all consumers migrate.

### Credential Manager

Credential Manager create keeps its platform handler and response model. After validating the
system request and current policy, it calls the same coordinator.

- `Saved` and `Queued` produce a successful create result.
- unavailable, full, oversized, or failed Inbox outcomes map to the Credential Manager create
  failure contract; they never report success, discard an older record, or launch authentication.
- no Legacy setting or Legacy Android type enters this path.
- it does not create an Autofill authorization interaction solely for saving.
- fill/discovery authentication and result delivery remain unchanged.

## Inbox Drain and Idempotency

An application-level `AutofillSaveInboxDrainer` observes an opened full or Autofill Vault session.
Security/session code publishes state but does not depend on Autofill packages; the dependency
direction remains `app Autofill -> domain/security ports`.

Drain is single-flight:

1. list pending records in creation order;
2. obtain the Vault DEK only inside the key operation and unwrap the Inbox private key;
3. decrypt one record and validate its version, AAD, bounds, and duplicated encrypted `draft_id`;
4. materialize the typed draft with its preallocated `target_entry_id`;
5. import through the SQLCipher transaction protocol below;
6. remove the pending file only after the database transaction commits;
7. wipe plaintext and key material before processing the next record.

### Import transaction protocol

SQLCipher adds an `autofill_import_log` table whose primary key is the 16-byte `draft_id`. A single
database transaction performs these steps:

1. insert `autofill_import_log(draft_id, imported_at_ms, entry_id)`;
2. materialize and insert the Entry, secret fields, revision and activity records with the encrypted
   `target_entry_id`;
3. commit both the import marker and Entry changes together.

Any failure rolls back both steps. If the `draft_id` primary-key insert conflicts, the importer reads
the existing marker, treats the draft as already imported, and deletes the pending file without
creating another Entry. The file is deleted only after a successful transaction or a verified
existing marker. A crash after commit and before deletion is therefore safe even if the imported
Entry is subsequently moved or deleted.

Import-log cleanup retains markers for 30 days, which is longer than the 24-hour pending lifetime
and 7-day quarantine lifetime. Cleanup must not remove a marker while any active or quarantined file
with the same draft ID remains.

Classification of failures:

- unsupported version, invalid length, AAD mismatch, OAEP/GCM authentication failure, or mismatched
  draft ID: move to quarantine and emit a redacted ERROR;
- database/session unavailable: abort the drain and retain all remaining active records;
- record-specific transient write failure: update retry state, back off, and quarantine after the
  third failed attempt;
- existing import-log draft ID: treat as imported and remove the pending file;
- expired record: remove without decrypting and emit an aggregated INFO count.

The drainer never runs while the Vault is sealed and never opens the Vault itself.

## Logging and Privacy

The generic observability system receives typed Autofill statements but no special export format.
Events include operation/source/outcome, queue size, ciphertext size, retry count, elapsed time, and
reason/error code. They never include draft fields, filenames, raw draft IDs, Entry IDs, package
names, domains, ciphertext, key material, or exception messages. When record-level correlation is
needed, the logger emits an HMAC-derived `pending_ref`; it never emits the raw `draft_id`.

Required semantic events:

- save accepted directly;
- save durably queued;
- Inbox unavailable/full;
- drain started/completed with aggregate counts;
- record import transient failure;
- record rejected as corrupt or unsupported;
- key provisioning/unwrap failure.

Errors use the centralized catalog and sanitized exception mapping. Suggested child codes live
under the existing Autofill category rather than creating a second Autofill-only catalog:

```text
AUTOFILL_SAVE_INBOX_UNAVAILABLE
AUTOFILL_SAVE_INBOX_FULL
AUTOFILL_SAVE_INBOX_CRYPTO_FAILED
AUTOFILL_SAVE_INBOX_CORRUPT
AUTOFILL_SAVE_INBOX_IMPORT_FAILED
```

## Module and Ownership Boundaries

Target responsibilities:

```text
domain
  AutofillPendingSaveId and typed outcomes/ports only

core
  Inbox key derivation, private-key envelope, hybrid envelope codec

data
  bootstrap public/private-envelope persistence
  internal atomic pending-record file store
  autofill_import_log and transactional idempotent Entry writer

app
  AcceptAutofillSave coordinator
  AutofillSaveInboxDrainer
  Legacy and Credential Manager adapters
```

Domain does not depend on cryptographic formats or filesystem types. Data does not own
authentication policy. Security does not depend on Autofill orchestration. Platform adapters do not
write files or repositories directly.

## Validation Strategy

Tests must prove behavior, not preserve old class names or structural text:

### Cryptography and storage

- public material seals a payload while no decrypt API is available to the locked writer;
- private-key envelope parsing uses the exact `PASSLYINBOXKEY` prefix and rejects any changed magic,
  version, key ID, fingerprint, nonce, ciphertext length, ciphertext or tag;
- an absent Proto message provisions lazily after unlock, while a partial, zero-version or unknown
  nested message fails integrity checks and is never overwritten as if absent;
- the bootstrap store saves and clears the nested Inbox material in one DataStore transaction and
  exposes no partial child-field updates;
- correct Vault-derived material opens it; a different Vault DEK, modified header, nonce,
  ciphertext, or wrapped key fails authentication;
- plaintext secret fixtures never occur in the envelope file;
- oversized, truncated, unsupported, and path-manipulated records are rejected;
- atomic-write recovery removes temporary files without corrupting committed records;
- capacity and retention boundaries are deterministic across multiple key-ID directories;
- startup/unlock removes non-current key-ID directories but retains current quarantine until expiry;
- manifest and extraction-rule verification keeps the Inbox root and `bootstrap.pb` outside Android
  Auto Backup and device transfer.

### Save behavior

- Legacy with an open Autofill/full session writes directly, creates no Inbox file, and does not
  authenticate;
- Legacy with a sealed session queues without calling `AuthenticationRequester`; fill and read
  operations still authenticate;
- Credential Manager create with an open session writes directly and does not create a pending
  record;
- Credential Manager create with a sealed session queues without authentication and does not reuse
  Legacy `SaveInfo` semantics;
- a queued platform success is represented internally as `Queued`, never `Saved` or `Imported`;
- unavailable provisioning and full storage report failure rather than false success;
- Legacy callback and Credential Manager result complete exactly once;
- Legacy and Credential Manager platform results remain independent.

### Drain behavior

- opening a session drains queued records in order;
- database failure retains the record;
- a crash after SQLCipher commit but before file deletion is absorbed by the import-log unique key
  and cannot create a duplicate;
- corrupt records do not block later valid records;
- a replaced public key or private/public mismatch stops import, quarantines affected records, and
  writes no Entry;
- drain cancellation preserves the current uncommitted record and wipes plaintext buffers;
- Vault deletion clears keys and pending files.

### Regression verification

- existing fill authentication, candidate scoping, result-before-close ordering, and both Autofill
  implementations continue to pass;
- module-boundary verification, lint, unit tests, and debug assembly pass on the final revision;
- obsolete save-authentication tests are deleted rather than kept as guards for removed behavior.

## Implementation Phases

Each phase is independently compilable and committed as one task:

1. Add Inbox domain contracts, key-envelope codec, the nested bootstrap message, provisioning, and focused
   cryptographic tests.
2. Add the bounded atomic Inbox record store and its format/security tests.
3. Add idempotent Entry import and the session-driven drainer.
4. Replace Legacy locked-save authentication with direct-or-queued coordination and delete the old
   continuation Activity/store/route.
5. Move Credential Manager create onto the same coordinator and remove save-only authorization.
6. Update current Autofill/security documentation, ADR-0023 consequences, error catalog, and run
   complete verification; remove only tests made obsolete by deleted production behavior.

## Rejected Alternatives

- Do not use `FLAG_DELAY_SAVE` as a cross-session queue; Android offers no later Passly-controlled
  consume operation.
- Do not keep the SQLCipher key loaded behind a write-only repository interface; the process would
  still hold read capability.
- Do not restore an authentication-free symmetric `silent` key; it allows locked-state decryption.
- Do not use an ephemeral private key that is destroyed at lock; accepted records would become
  unrecoverable after process death.
- Do not use a device-authentication validity window as the Vault lock boundary; its lifecycle and
  authenticator are different from Passly authentication.
- Do not merge Legacy and Credential Manager Android result types; only the internal acceptance and
  Inbox operations are shared.

## Official References

- [SaveInfo.FLAG_DELAY_SAVE](https://developer.android.com/reference/android/service/autofill/SaveInfo#FLAG_DELAY_SAVE)
- [Build Autofill services](https://developer.android.com/identity/autofill/autofill-services)
- [KeyGenParameterSpec.Builder](https://developer.android.com/reference/android/security/keystore/KeyGenParameterSpec.Builder)
