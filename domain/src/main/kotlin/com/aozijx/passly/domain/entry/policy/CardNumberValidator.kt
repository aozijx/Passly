package com.aozijx.passly.domain.entry.policy

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

    fun validate(cardNumber: String): CardNumberValidationResult {
        val digits = cardNumber.filter { it.isDigit() }
        if (digits.isEmpty()) return CardNumberValidationResult.Empty
        if (digits.length !in 13..19) {
            return CardNumberValidationResult.Invalid(CardNumberInvalidReason.InvalidLength)
        }
        if (!luhn(digits)) {
            return CardNumberValidationResult.Invalid(CardNumberInvalidReason.ChecksumFailed)
        }
        return CardNumberValidationResult.Valid(inferNetwork(digits))
    }

    private fun inferNetwork(cardNumber: String): CardNetworkMatch {
        val digits = cardNumber.filter { it.isDigit() }
        val network = NETWORK_RULES.firstOrNull { it.pattern.matches(digits) }?.network
        return if (network == null) CardNetworkMatch.Unknown
        else CardNetworkMatch.Matched(network)
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

sealed interface CardNumberValidationResult {
    data object Empty : CardNumberValidationResult
    data class Valid(val network: CardNetworkMatch) : CardNumberValidationResult
    data class Invalid(val reason: CardNumberInvalidReason) : CardNumberValidationResult
}

sealed interface CardNetworkMatch {
    data class Matched(val network: CardNetwork) : CardNetworkMatch
    data object Unknown : CardNetworkMatch
}

enum class CardNumberInvalidReason {
    InvalidLength,
    ChecksumFailed,
}
