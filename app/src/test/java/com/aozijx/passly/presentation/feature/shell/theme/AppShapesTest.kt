package com.aozijx.passly.presentation.feature.shell.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class AppShapesTest {

    @Test
    fun `application corner radius drives the expressive shape scale`() {
        val shapes = appShapes(cornerRadiusDp = 20f)

        assertEquals(RoundedCornerShape(5.dp), shapes.extraSmall)
        assertEquals(RoundedCornerShape(12.dp), shapes.medium)
        assertEquals(RoundedCornerShape(20.dp), shapes.extraLarge)
    }

    @Test
    fun `application corner radius is clamped before creating shapes`() {
        val shapes = appShapes(cornerRadiusDp = 100f)

        assertEquals(RoundedCornerShape(48.dp), shapes.extraLarge)
    }
}
