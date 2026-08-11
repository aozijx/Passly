package com.aozijx.passly.feature.vault.editor.bankcard

import com.aozijx.passly.domain.authentication.SecureSessionAccessState
import com.aozijx.passly.domain.entry.repository.EntryCommandRepository
import com.aozijx.passly.feature.vault.editor.common.CreateEntryViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AddBankCardViewModel @Inject constructor(
    entryCommandRepository: EntryCommandRepository,
    vaultAccessState: SecureSessionAccessState
) : CreateEntryViewModel<AddBankCardFormState>(
    initialForm = AddBankCardFormState(),
    entryCommandRepository = entryCommandRepository,
    vaultAccessState = vaultAccessState,
    isFormValid = AddBankCardFormState::isValid,
    createEntry = { BankCardEntryFactory.create(it) }
) {

    fun onField(transform: (AddBankCardFormState) -> AddBankCardFormState) =
        mutateForm(transform)

    fun updateCardNumber(value: String) {
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