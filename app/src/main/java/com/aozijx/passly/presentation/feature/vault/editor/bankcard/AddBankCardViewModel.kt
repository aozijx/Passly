package com.aozijx.passly.presentation.feature.vault.editor.bankcard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.feature.vault.editor.bankcard.CardNumberValidator
import com.aozijx.passly.feature.vault.editor.bankcard.ValidationResult
import com.aozijx.passly.feature.vault.entry.CreateEntryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddBankCardViewModel @Inject constructor(
    private val createEntryUseCase: CreateEntryUseCase,
    private val secureSessionAccessState: SecureSessionAccessState,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddBankCardUiState())
    val uiState = _uiState.asStateFlow()
    private val _effects = Channel<AddBankCardEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            secureSessionAccessState.authenticationState.collect {
                if (!secureSessionAccessState.hasFullSecureSessionAccess()) clearSensitiveContent()
            }
        }
    }

    fun onAction(action: AddBankCardAction) {
        when (action) {
            is AddBankCardAction.TitleChanged -> mutateForm { it.copy(title = action.value) }
            is AddBankCardAction.CardTypeChanged -> mutateForm { it.copy(cardType = action.value) }
            is AddBankCardAction.CardholderChanged -> mutateForm { it.copy(cardholder = action.value) }
            is AddBankCardAction.CardNumberChanged -> updateCardNumber(action.value)
            is AddBankCardAction.CardNumberVisibilityChanged -> mutateForm { it.copy(isCardNumberVisible = action.visible) }
            is AddBankCardAction.PaymentPinChanged -> mutateForm { it.copy(paymentPin = action.value) }
            is AddBankCardAction.PinVisibilityChanged -> mutateForm { it.copy(isPinVisible = action.visible) }
            is AddBankCardAction.CvvChanged -> mutateForm { it.copy(cardCvv = action.value) }
            is AddBankCardAction.CvvVisibilityChanged -> mutateForm { it.copy(isCvvVisible = action.visible) }
            is AddBankCardAction.ExpiryMonthChanged -> mutateForm { it.copy(cardExpiryMonth = action.value) }
            is AddBankCardAction.ExpiryYearChanged -> mutateForm { it.copy(cardExpiryYear = action.value) }
            is AddBankCardAction.TagsChanged -> mutateForm { it.copy(tags = action.value) }
            is AddBankCardAction.BillingAddressChanged -> mutateForm { it.copy(billingAddress = action.value) }
            is AddBankCardAction.NotesChanged -> mutateForm { it.copy(notes = action.value) }
            AddBankCardAction.Save -> save()
        }
    }

    private fun mutateForm(transform: (AddBankCardFormState) -> AddBankCardFormState) {
        val current = _uiState.value
        if (current.isSaving) return
        val form = transform(current.form)
        _uiState.value = current.copy(form = form, canSave = form.isValid)
    }

    private fun updateCardNumber(value: String) {
        val digits = value.filter(Char::isDigit)
        val validationResult = CardNumberValidator.validate(digits)
        mutateForm {
            it.copy(
                cardNumber = value,
                cardNumberError = (validationResult as? ValidationResult.Invalid)?.message,
                inferredNetwork = if (validationResult is ValidationResult.Valid) {
                    CardNumberValidator.inferNetwork(digits)
                } else null,
            )
        }
    }

    private fun save() {
        val current = _uiState.value
        if (!current.canSave || current.isSaving) return
        _uiState.value = current.copy(canSave = false, isSaving = true)
        viewModelScope.launch {
            try {
                when (val result = createEntryUseCase(current.form.toEntryDraft())) {
                    is AppResult.Success -> {
                        clearSensitiveContent()
                        _effects.send(AddBankCardEffect.Saved)
                    }
                    is AppResult.Failure -> restoreAfterFailure(result.error.code)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                restoreAfterFailure("创建条目失败")
            }
        }
    }

    private suspend fun restoreAfterFailure(message: String?) {
        val form = _uiState.value.form
        _uiState.value = _uiState.value.copy(canSave = form.isValid, isSaving = false)
        _effects.send(AddBankCardEffect.SaveFailed(message))
    }

    private fun clearSensitiveContent() {
        _uiState.value = AddBankCardUiState()
    }

    override fun onCleared() {
        clearSensitiveContent()
        super.onCleared()
    }
}
