package com.aozijx.passly.presentation.ui.shared.components.topbar

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class PasslyTopBarStyleTest {

    @Test
    fun `palette maps every top bar role from its color scheme`() {
        val scheme = lightColorScheme(
            surface = Color(0xFF110000),
            surfaceContainer = Color(0xFF220000),
            onSurface = Color(0xFF330000),
            onSurfaceVariant = Color(0xFF440000),
        )

        val palette = scheme.toPasslyTopBarPalette()

        assertEquals(Color(0xFF110000), palette.container)
        assertEquals(Color(0xFF220000), palette.scrolledContainer)
        assertEquals(Color(0xFF330000), palette.title)
        assertEquals(Color(0xFF330000), palette.navigationIcon)
        assertEquals(Color(0xFF440000), palette.actionIcon)
    }
}
