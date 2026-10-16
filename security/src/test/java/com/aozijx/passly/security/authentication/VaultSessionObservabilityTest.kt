package com.aozijx.passly.security.authentication

import com.aozijx.passly.core.observability.EventId
import com.aozijx.passly.core.observability.Events
import com.aozijx.passly.core.observability.ExceptionKind
import com.aozijx.passly.core.observability.LogFieldKeys
import com.aozijx.passly.core.observability.LogLevel
import com.aozijx.passly.core.observability.LogValue
import com.aozijx.passly.core.observability.ProcessSessionId
import com.aozijx.passly.core.observability.SanitizedLogRecord
import com.aozijx.passly.core.observability.SpanId
import com.aozijx.passly.core.observability.TraceId
import com.aozijx.passly.core.observability.Operations
import com.aozijx.passly.core.observability.internal.DefaultLogger
import com.aozijx.passly.core.observability.internal.ObservabilityIdGenerator
import com.aozijx.passly.core.observability.internal.ObservabilityTimeSource
import com.aozijx.passly.core.observability.internal.ProcessSessionProvider
import com.aozijx.passly.core.observability.internal.SecureSessionProvider
import com.aozijx.passly.core.observability.privacy.ContractViolationBehavior
import com.aozijx.passly.core.observability.privacy.IdentifierHasher
import com.aozijx.passly.core.observability.privacy.PrivacyPolicy
import com.aozijx.passly.core.observability.sink.LogSink
import com.aozijx.passly.domain.access.model.AuthenticationMethod
import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.AuthenticationFailure
import com.aozijx.passly.domain.access.model.AuthenticationFailureCode
import com.aozijx.passly.domain.access.model.AuthenticationRequestId
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.model.LockReason
import com.aozijx.passly.domain.access.model.LockTrigger
import com.aozijx.passly.domain.access.model.SessionLockRequest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultSessionObservabilityTest {
    @Test
    fun `lock start and completion share trace and expose controlled lifecycle fields`() = runTest {
        val records = mutableListOf<SanitizedLogRecord>()
        val events = SecuritySessionEventLogger(logger(records))

        events.traceLock(
            SessionLockRequest(LockReason.BACKGROUND, LockTrigger.APP_BACKGROUND),
            timeoutMs = 5_000,
        ) { }

        assertEquals(
            listOf(Events.Security.LockStarted, Events.Security.LockCompleted),
            records.map(SanitizedLogRecord::event),
        )
        assertEquals(1, records.map { it.trace.traceId }.distinct().size)
        assertEquals(listOf(LogLevel.INFO, LogLevel.INFO), records.map(SanitizedLogRecord::level))
        val completed = records.last()
        assertEquals(
            "BACKGROUND",
            (completed.fields.values[LogFieldKeys.LockReason] as LogValue.ReasonCodeValue).value.value,
        )
        assertEquals(
            "APP_BACKGROUND",
            (completed.fields.values[LogFieldKeys.Trigger] as LogValue.EnumValue).value.value,
        )
        assertEquals(5_000L, (completed.fields.values[LogFieldKeys.TimeoutMs] as LogValue.DurationMs).value)
        assertEquals("SUCCESS", (completed.fields.values[LogFieldKeys.Result] as LogValue.EnumValue).value.value)
    }

    @Test
    fun `security span inherits an existing operation trace`() = runTest {
        val records = mutableListOf<SanitizedLogRecord>()
        val logger = logger(records)
        val events = SecuritySessionEventLogger(logger)
        var operationTraceId: TraceId? = null

        logger.withRootTrace(Operations.AutofillLegacyRequest) {
            operationTraceId = traceId
            events.traceLock(
                SessionLockRequest(
                    LockReason.AUTOFILL_REQUEST_FINISHED,
                    LockTrigger.AUTOFILL_COMPLETION,
                ),
                timeoutMs = 5_000,
            ) { }
        }

        assertTrue(records.all { it.trace.traceId == operationTraceId })
        assertEquals(1, records.map { it.trace.spanId }.distinct().size)
        assertFalse(records.first().trace.parentSpanId == null)
    }

    @Test
    fun `lock exception is safely logged and original failure is rethrown`() {
        val records = mutableListOf<SanitizedLogRecord>()
        val events = SecuritySessionEventLogger(logger(records))
        val failure = IllegalStateException("master password must never escape")

        val thrown = assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking {
                events.traceLock(
                    SessionLockRequest(LockReason.APP_EXIT, LockTrigger.APP_EXIT),
                    timeoutMs = 5_000,
                ) { throw failure }
            }
        }

        assertSame(failure, thrown)
        val error = records.last()
        assertEquals(Events.Security.LockCompleted, error.event)
        assertEquals(LogLevel.ERROR, error.level)
        assertEquals("FAILURE", (error.fields.values[LogFieldKeys.Result] as LogValue.EnumValue).value.value)
        assertEquals("IllegalStateException", error.error!!.exception!!.type.value)
        assertEquals(ExceptionKind.LIFECYCLE, error.error!!.exception!!.kind)
        assertFalse(error.toString().contains("master password"))
    }

    @Test
    fun `authentication completion records normalized decryption method`() = runTest {
        val records = mutableListOf<SanitizedLogRecord>()
        val events = SecuritySessionEventLogger(logger(records))

        events.traceAuthentication(AuthenticationPurpose.UNLOCK_VAULT, AuthenticationMethod.APP_PASSWORD) { }

        assertEquals(
            listOf(Events.Security.UnlockStarted, Events.Security.UnlockCompleted),
            records.map(SanitizedLogRecord::event),
        )
        assertEquals(1, records.map { it.trace.traceId }.distinct().size)
        assertEquals(
            "MASTER_PASSWORD",
            (records.last().fields.values[LogFieldKeys.AuthenticationMethod] as LogValue.EnumValue).value.value,
        )
        assertEquals(
            "VAULT_UNLOCK",
            (records.last().fields.values[LogFieldKeys.AuthenticationPurpose] as LogValue.EnumValue).value.value,
        )
    }

    @Test
    fun `handled lock failure is error outcome without changing cleanup control flow`() = runTest {
        val records = mutableListOf<SanitizedLogRecord>()
        val events = SecuritySessionEventLogger(logger(records))
        val failure = IllegalStateException("close failed")

        events.traceLock(
            SessionLockRequest(LockReason.BACKGROUND, LockTrigger.APP_BACKGROUND),
            timeoutMs = 5_000,
        ) {
            recordFailure(failure)
        }

        val completed = records.last()
        assertEquals(LogLevel.ERROR, completed.level)
        assertEquals("FAILURE", (completed.fields.values[LogFieldKeys.Result] as LogValue.EnumValue).value.value)
        assertEquals("IllegalStateException", completed.error!!.exception!!.type.value)
    }

    @Test
    fun `caught authentication exception is emitted once as error`() = runTest {
        val records = mutableListOf<SanitizedLogRecord>()
        val events = SecuritySessionEventLogger(logger(records))
        val failure = IllegalStateException("credential secret")

        events.traceAuthentication(AuthenticationPurpose.AUTOFILL) {
            recordFailure(failure)
            AuthenticationResult.Failure(
                AuthenticationFailure(
                    AuthenticationFailureCode.SESSION_TRANSITION_FAILED,
                    AuthenticationRequestId("request"),
                ),
            )
        }

        val failures = records.filter { it.event == Events.Security.AuthenticationFailed }
        assertEquals(1, failures.size)
        assertEquals(LogLevel.ERROR, failures.single().level)
        assertEquals(
            "AUTOFILL",
            (failures.single().fields.values[LogFieldKeys.AuthenticationPurpose] as LogValue.EnumValue).value.value,
        )
        assertFalse(failures.single().toString().contains("credential secret"))
    }

    private fun logger(records: MutableList<SanitizedLogRecord>) = DefaultLogger(
        sanitizer = PrivacyPolicy(
            IdentifierHasher { _, _ -> null },
            ContractViolationBehavior.THROW,
        ),
        sinks = listOf(LogSink(records::add)),
        lifecycleSinks = emptyList(),
        idGenerator = SequentialIds(),
        timeSource = object : ObservabilityTimeSource {
            private var elapsed = 100L
            override fun wallTimeMs() = 1_700_000_000_000L
            override fun elapsedRealtimeMs() = elapsed++
        },
        processSessionProvider = ProcessSessionProvider { ProcessSessionId("process_session") },
        secureSessionProvider = SecureSessionProvider { null },
    )

    private class SequentialIds : ObservabilityIdGenerator {
        private var next = 0
        private fun value(prefix: String) = "${prefix}_${++next}_value"
        override fun eventId() = EventId(value("event"))
        override fun traceId() = TraceId(value("trace"))
        override fun spanId() = SpanId(value("span"))
    }
}
