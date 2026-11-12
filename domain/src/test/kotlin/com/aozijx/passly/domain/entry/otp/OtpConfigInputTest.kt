package com.aozijx.passly.domain.entry.otp

import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OtpConfigInputTest {
    private val totp =
        OtpConfigInput.Totp(
            secret = " my====== ",
            algorithm = OtpHashAlgorithm.SHA1,
            digits = "6",
            periodSeconds = "30",
            encoding = OtpSecretEncoding.BASE32,
            issuer = " Bank ",
            accountName = " alice ",
        )

    @Test
    fun `valid TOTP input keeps chosen parameters and canonicalizes base32`() {
        val config = (totp.copy(digits = "7", periodSeconds = "60").parse() as OtpConfigInputResult.Valid).config
        assertEquals("MY", config.secret)
        assertEquals(7, (config as OtpConfig.Totp).digits)
        assertEquals(60, config.periodSeconds)
        assertEquals("Bank", config.issuer)
        assertEquals("alice", config.accountName)
    }

    @Test
    fun `invalid active TOTP numeric fields cannot acquire defaults`() {
        for (value in listOf("", "bad", "-1", "0", "2147483648")) {
            assertInvalid(totp.copy(periodSeconds = value), FieldKey.OTP_PERIOD)
        }
        for (value in listOf("", "bad", "5", "9", "2147483648")) {
            assertInvalid(totp.copy(digits = value), FieldKey.OTP_DIGITS)
        }
    }

    @Test
    fun `HOTP owns a counter and cannot carry a time period`() {
        val input =
            OtpConfigInput.Hotp(
                secret = totp.secret,
                algorithm = OtpHashAlgorithm.SHA1,
                digits = "6",
                counter = "0",
                encoding = OtpSecretEncoding.BASE32,
                issuer = totp.issuer,
                accountName = totp.accountName,
            )
        for (counter in listOf(0L, Long.MAX_VALUE)) {
            val config = (input.copy(counter = counter.toString()).parse() as OtpConfigInputResult.Valid).config
            assertEquals(counter, (config as OtpConfig.Hotp).counter)
        }
        for (value in listOf("", "bad", "-1", "9223372036854775808")) {
            assertInvalid(input.copy(counter = value), FieldKey.OTP_COUNTER)
        }
    }

    @Test
    fun `Steam input has no configurable algorithm digits period or counter`() {
        val input =
            OtpConfigInput.Steam(
                secret = totp.secret,
                encoding = totp.encoding,
                issuer = totp.issuer,
                accountName = totp.accountName,
            )
        assertTrue(input.parse() is OtpConfigInputResult.Valid)
    }

    @Test
    fun `secrets are validated using their selected encoding`() {
        val base64 = totp.copy(secret = " Zm9v ", encoding = OtpSecretEncoding.BASE64)
        val config = (base64.parse() as OtpConfigInputResult.Valid).config
        assertEquals("Zm9v", config.secret)
        assertEquals(OtpSecretEncoding.BASE64, config.encoding)
        assertInvalid(base64.copy(secret = "INVALID!"), FieldKey.OTP_SECRET)
        assertInvalid(totp.copy(secret = "MZ======"), FieldKey.OTP_SECRET)
        assertInvalid(totp.copy(secret = ""), FieldKey.OTP_SECRET)
    }

    @Test
    fun `input and valid result text never include secret or identity metadata`() {
        val privateInput = totp.copy(secret = "JBSWY3DPEHPK3PXP", accountName = "private-account")
        for (text in listOf(privateInput.toString(), privateInput.parse().toString())) {
            assertFalse(text.contains("JBSWY3DPEHPK3PXP"))
            assertFalse(text.contains("private-account"))
        }
    }

    private fun assertInvalid(
        value: OtpConfigInput,
        field: FieldKey,
    ) {
        val result = value.parse()
        assertTrue(result is OtpConfigInputResult.Invalid)
        assertTrue(field in (result as OtpConfigInputResult.Invalid).fields)
    }
}
