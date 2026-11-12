package com.aozijx.passly.domain.entry.otp.generation

import com.aozijx.passly.domain.entry.model.otp.OtpGenerationError
import com.aozijx.passly.domain.entry.model.otp.OtpHashAlgorithm
import com.aozijx.passly.domain.entry.model.otp.OtpMovingFactor
import com.aozijx.passly.domain.entry.model.otp.OtpOutputFormat
import com.aozijx.passly.domain.entry.model.otp.OtpTypeDefinitions
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * RFC 4226 / RFC 6238 compatible OTP generator.
 */
object OtpGenerator {
    private const val STEAM_CHARS = "23456789BCDFGHJKMNPQRTVWXY"
    private const val HMAC_SHA1 = "HmacSHA1"
    private const val HMAC_SHA256 = "HmacSHA256"
    private const val HMAC_SHA512 = "HmacSHA512"
    private const val DEFAULT_PERIOD_SECONDS = 30

    fun generate(
        material: OtpGenerationMaterial,
        overrideCounter: Long? = null,
        timestamp: Long = System.currentTimeMillis() / 1000,
    ): OtpResult =
        try {
            val config = material.config
            when (OtpTypeDefinitions[config.type].movingFactor) {
                OtpMovingFactor.TIME -> generateTimed(config, material.secretBytes, timestamp)
                OtpMovingFactor.COUNTER -> generateCounterBased(config, material.secretBytes, overrideCounter ?: config.counter)
            }
        } catch (e: OtpGenerationError) {
            OtpResult.Failure(e)
        } catch (e: Exception) {
            OtpResult.Failure(OtpGenerationError.CryptoError(e))
        }

    private fun generateTimed(
        config: OtpGenerationConfig,
        secretBytes: ByteArray,
        timestamp: Long,
    ): OtpResult {
        val definition = OtpTypeDefinitions[config.type]
        val period = definition.fixedPeriodSeconds ?: config.periodSeconds?.coerceAtLeast(1) ?: DEFAULT_PERIOD_SECONDS
        val timeWindow = timestamp / period
        return computeOtp(config, secretBytes, timeWindow, config.digits)
    }

    private fun generateCounterBased(
        config: OtpGenerationConfig,
        secretBytes: ByteArray,
        counter: Long?,
    ): OtpResult {
        val c = counter ?: return OtpResult.Failure(OtpGenerationError.InvalidCounter)
        if (c < 0) return OtpResult.Failure(OtpGenerationError.InvalidCounter)
        if (c == Long.MAX_VALUE) return OtpResult.Failure(OtpGenerationError.CounterExhausted)
        val result = computeOtp(config, secretBytes, c, config.digits)
        return if (result is OtpResult.Success) {
            OtpResult.Success(code = result.code, nextCounter = c + 1)
        } else {
            result
        }
    }

    private fun computeOtp(
        config: OtpGenerationConfig,
        secretBytes: ByteArray,
        movingFactor: Long,
        digits: Int,
    ): OtpResult {
        val decoded = OtpSecretCodec.decode(secretBytes, config.encoding)
        try {
            val definition = OtpTypeDefinitions[config.type]
            val algorithm = definition.fixedAlgorithm ?: config.algorithm
            val hmacAlgo =
                when (algorithm) {
                    OtpHashAlgorithm.SHA1 -> HMAC_SHA1
                    OtpHashAlgorithm.SHA256 -> HMAC_SHA256
                    OtpHashAlgorithm.SHA512 -> HMAC_SHA512
                }
            val mac = Mac.getInstance(hmacAlgo)
            mac.init(SecretKeySpec(decoded, hmacAlgo))
            val data = ByteArray(8)
            try {
                var value = movingFactor
                for (index in 7 downTo 0) {
                    data[index] = (value and 0xFF).toByte()
                    value = value ushr 8
                }
                val hash = mac.doFinal(data)
                try {
                    val offset = hash.last().toInt() and 0x0f
                    val binary =
                        ((hash[offset].toInt() and 0x7f) shl 24) or
                            ((hash[offset + 1].toInt() and 0xff) shl 16) or
                            ((hash[offset + 2].toInt() and 0xff) shl 8) or
                            (hash[offset + 3].toInt() and 0xff)
                    val code =
                        if (definition.outputFormat == OtpOutputFormat.STEAM_ALPHABET) {
                            formatSteamCode(binary)
                        } else {
                            val divisor = POWERS_OF_10[digits.coerceIn(1, POWERS_OF_10.size - 1)]
                            (binary % divisor).toString().padStart(digits, '0')
                        }
                    return OtpResult.Success(code)
                } finally {
                    hash.fill(0)
                }
            } finally {
                data.fill(0)
            }
        } finally {
            decoded.fill(0)
        }
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

    private val POWERS_OF_10 =
        longArrayOf(
            1L,
            10L,
            100L,
            1000L,
            10000L,
            100000L,
            1000000L,
            10000000L,
            100000000L,
            1000000000L,
        )

}

sealed class OtpResult {
    data class Success(
        val code: String,
        val nextCounter: Long? = null,
    ) : OtpResult()

    data class Failure(
        val error: OtpGenerationError,
    ) : OtpResult()
}
