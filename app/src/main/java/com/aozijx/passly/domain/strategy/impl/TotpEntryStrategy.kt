package com.aozijx.passly.domain.strategy.impl

import com.aozijx.passly.domain.model.entry.EntryType
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.strategy.EntryTypeStrategy

/**
 * TOTP 类型的业务策略实现
 */
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TotpEntryStrategy @Inject constructor() : EntryTypeStrategy {
    override val entryType = EntryType.TOTP

    override fun validateRequiredFields(entry: VaultEntry): String? {
        if (entry.title.isBlank()) return "TOTP 标题不能为空"
        if (entry.credential.otp?.secret.isNullOrBlank()) return "TOTP 密钥不能为空"
        return null
    }

    override fun validateFieldContent(entry: VaultEntry): String? {
        if ((entry.credential.otp?.periodSeconds ?: 30) <= 0) return "TOTP 周期必须大于 0"
        if ((entry.credential.otp?.digits ?: 6) !in 5..8) return "TOTP 位数应在 5-8 位"
        return null
    }

    override fun getSensitiveFields(): Set<String> {
        return setOf("totpSecret")
    }

    override fun extractSummary(entry: VaultEntry): String {
        return "${entry.credential.otp?.digits ?: 6} 位 / ${entry.credential.otp?.periodSeconds ?: 30}s"
    }

    override fun suggestedCategory(): String = "认证"

    override fun supportsAutofill(): Boolean = false

    override fun initializeDefaults(entry: VaultEntry): VaultEntry {
        return entry
    }


}
