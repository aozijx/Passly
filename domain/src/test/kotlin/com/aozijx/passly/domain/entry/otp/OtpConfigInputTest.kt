package com.aozijx.passly.domain.entry.otp

import com.aozijx.passly.domain.entry.model.FieldKey
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.OtpType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OtpConfigInputTest {
    private val input = OtpConfigInput(secret = " my====== ", issuer = " Bank ", accountName = " alice ")

    @Test
    fun `valid input keeps chosen parameters and canonicalizes base32`() {
        val config = (input.copy(digits = "7", period = "60").parse() as OtpConfigInputResult.Valid).config
        assertEquals("MY", config.secret)
        assertEquals(7, config.digits)
        assertEquals(60, config.periodSeconds)
        assertEquals("Bank", config.issuer)
        assertEquals("alice", config.accountName)
    }

    @Test
    fun `invalid active numeric fields cannot acquire defaults`() {
        for (value in listOf("", "bad", "-1", "0", "2147483648")) {
            assertInvalid(input.copy(period = value), FieldKey.OTP_PERIOD)
        }
        for (value in listOf("", "bad", "5", "9", "2147483648")) {
            assertInvalid(input.copy(digits = value), FieldKey.OTP_DIGITS)
        }
        for (value in listOf("", "bad", "-1", "9223372036854775808")) {
            assertInvalid(input.copy(type = OtpType.HOTP, counter = value), FieldKey.OTP_COUNTER)
        }
    }

    @Test
    fun `HOTP accepts zero and largest long and ignores inactive period`() {
        for (counter in listOf(0L, Long.MAX_VALUE)) {
            val config = (input.copy(type = OtpType.HOTP, counter = counter.toString(), period = "unused")
                .parse() as OtpConfigInputResult.Valid).config
            assertEquals(counter, config.counter)
            assertEquals(null, config.periodSeconds)
        }
    }

    @Test
    fun `Steam parameters must agree with its fixed generator`() {
        val steam = input.copy(type = OtpType.STEAM, digits = "5")
        assertTrue(steam.parse() is OtpConfigInputResult.Valid)
        assertInvalid(steam.copy(digits = "6"), FieldKey.OTP_DIGITS)
        assertInvalid(steam.copy(period = "60"), FieldKey.OTP_PERIOD)
        assertInvalid(steam.copy(algorithm = OtpHashAlgorithm.SHA256), FieldKey.OTP_ALGORITHM)
    }

    @Test
    fun `secrets are validated using their selected encoding`() {
        val base64 = input.copy(secret = " Zm9v ", encoding = OtpSecretEncoding.BASE64)
        val config = (base64.parse() as OtpConfigInputResult.Valid).config
        assertEquals("Zm9v", config.secret)
        assertEquals(OtpSecretEncoding.BASE64, config.encoding)
        assertInvalid(base64.copy(secret = "INVALID!"), FieldKey.OTP_SECRET)
        assertInvalid(input.copy(secret = "MZ======"), FieldKey.OTP_SECRET)
        assertInvalid(input.copy(secret = ""), FieldKey.OTP_SECRET)
    }

    @Test
    fun `input and valid result text never include secret or identity metadata`() {
        val privateInput = input.copy(secret = "JBSWY3DPEHPK3PXP", accountName = "private-account")
        for (text in listOf(privateInput.toString(), privateInput.parse().toString())) {
            assertFalse(text.contains("JBSWY3DPEHPK3PXP"))
            assertFalse(text.contains("private-account"))
        }
    }

    private fun assertInvalid(value: OtpConfigInput, field: FieldKey) {
        val result = value.parse()
        assertTrue(result is OtpConfigInputResult.Invalid)
        assertTrue(field in (result as OtpConfigInputResult.Invalid).fields)
    }
}
