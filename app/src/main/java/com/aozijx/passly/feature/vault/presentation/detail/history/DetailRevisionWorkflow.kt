package com.aozijx.passly.feature.vault.presentation.detail.history

import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.history.EntryRevisionId
import com.aozijx.passly.domain.entry.model.history.RevisionDifferenceKind
import com.aozijx.passly.domain.entry.model.history.RevisionFieldId
import com.aozijx.passly.domain.entry.model.sensitive.SensitiveFieldKey
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.feature.vault.history.CompareEntryRevisionUseCase
import com.aozijx.passly.feature.vault.history.ObserveEntryRevisionMetadataUseCase
import com.aozijx.passly.feature.vault.history.RestoreEntryRevisionUseCase
import com.aozijx.passly.feature.vault.history.RevealRevisionFieldsUseCase
import com.aozijx.passly.feature.vault.history.RevisionOperationResult
import com.aozijx.passly.feature.vault.presentation.detail.DetailWorkSession
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Owns revision observation/operation jobs and historical plaintext for one Detail page visit. */
internal class DetailRevisionWorkflowFactory
    @Inject
    constructor(
        private val observeMetadata: ObserveEntryRevisionMetadataUseCase,
        private val compareRevision: CompareEntryRevisionUseCase,
        private val revealFields: RevealRevisionFieldsUseCase,
        private val restoreRevision: RestoreEntryRevisionUseCase,
    ) {
        fun create(
            scope: CoroutineScope,
            workSession: DetailWorkSession,
            currentState: () -> DetailRevisionState,
            publishMutation: (DetailRevisionMutation) -> Unit,
            onRestoreSucceeded: (EntryId, DetailWorkSession.Scope.Active) -> Unit,
        ): DetailRevisionWorkflow =
            DetailRevisionWorkflow(
                scope = scope,
                workSession = workSession,
                currentState = currentState,
                publishMutation = publishMutation,
                onRestoreSucceeded = onRestoreSucceeded,
                observeMetadata = observeMetadata,
                compareRevision = compareRevision,
                revealFields = revealFields,
                restoreRevision = restoreRevision,
            )
    }

