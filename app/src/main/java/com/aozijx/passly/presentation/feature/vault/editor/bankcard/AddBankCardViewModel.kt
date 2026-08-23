package com.aozijx.passly.presentation.feature.vault.editor.bankcard

import com.aozijx.passly.feature.vault.entry.CreateEntryUseCase
import com.aozijx.passly.presentation.feature.vault.editor.common.CreateEntryViewModel
import com.aozijx.passly.feature.vault.editor.bankcard.CardNumberValidator
import com.aozijx.passly.feature.vault.editor.bankcard.ValidationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AddBankCardViewModel @Inject constructor(
    createEntryUseCase: CreateEntryUseCase,
) : CreateEntryViewModel<AddBankCardFormState>(
    initialForm = AddBankCardFormState(),
    isFormValid = AddBankCardFormState::isValid,
    saveForm = { createEntryUseCase(it.toEntryDraft()) },
    clearSensitiveForm = { AddBankCardFormState() },
) {

    fun onAction(action: AddBankCardAction) {
        when (action) {
            is AddBankCardAction.TitleChanged -> mutateForm { it.copy(title = action.value) }
            is AddBankCardAction.CardTypeChanged -> mutateForm {
                it.copy(cardType = action.value)
            }
            is AddBankCardAction.CardholderChanged -> mutateForm {
                it.copy(cardholder = action.value)
            }
            is AddBankCardAction.CardNumberChanged -> updateCardNumber(action.value)
            is AddBankCardAction.CardNumberVisibilityChanged -> mutateForm {
                it.copy(isCardNumberVisible = action.visible)
            }
            is AddBankCardAction.PaymentPinChanged -> mutateForm {
                it.copy(paymentPin = action.value)
            }
            is AddBankCardAction.PinVisibilityChanged -> mutateForm {
                it.copy(isPinVisible = action.visible)
            }
            is AddBankCardAction.CvvChanged -> mutateForm { it.copy(cardCvv = action.value) }
            is AddBankCardAction.CvvVisibilityChanged -> mutateForm {
                it.copy(isCvvVisible = action.visible)
            }
            is AddBankCardAction.ExpiryMonthChanged -> mutateForm {
                it.copy(cardExpiryMonth = action.value)
            }
            is AddBankCardAction.ExpiryYearChanged -> mutateForm {
                it.copy(cardExpiryYear = action.value)
            }
            is AddBankCardAction.TagsChanged -> mutateForm { it.copy(tags = action.value) }
            is AddBankCardAction.BillingAddressChanged -> mutateForm {
                it.copy(billingAddress = action.value)
            }
            is AddBankCardAction.NotesChanged -> mutateForm { it.copy(notes = action.value) }
            AddBankCardAction.Save -> saveEntry()
        }
    }

    private fun updateCardNumber(value: String) {
        val digits = value.filter { it.isDigit() }
        val validationResult = CardNumberValidator.validate(digits)
        mutateForm {
            it.copy(
                cardNumber = value,
                cardNumberError = (validationResult as? ValidationResult.Invalid)?.message,
                inferredNetwork = if (validationResult is ValidationResult.Valid) {
                    CardNumberValidator.inferNetwork(digits)
                } else null
            )
        }
    }
}
