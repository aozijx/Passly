package com.aozijx.passly.domain.entry.otp

import com.aozijx.passly.domain.entry.model.otp.OtpGenerationError
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class OtpSecretCodecTest {
    @Test
    fun `base32 encodes and decodes RFC 4648 vectors without padding`() {
        mapOf(
            "f" to "MY", "fo" to "MZXQ", "foo" to "MZXW6",
            "foob" to "MZXW6YQ", "fooba" to "MZXW6YTB", "foobar" to "MZXW6YTBOI",
        ).forEach { (plain, encoded) ->
            val bytes = plain.toByteArray(Charsets.US_ASCII)
            assertEquals(encoded, OtpSecretCodec.encodeBase32(bytes))
            assertArrayEquals(bytes, OtpSecretCodec.decode(encoded, OtpSecretEncoding.BASE32))
        }
    }

    @Test
    fun `empty and malformed keys have the same typed invalid-secret error`() {
        listOf("", "MZ", "MY=====", "M1", "MY======A").forEach { value ->
            assertEquals(OtpGenerationError.InvalidSecret, assertThrows(OtpGenerationError::class.java) {
                OtpSecretCodec.decode(value, OtpSecretEncoding.BASE32)
            })
        }
        listOf("", "!", "Zg===").forEach { value ->
            assertEquals(OtpGenerationError.InvalidSecret, assertThrows(OtpGenerationError::class.java) {
                OtpSecretCodec.decode(value, OtpSecretEncoding.BASE64)
            })
        }
    }
}
