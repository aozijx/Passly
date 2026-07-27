package com.aozijx.passly.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.colorResource
import com.aozijx.passly.R

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
        secondary = md_theme_light_secondary,
        onSecondary = Color.White,
        secondaryContainer = md_theme_light_secondaryContainer,
        onSecondaryContainer = md_theme_light_onSecondaryContainer,
        tertiary = md_theme_light_tertiary,
        onTertiary = Color.White,
        error = md_theme_light_error,
        onError = Color.White,
        background = md_theme_light_background,
        onBackground = Color(0xFF1A1C1E),
        surface = md_theme_light_background,
        onSurface = Color(0xFF1A1C1E),
        surfaceVariant = md_theme_light_surfaceVariant,
        onSurfaceVariant = Color(0xFF42474E),
        outline = md_theme_light_outline
    )

    @Composable
    fun darkScheme(): ColorScheme = darkColorScheme(
        primary = md_theme_dark_primary,
        onPrimary = md_theme_dark_onPrimary,
        primaryContainer = md_theme_dark_primaryContainer,
        onPrimaryContainer = md_theme_dark_onPrimaryContainer,
        secondary = md_theme_dark_secondary,
        onSecondary = Color(0xFF22323F),
        secondaryContainer = md_theme_dark_secondaryContainer,
        onSecondaryContainer = md_theme_dark_onSecondaryContainer,
        tertiary = md_theme_dark_tertiary,
        onTertiary = Color(0xFF003737),
        error = md_theme_dark_error,
        onError = Color(0xFF690005),
        background = md_theme_dark_background,
        onBackground = Color(0xFFE2E2E6),
        surface = md_theme_dark_background,
        onSurface = Color(0xFFE2E2E6),
        surfaceVariant = md_theme_dark_surfaceVariant,
        onSurfaceVariant = Color(0xFFC2C7CF),
        outline = md_theme_dark_outline
    )
}

/**
 * 一套品牌配色包含三个独立的强调色，而不是把一个颜色硬塞进所有角色。
 * 每个强调色分别生成 primary、secondary、tertiary 角色族。
 */
@Immutable
data class ThemePalette(
    val primaryArgb: Long,
    val secondaryArgb: Long,
    val tertiaryArgb: Long
) {
    val primary: Color get() = Color(primaryArgb)
    val secondary: Color get() = Color(secondaryArgb)
    val tertiary: Color get() = Color(tertiaryArgb)
    val previewColors: List<Color> get() = listOf(primary, secondary, tertiary)
}

internal fun ThemePalette.applyTo(
    base: ColorScheme,
    isDark: Boolean
): ColorScheme {
    val primaryRoles = primary.toRoleFamily(isDark)
    val secondaryRoles = secondary.toRoleFamily(isDark)
    val tertiaryRoles = tertiary.toRoleFamily(isDark)
    val surfaceAccent = lerp(primary, tertiary, 0.28f)
    val surfaceTintFraction = if (isDark) 0.08f else 0.055f

    return base.copy(
        primary = primaryRoles.accent,
        onPrimary = primaryRoles.onAccent,
        primaryContainer = primaryRoles.container,
        onPrimaryContainer = primaryRoles.onContainer,
        inversePrimary = primary.toRoleFamily(!isDark).accent,
        secondary = secondaryRoles.accent,
        onSecondary = secondaryRoles.onAccent,
        secondaryContainer = secondaryRoles.container,
        onSecondaryContainer = secondaryRoles.onContainer,
        tertiary = tertiaryRoles.accent,
        onTertiary = tertiaryRoles.onAccent,
        tertiaryContainer = tertiaryRoles.container,
        onTertiaryContainer = tertiaryRoles.onContainer,
        background = lerp(base.background, surfaceAccent, surfaceTintFraction * 0.7f),
        surface = lerp(base.surface, surfaceAccent, surfaceTintFraction),
        surfaceTint = primaryRoles.accent,
        surfaceVariant = lerp(base.surfaceVariant, secondary, surfaceTintFraction),
        surfaceContainerLowest = lerp(
            base.surfaceContainerLowest,
            surfaceAccent,
            surfaceTintFraction * 0.45f
        ),
        surfaceContainerLow = lerp(
            base.surfaceContainerLow,
            surfaceAccent,
            surfaceTintFraction * 0.65f
        ),
        surfaceContainer = lerp(
            base.surfaceContainer,
            surfaceAccent,
            surfaceTintFraction * 0.85f
        ),
        surfaceContainerHigh = lerp(
            base.surfaceContainerHigh,
            surfaceAccent,
            surfaceTintFraction
        ),
        surfaceContainerHighest = lerp(
            base.surfaceContainerHighest,
            surfaceAccent,
            surfaceTintFraction * 1.15f
        )
    )
}

private data class ColorRoleFamily(
    val accent: Color,
    val onAccent: Color,
    val container: Color,
    val onContainer: Color
)

private fun Color.toRoleFamily(isDark: Boolean): ColorRoleFamily {
    val accent = if (isDark) {
        lerp(this, Color.White, 0.42f)
    } else {
        lerp(this, Color.Black, 0.08f)
    }
    val container = if (isDark) {
        lerp(this, Color.Black, 0.43f)
    } else {
        lerp(this, Color.White, 0.78f)
    }
    return ColorRoleFamily(
        accent = accent,
        onAccent = accent.readableContentColor(),
        container = container,
        onContainer = container.readableContentColor()
    )
}

private fun Color.readableContentColor(): Color =
    if (luminance() > 0.42f) Color.Black else Color.White
