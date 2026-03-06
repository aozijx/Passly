package com.example.poop.service.autofill

import com.example.poop.data.VaultEntry
import com.example.poop.ui.screens.vault.utils.CryptoManager
import com.example.poop.ui.screens.vault.utils.TwoFAUtils
import com.example.poop.util.Logcat

/**
 * 自动填充凭据提供者
 * 核心职责：封装加密数据的解密逻辑以及 TOTP 动态码的生成
 */
object AutofillCredentialProvider {
    private const val TAG = "AutofillCredentialProvider"

    data class DecryptedCredential(
        val username: String,
        val password: String,
        val totpCode: String?
    )

    /**
     * 一键获取条目的明文凭据
     * 内部处理多字段解密逻辑，返回统一的数据模型
     */
    fun getCredentials(item: VaultEntry): DecryptedCredential? {
        return try {
            // 1. 分别获取各字段的 IV 并初始化解密器
            val ivUser = CryptoManager.getIvFromCipherText(item.username)
            val ivPass = CryptoManager.getIvFromCipherText(item.password)
            val ivTotp = item.totpSecret?.let { CryptoManager.getIvFromCipherText(it) }

            val userCipher = ivUser?.let { CryptoManager.getDecryptCipher(it) }
            val passCipher = ivPass?.let { CryptoManager.getDecryptCipher(it) }
            val totpCipher = ivTotp?.let { CryptoManager.getDecryptCipher(it) }

            // 2. 执行核心解密
            val username = if (userCipher != null) CryptoManager.decrypt(item.username, userCipher) else ""
            val password = if (passCipher != null) CryptoManager.decrypt(item.password, passCipher) else ""

            // 3. 处理 TOTP 生成
            val totpCode = item.totpSecret?.let { secret ->
                totpCipher?.let { cipher ->
                    val rawSecret = CryptoManager.decrypt(secret, cipher)
                    rawSecret?.let { 
                        TwoFAUtils.generateTotp(
                            secret = it,
                            digits = item.totpDigits,
                            period = item.totpPeriod,
                            algorithm = item.totpAlgorithm
                        ) 
                    }
                }
            }

            DecryptedCredential(username ?: "", password ?: "", totpCode)
        } catch (e: Exception) {
            Logcat.e(TAG, "Unexpected error during credential decryption", e)
            null
        }
    }
}
