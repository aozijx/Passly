package com.aozijx.passly.presentation.ui.vault.list.gesture

import org.junit.Assert.assertEquals
import org.junit.Test

class FabVisibilityGestureStateTest {
    @Test
    fun `scroll direction only emits visibility changes`() {
        val changes = mutableListOf<Boolean>()
        val state = FabVisibilityGestureState(changes::add)

        state.onScroll(-2f)
        state.onScroll(-4f)
        state.onScroll(0.5f)
        state.onScroll(3f)
        state.onScroll(5f)

        assertEquals(listOf(false, true), changes)
    }
}
