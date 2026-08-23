package com.aozijx.passly.presentation.feature.vault.editor.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.entry.model.EntryId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * 所有“新建条目”编辑器共用的保存状态机。
 *
 * 类型页面只负责字段更新、校验规则和 Entry 构造，不再复制异步保存模板。
 */
abstract class CreateEntryViewModel<Form>(
    initialForm: Form,
    private val isFormValid: (Form) -> Boolean,
    private val saveForm: suspend (Form) -> AppResult<EntryId>,
    private val clearSensitiveForm: (Form) -> Form,
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
        val current = _uiState.value
        if (current.isSaving) return
        val updated = transform(current.form)
        mutate(CreateEntryMutation.FormChanged(updated, isFormValid(updated)))
    }

    protected fun saveEntry() {
        val current = _uiState.value
        if (!current.canSave || current.isSaving) return
        mutate(CreateEntryMutation.SaveStarted)
        viewModelScope.launch {
            try {
                when (val result = saveForm(current.form)) {
                    is AppResult.Success -> {
                        _uiState.value = CreateEntryUiState(
                            form = clearSensitiveForm(_uiState.value.form),
                            canSave = false,
                        )
                        _effects.send(CreateEntryEffect.Saved)
                    }
                    is AppResult.Failure -> restoreAfterFailure(result.error.code)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                restoreAfterFailure("创建条目失败")
            }
        }
    }

    private suspend fun restoreAfterFailure(message: String?) {
        mutate(CreateEntryMutation.SaveFailed(isFormValid(_uiState.value.form)))
        _effects.send(CreateEntryEffect.SaveFailed(message))
    }

    private fun mutate(mutation: CreateEntryMutation<Form>) {
        _uiState.value = CreateEntryReducer.reduce(_uiState.value, mutation)
    }

    override fun onCleared() {
        _uiState.value = CreateEntryUiState(
            form = clearSensitiveForm(_uiState.value.form),
            canSave = false,
        )
        super.onCleared()
    }
}
