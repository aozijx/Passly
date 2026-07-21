package com.aozijx.passly.core.util

import android.util.Base64
import com.aozijx.passly.core.diagnostics.AppLog
import com.aozijx.passly.domain.model.entry.VaultEntry
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * 增强型 2FA 工具类
 * 支持标准 TOTP (RFC 6238) 和 Steam Guard 特有逻辑
 */
object TwoFAUtils {

    private const val STEAM_CHARS = "23456789BCDFGHJKMNPQRTVWXY"
    private val POWERS_OF_10 = longArrayOf(
        1L, 10L, 100L, 1000L, 10000L, 100000L,
        1000000L, 10000000L, 100000000L, 1000000000L, 10000000000L
    )

    /**
     * 从 VaultEntry 生成当前的 TOTP 验证码
     */
    fun generateCurrentTotpFromEntry(entry: VaultEntry): String? {
        val otpConfig = entry.credential.otp ?: return null
        val secret = otpConfig.secret
        if (secret.isBlank()) return null

        return try {
            generateTotp(
                secret = secret,
                digits = otpConfig.digits,
                period = otpConfig.period,
                algorithm = otpConfig.algorithm
            )
        } catch (e: Exception) {
            AppLog.e("TwoFAUtils", "Failed to generate TOTP from entry", e)
            null
        }
    }

    /**
     * 生成验证码
     * @param secret 密钥。标准 TOTP 为 Base32，Steam 内部常用 Base64 或 Base32
     * @param timestamp 可选的 Unix 时间戳（秒），如果不传则使用当前系统时间
     */
    fun generateTotp(
        secret: String,
        digits: Int = 6,
        period: Int = 30,
        algorithm: String = "SHA1",
        timestamp: Long? = null
    ): String {
        if (secret.isBlank()) return "000000"

        try {
            val algoUpper = algorithm.uppercase()
            val isSteam = algoUpper == "STEAM"

            val decodedKey = decodeSecret(secret, isSteam)
            if (decodedKey.isEmpty()) return "INVALID"

            val timeSeconds = timestamp ?: (System.currentTimeMillis() / 1000)
            val timeWindow = timeSeconds / period

            // 直接使用位运算构建 8 字节计数器，避免 ByteBuffer 内存分配
            val data = ByteArray(8)
            var v = timeWindow
            for (i in 7 downTo 0) {
                data[i] = (v and 0xFF).toByte()
                v = v ushr 8
            }

            val hmacAlgo = when (algoUpper) {
                "SHA256" -> "HmacSHA256"
                "SHA512" -> "HmacSHA512"
                else -> "HmacSHA1"
            }

            val signKey = SecretKeySpec(decodedKey, hmacAlgo)
            val mac = Mac.getInstance(hmacAlgo)
            mac.init(signKey)
            val hash = mac.doFinal(data)

            // RFC 4226: 提取 31 位二进制值 (Dynamic Truncation)
            val offset = hash[hash.size - 1].toInt() and 0x0f
            val binary = ((hash[offset].toInt() and 0x7f) shl 24) or
                    ((hash[offset + 1].toInt() and 0xff) shl 16) or
                    ((hash[offset + 2].toInt() and 0xff) shl 8) or
                    (hash[offset + 3].toInt() and 0xff)

            return if (isSteam) {
                formatSteamCode(binary)
            } else {
                // 使用预计算的 10 的幂次方，避免 Math.pow 的浮点运算开销
                val divisor =
                    if (digits < POWERS_OF_10.size) POWERS_OF_10[digits] else POWERS_OF_10[6]
                val otp = binary % divisor
                otp.toString().padStart(digits, '0')
            }
        } catch (e: Exception) {
            AppLog.e("TwoFAUtils", "Generate 2FA failed (Algo: $algorithm)", e)
            return "------"
        }
    }

    private fun decodeSecret(secret: String, isSteam: Boolean): ByteArray {
        return if (isSteam) {
            try {
                // Steam 启发式判断：长度 32 且不含 Base64 特有字符时优先尝试 Base32
                if (secret.length == 32 && !secret.contains("/") && !secret.contains("+")) {
                    base32Decode(secret)
                } else {
                    Base64.decode(secret, Base64.DEFAULT)
                }
            } catch (_: Exception) {
                base32Decode(secret)
            }
        } else {
            base32Decode(secret)
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

    /**
     * 高性能 Base32 解码器
     * 单次遍历，不产生冗余字符串
     */
    private fun base32Decode(base32: String): ByteArray {
        val clean = base32.trim().uppercase()
        if (clean.isEmpty()) return byteArrayOf()

        val output = ByteArray(clean.length * 5 / 8)
        var buffer = 0
        var bitsLeft = 0
        var index = 0

        for (char in clean) {
            val value = when (char) {
                in 'A'..'Z' -> char - 'A'
                in '2'..'7' -> char - '2' + 26
                else -> continue // 跳过空格、连字符或 '=' 填充符
            }

            buffer = (buffer shl 5) or value
            bitsLeft += 5
            if (bitsLeft >= 8) {
                bitsLeft -= 8
                output[index++] = (buffer shr bitsLeft).toByte()
                buffer = buffer and ((1 shl bitsLeft) - 1)
            }
        }
        return if (index == output.size) output else output.copyOf(index)
    }
}
