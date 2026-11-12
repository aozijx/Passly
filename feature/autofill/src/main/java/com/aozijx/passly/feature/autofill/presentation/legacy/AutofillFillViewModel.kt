package com.aozijx.passly.feature.autofill.presentation.legacy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.observability.LogStatements
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.Operations
import com.aozijx.passly.core.observability.Reasons
import com.aozijx.passly.core.observability.monotonicTimeMs
import com.aozijx.passly.core.observability.trace.TraceContinuationStore
import com.aozijx.passly.domain.autofill.model.AutofillAuthorizationOutcome
import com.aozijx.passly.domain.autofill.model.AutofillCandidateSummary
import com.aozijx.passly.domain.autofill.model.AutofillCredentialField
import com.aozijx.passly.domain.autofill.model.AutofillDiscoveryContext
import com.aozijx.passly.domain.autofill.model.AutofillDiscoveryOutcome
import com.aozijx.passly.domain.autofill.model.AutofillGrantContext
import com.aozijx.passly.domain.autofill.model.AutofillInteractionId
import com.aozijx.passly.domain.autofill.model.AutofillSelectionOutcome
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.feature.autofill.core.AuthorizeAutofillInteraction
import com.aozijx.passly.feature.autofill.core.ConfirmLegacyWebAutofill
import com.aozijx.passly.feature.autofill.core.DiscoverAutofillCandidates
import com.aozijx.passly.feature.autofill.core.FinishAutofillInteraction
import com.aozijx.passly.feature.autofill.core.ResolveAutofillSelection
import com.aozijx.passly.feature.autofill.presentation.requestedCredentialFields
import com.aozijx.passly.feature.autofill.presentation.result.AutofillActivityResultClaim
import com.aozijx.passly.feature.autofill.runtime.AutofillSelectionReceipt
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
internal class AutofillFillViewModel
    @Inject
    internal constructor(
        private val authorizeInteraction: AuthorizeAutofillInteraction,
        private val confirmWebAutofill: ConfirmLegacyWebAutofill,
        private val discoverCandidates: DiscoverAutofillCandidates,
        private val resolveSelection: ResolveAutofillSelection,
        private val finishInteraction: FinishAutofillInteraction,
        private val logger: Logger,
        private val traceContinuations: TraceContinuationStore,
    ) : ViewModel() {
        private val mutableUiState = MutableStateFlow<AutofillFillUiState>(AutofillFillUiState.Initial)
        internal val uiState: StateFlow<AutofillFillUiState> = mutableUiState.asStateFlow()
        private val resultClaim = AutofillActivityResultClaim()

        val hasClaimedPlatformResult: Boolean
            get() = resultClaim.isClaimed

        private var currentRequest: AutofillFillRequest? = null
        private var selectionContext: AutofillDiscoveryContext? = null

        @Volatile private var abandoned = false

        fun commitPlatformResult(deliver: (markPlatformCommitted: () -> Unit) -> Unit): Boolean {
            val state = mutableUiState.value
            if (state !is AutofillFillUiState.Result) return false
            val committed = resultClaim.commit(deliver)
            if (committed) mutate(AutofillFillMutation.ResultDelivered)
            return committed
        }

        internal fun onAction(action: AutofillFillUiAction) {
            when (action) {
                is AutofillFillUiAction.Initialize -> initialize(action.request)
                is AutofillFillUiAction.CandidateSelected -> selectCandidate(action.entryId)
                AutofillFillUiAction.ConfirmWebRecipient -> confirmWebRecipient()
                AutofillFillUiAction.ConfirmWebEntry -> confirmWebEntry()
                AutofillFillUiAction.Cancel -> cancel()
            }
        }

        private fun initialize(request: AutofillFillRequest) {
            if (abandoned) return
            if (currentRequest != null) return
            currentRequest = request
            selectionContext = null
            mutate(AutofillFillMutation.Loading)
            launchStep(request) {
                val authenticationStartedAt = monotonicTimeMs()
                logger.log(LogStatements.Autofill.authenticationStarted())
                when (authorizeInteraction(request.interactionId)) {
                    is AutofillAuthorizationOutcome.Authorized -> {
                        logger.log(
                            LogStatements.Autofill.authenticationCompleted(
                                monotonicTimeMs() - authenticationStartedAt,
                            ),
                        )
                        continueAuthorized(request)
                    }

                    is AutofillAuthorizationOutcome.Cancelled -> {
                        logger.log(
                            LogStatements.Autofill.discoveryUnavailable(
                                Reasons.Security.Authentication.AuthenticationRejected,
                            ),
                        )
                        traceContinuations.discard(request.interactionId.value)
                        mutate(AutofillFillMutation.Completed(AutofillFillResult.Cancelled))
                    }

                    is AutofillAuthorizationOutcome.Failed,
                    is AutofillAuthorizationOutcome.Rejected,
                    -> {
                        fail(request, AutofillFillFailure.AUTHENTICATION)
                    }
                }
            }
        }

        private suspend fun continueAuthorized(request: AutofillFillRequest) {
            val discoveryStartedAt = monotonicTimeMs()
            logger.log(LogStatements.Autofill.discoveryStarted())
            when (val discovery = discover(request)) {
                is AutofillDiscoveryOutcome.ResumeSelection -> {
                    selectionContext = discovery.context
                    resolve(
                        request,
                        discovery.entryId,
                        request.roleMap().requestedCredentialFields(),
                        context = discovery.context,
                    )
                }

                is AutofillDiscoveryOutcome.Candidates -> {
                    selectionContext = discovery.context
                    logger.log(
                        LogStatements.Autofill.discoveryCompleted(
                            monotonicTimeMs() - discoveryStartedAt,
                        ),
                    )
                    val directEntryId = request.directEntryId
                    val requestedFields = request.roleMap().requestedCredentialFields()
                    when {
                        directEntryId != null && request.webDomain != null -> {
                            val candidate = discovery.candidates.firstOrNull { it.entry.id == directEntryId }
                            if (candidate == null) {
                                fail(request, AutofillFillFailure.DISCOVERY)
                            } else {
                                showWebEntryConfirmation(request, candidate)
                            }
                        }

                        directEntryId != null -> {
                            resolve(request, directEntryId, requestedFields, discovery.context)
                        }

                        request.uiMode == AutofillPresentation.BOTTOM_SHEET -> {
                            traceContinuations.capture(request.interactionId.value)
                            mutate(AutofillFillMutation.CandidatesAvailable(discovery.candidates))
                        }

                        else -> {
                            returnDiscoveryResponse(request, discovery)
                        }
                    }
                }

                is AutofillDiscoveryOutcome.NoCandidates -> {
                    logger.log(
                        LogStatements.Autofill.discoveryUnavailable(Reasons.Autofill.Matching.NoMatchingEntry),
                    )
                    returnDiscoveryResponse(request, discovery)
                }

                is AutofillDiscoveryOutcome.Failed -> {
                    fail(
                        request = request,
                        failure = AutofillFillFailure.DISCOVERY,
                        diagnostic = discovery.failure,
                    )
                }

                is AutofillDiscoveryOutcome.RequiresWebRecipientConfirmation -> {
                    showWebRecipientConfirmation(request)
                }

                AutofillDiscoveryOutcome.Disabled,
                AutofillDiscoveryOutcome.UnsupportedFields,
                is AutofillDiscoveryOutcome.RequiresUnlock,
                -> {
                    fail(request, AutofillFillFailure.DISCOVERY)
                }
            }
        }

        private suspend fun discover(request: AutofillFillRequest): AutofillDiscoveryOutcome =
            discoverCandidates(
                request = request.toDomainRequest(),
                interactionId = request.interactionId,
                classifiedRoles = request.roleMap(),
                directEntryId = request.directEntryId,
            )

        private suspend fun returnDiscoveryResponse(
            request: AutofillFillRequest,
            outcome: AutofillDiscoveryOutcome,
        ) {
            traceContinuations.capture(request.interactionId.value)
            mutate(AutofillFillMutation.Completed(AutofillFillResult.Discovery(outcome)))
        }

        private suspend fun showWebRecipientConfirmation(request: AutofillFillRequest) {
            val domain = AutofillGrantContext(request.packageName, request.webDomain).normalized()?.webDomain
            if (domain == null) {
                fail(request, AutofillFillFailure.DISCOVERY)
                return
            }
            traceContinuations.capture(request.interactionId.value)
            mutate(AutofillFillMutation.WebRecipientConfirmationRequired(request.packageName, domain))
        }

        private suspend fun showWebEntryConfirmation(
            request: AutofillFillRequest,
            candidate: AutofillCandidateSummary,
        ) {
            val domain = AutofillGrantContext(request.packageName, request.webDomain).normalized()?.webDomain
            if (domain == null) {
                fail(request, AutofillFillFailure.DISCOVERY)
                return
            }
            traceContinuations.capture(request.interactionId.value)
            mutate(
                AutofillFillMutation.WebEntryConfirmationRequired(
                    packageName = request.packageName,
                    webDomain = domain,
                    candidate = candidate,
                ),
            )
        }

        private fun confirmWebRecipient() {
            if (mutableUiState.value !is AutofillFillUiState.ConfirmWebRecipient) return
            val request = currentRequest ?: return
            mutate(AutofillFillMutation.Loading)
            launchStep(request) {
                val scope = AutofillGrantContext(request.packageName, request.webDomain)
                if (!confirmWebAutofill.confirmRecipient(request.interactionId, scope)) {
                    fail(request, AutofillFillFailure.SELECTION)
                } else {
                    continueAuthorized(request)
                }
            }
        }

        private fun confirmWebEntry() {
            val request = currentRequest ?: return
            val confirmation = mutableUiState.value as? AutofillFillUiState.ConfirmWebEntry ?: return
            val context = selectionContext
            mutate(AutofillFillMutation.Loading)
            launchStep(request) {
                val scope = AutofillGrantContext(request.packageName, request.webDomain)
                val entryId = confirmation.candidate.entry.id
                if (!confirmWebAutofill.confirmEntry(request.interactionId, scope, entryId)) {
                    fail(request, AutofillFillFailure.SELECTION)
                } else if (context == null) {
                    fail(request, AutofillFillFailure.INTERNAL)
                } else {
                    resolve(
                        request,
                        entryId,
                        request.roleMap().requestedCredentialFields(),
                        context,
                    )
                }
            }
        }

        private fun selectCandidate(entryId: EntryId) {
            val request = currentRequest ?: return
            val candidates = (mutableUiState.value as? AutofillFillUiState.ShowCandidates)?.candidates ?: return
            val candidate = candidates.firstOrNull { it.entry.id == entryId } ?: return
            val context = selectionContext
            mutate(AutofillFillMutation.Loading)
            if (request.webDomain != null) {
                launchStep(request) {
                    showWebEntryConfirmation(request, candidate)
                }
                return
            }
            launchStep(request) {
                if (context == null) {
                    fail(request, AutofillFillFailure.INTERNAL)
                } else {
                    resolve(
                        request,
                        entryId,
                        request.roleMap().requestedCredentialFields(),
                        context,
                    )
                }
            }
        }

        private suspend fun resolve(
            request: AutofillFillRequest,
            entryId: EntryId,
            requestedFields: Set<AutofillCredentialField>,
            context: AutofillDiscoveryContext,
        ) {
            when (
                val outcome =
                    resolveSelection(
                        interactionId = request.interactionId,
                        scope = AutofillGrantContext(request.packageName, request.webDomain),
                        entryId = entryId,
                        requestedFields = requestedFields,
                        otpAutofillEnabled = context.otpAutofillEnabled,
                        strictDomainMatching = context.strictDomainMatching,
                    )
            ) {
                is AutofillSelectionOutcome.Filled -> {
                    returnDataset(request, outcome)
                }

                AutofillSelectionOutcome.Cancelled,
                AutofillSelectionOutcome.Unauthorized,
                AutofillSelectionOutcome.CandidateUnavailable,
                AutofillSelectionOutcome.ScopeMismatch,
                is AutofillSelectionOutcome.Rejected,
                is AutofillSelectionOutcome.Failed,
                -> {
                    fail(
                        request = request,
                        failure = AutofillFillFailure.SELECTION,
                        diagnostic = outcome.failureKind(),
                    )
                }
            }
        }

        private suspend fun returnDataset(
            request: AutofillFillRequest,
            outcome: AutofillSelectionOutcome.Filled,
        ) {
            val normalizedScope = AutofillGrantContext(request.packageName, request.webDomain).normalized()
            val receipt =
                AutofillSelectionReceipt(
                    interactionId = request.interactionId,
                    entryId = outcome.payload.entryId,
                    webScope = normalizedScope?.takeIf { it.webDomain != null },
                )
            traceContinuations.capture(request.interactionId.value)
            val result =
                AutofillFillResult.SelectedCredential(
                    payload = outcome.payload,
                    receipt = receipt,
                    target = request.resultTarget,
                )
            mutate(AutofillFillMutation.Completed(result))
        }

        fun cancel(fallbackInteractionId: AutofillInteractionId? = null) {
            if (abandoned) return
            abandoned = true
            val request = currentRequest
            val interactionId = request?.interactionId ?: fallbackInteractionId
            viewModelScope.coroutineContext.cancelChildren()
            viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
                withContext(NonCancellable + Dispatchers.IO) {
                    try {
                        interactionId?.let { finishInteraction.cancel(it) }
                    } finally {
                        interactionId?.let { traceContinuations.discard(it.value) }
                        if (!resultClaim.isClaimed) {
                            mutate(AutofillFillMutation.Completed(AutofillFillResult.Cancelled))
                        }
                    }
                }
            }
        }

        private suspend fun fail(
            request: AutofillFillRequest,
            failure: AutofillFillFailure,
            error: Exception? = null,
            diagnostic: Enum<*> = failure,
        ) {
            var cleanupFailure: Exception? = null
            var cleanupCancellation: CancellationException? = null
            var cleanupError: Error? = null
            withContext(NonCancellable) {
                try {
                    finishInteraction.cancel(request.interactionId)
                } catch (cancelled: CancellationException) {
                    cleanupCancellation = cancelled
                } catch (error: Error) {
                    cleanupError = error
                } catch (exception: Exception) {
                    cleanupFailure = exception
                }
                try {
                    traceContinuations.discard(request.interactionId.value)
                } catch (cancelled: CancellationException) {
                    cleanupCancellation?.addSuppressed(cancelled) ?: run { cleanupCancellation = cancelled }
                } catch (error: Error) {
                    cleanupError?.addSuppressed(error) ?: run { cleanupError = error }
                } catch (exception: Exception) {
                    cleanupFailure?.addSuppressed(exception) ?: run { cleanupFailure = exception }
                } finally {
                    mutate(AutofillFillMutation.Completed(AutofillFillResult.Failed(failure)))
                }
            }
            cleanupError?.let { fatalError ->
                cleanupCancellation?.let(fatalError::addSuppressed)
                throw fatalError
            }
            cleanupCancellation?.let { throw it }
            if (error == null) {
                logger.log(
                    LogStatements.Autofill.discoveryUnavailable(
                        reason = Reasons.Application.Fallback.UnknownFailure,
                        failureKind = diagnostic,
                    ),
                )
            } else {
                logger.log(LogStatements.Autofill.discoveryFailed(error))
            }
            cleanupFailure?.let { logger.log(LogStatements.Autofill.discoveryFailed(it)) }
        }

        private fun mutate(mutation: AutofillFillMutation) {
            if (abandoned && mutation != AutofillFillMutation.Completed(AutofillFillResult.Cancelled)) return
            mutableUiState.value = AutofillFillReducer.reduce(mutableUiState.value, mutation)
        }

        private fun launchStep(
            request: AutofillFillRequest,
            block: suspend com.aozijx.passly.core.observability.TraceScope.() -> Unit,
        ) {
            viewModelScope.launch {
                try {
                    continueTrace(request, block)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    if (mutableUiState.value !is AutofillFillUiState.Result) {
                        fail(request, AutofillFillFailure.INTERNAL, error)
                    } else {
                        logger.log(LogStatements.Autofill.discoveryFailed(error))
                    }
                }
            }
        }

        private suspend fun <T> continueTrace(
            request: AutofillFillRequest,
            block: suspend com.aozijx.passly.core.observability.TraceScope.() -> T,
        ): T =
            traceContinuations.continueOrRoot(
                request.interactionId.value,
                Operations.AutofillLegacyRequest,
                block,
            )

        private fun AutofillSelectionOutcome.failureKind(): Enum<*> =
            when (this) {
                AutofillSelectionOutcome.Cancelled -> SelectionFailureKind.CANCELLED
                AutofillSelectionOutcome.Unauthorized -> SelectionFailureKind.UNAUTHORIZED
                AutofillSelectionOutcome.CandidateUnavailable -> SelectionFailureKind.CANDIDATE_UNAVAILABLE
                AutofillSelectionOutcome.ScopeMismatch -> SelectionFailureKind.SCOPE_MISMATCH
                is AutofillSelectionOutcome.Rejected -> reason
                is AutofillSelectionOutcome.Failed -> failure
                is AutofillSelectionOutcome.Filled -> error("Filled outcome is not a failure")
            }

        private enum class SelectionFailureKind {
            CANCELLED,
            UNAUTHORIZED,
            CANDIDATE_UNAVAILABLE,
            SCOPE_MISMATCH,
        }
    }
