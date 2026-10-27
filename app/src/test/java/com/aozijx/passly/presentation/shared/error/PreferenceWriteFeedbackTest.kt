package com.aozijx.passly.presentation.shared.error

import com.aozijx.passly.R
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PreferenceWriteFeedbackTest {
    @Test
    fun `success feedback waits until preference storage acknowledges write`() = runTest {
        val stored = CompletableDeferred<Unit>()
        val feedback = mutableListOf<String>()
        backgroundScope.launch {
            writePreferenceWithFeedback(
                failureRes = R.string.ui_error_generic,
                onFailure = { feedback += "failed" },
                onSuccess = { feedback += "saved" },
            ) { stored.await() }
        }
        runCurrent()
        assertEquals(emptyList<String>(), feedback)
        stored.complete(Unit)
        runCurrent()
        assertEquals(listOf("saved"), feedback)
    }
}
