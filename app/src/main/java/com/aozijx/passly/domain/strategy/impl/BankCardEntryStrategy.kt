package com.aozijx.passly.domain.strategy.impl

import com.aozijx.passly.core.common.EntryType
import com.aozijx.passly.domain.model.FieldDefinition
import com.aozijx.passly.domain.model.FieldGroup
import com.aozijx.passly.domain.model.FieldType
import com.aozijx.passly.domain.model.core.VaultEntry
import com.aozijx.passly.domain.strategy.EntryTypeStrategy

/**
 * 银行卡类型的业务策略实现
 */
class BankCardEntryStrategy : EntryTypeStrategy {
    override val entryType = EntryType.BANK_CARD

    override fun validateRequiredFields(entry: VaultEntry): String? {
        if (entry.title.isBlank()) return "银行名称不能为空"
        if (entry.password.isBlank()) return "卡号不能为空"
        return null
    }

    override fun validateFieldContent(entry: VaultEntry): String? {
        // 卡号基本格式检查（Luhn 算法可选）
        val cardNumber = entry.password.filter { it.isDigit() }
        if (cardNumber.length !in 13..19) {
            return "无效的卡号长度"
        }

        // 验证有效期格式
        entry.cardExpiration?.let {
            if (!it.matches(Regex("^\\d{2}/\\d{2}$"))) {
                return "有效期格式应为 MM/YY"
            }
        }

        // CVV 格式检查
        entry.cardCvv?.let {
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
        val lastFour = entry.password.takeLast(4)
        return "••${lastFour}"
    }

    override fun suggestedCategory(): String = "金融"

    override fun supportsAutofill(): Boolean = false

    override fun initializeDefaults(entry: VaultEntry): VaultEntry {
        return entry.copy(
            category = suggestedCategory(), matchType = 0 // 不启用自动填充匹配
        )
    }

    override fun getDetailFieldGroups(entry: VaultEntry): List<FieldGroup> {
        return listOf(
            FieldGroup(
                title = "卡片信息", fields = listOf(
                    FieldDefinition("title", "银行/机构名称", isRequired = true),
                    FieldDefinition("password", "卡号", isSensitive = true, isRequired = true),
                    FieldDefinition("username", "持卡人姓名"),
                    FieldDefinition("category", "分类", fieldType = FieldType.SELECT)
                )
            ), FieldGroup(
                title = "安全详情", fields = listOf(
                    FieldDefinition("cardExpiration", "有效期 (MM/YY)"),
                    FieldDefinition("cardCvv", "CVV", isSensitive = true),
                    FieldDefinition("paymentPin", "支付密码", isSensitive = true),
                    FieldDefinition("securityAnswer", "密保答案", isSensitive = true)
                )
            ), FieldGroup(
                title = "其他", fields = listOf(
                    FieldDefinition("notes", "备注", fieldType = FieldType.TEXTAREA)
                )
            )
        )
    }
}
