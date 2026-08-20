package com.aozijx.passly.domain.entry.otp

import com.aozijx.passly.domain.entry.model.otp.OtpGenerationError
import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.OtpType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class OtpGeneratorTest {

    companion object {
        private const val SECRET_20 = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"
        private const val SECRET_32 = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZA===="
        private const val SECRET_64 =
            "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ" +
                    "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNA="
    }

    @Test
    fun `hotp - RFC 4226 测试向量`() {
        val config = OtpConfig(
            type = OtpType.HOTP,
            secret = SECRET_20,
            algorithm = OtpHashAlgorithm.SHA1,
            digits = 6,
            counter = 0L,
            encoding = OtpSecretEncoding.BASE32
        )

        val expected = listOf(
            "755224" to 0L,
            "287082" to 1L,
            "359152" to 2L,
            "969429" to 3L,
            "338314" to 4L,
            "254676" to 5L,
            "287922" to 6L,
            "162583" to 7L,
            "399871" to 8L,
            "520489" to 9L
        )

        for ((expectedCode, counter) in expected) {
            val result = OtpGenerator.generate(config, overrideCounter = counter)
            assertTrue(result is OtpResult.Success)
            val success = result as OtpResult.Success
            assertEquals(expectedCode, success.code)
            assertEquals(counter + 1, success.nextCounter)
        }
    }

    @Test
    fun `hotp - RFC 4226 测试向量 SHA256`() {
        val config = OtpConfig(
            type = OtpType.HOTP,
            secret = SECRET_20,
            algorithm = OtpHashAlgorithm.SHA256,
            digits = 6,
            counter = 0L,
            encoding = OtpSecretEncoding.BASE32
        )

        val result = OtpGenerator.generate(config, overrideCounter = 0)
        assertTrue(result is OtpResult.Success)
    }

    @Test
    fun `totp - RFC 6238 测试向量 SHA1`() {
        val config = OtpConfig(
            type = OtpType.TOTP,
            secret = SECRET_20,
            algorithm = OtpHashAlgorithm.SHA1,
            digits = 8,
            periodSeconds = 30,
            encoding = OtpSecretEncoding.BASE32
        )

        val vectors = listOf(
            59L to "94287082",
            1111111109L to "07081804",
            1111111111L to "14050471",
            1234567890L to "89005924",
            2000000000L to "69279037"
        )

        for ((timestamp, expectedCode) in vectors) {
            val result = OtpGenerator.generate(config, timestamp = timestamp)
            assertTrue(result is OtpResult.Success)
            val success = result as OtpResult.Success
            assertEquals(expectedCode, success.code)
        }
    }

    @Test
    fun `steam - 固定样例`() {
        val secret = "JBSWY3DPEHPK3PXP"
        val config = OtpConfig.steamDefaults(secret)

        val result = OtpGenerator.generate(config, timestamp = 1000000L)
        assertTrue(result is OtpResult.Success)
        val success = result as OtpResult.Success
        assertEquals("38CTQ", success.code)
    }

    @Test
    fun `base32 - RFC 标准解码`() {
        val decoded = OtpGenerator.base32DecodeStrict(SECRET_20)
        val expected = "12345678901234567890".toByteArray(Charsets.US_ASCII)
        assertTrue(expected.contentEquals(decoded))
    }
}
