# Passly Error Codes

> Generated from `config/observability/error-catalog.json`. Do not edit by hand.

## Errors by category

### Autofill

| Code | Default reason | Exposure | Description |
|---|---|---|---|
| `Autofill.Discovery.AutofillDiscoveryUnavailable` (`AUTOFILL_DISCOVERY_UNAVAILABLE`) | `Autofill.Matching.NoMatchingEntry` (`NO_MATCHING_ENTRY`) | INTERNAL | Autofill discovery could not produce a usable result. |
| `Autofill.Delivery.AutofillResultDeliveryFailed` (`AUTOFILL_RESULT_DELIVERY_FAILED`) | `Autofill.Platform.PlatformCallbackFailed` (`PLATFORM_CALLBACK_FAILED`) | INTERNAL | Autofill result delivery failed. |
| `Autofill.Session.AutofillSessionCloseFailed` (`AUTOFILL_SESSION_CLOSE_FAILED`) | `Database.Session.SessionCloseFailed` (`SESSION_CLOSE_FAILED`) | INTERNAL | Autofill session cleanup failed. |
| `Autofill.Save.AutofillSaveFailed` (`AUTOFILL_SAVE_FAILED`) | `Application.Fallback.UnknownFailure` (`UNKNOWN_FAILURE`) | INTERNAL | Autofill save processing failed. |
| `Autofill.Inbox.AutofillInboxKeyProvisionFailed` (`AUTOFILL_INBOX_KEY_PROVISION_FAILED`) | `Application.Fallback.UnknownFailure` (`UNKNOWN_FAILURE`) | INTERNAL | Autofill Inbox key provisioning or pending-record processing failed. |

### Database

| Code | Default reason | Exposure | Description |
|---|---|---|---|
| `Database.Session.DatabaseSessionOpenFailed` (`DATABASE_SESSION_OPEN_FAILED`) | `Database.Session.SessionOpenFailed` (`SESSION_OPEN_FAILED`) | INTERNAL | The managed database session failed to open. |
| `Database.Session.DatabaseSessionCloseFailed` (`DATABASE_SESSION_CLOSE_FAILED`) | `Database.Session.SessionCloseFailed` (`SESSION_CLOSE_FAILED`) | INTERNAL | The managed database session failed to close. |
| `Database.Initialization.DatabaseInitFailed` (`DATABASE_INIT_FAILED`) | `Database.OpenReset.DatabaseOpenFailed` (`DATABASE_OPEN_FAILED`) | INTERNAL | The application database could not be initialized. |
| `Database.Key.DatabaseLocked` (`DATABASE_LOCKED`) | `Security.Authentication.AuthenticationRejected` (`AUTHENTICATION_REJECTED`) | INTERNAL | The encrypted database key was rejected. |
| `Database.OpenReset.DatabaseOpenFailed` (`DATABASE_OPEN_FAILED`) | `Database.OpenReset.DatabaseOpenFailed` (`DATABASE_OPEN_FAILED`) | INTERNAL | The database failed to open. |
| `Database.OpenReset.DatabaseResetFailed` (`DATABASE_RESET_FAILED`) | `Database.OpenReset.DatabaseResetFailed` (`DATABASE_RESET_FAILED`) | INTERNAL | The database reset failed. |

### Security

| Code | Default reason | Exposure | Description |
|---|---|---|---|
| `Security.Authentication.AuthenticationFailed` (`AUTHENTICATION_FAILED`) | `Security.Authentication.AuthenticationFailed` (`AUTHENTICATION_FAILED`) | USER_ACTIONABLE | Authentication failed and the user can retry. |
| `Security.Cryptography.CryptoError` (`CRYPTO_ERROR`) | `Application.Fallback.UnknownFailure` (`UNKNOWN_FAILURE`) | INTERNAL | A cryptographic operation failed. |
| `Security.Cryptography.CryptoDataCorrupted` (`CRYPTO_DATA_CORRUPTED`) | `Security.Integrity.IntegrityFailure` (`INTEGRITY_FAILURE`) | INTERNAL | Encrypted data failed integrity validation. |
| `Security.Session.SessionModeRestricted` (`SESSION_MODE_RESTRICTED`) | `Security.Session.SessionRestricted` (`SESSION_RESTRICTED`) | INTERNAL | The current secure session does not allow the operation. |
| `Security.Lock.SecurityLockFailed` (`SECURITY_LOCK_FAILED`) | `Application.Fallback.UnknownFailure` (`UNKNOWN_FAILURE`) | INTERNAL | The vault lock operation failed. |
| `Security.Biometric.BiometricPromptFailed` (`BIOMETRIC_PROMPT_FAILED`) | `Security.Authentication.AuthenticationFailed` (`AUTHENTICATION_FAILED`) | USER_ACTIONABLE | Biometric authentication failed and the user can retry. |
| `Security.Vault.VaultOperationFailed` (`VAULT_OPERATION_FAILED`) | `Application.Fallback.UnknownFailure` (`UNKNOWN_FAILURE`) | INTERNAL | A protected vault operation failed. |

