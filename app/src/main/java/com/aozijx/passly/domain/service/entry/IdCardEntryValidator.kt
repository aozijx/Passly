package com.aozijx.passly.domain.service.entry

import com.aozijx.passly.domain.model.entry.EntrySecret
import com.aozijx.passly.domain.model.entry.VaultEntry
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IdCardEntryValidator @Inject constructor() : EntryValidator {
    override fun validateRequiredFields(entry: VaultEntry): String? {
        if (entry.summary.title.isBlank()) return "证件标题不能为空"
        if ((entry.secret as? EntrySecret.Identity)?.data?.idNumber.isNullOrBlank()) return "证件号码不能为空"
        return null
    }

    override fun validateFieldContent(entry: VaultEntry): String? {
        val idNumber = (entry.secret as? EntrySecret.Identity)?.data?.idNumber
        if (idNumber != null && idNumber.length < 6) return "证件号码长度异常"
        return null
    }
}
