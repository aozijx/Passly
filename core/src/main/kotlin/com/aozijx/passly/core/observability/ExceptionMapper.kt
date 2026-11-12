package com.aozijx.passly.core.observability

import java.io.IOException
import java.security.GeneralSecurityException

/** Maps failures to a stable diagnostic vocabulary without inspecting exception messages. */
object ExceptionMapper {
    fun map(
        errorCode: ErrorCode,
        reason: ReasonCode,
        throwable: Throwable,
    ): ExceptionKind =
        when (errorCode) {
            ErrorCodes.Database.Session.DatabaseSessionOpenFailed,
            ErrorCodes.Database.Session.DatabaseSessionCloseFailed,
            ErrorCodes.Database.OpenReset.DatabaseOpenFailed,
            ErrorCodes.Database.OpenReset.DatabaseResetFailed,
            ErrorCodes.Database.Initialization.DatabaseInitFailed,
            -> {
                ExceptionKind.DATABASE_IO
            }

            ErrorCodes.Security.Authentication.AuthenticationFailed,
            ErrorCodes.Security.Biometric.BiometricPromptFailed,
            ErrorCodes.Database.Key.DatabaseLocked,
            -> {
                ExceptionKind.AUTHENTICATION
            }

            ErrorCodes.Security.Cryptography.CryptoError,
            ErrorCodes.Security.Cryptography.CryptoDataCorrupted,
            -> ExceptionKind.CRYPTO

            ErrorCodes.Security.Session.SessionModeRestricted -> ExceptionKind.LIFECYCLE

            ErrorCodes.Application.Storage.FileIoError -> ExceptionKind.IO

            ErrorCodes.Security.Lock.SecurityLockFailed -> {
                ExceptionKind.LIFECYCLE
            }

            ErrorCodes.Application.Resources.ResourceCleanupFailed -> {
                ExceptionKind.IO
            }

            ErrorCodes.Autofill.Discovery.AutofillDiscoveryUnavailable,
            ErrorCodes.Autofill.Delivery.AutofillResultDeliveryFailed,
            ErrorCodes.Autofill.Session.AutofillSessionCloseFailed,
            ErrorCodes.Autofill.Save.AutofillSaveFailed,
            ErrorCodes.Application.Package.PackageIconUnavailable,
            ErrorCodes.Application.Scanner.ScannerFailed,
            ErrorCodes.Application.Native.NativeLibraryLoadFailed,
            -> {
                ExceptionKind.PLATFORM
            }

            ErrorCodes.Autofill.Inbox.AutofillInboxKeyProvisionFailed -> {
                when (reason) {
                    Reasons.Security.Integrity.IntegrityFailure,
                    Reasons.Security.KeyManagement.DekVerificationFailed,
                    Reasons.Security.KeyManagement.DekInstallFailed,
                    -> ExceptionKind.CRYPTO

                    else -> mapThrowableFamily(throwable)
                }
            }

            ErrorCodes.Backup.Operation.BackupOperationFailed -> {
                when (reason) {
                    Reasons.Backup.Authentication.BackupAuthenticationFailed -> ExceptionKind.AUTHENTICATION

                    Reasons.Security.Integrity.IntegrityFailure,
                    Reasons.Backup.Resource.BackupResourceManifestMismatch,
                    Reasons.Backup.Resource.BackupResourceReferenceMismatch,
                    Reasons.Backup.Resource.BackupResourceSizeMismatch,
                    Reasons.Backup.Resource.BackupResourceDigestMismatch,
                    -> ExceptionKind.CRYPTO

                    else -> mapThrowableFamily(throwable)
                }
            }

            ErrorCodes.Application.Diagnostics.DiagnosticsExportFailed -> {
                ExceptionKind.IO
            }

            ErrorCodes.Security.Vault.VaultOperationFailed -> {
                when (reason) {
                    Reasons.Security.KeyManagement.DekVerificationFailed,
                    Reasons.Security.KeyManagement.DekInstallFailed,
                    Reasons.Security.Vault.UnlockFailed,
                    -> ExceptionKind.CRYPTO

                    Reasons.Security.Authentication.AuthenticationFailed -> ExceptionKind.AUTHENTICATION

                    Reasons.Application.Callback.CallbackFailed -> ExceptionKind.LIFECYCLE

                    else -> mapThrowableFamily(throwable)
                }
            }

            else -> {
                mapThrowableFamily(throwable)
            }
        }

    private fun mapThrowableFamily(throwable: Throwable): ExceptionKind =
        when (throwable) {
            is GeneralSecurityException -> ExceptionKind.CRYPTO
            is IOException -> ExceptionKind.IO
            else -> ExceptionKind.UNKNOWN
        }
}
