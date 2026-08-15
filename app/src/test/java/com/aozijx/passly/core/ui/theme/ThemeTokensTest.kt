package com.aozijx.passly.core.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeTokensTest {

    @Test
    fun numericValues_areExposedAsRoundedGroupTokens() {
        val definition = passlyThemeDefinition(
            outerCornerRadiusDp = 36f,
            innerCornerRadiusDp = 10f,
            groupItemSpacingDp = 5f,
            groupContentPaddingDp = 20f
        )

        assertEquals(36.dp, definition.tokens.roundedGroup.outerRadius)
        assertEquals(10.dp, definition.tokens.roundedGroup.innerRadius)
        assertEquals(5.dp, definition.tokens.roundedGroup.itemSpacing)
        assertEquals(20.dp, definition.tokens.roundedGroup.contentPadding)
    }

    @Test
    fun outOfRangeValues_areClampedAtThemeBoundary() {
        val style = passlyThemeDefinition(
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
    fun emptyOrUnknownManualSelection_resolvesToTheAppDefault() {
        assertEquals(0L, themePresetByColor(0L).color)
        assertNull(manualThemePalette(null))
        assertNull(manualThemePalette(0L))
        assertNull(manualThemePalette(0xFF123456))
    }

    @Test
    fun manualPalettes_mapIndependentAccentFamiliesWithoutTintingSurfaces() {
        val neutralScheme = lightColorScheme()

        themePresets.mapNotNull(ThemePreset::palette).forEach { palette ->
            val scheme = palette.applyTo(neutralScheme, isDark = false)

            assertEquals(palette.primary.light.accent, scheme.primary)
            assertEquals(palette.secondary.light.accent, scheme.secondary)
            assertEquals(palette.tertiary.light.accent, scheme.tertiary)
            assertEquals(palette.primary.light.container, scheme.primaryFixed)
            assertEquals(palette.secondary.light.container, scheme.secondaryFixed)
            assertEquals(palette.tertiary.light.container, scheme.tertiaryFixed)
            assertEquals(neutralScheme.surface, scheme.surface)
            assertEquals(neutralScheme.surfaceContainer, scheme.surfaceContainer)
            assertEquals(neutralScheme.surfaceVariant, scheme.surfaceVariant)
        }
    }

    @Test
    fun manualPaletteForegroundPairsMeetAccessibleTextContrast() {
        themePresets.mapNotNull(ThemePreset::palette).forEach { palette ->
            listOf(palette.primary, palette.secondary, palette.tertiary).forEach { family ->
                listOf(family.light, family.dark).forEach { roles ->
                    assertTrue(contrastRatio(roles.accent, roles.onAccent) >= 4.5)
                    assertTrue(contrastRatio(roles.container, roles.onContainer) >= 4.5)
                }
            }
        }
    }

    private fun contrastRatio(first: Color, second: Color): Float {
        val lighter = maxOf(first.luminance(), second.luminance())
        val darker = minOf(first.luminance(), second.luminance())
        return (lighter + 0.05f) / (darker + 0.05f)
    }
}
