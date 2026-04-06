package com.aozijx.passly.domain.strategy.impl

import com.aozijx.passly.core.common.EntryType
import com.aozijx.passly.domain.model.FieldDefinition
import com.aozijx.passly.domain.model.FieldGroup
import com.aozijx.passly.domain.model.FieldType
import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.domain.strategy.EntryTypeStrategy

/**
 * 恢复码类型的业务策略实现
 */
class RecoveryCodeEntryStrategy : EntryTypeStrategy {
    override val entryType = EntryType.RECOVERY_CODE

    override fun validateRequiredFields(entry: VaultEntry): String? {
        if (entry.title.isBlank()) return "恢复码标题不能为空"
        if (entry.recoveryCodes.isNullOrBlank()) return "恢复码不能为空"
        return null
    }

    override fun validateFieldContent(entry: VaultEntry): String? {
        if (entry.recoveryCodes != null && entry.recoveryCodes.length < 4) {
            return "恢复码内容异常"
        }
        return null
    }

    override fun getSensitiveFields(): Set<String> {
        return setOf("recoveryCodes")
    }

    override fun extractSummary(entry: VaultEntry): String {
        return "恢复码"
    }

    override fun suggestedCategory(): String = "认证"

    override fun supportsAutofill(): Boolean = false

    override fun initializeDefaults(entry: VaultEntry): VaultEntry {
        return entry.copy(category = suggestedCategory())
    }

    override fun getDetailFieldGroups(entry: VaultEntry): List<FieldGroup> {
        return listOf(
            FieldGroup(
                title = "恢复凭据", fields = listOf(
                    FieldDefinition("title", "名称", isRequired = true), FieldDefinition(
                        "recoveryCodes",
                        "恢复码内容",
                        isSensitive = true,
                        isRequired = true,
                        fieldType = FieldType.TEXTAREA
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
