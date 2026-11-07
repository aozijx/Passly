# Backup Data Port Design

## Intent

Reduce the App Backup adapter's dependency on Data implementation classes while keeping archive formats, compatibility rules, stable wire keys, and import planning in App. Domain owns a persistence-neutral vault snapshot contract; Data implements it over Room, secret fields, and Vault resource storage.

## Ownership

- `:app` continues to own Backup operations, archive models and codecs, stable-key mapping, compatibility filtering, and import planning.
- `:domain` owns only the vault data contract needed by Backup: snapshot records composed from Domain `Entry` and `EntryLink` models, typed Vault resource content, read options, restore mode, result, and the `VaultBackupStore` port.
- `:data` implements `VaultBackupStore`; Room entities, `AppDatabaseSession`, mappers, `SecretFieldStore`, attachment GC/encryption, icon and attachment file access, and the resource file journal stay inside Data.
- No `:data` source imports App or Backup archive types. No Room Entity or Data implementation type crosses into App Backup code after cutover.

## Contract

`VaultBackupStore.read(options)` returns selected `Entry` and `EntryLink` values and, when requested, typed icon/attachment metadata and plaintext content. `VaultBackupStore.restore(snapshot, mode)` persists entries, relations, icons, and attachments and returns imported/skipped counts. `VaultBackupReadOptions` carries deleted-entry, entry-type, icon, and attachment filters plus per-resource and cumulative plaintext byte limits supplied by the caller. Data owns accumulated resource buffers until snapshot construction succeeds, clears them if a later read fails, and transfers them to the snapshot on success. The App archive mapper transfers ownership to a validated bundle or clears the snapshot buffers on failure. Restore mode is a Domain enum with explicit `APPEND` and `OVERWRITE` cases.

Icon paths remain represented on Domain `EntryIcon`, but Data owns their file validation and materialization because those paths are stored with Room entries and must commit with the database transaction. Data uses one file journal for both resource kinds, retains the attachment mutation lock, GC reactivation, and rollback behavior. Post-commit cleanup remains best effort: ordinary cleanup and logger exceptions do not change the committed restore result, while cancellation and fatal `Error` propagate.

The App gateway remains the protocol-to-Domain adapter: it maps archive records to Domain values on import and Domain snapshots to stable-key archive records on export. The compatibility cutover is one-way: migrate this gateway, then delete `RoomBackupSnapshotReader` and `RoomBackupSnapshotRestorer`; do not retain a forwarding implementation.

## Invariants

- Backup archive v1, Room schema v1, Proto schema, and stable external keys do not change.
- An overwrite with source entries that compatibility filtering removed all remains rejected before clearing stored data; a genuinely empty source archive retains current behavior.
- Append skips conflicting entry IDs inside the database transaction; links are restored only when both endpoints exist.
- Cancellation and fatal `Error` propagate. Failed restore rolls back database and staged file changes; post-commit cleanup failures do not report a committed restore as uncommitted.
- Attachment content IDs, encrypted-at-rest representation, icon paths, and existing resource-reference semantics remain unchanged.

## Verification

Run Domain and Data unit tests, Data AndroidTest compilation and relevant restore instrumentation tests when a device is available, `verifyArchitecture`, App Backup tests, App Kotlin compilation, and `git diff --check`. Inspect source imports after cutover to confirm App Backup no longer imports `com.aozijx.passly.data.*` implementation packages and Data does not import App Backup packages.
