package com.aozijx.passly.domain.strategy.impl

import com.aozijx.passly.core.common.EntryType
import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.domain.strategy.EntryTypeStrategy

/**
 * WiFi 类型的业务策略实现
 */
class WiFiEntryStrategy : EntryTypeStrategy {
    override val entryType = EntryType.WIFI

    override fun validateRequiredFields(entry: VaultEntry): String? {
        if (entry.title.isBlank()) return "WiFi 名称不能为空"
        if (entry.username.isBlank()) return "SSID 不能为空"
        if (entry.password.isBlank()) return "密码不能为空"
        return null
    }

    override fun validateFieldContent(entry: VaultEntry): String? {
        // WiFi 加密类型验证
        val validEncryption = setOf("WPA", "WPA2", "WPA3", "WEP", "Open")
        if (!validEncryption.contains(entry.wifiEncryptionType)) {
            return "无效的加密类型: ${entry.wifiEncryptionType}"
        }
        return null
    }

    override fun getSensitiveFields(): Set<String> {
        return setOf("password", "username")
    }

    override fun extractSummary(entry: VaultEntry): String {
        return entry.wifiEncryptionType ?: "Unknown"
    }

    override fun suggestedCategory(): String = "网络"

    override fun supportsAutofill(): Boolean = true

    override fun initializeDefaults(entry: VaultEntry): VaultEntry {
        return entry.copy(
            category = suggestedCategory(),
            wifiEncryptionType = entry.wifiEncryptionType ?: "WPA2"
        )
    }
}
