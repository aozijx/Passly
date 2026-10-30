package com.aozijx.passly.feature.vault.presentation.editor.wifi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.policy.WifiQrPayload
import com.aozijx.passly.feature.vault.entry.CreateEntryUseCase
import com.aozijx.passly.feature.vault.detail.EditDetailEntryUseCase
import com.aozijx.passly.feature.vault.detail.EditableEntryLoadResult
import com.aozijx.passly.feature.vault.detail.LoadEditableEntryUseCase
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.policy.EntryEdit
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.feature.vault.presentation.editor.EditorSaveSession
import com.aozijx.passly.feature.vault.presentation.editor.EditorLoadPhase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class AddWifiViewModel @Inject internal constructor(
    createEntryUseCase: CreateEntryUseCase,
    private val secureSessionAccessState: SecureSessionAccessState,
    private val loadEditableEntry: LoadEditableEntryUseCase,
    private val editDetailEntry: EditDetailEntryUseCase,
) : ViewModel() {
    private sealed interface InitializationPhase {
        data object Pending : InitializationPhase
        data object Complete : InitializationPhase
    }

    private var initializationPhase: InitializationPhase = InitializationPhase.Pending
    private var editingEntryId: EntryId? = null
    private var editLoadJob: kotlinx.coroutines.Job? = null
    private val _uiState = MutableStateFlow(AddWifiUiState())
    val uiState = _uiState.asStateFlow()
    private val saveSession = EditorSaveSession(
        scope = viewModelScope,
        access = secureSessionAccessState,
        create = { draft ->
            val entryId = editingEntryId
            if (entryId == null) createEntryUseCase(draft)
            else editDetailEntry.edit(entryId, EntryEdit.ReplaceForm(draft))
        },
        onSaving = { _uiState.value = _uiState.value.copy(savePhase = AddWifiSavePhase.Saving) },
        onIdle = {
            val current = _uiState.value
            _uiState.value = current.withForm(current.form).copy(savePhase = AddWifiSavePhase.Idle)
        },
        onClear = ::clearSensitiveContent,
    )
    internal val effects = saveSession.effects

    fun loadExistingEntry(rawEntryId: String) {
        if (editingEntryId != null) return
        val entryId = runCatching { EntryId(rawEntryId) }.getOrNull() ?: run {
            _uiState.value = _uiState.value.copy(loadPhase = EditorLoadPhase.Failed, loadFailed = true)
            return
        }
        editingEntryId = entryId
        initializationPhase = InitializationPhase.Complete
        if (!saveSession.canEdit()) {
            _uiState.value = _uiState.value.copy(loadPhase = EditorLoadPhase.Failed, loadFailed = true)
            return
        }
        val authentication = secureSessionAccessState.authenticationState.value
        _uiState.value = _uiState.value.copy(loadPhase = EditorLoadPhase.Loading)
        editLoadJob = viewModelScope.launch {
            when (val result = loadEditableEntry(entryId)) {
                is EditableEntryLoadResult.Loaded -> {
                    if (result.entry.type != EntryType.WIFI || result.entry.id != entryId ||
                        authentication !== result.authentication ||
                        secureSessionAccessState.authenticationState.value !== result.authentication ||
                        !secureSessionAccessState.hasFullSecureSessionAccess()
                    ) {
                        failEditLoad()
                    } else {
                        _uiState.value = _uiState.value.withForm(
                            _uiState.value.form.fromEntry(result.entry),
                        ).copy(loadPhase = EditorLoadPhase.Ready)
                    }
                }
                else -> failEditLoad()
            }
        }
    }

    fun onAction(action: AddWifiAction) {
        when (action) {
            is AddWifiAction.SsidChanged -> mutateForm { it.withSsid(action.value) }
            is AddWifiAction.SecurityTypeChanged -> mutateForm { it.withSecurityType(action.value) }
            is AddWifiAction.PasswordChanged -> mutateForm { it.copy(password = action.value) }
            is AddWifiAction.PasswordVisibilityChanged -> mutateForm {
                it.copy(isPasswordVisible = action.visible)
            }
            is AddWifiAction.HiddenChanged -> mutateForm { it.copy(isHidden = action.hidden) }
            is AddWifiAction.NotesChanged -> mutateForm { it.copy(notes = action.value) }
            is AddWifiAction.TagsChanged -> mutateForm { it.copy(tags = action.value) }
            AddWifiAction.Save -> save()
        }
    }

    fun initialize(loadContent: () -> AddWifiInitialContent): AddWifiInitializationResult {
        if (editingEntryId != null) return AddWifiInitializationResult.AlreadyInitialized
        if (initializationPhase == InitializationPhase.Complete) return AddWifiInitializationResult.AlreadyInitialized
        initializationPhase = InitializationPhase.Complete
        if (!saveSession.canEdit()) return AddWifiInitializationResult.Unavailable
        return when (val content = loadContent()) {
            AddWifiInitialContent.Blank -> AddWifiInitializationResult.Ready
            AddWifiInitialContent.SeedUnavailable -> AddWifiInitializationResult.Unavailable
            is AddWifiInitialContent.Scanned -> if (tryApplyScannedWifi(content.payload)) {
                AddWifiInitializationResult.Ready
            } else AddWifiInitializationResult.Unavailable
        }
    }

    fun tryApplyScannedWifi(payload: WifiQrPayload): Boolean =
        mutateForm { it.applyScanned(payload) }

    private fun mutateForm(transform: (AddWifiFormState) -> AddWifiFormState): Boolean {
        if (!saveSession.canEdit() || !canEditForm()) return false
        val current = _uiState.value
        _uiState.value = current.withForm(transform(current.form))
        return true
    }

    private fun save() {
        val current = _uiState.value
        if (!canEditForm()) return
        if (!current.canSave) return
        saveSession.save { current.form.toEntryDraft() }
    }

    private fun clearSensitiveContent() {
        editLoadJob?.cancel()
        editLoadJob = null
        _uiState.value = AddWifiUiState(
            loadPhase = if (editingEntryId == null) EditorLoadPhase.NotRequested else EditorLoadPhase.Failed,
        )
    }

    private fun canEditForm() = editingEntryId == null || _uiState.value.loadPhase == EditorLoadPhase.Ready

    private fun failEditLoad() {
        _uiState.value = _uiState.value.copy(
            form = AddWifiFormState(), loadPhase = EditorLoadPhase.Failed, loadFailed = true,
        )
    }

    override fun onCleared() {
        saveSession.close()
    }
}
