package com.aozijx.passly.domain.strategy.impl

import com.aozijx.passly.core.common.EntryType
import com.aozijx.passly.domain.model.FieldDefinition
import com.aozijx.passly.domain.model.FieldGroup
import com.aozijx.passly.domain.model.FieldType
import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.domain.strategy.EntryTypeStrategy

/**
 * 助记词类型的业务策略实现
 */
class SeedPhraseEntryStrategy : EntryTypeStrategy {
    override val entryType = EntryType.SEED_PHRASE

    override fun validateRequiredFields(entry: VaultEntry): String? {
        if (entry.title.isBlank()) return "钱包名称不能为空"
        if (entry.cryptoSeedPhrase.isNullOrBlank()) return "助记词不能为空"
        return null
    }

    override fun validateFieldContent(entry: VaultEntry): String? {
        entry.cryptoSeedPhrase?.let { phrase ->
            val wordCount = phrase.split(Regex("\\s+")).size
            if (wordCount !in setOf(12, 24)) {
                return "助记词应包含 12 或 24 个单词，实际 $wordCount 个"
            }
        }
        return null
    }

    override fun getSensitiveFields(): Set<String> {
        return setOf("cryptoSeedPhrase", "password")
    }

    override fun extractSummary(entry: VaultEntry): String {
        return "12/24 词"
    }

    override fun suggestedCategory(): String = "加密"

    override fun supportsAutofill(): Boolean = false

    override fun initializeDefaults(entry: VaultEntry): VaultEntry {
        return entry.copy(
            category = suggestedCategory(),
            notes = entry.notes ?: "备份好这个助记词，永远不要分享给任何人"
        )
    }

    override fun getDetailFieldGroups(entry: VaultEntry): List<FieldGroup> {
        return listOf(
            FieldGroup(
                title = "钱包信息", fields = listOf(
                    FieldDefinition("title", "名称", isRequired = true), FieldDefinition(
                        "cryptoSeedPhrase",
                        "助记词",
                        isSensitive = true,
                        isRequired = true,
                        fieldType = FieldType.TEXTAREA
                    ), FieldDefinition(
                        "password", "钱包密码", isSensitive = true, fieldType = FieldType.PASSWORD
                    ), FieldDefinition("category", "分类", fieldType = FieldType.SELECT)
                )
            ), FieldGroup(
                title = "其他", fields = listOf(
                    FieldDefinition("notes", "备注", fieldType = FieldType.TEXTAREA)
                )
            )
        )
    }
}
