package com.aozijx.passly.domain.entry.service

import com.aozijx.passly.domain.entry.model.VaultEntry
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SeedPhraseEntryValidator @Inject constructor() : EntryValidator {
    override fun validateRequiredFields(entry: VaultEntry): String? {
        if (entry.summary.title.isBlank()) return "钱包名称不能为空"
        if (entry.secret.identity?.seedPhrase.isNullOrBlank()) return "助记词不能为空"
        return null
    }

    override fun validateFieldContent(entry: VaultEntry): String? {
        entry.secret.identity?.seedPhrase?.let { phrase ->
            val wordCount = phrase.split(Regex("\\s+")).size
            if (wordCount !in setOf(12, 24)) return "助记词应包含 12 或 24 个单词，实际 $wordCount 个"
        }
        return null
    }
}
