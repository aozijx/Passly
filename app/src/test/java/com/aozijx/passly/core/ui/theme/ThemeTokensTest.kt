package com.aozijx.passly.core.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
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
    fun everyConfiguredTheme_resolvesToThreeSeeds() {
        AppThemeSchemes.all.forEach { scheme ->
            val seeds = scheme.seedResources
            assertTrue(seeds.primary != 0)
            assertTrue(seeds.secondary != 0)
            assertTrue(seeds.tertiary != 0)
        }
    }

    @Test
    fun manualPalettes_applyASubtleThreeSeedTintToTheFullScreenCanvas() {
        val neutralScheme = lightColorScheme()
        val scheme = neutralScheme.withGeneratedAccents(TEST_SEEDS, isDark = false)

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

    @Test
    fun themeSchemeCatalog_hasUniqueKeysAndTreatsUnknownKeysAsUnselected() {
        val keys = AppThemeSchemes.all.map { it.key }

        assertEquals(keys.size, keys.toSet().size)
        assertTrue(keys.all(String::isNotBlank))
        assertEquals(null, AppThemeSchemes.find("unknown"))
    }

    @Test
    fun canvasTint_blendsAllThreeSeeds() {
        val base = lightColorScheme()
        val seeds = TEST_SEEDS.copy(
            secondary = TEST_SEEDS.primary,
            tertiary = TEST_SEEDS.primary,
        )

        val secondaryChanged = base.withGeneratedAccents(
            seeds.copy(secondary = TEST_SEEDS.secondary),
            isDark = false,
        )
        val tertiaryChanged = base.withGeneratedAccents(
            seeds.copy(tertiary = TEST_SEEDS.tertiary),
            isDark = false,
        )
        val baseline = base.withGeneratedAccents(seeds, isDark = false)

        assertNotEquals(baseline.background, secondaryChanged.background)
        assertNotEquals(baseline.background, tertiaryChanged.background)
    }

    @Test
    fun canvasTintStrength_controlsTheCanvasBlend() {
        val base = lightColorScheme()
        val seeds = TEST_SEEDS

        val subtle = base.withGeneratedAccents(
            seeds = seeds,
            isDark = false,
            canvasTintFraction = 0.01f,
        )
        val strong = base.withGeneratedAccents(
            seeds = seeds,
            isDark = false,
            canvasTintFraction = 1f,
        )

        assertNotEquals(subtle.background, strong.background)
        assertNotEquals(subtle.surfaceContainer, strong.surfaceContainer)
    }

    @Test
    fun manualPaletteForegroundPairsMeetAccessibleTextContrast() {
        listOf(false, true).forEach { isDark ->
            val scheme = lightColorScheme().withGeneratedAccents(TEST_SEEDS, isDark)
            assertTrue(contrastRatio(scheme.primary, scheme.onPrimary) >= 4.5)
            assertTrue(contrastRatio(scheme.primaryContainer, scheme.onPrimaryContainer) >= 4.5)
            assertTrue(contrastRatio(scheme.secondary, scheme.onSecondary) >= 4.5)
            assertTrue(contrastRatio(scheme.tertiary, scheme.onTertiary) >= 4.5)
        }
    }

    private fun contrastRatio(first: Color, second: Color): Float {
        val lighter = maxOf(first.luminance(), second.luminance())
        val darker = minOf(first.luminance(), second.luminance())
        return (lighter + 0.05f) / (darker + 0.05f)
    }

    private companion object {
        val TEST_SEEDS = AccentSeeds(0xFF5B789E, 0xFF6C7E71, 0xFF85789B)
    }
}
