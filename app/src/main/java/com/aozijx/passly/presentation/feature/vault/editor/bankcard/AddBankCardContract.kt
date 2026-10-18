package com.aozijx.passly.presentation.feature.vault.editor.bankcard

import androidx.annotation.StringRes
import com.aozijx.passly.R
import com.aozijx.passly.feature.vault.editor.bankcard.CardNumberValidationResult

enum class CardType(@param:StringRes val labelRes: Int) {
    DEBIT(R.string.card_type_debit),
    CREDIT(R.string.card_type_credit)
}

sealed interface CardTypeSelection {
    data object Unselected : CardTypeSelection
    data class Selected(val type: CardType) : CardTypeSelection
}

sealed interface CardExpirySelection {
    data object NotSet : CardExpirySelection
    data class Selected(val month: Int, val year: Int) : CardExpirySelection {
        init {
            require(month in 1..12) { "Card expiry month must be between 1 and 12" }
            require(year > 0) { "Card expiry year must be positive" }
        }

        val storageValue: String
            get() = "${month.toString().padStart(2, '0')}/$year"
    }
}

data class AddBankCardFormState(
    val title: String = "",
    val cardType: CardTypeSelection = CardTypeSelection.Unselected,
    val cardholder: String = "",
    val cardNumber: String = "",
    val cardNumberValidation: CardNumberValidationResult = CardNumberValidationResult.Empty,
    val cardCvv: String = "",
    val paymentPin: String = "",
    val cardExpiry: CardExpirySelection = CardExpirySelection.NotSet,
    val billingAddress: String = "",
    val tags: String = "",
    val notes: String = "",
    val isCardNumberVisible: Boolean = false,
    val isCvvVisible: Boolean = false,
    val isPinVisible: Boolean = false
) {
    val isValid: Boolean
        get() = title.isNotBlank() && cardNumberValidation is CardNumberValidationResult.Valid
}

sealed interface AddBankCardSavePhase {
    data object Idle : AddBankCardSavePhase
    data object Saving : AddBankCardSavePhase
}

data class AddBankCardUiState(
    val form: AddBankCardFormState = AddBankCardFormState(),
    val savePhase: AddBankCardSavePhase = AddBankCardSavePhase.Idle,
) {
    val canSave: Boolean
        get() = form.isValid && savePhase == AddBankCardSavePhase.Idle
}

internal fun cardExpiryYearRange(currentYear: Int): IntRange = currentYear..(currentYear + 20)
