package com.aozijx.passly.presentation.feature.vault.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.presentation.feature.common.error.toUiMessage
import com.aozijx.passly.core.error.model.SessionModeRestricted
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.port.EntryListQueryRepository
import com.aozijx.passly.feature.vault.trash.TrashCommandResult
import com.aozijx.passly.feature.vault.trash.TrashCommandUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

@HiltViewModel
internal class TrashViewModel @Inject constructor(
    private val entryListQueryRepository: EntryListQueryRepository,
    private val trashCommand: TrashCommandUseCase,
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
                trashCommand.restore(
                    EntryId(action.entryId),
                    EntryVersion(action.expectedVersion),
                )
            }
            is TrashUiAction.DeletePermanently -> runEntryAction(action.entryId) {
                trashCommand.deletePermanently(
                    EntryId(action.entryId),
                    EntryVersion(action.expectedVersion),
                )
            }
            TrashUiAction.Empty -> emptyTrash()
            TrashUiAction.ClearError -> mutate(TrashMutation.ErrorCleared)
        }
    }

    private fun runEntryAction(
        entryId: String,
        operation: suspend () -> TrashCommandResult,
    ) {
        if (_uiState.value.isBusy) return
        viewModelScope.launch {
            mutate(TrashMutation.EntryActionStarted(entryId))
            try {
                applyResult(operation(), "回收站操作失败")
            } finally {
                mutate(TrashMutation.EntryActionFinished)
            }
        }
    }

    private fun emptyTrash() {
        if (_uiState.value.isBusy || _uiState.value.entries.isEmpty()) return
        viewModelScope.launch {
            mutate(TrashMutation.EmptyStarted)
            try {
                applyResult(trashCommand.empty(), "无法清空回收站")
            } finally {
                mutate(TrashMutation.EmptyFinished)
            }
        }
    }

    private fun applyResult(result: TrashCommandResult, fallback: String) {
        when (result) {
            TrashCommandResult.Completed,
            TrashCommandResult.NotAuthorized -> Unit

            is TrashCommandResult.Failed -> {
                val message = if (result.error is SessionModeRestricted) {
                    "当前会话不能操作回收站"
                } else {
                    result.error.toUiMessage(fallback)
                }
                mutate(TrashMutation.ActionFailed(message))
            }
        }
    }

    private fun mutate(mutation: TrashMutation) {
        _uiState.value = TrashReducer.reduce(_uiState.value, mutation)
    }
}
