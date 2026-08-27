package com.aozijx.passly.presentation.feature.vault.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.app.message.mapping.toUiMessage
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.port.EntryCommandRepository
import com.aozijx.passly.domain.entry.port.EntryListQueryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

@HiltViewModel
class TrashViewModel @Inject constructor(
    private val entryListQueryRepository: EntryListQueryRepository,
    private val entryCommandRepository: EntryCommandRepository,
    private val secureSessionAccessState: SecureSessionAccessState,
) : ViewModel() {
    private val _uiState = MutableStateFlow(TrashUiState())
    val uiState: StateFlow<TrashUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            entryListQueryRepository.deletedEntries
                .catch { mutate(TrashMutation.LoadFailed(it.toUiMessage("无法读取回收站"))) }
                .collect { mutate(TrashMutation.Loaded(it)) }
        }
    }

    fun onAction(action: TrashUiAction) {
        when (action) {
            is TrashUiAction.Restore -> runEntryAction(action.entryId) {
                entryCommandRepository.restoreEntry(EntryId(action.entryId), EntryVersion(action.expectedVersion))
            }
            is TrashUiAction.DeletePermanently -> runEntryAction(action.entryId) {
                entryCommandRepository.deletePermanently(EntryId(action.entryId), EntryVersion(action.expectedVersion))
            }
            TrashUiAction.Empty -> emptyTrash()
            TrashUiAction.ClearError -> mutate(TrashMutation.ErrorCleared)
        }
    }

    private fun runEntryAction(entryId: String, operation: suspend () -> AppResult<Unit>) {
        if (_uiState.value.isBusy || !requireAccess()) return
        viewModelScope.launch {
            if (!requireAccess()) return@launch
            mutate(TrashMutation.EntryActionStarted(entryId))
            try {
                operation().updateError("回收站操作失败")
            } finally {
                mutate(TrashMutation.EntryActionFinished)
            }
        }
    }

    private fun emptyTrash() {
        if (_uiState.value.isBusy || _uiState.value.entries.isEmpty() || !requireAccess()) return
        viewModelScope.launch {
            if (!requireAccess()) return@launch
            mutate(TrashMutation.EmptyStarted)
            try {
                entryCommandRepository.emptyTrash().updateError("无法清空回收站")
            } finally {
                mutate(TrashMutation.EmptyFinished)
            }
        }
    }

    private fun requireAccess(): Boolean {
        if (secureSessionAccessState.hasFullSecureSessionAccess()) return true
        mutate(TrashMutation.ActionFailed("当前会话不能操作回收站"))
        return false
    }

    private fun AppResult<*>.updateError(fallback: String) {
        if (this is AppResult.Failure) mutate(TrashMutation.ActionFailed(error.toUiMessage(fallback)))
    }

    private fun mutate(mutation: TrashMutation) {
        _uiState.value = TrashReducer.reduce(_uiState.value, mutation)
    }
}
