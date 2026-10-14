package com.aozijx.passly.core.observability.internal

import com.aozijx.passly.core.observability.DiagnosticId
import com.aozijx.passly.core.observability.ErrorCodes
import com.aozijx.passly.core.observability.EventId
import com.aozijx.passly.core.observability.Events
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
import org.junit.Assert.assertNotEquals
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
    fun `each reportable failure creates one diagnostic id reused by child events`() = runTest {
        val records = mutableListOf<SanitizedLogRecord>()
        val logger = logger(
            PrivacyPolicy(IdentifierHasher { _, _ -> null }, ContractViolationBehavior.REJECT),
            listOf(LogSink(records::add)),
        )
        var rootDiagnosticId: DiagnosticId? = null
        var childDiagnosticId: DiagnosticId? = null

        logger.withRootTrace(Operations.AutofillLegacyRequest) {
            rootDiagnosticId = logger.error(
                Events.Autofill.ResultDeliveryFailed,
                ErrorCodes.AutofillResultDeliveryFailed,
                Reasons.PlatformCallbackFailed,
                IllegalStateException("first"),
            )
            logger.inSpan(Operations.AutofillSessionClose) {
                childDiagnosticId = logger.warn(
                    Events.Autofill.SessionCloseFailed,
                    ErrorCodes.AutofillSessionCloseFailed,
                    Reasons.SessionCloseFailed,
                    IllegalStateException("second"),
                )
            }
        }

        assertEquals(rootDiagnosticId, childDiagnosticId)
        assertEquals(listOf(rootDiagnosticId, rootDiagnosticId), records.map { it.error?.diagnosticId })

        var separateFailureId: DiagnosticId? = null
        logger.withRootTrace(Operations.AutofillCredentialRequest) {
            separateFailureId = logger.error(
                Events.Autofill.ResultDeliveryFailed,
                ErrorCodes.AutofillResultDeliveryFailed,
                Reasons.PlatformCallbackFailed,
                IllegalStateException("third"),
            )
        }
        assertNotEquals(rootDiagnosticId, separateFailureId)
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
        override fun diagnosticId(): DiagnosticId {
            val seed = ++next
            return DiagnosticId.fromEntropy(ByteArray(8) { index -> (seed + index).toByte() })
        }
    }
}
