package com.aozijx.passly.core.observability

import java.io.IOException
import java.security.GeneralSecurityException

/** Maps failures to a stable diagnostic vocabulary without inspecting exception messages. */
object ExceptionMapper {
    fun map(errorCode: ErrorCode, throwable: Throwable): ExceptionKind = when (errorCode) {
        ErrorCodes.DatabaseSessionOpenFailed,
        ErrorCodes.DatabaseSessionCloseFailed,
        ErrorCodes.DatabaseOpenFailed,
        ErrorCodes.DatabaseResetFailed,
        -> ExceptionKind.DATABASE_IO

        ErrorCodes.AuthenticationFailed,
        ErrorCodes.BiometricPromptFailed,
        -> ExceptionKind.AUTHENTICATION

        ErrorCodes.SecurityLockFailed,
        ErrorCodes.ResourceCleanupFailed,
        -> ExceptionKind.LIFECYCLE

        ErrorCodes.AutofillDiscoveryUnavailable,
        ErrorCodes.AutofillResultDeliveryFailed,
        ErrorCodes.AutofillSessionCloseFailed,
        ErrorCodes.AutofillSaveFailed,
        ErrorCodes.PackageIconUnavailable,
        ErrorCodes.ScannerFailed,
        ErrorCodes.NativeLibraryLoadFailed,
        -> ExceptionKind.PLATFORM

        ErrorCodes.DiagnosticsExportFailed -> ExceptionKind.IO
        ErrorCodes.VaultOperationFailed -> ExceptionKind.CRYPTO
        else -> mapThrowableFamily(throwable)
    }

    private fun mapThrowableFamily(throwable: Throwable): ExceptionKind = when (throwable) {
        is GeneralSecurityException -> ExceptionKind.CRYPTO
        is IOException -> ExceptionKind.IO
        else -> ExceptionKind.UNKNOWN
    }
}
