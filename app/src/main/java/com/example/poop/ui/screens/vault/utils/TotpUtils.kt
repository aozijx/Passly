package com.example.poop.ui.screens.vault.utils

import com.example.poop.util.Logcat
import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

/**
 * TOTP (Time-based One-time Password) 工具类
 */
object TotpUtils {

    fun generateTotp(secret: String, digits: Int = 6, period: Int = 30): String {
        if (secret.isBlank()) return "000000"
        try {
            val decodedKey = base32Decode(secret)
            val timeWindow = System.currentTimeMillis() / 1000 / period
            val data = ByteBuffer.allocate(8).putLong(timeWindow).array()

            val signKey = SecretKeySpec(decodedKey, "HmacSHA1")
            val mac = Mac.getInstance("HmacSHA1")
            mac.init(signKey)
            val hash = mac.doFinal(data)

            val offset = hash[hash.size - 1].toInt() and 0x0f
            val truncatedHash = ByteBuffer.wrap(hash, offset, 4).int and 0x7fffffff
            val otp = truncatedHash % (10.0.pow(digits.toDouble()).toInt())

            return otp.toString().padStart(digits, '0')
        } catch (e: Exception) {
            Logcat.e("TotpUtils", "Generate TOTP failed", e)
            return "------"
        }
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
                else -> continue // 忽略非法字符
            }

            buffer = (buffer shl 5) or value
            bitsLeft += 5

            if (bitsLeft >= 8) {
                bitsLeft -= 8
                output[index++] = (buffer shr bitsLeft).toByte()
                buffer = buffer and ((1 shl bitsLeft) - 1) // 关键修复：清理已使用的位
            }
        }
        return output.copyOf(index)
    }
}
