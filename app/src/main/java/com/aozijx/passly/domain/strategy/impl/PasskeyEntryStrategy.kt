package com.aozijx.passly.domain.strategy.impl

import com.aozijx.passly.core.common.EntryType
import com.aozijx.passly.domain.model.FieldDefinition
import com.aozijx.passly.domain.model.FieldGroup
import com.aozijx.passly.domain.model.FieldType
import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.domain.strategy.EntryTypeStrategy

/**
 * Passkey 类型的业务策略实现
 */
class PasskeyEntryStrategy : EntryTypeStrategy {
    override val entryType = EntryType.PASSKEY

    override fun validateRequiredFields(entry: VaultEntry): String? {
        if (entry.title.isBlank()) return "Passkey 标题不能为空"
        if (entry.passkeyDataJson.isNullOrBlank()) return "Passkey 数据不能为空"
        return null
    }

    override fun validateFieldContent(entry: VaultEntry): String? {
        if (!entry.recoveryCodes.isNullOrBlank() && entry.recoveryCodes.length < 6) {
            return "恢复码内容异常"
        }
        return null
    }

    override fun getSensitiveFields(): Set<String> {
        return setOf("passkeyDataJson", "recoveryCodes")
    }

    override fun extractSummary(entry: VaultEntry): String {
        return if (entry.recoveryCodes.isNullOrBlank()) "Passkey" else "Passkey + 恢复码"
    }

    override fun suggestedCategory(): String = "认证"

    override fun supportsAutofill(): Boolean = false

    override fun initializeDefaults(entry: VaultEntry): VaultEntry {
        return entry.copy(category = suggestedCategory())
    }

    override fun getDetailFieldGroups(entry: VaultEntry): List<FieldGroup> {
        return listOf(
            FieldGroup(
                title = "通行密钥", fields = listOf(
                    FieldDefinition("title", "名称", isRequired = true),
                    FieldDefinition("username", "账户名"),
                    FieldDefinition(
                        "passkeyDataJson",
                        "Passkey 凭据 (JSON)",
                        isSensitive = true,
                        isRequired = true,
                        fieldType = FieldType.TEXTAREA
                    ),
                    FieldDefinition("category", "分类", fieldType = FieldType.SELECT)
                )
            ), FieldGroup(
                title = "备用方案", fields = listOf(
                    FieldDefinition(
                        "recoveryCodes",
                        "恢复码",
                        isSensitive = true,
                        fieldType = FieldType.TEXTAREA
                    )
                )
            ), FieldGroup(
                title = "其他", fields = listOf(
                    FieldDefinition("notes", "备注", fieldType = FieldType.TEXTAREA)
                )
            )
        )
    }
}
