package com.aozijx.passly.domain.strategy.impl

import com.aozijx.passly.domain.model.entry.EntrySecret
import com.aozijx.passly.domain.model.entry.EntryType
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.strategy.EntryTypeStrategy

/**
 * SSH 密钥类型的业务策略实现
 */
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SshKeyEntryStrategy @Inject constructor() : EntryTypeStrategy {
    override val entryType = EntryType.SSH_KEY

    override fun validateRequiredFields(entry: VaultEntry): String? {
        if (entry.title.isBlank()) return "标识名不能为空"
        if ((entry.secret as? EntrySecret.SshKey)?.data?.privateKey.isNullOrBlank()) return "SSH 私钥不能为空"
        return null
    }

    override fun validateFieldContent(entry: VaultEntry): String? {
        (entry.secret as? EntrySecret.SshKey)?.data?.privateKey?.let {
            if (!it.contains("BEGIN")) return "无效的 SSH 私钥格式"
        }
        return null
    }

    override fun getSensitiveFields(): Set<String> {
        return setOf("sshPrivateKey", "password")
    }

    override fun extractSummary(entry: VaultEntry): String {
        return entry.website?.matchDomains?.firstOrNull() ?: "无主机"
    }

    override fun suggestedCategory(): String = "技术"

    override fun supportsAutofill(): Boolean = false

    override fun initializeDefaults(entry: VaultEntry): VaultEntry {
        return entry
    }


}
