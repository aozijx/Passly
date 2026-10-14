package com.aozijx.passly.presentation.feature.autofill.legacy.save

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
) : ViewModel() {
    @Inject
    constructor(flow: LegacySaveFlow) : this(
        flow::beginContinuation,
        flow::completeContinuation,
        flow::cancel,
    )

    private val mutableState = MutableStateFlow<LegacySaveUiState>(LegacySaveUiState.Loading)
    val state: StateFlow<LegacySaveUiState> = mutableState.asStateFlow()

    private val launchStarted = AtomicBoolean(false)
    private var activeContinuation: LegacySaveContinuation? = null

    fun onToken(rawToken: String?) {
        if (!launchStarted.compareAndSet(false, true)) return
        val token = rawToken?.takeIf(String::isNotBlank)?.let(::LegacySaveToken)
        if (token == null) {
            mutableState.value = LegacySaveUiState.Unrecoverable
            return
        }
        viewModelScope.launch {
            try {
                when (val begin = beginContinuation(token)) {
                    is LegacySaveBeginResult.Ready -> complete(begin.continuation)
                    LegacySaveBeginResult.Disabled,
                    LegacySaveBeginResult.Unavailable,
                    -> mutableState.value = LegacySaveUiState.Unrecoverable
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                cancelActiveInteraction()
                mutableState.value = LegacySaveUiState.Unrecoverable
            }
        }
    }

    fun cancelActiveInteraction() {
        val interactionId = activeContinuation?.interactionId ?: return
        activeContinuation = null
        viewModelScope.launch {
            withContext(NonCancellable) { cancelInteraction(interactionId) }
        }
    }

    private suspend fun complete(continuation: LegacySaveContinuation) {
        activeContinuation = continuation
        val completion = completeContinuation(continuation)
        activeContinuation = null
        mutableState.value = when (completion) {
            LegacySaveCompletion.Saved -> LegacySaveUiState.Saved
            LegacySaveCompletion.NotAuthorized,
            LegacySaveCompletion.NotSaved,
            -> LegacySaveUiState.Unrecoverable
        }
    }
}
