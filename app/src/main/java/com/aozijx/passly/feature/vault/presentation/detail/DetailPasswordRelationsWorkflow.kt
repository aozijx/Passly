package com.aozijx.passly.feature.vault.presentation.detail

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.core.observability.ErrorCodes
import com.aozijx.passly.core.observability.Events
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.Reasons
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.relation.EntryLinkId
import com.aozijx.passly.domain.entry.model.relation.RelatedPasswordEntry
import com.aozijx.passly.domain.entry.port.RelatedPasswordOperations
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Owns related-password loading and mutations for the current Detail page. */
internal class DetailPasswordRelationsWorkflow(
    private val scope: CoroutineScope,
    private val operations: RelatedPasswordOperations,
    private val access: SecureSessionAccessState,
    private val workSession: DetailWorkSession,
    private val currentEntry: () -> Entry?,
    private val currentBindingState: () -> RelatedPasswordBindingUiState,
    private val currentRelatedPasswords: () -> List<RelatedPasswordEntry>,
    private val currentUnlinkState: () -> RelatedPasswordUnlinkUiState,
    private val presentationLoader: DetailPresentationLoader,
    private val logger: Logger,
    private val publish: (DetailMutation) -> Unit,
) {
    private var candidateJob: Job? = null
    private var candidateLoadEntryId: EntryId? = null
    private var candidateLoadGeneration = 0L
    private var candidateCache: CandidateCache? = null
    private var linkMutationJob: Job? = null

    private data class CandidateCache(
        val sourceEntryId: EntryId,
        val candidates: List<RelatedPasswordCandidateUiModel>,
    )

    fun open() {
        if (linkMutationJob?.isActive == true) return
        val entry = currentEntry()?.takeIf { it.type == EntryType.LOGIN && it.deletedAt == null } ?: return
        val token = workSession.current(entry.id) ?: return
        val authentication = access.authenticationState.value
        if (!isCurrent(entry.id, token, authentication)) return
        candidateCache
            ?.takeIf { it.sourceEntryId == entry.id }
            ?.let { cache ->
                publish(bindingState(RelatedPasswordBindingUiState.Ready(cache.candidates)))
                return
            }

        publish(bindingState(RelatedPasswordBindingUiState.Loading))
        if (candidateJob?.isActive == true && candidateLoadEntryId == entry.id) return

        candidateJob?.cancel()
        val generation = ++candidateLoadGeneration
        candidateLoadEntryId = entry.id
        candidateJob =
            scope.launch {
                try {
                    val candidates = operations.candidates(entry.id).map { it.toCandidateUiModel() }
                    if (isCurrent(entry.id, token, authentication)) {
                        candidateCache = CandidateCache(entry.id, candidates)
                        if (currentBindingState() == RelatedPasswordBindingUiState.Loading) {
                            publish(bindingState(RelatedPasswordBindingUiState.Ready(candidates)))
                        }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    if (isCurrent(entry.id, token, authentication) &&
                        currentBindingState() == RelatedPasswordBindingUiState.Loading
                    ) {
                        publish(bindingState(RelatedPasswordBindingUiState.LoadFailed))
                    }
                } finally {
                    if (candidateLoadGeneration == generation) {
                        candidateJob = null
                        candidateLoadEntryId = null
                    }
                }
            }
    }

    fun dismiss() {
        publish(bindingState(RelatedPasswordBindingUiState.Closed))
    }

    fun bind(targetEntryId: EntryId) {
        val entry = activeCurrentEntry() ?: return
        val picker = currentBindingState() as? RelatedPasswordBindingUiState.Ready ?: return
        if (picker.operation is RelatedPasswordBindingOperationState.Saving ||
            picker.candidates.none { it.id == targetEntryId.value }
        ) {
            return
        }
        val token = workSession.current(entry.id) ?: return
        val authentication = access.authenticationState.value
        if (linkMutationJob?.isActive == true) return
        cancelCandidateLoad()
        publish(bindingState(picker.copy(operation = RelatedPasswordBindingOperationState.Saving(targetEntryId.value))))
        linkMutationJob =
            scope.launch {
                try {
                    val result = operations.bind(entry.id, targetEntryId)
                    if (!isCurrent(entry.id, token, authentication)) return@launch
                    if (result is AppResult.Failure) {
                        if (currentBindingState() != RelatedPasswordBindingUiState.Closed) {
                            publish(bindingState(picker.copy(operation = RelatedPasswordBindingOperationState.Failed)))
                        }
                        return@launch
                    }
                    removeBoundCandidate(entry.id, targetEntryId)
                    val relations = reloadRelations(entry.id) ?: return@launch
                    if (!isCurrent(entry.id, token, authentication)) return@launch
                    publish(DetailMutation.RelationsChanged(entry.id, relations))
                    if (currentBindingState() != RelatedPasswordBindingUiState.Closed) {
                        publish(bindingState(RelatedPasswordBindingUiState.Closed))
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    if (isCurrent(entry.id, token, authentication) &&
                        currentBindingState() != RelatedPasswordBindingUiState.Closed
                    ) {
                        publish(bindingState(picker.copy(operation = RelatedPasswordBindingOperationState.Failed)))
                    }
                }
            }
    }

    fun requestUnlink(
        targetEntryId: EntryId,
        linkId: EntryLinkId,
    ) {
        val entry = activeCurrentEntry() ?: return
        if (workSession.current(entry.id) == null || !access.hasFullSecureSessionAccess()) return
        if (linkMutationJob?.isActive == true) return
        val related = currentRelatedPasswords().firstOrNull { it.linkId == linkId && it.targetEntryId == targetEntryId } ?: return
        val unlink = currentUnlinkState()
        if (unlink is RelatedPasswordUnlinkUiState.Open && unlink.phase == RelatedPasswordUnlinkPhase.Removing) return
        publish(
            unlinkState(
                RelatedPasswordUnlinkUiState.Open(
                    target =
                        RelatedPasswordUnlinkTargetUiModel(
                            targetEntryId = related.targetEntryId.value,
                            linkId = related.linkId.value,
                            targetTitle = related.title,
                        ),
                    phase = RelatedPasswordUnlinkPhase.AwaitingConfirmation,
                ),
            ),
        )
    }

    fun dismissUnlink() {
        when (val state = currentUnlinkState()) {
            is RelatedPasswordUnlinkUiState.Open -> {
                if (state.phase != RelatedPasswordUnlinkPhase.Removing) {
                    publish(unlinkState(RelatedPasswordUnlinkUiState.Closed))
                }
            }

            RelatedPasswordUnlinkUiState.Closed -> {
                Unit
            }
        }
    }

    fun confirmUnlink() {
        val target =
            when (val state = currentUnlinkState()) {
                is RelatedPasswordUnlinkUiState.Open -> {
                    when (state.phase) {
                        RelatedPasswordUnlinkPhase.AwaitingConfirmation,
                        RelatedPasswordUnlinkPhase.Failed,
                        -> state.target

                        RelatedPasswordUnlinkPhase.Removing -> return
                    }
                }

                RelatedPasswordUnlinkUiState.Closed -> {
                    return
                }
            }
        if (linkMutationJob?.isActive == true) return
        val entry = activeCurrentEntry() ?: return
        val token = workSession.current(entry.id) ?: return
        val authentication = access.authenticationState.value
        val targetEntryId = EntryId(target.targetEntryId)
        val linkId = EntryLinkId(target.linkId)

        publish(
            unlinkState(
                RelatedPasswordUnlinkUiState.Open(target, RelatedPasswordUnlinkPhase.Removing),
            ),
        )
        if (currentRelatedPasswords().none { it.linkId == linkId && it.targetEntryId == targetEntryId }) {
            linkMutationJob =
                scope.launch {
                    refreshStaleConfirmation(entry.id, token, authentication, target)
                }
            return
        }

        cancelCandidateLoad()
        linkMutationJob =
            scope.launch {
                var succeeded = false
                try {
                    succeeded = operations.unlink(entry.id, targetEntryId, linkId) is AppResult.Success
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    // The service owns operation logging. Refresh below resolves ambiguous outcomes.
                }
                if (!isCurrent(entry.id, token, authentication)) return@launch

                val relations =
                    try {
                        reloadRelations(entry.id)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        failConfirmationIfCurrent(entry.id, token, authentication, target)
                        return@launch
                    }
                if (!isCurrent(entry.id, token, authentication)) return@launch
                if (relations != null) {
                    candidateCache = null
                    publish(DetailMutation.RelationsChanged(entry.id, relations))
                }

                val linkStillPresent =
                    relations?.relatedPasswords?.any { it.linkId == linkId && it.targetEntryId == targetEntryId } == true
                if (succeeded || !linkStillPresent) {
                    publish(unlinkState(RelatedPasswordUnlinkUiState.Closed))
                } else {
                    publish(
                        unlinkState(
                            RelatedPasswordUnlinkUiState.Open(target, RelatedPasswordUnlinkPhase.Failed),
                        ),
                    )
                }
            }
    }

    fun close() {
        cancelCandidateLoad()
        candidateCache = null
        linkMutationJob?.cancel()
        linkMutationJob = null
    }

    private suspend fun refreshStaleConfirmation(
        entryId: EntryId,
        token: DetailWorkSession.Scope.Active,
        authentication: AuthenticationState,
        target: RelatedPasswordUnlinkTargetUiModel,
    ) {
        try {
            val relations = reloadRelations(entryId)
            if (!isCurrent(entryId, token, authentication)) return
            if (relations != null) {
                candidateCache = null
                publish(DetailMutation.RelationsChanged(entryId, relations))
            }
            publish(unlinkState(RelatedPasswordUnlinkUiState.Closed))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            failConfirmationIfCurrent(entryId, token, authentication, target)
        }
    }

    private suspend fun reloadRelations(entryId: EntryId) =
        try {
            presentationLoader.reloadRelations(entryId)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            logger.error(
                Events.Vault.Error,
                ErrorCodes.Security.Vault.VaultOperationFailed,
                Reasons.Application.Fallback.UnknownFailure,
                failure,
            )
            throw failure
        }

    private fun failConfirmationIfCurrent(
        entryId: EntryId,
        token: DetailWorkSession.Scope.Active,
        authentication: AuthenticationState,
        target: RelatedPasswordUnlinkTargetUiModel,
    ) {
        val removing = RelatedPasswordUnlinkUiState.Open(target, RelatedPasswordUnlinkPhase.Removing)
        if (isCurrent(entryId, token, authentication) && currentUnlinkState() == removing) {
            publish(
                unlinkState(
                    RelatedPasswordUnlinkUiState.Open(target, RelatedPasswordUnlinkPhase.Failed),
                ),
            )
        }
    }

    private fun activeCurrentEntry(): Entry? = currentEntry()?.takeIf { it.type == EntryType.LOGIN && it.deletedAt == null }

    private fun removeBoundCandidate(
        sourceEntryId: EntryId,
        targetEntryId: EntryId,
    ) {
        val cache = candidateCache?.takeIf { it.sourceEntryId == sourceEntryId } ?: return
        candidateCache = cache.copy(candidates = cache.candidates.filterNot { it.id == targetEntryId.value })
    }

    private fun cancelCandidateLoad() {
        candidateLoadGeneration++
        candidateLoadEntryId = null
        candidateJob?.cancel()
        candidateJob = null
    }

    private fun bindingState(value: RelatedPasswordBindingUiState) = DetailMutation.PasswordBindingStateChanged(value)

    private fun unlinkState(value: RelatedPasswordUnlinkUiState) = DetailMutation.PasswordUnlinkStateChanged(value)

    private fun isCurrent(
        entryId: EntryId,
        token: DetailWorkSession.Scope.Active,
        authentication: AuthenticationState,
    ): Boolean =
        workSession.isCurrent(token) && currentEntry()?.id == entryId &&
            access.authenticationState.value === authentication && access.hasFullSecureSessionAccess()
}
