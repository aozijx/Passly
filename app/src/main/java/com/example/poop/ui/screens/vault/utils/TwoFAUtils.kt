package com.example.poop.ui.screens.vault.utils

import android.util.Base64
import com.example.poop.data.VaultEntry
import com.example.poop.util.Logcat
import java.nio.ByteBuffer
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

/**
 * 增强型 2FA 工具类
 * 支持标准 TOTP (RFC 6238) 和 Steam Guard 特有逻辑
 */
object TwoFAUtils {

    /**
     * 从 VaultEntry 生成当前的 TOTP 验证码
     * 注意：AutofillService 保存时通常使用 isSilent = true
     */
    fun generateCurrentTotpFromEntry(entry: VaultEntry, crypto: CryptoManager, isSilent: Boolean = true): String? {
        val secretCiphertext = entry.totpSecret ?: return null
        if (secretCiphertext.isBlank()) return null
        
        return try {
            val iv = crypto.getIvFromCipherText(secretCiphertext) ?: return null
            val cipher = crypto.getDecryptCipher(iv, isSilent) ?: return null
            val secret = crypto.decrypt(secretCiphertext, cipher) ?: return null
            
            generateTotp(
                secret = secret,
                digits = entry.totpDigits,
                period = entry.totpPeriod,
                algorithm = entry.totpAlgorithm
            )
        } catch (e: Exception) {
            Logcat.e("TwoFAUtils", "Failed to generate TOTP from entry (isSilent=$isSilent)", e)
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
            
            // 1. 解码密钥
            val decodedKey = if (isSteam) {
                try {
                    // Steam 的 shared_secret 可能是 Base64 (从 JSON 提取) 或 Base32 (手动输入)
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

            if (decodedKey.isEmpty()) return "INVALID"

            // 2. 计算时间步长 (Steam 固定 30s)
            val timeSeconds = timestamp ?: (System.currentTimeMillis() / 1000)
            val timeWindow = timeSeconds / period
            val data = ByteBuffer.allocate(8).putLong(timeWindow).array()

            // 3. HMAC 签名 (Steam 强制使用 SHA1)
            val hmacAlgo = when (algoUpper) {
                "SHA256" -> "HmacSHA256"
                "SHA512" -> "HmacSHA512"
                else -> "HmacSHA1" 
            }

            val signKey = SecretKeySpec(decodedKey, hmacAlgo)
            val mac = Mac.getInstance(hmacAlgo)
            mac.init(signKey)
            val hash = mac.doFinal(data)

            // 4. 生成最终代码
            return if (isSteam) {
                generateSteamCode(hash)
            } else {
                // 标准数字逻辑
                val offset = hash[hash.size - 1].toInt() and 0x0f
                val truncatedHash = ((hash[offset].toInt() and 0x7f) shl 24) or
                        ((hash[offset + 1].toInt() and 0xff) shl 16) or
                        ((hash[offset + 2].toInt() and 0xff) shl 8) or
                        (hash[offset + 3].toInt() and 0xff)
                
                val otp = truncatedHash % (10.0.pow(digits.toDouble()).toLong())
                otp.toString().padStart(digits, '0')
            }
        } catch (e: Exception) {
            Logcat.e("TwoFAUtils", "Generate 2FA failed (Algo: $algorithm)", e)
            return "------"
        }
    }

    /**
     * Steam 特有的 5 位字母数字验证码生成算法
     * 字符集: 23456789BCDFGHJKMNPQRTVWXY (26个字符)
     */
    private fun generateSteamCode(hash: ByteArray): String {
        val steamChars = "23456789BCDFGHJKMNPQRTVWXY"
        val alphabetSize = steamChars.length // 应为 26

        val offset = hash[hash.size - 1].toInt() and 0x0f

        // RFC 4226 动态截断
        var fullCode = ((hash[offset].toInt() and 0x7f) shl 24) or
                ((hash[offset + 1].toInt() and 0xff) shl 16) or
                ((hash[offset + 2].toInt() and 0xff) shl 8) or
                (hash[offset + 3].toInt() and 0xff)

        val code = StringBuilder()
        repeat(5) {
            code.append(steamChars[fullCode % alphabetSize])
            fullCode /= alphabetSize
        }
        return code.toString()
    }

    private fun base32Decode(base32: String): ByteArray {
        val clean = base32.uppercase().replace(" ", "").replace("-", "").replace("=", "")
        if (clean.isEmpty()) return byteArrayOf()

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
