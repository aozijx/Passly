package com.example.poop.service.autofill

import com.example.poop.data.VaultEntry
import com.example.poop.util.Logcat
import com.example.poop.utils.CryptoManager

object AutofillCredentialProvider {
    private const val TAG = "AutofillCredentialProvider"

    data class BasicCredentials(
        val username: String,
        val password: String
    )

    /**
     * 解密凭据，支持自动回退模式 (Silent -> Secure)
     */
    fun getBasicCredentials(item: VaultEntry): BasicCredentials? {
        Logcat.d(TAG, "Attempting decryption for item: ${item.title} (ID: ${item.id})")

        // 1. 优先尝试静默模式 (AutofillService 自动保存的数据默认使用此模式)
        val silentResult = decryptWithMode(item, isSilent = true)
        if (silentResult != null) {
            Logcat.i(TAG, "Decryption success using SILENT mode")
            return silentResult
        }

        // 2. 如果静默解密失败（返回 null，通常是因为 Tag 校验失败），尝试安全模式
        Logcat.d(TAG, "Silent mode failed, retrying with SECURE mode")
        val secureResult = decryptWithMode(item, isSilent = false)
        if (secureResult != null) {
            Logcat.i(TAG, "Decryption success using SECURE mode")
            return secureResult
        }

        Logcat.e(TAG, "All decryption attempts failed for item: ${item.title}")
        return null
    }

    private fun decryptWithMode(item: VaultEntry, isSilent: Boolean): BasicCredentials? {
        return try {
            val ivUser = CryptoManager.getIvFromCipherText(item.username)
            val ivPass = CryptoManager.getIvFromCipherText(item.password)

            if (ivUser == null || ivPass == null) return null

            val userCipher = CryptoManager.getDecryptCipher(ivUser, isSilent)
            val passCipher = CryptoManager.getDecryptCipher(ivPass, isSilent)

            if (userCipher == null || passCipher == null) return null

            val username = CryptoManager.decrypt(item.username, userCipher)
            val password = CryptoManager.decrypt(item.password, passCipher)

            // 如果解密结果为 null（CryptoManager 内部捕获了 AEADBadTagException），返回 null 触发重试
            if (username == null || password == null) {
                return null
            }

            // 至少有一个不为空才算成功
            if (username.isBlank() && password.isBlank()) {
                return null
            }

            BasicCredentials(username, password)
        } catch (e: Exception) {
            Logcat.w(TAG, "[$isSilent] Decryption error: ${e.message}")
            null
        }
    }
}
