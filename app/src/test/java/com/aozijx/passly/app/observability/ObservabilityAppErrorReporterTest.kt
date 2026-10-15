package com.aozijx.passly.app.observability

import com.aozijx.passly.core.error.model.DatabaseInitFailed
import com.aozijx.passly.core.observability.*
import com.aozijx.passly.core.observability.internal.*
import com.aozijx.passly.core.observability.privacy.*
import com.aozijx.passly.core.observability.reporting.ErrorReportContext
import com.aozijx.passly.core.observability.sink.LogSink
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class ObservabilityAppErrorReporterTest {
    @Test
    fun `domain error id is not exposed and duplicate boundary reports collapse`() = runTest {
        val records = mutableListOf<SanitizedLogRecord>()
        val reporter = ObservabilityAppErrorReporter(logger(records))
        val error = DatabaseInitFailed(errorId = "018f-stable-error-id")
        val context = ErrorReportContext(Operations.DatabaseSession, LogCategory.DATABASE)

        reporter.report(error, context)
        reporter.report(error, context)

        val record = records.single()
        assertNotNull(record.error)
        assertNotEquals(error.errorId, record.trace.traceId.value)
        assertFalse(record.toString().contains(error.errorId))
    }

    private fun logger(records: MutableList<SanitizedLogRecord>) = DefaultLogger(
        sanitizer = PrivacyPolicy(IdentifierHasher { _, _ -> null }, ContractViolationBehavior.THROW),
        sinks = listOf(LogSink(records::add)),
        lifecycleSinks = emptyList(),
        idGenerator = object : ObservabilityIdGenerator {
            private var next = 0
            private fun id(prefix: String) = "${prefix}_${++next}_value"
            override fun eventId() = EventId(id("event"))
            override fun traceId() = TraceId(id("trace"))
            override fun spanId() = SpanId(id("span"))
        },
        timeSource = object : ObservabilityTimeSource {
            override fun wallTimeMs() = 1L
            override fun elapsedRealtimeMs() = 2L
        },
        processSessionProvider = ProcessSessionProvider { ProcessSessionId("process_session") },
        secureSessionProvider = SecureSessionProvider { null },
    )
}
