package com.aozijx.passly.domain.service.entry

import com.aozijx.passly.domain.model.entry.VaultEntry
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TotpEntryValidator @Inject constructor() : EntryValidator {
    override fun validateRequiredFields(entry: VaultEntry): String? {
        if (entry.summary.title.isBlank()) return "TOTP 标题不能为空"
        if (entry.secret.otp?.config?.secret.isNullOrBlank()) return "TOTP 密钥不能为空"
        return null
    }

    override fun validateFieldContent(entry: VaultEntry): String? {
        val config = entry.secret.otp?.config
        if ((config?.periodSeconds ?: 30) <= 0) return "TOTP 周期必须大于 0"
        if ((config?.digits ?: 6) !in 5..8) return "TOTP 位数应在 5-8 位"
        return null
    }
}
