package com.aozijx.passly.core.observability

import java.io.IOException
import java.security.GeneralSecurityException

/** Maps failures to a stable diagnostic vocabulary without inspecting exception messages. */
object ExceptionMapper {
    fun map(
        errorCode: ErrorCode,
        reason: ReasonCode,
        throwable: Throwable,
    ): ExceptionKind = when (errorCode) {
        ErrorCodes.Database.DatabaseSessionOpenFailed,
        ErrorCodes.Database.DatabaseSessionCloseFailed,
        ErrorCodes.Database.DatabaseOpenFailed,
        ErrorCodes.Database.DatabaseResetFailed,
        -> ExceptionKind.DATABASE_IO

        ErrorCodes.Security.AuthenticationFailed,
        ErrorCodes.Security.BiometricPromptFailed,
        -> ExceptionKind.AUTHENTICATION

        ErrorCodes.Security.SecurityLockFailed -> ExceptionKind.LIFECYCLE
        ErrorCodes.Application.ResourceCleanupFailed -> ExceptionKind.IO

        ErrorCodes.Autofill.AutofillDiscoveryUnavailable,
        ErrorCodes.Autofill.AutofillResultDeliveryFailed,
        ErrorCodes.Autofill.AutofillSessionCloseFailed,
        ErrorCodes.Autofill.AutofillSaveFailed,
        ErrorCodes.Application.PackageIconUnavailable,
        ErrorCodes.Application.ScannerFailed,
        ErrorCodes.Application.NativeLibraryLoadFailed,
        -> ExceptionKind.PLATFORM

        ErrorCodes.Application.DiagnosticsExportFailed -> ExceptionKind.IO
        ErrorCodes.Security.VaultOperationFailed -> when (reason) {
            Reasons.DekVerificationFailed,
            Reasons.DekInstallFailed,
            Reasons.UnlockFailed,
            -> ExceptionKind.CRYPTO
            Reasons.AuthenticationFailed -> ExceptionKind.AUTHENTICATION
            Reasons.CallbackFailed -> ExceptionKind.LIFECYCLE
            else -> mapThrowableFamily(throwable)
        }
        else -> mapThrowableFamily(throwable)
    }

    private fun mapThrowableFamily(throwable: Throwable): ExceptionKind = when (throwable) {
        is GeneralSecurityException -> ExceptionKind.CRYPTO
        is IOException -> ExceptionKind.IO
        else -> ExceptionKind.UNKNOWN
    }
}
