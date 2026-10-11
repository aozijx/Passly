package com.aozijx.passly.presentation.feature.vault.editor.bankcard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.feature.vault.editor.bankcard.CardNumberValidator
import com.aozijx.passly.feature.vault.entry.CreateEntryUseCase
import com.aozijx.passly.presentation.feature.vault.editor.EditorSaveEffect
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
    private val _effects = Channel<EditorSaveEffect>(Channel.BUFFERED)
    internal val effects = _effects.receiveAsFlow()

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
            is AddBankCardAction.CardTypeChanged -> mutateForm {
                it.copy(cardType = CardTypeSelection.Selected(action.value))
            }
            is AddBankCardAction.CardholderChanged -> mutateForm { it.copy(cardholder = action.value) }
            is AddBankCardAction.CardNumberChanged -> updateCardNumber(action.value)
            is AddBankCardAction.CardNumberVisibilityChanged -> mutateForm { it.copy(isCardNumberVisible = action.visible) }
            is AddBankCardAction.PaymentPinChanged -> mutateForm { it.copy(paymentPin = action.value) }
            is AddBankCardAction.PinVisibilityChanged -> mutateForm { it.copy(isPinVisible = action.visible) }
            is AddBankCardAction.CvvChanged -> mutateForm { it.copy(cardCvv = action.value) }
            is AddBankCardAction.CvvVisibilityChanged -> mutateForm { it.copy(isCvvVisible = action.visible) }
            is AddBankCardAction.ExpiryChanged -> mutateForm {
                it.copy(cardExpiry = action.value)
            }
            is AddBankCardAction.TagsChanged -> mutateForm { it.copy(tags = action.value) }
            is AddBankCardAction.BillingAddressChanged -> mutateForm { it.copy(billingAddress = action.value) }
            is AddBankCardAction.NotesChanged -> mutateForm { it.copy(notes = action.value) }
            AddBankCardAction.Save -> save()
        }
    }

    private fun mutateForm(transform: (AddBankCardFormState) -> AddBankCardFormState) {
        val current = _uiState.value
        if (current.savePhase == AddBankCardSavePhase.Saving) return
        val form = transform(current.form)
        _uiState.value = current.copy(form = form)
    }

    private fun updateCardNumber(value: String) {
        val digits = value.filter(Char::isDigit)
        mutateForm {
            it.copy(
                cardNumber = value,
                cardNumberValidation = CardNumberValidator.validate(digits),
            )
        }
    }

    private fun save() {
        val current = _uiState.value
        if (!current.canSave) return
        _uiState.value = current.copy(savePhase = AddBankCardSavePhase.Saving)
        viewModelScope.launch {
            try {
                when (val result = createEntryUseCase(current.form.toEntryDraft())) {
                    is AppResult.Success -> {
                        clearSensitiveContent()
                        _effects.send(EditorSaveEffect.Saved)
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
        _uiState.value = _uiState.value.copy(savePhase = AddBankCardSavePhase.Idle)
        _effects.send(EditorSaveEffect.Failed(message))
    }

    private fun clearSensitiveContent() {
        _uiState.value = AddBankCardUiState()
    }

    override fun onCleared() {
        clearSensitiveContent()
    }
}
