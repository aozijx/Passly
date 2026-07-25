package com.aozijx.passly.domain.entry.service

import com.aozijx.passly.domain.entry.model.VaultEntry
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoginEntryValidator @Inject constructor() : EntryValidator {
    override fun validateRequiredFields(entry: VaultEntry): String? {
        if (entry.summary.title.isBlank()) return "标题不能为空"
        if (entry.summary.username.isBlank()) return "用户名不能为空"
        if (entry.secret.login?.password.isNullOrBlank()) return "密码不能为空"
        return null
    }

    override fun validateFieldContent(entry: VaultEntry): String? {
        val passwordLength = entry.secret.login?.password?.length ?: 0
        if (passwordLength < 1) return "密码为空"
        return null
    }
}
