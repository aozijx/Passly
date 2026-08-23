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

sealed interface AddBankCardEffect {
    data object Saved : AddBankCardEffect
    data class SaveFailed(val message: String?) : AddBankCardEffect
}
