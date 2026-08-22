package com.aozijx.passly.core.ui.theme

import android.annotation.SuppressLint
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.google.android.material.color.utilities.DynamicScheme
import com.google.android.material.color.utilities.Hct
import com.google.android.material.color.utilities.MaterialDynamicColors
import com.google.android.material.color.utilities.TonalPalette
import com.google.android.material.color.utilities.Variant

/** Independent seeds for the three Material accent palettes. */
data class AccentSeeds(
    val primary: Long,
    val secondary: Long,
    val tertiary: Long,
)

/**
 * Generates Material accents and component containers from HCT tonal palettes while preserving
 * a three-seed canvas tint for the app's background and surface hierarchy.
 */
@SuppressLint("RestrictedApi")
fun ColorScheme.withGeneratedAccents(
    seeds: AccentSeeds,
    isDark: Boolean,
    canvasTintFraction: Float = DEFAULT_CANVAS_TINT_FRACTION,
): ColorScheme {
    val primaryArgb = seeds.primary.toInt()
    val scheme = DynamicScheme(
        Hct.fromInt(primaryArgb),
        Variant.TONAL_SPOT,
        isDark,
        0.0,
        TonalPalette.fromInt(primaryArgb),
        TonalPalette.fromInt(seeds.secondary.toInt()),
        TonalPalette.fromInt(seeds.tertiary.toInt()),
        TonalPalette.fromInt(primaryArgb),
        TonalPalette.fromInt(primaryArgb),
    )
    val roles = MaterialDynamicColors()
    fun role(color: Int): Color = Color(color)
    fun canvas(base: Color, multiplier: Float = 1f): Color =
        seeds.canvasTint(base, canvasTintFraction * multiplier)

    return copy(
        primary = role(roles.primary().getArgb(scheme)),
        onPrimary = role(roles.onPrimary().getArgb(scheme)),
        primaryContainer = role(roles.primaryContainer().getArgb(scheme)),
        onPrimaryContainer = role(roles.onPrimaryContainer().getArgb(scheme)),
        inversePrimary = role(roles.inversePrimary().getArgb(scheme)),
        secondary = role(roles.secondary().getArgb(scheme)),
        onSecondary = role(roles.onSecondary().getArgb(scheme)),
        secondaryContainer = role(roles.secondaryContainer().getArgb(scheme)),
        onSecondaryContainer = role(roles.onSecondaryContainer().getArgb(scheme)),
        tertiary = role(roles.tertiary().getArgb(scheme)),
        onTertiary = role(roles.onTertiary().getArgb(scheme)),
        tertiaryContainer = role(roles.tertiaryContainer().getArgb(scheme)),
        onTertiaryContainer = role(roles.onTertiaryContainer().getArgb(scheme)),
        surfaceTint = role(roles.surfaceTint().getArgb(scheme)),
        primaryFixed = role(roles.primaryFixed().getArgb(scheme)),
        primaryFixedDim = role(roles.primaryFixedDim().getArgb(scheme)),
        onPrimaryFixed = role(roles.onPrimaryFixed().getArgb(scheme)),
        onPrimaryFixedVariant = role(roles.onPrimaryFixedVariant().getArgb(scheme)),
        secondaryFixed = role(roles.secondaryFixed().getArgb(scheme)),
        secondaryFixedDim = role(roles.secondaryFixedDim().getArgb(scheme)),
        onSecondaryFixed = role(roles.onSecondaryFixed().getArgb(scheme)),
        onSecondaryFixedVariant = role(roles.onSecondaryFixedVariant().getArgb(scheme)),
        tertiaryFixed = role(roles.tertiaryFixed().getArgb(scheme)),
        tertiaryFixedDim = role(roles.tertiaryFixedDim().getArgb(scheme)),
        onTertiaryFixed = role(roles.onTertiaryFixed().getArgb(scheme)),
        onTertiaryFixedVariant = role(roles.onTertiaryFixedVariant().getArgb(scheme)),
        background = canvas(background),
        surface = canvas(surface),
        surfaceVariant = canvas(surfaceVariant, 0.9f),
        onSurfaceVariant = role(roles.onSurfaceVariant().getArgb(scheme)),
        surfaceDim = canvas(surfaceDim, 1.1f),
        surfaceBright = canvas(surfaceBright, 0.8f),
        surfaceContainerLowest = canvas(surfaceContainerLowest, 0.7f),
        surfaceContainerLow = canvas(surfaceContainerLow, 0.8f),
        surfaceContainer = canvas(surfaceContainer, 0.9f),
        surfaceContainerHigh = canvas(surfaceContainerHigh),
        surfaceContainerHighest = canvas(surfaceContainerHighest, 1.1f),
        inverseSurface = role(roles.inverseSurface().getArgb(scheme)),
        inverseOnSurface = role(roles.inverseOnSurface().getArgb(scheme)),
        outline = role(roles.outline().getArgb(scheme)),
        outlineVariant = role(roles.outlineVariant().getArgb(scheme)),
        scrim = role(roles.scrim().getArgb(scheme)),
    )
}

private fun AccentSeeds.canvasTint(base: Color, fraction: Float): Color {
    val blendedSeed = lerp(
        lerp(Color(primary), Color(secondary), 0.5f),
        Color(tertiary),
        1f / 3f,
    )
    return lerp(base, blendedSeed, fraction.coerceIn(0f, 1f))
}

private const val DEFAULT_CANVAS_TINT_FRACTION = 0.08f
