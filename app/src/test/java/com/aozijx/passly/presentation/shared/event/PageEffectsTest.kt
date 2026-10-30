package com.aozijx.passly.presentation.shared.event

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PageEffectsTest {
    @Test
    fun `queued effect from an exited page is skipped after reentry`() = runTest {
        val pageEffects = PageEffects<String>()
        val oldOwner = pageEffects.enter()

        assertTrue(pageEffects.tryEmit(oldOwner, "old page"))
        pageEffects.exit(oldOwner)
        val newOwner = pageEffects.enter()
        assertTrue(pageEffects.tryEmit(newOwner, "current page"))

        assertEquals("current page", pageEffects.effects.first())
    }

    @Test
    fun `late exit from an old composition cannot deactivate the new page`() = runTest {
        val pageEffects = PageEffects<String>()
        val oldOwner = pageEffects.enter()
        val newOwner = pageEffects.enter()

        pageEffects.exit(oldOwner)

        assertFalse(pageEffects.tryEmit(oldOwner, "old page"))
        assertTrue(pageEffects.tryEmit(newOwner, "current page"))
        assertEquals("current page", pageEffects.effects.first())
    }
}
