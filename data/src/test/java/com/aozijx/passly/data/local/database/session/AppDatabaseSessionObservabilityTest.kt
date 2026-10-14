package com.aozijx.passly.data.local.database.session

import com.aozijx.passly.core.observability.DiagnosticId
import com.aozijx.passly.core.observability.EventId
import com.aozijx.passly.core.observability.Events
import com.aozijx.passly.core.observability.LogFieldKeys
import com.aozijx.passly.core.observability.LogLevel
import com.aozijx.passly.core.observability.LogValue
import com.aozijx.passly.core.observability.ProcessSessionId
import com.aozijx.passly.core.observability.SanitizedLogRecord
import com.aozijx.passly.core.observability.SpanId
import com.aozijx.passly.core.observability.TraceId
import com.aozijx.passly.core.observability.internal.DefaultLogger
import com.aozijx.passly.core.observability.internal.ObservabilityIdGenerator
import com.aozijx.passly.core.observability.internal.ObservabilityTimeSource
import com.aozijx.passly.core.observability.internal.ProcessSessionProvider
import com.aozijx.passly.core.observability.internal.SecureSessionProvider
import com.aozijx.passly.core.observability.privacy.ContractViolationBehavior
import com.aozijx.passly.core.observability.privacy.IdentifierHasher
import com.aozijx.passly.core.observability.privacy.PrivacyPolicy
import com.aozijx.passly.core.observability.sink.LogSink
import com.aozijx.passly.runtime.session.SecureSessionState
import com.aozijx.passly.runtime.session.SessionLockType
import com.aozijx.passly.runtime.session.SessionTransitionEvent
import com.aozijx.passly.runtime.session.SessionTransitionOwner
import com.aozijx.passly.runtime.session.SessionTransitionReason
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class AppDatabaseSessionObservabilityTest {
    @Test
    fun `successful transition emits required structured database fields`() = runBlocking {
        val records = mutableListOf<SanitizedLogRecord>()
        val adapter = DatabaseSessionEventLogger(logger(records))

        adapter.record(
            SessionTransitionEvent(
                fromState = SecureSessionState.SEALED,
                toState = SecureSessionState.UNLOCKED,
                owner = SessionTransitionOwner.SESSION_RUNTIME,
                reason = SessionTransitionReason.OPEN,
                lockType = SessionLockType.NONE,
                elapsedMs = 17,
            ),
        )

        val record = records.single()
        assertEquals(Events.Database.SessionTransition, record.event)
        assertEquals(LogLevel.INFO, record.level)
        assertEquals("SEALED", (record.fields.values[LogFieldKeys.FromState] as LogValue.EnumValue).value.value)
        assertEquals("UNLOCKED", (record.fields.values[LogFieldKeys.ToState] as LogValue.EnumValue).value.value)
        assertEquals("NONE", (record.fields.values[LogFieldKeys.LockType] as LogValue.EnumValue).value.value)
        assertEquals("SESSION_RUNTIME", (record.fields.values[LogFieldKeys.Owner] as LogValue.EnumValue).value.value)
        assertEquals(17L, (record.fields.values[LogFieldKeys.ElapsedMs] as LogValue.DurationMs).value)
        assertFalse(record.fields.values.containsKey(LogFieldKeys.TimeoutMs))
        assertNull(record.error)
    }

    @Test
    fun `close failure emits safe error while drain timeout is throwable free degradation`() = runBlocking {
        val records = mutableListOf<SanitizedLogRecord>()
        val adapter = DatabaseSessionEventLogger(logger(records))
        val failure = IllegalStateException("database path and secret must not escape")

        adapter.record(
            SessionTransitionEvent(
                SecureSessionState.SEALED,
                SecureSessionState.SEALED,
                SessionTransitionOwner.SESSION_RUNTIME,
                SessionTransitionReason.CLOSE_FAILED,
                SessionLockType.SEALED,
                elapsedMs = 9,
                error = failure,
            ),
        )
        adapter.record(
            SessionTransitionEvent(
                SecureSessionState.UNLOCKED,
                SecureSessionState.SEALED,
                SessionTransitionOwner.SESSION_RUNTIME,
                SessionTransitionReason.DRAIN_TIMEOUT,
                SessionLockType.SEALED,
                elapsedMs = 25,
                timeoutMs = 20,
                activeLeaseCount = 2,
            ),
        )

        val error = records.first()
        assertEquals(Events.Database.SessionTransitionFailed, error.event)
        assertEquals(LogLevel.ERROR, error.level)
        assertEquals("IllegalStateException", error.error!!.exception!!.type.value)
        assertFalse(error.toString().contains("secret"))

        val timeout = records.last()
        assertEquals(Events.Database.SessionTransitionFailed, timeout.event)
        assertEquals(LogLevel.WARN, timeout.level)
        assertNull(timeout.error!!.exception)
        assertEquals(20L, (timeout.fields.values[LogFieldKeys.TimeoutMs] as LogValue.DurationMs).value)
        assertEquals(2L, (timeout.fields.values[LogFieldKeys.ActiveLeaseCount] as LogValue.Count).value)
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
            override fun wallTimeMs() = 1_700_000_000_000L
            override fun elapsedRealtimeMs() = 100L
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
        override fun diagnosticId() = DiagnosticId.fromEntropy(ByteArray(8) { (++next + it).toByte() })
    }
}
