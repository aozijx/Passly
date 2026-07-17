package com.aozijx.passly.domain.strategy.impl

import com.aozijx.passly.domain.model.entry.EntryType
import com.aozijx.passly.domain.model.entry.FieldDefinition
import com.aozijx.passly.domain.model.entry.FieldGroup
import com.aozijx.passly.domain.model.entry.FieldType
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.strategy.EntryTypeStrategy

/**
 * TOTP 类型的业务策略实现
 */
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TotpEntryStrategy @Inject constructor() : EntryTypeStrategy {
    override val entryType = EntryType.LOGIN

    override fun validateRequiredFields(entry: VaultEntry): String? {
        if (entry.title.isBlank()) return "TOTP 标题不能为空"
        if (entry.credential.twoFactor?.otp?.secret.isNullOrBlank()) return "TOTP 密钥不能为空"
        return null
    }

    override fun validateFieldContent(entry: VaultEntry): String? {
        if ((entry.credential.twoFactor?.otp?.period ?: 30) <= 0) return "TOTP 周期必须大于 0"
        if ((entry.credential.twoFactor?.otp?.digits ?: 6) !in 5..8) return "TOTP 位数应在 5-8 位"
        return null
    }

    override fun getSensitiveFields(): Set<String> {
        return setOf("totpSecret")
    }

    override fun extractSummary(entry: VaultEntry): String {
        return "${entry.credential.twoFactor?.otp?.digits ?: 6} 位 / ${entry.credential.twoFactor?.otp?.period ?: 30}s"
    }

    override fun suggestedCategory(): String = "认证"

    override fun supportsAutofill(): Boolean = false

    override fun initializeDefaults(entry: VaultEntry): VaultEntry {
        return entry
    }

    override fun getDetailFieldGroups(entry: VaultEntry): List<FieldGroup> {
        return listOf(
            FieldGroup(
                title = "基本信息", fields = listOf(
                    FieldDefinition("title", "标题", isRequired = true),
                    FieldDefinition("totpSecret", "密钥", isSensitive = true, isRequired = true),
                    FieldDefinition("totpIssuer", "发行方"),
                    FieldDefinition("username", "账户名"),
                    FieldDefinition("category", "分类", fieldType = FieldType.SELECT)
                )
            ), FieldGroup(
                title = "算法设置", fields = listOf(
                    FieldDefinition("totpDigits", "位数", fieldType = FieldType.SELECT),
                    FieldDefinition("totpPeriod", "更新周期 (s)", fieldType = FieldType.SELECT),
                    FieldDefinition("totpAlgorithm", "算法", fieldType = FieldType.SELECT)
                )
            ), FieldGroup(
                title = "其他", fields = listOf(
                    FieldDefinition("notes", "备注", fieldType = FieldType.TEXTAREA)
                )
            )
        )
    }
}
