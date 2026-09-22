package com.aozijx.passly.presentation.shared.components.topbar

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class TopAppBarContainerColorTest {

    @Test
    fun containerColorTracksScrollProgressWithoutASecondTimeAnimation() {
        val surface = Color.Black
        val scrolledSurface = Color.White

        assertEquals(surface, topAppBarContainerColor(surface, scrolledSurface, 0f))
        assertEquals(Color(0xFF636363), topAppBarContainerColor(surface, scrolledSurface, 0.5f))
        assertEquals(scrolledSurface, topAppBarContainerColor(surface, scrolledSurface, 1f))
    }

    @Test
    fun containerColorClampsTransientScrollOvershoot() {
        val surface = Color.Black
        val scrolledSurface = Color.White

        assertEquals(surface, topAppBarContainerColor(surface, scrolledSurface, -0.2f))
        assertEquals(scrolledSurface, topAppBarContainerColor(surface, scrolledSurface, 1.2f))
    }
}
