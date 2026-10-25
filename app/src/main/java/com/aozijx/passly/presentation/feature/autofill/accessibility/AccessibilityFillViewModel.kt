package com.aozijx.passly.presentation.feature.autofill.accessibility

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
import com.aozijx.passly.domain.autofill.model.AutofillDiscoveryOutcome
import com.aozijx.passly.domain.autofill.model.AutofillGrantContext
import com.aozijx.passly.domain.autofill.model.AutofillInteractionId
import com.aozijx.passly.domain.autofill.model.AutofillInteractionSource
import com.aozijx.passly.domain.autofill.model.AutofillRequest
import com.aozijx.passly.domain.autofill.model.AutofillSelectionOutcome
import com.aozijx.passly.domain.autofill.model.FieldRole
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.feature.autofill.accessibility.AccessibilityFillSessionStore
import com.aozijx.passly.feature.autofill.accessibility.AccessibilityCapture
import com.aozijx.passly.feature.autofill.core.AuthorizeAutofillInteraction
import com.aozijx.passly.feature.autofill.core.DiscoverAutofillCandidates
import com.aozijx.passly.feature.autofill.core.FinishAutofillInteraction
import com.aozijx.passly.feature.autofill.core.ResolveAutofillSelection
import com.aozijx.passly.feature.autofill.runtime.AutofillSelectionReceipt
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface AccessibilityFillUiState {
    data object Loading : AccessibilityFillUiState
    data class Candidates(val values: List<AutofillCandidateSummary>) : AccessibilityFillUiState
    data object Done : AccessibilityFillUiState
}

