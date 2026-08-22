package com.aozijx.passly.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.aozijx.passly.core.R

val md_theme_light_primary @Composable get() = colorResource(R.color.primary_light)
val md_theme_light_onPrimary @Composable get() = colorResource(R.color.on_primary_light)
val md_theme_light_primaryContainer @Composable get() = colorResource(R.color.primary_container_light)
val md_theme_light_onPrimaryContainer @Composable get() = colorResource(R.color.on_primary_container_light)
val md_theme_light_secondary @Composable get() = colorResource(R.color.secondary_light)
val md_theme_light_secondaryContainer @Composable get() = colorResource(R.color.secondary_container_light)
val md_theme_light_onSecondaryContainer @Composable get() = colorResource(R.color.on_secondary_container_light)
val md_theme_light_tertiary @Composable get() = colorResource(R.color.tertiary_light)
val md_theme_light_error @Composable get() = colorResource(R.color.error_light)
val md_theme_light_background @Composable get() = colorResource(R.color.background_light)
val md_theme_light_surfaceVariant @Composable get() = colorResource(R.color.surface_variant_light)
val md_theme_light_outline @Composable get() = colorResource(R.color.outline_light)

val md_theme_dark_primary @Composable get() = colorResource(R.color.primary_dark)
val md_theme_dark_onPrimary @Composable get() = colorResource(R.color.on_primary_dark)
val md_theme_dark_primaryContainer @Composable get() = colorResource(R.color.primary_container_dark)
val md_theme_dark_onPrimaryContainer @Composable get() = colorResource(R.color.on_primary_container_dark)
val md_theme_dark_secondary @Composable get() = colorResource(R.color.secondary_dark)
val md_theme_dark_secondaryContainer @Composable get() = colorResource(R.color.secondary_container_dark)
val md_theme_dark_onSecondaryContainer @Composable get() = colorResource(R.color.on_secondary_container_dark)
val md_theme_dark_tertiary @Composable get() = colorResource(R.color.tertiary_dark)
val md_theme_dark_error @Composable get() = colorResource(R.color.error_dark)
val md_theme_dark_background @Composable get() = colorResource(R.color.background_dark)
val md_theme_dark_surfaceVariant @Composable get() = colorResource(R.color.surface_variant_dark)
val md_theme_dark_outline @Composable get() = colorResource(R.color.outline_dark)

object AppColor {
    @Composable
    fun lightScheme(): ColorScheme = lightColorScheme(
        primary = md_theme_light_primary,
        onPrimary = md_theme_light_onPrimary,
        primaryContainer = md_theme_light_primaryContainer,
        onPrimaryContainer = md_theme_light_onPrimaryContainer,
        inversePrimary = md_theme_dark_primary,
        secondary = md_theme_light_secondary,
        onSecondary = Color.White,
        secondaryContainer = md_theme_light_secondaryContainer,
        onSecondaryContainer = md_theme_light_onSecondaryContainer,
        tertiary = md_theme_light_tertiary,
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFF9CF1F1),
        onTertiaryContainer = Color(0xFF002020),
        error = md_theme_light_error,
        onError = Color.White,
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        background = md_theme_light_background,
        onBackground = Color(0xFF1A1C1E),
        surface = md_theme_light_background,
        onSurface = Color(0xFF1A1C1E),
        surfaceVariant = md_theme_light_surfaceVariant,
        onSurfaceVariant = Color(0xFF42474E),
        outline = md_theme_light_outline,
        outlineVariant = Color(0xFFC2C7CF),
        inverseSurface = Color(0xFF2F3033),
        inverseOnSurface = Color(0xFFF0F0F4),
        surfaceDim = Color(0xFFD9DAE0),
        surfaceBright = Color(0xFFF8F9FF),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFF2F3F9),
        surfaceContainer = Color(0xFFECEEF4),
        surfaceContainerHigh = Color(0xFFE7E8EE),
        surfaceContainerHighest = Color(0xFFE1E2E8),
        scrim = Color.Black,
        surfaceTint = md_theme_light_primary,
        primaryFixed = md_theme_light_primaryContainer,
        primaryFixedDim = md_theme_dark_primary,
        onPrimaryFixed = md_theme_light_onPrimaryContainer,
        onPrimaryFixedVariant = md_theme_dark_primaryContainer,
        secondaryFixed = md_theme_light_secondaryContainer,
        secondaryFixedDim = md_theme_dark_secondary,
        onSecondaryFixed = Color(0xFF0C1D29),
        onSecondaryFixedVariant = md_theme_dark_secondaryContainer,
        tertiaryFixed = Color(0xFF9CF1F1),
        tertiaryFixedDim = md_theme_dark_tertiary,
        onTertiaryFixed = Color(0xFF002020),
        onTertiaryFixedVariant = Color(0xFF004F4F),
    )

    @Composable
    fun darkScheme(): ColorScheme = darkColorScheme(
        primary = md_theme_dark_primary,
        onPrimary = md_theme_dark_onPrimary,
        primaryContainer = md_theme_dark_primaryContainer,
        onPrimaryContainer = md_theme_dark_onPrimaryContainer,
        inversePrimary = md_theme_light_primary,
        secondary = md_theme_dark_secondary,
        onSecondary = Color(0xFF22323F),
        secondaryContainer = md_theme_dark_secondaryContainer,
        onSecondaryContainer = md_theme_dark_onSecondaryContainer,
        tertiary = md_theme_dark_tertiary,
        onTertiary = Color(0xFF003737),
        tertiaryContainer = Color(0xFF004F4F),
        onTertiaryContainer = Color(0xFF9CF1F1),
        error = md_theme_dark_error,
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        background = md_theme_dark_background,
        onBackground = Color(0xFFE2E2E6),
        surface = md_theme_dark_background,
        onSurface = Color(0xFFE2E2E6),
        surfaceVariant = md_theme_dark_surfaceVariant,
        onSurfaceVariant = Color(0xFFC2C7CF),
        outline = md_theme_dark_outline,
        outlineVariant = Color(0xFF42474E),
        inverseSurface = Color(0xFFE2E2E6),
        inverseOnSurface = Color(0xFF2F3033),
        surfaceDim = Color(0xFF111316),
        surfaceBright = Color(0xFF37393C),
        surfaceContainerLowest = Color(0xFF0D0E11),
        surfaceContainerLow = Color(0xFF1A1C1E),
        surfaceContainer = Color(0xFF1E2022),
        surfaceContainerHigh = Color(0xFF282A2D),
        surfaceContainerHighest = Color(0xFF333538),
        scrim = Color.Black,
        surfaceTint = md_theme_dark_primary,
        primaryFixed = md_theme_light_primaryContainer,
        primaryFixedDim = md_theme_dark_primary,
        onPrimaryFixed = md_theme_light_onPrimaryContainer,
        onPrimaryFixedVariant = md_theme_dark_primaryContainer,
        secondaryFixed = md_theme_light_secondaryContainer,
        secondaryFixedDim = md_theme_dark_secondary,
        onSecondaryFixed = Color(0xFF0C1D29),
        onSecondaryFixedVariant = md_theme_dark_secondaryContainer,
        tertiaryFixed = Color(0xFF9CF1F1),
        tertiaryFixedDim = md_theme_dark_tertiary,
        onTertiaryFixed = Color(0xFF002020),
        onTertiaryFixedVariant = Color(0xFF004F4F),
    )
}

