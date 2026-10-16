package com.aozijx.passly.presentation.feature.autofill.legacy

import android.content.Context
import android.service.autofill.FillResponse
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.LogStatements
import com.aozijx.passly.core.observability.Operations
import com.aozijx.passly.core.observability.Reasons
import com.aozijx.passly.core.observability.monotonicTimeMs
import com.aozijx.passly.core.observability.trace.TraceContinuationStore
import com.aozijx.passly.domain.autofill.model.AutofillAuthorizationOutcome
import com.aozijx.passly.domain.autofill.model.AutofillDiscoveryOutcome
import com.aozijx.passly.domain.autofill.model.AutofillField
import com.aozijx.passly.domain.autofill.model.AutofillGrantContext
import com.aozijx.passly.domain.autofill.model.AutofillInteractionState
import com.aozijx.passly.domain.autofill.model.AutofillRequest
import com.aozijx.passly.domain.autofill.model.AutofillSelectionOutcome
import com.aozijx.passly.domain.autofill.model.AutofillSource
import com.aozijx.passly.domain.autofill.model.FieldRole
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.settings.model.AutofillPresentation
import com.aozijx.passly.feature.autofill.core.AuthorizeAutofillInteraction
import com.aozijx.passly.feature.autofill.core.DiscoverAutofillCandidates
import com.aozijx.passly.feature.autofill.core.FinishAutofillInteraction
import com.aozijx.passly.feature.autofill.core.ResolveAutofillSelection
import com.aozijx.passly.feature.autofill.legacy.service.builder.LegacyResponseFactory
import com.aozijx.passly.feature.autofill.legacy.service.parser.EditableFieldInfo
import com.aozijx.passly.feature.autofill.legacy.service.parser.ParsedStructure
import com.aozijx.passly.feature.autofill.runtime.AutofillInteractionStore
import com.aozijx.passly.feature.autofill.runtime.AutofillSelectionReceipt
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class AutofillFillViewModel @Inject constructor(
    private val authorizeInteraction: AuthorizeAutofillInteraction,
    private val discoverCandidates: DiscoverAutofillCandidates,
    private val resolveSelection: ResolveAutofillSelection,
    private val finishInteraction: FinishAutofillInteraction,
    private val interactionStore: AutofillInteractionStore,
    private val responseFactory: LegacyResponseFactory,
    private val logger: Logger,
    private val traceContinuations: TraceContinuationStore,
    @param:ApplicationContext private val appContext: Context,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow<AutofillFillUiState>(AutofillFillUiState.Initial)
    val uiState: StateFlow<AutofillFillUiState> = mutableUiState.asStateFlow()

    private var currentRequest: AutofillFillRequest? = null

    fun onAction(action: AutofillFillUiAction) {
        when (action) {
            is AutofillFillUiAction.Initialize -> initialize(action.request)
            is AutofillFillUiAction.CandidateSelected -> selectCandidate(action.entryId)
            AutofillFillUiAction.Cancel -> cancel()
        }
    }

    private fun initialize(request: AutofillFillRequest) {
        if (currentRequest == request && mutableUiState.value != AutofillFillUiState.Initial) return
        currentRequest = request
        viewModelScope.launch {
            continueTrace(request) {
                mutate(AutofillFillMutation.Loading)
                val authenticationStartedAt = monotonicTimeMs()
                logger.log(LogStatements.Autofill.authenticationStarted())
                try {
                    when (authorizeInteraction(request.interactionId)) {
                        is AutofillAuthorizationOutcome.Authorized -> {
                            logger.log(
                                LogStatements.Autofill.authenticationCompleted(
                                    monotonicTimeMs() - authenticationStartedAt,
                                ),
                            )
                            continueAuthorized(request)
                        }
                        is AutofillAuthorizationOutcome.Cancelled,
                        is AutofillAuthorizationOutcome.Failed,
                        is AutofillAuthorizationOutcome.Rejected,
                        -> {
                            logger.log(
                                LogStatements.Autofill.discoveryUnavailable(
                                    Reasons.AuthenticationRejected,
                                ),
                            )
                            traceContinuations.discard(request.interactionId.value)
                            mutate(AutofillFillMutation.Completed(AutofillFillResult.Cancelled))
                        }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    fail(request, "legacy.fill_authorization_failed", error)
                }
            }
        }
    }

    private suspend fun continueAuthorized(request: AutofillFillRequest) {
        val snapshot = interactionStore.snapshot(request.interactionId)
        if (request.directEntryId != null && snapshot?.state == AutofillInteractionState.CandidatesReady) {
            resolve(request, request.directEntryId)
            return
        }

        val discoveryStartedAt = monotonicTimeMs()
        logger.log(LogStatements.Autofill.discoveryStarted())
        when (val discovery = discover(request)) {
            is AutofillDiscoveryOutcome.Candidates -> {
                logger.log(
                    LogStatements.Autofill.discoveryCompleted(
                        monotonicTimeMs() - discoveryStartedAt,
                    ),
                )
                val directEntryId = request.directEntryId
                when {
                    directEntryId != null -> resolve(request, directEntryId)
                    request.uiMode == AutofillPresentation.BOTTOM_SHEET -> {
                        traceContinuations.capture(request.interactionId.value)
                        mutate(AutofillFillMutation.CandidatesAvailable(discovery.candidates))
                    }
                    else -> returnDiscoveryResponse(request, discovery)
                }
            }

            is AutofillDiscoveryOutcome.NoCandidates -> {
                logger.log(
                    LogStatements.Autofill.discoveryUnavailable(Reasons.NoMatchingEntry),
                )
                returnDiscoveryResponse(request, discovery)
            }
            AutofillDiscoveryOutcome.Disabled,
            AutofillDiscoveryOutcome.UnsupportedFields,
            is AutofillDiscoveryOutcome.Failed,
            is AutofillDiscoveryOutcome.RequiresUnlock,
            -> fail(request, "legacy.discovery_unavailable")
        }
    }

    private suspend fun discover(request: AutofillFillRequest): AutofillDiscoveryOutcome =
        discoverCandidates(
            request = request.toDomainRequest(),
            interactionId = request.interactionId,
            classifiedRoles = request.roleMap(),
        )

    private suspend fun returnDiscoveryResponse(
        request: AutofillFillRequest,
        outcome: AutofillDiscoveryOutcome,
    ) {
        val response = responseFactory.buildFillResponse(appContext, outcome, request.toParsedStructure())
        if (response == null) {
            finishInteraction.cancel(request.interactionId)
            mutate(AutofillFillMutation.Completed(AutofillFillResult.Cancelled))
        } else {
            traceContinuations.capture(request.interactionId.value)
            mutate(AutofillFillMutation.Completed(AutofillFillResult.Response(response)))
        }
    }

    private fun selectCandidate(entryId: EntryId) {
        val request = currentRequest ?: return
        viewModelScope.launch {
            continueTrace(request) {
                mutate(AutofillFillMutation.Loading)
                try {
                    resolve(request, entryId)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    fail(request, "legacy.selection_failed", error)
                }
            }
        }
    }

    private suspend fun resolve(request: AutofillFillRequest, entryId: EntryId) {
        when (
            val outcome = resolveSelection(
                interactionId = request.interactionId,
                scope = AutofillGrantContext(request.packageName, request.webDomain),
                entryId = entryId,
                includeOtp = request.otpIds.isNotEmpty(),
            )
        ) {
            is AutofillSelectionOutcome.Filled -> returnDataset(request, outcome)
            AutofillSelectionOutcome.Cancelled,
            AutofillSelectionOutcome.Unauthorized,
            AutofillSelectionOutcome.CandidateUnavailable,
            AutofillSelectionOutcome.ScopeMismatch,
            is AutofillSelectionOutcome.Rejected,
            is AutofillSelectionOutcome.Failed,
            -> fail(request, "legacy.selection_unavailable")
        }
    }

    private suspend fun returnDataset(
        request: AutofillFillRequest,
        outcome: AutofillSelectionOutcome.Filled,
    ) {
        val dataset = responseFactory.buildDataset(
            payload = outcome.payload,
            usernameIds = request.usernameIds,
            passwordIds = request.passwordIds,
            otpIds = request.otpIds,
        )
        if (dataset == null) {
            fail(request, "legacy.no_fillable_fields")
            return
        }

        val receipt = AutofillSelectionReceipt(request.interactionId, outcome.payload.entryId)
        traceContinuations.capture(request.interactionId.value)
        val result = if (request.returnsDataset) {
            AutofillFillResult.DatasetResult(dataset, receipt)
        } else {
            AutofillFillResult.Response(
                value = FillResponse.Builder().addDataset(dataset).build(),
                receipt = receipt,
            )
        }
        mutate(AutofillFillMutation.Completed(result))
    }

    private fun cancel() {
        val request = currentRequest
        viewModelScope.launch {
            request?.let {
                finishInteraction.cancel(it.interactionId)
                traceContinuations.discard(it.interactionId.value)
            }
            mutate(AutofillFillMutation.Completed(AutofillFillResult.Cancelled))
        }
    }

    private suspend fun fail(
        request: AutofillFillRequest,
        event: String,
        error: Exception? = null,
    ) {
        finishInteraction.cancel(request.interactionId)
        traceContinuations.discard(request.interactionId.value)
        if (error == null) {
            logger.log(LogStatements.Autofill.discoveryUnavailable(Reasons.UnknownFailure))
        } else {
            logger.log(LogStatements.Autofill.discoveryFailed(error))
        }
        mutate(AutofillFillMutation.Failed(event))
    }

    private fun mutate(mutation: AutofillFillMutation) {
        mutableUiState.value = AutofillFillReducer.reduce(mutableUiState.value, mutation)
    }

    private suspend fun <T> continueTrace(
        request: AutofillFillRequest,
        block: suspend com.aozijx.passly.core.observability.TraceScope.() -> T,
    ): T = traceContinuations.continueOrRoot(
            request.interactionId.value,
            Operations.AutofillLegacyRequest,
            block,
        )

    private fun AutofillFillRequest.roleMap(): Map<String, FieldRole> = buildMap {
        usernameIds.forEach { put(it.toString(), FieldRole.USERNAME) }
        passwordIds.forEach { put(it.toString(), FieldRole.PASSWORD) }
        otpIds.forEach { put(it.toString(), FieldRole.OTP) }
    }

    private fun AutofillFillRequest.toDomainRequest(): AutofillRequest {
        val roles = roleMap()
        return AutofillRequest(
            packageName = packageName,
            domain = webDomain,
            fields = editableIds.map { id ->
                AutofillField(
                    id = id.toString(),
                    hints = emptySet(),
                    inputType = null,
                    isFocused = false,
                    role = roles[id.toString()],
                )
            },
            source = AutofillSource.AUTOFILL_SERVICE,
        )
    }

    private fun AutofillFillRequest.toParsedStructure(): ParsedStructure = ParsedStructure(
        packageName = packageName,
        webDomain = webDomain,
        editableFields = editableIds.map(::EditableFieldInfo),
    )
}
