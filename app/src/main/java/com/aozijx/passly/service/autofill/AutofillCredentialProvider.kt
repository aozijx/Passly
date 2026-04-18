package com.aozijx.passly.service.autofill

import com.aozijx.passly.core.common.EntryType
import com.aozijx.passly.core.crypto.CryptoAccess
import com.aozijx.passly.core.logging.Logcat
import com.aozijx.passly.domain.model.core.VaultEntry
import com.aozijx.passly.domain.strategy.EntryTypeStrategyFactory
import com.aozijx.passly.domain.strategy.EntryTypeStrategyRegistry

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
            val username = CryptoAccess.decryptOrNull(item.username) ?: return null
            val password = CryptoAccess.decryptOrNull(item.password) ?: return null

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

    /**
     * 构建数据集展示的副标题（用户名 + 策略摘要）
     */
    fun buildSubtitle(entry: VaultEntry, decryptedUsername: String): String {
        EntryTypeStrategyRegistry.ensureRegistered()
        val strategy = runCatching {
            EntryTypeStrategyFactory.getStrategy(EntryType.fromValue(entry.entryType))
        }.getOrNull()

        val strategySummary = strategy
            ?.let { runCatching { it.extractSummary(entry) }.getOrDefault("") }
            .orEmpty()

        val infoParts = mutableListOf<String>()
        if (decryptedUsername.isNotBlank()) infoParts += decryptedUsername
        if (strategySummary.isNotBlank()) infoParts += strategySummary
        if (infoParts.isEmpty()) infoParts += EntryType.fromValue(entry.entryType).displayName
        val joined = infoParts.joinToString(" · ")
        return if (!entry.totpSecret.isNullOrBlank()) "OTP · $joined" else joined
    }
}