package com.aozijx.passly.core.otp

import com.aozijx.passly.domain.entry.model.otp.OtpConfig
import com.aozijx.passly.domain.entry.model.otp.OtpGenerationError
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import com.aozijx.passly.domain.entry.model.otp.OtpType
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * RFC 4226 / RFC 6238 兼容的 OTP 生成器。
 *
 * 原则：
 * - 生成失败返回 [OtpGenerationError] 类型化错误，不返回占位字符串
 * - Steam 是 [OtpType.STEAM] 类型，使用 Steam 字符表（5 位）
 * - Base32 解码遇到非法字符必须失败，不能静默跳过
 * - HOTP 使用 counter，生成后由调用方持久化递增后的 counter
 */
object OtpGenerator {

    private const val STEAM_CHARS = "23456789BCDFGHJKMNPQRTVWXY"
    private const val HMAC_SHA1 = "HmacSHA1"
    private const val HMAC_SHA256 = "HmacSHA256"
    private const val HMAC_SHA512 = "HmacSHA512"

    /**
     * 生成 OTP 验证码。
     *
     * @param config OTP 配置（type/secret/algorithm/digits/periodSeconds/counter/encoding）
     * @param overrideCounter 可选覆盖 counter（用于 HOTP 手动指定），为 null 时使用 [config.counter]
     * @param timestamp 可选 Unix 时间戳（秒），仅 TOTP/STEAM 有效，默认当前系统时间
     */
    fun generate(
        config: OtpConfig,
        overrideCounter: Long? = null,
        timestamp: Long = System.currentTimeMillis() / 1000
    ): OtpResult {
        return try {
            when (config.type) {
                OtpType.TOTP, OtpType.STEAM -> generateTimed(config, timestamp)
                OtpType.HOTP -> generateCounterBased(config, overrideCounter ?: config.counter)
            }
        } catch (e: OtpGenerationError) {
            OtpResult.Failure(e)
        } catch (e: Exception) {
            OtpResult.Failure(OtpGenerationError.CryptoError(e))
        }
    }

    /**
     * 严格 Base32 解码。
     * 遇到任何不在 RFC 4648 Base32 字符集中的字符都会失败。
     * 不跳过空格、连字符、填充符。
     */
    fun base32DecodeStrict(input: String): ByteArray {
        val clean = input.trim().uppercase()
        if (clean.isEmpty()) throw OtpGenerationError.InvalidSecret

        val output = ByteArray((clean.length * 5 + 7) / 8)
        var buffer = 0
        var bitsLeft = 0
        var index = 0

        for (char in clean) {
            val value: Int = when (char) {
                in 'A'..'Z' -> char - 'A'
                in '2'..'7' -> char - '2' + 26
                '=' -> {
                    // Padding: stop decoding when we hit '='
                    break
                }

                else -> throw OtpGenerationError.InvalidSecret
            }

            buffer = (buffer shl 5) or value
            bitsLeft += 5
            if (bitsLeft >= 8) {
                bitsLeft -= 8
                output[index++] = (buffer shr bitsLeft).toByte()
                buffer = buffer and ((1 shl bitsLeft) - 1)
            }
        }

        return output.copyOf(index)
    }

    private fun generateTimed(config: OtpConfig, timestamp: Long): OtpResult {
        val period = if (config.type == OtpType.STEAM) {
            STEAM_PERIOD_SECONDS
        } else {
            config.periodSeconds?.coerceAtLeast(1) ?: 30
        }
        val timeWindow = timestamp / period
        val digits = if (config.type == OtpType.STEAM) 5 else config.digits
        return computeOtp(config, timeWindow, digits)
    }

