package com.aozijx.passly.core.otp

import com.aozijx.passly.domain.model.otp.OtpConfig
import com.aozijx.passly.domain.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.model.otp.OtpType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RFC 4226 / RFC 6238 兼容性测试 + Steam 固定样例。
 *
 * ## RFC 4226 (HOTP)
 * Secret = "12345678901234567890" (20 字节 ASCII)
 * Base32 = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"
 *
 * ## RFC 6238 (TOTP)
 * SHA1:    Secret = "12345678901234567890" (20 字节)
 * SHA256:  Secret = "12345678901234567890123456789012" (32 字节)
 * SHA512:  Secret = "1234567890123456789012345678901234567890123456789012345678901234" (64 字节)
 *
 * ## Steam 样例
 * 使用 Steam 字符表，5 位输出
 */
class OtpGeneratorTest {

    companion object {
        /** RFC 4226/6238 SHA1: 20 字节密钥 */
        private const val SECRET_20 = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"

        /** RFC 6238 SHA256: 32 字节密钥 */
        private const val SECRET_32 = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZA===="

        /** RFC 6238 SHA512: 64 字节密钥 */
        private const val SECRET_64 =
            "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ" +
                    "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNA="
    }

    // ============================================================
    // RFC 4226 (HOTP) 测试向量
    // ============================================================

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

