package com.aozijx.passly.presentation.feature.vault.editor.password

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
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
class AddPasswordViewModel @Inject constructor(
    private val createEntryUseCase: CreateEntryUseCase,
    private val secureSessionAccessState: SecureSessionAccessState,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AddPasswordUiState())
    val uiState = _uiState.asStateFlow()
    private val _effects = Channel<AddPasswordEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            secureSessionAccessState.authenticationState.collect {
                if (!secureSessionAccessState.hasFullSecureSessionAccess()) clearSensitiveContent()
            }
        }
    }

    fun onAction(action: AddPasswordAction) {
        when (action) {
            is AddPasswordAction.TitleChanged -> mutateForm { it.copy(title = action.value) }
            is AddPasswordAction.UsernameChanged -> mutateForm { it.copy(username = action.value) }
            is AddPasswordAction.PasswordChanged -> mutateForm { it.copy(password = action.value) }
            is AddPasswordAction.PasswordVisibilityChanged -> mutateForm { it.copy(isPasswordVisible = action.visible) }
            is AddPasswordAction.WebsiteChanged -> mutateForm { it.copy(website = action.value) }
            is AddPasswordAction.NotesChanged -> mutateForm { it.copy(notes = action.value) }
            is AddPasswordAction.TagsChanged -> mutateForm { it.copy(tags = action.value) }
            AddPasswordAction.Save -> save()
        }
    }

    private fun mutateForm(transform: (AddPasswordFormState) -> AddPasswordFormState) {
        val current = _uiState.value
        if (current.isSaving) return
        val form = transform(current.form)
        _uiState.value = current.copy(form = form, canSave = form.isValid)
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
                        _effects.send(AddPasswordEffect.Saved)
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
        _effects.send(AddPasswordEffect.SaveFailed(message))
    }

    private fun clearSensitiveContent() {
        _uiState.value = AddPasswordUiState()
    }

    override fun onCleared() {
        clearSensitiveContent()
        super.onCleared()
    }
}
