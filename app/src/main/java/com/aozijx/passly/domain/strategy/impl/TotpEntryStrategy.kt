package com.aozijx.passly.domain.strategy.impl

import com.aozijx.passly.core.common.EntryType
import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.domain.strategy.EntryTypeStrategy

/**
 * TOTP 类型的业务策略实现
 */
class TotpEntryStrategy : EntryTypeStrategy {
    override val entryType = EntryType.TOTP

    override fun validateRequiredFields(entry: VaultEntry): String? {
        if (entry.title.isBlank()) return "TOTP 标题不能为空"
        if (entry.totpSecret.isNullOrBlank()) return "TOTP 密钥不能为空"
        return null
    }

    override fun validateFieldContent(entry: VaultEntry): String? {
        if (entry.totpPeriod <= 0) return "TOTP 周期必须大于 0"
        if (entry.totpDigits !in 5..8) return "TOTP 位数应在 5-8 位"
        return null
    }

    override fun getSensitiveFields(): Set<String> {
        return setOf("totpSecret")
    }

    override fun extractSummary(entry: VaultEntry): String {
        return "${entry.totpDigits} 位 / ${entry.totpPeriod}s"
    }

    override fun suggestedCategory(): String = "认证"

    override fun supportsAutofill(): Boolean = false

    override fun initializeDefaults(entry: VaultEntry): VaultEntry {
        return entry.copy(category = suggestedCategory())
    }
}
