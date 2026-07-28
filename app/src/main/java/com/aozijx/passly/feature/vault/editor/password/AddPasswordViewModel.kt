package com.aozijx.passly.feature.vault.editor.password

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.domain.entry.repository.EntryCommandRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddPasswordViewModel @Inject constructor(
    private val entryCommandRepository: EntryCommandRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddPasswordUiState())
    val uiState: StateFlow<AddPasswordUiState> = _uiState.asStateFlow()

    private val _effects = Channel<AddPasswordEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun updateTitle(value: String) = update { copy(title = value) }
    fun updateUsername(value: String) = update { copy(username = value) }
    fun updatePassword(value: String) = update { copy(password = value) }
    fun updateWebsite(value: String) = update { copy(website = value) }
    fun updateNotes(value: String) = update { copy(notes = value) }
    fun setPasswordVisible(visible: Boolean) = update { copy(isPasswordVisible = visible) }

    fun save() {
        val current = _uiState.value
        if (!current.canSave) return

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                when (
                    val result = entryCommandRepository.createEntry(
                        PasswordEntryFactory.create(current)
                    )
                ) {
                    is AppResult.Success -> _effects.send(AddPasswordEffect.Saved)
                    is AppResult.Failure -> {
                        _uiState.update { it.copy(isSaving = false) }
                        _effects.send(AddPasswordEffect.SaveFailed(result.error.message))
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                _uiState.update { it.copy(isSaving = false) }
                _effects.send(AddPasswordEffect.SaveFailed(error.message))
            }
        }
    }

    override fun onCleared() {
        _uiState.value = AddPasswordUiState()
        super.onCleared()
    }

    private inline fun update(transform: AddPasswordUiState.() -> AddPasswordUiState) {
        _uiState.update { current ->
            if (current.isSaving) current else current.transform()
        }
    }

}
