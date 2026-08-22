package com.aozijx.passly.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource

/** Resolves the active color scheme, keeping policy outside the theme host composable. */
@Composable
fun rememberAppColorScheme(
    isDark: Boolean,
    dynamicColor: Boolean,
    themeKey: String,
    canvasTintPercent: Int,
): ColorScheme {
    val context = LocalContext.current
    val selectedScheme = remember(themeKey) { AppThemeSchemes.find(themeKey) }
    val baseColorScheme = if (isDark) darkColorScheme() else lightColorScheme()

    return when {
        dynamicColor -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        selectedScheme != null -> baseColorScheme.withGeneratedAccents(
            seeds = selectedScheme.resolveAccentSeeds(),
            isDark = isDark,
            canvasTintFraction = canvasTintPercent / 100f,
        )
        else -> baseColorScheme.withGeneratedAccents(
            seeds = AppThemeSchemes.all.first().resolveAccentSeeds(),
            isDark = isDark,
            canvasTintFraction = canvasTintPercent / 100f,
        )
    }
}

@Composable
fun ThemeSchemeDefinition.resolveSeedColors(): List<Color> = listOf(
    colorResource(seedResources.primary),
    colorResource(seedResources.secondary),
    colorResource(seedResources.tertiary),
)

@Composable
fun ThemeSchemeDefinition.resolveAccentSeeds(): AccentSeeds {
    val colors = resolveSeedColors()
    return AccentSeeds(
        primary = colors[0].toArgb().toLong(),
        secondary = colors[1].toArgb().toLong(),
        tertiary = colors[2].toArgb().toLong(),
    )
}
