package com.aozijx.passly.core.observability.privacy

import com.aozijx.passly.core.observability.ExceptionKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExceptionSnapshotFactoryTest {
    private val factory = ExceptionSnapshotFactory()

    @Test
    fun `snapshot keeps sanitized application and foreign frames without throwable messages or paths`() {
        val root = IllegalStateException("PASSWORD_SENTINEL")
        root.stackTrace =
            arrayOf(
                StackTraceElement("com.aozijx.passly.feature.login.LoginUseCase", "invoke", "LoginUseCase.kt", 42),
                StackTraceElement("com.aozijx.passly.feature.login.OtherUseCase", "invoke", "PATH_SENTINEL", 43),
                StackTraceElement("com.bank.secret.CustomerActivity", "render", "CustomerActivity.kt", 8),
            )
        root.addSuppressed(IllegalArgumentException("OTP_SENTINEL"))

        val snapshot = factory.create(root, ExceptionKind.AUTHENTICATION)
        val encoded = snapshot.toString()

        assertEquals("IllegalStateException", snapshot.type.value)
        assertEquals(ExceptionKind.AUTHENTICATION, snapshot.kind)
        assertEquals(
            listOf(
                "feature.login.LoginUseCase#invoke(LoginUseCase.kt:42)",
                "feature.login.OtherUseCase#invoke",
                "com.bank.secret.CustomerActivity#render(CustomerActivity.kt:8)",
            ),
            snapshot.stackFrames.map { it.value },
        )
        assertEquals(1, snapshot.externalFrameCount)
        listOf("PASSWORD_SENTINEL", "PATH_SENTINEL", "OTP_SENTINEL")
            .forEach { assertFalse(encoded.contains(it)) }
    }

    @Test
    fun `snapshot keeps at most 24 Passly frames and 3 cause types`() {
        val root = IllegalStateException("root")
        root.stackTrace =
            Array(30) { index ->
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

        assertEquals(24, snapshot.stackFrames.size)
        assertEquals(3, snapshot.causeTypes.size)
        assertTrue(snapshot.causeTypes.all { it.value.endsWith("Exception") })
    }

    @Test
    fun `snapshot keeps mixed application and framework frames without package filtering`() {
        val root = IllegalStateException("secret")
        root.stackTrace =
            arrayOf(
                StackTraceElement("b08", "a", "VaultSessionController.kt", 321),
                StackTraceElement("android.os.Handler", "dispatchMessage", "Handler.java", 99),
            )

        val snapshot = factory.create(root, ExceptionKind.LIFECYCLE)

        assertEquals(
            listOf(
                "b08#a(VaultSessionController.kt:321)",
                "android.os.Handler#dispatchMessage(Handler.java:99)",
            ),
            snapshot.stackFrames.map { it.value },
        )
        assertEquals(2, snapshot.externalFrameCount)
    }
}