    private fun generateCounterBased(config: OtpConfig, counter: Long?): OtpResult {
        val c = counter ?: return OtpResult.Failure(OtpGenerationError.InvalidCounter)
        if (c < 0) return OtpResult.Failure(OtpGenerationError.InvalidCounter)
        val result = computeOtp(config, c, config.digits)
        // HOTP 生成成功后返回递增后的 counter
        return if (result is OtpResult.Success) {
            OtpResult.Success(code = result.code, nextCounter = c + 1)
        } else {
            result
        }
    }

    private fun computeOtp(config: OtpConfig, movingFactor: Long, digits: Int): OtpResult {
        val decoded = decodeSecret(config)
        if (decoded.isEmpty()) return OtpResult.Failure(OtpGenerationError.InvalidSecret)

        val algorithm = if (config.type == OtpType.STEAM) {
            OtpHashAlgorithm.SHA1
        } else {
            config.algorithm
        }
        val hmacAlgo = when (algorithm) {
            OtpHashAlgorithm.SHA1 -> HMAC_SHA1
            OtpHashAlgorithm.SHA256 -> HMAC_SHA256
            OtpHashAlgorithm.SHA512 -> HMAC_SHA512
        }

        val key = SecretKeySpec(decoded, hmacAlgo)
        val mac = Mac.getInstance(hmacAlgo)
        mac.init(key)

        val data = ByteArray(8)
        var v = movingFactor
        for (i in 7 downTo 0) {
            data[i] = (v and 0xFF).toByte()
            v = v ushr 8
        }

        val hash = mac.doFinal(data)

        // RFC 4226: Dynamic Truncation
        val offset = hash[hash.size - 1].toInt() and 0x0f
        val binary = ((hash[offset].toInt() and 0x7f) shl 24) or
                ((hash[offset + 1].toInt() and 0xff) shl 16) or
                ((hash[offset + 2].toInt() and 0xff) shl 8) or
                (hash[offset + 3].toInt() and 0xff)

        val code = if (config.type == OtpType.STEAM) {
            formatSteamCode(binary)
        } else {
            val divisor = POWERS_OF_10[digits.coerceIn(1, POWERS_OF_10.size - 1)]
            val otp = binary % divisor
            otp.toString().padStart(digits, '0')
        }

        return OtpResult.Success(code)
    }

    private fun formatSteamCode(binary: Int): String {
        val alphabetSize = STEAM_CHARS.length
        var num = binary
        val code = StringBuilder()
        repeat(5) {
            code.append(STEAM_CHARS[num % alphabetSize])
            num /= alphabetSize
        }
        return code.toString()
    }

    /**
     * 严格按 [OtpConfig.encoding] 解码 Secret。
     *
     * 不能在生成阶段猜测编码：部分合法 Base32 字符串同时也是合法 Base64，
     * Android Base64 解码器会直接接受它们并产生另一组密钥，最终得到错误验证码。
     * 编码识别只能发生在 URI/文件导入边界，进入领域模型后必须是确定值。
     */
    private fun decodeSecret(config: OtpConfig): ByteArray {
        return when (config.encoding) {
            OtpSecretEncoding.BASE32 -> base32DecodeStrict(config.secret)
            OtpSecretEncoding.BASE64 -> {
                try {
                    Base64.getDecoder().decode(config.secret.trim())
                } catch (e: Exception) {
                    throw OtpGenerationError.InvalidSecret
                }
            }
        }
    }

    private val POWERS_OF_10 = longArrayOf(
        1L, 10L, 100L, 1000L, 10000L, 100000L,
        1000000L, 10000000L, 100000000L, 1000000000L
    )

    private const val STEAM_PERIOD_SECONDS = 30
}

/**
 * OTP 生成结果。
 */
sealed class OtpResult {
    /**
     * @param code 生成的 OTP 验证码
     * @param nextCounter HOTP 递增后的新 counter 值（仅 HOTP 有效，TOTP/STEAM 为 null）
     */
    data class Success(val code: String, val nextCounter: Long? = null) : OtpResult()
    data class Failure(val error: OtpGenerationError) : OtpResult()
}
