package com.aozijx.passly.feature.vault.presentation.editor.bankcard

import androidx.annotation.StringRes
import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.credential.CardExpiry
import com.aozijx.passly.domain.entry.policy.CardNumberValidationResult

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
    data class Selected(val value: CardExpiry) : CardExpirySelection
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
    val issuingBank: String = "",
    val bankBranch: String = "",
    val cardProduct: String = "",
    val currency: String = "",
    val bankPhone: String = "",
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
