package com.aozijx.passly.core.observability.internal

import com.aozijx.passly.core.observability.ErrorCodes
import com.aozijx.passly.core.observability.EventId
import com.aozijx.passly.core.observability.Events
import com.aozijx.passly.core.observability.ExceptionKind
import com.aozijx.passly.core.observability.LogStatements
import com.aozijx.passly.core.observability.Operations
import com.aozijx.passly.core.observability.ProcessSessionId
import com.aozijx.passly.core.observability.Reasons
import com.aozijx.passly.core.observability.SanitizedLogRecord
import com.aozijx.passly.core.observability.SecureSessionId
import com.aozijx.passly.core.observability.SpanId
import com.aozijx.passly.core.observability.TraceId
import com.aozijx.passly.core.observability.privacy.ContractViolationBehavior
import com.aozijx.passly.core.observability.privacy.IdentifierHasher
import com.aozijx.passly.core.observability.privacy.LogSanitizer
import com.aozijx.passly.core.observability.privacy.PrivacyPolicy
import com.aozijx.passly.core.observability.sink.LogSink
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class DefaultLoggerTest {
    @Test
    fun `privacy policy runs once and all sinks receive the same immutable record`() = runTest {
        val recordsA = mutableListOf<SanitizedLogRecord>()
        val recordsB = mutableListOf<SanitizedLogRecord>()
        val delegate = PrivacyPolicy(IdentifierHasher { _, _ -> null }, ContractViolationBehavior.REJECT)
        var sanitizeCount = 0
        val sanitizer = LogSanitizer { event, context ->
            sanitizeCount += 1
            delegate.sanitize(event, context)
        }
        val logger = logger(sanitizer, listOf(LogSink(recordsA::add), LogSink(recordsB::add)))

        logger.withRootTrace(Operations.ApplicationStartup) {
            logger.info(Events.Application.BackgroundLockSkipped)
        }

        assertEquals(1, sanitizeCount)
        assertEquals(1, recordsA.size)
        assertSame(recordsA.single(), recordsB.single())
    }

    @Test
    fun `child failures share the operation trace without a second correlation identifier`() = runTest {
        val records = mutableListOf<SanitizedLogRecord>()
        val logger = logger(
            PrivacyPolicy(IdentifierHasher { _, _ -> null }, ContractViolationBehavior.REJECT),
            listOf(LogSink(records::add)),
        )
        logger.withRootTrace(Operations.AutofillLegacyRequest) {
            logger.error(
                Events.Autofill.ResultDeliveryFailed,
                ErrorCodes.AutofillResultDeliveryFailed,
                Reasons.PlatformCallbackFailed,
                IllegalStateException("first"),
            )
            logger.inSpan(Operations.AutofillSessionClose) {
                logger.warn(
                    Events.Autofill.SessionCloseFailed,
                    ErrorCodes.AutofillSessionCloseFailed,
                    Reasons.SessionCloseFailed,
                    IllegalStateException("second"),
                )
            }
        }

        assertEquals(1, records.map { it.trace.traceId }.distinct().size)
        assertEquals(records.first().trace.traceId, records.last().trace.traceId)
        assertEquals(records.first().trace.spanId, records.last().trace.parentSpanId)
    }

    @Test
    fun `typed log statement supplies the event code and reason contract`() = runTest {
        val records = mutableListOf<SanitizedLogRecord>()
        val logger = logger(
            PrivacyPolicy(IdentifierHasher { _, _ -> null }, ContractViolationBehavior.REJECT),
            listOf(LogSink(records::add)),
        )
        val failure = IllegalStateException("must not become a log message")

        logger.withRootTrace(Operations.AutofillResultDelivery) {
            logger.log(LogStatements.Autofill.resultDeliveryFailed(failure, 1))
        }

        val record = records.single()
        assertEquals(Events.Autofill.ResultDeliveryFailed, record.event)
        assertEquals(ErrorCodes.AutofillResultDeliveryFailed, record.error?.errorCode)
        assertEquals(Reasons.PlatformCallbackFailed, record.error?.reason)
        assertEquals("IllegalStateException", record.error?.exception?.type?.value)
        assertEquals(ExceptionKind.PLATFORM, record.error?.exception?.kind)
    }

    @Test
    fun `typed statement catalog is shared by app database security and autofill`() = runTest {
        val records = mutableListOf<SanitizedLogRecord>()
        val logger = logger(
            PrivacyPolicy(IdentifierHasher { _, _ -> null }, ContractViolationBehavior.REJECT),
            listOf(LogSink(records::add)),
        )

        logger.withRootTrace(Operations.ApplicationStartup) {
            logger.log(LogStatements.Application.backgroundLockSkipped())
            logger.log(
                LogStatements.Database.sessionTransition(
                    LogStatements.Database.sessionFields(
                        fromState = TestState.LOCKED,
                        toState = TestState.OPEN,
                        lockType = TestState.SOFT,
                        reason = Reasons.SessionOpen,
                        owner = TestState.APP,
                        elapsedMs = 12,
                        timeoutMs = null,
                        activeLeaseCount = 0,
                    ),
                ),
            )
            logger.log(
                LogStatements.Security.authenticationRejected(
                    LogStatements.Security.authenticationFields(
                        method = TestState.BIOMETRIC,
                        purpose = TestState.APP,
                        result = TestState.REJECTED,
                        elapsedMs = 8,
                    ),
                ),
            )
            logger.log(LogStatements.Autofill.discoveryUnavailable(Reasons.NoMatchingEntry))
        }

        assertEquals(
            listOf(
                Events.Application.BackgroundLockSkipped,
                Events.Database.SessionTransition,
                Events.Security.AuthenticationFailed,
                Events.Autofill.DiscoveryUnavailable,
            ),
            records.map(SanitizedLogRecord::event),
        )
    }

    private fun logger(
        sanitizer: LogSanitizer,
        sinks: List<LogSink>,
    ) = DefaultLogger(
        sanitizer = sanitizer,
        sinks = sinks,
        lifecycleSinks = emptyList(),
        idGenerator = SequentialIdGenerator(),
        timeSource = object : ObservabilityTimeSource {
            override fun wallTimeMs(): Long = 1_700_000_000_000
            override fun elapsedRealtimeMs(): Long = 100
        },
        processSessionProvider = ProcessSessionProvider { ProcessSessionId("process_session") },
        secureSessionProvider = SecureSessionProvider { SecureSessionId("secure_session") },
    )

    private class SequentialIdGenerator : ObservabilityIdGenerator {
        private var next = 0
        private fun id(prefix: String) = "${prefix}_${++next}_value"
        override fun eventId() = EventId(id("event"))
        override fun traceId() = TraceId(id("trace"))
        override fun spanId() = SpanId(id("span"))
    }

    private enum class TestState { LOCKED, OPEN, SOFT, APP, BIOMETRIC, REJECTED }
}