internal class DetailRevisionWorkflow internal constructor(
    private val scope: CoroutineScope,
    private val workSession: DetailWorkSession,
    private val currentState: () -> DetailRevisionState,
    private val publishMutation: (DetailRevisionMutation) -> Unit,
    private val onRestoreSucceeded: (EntryId, DetailWorkSession.Scope.Active) -> Unit,
    private val observeMetadata: ObserveEntryRevisionMetadataUseCase,
    private val compareRevision: CompareEntryRevisionUseCase,
    private val revealFields: RevealRevisionFieldsUseCase,
    private val restoreRevision: RestoreEntryRevisionUseCase,
) {
    private val revealStore = DetailRevisionRevealStore()
    private val _revealedFields = MutableStateFlow(emptyMap<SensitiveFieldKey, SensitiveValue>())
    val revealedFields: StateFlow<Map<SensitiveFieldKey, SensitiveValue>> = _revealedFields.asStateFlow()

    private var observationJob: Job? = null
    private var operationJob: Job? = null
    private var observationGeneration = 0L

    fun open(entryId: EntryId) {
        if ((currentState() as? DetailRevisionState.Open)?.entryId == entryId) return
        val token = workSession.current(entryId) ?: return
        cancelObservation()
        cancelOperation()
        clearReveals()
        publishMutation(DetailRevisionMutation.Opened(entryId))
        observeHistory(entryId, token)
    }

    fun retry() {
        val state = currentState() as? DetailRevisionState.Open ?: return
        if (state.page != DetailRevisionPage.List || state.revisions !is DetailRevisionListState.Failed) return
        val token = workSession.current(state.entryId) ?: return
        cancelObservation()
        publishMutation(DetailRevisionMutation.Opened(state.entryId))
        observeHistory(state.entryId, token)
    }

    fun back() {
        if (DetailRevisionReducer.isRestoreInProgress(currentState())) return
        cancelOperation()
        clearReveals()
        publishMutation(DetailRevisionMutation.BackToList)
    }

    fun requestRestore() = publishMutation(DetailRevisionMutation.RestoreRequested)

    fun cancelRestore() = publishMutation(DetailRevisionMutation.RestoreCancelled)

    fun close() {
        if (DetailRevisionReducer.isRestoreInProgress(currentState())) return
        cancelObservation()
        cancelOperation()
        clearReveals()
        publishMutation(DetailRevisionMutation.Closed)
    }

    fun select(rawRevisionId: String) {
        val state = currentState() as? DetailRevisionState.Open ?: return
        if (DetailRevisionReducer.isRestoreInProgress(state)) return
        val token = workSession.current(state.entryId) ?: return
        val revisionId = EntryRevisionId(rawRevisionId)
        cancelOperation()
        clearReveals()
        publishMutation(DetailRevisionMutation.ComparisonStarted(state.entryId.value, rawRevisionId))
        operationJob =
            scope.launch {
                val result = compareRevision(state.entryId, revisionId)
                if (!workSession.isCurrent(token) || !currentCoroutineContext().isActive) return@launch
                when (result) {
                    is RevisionOperationResult.Succeeded -> {
                        publishMutation(
                            DetailRevisionMutation.ComparisonLoaded(
                                state.entryId.value,
                                rawRevisionId,
                                result.value,
                            ),
                        )
                    }

                    else -> publishFailure(state.entryId, rawRevisionId, result)
                }
            }
    }

    fun reveal(key: SensitiveFieldKey) {
        val state = currentState() as? DetailRevisionState.Open ?: return
        val comparison = state.page as? DetailRevisionPage.Comparison ?: return
        val ready = comparison.phase as? DetailRevisionComparisonPhase.Ready ?: return
        if (ready.restore != DetailRevisionRestorePhase.Idle ||
            ready.differences.none {
                it.field == RevisionFieldId.Sensitive(key) &&
                    it.kind in setOf(RevisionDifferenceKind.CHANGED, RevisionDifferenceKind.REMOVED)
            }
        ) {
            return
        }
        val token = workSession.current(state.entryId) ?: return
        val revisionId = EntryRevisionId(comparison.revisionId)
        cancelOperation()
        operationJob =
            scope.launch {
                val result = revealFields(state.entryId, revisionId, setOf(key))
                val currentPage = (currentState() as? DetailRevisionState.Open)?.page
                if (!workSession.isCurrent(token) || currentPage !== comparison || !currentCoroutineContext().isActive) {
                    if (result is RevisionOperationResult.Succeeded) result.value.forEach { it.value.wipe() }
                    return@launch
                }
                when (result) {
                    is RevisionOperationResult.Succeeded -> {
                        workSession.publishReveals(token, result.value.map { it.key to it.value }, revealStore::replace)
                        if (workSession.isCurrent(token)) publishRevealsSnapshot()
                    }

                    RevisionOperationResult.Cancelled -> Unit
                    else -> publishFailure(state.entryId, revisionId.value, result)
                }
            }
    }

    fun restore() {
        val state = currentState() as? DetailRevisionState.Open ?: return
        val comparison = state.page as? DetailRevisionPage.Comparison ?: return
        val ready = comparison.phase as? DetailRevisionComparisonPhase.Ready ?: return
        if (ready.restore != DetailRevisionRestorePhase.Confirming) return
        val token = workSession.current(state.entryId) ?: return
        val revisionId = EntryRevisionId(comparison.revisionId)
        cancelOperation()
        publishMutation(DetailRevisionMutation.RestoreStarted)
        operationJob =
            scope.launch {
                val result = restoreRevision(state.entryId, revisionId)
                if (!workSession.isCurrent(token) || !currentCoroutineContext().isActive) return@launch
                when (result) {
                    is RevisionOperationResult.Succeeded -> {
                        clearReveals()
                        publishMutation(DetailRevisionMutation.RestoreSucceeded)
                        onRestoreSucceeded(state.entryId, token)
                    }

                    RevisionOperationResult.Cancelled -> publishMutation(DetailRevisionMutation.RestoreAborted)
                    else -> publishFailure(state.entryId, revisionId.value, result)
                }
            }
    }

    /** Invalidates every outstanding result when the page entry or secure session is replaced. */
    fun reset() {
        cancelObservation()
        cancelOperation()
        clearReveals()
    }

    private fun observeHistory(
        entryId: EntryId,
        token: DetailWorkSession.Scope.Active,
    ) {
        val generation = ++observationGeneration
        observationJob =
            scope.launch {
                try {
                    val metadata =
                        when (val observation = observeMetadata(entryId)) {
                            is com.aozijx.passly.core.error.result.AppResult.Failure -> {
                                if (!workSession.isCurrent(token) || generation != observationGeneration) return@launch
                                publishFailure(entryId, null, RevisionOperationResult.Failed(observation.error))
                                return@launch
                            }

                            is com.aozijx.passly.core.error.result.AppResult.Success -> observation.data
                        }
                    metadata.collect { revisions ->
                        if (!workSession.isCurrent(token) || generation != observationGeneration) return@collect
                        publishMutation(DetailRevisionMutation.MetadataChanged(entryId.value, revisions))
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: com.aozijx.passly.runtime.session.SessionLockedException) {
                    // Session closure invalidates this visit and clears revision state.
                } catch (_: Exception) {
                    if (workSession.isCurrent(token) && generation == observationGeneration) {
                        publishMutation(
                            DetailRevisionMutation.Failed(
                                entryId.value,
                                null,
                                DetailRevisionFailure.UNEXPECTED,
                            ),
                        )
                    }
                }
            }
    }

    private fun publishFailure(
        entryId: EntryId,
        revisionId: String?,
        result: RevisionOperationResult<*>,
    ) {
        val failure =
            when (result) {
                RevisionOperationResult.Missing -> DetailRevisionFailure.MISSING
                RevisionOperationResult.Stale -> DetailRevisionFailure.STALE
                RevisionOperationResult.InvalidSnapshot -> DetailRevisionFailure.INVALID
                is RevisionOperationResult.AuthenticationDenied -> DetailRevisionFailure.AUTHENTICATION
                else -> DetailRevisionFailure.UNEXPECTED
            }
        publishMutation(DetailRevisionMutation.Failed(entryId.value, revisionId, failure))
    }

    private fun cancelObservation() {
        observationGeneration++
        observationJob?.cancel()
        observationJob = null
    }

    private fun cancelOperation() {
        operationJob?.cancel()
        operationJob = null
    }

    private fun clearReveals() {
        revealStore.clear()
        publishRevealsSnapshot()
    }

    private fun publishRevealsSnapshot() {
        val snapshot = revealStore.snapshot()
        _revealedFields.value = snapshot
    }
}
