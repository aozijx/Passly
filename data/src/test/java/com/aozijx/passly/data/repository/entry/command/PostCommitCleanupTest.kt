package com.aozijx.passly.data.repository.entry.command

import com.aozijx.passly.core.error.model.SessionModeRestricted
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.core.observability.ErrorCode
import com.aozijx.passly.core.observability.EventKey
import com.aozijx.passly.core.observability.LogFields
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.OperationCode
import com.aozijx.passly.core.observability.ReasonCode
import com.aozijx.passly.core.observability.Reasons
import com.aozijx.passly.core.observability.SpanId
import com.aozijx.passly.core.observability.TraceId
import com.aozijx.passly.core.observability.TraceResumeResult
import com.aozijx.passly.core.observability.TraceScope
import com.aozijx.passly.core.observability.trace.TraceToken
import java.util.concurrent.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class PostCommitCleanupTest {
    @Test
    fun ordinaryCleanupFailureKeepsCommittedResultAndLogsFailure() = kotlinx.coroutines.test.runTest {
        val logger = RecordingLogger()
        val cleanup = PostCommitCleanup(logger)
        val committed = AppResult.success("entry-id")
        val failure = IllegalStateException("cleanup failed")

        val result = cleanup.afterCommit(committed) { throw failure }

        assertSame(committed, result)
        assertEquals(listOf(failure), logger.warnings)
        assertEquals(listOf(Reasons.StorageFailure), logger.reasons)
    }

    @Test
    fun failedTransactionDoesNotRunCleanup() = kotlinx.coroutines.test.runTest {
        val logger = RecordingLogger()
        val cleanup = PostCommitCleanup(logger)
        val failed = AppResult.failure(SessionModeRestricted())
        var invoked = false

        val result = cleanup.afterCommit(failed) { invoked = true }

        assertSame(failed, result)
        assertEquals(false, invoked)
        assertEquals(emptyList<Throwable>(), logger.warnings)
    }

    @Test
    fun cancellationAndFatalErrorsStillPropagate() {
        val cleanup = PostCommitCleanup(RecordingLogger())
        val cancellation = CancellationException("caller left")
        val fatal = AssertionError("fatal")

        assertSame(cancellation, assertThrows(CancellationException::class.java) {
            kotlinx.coroutines.runBlocking { cleanup.run { throw cancellation } }
        })
        assertSame(fatal, assertThrows(AssertionError::class.java) {
            kotlinx.coroutines.runBlocking { cleanup.run { throw fatal } }
        })
    }

    @Test
    fun laterCleanupPhaseStillRunsAfterAnEarlierPhaseFails() = kotlinx.coroutines.test.runTest {
        val cleanup = PostCommitCleanup(RecordingLogger())
        var iconCleanupRan = false

        cleanup.run { throw IllegalStateException("attachment cleanup failed") }
        cleanup.run { iconCleanupRan = true }

        assertEquals(true, iconCleanupRan)
    }

    @Test
    fun ordinaryLoggingFailureIsSuppressedOnCleanupFailure() = kotlinx.coroutines.test.runTest {
        val cleanupFailure = IllegalStateException("cleanup failed")
        val loggerFailure = IllegalArgumentException("logging failed")
        val cleanup = PostCommitCleanup(RecordingLogger { throw loggerFailure })

        cleanup.run { throw cleanupFailure }

        assertEquals(listOf(loggerFailure), cleanupFailure.suppressed.toList())
    }

    @Test
    fun fatalLoggingFailurePropagates() {
        val fatal = AssertionError("fatal logger failure")
        val cleanup = PostCommitCleanup(RecordingLogger { throw fatal })

        assertSame(fatal, assertThrows(AssertionError::class.java) {
            kotlinx.coroutines.runBlocking { cleanup.run { throw IllegalStateException("cleanup failed") } }
        })
    }

    private class RecordingLogger(
        private val onWarn: () -> Unit = {},
    ) : Logger {
        val warnings = mutableListOf<Throwable>()
        val reasons = mutableListOf<ReasonCode>()

        override fun debug(event: EventKey, fields: LogFields) = Unit
        override fun info(event: EventKey, fields: LogFields) = Unit
        override fun degraded(event: EventKey, errorCode: ErrorCode, reason: ReasonCode, fields: LogFields) = Unit
        override fun warn(event: EventKey, errorCode: ErrorCode, reason: ReasonCode, throwable: Throwable, fields: LogFields) {
            warnings += throwable
            reasons += reason
            onWarn()
        }
        override fun error(event: EventKey, errorCode: ErrorCode, reason: ReasonCode, throwable: Throwable, fields: LogFields) = Unit
        override fun fatal(event: EventKey, errorCode: ErrorCode, reason: ReasonCode, throwable: Throwable, fields: LogFields) = Unit

        override suspend fun <T> withRootTrace(
            operation: OperationCode,
            block: suspend TraceScope.() -> T,
        ): T = TestTraceScope.block()

        override suspend fun <T> inSpan(
            operation: OperationCode,
            block: suspend TraceScope.() -> T,
        ): T = TestTraceScope.block()

        override fun captureTraceToken(singleUse: Boolean): TraceToken = error("Unused")

        override suspend fun <T> resumeTrace(
            token: TraceToken,
            operation: OperationCode,
            block: suspend TraceScope.() -> T,
        ): TraceResumeResult<T> = error("Unused")
    }

    private object TestTraceScope : TraceScope {
        override val traceId = TraceId("post_commit_cleanup")
        override val spanId = SpanId("post_commit_cleanup")
        override val parentSpanId: SpanId? = null
    }
}
