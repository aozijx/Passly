package com.aozijx.passly.domain.entry.otp.generation

import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpGenerationError
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.OtpType
import org.junit.Assert.assertEquals
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
        val config =
            OtpConfig(
                type = OtpType.HOTP,
                secret = SECRET_20,
                algorithm = OtpHashAlgorithm.SHA1,
                digits = 6,
                counter = 0L,
                encoding = OtpSecretEncoding.BASE32,
            )

        val expected =
            listOf(
                "755224" to 0L,
                "287082" to 1L,
                "359152" to 2L,
                "969429" to 3L,
                "338314" to 4L,
                "254676" to 5L,
                "287922" to 6L,
                "162583" to 7L,
                "399871" to 8L,
                "520489" to 9L,
            )

        for ((expectedCode, counter) in expected) {
            val result = generate(config, overrideCounter = counter)
            assertTrue(result is OtpResult.Success)
            val success = result as OtpResult.Success
            assertEquals(expectedCode, success.code)
            assertEquals(counter + 1, success.nextCounter)
        }
    }

    @Test
    fun `hotp exhausted stored or overridden counter cannot return wrapped next counter`() {
        val config = OtpConfig(type = OtpType.HOTP, secret = SECRET_20, counter = Long.MAX_VALUE)

        val exhausted = OtpResult.Failure(OtpGenerationError.CounterExhausted)
        assertEquals(exhausted, generate(config))
        assertEquals(exhausted, generate(config.copy(counter = 0), overrideCounter = Long.MAX_VALUE))
        assertEquals(OtpResult.Failure(OtpGenerationError.InvalidCounter), generate(config, overrideCounter = -1))
    }

    @Test
    fun `hotp last advanceable counter returns largest representable next counter`() {
        val config = OtpConfig(type = OtpType.HOTP, secret = SECRET_20, counter = Long.MAX_VALUE - 1)
        val result = generate(config)

        assertTrue(result is OtpResult.Success)
        assertEquals(Long.MAX_VALUE, (result as OtpResult.Success).nextCounter)
    }

    @Test
    fun `totp - RFC 6238 vectors cover each supported hash algorithm`() {
        val timestamps = listOf(59L, 1111111109L, 1111111111L, 1234567890L, 2000000000L)
        val cases =
            listOf(
                Triple(
                    OtpHashAlgorithm.SHA1,
                    SECRET_20,
                    listOf("94287082", "07081804", "14050471", "89005924", "69279037"),
                ),
                Triple(
                    OtpHashAlgorithm.SHA256,
                    SECRET_32,
                    listOf("46119246", "68084774", "67062674", "91819424", "90698825"),
                ),
                Triple(
                    OtpHashAlgorithm.SHA512,
                    SECRET_64,
                    listOf("90693936", "25091201", "99943326", "93441116", "38618901"),
                ),
            )

        for ((algorithm, secret, expectedCodes) in cases) {
            val config =
                OtpConfig(
                    type = OtpType.TOTP,
                    secret = secret,
                    algorithm = algorithm,
                    digits = 8,
                    periodSeconds = 30,
                    encoding = OtpSecretEncoding.BASE32,
                )

            for ((timestamp, expectedCode) in timestamps.zip(expectedCodes)) {
                val result = generate(config, timestamp = timestamp)
                assertTrue("$algorithm at $timestamp should succeed", result is OtpResult.Success)
                assertEquals(expectedCode, (result as OtpResult.Success).code)
            }
        }
    }

    @Test
    fun `byte material generates code without mutation and close clears seed`() {
        val original = SECRET_20.toByteArray(Charsets.US_ASCII)
        val seed = original.clone()
        val material =
            OtpGenerationMaterial(
                OtpConfig(type = OtpType.TOTP, secret = null, digits = 8, encoding = OtpSecretEncoding.BASE32).toGenerationConfig(),
                seed,
            )

        assertEquals(OtpResult.Success("94287082"), OtpGenerator.generate(material, timestamp = 59L))
        assertTrue(original.contentEquals(seed))

        material.close()

        assertTrue(seed.all { it == 0.toByte() })
    }

    @Test
    fun `base64 seed bytes generate the RFC 6238 code with the selected encoding`() {
        assertEquals(
            OtpResult.Success("94287082"),
            generateByteSeed("MTIzNDU2Nzg5MDEyMzQ1Njc4OTA=", OtpSecretEncoding.BASE64),
        )
    }

    @Test
    fun `malformed base64 seed bytes return invalid secret`() {
        assertEquals(
            OtpResult.Failure(OtpGenerationError.InvalidSecret),
            generateByteSeed("!", OtpSecretEncoding.BASE64),
        )
    }

    @Test
    fun `steam - 固定样例`() {
        val secret = "JBSWY3DPEHPK3PXP"
        val config = OtpConfig.steamDefaults(secret)

        val result = generate(config, timestamp = 1000000L)
        assertTrue(result is OtpResult.Success)
        val success = result as OtpResult.Success
        assertEquals("38CTQ", success.code)
    }

    @Test
    fun `base32 - RFC 标准解码`() {
        val decoded = OtpSecretCodec.decodeBase32(SECRET_20)
        val expected = "12345678901234567890".toByteArray(Charsets.US_ASCII)
        assertTrue(expected.contentEquals(decoded))
    }

    @Test
    fun `base32 rejects malformed padding instead of generating a code`() {
        for (secret in listOf("JBSW=Y3DPEHPK3PXP", "JBSWY3DP=", "MZ======")) {
            val config = OtpConfig(secret = secret, encoding = OtpSecretEncoding.BASE32)

            assertEquals(OtpResult.Failure(OtpGenerationError.InvalidSecret), generate(config))
        }
    }

    @Test
    fun `base32 accepts equivalent padded and unpadded secrets`() {
        val expected = "f".toByteArray(Charsets.US_ASCII)

        assertTrue(expected.contentEquals(OtpSecretCodec.decodeBase32("MY======")))
        assertTrue(expected.contentEquals(OtpSecretCodec.decodeBase32("MY")))
    }
}

private fun generateByteSeed(
    encodedSecret: String,
    encoding: OtpSecretEncoding,
): OtpResult {
    val material =
        OtpGenerationMaterial(
            OtpConfig(
                type = OtpType.TOTP,
                secret = null,
                digits = 8,
                periodSeconds = 30,
                encoding = encoding,
            ).toGenerationConfig(),
            encodedSecret.toByteArray(Charsets.US_ASCII),
        )

    return material.use { OtpGenerator.generate(it, timestamp = 59L) }
}

private fun generate(
    config: OtpConfig,
    overrideCounter: Long? = null,
    timestamp: Long = System.currentTimeMillis() / 1000,
): OtpResult {
    val material = OtpGenerationMaterial(config.toGenerationConfig(), config.secret?.toByteArray() ?: byteArrayOf())
    return material.use { OtpGenerator.generate(it, overrideCounter, timestamp) }
}
