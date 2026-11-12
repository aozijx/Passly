# Passly Error Codes

> Generated from `config/observability/error-catalog.json`. Do not edit by hand.

## Errors by category

### Autofill

| Code | Default reason | Exposure | Description |
|---|---|---|---|
| `AUTOFILL_DISCOVERY_UNAVAILABLE` | `NO_MATCHING_ENTRY` | INTERNAL | Autofill discovery could not produce a usable result. |
| `AUTOFILL_RESULT_DELIVERY_FAILED` | `PLATFORM_CALLBACK_FAILED` | INTERNAL | Autofill result delivery failed. |
| `AUTOFILL_SESSION_CLOSE_FAILED` | `SESSION_CLOSE_FAILED` | INTERNAL | Autofill session cleanup failed. |
| `AUTOFILL_SAVE_FAILED` | `UNKNOWN_FAILURE` | INTERNAL | Autofill save processing failed. |

### Database

| Code | Default reason | Exposure | Description |
|---|---|---|---|
| `DATABASE_SESSION_OPEN_FAILED` | `SESSION_OPEN_FAILED` | INTERNAL | The managed database session failed to open. |
| `DATABASE_SESSION_CLOSE_FAILED` | `SESSION_CLOSE_FAILED` | INTERNAL | The managed database session failed to close. |
| `DATABASE_OPEN_FAILED` | `DATABASE_OPEN_FAILED` | INTERNAL | The database failed to open. |
| `DATABASE_RESET_FAILED` | `DATABASE_RESET_FAILED` | INTERNAL | The database reset failed. |

### Security

| Code | Default reason | Exposure | Description |
|---|---|---|---|
| `AUTHENTICATION_FAILED` | `AUTHENTICATION_FAILED` | USER_ACTIONABLE | Authentication failed and the user can retry. |
| `SECURITY_LOCK_FAILED` | `UNKNOWN_FAILURE` | INTERNAL | The vault lock operation failed. |
| `BIOMETRIC_PROMPT_FAILED` | `AUTHENTICATION_FAILED` | USER_ACTIONABLE | Biometric authentication failed and the user can retry. |
| `VAULT_OPERATION_FAILED` | `UNKNOWN_FAILURE` | INTERNAL | A protected vault operation failed. |

### Application

| Code | Default reason | Exposure | Description |
|---|---|---|---|
| `DIAGNOSTICS_EXPORT_FAILED` | `STORAGE_FAILURE` | INTERNAL | Diagnostics export failed. |
| `APPLICATION_UNHANDLED_EXCEPTION` | `UNKNOWN_FAILURE` | INTERNAL | An unhandled application failure reached the crash boundary. |
| `NATIVE_LIBRARY_LOAD_FAILED` | `NATIVE_LIBRARY_UNAVAILABLE` | INTERNAL | A required native library failed to load. |
| `PACKAGE_ICON_UNAVAILABLE` | `PACKAGE_NOT_INSTALLED` | INTERNAL | A package icon could not be loaded. |
| `SCANNED_OTP_INVALID` | `INVALID_INPUT` | INTERNAL | A scanned OTP payload failed validation. |
| `SCANNER_FAILED` | `SCANNER_PROCESSING_FAILED` | INTERNAL | Scanner processing failed. |
| `RESOURCE_CLEANUP_FAILED` | `FILE_DELETE_FAILED` | INTERNAL | Confined resource cleanup failed. |

### Vault

| Code | Default reason | Exposure | Description |
|---|---|---|---|
| `VAULT_ENTRY_CREATE_FAILED` | `UNKNOWN_FAILURE` | INTERNAL | A vault entry could not be created. |
| `VAULT_ENTRY_UPDATE_FAILED` | `UNKNOWN_FAILURE` | INTERNAL | A vault entry could not be updated. |

### Backup

| Code | Default reason | Exposure | Description |
|---|---|---|---|
| `BACKUP_FAILED` | `UNKNOWN_FAILURE` | INTERNAL | A backup import or export operation failed. |

## Reasons

