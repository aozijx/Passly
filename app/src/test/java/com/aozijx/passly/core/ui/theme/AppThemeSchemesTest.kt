package com.aozijx.passly.core.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppThemeSchemesTest {

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
    fun manualPalette_replacesTheCompleteColorSchemeInsteadOfTintingItsInput() {
        val scheme = generateAppColorScheme(TEST_SEEDS, isDark = false)

        assertNotEquals(lightColorScheme().background, scheme.background)
        assertNotEquals(lightColorScheme().surface, scheme.surface)
        assertNotEquals(lightColorScheme().surfaceContainer, scheme.surfaceContainer)
    }

    @Test
    fun themeSchemeCatalog_hasUniqueKeysAndTreatsUnknownKeysAsUnselected() {
        val keys = AppThemeSchemes.all.map { it.key }

        assertEquals(keys.size, keys.toSet().size)
        assertTrue(keys.all(String::isNotBlank))
        assertEquals(null, AppThemeSchemes.find("unknown"))
    }

    @Test
    fun manualPalette_usesAllThreeAccentSeeds() {
        val seeds = TEST_SEEDS.copy(
            secondary = TEST_SEEDS.primary,
            tertiary = TEST_SEEDS.primary,
        )

        val secondaryChanged = generateAppColorScheme(
            seeds.copy(secondary = TEST_SEEDS.secondary),
            isDark = false,
        )
        val tertiaryChanged = generateAppColorScheme(
            seeds.copy(tertiary = TEST_SEEDS.tertiary),
            isDark = false,
        )
        val baseline = generateAppColorScheme(seeds, isDark = false)

        assertNotEquals(baseline.secondary, secondaryChanged.secondary)
        assertNotEquals(baseline.tertiary, tertiaryChanged.tertiary)
    }

    @Test
    fun manualPalette_usesNeutralSurfacesInsteadOfASeedColoredOverlay() {
        val scheme = generateAppColorScheme(TEST_SEEDS, isDark = false)

        assertTrue(scheme.background.channelSpread() < 0.08f)
        assertTrue(scheme.surfaceContainer.channelSpread() < 0.08f)
    }

    @Test
    fun manualPaletteForegroundPairsMeetAccessibleTextContrast() {
        listOf(false, true).forEach { isDark ->
            val scheme = generateAppColorScheme(TEST_SEEDS, isDark)
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

    private fun Color.channelSpread(): Float =
        maxOf(red, green, blue) - minOf(red, green, blue)

    private companion object {
        val TEST_SEEDS = AccentSeeds(0xFF5B789E, 0xFF6C7E71, 0xFF85789B)
    }
}
