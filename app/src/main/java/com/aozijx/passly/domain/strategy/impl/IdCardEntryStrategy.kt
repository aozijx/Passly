package com.aozijx.passly.domain.strategy.impl

import com.aozijx.passly.core.common.EntryType
import com.aozijx.passly.domain.model.FieldDefinition
import com.aozijx.passly.domain.model.FieldGroup
import com.aozijx.passly.domain.model.FieldType
import com.aozijx.passly.domain.model.core.VaultEntry
import com.aozijx.passly.domain.strategy.EntryTypeStrategy

/**
 * 证件类型的业务策略实现
 */
class IdCardEntryStrategy : EntryTypeStrategy {
    override val entryType = EntryType.ID_CARD

    override fun validateRequiredFields(entry: VaultEntry): String? {
        if (entry.title.isBlank()) return "证件标题不能为空"
        if (entry.idNumber.isNullOrBlank()) return "证件号码不能为空"
        return null
    }

    override fun validateFieldContent(entry: VaultEntry): String? {
        if (entry.idNumber != null && entry.idNumber.length < 6) {
            return "证件号码长度异常"
        }
        return null
    }

    override fun getSensitiveFields(): Set<String> {
        return setOf("idNumber")
    }

    override fun extractSummary(entry: VaultEntry): String {
        return if (entry.cardExpiration.isNullOrBlank()) "证件信息" else "有效期 ${entry.cardExpiration}"
    }

    override fun suggestedCategory(): String = "身份"

    override fun supportsAutofill(): Boolean = false

    override fun initializeDefaults(entry: VaultEntry): VaultEntry {
        return entry.copy(category = suggestedCategory())
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
