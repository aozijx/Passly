package com.aozijx.passly.presentation.feature.vault.editor.wifi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.feature.vault.entry.CreateEntryUseCase
import com.aozijx.passly.presentation.feature.vault.editor.EditorSaveEffect
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@HiltViewModel
class AddWifiViewModel @Inject constructor(
    private val createEntryUseCase: CreateEntryUseCase,
    private val secureSessionAccessState: SecureSessionAccessState,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddWifiUiState())
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

    fun onAction(action: AddWifiAction) {
        when (action) {
            is AddWifiAction.TitleChanged -> mutateForm { it.withTitle(action.value) }
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

    private fun mutateForm(transform: (AddWifiFormState) -> AddWifiFormState) {
        val current = _uiState.value
        if (current.isSaving) return
        _uiState.value = current.withForm(transform(current.form))
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
                        _effects.send(EditorSaveEffect.Saved)
                    }
                    is AppResult.Failure -> restoreAfterFailure(result.error.code)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                restoreAfterFailure(null)
            }
        }
    }

    private suspend fun restoreAfterFailure(message: String?) {
        val current = _uiState.value
        _uiState.value = current.withForm(current.form).copy(isSaving = false)
        _effects.send(EditorSaveEffect.Failed(message))
    }

    private fun clearSensitiveContent() {
        _uiState.value = AddWifiUiState()
    }

    override fun onCleared() {
        clearSensitiveContent()
    }
}
