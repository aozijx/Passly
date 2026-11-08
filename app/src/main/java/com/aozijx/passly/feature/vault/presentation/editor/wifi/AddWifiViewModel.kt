package com.aozijx.passly.feature.vault.presentation.editor.wifi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.policy.EntryEdit
import com.aozijx.passly.domain.entry.policy.WifiQrPayload
import com.aozijx.passly.domain.entry.port.EntryCreation
import com.aozijx.passly.feature.vault.entry.EditEntryUseCase
import com.aozijx.passly.feature.vault.entry.EntryEditorSeed
import com.aozijx.passly.feature.vault.entry.EntryEditorSeedConsumeResult
import com.aozijx.passly.feature.vault.presentation.editor.EditorLoadPhase
import com.aozijx.passly.feature.vault.presentation.editor.EditorSaveState
import com.aozijx.passly.feature.vault.presentation.editor.EditorWriteSession
import com.aozijx.passly.feature.vault.presentation.editor.EditorWriteSessionClearReason
import com.aozijx.passly.feature.vault.presentation.editor.toEditorLoadPhase
import com.aozijx.passly.feature.vault.presentation.editor.matchesCurrentAccess
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class AddWifiViewModel
    @Inject
    internal constructor(
        createEntryUseCase: EntryCreation,
        private val secureSessionAccessState: SecureSessionAccessState,
        private val editEntry: EditEntryUseCase,
    ) : ViewModel() {
        private sealed interface InitializationPhase {
            data object Pending : InitializationPhase

            data object AwaitingContent : InitializationPhase

            data object Complete : InitializationPhase
        }

        private var initializationPhase: InitializationPhase = InitializationPhase.Pending
        private var editingEntryId: EntryId? = null
        private val _uiState = MutableStateFlow(AddWifiUiState())
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
                onSaving = { _uiState.value = _uiState.value.copy(saveState = EditorSaveState.Saving) },
                onIdle = {
                    val current = _uiState.value
                    _uiState.value = current.withForm(current.form).copy(saveState = EditorSaveState.Idle)
                },
                onClear = ::clearSensitiveContent,
            )
        internal val effects = writeSession.effects

        fun beginInitialization(): AddWifiInitializationGate {
            if (editingEntryId != null || initializationPhase != InitializationPhase.Pending) {
                return AddWifiInitializationGate.AlreadyInitialized
            }
            initializationPhase = InitializationPhase.AwaitingContent
            if (!writeSession.canEdit()) {
                initializationPhase = InitializationPhase.Complete
                return AddWifiInitializationGate.Unavailable
            }
            return AddWifiInitializationGate.NeedsInitialContent
        }

        fun loadExistingEntry(result: EntryEditorSeedConsumeResult) {
            if (editingEntryId != null) return
            val seed = (result as? EntryEditorSeedConsumeResult.Consumed)?.seed as? EntryEditorSeed.Editable
            if (seed == null || !seed.matchesCurrentAccess(EntryType.WIFI, secureSessionAccessState) || !writeSession.canEdit()) {
                failEditLoad()
                return
            }
            editingEntryId = seed.entry.id
            initializationPhase = InitializationPhase.Complete
            _uiState.value =
                _uiState.value.copy(
                    form = _uiState.value.form.fromEntry(seed.entry),
                    loadPhase = EditorLoadPhase.Ready,
                )
        }

        fun onAction(action: AddWifiAction) {
            when (action) {
                is AddWifiAction.SsidChanged -> {
                    mutateForm { it.withSsid(action.value) }
                }

                is AddWifiAction.SecurityTypeChanged -> {
                    mutateForm { it.withSecurityType(action.value) }
                }

                is AddWifiAction.PasswordChanged -> {
                    mutateForm { it.copy(password = action.value) }
                }

                is AddWifiAction.PasswordVisibilityChanged -> {
                    mutateForm {
                        it.copy(isPasswordVisible = action.visible)
                    }
                }

                is AddWifiAction.HiddenChanged -> {
                    mutateForm { it.copy(isHidden = action.hidden) }
                }

                is AddWifiAction.NotesChanged -> {
                    mutateForm { it.copy(notes = action.value) }
                }

                is AddWifiAction.TagsChanged -> {
                    mutateForm { it.copy(tags = action.value) }
                }

                AddWifiAction.Save -> {
                    save()
                }
            }
        }

        fun initialize(initialContent: AddWifiInitialContent): AddWifiInitializationResult {
            if (editingEntryId != null) return AddWifiInitializationResult.AlreadyInitialized
            if (initializationPhase == InitializationPhase.Complete) return AddWifiInitializationResult.AlreadyInitialized
            initializationPhase = InitializationPhase.Complete
            if (!writeSession.canEdit()) return AddWifiInitializationResult.Unavailable
            return when (initialContent) {
                AddWifiInitialContent.Blank -> {
                    AddWifiInitializationResult.Ready
                }

                AddWifiInitialContent.SeedUnavailable -> {
                    AddWifiInitializationResult.Unavailable
                }

                is AddWifiInitialContent.Scanned -> {
                    if (tryApplyScannedWifi(initialContent.payload)) {
                        AddWifiInitializationResult.Ready
                    } else {
                        AddWifiInitializationResult.Unavailable
                    }
                }
            }
        }

        fun tryApplyScannedWifi(payload: WifiQrPayload): Boolean = mutateForm { it.applyScanned(payload) }

        private fun mutateForm(transform: (AddWifiFormState) -> AddWifiFormState): Boolean {
            if (!writeSession.canEdit() || !canEditForm()) return false
            val current = _uiState.value
            _uiState.value = current.withForm(transform(current.form))
            return true
        }

        private fun save() {
            val current = _uiState.value
            if (!canEditForm()) return
            if (!current.canSave) return
            writeSession.save { current.form.toEntryDraft() }
        }

        private fun clearSensitiveContent(reason: EditorWriteSessionClearReason) {
            _uiState.value =
                AddWifiUiState(
                    loadPhase = reason.toEditorLoadPhase(editingExisting = editingEntryId != null),
                )
        }

        private fun canEditForm() = editingEntryId == null || _uiState.value.loadPhase == EditorLoadPhase.Ready

        private fun failEditLoad() {
            _uiState.value =
                _uiState.value.copy(
                    form = AddWifiFormState(),
                    loadPhase = EditorLoadPhase.Failed,
                )
        }

        override fun onCleared() {
            writeSession.close()
        }
    }
