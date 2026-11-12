package com.aozijx.passly.feature.vault.presentation.editor.bankcard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.policy.EntryEdit
import com.aozijx.passly.domain.entry.port.EntryCreation
import com.aozijx.passly.feature.vault.entry.EditEntryUseCase
import com.aozijx.passly.feature.vault.entry.EntryEditorSeed
import com.aozijx.passly.feature.vault.entry.EntryEditorSeedConsumeResult
import com.aozijx.passly.feature.vault.presentation.editor.EditorLoadPhase
import com.aozijx.passly.feature.vault.presentation.editor.EditorWriteSession
import com.aozijx.passly.feature.vault.presentation.editor.EditorWriteSessionClearReason
import com.aozijx.passly.feature.vault.presentation.editor.matchesCurrentAccess
import com.aozijx.passly.feature.vault.presentation.editor.toEditorLoadPhase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class AddBankCardViewModel
    @Inject
    internal constructor(
        createEntryUseCase: EntryCreation,
        private val secureSessionAccessState: SecureSessionAccessState,
        private val editEntry: EditEntryUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(AddBankCardUiState())
        private var editingEntryId: EntryId? = null
        val uiState = _uiState.asStateFlow()
        private val writeSession =
            EditorWriteSession(
                scope = viewModelScope,
                access = secureSessionAccessState,
                write = { draft ->
                    val entryId = editingEntryId
                    if (entryId == null) {
                        createEntryUseCase(draft)
                    } else {
                        editEntry.edit(entryId, EntryEdit.ReplaceForm(draft))
                    }
                },
                onSaving = { _uiState.value = _uiState.value.copy(isSaving = true) },
                onIdle = {
                    _uiState.value = _uiState.value.copy(isSaving = false)
                },
                onClear = ::clearSensitiveContent,
            )
        internal val effects = writeSession.effects

        fun loadExistingEntry(result: EntryEditorSeedConsumeResult) {
            if (editingEntryId != null) return
            val seed = (result as? EntryEditorSeedConsumeResult.Consumed)?.seed as? EntryEditorSeed.Editable
            if (seed == null || !seed.matchesCurrentAccess(EntryType.BANK_CARD, secureSessionAccessState) || !writeSession.canEdit()) {
                failEditLoad()
                return
            }
            editingEntryId = seed.entry.id
            _uiState.value =
                _uiState.value.copy(
                    form = _uiState.value.form.fromEntry(seed.entry),
                    loadPhase = EditorLoadPhase.Ready,
                )
        }

        fun onAction(action: AddBankCardAction) {
            when (action) {
                is AddBankCardAction.TitleChanged -> {
                    mutateForm { it.copy(title = action.value) }
                }

                is AddBankCardAction.CardTypeChanged -> {
                    mutateForm {
                        it.copy(cardTypeSelection = CardTypeSelection.Selected(action.value))
                    }
                }

                is AddBankCardAction.CardholderChanged -> {
                    mutateForm { it.copy(cardholder = action.value) }
                }

                is AddBankCardAction.CardNumberChanged -> {
                    mutateForm { it.copy(cardNumber = action.value) }
                }

                is AddBankCardAction.CardNumberVisibilityChanged -> {
                    mutateForm { it.copy(isCardNumberVisible = action.visible) }
                }

                is AddBankCardAction.PaymentPinChanged -> {
                    mutateForm { it.copy(paymentPin = action.value) }
                }

                is AddBankCardAction.PinVisibilityChanged -> {
                    mutateForm { it.copy(isPinVisible = action.visible) }
                }

                is AddBankCardAction.CvvChanged -> {
                    mutateForm { it.copy(cardCvv = action.value) }
                }

                is AddBankCardAction.CvvVisibilityChanged -> {
                    mutateForm { it.copy(isCvvVisible = action.visible) }
                }

                is AddBankCardAction.ExpiryChanged -> {
                    mutateForm {
                        it.copy(cardExpirySelection = action.selection)
                    }
                }

                is AddBankCardAction.TagsChanged -> {
                    mutateForm { it.copy(tags = action.value) }
                }

                is AddBankCardAction.BillingAddressChanged -> {
                    mutateForm { it.copy(billingAddress = action.value) }
                }

                is AddBankCardAction.IssuingBankChanged -> {
                    mutateForm { it.copy(issuingBank = action.value) }
                }

                is AddBankCardAction.BankBranchChanged -> {
                    mutateForm { it.copy(bankBranch = action.value) }
                }

                is AddBankCardAction.CardProductChanged -> {
                    mutateForm { it.copy(cardProduct = action.value) }
                }

                is AddBankCardAction.CurrencyChanged -> {
                    mutateForm { it.copy(currency = action.value) }
                }

                is AddBankCardAction.BankPhoneChanged -> {
                    mutateForm { it.copy(bankPhone = action.value) }
                }

                is AddBankCardAction.NotesChanged -> {
                    mutateForm { it.copy(notes = action.value) }
                }

                AddBankCardAction.Save -> {
                    save()
                }
            }
        }

        private fun mutateForm(transform: (AddBankCardFormState) -> AddBankCardFormState) {
            if (!writeSession.canEdit()) return
            val current = _uiState.value
            if (!current.loadPhase.allowsSaving) return
            val form = transform(current.form)
            _uiState.value = current.copy(form = form)
        }

        private fun save() {
            val current = _uiState.value
            if (!current.loadPhase.allowsSaving) return
            if (!current.canSave) return
            writeSession.save { current.form.toEntryDraft() }
        }

        private fun clearSensitiveContent(reason: EditorWriteSessionClearReason) {
            _uiState.value =
                AddBankCardUiState(
                    loadPhase = reason.toEditorLoadPhase(editingExisting = editingEntryId != null),
                )
        }

        private fun failEditLoad() {
            _uiState.value =
                _uiState.value.copy(
                    form = AddBankCardFormState(),
                    loadPhase = EditorLoadPhase.Failed,
                )
        }

        override fun onCleared() {
            writeSession.close()
        }
    }
