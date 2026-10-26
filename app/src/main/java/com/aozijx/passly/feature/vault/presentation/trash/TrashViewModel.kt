package com.aozijx.passly.feature.vault.presentation.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.presentation.shared.error.toUiMessage
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

@HiltViewModel
internal class TrashViewModel @Inject constructor(
    private val entryListQueryRepository: EntryListQueryRepository,
    private val trashCommand: TrashCommandUseCase,
) : ViewModel() {
    private sealed interface Observation {
        data object Pending : Observation
        class Active(val ticket: Any, val job: Job) : Observation
        data object Destroyed : Observation
    }

    private var observation: Observation = Observation.Pending
    private val _uiState = MutableStateFlow(TrashUiState())
    val uiState: StateFlow<TrashUiState> = _uiState.asStateFlow()

    init {
        observeTrash()
    }

    private fun observeTrash() {
        if (observation == Observation.Destroyed) return
        val previous = observation
        observation = Observation.Pending
        if (previous is Observation.Active) previous.job.cancel()
        mutate(TrashMutation.LoadStarted)
        val ticket = Any()
        val job = viewModelScope.launch(start = CoroutineStart.LAZY) {
            entryListQueryRepository.deletedEntries
                .catch {
                    currentCoroutineContext().ensureActive()
                    if (isCurrentObservation(ticket)) mutate(TrashMutation.LoadFailed(it.toUiMessage("无法读取回收站")))
                }
                .collect {
                    currentCoroutineContext().ensureActive()
                    if (isCurrentObservation(ticket)) mutate(TrashMutation.Loaded(it))
                }
        }
        observation = Observation.Active(ticket, job)
        job.start()
    }

    private fun isCurrentObservation(ticket: Any): Boolean =
        (observation as? Observation.Active)?.ticket === ticket

    fun onAction(action: TrashUiAction) {
        if (observation == Observation.Destroyed) return
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
            TrashUiAction.RetryLoad -> if (uiState.value.content is TrashContentState.Failed) observeTrash()
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
        val entries = (_uiState.value.content as? TrashContentState.Loaded)?.entries.orEmpty()
        if (_uiState.value.isBusy || entries.isEmpty()) return
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
        if (observation == Observation.Destroyed) return
        _uiState.value = TrashReducer.reduce(_uiState.value, mutation)
    }

    override fun onCleared() {
        val previous = observation
        observation = Observation.Destroyed
        if (previous is Observation.Active) previous.job.cancel()
        _uiState.value = TrashUiState()
    }
}
