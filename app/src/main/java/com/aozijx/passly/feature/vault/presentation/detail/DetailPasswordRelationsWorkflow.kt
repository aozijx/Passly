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
import com.aozijx.passly.feature.vault.detail.DetailRelatedPassword
import com.aozijx.passly.feature.vault.detail.RelatedPasswordLinks
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Owns related-password loading and mutations for the current Detail page. */
internal class DetailPasswordRelationsWorkflow(
    private val scope: CoroutineScope,
    private val links: RelatedPasswordLinks,
    private val access: SecureSessionAccessState,
    private val workSession: DetailWorkSession,
    private val currentEntry: () -> Entry?,
    private val currentBindingState: () -> RelatedPasswordBindingUiState,
    private val currentRelatedPasswords: () -> List<DetailRelatedPassword>,
    private val currentUnlinkState: () -> RelatedPasswordUnlinkUiState,
    private val presentationLoader: DetailPresentationLoader,
    private val logger: Logger,
    private val publish: (DetailMutation) -> Unit,
) {
    private var candidateJob: Job? = null
    private var linkMutationJob: Job? = null

    fun open() {
        if (linkMutationJob?.isActive == true) return
        val entry = currentEntry()?.takeIf { it.type == EntryType.LOGIN && it.deletedAt == null } ?: return
        val token = workSession.current(entry.id) ?: return
        val authentication = access.authenticationState.value
        candidateJob?.cancel()
        publish(bindingState(RelatedPasswordBindingUiState.Loading))
        candidateJob =
            scope.launch {
                try {
                    val candidates = links.candidates(entry.id)
                    if (isCurrent(entry.id, token, authentication) &&
                        currentBindingState() == RelatedPasswordBindingUiState.Loading
                    ) {
                        publish(
                            bindingState(
                                RelatedPasswordBindingUiState.Ready(
                                    candidates.map {
                                        RelatedPasswordCandidateUiModel(
                                            it.id.value,
                                            it.title,
                                            it.username.takeIf(String::isNotBlank),
                                        )
                                    },
                                ),
                            ),
                        )
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    if (isCurrent(entry.id, token, authentication) &&
                        currentBindingState() == RelatedPasswordBindingUiState.Loading
                    ) {
                        publish(bindingState(RelatedPasswordBindingUiState.LoadFailed))
                    }
                }
            }
    }

    fun dismiss() {
        if (currentBindingState() == RelatedPasswordBindingUiState.Loading) candidateJob?.cancel()
        publish(bindingState(RelatedPasswordBindingUiState.Closed))
    }

    fun bind(targetEntryId: EntryId) {
        val entry = activeCurrentEntry() ?: return
        val picker = currentBindingState() as? RelatedPasswordBindingUiState.Ready ?: return
        if (picker.savingEntryId != null || picker.candidates.none { it.id == targetEntryId.value }) return
        val token = workSession.current(entry.id) ?: return
        val authentication = access.authenticationState.value
        if (linkMutationJob?.isActive == true) return
        candidateJob?.cancel()
        publish(bindingState(picker.copy(savingEntryId = targetEntryId.value, bindFailed = false)))
        linkMutationJob =
            scope.launch {
                try {
                    val result = links.bind(entry.id, targetEntryId)
                    if (!isCurrent(entry.id, token, authentication)) return@launch
                    if (result is AppResult.Failure) {
                        if (currentBindingState() != RelatedPasswordBindingUiState.Closed) {
                            publish(bindingState(picker.copy(bindFailed = true)))
                        }
                        return@launch
                    }
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
                        publish(bindingState(picker.copy(bindFailed = true)))
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
        if (currentUnlinkState() is RelatedPasswordUnlinkUiState.Removing) return
        publish(
            unlinkState(
                RelatedPasswordUnlinkUiState.AwaitingConfirmation(
                    target =
                        RelatedPasswordUnlinkTargetUiModel(
                            targetEntryId = related.targetEntryId.value,
                            linkId = related.linkId.value,
                            targetTitle = related.title,
                        ),
                ),
            ),
        )
    }

    fun dismissUnlink() {
        when (currentUnlinkState()) {
            is RelatedPasswordUnlinkUiState.AwaitingConfirmation,
            is RelatedPasswordUnlinkUiState.Failed,
            -> publish(unlinkState(RelatedPasswordUnlinkUiState.Closed))

            RelatedPasswordUnlinkUiState.Closed,
            is RelatedPasswordUnlinkUiState.Removing,
            -> Unit
        }
    }

    fun confirmUnlink() {
        val target =
            when (val state = currentUnlinkState()) {
                is RelatedPasswordUnlinkUiState.AwaitingConfirmation -> state.target
                is RelatedPasswordUnlinkUiState.Failed -> state.target
                RelatedPasswordUnlinkUiState.Closed,
                is RelatedPasswordUnlinkUiState.Removing,
                -> return
            }
        if (linkMutationJob?.isActive == true) return
        val entry = activeCurrentEntry() ?: return
        val token = workSession.current(entry.id) ?: return
        val authentication = access.authenticationState.value
        val targetEntryId = EntryId(target.targetEntryId)
        val linkId = EntryLinkId(target.linkId)

        publish(unlinkState(RelatedPasswordUnlinkUiState.Removing(target)))
        if (currentRelatedPasswords().none { it.linkId == linkId && it.targetEntryId == targetEntryId }) {
            linkMutationJob =
                scope.launch {
                    refreshStaleConfirmation(entry.id, token, authentication, target)
                }
            return
        }

        candidateJob?.cancel()
        linkMutationJob =
            scope.launch {
                var succeeded = false
                try {
                    succeeded = links.unlink(entry.id, targetEntryId, linkId) is AppResult.Success
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
                if (relations != null) publish(DetailMutation.RelationsChanged(entry.id, relations))

                val linkStillPresent =
                    relations?.relatedPasswords?.any { it.linkId == linkId && it.targetEntryId == targetEntryId } == true
                if (succeeded || !linkStillPresent) {
                    publish(unlinkState(RelatedPasswordUnlinkUiState.Closed))
                } else {
                    publish(unlinkState(RelatedPasswordUnlinkUiState.Failed(target)))
                }
            }
    }

    fun close() {
        candidateJob?.cancel()
        candidateJob = null
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
            if (relations != null) publish(DetailMutation.RelationsChanged(entryId, relations))
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
                ErrorCodes.Security.VaultOperationFailed,
                Reasons.UnknownFailure,
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
        if (isCurrent(entryId, token, authentication) && currentUnlinkState() == RelatedPasswordUnlinkUiState.Removing(target)) {
            publish(unlinkState(RelatedPasswordUnlinkUiState.Failed(target)))
        }
    }

    private fun activeCurrentEntry(): Entry? =
        currentEntry()?.takeIf { it.type == EntryType.LOGIN && it.deletedAt == null }

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
