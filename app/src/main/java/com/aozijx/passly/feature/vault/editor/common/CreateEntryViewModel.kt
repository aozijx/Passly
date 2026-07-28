package com.aozijx.passly.feature.vault.editor.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.domain.entry.model.VaultEntry
import com.aozijx.passly.domain.entry.repository.EntryCommandRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreateEntryUiState<Form>(
    val form: Form,
    val canSave: Boolean,
    val isSaving: Boolean = false
)

sealed interface CreateEntryEffect {
    data object Saved : CreateEntryEffect
    data class SaveFailed(val message: String?) : CreateEntryEffect
}

/**
 * 所有“新建条目”编辑器共用的保存状态机。
 *
 * 类型页面只负责字段更新、校验规则和 VaultEntry 构造，不再复制异步保存模板。
 */
abstract class CreateEntryViewModel<Form>(
    initialForm: Form,
    private val entryCommandRepository: EntryCommandRepository,
    private val isFormValid: (Form) -> Boolean,
    private val createEntry: (Form) -> VaultEntry
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        CreateEntryUiState(
            form = initialForm,
            canSave = isFormValid(initialForm)
        )
    )
    val uiState: StateFlow<CreateEntryUiState<Form>> = _uiState.asStateFlow()

    private val _effects = Channel<CreateEntryEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    protected fun mutateForm(transform: (Form) -> Form) {
        _uiState.update { current ->
            if (current.isSaving) {
                current
            } else {
                val updated = transform(current.form)
                current.copy(
                    form = updated,
                    canSave = isFormValid(updated)
                )
            }
        }
    }

    fun save() {
        val current = _uiState.value
        if (!current.canSave || current.isSaving) return

        _uiState.update { it.copy(isSaving = true, canSave = false) }
        viewModelScope.launch {
            try {
                when (
                    val result = entryCommandRepository.createEntry(
                        createEntry(current.form)
                    )
                ) {
                    is AppResult.Success -> _effects.send(CreateEntryEffect.Saved)
                    is AppResult.Failure -> restoreAfterFailure(result.error.message)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                restoreAfterFailure(error.message)
            }
        }
    }

    private suspend fun restoreAfterFailure(message: String?) {
        _uiState.update { current ->
            current.copy(
                isSaving = false,
                canSave = isFormValid(current.form)
            )
        }
        _effects.send(CreateEntryEffect.SaveFailed(message))
    }
}
