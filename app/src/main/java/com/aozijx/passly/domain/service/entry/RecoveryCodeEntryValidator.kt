package com.aozijx.passly.domain.service.entry

import com.aozijx.passly.domain.model.entry.EntrySecret
import com.aozijx.passly.domain.model.entry.VaultEntry
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecoveryCodeEntryValidator @Inject constructor() : EntryValidator {
    override fun validateRequiredFields(entry: VaultEntry): String? {
        if (entry.summary.title.isBlank()) return "恢复码标题不能为空"
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
}
