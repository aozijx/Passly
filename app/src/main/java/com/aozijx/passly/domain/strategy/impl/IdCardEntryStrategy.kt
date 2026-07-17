package com.aozijx.passly.domain.strategy.impl

import com.aozijx.passly.domain.model.entry.EntryType
import com.aozijx.passly.domain.model.entry.FieldDefinition
import com.aozijx.passly.domain.model.entry.FieldGroup
import com.aozijx.passly.domain.model.entry.FieldType
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.strategy.EntryTypeStrategy

/**
 * 证件类型的业务策略实现
 */
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IdCardEntryStrategy @Inject constructor() : EntryTypeStrategy {
    override val entryType = EntryType.ID_CARD

    override fun validateRequiredFields(entry: VaultEntry): String? {
        if (entry.title.isBlank()) return "证件标题不能为空"
        if (entry.credential.idNumber.isNullOrBlank()) return "证件号码不能为空"
        return null
    }

    override fun validateFieldContent(entry: VaultEntry): String? {
        if (entry.credential.idNumber != null && entry.credential.idNumber.length < 6) {
            return "证件号码长度异常"
        }
        return null
    }

    override fun getSensitiveFields(): Set<String> {
        return setOf("idNumber")
    }

    override fun extractSummary(entry: VaultEntry): String {
        return if (entry.credential.cardExpiry.isNullOrBlank()) "证件信息" else "有效期 ${entry.credential.cardExpiry}"
    }

    override fun suggestedCategory(): String = "身份"

    override fun supportsAutofill(): Boolean = false

    override fun initializeDefaults(entry: VaultEntry): VaultEntry {
        return entry
    }

    override fun getDetailFieldGroups(entry: VaultEntry): List<FieldGroup> {
        return listOf(
            FieldGroup(
                title = "证件信息", fields = listOf(
                    FieldDefinition("title", "证件名称", isRequired = true),
                    FieldDefinition("idNumber", "证件号码", isSensitive = true, isRequired = true),
                    FieldDefinition("username", "姓名"),
                    FieldDefinition("category", "分类", fieldType = FieldType.SELECT)
                )
            ), FieldGroup(
                title = "有效期", fields = listOf(
                    FieldDefinition("cardExpiration", "有效期 (YYYY-MM-DD)")
                )
            ), FieldGroup(
                title = "其他", fields = listOf(
                    FieldDefinition("notes", "备注", fieldType = FieldType.TEXTAREA)
                )
            )
        )
    }
}
