package com.aozijx.passly.presentation.feature.autofill.legacy.save

import android.os.Parcelable
import com.aozijx.passly.domain.autofill.model.AutofillGrantContext
import com.aozijx.passly.domain.autofill.model.AutofillInteractionId
import com.aozijx.passly.domain.autofill.model.AutofillSaveDraft
import com.aozijx.passly.feature.autofill.legacy.LegacySaveBeginResult
import com.aozijx.passly.feature.autofill.legacy.LegacySaveCompletion
import com.aozijx.passly.feature.autofill.legacy.LegacySaveContinuation
import com.aozijx.passly.feature.autofill.legacy.runtime.LegacySaveToken
import com.aozijx.passly.testing.MainDispatcherRule
import java.io.Serializable
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LegacySaveViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun launchIsHandledOnceAndContinuationSurvivesRepeatedRouteEffects() = runTest {
        var beginCalls = 0
        val completion = CompletableDeferred<LegacySaveCompletion>()
        val viewModel = viewModel(
            begin = {
                beginCalls++
                LegacySaveBeginResult.Ready(continuation())
            },
            complete = { completion.await() },
        )

        viewModel.onToken("opaque")
        viewModel.onToken("opaque")
        runCurrent()

        assertEquals(1, beginCalls)
        assertEquals(LegacySaveUiState.Loading, viewModel.state.value)

        completion.complete(LegacySaveCompletion.Saved)
        runCurrent()

        assertEquals(LegacySaveUiState.Saved, viewModel.state.value)
    }

    @Test
    fun terminalSuccessClearsContinuationAndBackCancellationIsIdempotent() = runTest {
        val completion = CompletableDeferred<LegacySaveCompletion>()
        var cancellations = 0
        val viewModel = viewModel(
            begin = { LegacySaveBeginResult.Ready(continuation()) },
            complete = { completion.await() },
            cancel = { cancellations++ },
        )
        viewModel.onToken("opaque")
        runCurrent()

        viewModel.cancelActiveInteraction()
        viewModel.cancelActiveInteraction()
        runCurrent()
        assertEquals(1, cancellations)

        completion.complete(LegacySaveCompletion.Saved)
        runCurrent()
        viewModel.cancelActiveInteraction()
        runCurrent()
        assertEquals(1, cancellations)
    }

    @Test
    fun missingOrConsumedTokenIsUnrecoverable() = runTest {
        val viewModel = viewModel(begin = { LegacySaveBeginResult.Unavailable })

        viewModel.onToken(null)
        runCurrent()

        assertEquals(LegacySaveUiState.Unrecoverable, viewModel.state.value)
    }

    @Test
    fun uiStateNeverCarriesParcelableOrSerializablePlaintext() {
        listOf(
            LegacySaveUiState.Loading,
            LegacySaveUiState.Saved,
            LegacySaveUiState.Unrecoverable,
        ).forEach { state ->
            assertFalse(state is Parcelable)
            assertFalse(state is Serializable)
        }
    }

    private fun viewModel(
        begin: suspend (LegacySaveToken) -> LegacySaveBeginResult,
        complete: suspend (LegacySaveContinuation) -> LegacySaveCompletion = {
            LegacySaveCompletion.NotSaved
        },
        cancel: suspend (AutofillInteractionId) -> Unit = {},
    ) = LegacySaveViewModel(begin, complete, cancel)

    private fun continuation() = LegacySaveContinuation(
        interactionId = AutofillInteractionId("legacy-save"),
        draft = AutofillSaveDraft(
            packageName = "com.example",
            webDomain = "example.com",
            pageTitle = null,
            username = "user",
            password = "secret",
        ),
        scope = AutofillGrantContext("com.example", "example.com").normalized(),
    )
}
