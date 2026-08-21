package com.aozijx.passly.core.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.aozijx.passly.domain.settings.model.FallbackPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
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
    fun everyPersistedPalette_resolvesToThreeSeeds() {
        FallbackPalette.entries.forEach { palette ->
            val seeds = palette.accentSeeds()
            assertTrue(seeds.primary != 0L)
            assertTrue(seeds.secondary != 0L)
            assertTrue(seeds.tertiary != 0L)
        }
    }

    @Test
    fun manualPalettes_applyASubtleThreeSeedTintToTheFullScreenCanvas() {
        val neutralScheme = lightColorScheme()

        FallbackPalette.entries.map { it.accentSeeds() }.forEach { seeds ->
            val scheme = neutralScheme.withGeneratedAccents(seeds, isDark = false)

            assertTrue(
                scheme.primary != neutralScheme.primary ||
                    scheme.secondary != neutralScheme.secondary ||
                    scheme.tertiary != neutralScheme.tertiary
            )
            assertNotEquals(neutralScheme.background, scheme.background)
            assertEquals(neutralScheme.onBackground, scheme.onBackground)
            assertNotEquals(neutralScheme.surface, scheme.surface)
            assertEquals(neutralScheme.onSurface, scheme.onSurface)
            assertTrue(scheme.surfaceContainer != neutralScheme.surfaceContainer)
            assertTrue(scheme.surfaceVariant != neutralScheme.surfaceVariant)
        }
    }

    @Test
    fun canvasTint_blendsAllThreeSeeds() {
        val base = lightColorScheme()
        val seeds = AccentSeeds(0xFF4285F4, 0xFF4285F4, 0xFF4285F4)

        val secondaryChanged = base.withGeneratedAccents(
            seeds.copy(secondary = 0xFF34A853),
            isDark = false,
        )
        val tertiaryChanged = base.withGeneratedAccents(
            seeds.copy(tertiary = 0xFFEA4335),
            isDark = false,
        )
        val baseline = base.withGeneratedAccents(seeds, isDark = false)

        assertNotEquals(baseline.background, secondaryChanged.background)
        assertNotEquals(baseline.background, tertiaryChanged.background)
    }

    @Test
    fun manualPaletteForegroundPairsMeetAccessibleTextContrast() {
        FallbackPalette.entries.map { it.accentSeeds() }.forEach { seeds ->
            listOf(false, true).forEach { isDark ->
                val scheme = lightColorScheme().withGeneratedAccents(seeds, isDark)
                assertTrue(contrastRatio(scheme.primary, scheme.onPrimary) >= 4.5)
                assertTrue(contrastRatio(scheme.primaryContainer, scheme.onPrimaryContainer) >= 4.5)
                assertTrue(contrastRatio(scheme.secondary, scheme.onSecondary) >= 4.5)
                assertTrue(contrastRatio(scheme.tertiary, scheme.onTertiary) >= 4.5)
            }
        }
    }

    private fun contrastRatio(first: Color, second: Color): Float {
        val lighter = maxOf(first.luminance(), second.luminance())
        val darker = minOf(first.luminance(), second.luminance())
        return (lighter + 0.05f) / (darker + 0.05f)
    }
}
