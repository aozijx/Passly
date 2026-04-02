package com.aozijx.passly.service.autofill

import com.aozijx.passly.core.crypto.CryptoManager
import com.aozijx.passly.core.util.Logcat
import com.aozijx.passly.data.model.VaultEntry

/**
 * 自动填充凭据提供器
 * 负责在自动填充流程中解密用户名和密码
 */
object AutofillCredentialProvider {
    private const val TAG = "AutofillCredentialProvider"

    data class BasicCredentials(
        val username: String,
        val password: String
    )

    /**
     * 解密凭据，内部已封装 AES-GCM 解密逻辑
     */
    fun getBasicCredentials(item: VaultEntry): BasicCredentials? {
        Logcat.d(TAG, "Attempting decryption for item: ${item.title} (ID: ${item.id})")

        return try {
            // 直接调用重构后的解密逻辑，内部已处理 IV 提取和 Tag 校验
            val username = CryptoManager.decrypt(item.username)
            val password = CryptoManager.decrypt(item.password)

            if (username.isBlank() && password.isBlank()) {
                null
            } else {
                BasicCredentials(username, password)
            }
        } catch (e: Exception) {
            Logcat.e(TAG, "Decryption failed for item: ${item.title}", e)
            null
        }
    }
}