| Code | Description |
|---|---|
| `NO_FILLABLE_FIELDS` | The request contains no supported fillable fields. |
| `SESSION_RESTRICTED` | The secure session does not permit the operation. |
| `DATABASE_UNAVAILABLE` | The encrypted database is unavailable. |
| `NO_MATCHING_ENTRY` | No matching vault entry was discovered. |
| `PLATFORM_CALLBACK_FAILED` | An Android platform result callback failed. |
| `DATABASE_RESOURCE_CLOSE_FAILED` | The encrypted database resource could not close cleanly. |
| `AUTHENTICATION_REJECTED` | The authentication result was rejected. |
| `STORAGE_FAILURE` | A controlled storage operation failed. |
| `UNKNOWN_FAILURE` | The failure has no narrower safe reason. |
| `NATIVE_LIBRARY_UNAVAILABLE` | A required native library is unavailable. |
| `SESSION_OPEN` | A database session is opening. |
| `SESSION_OPEN_FAILED` | A database session failed to open. |
| `SESSION_RESUME` | A database session is resuming. |
| `SESSION_SOFT_LOCK` | A database session is entering a soft lock. |
| `SESSION_SEAL` | A database session is sealing its material. |
| `SESSION_DRAIN_TIMEOUT` | Active database leases did not drain before timeout. |
| `SESSION_CLOSE_FAILED` | A session close operation failed. |
| `SESSION_SOFT_LOCK_FAILED` | The secure session could not enter soft-lock state. |
| `SESSION_SEAL_FAILED` | The secure session could not seal its protected resources. |
| `USER` | The user explicitly initiated the operation. |
| `IDLE_TIMEOUT` | The configured idle timeout elapsed. |
| `AUTOFILL_REQUEST_FINISHED` | An Autofill request completed its observable lifecycle. |
| `BACKGROUND` | The application entered the background. |
| `RECOVERY_EXIT` | The recovery flow exited. |
| `INTEGRITY_FAILURE` | A protected integrity check failed. |
| `APP_EXIT` | The application exit path initiated the operation. |
| `PACKAGE_NOT_INSTALLED` | The requested package is not installed. |
| `IMAGE_DECODE_FAILED` | A non-sensitive image could not be decoded. |
| `FILE_DELETE_FAILED` | A confined file deletion failed. |
| `DATABASE_OPEN_FAILED` | The database could not be opened. |
| `DATABASE_RESET_FAILED` | The database reset operation failed. |
| `INVALID_CONFIGURATION` | A controlled configuration value is invalid. |
| `HOST_UNAVAILABLE` | A required Android host is unavailable. |
| `PERMISSION_DENIED` | The required permission was denied. |
| `INVALID_INPUT` | Input failed controlled validation. |
| `SCANNER_PROCESSING_FAILED` | The scanner could not process the frame. |
| `AUTHENTICATION_FAILED` | Authentication failed without a narrower safe reason. |
| `DEK_VERIFICATION_FAILED` | The data-encryption-key verification failed. |
| `DEK_INSTALL_FAILED` | The data-encryption-key installation failed. |
| `UNLOCK_FAILED` | The vault unlock operation failed. |
| `CALLBACK_FAILED` | A controlled application callback failed. |
| `NOT_FOUND` | The requested vault object no longer exists. |
| `CONFLICT` | The operation conflicted with a newer vault state. |
| `KEYSTORE_UNAVAILABLE` | The Android keystore biometric key operation failed. |
| `BACKUP_AUTHENTICATION_FAILED` | Encrypted backup authentication failed; the password may be incorrect or the file may be damaged. |
| `BACKUP_CONTAINER_HEADER_TRUNCATED` | The encrypted backup container header ended before all required bytes were read. |
| `BACKUP_RECORD_HEADER_TRUNCATED` | An encrypted backup record header ended before all required bytes were read. |
| `BACKUP_RECORD_DATA_TRUNCATED` | An encrypted backup record payload ended before all required bytes were read. |
| `BACKUP_TERMINAL_MISSING` | The encrypted backup is missing its authenticated terminal record. |
| `BACKUP_RESOURCE_MANIFEST_MISMATCH` | Declared backup resources do not match the resources present in the archive. |
| `BACKUP_RESOURCE_REFERENCE_MISMATCH` | Backup entries reference a resource that is absent from the validated resource set. |
| `BACKUP_RESOURCE_SIZE_MISMATCH` | A backup resource size does not match its declared size. |
| `BACKUP_RESOURCE_DIGEST_MISMATCH` | A backup resource digest does not match its content. |
| `BACKUP_ZIP_DOCUMENT_MISSING` | The backup ZIP does not contain the required document entry. |
| `BACKUP_ZIP_DUPLICATE_ENTRY` | The backup ZIP contains duplicate entry names. |
| `BACKUP_ZIP_INVALID_ENTRY` | The backup ZIP contains an invalid or unsupported entry path. |
| `BACKUP_DOCUMENT_FORMAT_UNSUPPORTED` | The backup document format identifier is unsupported. |
| `BACKUP_DOCUMENT_VERSION_UNSUPPORTED` | The backup document version is unsupported. |
| `BACKUP_DOCUMENT_DUPLICATE_ENTRY_ID` | The backup document contains duplicate entry identifiers. |
| `BACKUP_DOCUMENT_DUPLICATE_RESOURCE_ID` | The backup document contains duplicate resource identifiers. |
| `BACKUP_DOCUMENT_DUPLICATE_LINK_ID` | The backup document contains duplicate link identifiers. |
| `BACKUP_DOCUMENT_MALFORMED` | The backup document could not be decoded as a supported document. |
| `BACKUP_PASSWORD_REQUIRED` | The selected backup format requires a non-empty password. |
| `BACKUP_FORMAT_UNSUPPORTED` | The requested backup format is not supported by the installed application. |
| `BACKUP_CONTENT_UNSUPPORTED` | The backup contains a feature or record that this importer does not support. |
| `BACKUP_ZIP_MALFORMED` | The encrypted backup ZIP structure could not be read. |
