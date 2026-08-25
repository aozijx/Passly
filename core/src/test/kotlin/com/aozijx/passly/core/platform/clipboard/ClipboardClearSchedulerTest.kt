package com.aozijx.passly.core.platform.clipboard

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ClipboardClearSchedulerTest {

    @Test
    fun `scheduled token is cleared only after delay`() = runTest {
        val cleared = mutableListOf<String>()
        val scheduler = ClipboardClearScheduler(this, StandardTestDispatcher(testScheduler))

        scheduler.schedule("first", 30_000) { cleared += it }
        advanceTimeBy(29_999)
        assertEquals(emptyList<String>(), cleared)

        advanceUntilIdle()
        assertEquals(listOf("first"), cleared)
    }

    @Test
    fun `new schedule replaces previous token`() = runTest {
        val cleared = mutableListOf<String>()
        val scheduler = ClipboardClearScheduler(this, StandardTestDispatcher(testScheduler))

        scheduler.schedule("first", 30_000) { cleared += it }
        scheduler.schedule("second", 15_000) { cleared += it }
        advanceUntilIdle()

        assertEquals(listOf("second"), cleared)
    }

    @Test
    fun `cancel prevents pending clear`() = runTest {
        val cleared = mutableListOf<String>()
        val scheduler = ClipboardClearScheduler(this, StandardTestDispatcher(testScheduler))

        scheduler.schedule("first", 15_000) { cleared += it }
        scheduler.cancel()
        advanceUntilIdle()

        assertEquals(emptyList<String>(), cleared)
    }
}
