package com.aozijx.passly.presentation.feature.shell.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Test

class AnimatedColorSchemeTest {

    @Test
    fun `one progress interpolates all material color roles`() {
        val start = distinctColorScheme(0x001000)
        val end = distinctColorScheme(0x801000)

        val result = lerpColorScheme(start, end, fraction = 0.4f)

        fun assertRole(from: Color, to: Color, actual: Color) =
            assertEquals(lerp(from, to, 0.4f), actual)

        assertRole(start.primary, end.primary, result.primary)
        assertRole(start.onPrimary, end.onPrimary, result.onPrimary)
        assertRole(start.primaryContainer, end.primaryContainer, result.primaryContainer)
        assertRole(start.onPrimaryContainer, end.onPrimaryContainer, result.onPrimaryContainer)
        assertRole(start.inversePrimary, end.inversePrimary, result.inversePrimary)
        assertRole(start.secondary, end.secondary, result.secondary)
        assertRole(start.onSecondary, end.onSecondary, result.onSecondary)
        assertRole(start.secondaryContainer, end.secondaryContainer, result.secondaryContainer)
        assertRole(start.onSecondaryContainer, end.onSecondaryContainer, result.onSecondaryContainer)
        assertRole(start.tertiary, end.tertiary, result.tertiary)
        assertRole(start.onTertiary, end.onTertiary, result.onTertiary)
        assertRole(start.tertiaryContainer, end.tertiaryContainer, result.tertiaryContainer)
        assertRole(start.onTertiaryContainer, end.onTertiaryContainer, result.onTertiaryContainer)
        assertRole(start.background, end.background, result.background)
        assertRole(start.onBackground, end.onBackground, result.onBackground)
        assertRole(start.surface, end.surface, result.surface)
        assertRole(start.onSurface, end.onSurface, result.onSurface)
        assertRole(start.surfaceVariant, end.surfaceVariant, result.surfaceVariant)
        assertRole(start.onSurfaceVariant, end.onSurfaceVariant, result.onSurfaceVariant)
        assertRole(start.surfaceTint, end.surfaceTint, result.surfaceTint)
        assertRole(start.inverseSurface, end.inverseSurface, result.inverseSurface)
        assertRole(start.inverseOnSurface, end.inverseOnSurface, result.inverseOnSurface)
        assertRole(start.error, end.error, result.error)
        assertRole(start.onError, end.onError, result.onError)
        assertRole(start.errorContainer, end.errorContainer, result.errorContainer)
        assertRole(start.onErrorContainer, end.onErrorContainer, result.onErrorContainer)
        assertRole(start.outline, end.outline, result.outline)
        assertRole(start.outlineVariant, end.outlineVariant, result.outlineVariant)
        assertRole(start.scrim, end.scrim, result.scrim)
        assertRole(start.surfaceBright, end.surfaceBright, result.surfaceBright)
        assertRole(start.surfaceDim, end.surfaceDim, result.surfaceDim)
        assertRole(start.surfaceContainer, end.surfaceContainer, result.surfaceContainer)
        assertRole(start.surfaceContainerHigh, end.surfaceContainerHigh, result.surfaceContainerHigh)
        assertRole(start.surfaceContainerHighest, end.surfaceContainerHighest, result.surfaceContainerHighest)
        assertRole(start.surfaceContainerLow, end.surfaceContainerLow, result.surfaceContainerLow)
        assertRole(start.surfaceContainerLowest, end.surfaceContainerLowest, result.surfaceContainerLowest)
        assertRole(start.primaryFixed, end.primaryFixed, result.primaryFixed)
        assertRole(start.primaryFixedDim, end.primaryFixedDim, result.primaryFixedDim)
        assertRole(start.onPrimaryFixed, end.onPrimaryFixed, result.onPrimaryFixed)
        assertRole(start.onPrimaryFixedVariant, end.onPrimaryFixedVariant, result.onPrimaryFixedVariant)
        assertRole(start.secondaryFixed, end.secondaryFixed, result.secondaryFixed)
        assertRole(start.secondaryFixedDim, end.secondaryFixedDim, result.secondaryFixedDim)
        assertRole(start.onSecondaryFixed, end.onSecondaryFixed, result.onSecondaryFixed)
        assertRole(start.onSecondaryFixedVariant, end.onSecondaryFixedVariant, result.onSecondaryFixedVariant)
        assertRole(start.tertiaryFixed, end.tertiaryFixed, result.tertiaryFixed)
        assertRole(start.tertiaryFixedDim, end.tertiaryFixedDim, result.tertiaryFixedDim)
        assertRole(start.onTertiaryFixed, end.onTertiaryFixed, result.onTertiaryFixed)
        assertRole(start.onTertiaryFixedVariant, end.onTertiaryFixedVariant, result.onTertiaryFixedVariant)
    }

