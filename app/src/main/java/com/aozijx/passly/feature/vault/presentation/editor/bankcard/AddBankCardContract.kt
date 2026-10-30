package com.aozijx.passly.feature.vault.presentation.editor.bankcard

import androidx.annotation.StringRes
import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.credential.CardExpiry
import com.aozijx.passly.domain.entry.policy.CardNumberValidationResult
import com.aozijx.passly.domain.entry.policy.CardNumberValidator
import com.aozijx.passly.domain.entry.policy.CardCvvValidator
import com.aozijx.passly.feature.vault.presentation.editor.EditorLoadPhase

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
    val cardNumberValidation: CardNumberValidationResult
        get() = CardNumberValidator.validate(cardNumber)

    val isCvvValid: Boolean
        get() = CardCvvValidator.isValid(cardCvv.trim())

    val isValid: Boolean
        get() = title.isNotBlank() && cardNumberValidation is CardNumberValidationResult.Valid && isCvvValid
}

sealed interface AddBankCardSavePhase {
    data object Idle : AddBankCardSavePhase
    data object Saving : AddBankCardSavePhase
}

data class AddBankCardUiState(
    val form: AddBankCardFormState = AddBankCardFormState(),
    val savePhase: AddBankCardSavePhase = AddBankCardSavePhase.Idle,
    val loadPhase: EditorLoadPhase = EditorLoadPhase.NotRequested,
    val loadFailed: Boolean = false,
) {
    val canSave: Boolean
        get() = form.isValid && savePhase == AddBankCardSavePhase.Idle &&
            loadPhase != EditorLoadPhase.Loading && loadPhase != EditorLoadPhase.Failed
}

internal fun cardExpiryYearRange(currentYear: Int): IntRange = currentYear..(currentYear + 20)
