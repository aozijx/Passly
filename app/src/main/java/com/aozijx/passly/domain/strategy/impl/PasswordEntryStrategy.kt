package com.aozijx.passly.domain.strategy.impl

import com.aozijx.passly.domain.model.entry.EntrySecret
import com.aozijx.passly.domain.model.entry.EntryType
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.strategy.EntryTypeStrategy

import javax.inject.Inject
import javax.inject.Singleton

/**
 * 密码类型的业务策略实现
 */
@Singleton
class PasswordEntryStrategy @Inject constructor() : EntryTypeStrategy {
    override val entryType = EntryType.LOGIN

    override fun validateRequiredFields(entry: VaultEntry): String? {
        if (entry.title.isBlank()) return "标题不能为空"
        if (entry.username.isBlank()) return "用户名不能为空"
        if ((entry.secret as? EntrySecret.Login)?.data?.password.isNullOrBlank()) return "密码不能为空"
        return null
    }

    override fun validateFieldContent(entry: VaultEntry): String? {
        // 检查是否存在基本的密码安全性
        val passwordLength = (entry.secret as? EntrySecret.Login)?.data?.password?.length ?: 0
        if (passwordLength < 1) return "密码为空"
        // 可以添加更复杂的密码强度检查
        return null
    }

    override fun getSensitiveFields(): Set<String> {
        return setOf("password", "username", "totpSecret")
    }

    override fun extractSummary(entry: VaultEntry): String {
        return entry.website?.matchDomains?.firstOrNull() ?: "无网址"
    }

    override fun suggestedCategory(): String = "账户"

    override fun supportsAutofill(): Boolean = true

    override fun initializeDefaults(entry: VaultEntry): VaultEntry {
        return entry
    }


}
