package com.aozijx.passly.domain.strategy.impl

import com.aozijx.passly.core.common.EntryType
import com.aozijx.passly.domain.model.FieldDefinition
import com.aozijx.passly.domain.model.FieldGroup
import com.aozijx.passly.domain.model.FieldType
import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.domain.strategy.EntryTypeStrategy

/**
 * WiFi 类型的业务策略实现
 */
class WiFiEntryStrategy : EntryTypeStrategy {
    override val entryType = EntryType.WIFI

    override fun validateRequiredFields(entry: VaultEntry): String? {
        if (entry.title.isBlank()) return "WiFi 标题不能为空"
        if (entry.username.isBlank()) return "SSID 不能为空"
        if (entry.password.isBlank()) return "WiFi 密码不能为空"
        return null
    }

    override fun validateFieldContent(entry: VaultEntry): String? {
        if (entry.password.length < 8) return "WiFi 密码长度应在 8 位及以上"
        return null
    }

    override fun getSensitiveFields(): Set<String> {
        return setOf("password")
    }

    override fun extractSummary(entry: VaultEntry): String {
        return "加密类型 ${entry.wifiSecurityType ?: "WPA/WPA2"}"
    }

    override fun suggestedCategory(): String = "网络"

    override fun supportsAutofill(): Boolean = true

    override fun initializeDefaults(entry: VaultEntry): VaultEntry {
        return entry.copy(category = suggestedCategory())
    }

    override fun getDetailFieldGroups(entry: VaultEntry): List<FieldGroup> {
        return listOf(
            FieldGroup(
                title = "网络连接", fields = listOf(
                    FieldDefinition("title", "名称", isRequired = true),
                    FieldDefinition("username", "SSID", isRequired = true),
                    FieldDefinition(
                        "password",
                        "WiFi 密码",
                        isSensitive = true,
                        isRequired = true,
                        fieldType = FieldType.PASSWORD
                    ),
                    FieldDefinition("category", "分类", fieldType = FieldType.SELECT)
                )
            ), FieldGroup(
                title = "网络详情", fields = listOf(
                    FieldDefinition("wifiSecurityType", "安全性", fieldType = FieldType.SELECT),
                    FieldDefinition("wifiIsHidden", "隐藏网络", fieldType = FieldType.TOGGLE)
                )
            ), FieldGroup(
                title = "其他", fields = listOf(
                    FieldDefinition("notes", "备注", fieldType = FieldType.TEXTAREA)
                )
            )
        )
    }
}
