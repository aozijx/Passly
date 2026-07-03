package com.aozijx.passly.service.autofill.credential

import com.aozijx.passly.domain.model.EntryType
import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.domain.strategy.EntryTypeStrategyFactory

object AutofillCredentialProvider {

    data class BasicCredentials(
        val username: String,
        val password: String
    )

    fun getBasicCredentials(item: VaultEntry): BasicCredentials? {
        val username = item.username
        val password = item.password

        if (username.isBlank() && password.isBlank()) {
            return null
        }
        return BasicCredentials(username, password)
    }

    fun buildSubtitle(entry: VaultEntry, decryptedUsername: String): String {
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