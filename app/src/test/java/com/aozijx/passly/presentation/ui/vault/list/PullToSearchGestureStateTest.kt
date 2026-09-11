package com.aozijx.passly.presentation.ui.vault.list

import org.junit.Assert.assertEquals
import org.junit.Test

class PullToSearchGestureStateTest {
    @Test
    fun `pull triggers once only after threshold while list is at top`() {
        var triggers = 0
        val state = PullToSearchGestureState(thresholdPx = 80f) { triggers++ }

        state.onPull(deltaY = 60f, isAtTop = true)
        state.onPull(deltaY = 30f, isAtTop = false)
        state.onPull(deltaY = 50f, isAtTop = true)
        assertEquals(0, triggers)

        state.onPull(deltaY = 30f, isAtTop = true)
        state.onPull(deltaY = 100f, isAtTop = true)

        assertEquals(1, triggers)
    }
}
