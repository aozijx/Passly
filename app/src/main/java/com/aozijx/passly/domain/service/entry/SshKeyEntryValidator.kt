package com.aozijx.passly.domain.service.entry

import com.aozijx.passly.domain.model.entry.EntrySecret
import com.aozijx.passly.domain.model.entry.VaultEntry
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SshKeyEntryValidator @Inject constructor() : EntryValidator {
    override fun validateRequiredFields(entry: VaultEntry): String? {
        if (entry.summary.title.isBlank()) return "标识名不能为空"
        if ((entry.secret as? EntrySecret.SshKey)?.data?.privateKey.isNullOrBlank()) return "SSH 私钥不能为空"
        return null
    }

    override fun validateFieldContent(entry: VaultEntry): String? {
        (entry.secret as? EntrySecret.SshKey)?.data?.privateKey?.let {
            if (!it.contains("BEGIN")) return "无效的 SSH 私钥格式"
        }
        return null
    }
}