### Application

| Code | Default reason | Exposure | Description |
|---|---|---|---|
| `Application.Storage.FileIoError` (`FILE_IO_ERROR`) | `Application.Storage.StorageFailure` (`STORAGE_FAILURE`) | INTERNAL | A file operation failed. |
| `Application.Validation.ValidationError` (`VALIDATION_ERROR`) | `Application.Validation.InvalidInput` (`INVALID_INPUT`) | INTERNAL | A domain value failed validation. |
| `Application.Runtime.Unexpected` (`UNEXPECTED`) | `Application.Fallback.UnknownFailure` (`UNKNOWN_FAILURE`) | INTERNAL | An unclassified application operation failed. |
| `Application.Diagnostics.DiagnosticsExportFailed` (`DIAGNOSTICS_EXPORT_FAILED`) | `Application.Storage.StorageFailure` (`STORAGE_FAILURE`) | INTERNAL | Diagnostics export failed. |
| `Application.Runtime.ApplicationUnhandledException` (`APPLICATION_UNHANDLED_EXCEPTION`) | `Application.Fallback.UnknownFailure` (`UNKNOWN_FAILURE`) | INTERNAL | An unhandled application failure reached the crash boundary. |
| `Application.Native.NativeLibraryLoadFailed` (`NATIVE_LIBRARY_LOAD_FAILED`) | `Application.Native.NativeLibraryUnavailable` (`NATIVE_LIBRARY_UNAVAILABLE`) | INTERNAL | A required native library failed to load. |
| `Application.Package.PackageIconUnavailable` (`PACKAGE_ICON_UNAVAILABLE`) | `Application.Package.PackageNotInstalled` (`PACKAGE_NOT_INSTALLED`) | INTERNAL | A package icon could not be loaded. |
| `Application.Scanner.ScannedOtpInvalid` (`SCANNED_OTP_INVALID`) | `Application.Validation.InvalidInput` (`INVALID_INPUT`) | INTERNAL | A scanned OTP payload failed validation. |
| `Application.Scanner.ScannerFailed` (`SCANNER_FAILED`) | `Application.Scanner.ScannerProcessingFailed` (`SCANNER_PROCESSING_FAILED`) | INTERNAL | Scanner processing failed. |
| `Application.Resources.ResourceCleanupFailed` (`RESOURCE_CLEANUP_FAILED`) | `Application.Storage.FileDeleteFailed` (`FILE_DELETE_FAILED`) | INTERNAL | Confined resource cleanup failed. |

### Vault

| Code | Default reason | Exposure | Description |
|---|---|---|---|
| `Vault.Lookup.NotFound` (`NOT_FOUND`) | `Vault.Lookup.NotFound` (`NOT_FOUND`) | INTERNAL | A requested vault object no longer exists. |
| `Vault.Mutation.Conflict` (`CONFLICT`) | `Vault.Mutation.Conflict` (`CONFLICT`) | INTERNAL | A mutation conflicted with a newer vault state. |
| `Vault.Entry.VaultEntryCreateFailed` (`VAULT_ENTRY_CREATE_FAILED`) | `Application.Fallback.UnknownFailure` (`UNKNOWN_FAILURE`) | INTERNAL | A vault entry could not be created. |
| `Vault.Entry.VaultEntryUpdateFailed` (`VAULT_ENTRY_UPDATE_FAILED`) | `Application.Fallback.UnknownFailure` (`UNKNOWN_FAILURE`) | INTERNAL | A vault entry could not be updated. |

### Backup

| Code | Default reason | Exposure | Description |
|---|---|---|---|
| `Backup.Operation.BackupOperationFailed` (`BACKUP_FAILED`) | `Application.Fallback.UnknownFailure` (`UNKNOWN_FAILURE`) | INTERNAL | A backup import or export operation failed. |

## Reasons

