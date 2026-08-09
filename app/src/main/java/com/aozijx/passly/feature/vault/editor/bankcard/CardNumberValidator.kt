package com.aozijx.passly.feature.vault.editor.bankcard

object CardNumberValidator {

    private val NETWORK_RULES = listOf(
        NetworkRule(CardNetwork.VISA, Regex("^4\\d{12}(?:\\d{3})?$")),
        NetworkRule(
            CardNetwork.MASTERCARD,
            Regex("^(5[1-5]\\d{14}|2(2[2-9][1-9]|[3-6]\\d{2}|7[01]\\d|720)\\d{12})$")
        ),
        NetworkRule(CardNetwork.AMEX, Regex("^3[47]\\d{13}$")),
        NetworkRule(
            CardNetwork.DISCOVER,
            Regex("^(6011\\d{12}|65\\d{14}|64[4-9]\\d{13}|622(12[6-9]|1[3-9]\\d|[2-8]\\d{2}|9[01]\\d|92[0-5])\\d{10})$")
        ),
        NetworkRule(CardNetwork.UNIONPAY, Regex("^62\\d{14,17}$")),
        NetworkRule(CardNetwork.JCB, Regex("^35(2[89]|[3-8]\\d)\\d{12}$"))
    )

    fun validate(cardNumber: String): ValidationResult {
        val digits = cardNumber.filter { it.isDigit() }
        if (digits.isEmpty()) return ValidationResult.Valid
        if (digits.length !in 13..19) {
            return ValidationResult.Invalid("卡号位数不正确")
        }
        if (!luhn(digits)) {
            return ValidationResult.Invalid("卡号校验失败")
        }
        return ValidationResult.Valid
    }

    fun inferNetwork(cardNumber: String): CardNetwork? {
        val digits = cardNumber.filter { it.isDigit() }
        if (digits.length < 6) return null
        return NETWORK_RULES.firstOrNull { it.pattern.matches(digits) }?.network
    }

    private fun luhn(digits: String): Boolean {
        var sum = 0
        var alternate = false
        for (i in digits.lastIndex downTo 0) {
            var n = digits[i].digitToInt()
            if (alternate) {
                n *= 2
                if (n > 9) n -= 9
            }
            sum += n
            alternate = !alternate
        }
        return sum % 10 == 0
    }

    private data class NetworkRule(
        val network: CardNetwork,
        val pattern: Regex
    )
}

enum class CardNetwork {
    VISA, MASTERCARD, AMEX, DISCOVER, UNIONPAY, JCB
}

sealed class ValidationResult {
    data object Valid : ValidationResult()
    data class Invalid(val message: String) : ValidationResult()
}