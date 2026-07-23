package com.aozijx.passly.domain.strategy.impl

import com.aozijx.passly.domain.model.entry.EntryType
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.strategy.EntryTypeStrategy

/**
 * 银行卡类型的业务策略实现
 */
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BankCardEntryStrategy @Inject constructor() : EntryTypeStrategy {
    override val entryType = EntryType.BANK_CARD

    override fun validateRequiredFields(entry: VaultEntry): String? {
        if (entry.title.isBlank()) return "银行名称不能为空"
        if (entry.credential.password.isNullOrBlank()) return "卡号不能为空"
        return null
    }

    override fun validateFieldContent(entry: VaultEntry): String? {
        // 卡号基本格式检查（Luhn 算法可选）
        val cardNumber = entry.credential.password.orEmpty().filter { it.isDigit() }
        if (cardNumber.length !in 13..19) {
            return "无效的卡号长度"
        }

        // 验证有效期格式
        entry.credential.cardExpiry?.let {
            if (!it.matches(Regex("^\\d{2}/\\d{2}$"))) {
                return "有效期格式应为 MM/YY"
            }
        }

        // CVV 格式检查
        entry.credential.cardCvv?.let {
            if (!it.matches(Regex("^\\d{3,4}$"))) {
                return "CVV 应为 3-4 位数字"
            }
        }

        return null
    }

    override fun getSensitiveFields(): Set<String> {
        return setOf(
            "password", "username", "cardCvv", "cardExpiration", "paymentPin", "securityAnswer"
        )
    }

    override fun extractSummary(entry: VaultEntry): String {
        // 显示卡号末四位
        val lastFour = entry.credential.password.orEmpty().takeLast(4)
        return "••${lastFour}"
    }

    override fun suggestedCategory(): String = "金融"

    override fun supportsAutofill(): Boolean = false

    override fun initializeDefaults(entry: VaultEntry): VaultEntry {
        return entry
    }


}
