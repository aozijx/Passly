package com.example.poop.ui.screens.vault.utils

import com.example.poop.util.Logcat
import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

/**
 * TOTP (Time-based One-time Password) 工具类
 * 支持标准 TOTP 以及 Steam 特有的验证码逻辑
 */
object TotpUtils {

    /**
     * 生成 TOTP 验证码
     * @param secret Base32 编码的密钥
     * @param digits 验证码位数 (通常为 6 或 8, Steam 为 5)
     * @param period 步长 (通常为 30 秒)
     * @param algorithm 散列算法 (SHA1, SHA256, SHA512, STEAM)
     */
    fun generateTotp(
        secret: String, 
        digits: Int = 6, 
        period: Int = 30,
        algorithm: String = "SHA1"
    ): String {
        if (secret.isBlank()) return "000000"
        try {
            val isSteam = algorithm.uppercase() == "STEAM"
            val decodedKey = base32Decode(secret)
            val timeWindow = System.currentTimeMillis() / 1000 / period
            val data = ByteBuffer.allocate(8).putLong(timeWindow).array()

            // Steam 强制使用 SHA1
            val hmacAlgo = when (algorithm.uppercase()) {
                "SHA256" -> "HmacSHA256"
                "SHA512" -> "HmacSHA512"
                else -> "HmacSHA1"
            }

            val signKey = SecretKeySpec(decodedKey, hmacAlgo)
            val mac = Mac.getInstance(hmacAlgo)
            mac.init(signKey)
            val hash = mac.doFinal(data)

            return if (isSteam) {
                generateSteamCode(hash)
            } else {
                val offset = hash[hash.size - 1].toInt() and 0x0f
                val truncatedHash = ByteBuffer.wrap(hash, offset, 4).int and 0x7fffffff
                val otp = truncatedHash % (10.0.pow(digits.toDouble()).toInt())
                otp.toString().padStart(digits, '0')
            }
        } catch (e: Exception) {
            Logcat.e("TotpUtils", "Generate TOTP failed (Algo: $algorithm)", e)
            return "------"
        }
    }

    /**
     * Steam 特有的验证码生成逻辑：
     * 1. 采用 SHA1 散列
     * 2. 结果不是纯数字，而是使用特定的 26 个字符集
     * 3. 长度固定为 5 位
     */
    private fun generateSteamCode(hash: ByteArray): String {
        val steamChars = "23456789BCDFGHJKMNPQRTVWXY"
        val offset = hash[hash.size - 1].toInt() and 0x0f
        var fullCode = (hash[offset].toInt() and 0x7f shl 24) or
                (hash[offset + 1].toInt() and 0xff shl 16) or
                (hash[offset + 2].toInt() and 0xff shl 8) or
                (hash[offset + 3].toInt() and 0xff)

        val code = StringBuilder()
        repeat(5) {
            code.append(steamChars[fullCode % steamChars.length])
            fullCode /= steamChars.length
        }
        return code.toString()
    }

    private fun base32Decode(base32: String): ByteArray {
        val clean = base32.uppercase().replace(" ", "").replace("-", "").replace("=", "")
        val output = ByteArray(clean.length * 5 / 8)
        var buffer = 0
        var bitsLeft = 0
        var index = 0

        for (char in clean) {
            val value = when (char) {
                in 'A'..'Z' -> char - 'A'
                in '2'..'7' -> char - '2' + 26
                else -> continue
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
}
