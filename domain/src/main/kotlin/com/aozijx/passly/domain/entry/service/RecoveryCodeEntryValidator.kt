package com.aozijx.passly.domain.entry.service

import com.aozijx.passly.domain.entry.model.EntryAggregate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecoveryCodeEntryValidator @Inject constructor() : EntryValidator {
    override fun validateRequiredFields(entry: EntryAggregate): String? {
        if (entry.summary.title.isBlank()) return "恢复码标题不能为空"
        val recoveryCodes = entry.secret.identity?.recoveryCodes
        if (recoveryCodes.isNullOrEmpty()) return "恢复码不能为空"
        return null
    }

    override fun validateFieldContent(entry: EntryAggregate): String? {
        val recoveryCodes = entry.secret.identity?.recoveryCodes.orEmpty()
        if (recoveryCodes.isNotEmpty() && recoveryCodes.any { it.length < 4 }) return "恢复码内容异常"
        return null
    }
}
