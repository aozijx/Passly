package com.aozijx.passly.domain.strategy.impl

import com.aozijx.passly.domain.model.entry.EntrySecret
import com.aozijx.passly.domain.model.entry.EntryType
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.strategy.EntryTypeStrategy

/**
 * Passkey 类型的业务策略实现
 */
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PasskeyEntryStrategy @Inject constructor() : EntryTypeStrategy {
    override val entryType = EntryType.PASSKEY

    override fun validateRequiredFields(entry: VaultEntry): String? {
        if (entry.title.isBlank()) return "Passkey 标题不能为空"
        if ((entry.secret as? EntrySecret.Passkey)?.data?.privateKeyReference.isNullOrBlank()) return "Passkey 数据不能为空"
        return null
    }

    override fun validateFieldContent(entry: VaultEntry): String? {
        return null
    }

    override fun getSensitiveFields(): Set<String> {
        return setOf("passkeyPrivateKeyReference", "recoveryCodes")
    }

    override fun extractSummary(entry: VaultEntry): String {
        return "Passkey"
    }

    override fun suggestedCategory(): String = "认证"

    override fun supportsAutofill(): Boolean = false

    override fun initializeDefaults(entry: VaultEntry): VaultEntry {
        return entry
    }


}
