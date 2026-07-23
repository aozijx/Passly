package com.aozijx.passly.domain.strategy.impl

import com.aozijx.passly.domain.model.entry.EntryType
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.strategy.EntryTypeStrategy

/**
 * 证件类型的业务策略实现
 */
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IdCardEntryStrategy @Inject constructor() : EntryTypeStrategy {
    override val entryType = EntryType.ID_CARD

    override fun validateRequiredFields(entry: VaultEntry): String? {
        if (entry.title.isBlank()) return "证件标题不能为空"
        if (entry.credential.idNumber.isNullOrBlank()) return "证件号码不能为空"
        return null
    }

    override fun validateFieldContent(entry: VaultEntry): String? {
        if (entry.credential.idNumber != null && entry.credential.idNumber.length < 6) {
            return "证件号码长度异常"
        }
        return null
    }

    override fun getSensitiveFields(): Set<String> {
        return setOf("idNumber")
    }

    override fun extractSummary(entry: VaultEntry): String {
        return if (entry.credential.cardExpiry.isNullOrBlank()) "证件信息" else "有效期 ${entry.credential.cardExpiry}"
    }

    override fun suggestedCategory(): String = "身份"

    override fun supportsAutofill(): Boolean = false

    override fun initializeDefaults(entry: VaultEntry): VaultEntry {
        return entry
    }


}