@HiltViewModel
class AccessibilityFillViewModel @Inject constructor(
    private val sessions: AccessibilityFillSessionStore,
    private val discoverCandidates: DiscoverAutofillCandidates,
    private val authorizeInteraction: AuthorizeAutofillInteraction,
    private val resolveSelection: ResolveAutofillSelection,
    private val finishInteraction: FinishAutofillInteraction,
    private val traceContinuations: TraceContinuationStore,
    private val logger: Logger,
) : ViewModel() {
    private val mutableState = MutableStateFlow<AccessibilityFillUiState>(AccessibilityFillUiState.Loading)
    val uiState: StateFlow<AccessibilityFillUiState> = mutableState.asStateFlow()

    private var active: ActiveSelection? = null
    private var resolving: ActiveSelection? = null
    private var startedToken: String? = null
    @Volatile private var abandoned = false

    private data class ActiveSelection(
        val token: String,
        val interactionId: AutofillInteractionId,
        val request: AutofillRequest,
        val roles: Map<String, FieldRole>,
        val includeOtp: Boolean,
    )

    fun start() {
        val captured = sessions.pendingCapture()
        if (captured !is AccessibilityCapture.Ready) {
            mutableState.value = AccessibilityFillUiState.Done
            return
        }
        val token = captured.token
        if (startedToken == token) return
        startedToken = token
        viewModelScope.launch {
            traceContinuations.continueOrRoot(token, Operations.AutofillAccessibilityRequest) {
                val request = captured.request
                val logFields = LogStatements.Autofill.context(
                    AutofillInteractionSource.ACCESSIBILITY,
                    request.packageName,
                )
                var interactionId: AutofillInteractionId? = null
                try {
                    val startedAt = monotonicTimeMs()
                    logger.log(LogStatements.Autofill.discoveryStarted(logFields))
                    var outcome = discoverCandidates(request)
                    if (outcome is AutofillDiscoveryOutcome.RequiresUnlock) {
                        interactionId = outcome.snapshot.id
                        logger.log(LogStatements.Autofill.authenticationStarted())
                        val authStartedAt = monotonicTimeMs()
                        when (authorizeInteraction(interactionId)) {
                            is AutofillAuthorizationOutcome.Authorized -> {
                                logger.log(LogStatements.Autofill.authenticationCompleted(
                                    monotonicTimeMs() - authStartedAt,
                                ))
                                outcome = discoverCandidates(request, interactionId)
                            }
                            else -> {
                                logger.log(LogStatements.Autofill.fallbackUnavailable(
                                    Reasons.AuthenticationRejected,
                                    logFields,
                                ))
                                finishInteraction.cancel(interactionId)
                                sessions.clearCaptured(token)
                                mutableState.value = AccessibilityFillUiState.Done
                                return@continueOrRoot
                            }
                        }
                    }
                    if (abandoned) {
                        interactionId?.let { finishInteraction.cancel(it) }
                        sessions.clearCaptured(token)
                        return@continueOrRoot
                    }
                    if (outcome is AutofillDiscoveryOutcome.Candidates) {
                        interactionId = outcome.snapshot.id
                        active = ActiveSelection(
                            token,
                            interactionId,
                            request,
                            outcome.context.roleMap,
                            outcome.context.includeOtp && outcome.context.roleMap.values.contains(FieldRole.OTP),
                        )
                        traceContinuations.capture(interactionId.value)
                        logger.log(LogStatements.Autofill.discoveryCompleted(
                            monotonicTimeMs() - startedAt,
                            logFields,
                        ))
                        mutableState.value = AccessibilityFillUiState.Candidates(outcome.candidates)
                    } else {
                        interactionId = when (outcome) {
                            is AutofillDiscoveryOutcome.NoCandidates -> outcome.snapshot.id
                            is AutofillDiscoveryOutcome.RequiresUnlock -> outcome.snapshot.id
                            else -> interactionId
                        }
                        logger.log(LogStatements.Autofill.fallbackUnavailable(
                            Reasons.NoMatchingEntry,
                            logFields,
                        ))
                        interactionId?.let { finishInteraction.cancel(it) }
                        sessions.clearCaptured(token)
                        mutableState.value = AccessibilityFillUiState.Done
                    }
                } catch (cancelled: CancellationException) {
                    withContext(NonCancellable) {
                        interactionId?.let { finishInteraction.cancel(it) }
                        sessions.clearCaptured(token)
                    }
                    throw cancelled
                } catch (error: Exception) {
                    logger.log(LogStatements.Autofill.fallbackFailed(error, logFields))
                    interactionId?.let { finishInteraction.cancel(it) }
                    sessions.clearCaptured(token)
                    mutableState.value = AccessibilityFillUiState.Done
                }
            }
        }
    }

    fun select(entryId: EntryId) {
        val selection = active ?: return
        active = null
        resolving = selection
        mutableState.value = AccessibilityFillUiState.Loading
        viewModelScope.launch {
            traceContinuations.continueOrRoot(
                selection.interactionId.value,
                Operations.AutofillAccessibilityRequest,
            ) {
                val logFields = LogStatements.Autofill.context(
                    AutofillInteractionSource.ACCESSIBILITY,
                    selection.request.packageName,
                )
                var prepared = false
                try {
                    val outcome = resolveSelection(
                        selection.interactionId,
                        AutofillGrantContext(selection.request.packageName),
                        entryId,
                        selection.includeOtp,
                    )
                    if (abandoned) {
                        finishInteraction.cancel(selection.interactionId)
                        return@continueOrRoot
                    }
                    if (outcome is AutofillSelectionOutcome.Filled) {
                        val receipt = AutofillSelectionReceipt(
                            selection.interactionId,
                            outcome.payload.entryId,
                        )
                        check(sessions.prepareDelivery(
                            selection.token,
                            selection.roles,
                            outcome.payload,
                            receipt,
                        )) {
                            "Accessibility target is unavailable"
                        }
                        prepared = true
                        traceContinuations.capture(selection.interactionId.value)
                    } else {
                        logger.log(LogStatements.Autofill.fallbackUnavailable(
                            Reasons.NoMatchingEntry,
                            logFields,
                        ))
                        finishInteraction.cancel(selection.interactionId)
                    }
                } catch (cancelled: CancellationException) {
                    withContext(NonCancellable) {
                        sessions.abortPending(selection.token)
                        finishInteraction.cancel(selection.interactionId)
                    }
                    prepared = false
                    throw cancelled
                } catch (error: Exception) {
                    logger.log(LogStatements.Autofill.fallbackFailed(error, logFields))
                    sessions.abortPending(selection.token)
                    finishInteraction.cancel(selection.interactionId)
                    prepared = false
                } finally {
                    resolving = null
                    sessions.clearCaptured(selection.token)
                    if (!prepared) traceContinuations.discard(selection.interactionId.value)
                    mutableState.value = AccessibilityFillUiState.Done
                }
            }
        }
    }

    fun cancel() {
        viewModelScope.launch { abandon() }
    }

    suspend fun abandon() = withContext(NonCancellable) {
        abandoned = true
        val selection = active ?: resolving
        active = null
        resolving = null
        startedToken?.let(sessions::clearCaptured)
        selection?.let {
            finishInteraction.cancel(it.interactionId)
            traceContinuations.discard(it.interactionId.value)
        }
        mutableState.value = AccessibilityFillUiState.Done
    }
}
