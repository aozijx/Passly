package com.aozijx.passly.feature.vault.editor.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.authentication.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.EntryAggregate
import com.aozijx.passly.domain.entry.repository.EntryCommandRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 所有“新建条目”编辑器共用的保存状态机。
 *
 * 类型页面只负责字段更新、校验规则和 EntryAggregate 构造，不再复制异步保存模板。
 */
abstract class CreateEntryViewModel<Form>(
    initialForm: Form,
    private val entryCommandRepository: EntryCommandRepository,
    private val vaultAccessState: SecureSessionAccessState,
    private val isFormValid: (Form) -> Boolean,
    private val createEntry: (Form) -> EntryAggregate
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
        if (!vaultAccessState.hasFullSecureSessionAccess()) {
            _effects.trySend(CreateEntryEffect.SaveFailed("当前会话不能新建条目"))
            return
        }

        _uiState.update { it.copy(isSaving = true, canSave = false) }
        viewModelScope.launch {
            if (!vaultAccessState.hasFullSecureSessionAccess()) {
                restoreAfterFailure("当前会话不能新建条目")
                return@launch
            }
            try {
                when (
                    val result = entryCommandRepository.createEntry(
                        createEntry(current.form)
                    )
                ) {
                    is AppResult.Success -> _effects.send(CreateEntryEffect.Saved)
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
        _uiState.update { current ->
            current.copy(
                isSaving = false,
                canSave = isFormValid(current.form)
            )
        }
        _effects.send(CreateEntryEffect.SaveFailed(message))
    }
}
