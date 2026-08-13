package com.aozijx.passly.domain.entry.service

import com.aozijx.passly.domain.entry.model.EntryAggregate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SshKeyEntryValidator @Inject constructor() : EntryValidator {
    override fun validateRequiredFields(entry: EntryAggregate): String? {
        if (entry.summary.title.isBlank()) return "标识名不能为空"
        if (entry.secret.ssh?.privateKey.isNullOrBlank()) return "SSH 私钥不能为空"
        return null
    }

    override fun validateFieldContent(entry: EntryAggregate): String? {
        entry.secret.ssh?.privateKey?.let {
            if (!it.contains("BEGIN")) return "无效的 SSH 私钥格式"
        }
        return null
    }
}
