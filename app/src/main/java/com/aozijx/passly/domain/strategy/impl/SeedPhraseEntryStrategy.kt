package com.aozijx.passly.domain.strategy.impl

import com.aozijx.passly.domain.model.entry.EntrySecret
import com.aozijx.passly.domain.model.entry.EntryType
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.strategy.EntryTypeStrategy

/**
 * 助记词类型的业务策略实现
 */
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SeedPhraseEntryStrategy @Inject constructor() : EntryTypeStrategy {
    override val entryType = EntryType.SEED_PHRASE

    override fun validateRequiredFields(entry: VaultEntry): String? {
        if (entry.title.isBlank()) return "钱包名称不能为空"
        if ((entry.secret as? EntrySecret.Identity)?.data?.seedPhrase.isNullOrBlank()) return "助记词不能为空"
        return null
    }

    override fun validateFieldContent(entry: VaultEntry): String? {
        (entry.secret as? EntrySecret.Identity)?.data?.seedPhrase?.let { phrase ->
            val wordCount = phrase.split(Regex("\\s+")).size
            if (wordCount !in setOf(12, 24)) {
                return "助记词应包含 12 或 24 个单词，实际 $wordCount 个"
            }
        }
        return null
    }

    override fun getSensitiveFields(): Set<String> {
        return setOf("seedPhrase", "password")
    }

    override fun extractSummary(entry: VaultEntry): String {
        return "12/24 词"
    }

    override fun suggestedCategory(): String = "加密"

    override fun supportsAutofill(): Boolean = false

    override fun initializeDefaults(entry: VaultEntry): VaultEntry {
        return entry
    }


}
