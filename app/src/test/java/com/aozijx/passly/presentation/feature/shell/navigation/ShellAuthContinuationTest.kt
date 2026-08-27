package com.aozijx.passly.presentation.feature.shell.navigation

import com.aozijx.passly.presentation.feature.shell.AppShellAuthResult
import org.junit.Assert.assertEquals
import org.junit.Test

class ShellAuthContinuationTest {
    @Test
    fun successConsumesPendingContinuationExactlyOnce() {
        var calls = 0
        val continuation = ShellAuthContinuation()
        continuation.replace { calls++ }

        continuation.onResult(AppShellAuthResult.Success)
        continuation.onResult(AppShellAuthResult.Success)

        assertEquals(1, calls)
    }

    @Test
    fun notAuthorizedClearsPendingContinuation() {
        var calls = 0
        val continuation = ShellAuthContinuation()
        continuation.replace { calls++ }

        continuation.onResult(AppShellAuthResult.NotAuthorized)
        continuation.onResult(AppShellAuthResult.Success)

        assertEquals(0, calls)
    }
}
