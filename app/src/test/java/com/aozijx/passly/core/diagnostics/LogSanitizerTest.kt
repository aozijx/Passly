package com.aozijx.passly.core.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LogSanitizerTest {
    @Test
    fun sensitiveValuesAreRemovedBeforeSinkDispatch() {
        var received: SanitizedLogEvent? = null
        val logger = StructuredLogger(
            object : LogSink {
                override fun write(event: SanitizedLogEvent) {
                    received = event
                }
            }
        )

        logger.log(
            LogEvent(
                level = LogLevel.ERROR,
                category = LogCategory.AUTHENTICATION,
                name = "password=do-not-log https://example.test/private",
                fields = mapOf(
                    "username" to "alice@example.test",
                    "result" to "token=abc123",
                    "attempt" to "2"
                ),
                throwable = IllegalStateException("password=also-secret")
            )
        )

        val event = requireNotNull(received)
        assertFalse(event.name.contains("do-not-log"))
        assertFalse(event.name.contains("example.test"))
        assertNull(event.fields["username"])
        assertEquals("[REDACTED]", event.fields["result"])
        assertEquals("2", event.fields["attempt"])
        assertEquals("IllegalStateException", event.throwableType)
        assertTrue(event.appStackFrames.none { it.contains("also-secret") })
    }
}
