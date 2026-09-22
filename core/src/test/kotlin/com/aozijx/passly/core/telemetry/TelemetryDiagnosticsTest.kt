package com.aozijx.passly.core.telemetry

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TelemetryDiagnosticsTest {
    @Test
    fun throwableSnapshotKeepsAppLocationsWithoutMessage() {
        val error = IllegalArgumentException("secret entry value").apply {
            stackTrace = arrayOf(
                StackTraceElement(
                    "com.aozijx.passly.feature.Example",
                    "run",
                    "Example.kt",
                    42,
                ),
                StackTraceElement("java.lang.Thread", "run", "Thread.java", 1),
            )
        }

        val snapshot = error.toTelemetrySnapshot()

        assertTrue(snapshot.type == "IllegalArgumentException")
        assertTrue(snapshot.appStackFrames.single().endsWith("Example.run(Example.kt:42)"))
        assertFalse(snapshot.appStackFrames.joinToString().contains("secret entry value"))
    }

    @Test
    fun formatterIncludesSafeFieldsCorrelationAndMultilineFrames() {
        val event = TelemetryEvent(
            level = EventLevel.ERROR,
            category = EventCategory.AUTOFILL,
            name = "autofill.credential_request_failed",
            fields = mapOf("operation" to SafeLogValue.OperationCodeValue(OperationCode("resolve_credential"))),
            throwableType = "IllegalArgumentException",
            appStackFrames = listOf("com.aozijx.passly.Example.run(Example.kt:42)"),
            correlationId = "test-correlation",
            timestampMs = 1L,
        )

        val formatted = TelemetryEventFormatter.format(event, multiline = true)

        assertTrue(formatted.contains("operation=resolve_credential"))
        assertTrue(formatted.contains("error=IllegalArgumentException"))
        assertTrue(formatted.contains("correlation=test-correlation"))
        assertTrue(formatted.contains("\n  at com.aozijx.passly.Example.run(Example.kt:42)"))
    }
}
