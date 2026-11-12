package com.aozijx.passly.core.observability

import java.io.IOException
import javax.crypto.AEADBadTagException
import org.junit.Assert.assertEquals
import org.junit.Test

class ExceptionMapperTest {
    @Test
    fun `error context produces stable kinds without depending on exception class names`() {
        val obfuscatedFailure = object : RuntimeException("P@ssw0rd!") {}

        assertEquals(
            ExceptionKind.DATABASE_IO,
            ExceptionMapper.map(ErrorCodes.Database.OpenReset.DatabaseOpenFailed, Reasons.Database.OpenReset.DatabaseOpenFailed, obfuscatedFailure),
        )
        assertEquals(
            ExceptionKind.AUTHENTICATION,
            ExceptionMapper.map(ErrorCodes.Security.Authentication.AuthenticationFailed, Reasons.Security.Authentication.AuthenticationFailed, obfuscatedFailure),
        )
        assertEquals(
            ExceptionKind.PLATFORM,
            ExceptionMapper.map(
                ErrorCodes.Autofill.Delivery.AutofillResultDeliveryFailed,
                Reasons.Autofill.Platform.PlatformCallbackFailed,
                obfuscatedFailure,
            ),
        )
    }

    @Test
    fun `broad error codes use the reason to classify the failing subsystem`() {
        val failure = RuntimeException("secret")

        assertEquals(
            ExceptionKind.IO,
            ExceptionMapper.map(
                ErrorCodes.Application.Resources.ResourceCleanupFailed,
                Reasons.Application.Storage.FileDeleteFailed,
                failure,
            ),
        )
        assertEquals(
            ExceptionKind.CRYPTO,
            ExceptionMapper.map(
                ErrorCodes.Security.Vault.VaultOperationFailed,
                Reasons.Security.KeyManagement.DekVerificationFailed,
                failure,
            ),
        )
        assertEquals(
            ExceptionKind.LIFECYCLE,
            ExceptionMapper.map(
                ErrorCodes.Security.Vault.VaultOperationFailed,
                Reasons.Application.Callback.CallbackFailed,
                failure,
            ),
        )
    }

    @Test
    fun `unclassified errors use controlled throwable families only`() {
        assertEquals(
            ExceptionKind.CRYPTO,
            ExceptionMapper.map(
                ErrorCodes.Application.Runtime.ApplicationUnhandledException,
                Reasons.Application.Fallback.UnknownFailure,
                AEADBadTagException(),
            ),
        )
        assertEquals(
            ExceptionKind.IO,
            ExceptionMapper.map(
                ErrorCodes.Application.Runtime.ApplicationUnhandledException,
                Reasons.Application.Fallback.UnknownFailure,
                IOException("secret"),
            ),
        )
        assertEquals(
            ExceptionKind.UNKNOWN,
            ExceptionMapper.map(
                ErrorCodes.Application.Runtime.ApplicationUnhandledException,
                Reasons.Application.Fallback.UnknownFailure,
                RuntimeException("secret"),
            ),
        )
    }
}
