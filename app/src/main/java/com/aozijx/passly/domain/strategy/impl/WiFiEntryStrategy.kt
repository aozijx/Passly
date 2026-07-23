package com.aozijx.passly.domain.strategy.impl

import com.aozijx.passly.domain.model.entry.EntryType
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.strategy.EntryTypeStrategy

/**
 * WiFi 类型的业务策略实现
 */
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WiFiEntryStrategy @Inject constructor() : EntryTypeStrategy {
    override val entryType = EntryType.WIFI

    override fun validateRequiredFields(entry: VaultEntry): String? {
        if (entry.title.isBlank()) return "WiFi 标题不能为空"
        if (entry.username.isBlank()) return "SSID 不能为空"
        if (entry.credential.password.isNullOrBlank()) return "WiFi 密码不能为空"
        return null
    }

    override fun validateFieldContent(entry: VaultEntry): String? {
        if ((entry.credential.password?.length ?: 0) < 8) return "WiFi 密码长度应在 8 位及以上"
        return null
    }

    override fun getSensitiveFields(): Set<String> {
        return setOf("password")
    }

    override fun extractSummary(entry: VaultEntry): String {
        return "加密类型 ${entry.credential.wifiSecurityType ?: "WPA/WPA2"}"
    }

    override fun suggestedCategory(): String = "网络"

    override fun supportsAutofill(): Boolean = true

    override fun initializeDefaults(entry: VaultEntry): VaultEntry {
        return entry
    }


}
