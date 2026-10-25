package com.aozijx.passly.domain.entry.otp

import com.aozijx.passly.domain.entry.model.otp.OtpGenerationError
import com.aozijx.passly.domain.entry.model.otp.OtpSecretEncoding
import java.util.Base64

object OtpSecretCodec {
    private const val BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    /** Validates a secret and returns equivalent unpadded Base32, erasing decoded bytes on every exit. */
    fun canonicalBase32(value: String, encoding: OtpSecretEncoding): String {
        val source = if (encoding == OtpSecretEncoding.BASE32) value.replace(" ", "") else value
        val decoded = decode(source, encoding)
        return try {
            encodeBase32(decoded)
        } finally {
            decoded.fill(0)
        }
    }

    internal fun decode(secret: String, encoding: OtpSecretEncoding): ByteArray {
        val decoded = when (encoding) {
            OtpSecretEncoding.BASE32 -> decodeBase32(secret)
            OtpSecretEncoding.BASE64 -> try {
                Base64.getDecoder().decode(secret.trim())
            } catch (_: IllegalArgumentException) {
                throw OtpGenerationError.InvalidSecret
            }
        }
        if (decoded.isEmpty()) throw OtpGenerationError.InvalidSecret
        return decoded
    }

    internal fun decodeBase32(input: String): ByteArray {
        val clean = input.trim().uppercase()
        if (clean.isEmpty()) throw OtpGenerationError.InvalidSecret
        val dataLength = clean.indexOf('=').takeIf { it >= 0 } ?: clean.length
        val expectedPadding = when (dataLength % 8) {
            0 -> 0
            2 -> 6
            4 -> 4
            5 -> 3
            7 -> 1
            else -> throw OtpGenerationError.InvalidSecret
        }
        val paddingLength = clean.length - dataLength
        if (dataLength == 0 || paddingLength > 0 && (
                paddingLength != expectedPadding || clean.length % 8 != 0
            )
        ) throw OtpGenerationError.InvalidSecret
        for (index in dataLength until clean.length) {
            if (clean[index] != '=') throw OtpGenerationError.InvalidSecret
        }

        val output = ByteArray(dataLength * 5 / 8)
        try {
            var buffer = 0
            var bitsLeft = 0
            var index = 0
            for (charIndex in 0 until dataLength) {
                val value = when (val char = clean[charIndex]) {
                    in 'A'..'Z' -> char - 'A'
                    in '2'..'7' -> char - '2' + 26
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
            if (buffer != 0) throw OtpGenerationError.InvalidSecret
            return output
        } catch (failure: Throwable) {
            output.fill(0)
            throw failure
        }
    }

    internal fun encodeBase32(bytes: ByteArray): String = buildString {
        var buffer = 0
        var bitsLeft = 0
        bytes.forEach { byte ->
            buffer = (buffer shl 8) or (byte.toInt() and 0xff)
            bitsLeft += 8
            while (bitsLeft >= 5) {
                bitsLeft -= 5
                append(BASE32_ALPHABET[(buffer ushr bitsLeft) and 31])
            }
            buffer = buffer and ((1 shl bitsLeft) - 1)
        }
        if (bitsLeft > 0) append(BASE32_ALPHABET[(buffer shl (5 - bitsLeft)) and 31])
    }
}
