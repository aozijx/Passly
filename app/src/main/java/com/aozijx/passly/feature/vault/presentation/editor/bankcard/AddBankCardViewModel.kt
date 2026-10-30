package com.aozijx.passly.feature.vault.presentation.editor.bankcard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.feature.vault.entry.CreateEntryUseCase
import com.aozijx.passly.feature.vault.detail.EditDetailEntryUseCase
import com.aozijx.passly.feature.vault.detail.EditableEntryLoadResult
import com.aozijx.passly.feature.vault.detail.LoadEditableEntryUseCase
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.policy.EntryEdit
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.feature.vault.presentation.editor.EditorWriteSession
import com.aozijx.passly.feature.vault.presentation.editor.EditorLoadPhase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddBankCardViewModel @Inject internal constructor(
    createEntryUseCase: CreateEntryUseCase,
    private val secureSessionAccessState: SecureSessionAccessState,
    private val loadEditableEntry: LoadEditableEntryUseCase,
    private val editDetailEntry: EditDetailEntryUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddBankCardUiState())
    private var editingEntryId: EntryId? = null
    private var editLoadJob: kotlinx.coroutines.Job? = null
    val uiState = _uiState.asStateFlow()
    private val writeSession = EditorWriteSession(
        scope = viewModelScope,
        access = secureSessionAccessState,
        write = { draft ->
            val entryId = editingEntryId
            if (entryId == null) createEntryUseCase(draft)
            else editDetailEntry.edit(entryId, EntryEdit.ReplaceForm(draft))
        },
        onSaving = { _uiState.value = _uiState.value.copy(savePhase = AddBankCardSavePhase.Saving) },
        onIdle = {
            _uiState.value = _uiState.value.copy(savePhase = AddBankCardSavePhase.Idle)
        },
        onClear = ::clearSensitiveContent,
    )
    internal val effects = writeSession.effects

    fun loadExistingEntry(rawEntryId: String) {
        if (editingEntryId != null) return
        val entryId = runCatching { EntryId(rawEntryId) }.getOrNull() ?: run {
            _uiState.value = _uiState.value.copy(loadPhase = EditorLoadPhase.Failed, loadFailed = true)
            return
        }
        editingEntryId = entryId
        if (!writeSession.canEdit()) {
            _uiState.value = _uiState.value.copy(loadPhase = EditorLoadPhase.Failed, loadFailed = true)
            return
        }
        val authentication = secureSessionAccessState.authenticationState.value
        _uiState.value = _uiState.value.copy(loadPhase = EditorLoadPhase.Loading)
        editLoadJob = viewModelScope.launch {
            when (val result = loadEditableEntry(entryId)) {
                is EditableEntryLoadResult.Loaded -> {
                    if (result.entry.type != EntryType.BANK_CARD || result.entry.id != entryId ||
                        authentication !== result.authentication ||
                        secureSessionAccessState.authenticationState.value !== result.authentication ||
                        !secureSessionAccessState.hasFullSecureSessionAccess()
                    ) {
                        failEditLoad()
                    } else {
                        _uiState.value = _uiState.value.copy(
                            form = _uiState.value.form.fromEntry(result.entry),
                            loadPhase = EditorLoadPhase.Ready,
                        )
                    }
                }
                else -> failEditLoad()
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
            is AddBankCardAction.CardNumberChanged -> mutateForm { it.copy(cardNumber = action.value) }
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
            is AddBankCardAction.IssuingBankChanged -> mutateForm { it.copy(issuingBank = action.value) }
            is AddBankCardAction.BankBranchChanged -> mutateForm { it.copy(bankBranch = action.value) }
            is AddBankCardAction.CardProductChanged -> mutateForm { it.copy(cardProduct = action.value) }
            is AddBankCardAction.CurrencyChanged -> mutateForm { it.copy(currency = action.value) }
            is AddBankCardAction.BankPhoneChanged -> mutateForm { it.copy(bankPhone = action.value) }
            is AddBankCardAction.NotesChanged -> mutateForm { it.copy(notes = action.value) }
            AddBankCardAction.Save -> save()
        }
    }

    private fun mutateForm(transform: (AddBankCardFormState) -> AddBankCardFormState) {
        if (!writeSession.canEdit() || !canEditForm()) return
        val current = _uiState.value
        val form = transform(current.form)
        _uiState.value = current.copy(form = form)
    }

    private fun save() {
        val current = _uiState.value
        if (!canEditForm()) return
        if (!current.canSave) return
        writeSession.save { current.form.toEntryDraft() }
    }

    private fun clearSensitiveContent() {
        editLoadJob?.cancel()
        editLoadJob = null
        _uiState.value = AddBankCardUiState(
            loadPhase = if (editingEntryId == null) EditorLoadPhase.NotRequested else EditorLoadPhase.Failed,
        )
    }

    private fun canEditForm() = editingEntryId == null || _uiState.value.loadPhase == EditorLoadPhase.Ready

    private fun failEditLoad() {
        _uiState.value = _uiState.value.copy(
            form = AddBankCardFormState(), loadPhase = EditorLoadPhase.Failed, loadFailed = true,
        )
    }

    override fun onCleared() {
        writeSession.close()
    }
}
