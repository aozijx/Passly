package com.aozijx.passly.domain.entry.service

import com.aozijx.passly.domain.entry.model.EntryAggregate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PasskeyEntryValidator @Inject constructor() : EntryValidator {
    override fun validateRequiredFields(entry: EntryAggregate): String? {
        if (entry.summary.title.isBlank()) return "Passkey 标题不能为空"
        if (entry.secret.passkey?.privateKeyReference.isNullOrBlank()) return "Passkey 数据不能为空"
        return null
    }

    override fun validateFieldContent(entry: EntryAggregate): String? = null
}