        // Expected values from RFC 4226 Table 1
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
            assertTrue("HOTP counter=$counter 应成功生成", result is OtpResult.Success)
            val success = result as OtpResult.Success
            assertEquals("HOTP counter=$counter code 不匹配", expectedCode, success.code)
            assertEquals(
                "HOTP counter=$counter nextCounter 应递增",
                counter + 1,
                success.nextCounter
            )
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
        assertTrue("HOTP SHA256 应成功生成", result is OtpResult.Success)
    }

    @Test
    fun `hotp - 空 counter 返回 InvalidCounter`() {
        val config = OtpConfig(
            type = OtpType.HOTP,
            secret = SECRET_20,
            algorithm = OtpHashAlgorithm.SHA1,
            digits = 6,
            counter = null,
            encoding = OtpSecretEncoding.BASE32
        )
        val result = OtpGenerator.generate(config, overrideCounter = null)
        assertTrue("HOTP counter=null 应返回失败", result is OtpResult.Failure)
        val failure = result as OtpResult.Failure
        assertTrue(
            "HOTP counter=null 应返回 InvalidCounter",
            failure.error is OtpError.InvalidCounter
        )
    }

    @Test
    fun `hotp - 负 counter 返回 InvalidCounter`() {
        val config = OtpConfig(
            type = OtpType.HOTP,
            secret = SECRET_20,
            algorithm = OtpHashAlgorithm.SHA1,
            digits = 6,
            counter = -1L,
            encoding = OtpSecretEncoding.BASE32
        )
        val result = OtpGenerator.generate(config, overrideCounter = -1)
        assertTrue("HOTP counter=-1 应返回失败", result is OtpResult.Failure)
        val failure = result as OtpResult.Failure
        assertTrue(
            "HOTP counter=-1 应返回 InvalidCounter",
            failure.error is OtpError.InvalidCounter
        )
    }

    // ============================================================
    // RFC 6238 (TOTP) 测试向量
    // ============================================================

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
            assertTrue("TOTP SHA1 time=$timestamp 应成功生成", result is OtpResult.Success)
            val success = result as OtpResult.Success
            assertEquals("TOTP SHA1 time=$timestamp code 不匹配", expectedCode, success.code)
        }
    }

    @Test
    fun `totp - RFC 6238 测试向量 SHA256`() {
        val config = OtpConfig(
            type = OtpType.TOTP,
            secret = SECRET_32,
            algorithm = OtpHashAlgorithm.SHA256,
            digits = 8,
            periodSeconds = 30,
            encoding = OtpSecretEncoding.BASE32
        )

        val vectors = listOf(
            59L to "46119246",
            1111111109L to "68084774"
        )

        for ((timestamp, expectedCode) in vectors) {
            val result = OtpGenerator.generate(config, timestamp = timestamp)
            assertTrue("TOTP SHA256 time=$timestamp 应成功生成", result is OtpResult.Success)
            val success = result as OtpResult.Success
            assertEquals("TOTP SHA256 time=$timestamp code 不匹配", expectedCode, success.code)
        }
    }

    @Test
    fun `totp - RFC 6238 测试向量 SHA512`() {
        val config = OtpConfig(
            type = OtpType.TOTP,
            secret = SECRET_64,
            algorithm = OtpHashAlgorithm.SHA512,
            digits = 8,
            periodSeconds = 30,
            encoding = OtpSecretEncoding.BASE32
        )

        val vectors = listOf(
            59L to "90693936",
            1111111109L to "25091201"
        )

        for ((timestamp, expectedCode) in vectors) {
            val result = OtpGenerator.generate(config, timestamp = timestamp)
            assertTrue("TOTP SHA512 time=$timestamp 应成功生成", result is OtpResult.Success)
            val success = result as OtpResult.Success
            assertEquals("TOTP SHA512 time=$timestamp code 不匹配", expectedCode, success.code)
        }
    }

    // ============================================================
    // Steam 测试样例
    // ============================================================

    @Test
    fun `steam - 固定样例`() {
        val secret = "JBSWY3DPEHPK3PXP"
        val config = OtpConfig.steamDefaults(secret)

        val result = OtpGenerator.generate(config, timestamp = 1000000L)
        assertTrue("Steam 应成功生成", result is OtpResult.Success)
        val success = result as OtpResult.Success
        assertEquals("Steam 固定向量必须走字符表分支", "38CTQ", success.code)
        assertEquals("Steam 代码应为 5 位", 5, success.code.length)
        assertTrue("Steam 固定向量应包含字母，不能退化成数字 TOTP", success.code.any(Char::isLetter))
        for (ch in success.code) {
            assertTrue(
                "Steam 字符 '$ch' 不在有效字符表中",
                "23456789BCDFGHJKMNPQRTVWXY".contains(ch)
            )
        }
    }

    @Test
    fun `steam - 默认配置验证`() {
        val config = OtpConfig.steamDefaults("JBSWY3DPEHPK3PXP")
        assertEquals(OtpType.STEAM, config.type)
        assertEquals(OtpHashAlgorithm.SHA1, config.algorithm)
        assertEquals(5, config.digits)
        assertEquals(30, config.periodSeconds)
    }

    @Test
    fun `steam - Base64 shared secret 与同一密钥的 Base32 结果一致`() {
        val config = OtpConfig.steamDefaults("SGVsbG8h3q2+7w==").copy(
            encoding = OtpSecretEncoding.BASE64
        )

        val result = OtpGenerator.generate(config, timestamp = 1000000L)

        assertTrue("Steam Base64 shared secret 应成功生成", result is OtpResult.Success)
        assertEquals("38CTQ", (result as OtpResult.Success).code)
    }

    @Test
    fun `steam - 固定使用 SHA1 和 30 秒周期`() {
        val config = OtpConfig.steamDefaults("JBSWY3DPEHPK3PXP").copy(
            algorithm = OtpHashAlgorithm.SHA512,
            periodSeconds = 60
        )

        val result = OtpGenerator.generate(config, timestamp = 1000000L)

        assertTrue(result is OtpResult.Success)
        assertEquals("38CTQ", (result as OtpResult.Success).code)
    }

    @Test
    fun `steam - 不同时间戳生成不同代码`() {
        val config = OtpConfig.steamDefaults("JBSWY3DPEHPK3PXP")
        val code1 = (OtpGenerator.generate(config, timestamp = 1000000L) as OtpResult.Success).code
        val code2 = (OtpGenerator.generate(config, timestamp = 2000000L) as OtpResult.Success).code
        assertTrue("不同时间戳应生成不同 Steam 代码", code1 != code2)
    }

    // ============================================================
    // Base32 严格解码测试
    // ============================================================

    @Test
    fun `base32 - RFC 标准解码`() {
        // RFC 4648: 解码后应得到 "12345678901234567890" (20 字节)
        val decoded = OtpGenerator.base32DecodeStrict(SECRET_20)
        val expected = "12345678901234567890".toByteArray(Charsets.US_ASCII)
        assertEquals("Base32 解码结果长度应匹配", expected.size, decoded.size)
        assertTrue("Base32 解码结果应正确", expected.contentEquals(decoded))
    }

    @Test
    fun `base32 - 小写字母解码正常`() {
        val decoded =
            OtpGenerator.base32DecodeStrict("gezdgnbvg y3tqojqgezdgnbvg y3tqojq".replace(" ", ""))
        assertEquals("Base32 小写解码长度应匹配", 20, decoded.size)
    }

    @Test
    fun `base32 - 非法字符抛出 InvalidSecret`() {
        val invalidInputs = listOf(
            "JBSWY3DPEHPK3P!P",
            "JBSWY3DP#HPK3PXP",
            "JBSWY3DPEHPK3P P",
            "JBSWY3DPEHPK3P.P",
            "JBSWY3DP-EHPK3PXP",
        )
        for (input in invalidInputs) {
            try {
                OtpGenerator.base32DecodeStrict(input)
                assertTrue("输入 '$input' 应抛出 InvalidSecret", false)
            } catch (e: OtpError.InvalidSecret) {
                // 预期的异常
            }
        }
    }

    @Test
    fun `base32 - 空输入抛出 InvalidSecret`() {
        try {
            OtpGenerator.base32DecodeStrict("")
            assertTrue("空输入应抛出 InvalidSecret", false)
        } catch (e: OtpError.InvalidSecret) {
            // 预期的异常
        }
    }

    @Test
    fun `base32 - 填充符 = 可正常解码`() {
        // SECRET_32 末尾有 ==== 填充符
        val decoded = OtpGenerator.base32DecodeStrict(SECRET_32)
        assertEquals("带填充的 Base32 解码长度应为 32", 32, decoded.size)
    }

    // ============================================================
    // 错误处理测试
    // ============================================================

    @Test
    fun `error - 空 secret 返回 InvalidSecret`() {
        val config = OtpConfig(
            type = OtpType.TOTP,
            secret = "",
            algorithm = OtpHashAlgorithm.SHA1,
            digits = 6,
            periodSeconds = 30,
            encoding = OtpSecretEncoding.BASE32
        )
        val result = OtpGenerator.generate(config)
        assertTrue("空 secret 应返回 Failure", result is OtpResult.Failure)
        val failure = result as OtpResult.Failure
        assertTrue(
            "空 secret 应返回 InvalidSecret",
            failure.error is OtpError.InvalidSecret
        )
    }

    @Test
    fun `error - TOTP 使用非法 secret 字符`() {
        val config = OtpConfig(
            type = OtpType.TOTP,
            secret = "JBSWY3DP#HPK3PXP",
            algorithm = OtpHashAlgorithm.SHA1,
            digits = 6,
            periodSeconds = 30,
            encoding = OtpSecretEncoding.BASE32
        )
        val result = OtpGenerator.generate(config)
        assertTrue("非法 Base32 应返回 Failure", result is OtpResult.Failure)
        val failure = result as OtpResult.Failure
        assertTrue(
            "非法 Base32 应返回 InvalidSecret",
            failure.error is OtpError.InvalidSecret
        )
    }

    @Test
    fun `error - Base64 非法字符返回 InvalidSecret`() {
        val config = OtpConfig(
            type = OtpType.TOTP,
            secret = "!!!invalid base64!!!",
            algorithm = OtpHashAlgorithm.SHA1,
            digits = 6,
            periodSeconds = 30,
            encoding = OtpSecretEncoding.BASE64
        )
        val result = OtpGenerator.generate(config)
        assertTrue("非法 Base64 应返回 Failure", result is OtpResult.Failure)
    }

    // ============================================================
    // HOTP 递增 counter 验证
    // ============================================================

    @Test
    fun `hotp - nextCounter 连续递增`() {
        val config = OtpConfig(
            type = OtpType.HOTP,
            secret = SECRET_20,
            algorithm = OtpHashAlgorithm.SHA1,
            digits = 6,
            counter = 0L,
            encoding = OtpSecretEncoding.BASE32
        )

        var counter = 0L
        for (i in 0 until 3) {
            val result = OtpGenerator.generate(config, overrideCounter = counter)
            assertTrue("HOTP 生成应成功", result is OtpResult.Success)
            val success = result as OtpResult.Success
            assertEquals("HOTP nextCounter 应等于 counter+1", counter + 1, success.nextCounter!!)
            counter = success.nextCounter
        }
        assertEquals("HOTP 三次递增后 counter 应为 3", 3L, counter)
    }
}
