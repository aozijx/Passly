package com.aozijx.passly.feature.vault.editor.bankcard

import androidx.annotation.StringRes
import com.aozijx.passly.R

enum class CardType(@param:StringRes val labelRes: Int) {
    DEBIT(R.string.card_type_debit),
    CREDIT(R.string.card_type_credit)
}

data class AddBankCardFormState(
    val title: String = "",
    val cardType: CardType? = null,
    val cardholder: String = "",
    val cardNumber: String = "",
    val cardNumberError: String? = null,
    val inferredNetwork: CardNetwork? = null,
    val cardCvv: String = "",
    val paymentPin: String = "",
    val cardExpiryMonth: String = "",
    val cardExpiryYear: String = "",
    val billingAddress: String = "",
    val tags: String = "",
    val notes: String = "",
    val isCardNumberVisible: Boolean = false,
    val isCvvVisible: Boolean = false,
    val isPinVisible: Boolean = false
) {
    val isValid: Boolean
        get() = title.isNotBlank() && cardNumber.isNotBlank() && cardNumberError == null
}