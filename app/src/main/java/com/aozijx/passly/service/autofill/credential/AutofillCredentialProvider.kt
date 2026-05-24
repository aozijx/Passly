package com.aozijx.passly.service.autofill.credential

import com.aozijx.passly.domain.model.EntryType
import com.aozijx.passly.core.crypto.encryption.CryptoAccess
import com.aozijx.passly.core.logging.Logcat
import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.domain.strategy.EntryTypeStrategyFactory
import com.aozijx.passly.domain.strategy.EntryTypeStrategyRegistry

object AutofillCredentialProvider {
    private const val TAG = "AutofillCredentialProvider"

    data class BasicCredentials(
        val username: String,
        val password: String
    )

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