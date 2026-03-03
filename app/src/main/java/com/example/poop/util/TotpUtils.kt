package com.example.poop.util

import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

/**
 * TOTP (Time-based One-time Password) 工具类
 * 符合 RFC 6238 标准，支持 Google/Microsoft 等主流 2FA
 */
object TotpUtils {

    /**
     * 根据 Base32 密钥生成 TOTP 验证码
     * @param secret Base32 编码的密钥字符串（不区分大小写）
     * @param digits 验证码位数（默认 6）
     * @param period 时间步长秒数（默认 30）
     * @return 6位（或指定位数）数字字符串
     */
    fun generateTotp(secret: String, digits: Int = 6, period: Int = 30): String {
        try {
            // 1. Base32 解码（手写实现，无需外部库）
            val decodedKey = base32Decode(secret.uppercase())

            // 2. 计算时间窗口
            val timeWindow = System.currentTimeMillis() / 1000 / period

            // 3. 准备 HMAC 数据（8 字节大端时间戳）
            val data = ByteBuffer.allocate(8).putLong(timeWindow).array()

            // 4. HMAC-SHA1 计算
            val signKey = SecretKeySpec(decodedKey, "HmacSHA1")
            val mac = Mac.getInstance("HmacSHA1")
            mac.init(signKey)
            val hash = mac.doFinal(data)

            // 5. 动态截断（RFC 6238 标准算法）
            val offset = hash[hash.size - 1].toInt() and 0x0f
            val truncatedHash = ByteBuffer.wrap(hash, offset, 4).int and 0x7fffffff

            // 6. 取模得到最终数字
            val otp = truncatedHash % (10.0.pow(digits.toDouble()).toInt())

            // 7. 补零到指定位数
            return otp.toString().padStart(digits, '0')
        } catch (e: Exception) {
            Logcat.e("TotpUtils", "Failed to generate TOTP", e)
            return "000000"  // 容错返回，避免崩溃
        }
    }

    /**
     * 手写 Base32 解码（RFC 4648 标准）
     * 支持 A-Z 2-7 =（填充符）
     */
    private fun base32Decode(base32: String): ByteArray {
        val clean = base32.uppercase().replace("=", "")
        val output = ByteArray(clean.length * 5 / 8)
        var buffer = 0
        var bitsLeft = 0
        var index = 0

        for (char in clean) {
            val value = when (char) {
                in 'A'..'Z' -> char - 'A'
                in '2'..'7' -> char - '2' + 26
                else -> throw IllegalArgumentException("Invalid Base32 character: $char")
            }

            buffer = (buffer shl 5) or value
            bitsLeft += 5

            if (bitsLeft >= 8) {
                bitsLeft -= 8
                output[index++] = (buffer shr bitsLeft).toByte()
            }
        }

        return output.copyOf(index)
    }
}