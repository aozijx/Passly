package com.aozijx.passly.core.ui.theme

import android.annotation.SuppressLint
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
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
 * Generates Material emphasis roles from HCT tonal palettes while preserving [base]'s neutral,
 * surface, outline and error roles.
 */
@SuppressLint("RestrictedApi")
fun ColorScheme.withGeneratedAccents(seeds: AccentSeeds, isDark: Boolean): ColorScheme {
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
    )
}
