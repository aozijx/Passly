package com.aozijx.passly.feature.vault.presentation.editor.bankcard

import androidx.annotation.StringRes
import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.credential.CardExpiry
import com.aozijx.passly.domain.entry.policy.CardCvvValidator
import com.aozijx.passly.domain.entry.policy.CardNumberInvalidReason
import com.aozijx.passly.domain.entry.policy.CardNumberValidationResult
import com.aozijx.passly.domain.entry.policy.CardNumberValidator
import com.aozijx.passly.feature.vault.presentation.editor.EditorLoadPhase

enum class CardType(
    @param:StringRes val labelRes: Int,
) {
    DEBIT(R.string.card_type_debit),
    CREDIT(R.string.card_type_credit),
}

sealed interface CardTypeSelection {
    data object Unselected : CardTypeSelection

    data class Selected(
        val type: CardType,
    ) : CardTypeSelection
}

sealed interface CardExpirySelection {
    data object NotSet : CardExpirySelection

    data class Selected(
        val expiry: CardExpiry,
    ) : CardExpirySelection
}

sealed interface BankCardFieldFeedbackUiState {
    data object Hidden : BankCardFieldFeedbackUiState

    data class Message(
        @param:StringRes val messageRes: Int,
    ) : BankCardFieldFeedbackUiState

    val isError: Boolean
        get() = this is Message
}

data class AddBankCardFormState(
    val title: String = "",
    val cardTypeSelection: CardTypeSelection = CardTypeSelection.Unselected,
    val cardholder: String = "",
    val cardNumber: String = "",
    val cardCvv: String = "",
    val paymentPin: String = "",
    val cardExpirySelection: CardExpirySelection = CardExpirySelection.NotSet,
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

    val cardNumberFeedback: BankCardFieldFeedbackUiState
        get() =
            when (val validation = cardNumberValidation) {
                CardNumberValidationResult.Empty,
                is CardNumberValidationResult.Valid,
                -> BankCardFieldFeedbackUiState.Hidden

                is CardNumberValidationResult.Invalid -> validation.reason.toFeedbackUiState()
            }

    val cvvFeedback: BankCardFieldFeedbackUiState
        get() =
            if (isCvvValid) {
                BankCardFieldFeedbackUiState.Hidden
            } else {
                BankCardFieldFeedbackUiState.Message(R.string.vault_card_cvv_invalid)
            }

    val isValid: Boolean
        get() = title.isNotBlank() && cardNumberValidation is CardNumberValidationResult.Valid && isCvvValid
}

private fun CardNumberInvalidReason.toFeedbackUiState(): BankCardFieldFeedbackUiState.Message =
    BankCardFieldFeedbackUiState.Message(
        messageRes =
            when (this) {
                CardNumberInvalidReason.InvalidLength -> R.string.card_number_invalid_length
                CardNumberInvalidReason.InvalidCharacters -> R.string.card_number_invalid_characters
                CardNumberInvalidReason.ChecksumFailed -> R.string.card_number_checksum_failed
            },
    )

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
