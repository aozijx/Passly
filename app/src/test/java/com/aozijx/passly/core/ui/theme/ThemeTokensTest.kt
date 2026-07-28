package com.aozijx.passly.core.ui.theme

import androidx.compose.ui.unit.dp
import com.aozijx.passly.domain.settings.model.FallbackPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeTokensTest {

    @Test
    fun numericValues_areExposedAsRoundedGroupTokens() {
        val definition = passlyThemeDefinition(
            expressive = true,
            outerCornerRadiusDp = 36f,
            innerCornerRadiusDp = 10f,
            groupItemSpacingDp = 5f,
            groupContentPaddingDp = 20f
        )

        assertTrue(definition.tokens.isExpressive)
        assertEquals(36.dp, definition.tokens.roundedGroup.outerRadius)
        assertEquals(10.dp, definition.tokens.roundedGroup.innerRadius)
        assertEquals(5.dp, definition.tokens.roundedGroup.itemSpacing)
        assertEquals(20.dp, definition.tokens.roundedGroup.contentPadding)
    }

    @Test
    fun expressiveFlag_doesNotOverrideUserGeometry() {
        val definition = passlyThemeDefinition(
            expressive = false,
            outerCornerRadiusDp = 22f,
            innerCornerRadiusDp = 6f,
            groupItemSpacingDp = 2f,
            groupContentPaddingDp = 16f
        )

        assertFalse(definition.tokens.isExpressive)
        assertEquals(22.dp, definition.tokens.roundedGroup.outerRadius)
        assertEquals(6.dp, definition.tokens.roundedGroup.innerRadius)
    }

    @Test
    fun outOfRangeValues_areClampedAtThemeBoundary() {
        val style = passlyThemeDefinition(
            expressive = true,
            outerCornerRadiusDp = 80f,
            innerCornerRadiusDp = -4f,
            groupItemSpacingDp = 30f,
            groupContentPaddingDp = 2f
        ).tokens.roundedGroup

        assertEquals(48.dp, style.outerRadius)
        assertEquals(0.dp, style.innerRadius)
        assertEquals(12.dp, style.itemSpacing)
        assertEquals(8.dp, style.contentPadding)
    }

    @Test
    fun fallbackPalettes_resolveToDistinctNonDefaultPresets() {
        val presets = FallbackPalette.entries.map(::themePresetByFallbackPalette)

        assertEquals(FallbackPalette.entries.size, presets.map { it.color }.distinct().size)
        assertTrue(presets.all { it.color != 0L })
        assertTrue(presets.all { it.palette != null })
    }
}
