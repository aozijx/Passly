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
            ExceptionMapper.map(ErrorCodes.Database.DatabaseOpenFailed, obfuscatedFailure),
        )
        assertEquals(
            ExceptionKind.AUTHENTICATION,
            ExceptionMapper.map(ErrorCodes.Security.AuthenticationFailed, obfuscatedFailure),
        )
        assertEquals(
            ExceptionKind.PLATFORM,
            ExceptionMapper.map(ErrorCodes.Autofill.AutofillResultDeliveryFailed, obfuscatedFailure),
        )
    }

    @Test
    fun `unclassified errors use controlled throwable families only`() {
        assertEquals(
            ExceptionKind.CRYPTO,
            ExceptionMapper.map(ErrorCodes.Application.ApplicationUnhandledException, AEADBadTagException()),
        )
        assertEquals(
            ExceptionKind.IO,
            ExceptionMapper.map(ErrorCodes.Application.ApplicationUnhandledException, IOException("secret")),
        )
        assertEquals(
            ExceptionKind.UNKNOWN,
            ExceptionMapper.map(ErrorCodes.Application.ApplicationUnhandledException, RuntimeException("secret")),
        )
    }
}
