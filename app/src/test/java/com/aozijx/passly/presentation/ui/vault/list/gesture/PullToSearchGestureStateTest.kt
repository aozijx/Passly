package com.aozijx.passly.presentation.ui.vault.list.gesture

import org.junit.Assert.assertEquals
import org.junit.Test

class PullToSearchGestureStateTest {
    @Test
    fun `pull triggers once only after threshold while list is at top`() {
        var triggers = 0
        val state = PullToSearchGestureState(
            thresholdPx = 80f,
            onProgressChanged = {},
            onTriggered = { triggers++ },
        )

        state.onPull(deltaY = 60f, isAtTop = true)
        state.onPull(deltaY = 30f, isAtTop = false)
        state.onPull(deltaY = 50f, isAtTop = true)
        assertEquals(0, triggers)

        state.onPull(deltaY = 30f, isAtTop = true)
        state.onPull(deltaY = 100f, isAtTop = true)

        assertEquals(1, triggers)
    }

    @Test
    fun `leaving top clears an unfinished pull`() {
        var triggers = 0
        val state = PullToSearchGestureState(
            thresholdPx = 80f,
            onProgressChanged = {},
            onTriggered = { triggers++ },
        )

        state.onPull(deltaY = 50f, isAtTop = true)
        state.onPull(deltaY = 10f, isAtTop = false)
        state.onPull(deltaY = 40f, isAtTop = true)

        assertEquals(0, triggers)
    }

    @Test
    fun `pull reports bounded expansion progress and resets when released`() {
        val progress = mutableListOf<Float>()
        val state = PullToSearchGestureState(
            thresholdPx = 100f,
            onProgressChanged = progress::add,
            onTriggered = {},
        )

        state.onPull(deltaY = 25f, isAtTop = true)
        state.onPull(deltaY = 50f, isAtTop = true)
        state.reset()

        assertEquals(listOf(0.25f, 0.75f, 0f), progress)
    }

    @Test
    fun `small reverse drag reduces progress instead of dropping the anchor`() {
        val progress = mutableListOf<Float>()
        val state = PullToSearchGestureState(
            thresholdPx = 100f,
            onProgressChanged = progress::add,
            onTriggered = {},
        )

        state.onPull(deltaY = 60f, isAtTop = true)
        state.onPull(deltaY = -20f, isAtTop = true)

        assertEquals(listOf(0.6f, 0.4f), progress)
    }
}