    @Test
    fun `target signature observes fixed color role changes`() {
        val initial = lightColorScheme(primaryFixed = Color.Red)
        val changed = initial.copy(primaryFixed = Color.Blue)

        assertNotEquals(initial.colorSchemeSignature(), changed.colorSchemeSignature())
    }

    @Test
    fun `settled progress reuses its endpoint color scheme`() {
        val start = lightColorScheme(primary = Color.Red)
        val end = lightColorScheme(primary = Color.Blue)

        assertSame(start, lerpColorScheme(start, end, fraction = 0f))
        assertSame(end, lerpColorScheme(start, end, fraction = 1f))
    }

    @Test
    fun `retarget starts from the currently displayed color scheme`() {
        val first = lightColorScheme(primary = Color.Black, surface = Color.Red)
        val second = lightColorScheme(primary = Color.White, surface = Color.Blue)
        val third = lightColorScheme(primary = Color.Red, surface = Color.Green)

        val retargeted = retargetColorScheme(
            start = first,
            stop = second,
            progress = 0.35f,
            newTarget = third,
        )

        assertEquals(lerp(first.primary, second.primary, 0.35f), retargeted.start.primary)
        assertEquals(lerp(first.surface, second.surface, 0.35f), retargeted.start.surface)
        assertSame(third, retargeted.stop)
    }

    @Test
    fun `unspecified roles adopt the available color during transition`() {
        val start = lightColorScheme().copy(primaryFixed = Color.Unspecified)
        val end = lightColorScheme().copy(primaryFixed = Color.Red)

        val result = lerpColorScheme(start, end, fraction = 0.5f)

        assertEquals(Color.Red, result.primaryFixed)
    }

    private fun distinctColorScheme(offset: Int): androidx.compose.material3.ColorScheme {
        var role = offset
        fun next() = Color(0xFF000000.toInt() or role++)
        return lightColorScheme(
            primary = next(), onPrimary = next(), primaryContainer = next(),
            onPrimaryContainer = next(), inversePrimary = next(), secondary = next(),
            onSecondary = next(), secondaryContainer = next(), onSecondaryContainer = next(),
            tertiary = next(), onTertiary = next(), tertiaryContainer = next(),
            onTertiaryContainer = next(), background = next(), onBackground = next(),
            surface = next(), onSurface = next(), surfaceVariant = next(),
            onSurfaceVariant = next(), surfaceTint = next(), inverseSurface = next(),
            inverseOnSurface = next(), error = next(), onError = next(),
            errorContainer = next(), onErrorContainer = next(), outline = next(),
            outlineVariant = next(), scrim = next(), surfaceBright = next(),
            surfaceDim = next(), surfaceContainer = next(), surfaceContainerHigh = next(),
            surfaceContainerHighest = next(), surfaceContainerLow = next(),
            surfaceContainerLowest = next(), primaryFixed = next(), primaryFixedDim = next(),
            onPrimaryFixed = next(), onPrimaryFixedVariant = next(), secondaryFixed = next(),
            secondaryFixedDim = next(), onSecondaryFixed = next(),
            onSecondaryFixedVariant = next(), tertiaryFixed = next(), tertiaryFixedDim = next(),
            onTertiaryFixed = next(), onTertiaryFixedVariant = next(),
        )
    }
}