| Owner | Code | Description |
|---|---|---|
| `Autofill.Matching.NoFillableFields` | `NO_FILLABLE_FIELDS` | The request contains no supported fillable fields. |
| `Security.Session.SessionRestricted` | `SESSION_RESTRICTED` | The secure session does not permit the operation. |
| `Database.Availability.DatabaseUnavailable` | `DATABASE_UNAVAILABLE` | The encrypted database is unavailable. |
| `Autofill.Matching.NoMatchingEntry` | `NO_MATCHING_ENTRY` | No matching vault entry was discovered. |
| `Autofill.Platform.PlatformCallbackFailed` | `PLATFORM_CALLBACK_FAILED` | An Android platform result callback failed. |
| `Database.Resource.DatabaseResourceCloseFailed` | `DATABASE_RESOURCE_CLOSE_FAILED` | The encrypted database resource could not close cleanly. |
| `Security.Authentication.AuthenticationRejected` | `AUTHENTICATION_REJECTED` | The authentication result was rejected. |
| `Application.Storage.StorageFailure` | `STORAGE_FAILURE` | A controlled storage operation failed. |
| `Application.Fallback.UnknownFailure` | `UNKNOWN_FAILURE` | The failure has no narrower safe reason. |
| `Application.Native.NativeLibraryUnavailable` | `NATIVE_LIBRARY_UNAVAILABLE` | A required native library is unavailable. |
| `Database.Session.SessionOpen` | `SESSION_OPEN` | A database session is opening. |
| `Database.Session.SessionOpenFailed` | `SESSION_OPEN_FAILED` | A database session failed to open. |
| `Database.Session.SessionResume` | `SESSION_RESUME` | A database session is resuming. |
| `Database.Session.SessionSoftLock` | `SESSION_SOFT_LOCK` | A database session is entering a soft lock. |
| `Database.Session.SessionSeal` | `SESSION_SEAL` | A database session is sealing its material. |
| `Database.Session.SessionDrainTimeout` | `SESSION_DRAIN_TIMEOUT` | Active database leases did not drain before timeout. |
| `Database.Session.SessionCloseFailed` | `SESSION_CLOSE_FAILED` | A session close operation failed. |
| `Database.Session.SessionSoftLockFailed` | `SESSION_SOFT_LOCK_FAILED` | The secure session could not enter soft-lock state. |
| `Database.Session.SessionSealFailed` | `SESSION_SEAL_FAILED` | The secure session could not seal its protected resources. |
| `Application.Lifecycle.User` | `USER` | The user explicitly initiated the operation. |
| `Application.Lifecycle.IdleTimeout` | `IDLE_TIMEOUT` | The configured idle timeout elapsed. |
| `Autofill.Session.AutofillRequestFinished` | `AUTOFILL_REQUEST_FINISHED` | An Autofill request completed its observable lifecycle. |
| `Application.Lifecycle.Background` | `BACKGROUND` | The application entered the background. |
| `Security.Recovery.RecoveryExit` | `RECOVERY_EXIT` | The recovery flow exited. |
| `Security.Integrity.IntegrityFailure` | `INTEGRITY_FAILURE` | A protected integrity check failed. |
| `Application.Lifecycle.AppExit` | `APP_EXIT` | The application exit path initiated the operation. |
| `Application.Package.PackageNotInstalled` | `PACKAGE_NOT_INSTALLED` | The requested package is not installed. |
| `Application.Image.ImageDecodeFailed` | `IMAGE_DECODE_FAILED` | A non-sensitive image could not be decoded. |
| `Application.Storage.FileDeleteFailed` | `FILE_DELETE_FAILED` | A confined file deletion failed. |
| `Database.OpenReset.DatabaseOpenFailed` | `DATABASE_OPEN_FAILED` | The database could not be opened. |
| `Database.OpenReset.DatabaseResetFailed` | `DATABASE_RESET_FAILED` | The database reset operation failed. |
| `Application.Validation.InvalidConfiguration` | `INVALID_CONFIGURATION` | A controlled configuration value is invalid. |
| `Autofill.Platform.HostUnavailable` | `HOST_UNAVAILABLE` | A required Android host is unavailable. |
| `Autofill.Platform.PermissionDenied` | `PERMISSION_DENIED` | The required permission was denied. |
| `Application.Validation.InvalidInput` | `INVALID_INPUT` | Input failed controlled validation. |
| `Application.Scanner.ScannerProcessingFailed` | `SCANNER_PROCESSING_FAILED` | The scanner could not process the frame. |
| `Security.Authentication.AuthenticationFailed` | `AUTHENTICATION_FAILED` | Authentication failed without a narrower safe reason. |
| `Security.KeyManagement.DekVerificationFailed` | `DEK_VERIFICATION_FAILED` | The data-encryption-key verification failed. |
| `Security.KeyManagement.DekInstallFailed` | `DEK_INSTALL_FAILED` | The data-encryption-key installation failed. |
| `Security.Vault.UnlockFailed` | `UNLOCK_FAILED` | The vault unlock operation failed. |
| `Application.Callback.CallbackFailed` | `CALLBACK_FAILED` | A controlled application callback failed. |
| `Vault.Lookup.NotFound` | `NOT_FOUND` | The requested vault object no longer exists. |
| `Vault.Mutation.Conflict` | `CONFLICT` | The operation conflicted with a newer vault state. |
| `Security.Keystore.KeyStoreUnavailable` | `KEYSTORE_UNAVAILABLE` | The Android keystore biometric key operation failed. |
| `Backup.Authentication.BackupAuthenticationFailed` | `BACKUP_AUTHENTICATION_FAILED` | Encrypted backup authentication failed; the password may be incorrect or the file may be damaged. |
| `Backup.Container.BackupContainerHeaderTruncated` | `BACKUP_CONTAINER_HEADER_TRUNCATED` | The encrypted backup container header ended before all required bytes were read. |
| `Backup.Container.BackupRecordHeaderTruncated` | `BACKUP_RECORD_HEADER_TRUNCATED` | An encrypted backup record header ended before all required bytes were read. |
| `Backup.Container.BackupRecordDataTruncated` | `BACKUP_RECORD_DATA_TRUNCATED` | An encrypted backup record payload ended before all required bytes were read. |
| `Backup.Container.BackupTerminalMissing` | `BACKUP_TERMINAL_MISSING` | The encrypted backup is missing its authenticated terminal record. |
| `Backup.Resource.BackupResourceManifestMismatch` | `BACKUP_RESOURCE_MANIFEST_MISMATCH` | Declared backup resources do not match the resources present in the archive. |
| `Backup.Resource.BackupResourceReferenceMismatch` | `BACKUP_RESOURCE_REFERENCE_MISMATCH` | Backup entries reference a resource that is absent from the validated resource set. |
| `Backup.Resource.BackupResourceSizeMismatch` | `BACKUP_RESOURCE_SIZE_MISMATCH` | A backup resource size does not match its declared size. |
| `Backup.Resource.BackupResourceDigestMismatch` | `BACKUP_RESOURCE_DIGEST_MISMATCH` | A backup resource digest does not match its content. |
| `Backup.Archive.BackupZipDocumentMissing` | `BACKUP_ZIP_DOCUMENT_MISSING` | The backup ZIP does not contain the required document entry. |
| `Backup.Archive.BackupZipDuplicateEntry` | `BACKUP_ZIP_DUPLICATE_ENTRY` | The backup ZIP contains duplicate entry names. |
| `Backup.Archive.BackupZipInvalidEntry` | `BACKUP_ZIP_INVALID_ENTRY` | The backup ZIP contains an invalid or unsupported entry path. |
| `Backup.Document.BackupDocumentFormatUnsupported` | `BACKUP_DOCUMENT_FORMAT_UNSUPPORTED` | The backup document format identifier is unsupported. |
| `Backup.Document.BackupDocumentVersionUnsupported` | `BACKUP_DOCUMENT_VERSION_UNSUPPORTED` | The backup document version is unsupported. |
| `Backup.Document.BackupDocumentDuplicateEntryId` | `BACKUP_DOCUMENT_DUPLICATE_ENTRY_ID` | The backup document contains duplicate entry identifiers. |
| `Backup.Document.BackupDocumentDuplicateResourceId` | `BACKUP_DOCUMENT_DUPLICATE_RESOURCE_ID` | The backup document contains duplicate resource identifiers. |
| `Backup.Document.BackupDocumentDuplicateLinkId` | `BACKUP_DOCUMENT_DUPLICATE_LINK_ID` | The backup document contains duplicate link identifiers. |
| `Backup.Document.BackupDocumentMalformed` | `BACKUP_DOCUMENT_MALFORMED` | The backup document could not be decoded as a supported document. |
| `Backup.Authentication.BackupPasswordRequired` | `BACKUP_PASSWORD_REQUIRED` | The selected backup format requires a non-empty password. |
| `Backup.ImportExport.BackupFormatUnsupported` | `BACKUP_FORMAT_UNSUPPORTED` | The requested backup format is not supported by the installed application. |
| `Backup.ImportExport.BackupContentUnsupported` | `BACKUP_CONTENT_UNSUPPORTED` | The backup contains a feature or record that this importer does not support. |
| `Backup.Archive.BackupZipMalformed` | `BACKUP_ZIP_MALFORMED` | The encrypted backup ZIP structure could not be read. |
