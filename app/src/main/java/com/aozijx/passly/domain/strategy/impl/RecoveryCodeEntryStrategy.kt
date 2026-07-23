package com.aozijx.passly.domain.strategy.impl

import com.aozijx.passly.domain.model.entry.EntrySecret
import com.aozijx.passly.domain.model.entry.EntryType
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.strategy.EntryTypeStrategy

/**
 * 恢复码类型的业务策略实现
 */
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecoveryCodeEntryStrategy @Inject constructor() : EntryTypeStrategy {
    override val entryType = EntryType.RECOVERY_CODE

    override fun validateRequiredFields(entry: VaultEntry): String? {
        if (entry.title.isBlank()) return "恢复码标题不能为空"
        val recoveryCodes = (entry.secret as? EntrySecret.Identity)?.data?.recoveryCodes
        if (recoveryCodes.isNullOrEmpty()) return "恢复码不能为空"
        return null
    }

    override fun validateFieldContent(entry: VaultEntry): String? {
        val recoveryCodes = (entry.secret as? EntrySecret.Identity)?.data?.recoveryCodes.orEmpty()
        if (recoveryCodes.isNotEmpty() && recoveryCodes.any { it.length < 4 }) {
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
        return entry
    }


}
