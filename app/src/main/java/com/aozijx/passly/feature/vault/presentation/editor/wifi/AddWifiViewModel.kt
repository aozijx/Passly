package com.aozijx.passly.feature.vault.presentation.editor.wifi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.feature.vault.entry.CreateEntryUseCase
import com.aozijx.passly.feature.vault.presentation.editor.EditorSaveSession
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class AddWifiViewModel @Inject constructor(
    createEntryUseCase: CreateEntryUseCase,
    secureSessionAccessState: SecureSessionAccessState,
) : ViewModel() {
    private sealed interface InitializationPhase {
        data object Pending : InitializationPhase
        data object Complete : InitializationPhase
    }

    private var initializationPhase: InitializationPhase = InitializationPhase.Pending
    private val _uiState = MutableStateFlow(AddWifiUiState())
    val uiState = _uiState.asStateFlow()
    private val saveSession = EditorSaveSession(
        scope = viewModelScope,
        access = secureSessionAccessState,
        create = createEntryUseCase::invoke,
        onSaving = { _uiState.value = _uiState.value.copy(savePhase = AddWifiSavePhase.Saving) },
        onIdle = {
            val current = _uiState.value
            _uiState.value = current.withForm(current.form).copy(savePhase = AddWifiSavePhase.Idle)
        },
        onClear = ::clearSensitiveContent,
    )
    internal val effects = saveSession.effects

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
            is AddWifiAction.ApplyScannedWifi -> mutateForm { it.applyScanned(action.payload) }
            AddWifiAction.Save -> save()
        }
    }

    fun initialize(content: AddWifiInitialContent) {
        if (initializationPhase == InitializationPhase.Complete) return
        initializationPhase = InitializationPhase.Complete
        when (content) {
            AddWifiInitialContent.Blank,
            AddWifiInitialContent.SeedUnavailable,
            -> Unit
            is AddWifiInitialContent.Scanned -> mutateForm { it.applyScanned(content.payload) }
        }
    }

    private fun mutateForm(transform: (AddWifiFormState) -> AddWifiFormState) {
        if (!saveSession.canEdit()) return
        val current = _uiState.value
        _uiState.value = current.withForm(transform(current.form))
    }

    private fun save() {
        val current = _uiState.value
        if (!current.canSave) return
        saveSession.save { current.form.toEntryDraft() }
    }

    private fun clearSensitiveContent() {
        _uiState.value = AddWifiUiState()
    }

    override fun onCleared() {
        saveSession.close()
    }
}
