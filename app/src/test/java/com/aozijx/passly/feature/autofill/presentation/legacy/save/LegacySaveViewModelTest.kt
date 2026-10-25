package com.aozijx.passly.feature.autofill.presentation.legacy.save

import android.os.Parcelable
import com.aozijx.passly.core.observability.Events
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.Operations
import com.aozijx.passly.core.observability.ProcessSessionId
import com.aozijx.passly.core.observability.Reasons
import com.aozijx.passly.core.observability.SanitizedLogRecord
import com.aozijx.passly.core.observability.internal.DefaultLogger
import com.aozijx.passly.core.observability.internal.ObservabilityTimeSource
import com.aozijx.passly.core.observability.internal.ProcessSessionProvider
import com.aozijx.passly.core.observability.internal.SecureObservabilityIdGenerator
import com.aozijx.passly.core.observability.internal.SecureSessionProvider
import com.aozijx.passly.core.observability.privacy.ContractViolationBehavior
import com.aozijx.passly.core.observability.privacy.IdentifierHasher
import com.aozijx.passly.core.observability.privacy.PrivacyPolicy
import com.aozijx.passly.core.observability.sink.LogSink
import com.aozijx.passly.domain.autofill.model.AutofillGrantContext
import com.aozijx.passly.domain.autofill.model.AutofillInteractionId
import com.aozijx.passly.domain.autofill.model.AutofillSaveDraft
import com.aozijx.passly.feature.autofill.legacy.LegacySaveBeginResult
import com.aozijx.passly.feature.autofill.legacy.LegacySaveCompletion
import com.aozijx.passly.feature.autofill.legacy.LegacySaveContinuation
import com.aozijx.passly.feature.autofill.legacy.runtime.LegacySaveToken
import com.aozijx.passly.testing.MainDispatcherRule
import com.aozijx.passly.testing.RecordingTestLogger
import java.io.Serializable
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun terminalOutcomesLogAutofillSaveEventsWithinLegacyRequestSpan() = runTest {
        val savedLogger = RecordingTestLogger()
        val savedViewModel = viewModel(
            begin = { LegacySaveBeginResult.Ready(continuation()) },
            complete = { LegacySaveCompletion.Saved },
            logger = savedLogger,
        )
        savedViewModel.onToken("opaque")
        runCurrent()

        assertEquals(
            listOf(Events.Autofill.SaveCompleted),
            savedLogger.events.map { it.event },
        )
        assertEquals(listOf(Operations.AutofillLegacyRequest), savedLogger.spans)

        val unauthorizedLogger = RecordingTestLogger()
        val unauthorizedViewModel = viewModel(
            begin = { LegacySaveBeginResult.Ready(continuation()) },
            complete = { LegacySaveCompletion.NotAuthorized },
            logger = unauthorizedLogger,
        )
        unauthorizedViewModel.onToken("opaque")
        runCurrent()

        assertEquals(listOf(Events.Autofill.SaveFailed), unauthorizedLogger.events.map { it.event })
        assertEquals(Reasons.AuthenticationRejected, unauthorizedLogger.events.single().reason)
        assertEquals(LegacySaveUiState.Unrecoverable, unauthorizedViewModel.state.value)

        val notSavedLogger = RecordingTestLogger()
        val notSavedViewModel = viewModel(
            begin = { LegacySaveBeginResult.Ready(continuation()) },
            complete = { LegacySaveCompletion.NotSaved },
            logger = notSavedLogger,
        )
        notSavedViewModel.onToken("opaque")
        runCurrent()

        assertEquals(Reasons.StorageFailure, notSavedLogger.events.single().reason)
        assertEquals(LegacySaveUiState.Unrecoverable, notSavedViewModel.state.value)

        val disabledLogger = RecordingTestLogger()
        val disabledViewModel = viewModel(
            begin = { LegacySaveBeginResult.Disabled },
            logger = disabledLogger,
        )
        disabledViewModel.onToken("opaque")
        runCurrent()

        assertEquals(Reasons.SessionRestricted, disabledLogger.events.single().reason)
        assertEquals(LegacySaveUiState.Unrecoverable, disabledViewModel.state.value)
    }

    @Test
    fun completeFailureLogsSaveFailedWithThrowableAndCancelsInteraction() = runTest {
        val logger = RecordingTestLogger()
        var cancellations = 0
        val viewModel = viewModel(
            begin = { LegacySaveBeginResult.Ready(continuation()) },
            complete = { error("complete failed") },
            cancel = { cancellations++ },
            logger = logger,
        )

        viewModel.onToken("opaque")
        runCurrent()

        val recorded = logger.events.single()
        assertEquals(Events.Autofill.SaveFailed, recorded.event)
        assertTrue(recorded.throwable is IllegalStateException)
        assertEquals(Reasons.StorageFailure, recorded.reason)
        assertEquals(1, cancellations)
        assertEquals(LegacySaveUiState.Unrecoverable, viewModel.state.value)
    }

    @Test
    fun missingTokenLogsSaveUnavailable() = runTest {
        val logger = RecordingTestLogger()
        val viewModel = viewModel(begin = { LegacySaveBeginResult.Unavailable }, logger = logger)

        viewModel.onToken(null)
        runCurrent()

        assertEquals(listOf(Events.Autofill.SaveFailed), logger.events.map { it.event })
        assertEquals(Reasons.UnknownFailure, logger.events.single().reason)
        assertEquals(LegacySaveUiState.Unrecoverable, viewModel.state.value)
    }

    @Test
    fun cancelInteractionFailureIsLoggedAsSessionCloseFailed() = runTest {
        val logger = RecordingTestLogger()
        val completion = CompletableDeferred<LegacySaveCompletion>()
        val viewModel = viewModel(
            begin = { LegacySaveBeginResult.Ready(continuation()) },
            complete = { completion.await() },
            cancel = { error("cancel failed") },
            logger = logger,
        )
        viewModel.onToken("opaque")
        runCurrent()

        viewModel.cancelActiveInteraction()
        runCurrent()

        val recorded = logger.events.last()
        assertEquals(Events.Autofill.SessionCloseFailed, recorded.event)
        assertTrue(recorded.throwable is IllegalStateException)
    }

    @Test
    fun completionFailureKeepsCleanupFailureInTheSaveTrace() = runTest {
        val records = mutableListOf<SanitizedLogRecord>()
        val viewModel = viewModel(
            begin = { LegacySaveBeginResult.Ready(continuation()) },
            complete = { error("save failed") },
            cancel = { error("close failed") },
            logger = productionLogger(records),
        )

        viewModel.onToken("opaque")
        runCurrent()

        assertEquals(
            listOf(Events.Autofill.SaveFailed, Events.Autofill.SessionCloseFailed),
            records.map { it.event },
        )
        assertEquals(records.first().trace.traceId, records.last().trace.traceId)
        assertEquals(records.first().trace.spanId, records.last().trace.parentSpanId)
    }

    @Test
    fun exitCleanupFailureHasItsOwnTrace() = runTest {
        val records = mutableListOf<SanitizedLogRecord>()
        val completion = CompletableDeferred<LegacySaveCompletion>()
        val viewModel = viewModel(
            begin = { LegacySaveBeginResult.Ready(continuation()) },
            complete = { completion.await() },
            cancel = { error("close failed") },
            logger = productionLogger(records),
        )

        viewModel.onToken("opaque")
        runCurrent()
        viewModel.cancelActiveInteraction()
        runCurrent()

        assertEquals(listOf(Events.Autofill.SessionCloseFailed), records.map { it.event })
    }

    private fun viewModel(
        begin: suspend (LegacySaveToken) -> LegacySaveBeginResult,
        complete: suspend (LegacySaveContinuation) -> LegacySaveCompletion = {
            LegacySaveCompletion.NotSaved
        },
        cancel: suspend (AutofillInteractionId) -> Unit = {},
        logger: Logger = RecordingTestLogger(),
    ) = LegacySaveViewModel(begin, complete, cancel, logger)

    private fun productionLogger(records: MutableList<SanitizedLogRecord>): Logger = DefaultLogger(
        sanitizer = PrivacyPolicy(IdentifierHasher { _, _ -> null }, ContractViolationBehavior.THROW),
        sinks = listOf(LogSink(records::add)),
        lifecycleSinks = emptyList(),
        idGenerator = SecureObservabilityIdGenerator(),
        timeSource = object : ObservabilityTimeSource {
            override fun wallTimeMs(): Long = 1_700_000_000_000
            override fun elapsedRealtimeMs(): Long = 100
        },
        processSessionProvider = ProcessSessionProvider { ProcessSessionId("test_process") },
        secureSessionProvider = SecureSessionProvider { null },
    )

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
