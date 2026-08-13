package com.aozijx.passly.domain.entry.service

import com.aozijx.passly.domain.entry.model.EntryAggregate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BankCardEntryValidator @Inject constructor() : EntryValidator {
    override fun validateRequiredFields(entry: EntryAggregate): String? {
        if (entry.summary.title.isBlank()) return "银行名称不能为空"
        val cardSecret = entry.secret.card
        val hasCardNumber = cardSecret?.hasCardNumber == true || !cardSecret?.cardNumber.isNullOrBlank()
        if (!hasCardNumber) return "卡号不能为空"
        return null
    }

    override fun validateFieldContent(entry: EntryAggregate): String? {
        val cardSecret = entry.secret.card
        val cardNumber = cardSecret?.cardNumber.orEmpty().filter { it.isDigit() }
        if (cardNumber.isNotEmpty() && cardNumber.length !in 13..19) return "无效的卡号长度"

        cardSecret?.cardExpiry?.let {
            if (!it.matches(Regex("^\\d{2}/\\d{2}$"))) return "有效期格式应为 MM/YY"
        }

        cardSecret?.cardCvv?.let {
            if (!it.matches(Regex("^\\d{3,4}$"))) return "CVV 应为 3-4 位数字"
        }

        return null
    }
}
