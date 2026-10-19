package com.aozijx.passly.core.observability

import com.aozijx.passly.core.observability.sink.LogSink
import java.lang.reflect.Modifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class LogContractTest {
    @Test
    fun `event and field identifiers reject free text and invalid grammar`() {
        assertEquals("autofill.discovery.started", EventKey("autofill.discovery.started").value)
        assertEquals("autofill.result.delivery_failed", Events.Autofill.ResultDeliveryFailed.value)
        assertEquals("elapsed_ms", LogFieldKey("elapsed_ms").value)
        assertEquals("OK", EnumName("OK").value)

        listOf("Autofill.Started", "contains space", "a", "autofill/started").forEach { value ->
            assertThrows(IllegalArgumentException::class.java) { EventKey(value) }
        }
        listOf("ElapsedMs", "target.package", "has space", "").forEach { value ->
            assertThrows(IllegalArgumentException::class.java) { LogFieldKey(value) }
        }
    }

    @Test
    fun `log fields expose no arbitrary string value`() {
        val constructorSignatures = LogValue::class.java.declaredClasses.associate { type ->
            type.simpleName to type.declaredConstructors
                .single { Modifier.isPublic(it.modifiers) }
                .parameterTypes
                .filterNot { it.name == "kotlin.jvm.internal.DefaultConstructorMarker" }
                .toList()
        }

        assertEquals(
            mapOf(
                "BooleanValue" to listOf(Boolean::class.javaPrimitiveType),
                "Count" to listOf(Long::class.javaPrimitiveType),
                "DurationMs" to listOf(Long::class.javaPrimitiveType),
                "EnumValue" to listOf(String::class.java),
                "ErrorCodeValue" to listOf(String::class.java),
                "HashedIdentifierInput" to listOf(IdentifierNamespace::class.java, String::class.java),
                "HashedIdentifierValue" to listOf(String::class.java),
                "OperationCodeValue" to listOf(String::class.java),
                "Ratio" to listOf(Double::class.javaPrimitiveType),
                "ReasonCodeValue" to listOf(String::class.java),
            ),
            constructorSignatures,
        )
    }

    @Test
    fun `warn and error require code reason and throwable at the API boundary`() {
        val warnContract:
            Logger.(EventKey, ErrorCode, ReasonCode, Throwable, LogFields) -> Unit =
            Logger::warn
        val errorContract:
            Logger.(EventKey, ErrorCode, ReasonCode, Throwable, LogFields) -> Unit =
            Logger::error

        assertFalse(warnContract == errorContract)
        assertEquals(SanitizedLogRecord::class.java, LogSink::class.java.methods.single().parameterTypes.single())
    }

    @Test
    fun `exception contract accepts only bounded source symbols`() {
        assertEquals("IllegalStateException", ExceptionType("IllegalStateException").value)
        assertThrows(IllegalArgumentException::class.java) {
            ExceptionType("java.lang.IllegalStateException: secret")
        }
        assertThrows(IllegalArgumentException::class.java) {
            ExceptionSnapshot(
                type = ExceptionType("IllegalStateException"),
                appStackFrames = emptyList(),
                causeTypes = emptyList(),
                foreignFrameCount = -1,
            )
        }
    }
}
