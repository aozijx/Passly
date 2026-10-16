package com.aozijx.passly.core.observability.privacy

import com.aozijx.passly.core.observability.ExceptionKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExceptionSnapshotFactoryTest {
    private val factory = ExceptionSnapshotFactory()

    @Test
    fun `snapshot never reads or emits throwable messages suppressed messages or foreign frames`() {
        val root = IllegalStateException("PASSWORD_SENTINEL")
        root.stackTrace = arrayOf(
            StackTraceElement("com.aozijx.passly.feature.login.LoginUseCase", "invoke", "PATH_SENTINEL", 42),
            StackTraceElement("com.bank.secret.CustomerActivity", "TOKEN_SENTINEL", "CustomerActivity.kt", 8),
        )
        root.addSuppressed(IllegalArgumentException("OTP_SENTINEL"))

        val snapshot = factory.create(root, ExceptionKind.AUTHENTICATION)
        val encoded = snapshot.toString()

        assertEquals("IllegalStateException", snapshot.type.value)
        assertEquals(ExceptionKind.AUTHENTICATION, snapshot.kind)
        assertEquals(listOf("feature.login.LoginUseCase#invoke:42"), snapshot.appStackFrames.map { it.value })
        assertEquals(1, snapshot.foreignFrameCount)
        listOf("PASSWORD_SENTINEL", "PATH_SENTINEL", "TOKEN_SENTINEL", "OTP_SENTINEL", "com.bank.secret")
            .forEach { assertFalse(encoded.contains(it)) }
    }

    @Test
    fun `snapshot keeps at most 24 Passly frames and 3 cause types`() {
        val root = IllegalStateException("root")
        root.stackTrace = Array(30) { index ->
            StackTraceElement("com.aozijx.passly.core.SafeClass$index", "call", "SafeClass.kt", index + 1)
        }
        root.initCause(
            IllegalArgumentException("one").apply {
                initCause(
                    UnsupportedOperationException("two").apply {
                        initCause(
                            SecurityException("three").apply {
                                initCause(ArithmeticException("four"))
                            },
                        )
                    },
                )
            },
        )

        val snapshot = factory.create(root)

        assertEquals(24, snapshot.appStackFrames.size)
        assertEquals(3, snapshot.causeTypes.size)
        assertTrue(snapshot.causeTypes.all { it.value.endsWith("Exception") })
    }
}
