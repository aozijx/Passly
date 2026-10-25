package com.aozijx.passly.feature.autofill.presentation.legacy.save

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.LogStatements
import com.aozijx.passly.core.observability.Operations
import com.aozijx.passly.core.observability.ReasonCode
import com.aozijx.passly.core.observability.Reasons
import com.aozijx.passly.core.observability.monotonicTimeMs
import com.aozijx.passly.domain.autofill.model.AutofillInteractionId
import com.aozijx.passly.feature.autofill.legacy.LegacySaveBeginResult
import com.aozijx.passly.feature.autofill.legacy.LegacySaveCompletion
import com.aozijx.passly.feature.autofill.legacy.LegacySaveContinuation
import com.aozijx.passly.feature.autofill.legacy.LegacySaveFlow
import com.aozijx.passly.feature.autofill.legacy.runtime.LegacySaveToken
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class LegacySaveViewModel internal constructor(
    private val beginContinuation: suspend (LegacySaveToken) -> LegacySaveBeginResult,
    private val completeContinuation: suspend (LegacySaveContinuation) -> LegacySaveCompletion,
    private val cancelInteraction: suspend (AutofillInteractionId) -> Unit,
    private val logger: Logger,
) : ViewModel() {
    @Inject
    constructor(flow: LegacySaveFlow, logger: Logger) : this(
        flow::beginContinuation,
        flow::completeContinuation,
        flow::cancel,
        logger,
    )

    private val mutableState = MutableStateFlow<LegacySaveUiState>(LegacySaveUiState.Loading)
    val state: StateFlow<LegacySaveUiState> = mutableState.asStateFlow()

    private val launchStarted = AtomicBoolean(false)
    private var activeContinuation: LegacySaveContinuation? = null

    fun onToken(rawToken: String?) {
        if (!launchStarted.compareAndSet(false, true)) return
        val token = rawToken?.takeIf(String::isNotBlank)?.let(::LegacySaveToken)
        viewModelScope.launch {
            logger.withRootTrace(Operations.AutofillLegacyRequest) {
                if (token == null) {
                    logger.log(LogStatements.Autofill.saveUnavailable(Reasons.UnknownFailure))
                    mutableState.value = LegacySaveUiState.Unrecoverable
                    return@withRootTrace
                }
                try {
                    when (val begin = beginContinuation(token)) {
                        is LegacySaveBeginResult.Ready -> complete(begin.continuation)
                        LegacySaveBeginResult.Disabled -> saveUnavailable(Reasons.SessionRestricted)
                        LegacySaveBeginResult.Unavailable -> saveUnavailable(Reasons.UnknownFailure)
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    logger.log(LogStatements.Autofill.saveFailed(error))
                    val interactionId = takeActiveInteractionId()
                    if (interactionId != null) {
                        logger.inSpan(Operations.AutofillSessionClose) {
                            closeInteraction(interactionId)
                        }
                    }
                    mutableState.value = LegacySaveUiState.Unrecoverable
                }
            }
        }
    }

    fun cancelActiveInteraction() {
        val interactionId = takeActiveInteractionId() ?: return
        viewModelScope.launch {
            logger.withRootTrace(Operations.AutofillSessionClose) {
                closeInteraction(interactionId)
            }
        }
    }

    private fun takeActiveInteractionId(): AutofillInteractionId? =
        activeContinuation?.interactionId.also { activeContinuation = null }

    private suspend fun closeInteraction(interactionId: AutofillInteractionId) {
        val startedAt = monotonicTimeMs()
        try {
            withContext(NonCancellable) { cancelInteraction(interactionId) }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            logger.log(
                LogStatements.Autofill.sessionCloseFailed(error, monotonicTimeMs() - startedAt),
            )
        }
    }

    private suspend fun complete(continuation: LegacySaveContinuation) {
        activeContinuation = continuation
        val completion = completeContinuation(continuation)
        activeContinuation = null
        mutableState.value = when (completion) {
            LegacySaveCompletion.Saved -> {
                logger.log(LogStatements.Autofill.saveCompleted())
                LegacySaveUiState.Saved
            }
            LegacySaveCompletion.NotAuthorized -> {
                logger.log(LogStatements.Autofill.saveUnavailable(Reasons.AuthenticationRejected))
                LegacySaveUiState.Unrecoverable
            }
            LegacySaveCompletion.NotSaved -> {
                logger.log(LogStatements.Autofill.saveUnavailable(Reasons.StorageFailure))
                LegacySaveUiState.Unrecoverable
            }
        }
    }

    private suspend fun saveUnavailable(reason: ReasonCode) {
        logger.log(LogStatements.Autofill.saveUnavailable(reason))
        mutableState.value = LegacySaveUiState.Unrecoverable
    }
}
