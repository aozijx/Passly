package com.aozijx.passly.presentation.feature.vault.editor.bankcard

import androidx.annotation.StringRes
import com.aozijx.passly.R
import com.aozijx.passly.feature.vault.editor.bankcard.CardNetwork

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

data class AddBankCardUiState(
    val form: AddBankCardFormState = AddBankCardFormState(),
    val canSave: Boolean = false,
    val isSaving: Boolean = false,
)

internal fun cardExpiryYearRange(currentYear: Int): IntRange = 1900..currentYear

internal fun AddBankCardFormState.withCardExpiry(month: Int, year: Int): AddBankCardFormState {
    require(month in 1..12) { "Card expiry month must be between 1 and 12" }
    require(year > 0) { "Card expiry year must be positive" }
    return copy(
        cardExpiryMonth = month.toString().padStart(2, '0'),
        cardExpiryYear = year.toString(),
    )
}

internal fun AddBankCardFormState.withoutCardExpiry(): AddBankCardFormState = copy(
    cardExpiryMonth = "",
    cardExpiryYear = "",
)
