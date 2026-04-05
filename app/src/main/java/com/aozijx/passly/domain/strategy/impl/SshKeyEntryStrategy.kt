package com.aozijx.passly.domain.strategy.impl

import com.aozijx.passly.core.common.EntryType
import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.domain.strategy.EntryTypeStrategy

/**
 * SSH 密钥类型的业务策略实现
 */
class SshKeyEntryStrategy : EntryTypeStrategy {
    override val entryType = EntryType.SSH_KEY

    override fun validateRequiredFields(entry: VaultEntry): String? {
        if (entry.title.isBlank()) return "标识名不能为空"
        if (entry.sshPrivateKey.isNullOrBlank()) return "SSH 私钥不能为空"
        return null
    }

    override fun validateFieldContent(entry: VaultEntry): String? {
        entry.sshPrivateKey?.let {
            if (!it.contains("BEGIN")) return "无效的 SSH 私钥格式"
        }
        return null
    }

    override fun getSensitiveFields(): Set<String> {
        return setOf("sshPrivateKey", "password")
    }

    override fun extractSummary(entry: VaultEntry): String {
        return entry.uriList?.firstOrNull() ?: "无主机"
    }

    override fun suggestedCategory(): String = "技术"

    override fun supportsAutofill(): Boolean = false

    override fun initializeDefaults(entry: VaultEntry): VaultEntry {
        return entry.copy(
            category = suggestedCategory(),
            notes = entry.notes ?: "SSH 连接凭据"
        )
    }
}
