package com.aozijx.passly.feature.vault.presentation.detail

import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.feature.vault.detail.RelatedPasswordBinding
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.RelatedPasswordBindingUiState
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.RelatedPasswordCandidateUiModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Owns the picker request and bind operation for the current Detail page. */
internal class DetailPasswordBindingWorkflow(
    private val scope: CoroutineScope,
    private val binding: RelatedPasswordBinding,
    private val access: SecureSessionAccessState,
    private val workSession: DetailWorkSession,
    private val currentEntry: () -> Entry?,
    private val currentBindingState: () -> RelatedPasswordBindingUiState,
    private val presentationLoader: DetailPresentationLoader,
    private val publish: (DetailMutation) -> Unit,
) {
    private var candidateJob: Job? = null
    private var bindingJob: Job? = null

    fun open() {
        if (bindingJob?.isActive == true) return
        val entry = currentEntry()?.takeIf { it.type == EntryType.LOGIN && it.deletedAt == null } ?: return
        val token = workSession.current(entry.id) ?: return
        val authentication = access.authenticationState.value
        candidateJob?.cancel()
        publish(state(RelatedPasswordBindingUiState.Loading))
        candidateJob =
            scope.launch {
                try {
                    val candidates = binding.candidates(entry.id)
                    if (isCurrent(entry.id, token, authentication) &&
                        currentBindingState() == RelatedPasswordBindingUiState.Loading
                    ) {
                        publish(
                            state(
                                RelatedPasswordBindingUiState.Ready(
                                    candidates.map { RelatedPasswordCandidateUiModel(it.id.value, it.title, it.username) },
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
                        publish(state(RelatedPasswordBindingUiState.LoadFailed))
                    }
                }
            }
    }

    fun dismiss() {
        if (currentBindingState() == RelatedPasswordBindingUiState.Loading) candidateJob?.cancel()
        publish(state(RelatedPasswordBindingUiState.Closed))
    }

    fun bind(targetEntryId: EntryId) {
        val entry = currentEntry()?.takeIf { it.type == EntryType.LOGIN && it.deletedAt == null } ?: return
        val picker = currentBindingState() as? RelatedPasswordBindingUiState.Ready ?: return
        if (picker.savingEntryId != null || picker.candidates.none { it.id == targetEntryId.value }) return
        val token = workSession.current(entry.id) ?: return
        val authentication = access.authenticationState.value
        if (bindingJob?.isActive == true) return
        candidateJob?.cancel()
        publish(state(picker.copy(savingEntryId = targetEntryId.value, bindFailed = false)))
        bindingJob =
            scope.launch {
                try {
                    val result = binding.bind(entry.id, targetEntryId)
                    if (!isCurrent(entry.id, token, authentication)) return@launch
                    if (result.isFailure) {
                        if (currentBindingState() != RelatedPasswordBindingUiState.Closed) {
                            publish(state(picker.copy(bindFailed = true)))
                        }
                        return@launch
                    }
                    val relatedEntries = presentationLoader.reloadRelatedEntries(entry.id) ?: return@launch
                    if (!isCurrent(entry.id, token, authentication)) return@launch
                    publish(DetailMutation.RelatedEntriesChanged(entry.id, relatedEntries))
                    if (currentBindingState() != RelatedPasswordBindingUiState.Closed) {
                        publish(state(RelatedPasswordBindingUiState.Closed))
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    if (isCurrent(entry.id, token, authentication) &&
                        currentBindingState() != RelatedPasswordBindingUiState.Closed
                    ) {
                        publish(state(picker.copy(bindFailed = true)))
                    }
                }
            }
    }

    fun close() {
        candidateJob?.cancel()
        candidateJob = null
        bindingJob?.cancel()
        bindingJob = null
    }

    private fun state(value: RelatedPasswordBindingUiState) = DetailMutation.PasswordBindingStateChanged(value)

    private fun isCurrent(
        entryId: EntryId,
        token: DetailWorkSession.Scope.Active,
        authentication: AuthenticationState,
    ): Boolean =
        workSession.isCurrent(token) && currentEntry()?.id == entryId &&
            access.authenticationState.value === authentication && access.hasFullSecureSessionAccess()
}
