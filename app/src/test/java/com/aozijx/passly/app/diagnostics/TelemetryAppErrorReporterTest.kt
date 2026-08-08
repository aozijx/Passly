package com.aozijx.passly.app.diagnostics

import com.aozijx.passly.core.error.model.AppError
import com.aozijx.passly.core.error.model.Conflict
import com.aozijx.passly.core.error.model.DatabaseInitFailed
import com.aozijx.passly.core.error.model.ValidationError
import com.aozijx.passly.core.error.mapping.fromThrowable
import com.aozijx.passly.core.telemetry.EventCategory
import com.aozijx.passly.core.telemetry.EventLevel
import com.aozijx.passly.core.telemetry.OperationCode
import com.aozijx.passly.core.telemetry.SafeLogValue
import com.aozijx.passly.core.telemetry.TelemetryEmitter
import com.aozijx.passly.core.telemetry.TelemetryEvent
import com.aozijx.passly.core.telemetry.reporting.ErrorReportContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class TelemetryAppErrorReporterTest {

    @Test
    fun `reports only whitelist fields, no free text`() {
        val events = mutableListOf<TelemetryEvent>()
        val emitter = TelemetryEmitter { events.add(it) }
        val reporter = TelemetryAppErrorReporter(emitter)

        val error = DatabaseInitFailed()
        reporter.report(
            error = error,
            context = ErrorReportContext(
                operation = OperationCode("db_init"),
                category = EventCategory.DATABASE
            )
        )

        assertEquals(1, events.size)
        val event = events.single()
        val fields = event.fields

        // Whitelist fields present
        assertTrue(fields["error_code"] is SafeLogValue.ErrorCodeValue)
        assertTrue(fields["error_layer"] is SafeLogValue.EnumName)
        assertTrue(fields["severity"] is SafeLogValue.EnumName)
        assertTrue(fields["recoverable"] is SafeLogValue.BooleanValue)
        assertTrue(fields["operation"] is SafeLogValue.OperationCodeValue)

        // No free text fields
        val fieldNames = fields.keys
        assertTrue("Must not contain error.message", "error_message" !in fieldNames)
        assertTrue("Must not contain message", "message" !in fieldNames)
        assertTrue("Must not contain entryId", "entry_id" !in fieldNames)
        assertTrue("Must not contain file path", "file_path" !in fieldNames)
        assertTrue("Must not contain url", "url" !in fieldNames)
        assertTrue("Must not contain domain", "domain" !in fieldNames)
        assertTrue("Must not contain username", "username" !in fieldNames)

        // Only 5 expected fields
        assertEquals(5, fields.size)
    }

    @Test
    fun `maps severity to event level`() {
        val events = mutableListOf<TelemetryEvent>()
        val emitter = TelemetryEmitter { events.add(it) }
        val reporter = TelemetryAppErrorReporter(emitter)

        // ERROR severity → EventLevel.ERROR
        reporter.report(
            error = DatabaseInitFailed(),
            context = ErrorReportContext(
                operation = OperationCode("db_init"),
                category = EventCategory.DATABASE
            )
        )
        assertEquals(EventLevel.ERROR, events.last().level)

        // WARNING severity → EventLevel.WARN
        reporter.report(
            error = Conflict(),
            context = ErrorReportContext(
                operation = OperationCode("entry_update"),
                category = EventCategory.DATABASE
            )
        )
        assertEquals(EventLevel.WARN, events.last().level)
    }

    @Test
    fun `correlation ID is set from errorId`() {
        val events = mutableListOf<TelemetryEvent>()
        val emitter = TelemetryEmitter { events.add(it) }
        val reporter = TelemetryAppErrorReporter(emitter)

        val error = ValidationError()
        reporter.report(
            error = error,
            context = ErrorReportContext(
                operation = OperationCode("validate"),
                category = EventCategory.DATABASE
            )
        )

        assertNotNull(events.single().correlationId)
        assertEquals(error.errorId, events.single().correlationId)
    }

    @Test
    fun `conflict is not reported as error but as warn`() {
        val events = mutableListOf<TelemetryEvent>()
        val emitter = TelemetryEmitter { events.add(it) }
        val reporter = TelemetryAppErrorReporter(emitter)

        reporter.report(
            error = Conflict(),
            context = ErrorReportContext(
                operation = OperationCode("entry_update"),
                category = EventCategory.DATABASE
            )
        )

        val event = events.single()
        assertEquals(EventLevel.WARN, event.level)
        assertEquals("error.conflict", event.name)
    }

    @Test
    fun `throwableType is null for manually created AppError`() {
        val events = mutableListOf<TelemetryEvent>()
        val emitter = TelemetryEmitter { events.add(it) }
        val reporter = TelemetryAppErrorReporter(emitter)

        // Manually created AppError (e.g., throw Conflict()) has no throwableType
        reporter.report(
            error = DatabaseInitFailed(),
            context = ErrorReportContext(
                operation = OperationCode("db_init"),
                category = EventCategory.DATABASE
            )
        )

        assertEquals(null, events.single().throwableType)
    }

    @Test
    fun `throwableType is set when mapped from real exception`() {
        val events = mutableListOf<TelemetryEvent>()
        val emitter = TelemetryEmitter { events.add(it) }
        val reporter = TelemetryAppErrorReporter(emitter)

        val mappedError = AppError.fromThrowable(IOException("disk full"))
        reporter.report(
            error = mappedError,
            context = ErrorReportContext(
                operation = OperationCode("file_write"),
                category = EventCategory.DATABASE
            )
        )

        assertEquals("IOException", events.single().throwableType)
    }
}