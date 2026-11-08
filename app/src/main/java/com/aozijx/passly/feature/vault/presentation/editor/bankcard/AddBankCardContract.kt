package com.aozijx.passly.feature.vault.presentation.editor.bankcard

import androidx.annotation.StringRes
import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.credential.CardExpiry
import com.aozijx.passly.domain.entry.policy.CardCvvValidator
import com.aozijx.passly.domain.entry.policy.CardNumberValidationResult
import com.aozijx.passly.domain.entry.policy.CardNumberValidator
import com.aozijx.passly.feature.vault.presentation.editor.EditorLoadPhase

enum class CardType(
    @param:StringRes val labelRes: Int,
) {
    DEBIT(R.string.card_type_debit),
    CREDIT(R.string.card_type_credit),
}

data class AddBankCardFormState(
    val title: String = "",
    val cardType: CardType? = null,
    val cardholder: String = "",
    val cardNumber: String = "",
    val cardCvv: String = "",
    val paymentPin: String = "",
    val cardExpiry: CardExpiry? = null,
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
    val isPinVisible: Boolean = false,
) {
    val cardNumberValidation: CardNumberValidationResult
        get() = CardNumberValidator.validate(cardNumber)

    val isCvvValid: Boolean
        get() = CardCvvValidator.isValid(cardCvv.trim())

    val isValid: Boolean
        get() = title.isNotBlank() && cardNumberValidation is CardNumberValidationResult.Valid && isCvvValid
}

data class AddBankCardUiState(
    val form: AddBankCardFormState = AddBankCardFormState(),
    val isSaving: Boolean = false,
    val loadPhase: EditorLoadPhase = EditorLoadPhase.NotRequested,
) {
    val canSave: Boolean
        get() =
            form.isValid && !isSaving && loadPhase.allowsSaving
}

internal fun cardExpiryYearRange(currentYear: Int): IntRange = currentYear..(currentYear + 20)
