package com.example.poop.ui.theme

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource
import androidx.core.content.ContextCompat
import com.example.poop.R

/**
 * 1. 标准变量名映射 (Material Theme Builder 风格)
 * 将 XML 的数据源映射为 Compose 的类型安全对象
 */
val md_theme_light_primary @Composable get() = colorResource(R.color.primary_light)
val md_theme_light_onPrimary @Composable get() = colorResource(R.color.on_primary_light)
val md_theme_light_primaryContainer @Composable get() = colorResource(R.color.primary_container_light)
val md_theme_light_onPrimaryContainer @Composable get() = colorResource(R.color.on_primary_container_light)
val md_theme_light_secondary @Composable get() = colorResource(R.color.secondary_light)
val md_theme_light_onSecondary @Composable get() = colorResource(R.color.on_secondary_light)
val md_theme_light_secondaryContainer @Composable get() = colorResource(R.color.secondary_container_light)
val md_theme_light_onSecondaryContainer @Composable get() = colorResource(R.color.on_secondary_container_light)
val md_theme_light_tertiary @Composable get() = colorResource(R.color.tertiary_light)
val md_theme_light_onTertiary @Composable get() = colorResource(R.color.on_tertiary_light)
val md_theme_light_tertiaryContainer @Composable get() = colorResource(R.color.tertiary_container_light)
val md_theme_light_onTertiaryContainer @Composable get() = colorResource(R.color.on_tertiary_container_light)
val md_theme_light_error @Composable get() = colorResource(R.color.error_light)
val md_theme_light_onError @Composable get() = colorResource(R.color.on_error_light)
val md_theme_light_background @Composable get() = colorResource(R.color.background_light)
val md_theme_light_onBackground @Composable get() = colorResource(R.color.on_background_light)
val md_theme_light_surface @Composable get() = colorResource(R.color.surface_light)
val md_theme_light_onSurface @Composable get() = colorResource(R.color.on_surface_light)
val md_theme_light_surfaceVariant @Composable get() = colorResource(R.color.surface_variant_light)
val md_theme_light_onSurfaceVariant @Composable get() = colorResource(R.color.on_surface_variant_light)
val md_theme_light_outline @Composable get() = colorResource(R.color.outline_light)

val md_theme_dark_primary @Composable get() = colorResource(R.color.primary_dark)
val md_theme_dark_onPrimary @Composable get() = colorResource(R.color.on_primary_dark)
val md_theme_dark_primaryContainer @Composable get() = colorResource(R.color.primary_container_dark)
val md_theme_dark_onPrimaryContainer @Composable get() = colorResource(R.color.on_primary_container_dark)
val md_theme_dark_secondary @Composable get() = colorResource(R.color.secondary_dark)
val md_theme_dark_onSecondary @Composable get() = colorResource(R.color.on_secondary_dark)
val md_theme_dark_secondaryContainer @Composable get() = colorResource(R.color.secondary_container_dark)
val md_theme_dark_onSecondaryContainer @Composable get() = colorResource(R.color.on_secondary_container_dark)
val md_theme_dark_tertiary @Composable get() = colorResource(R.color.tertiary_dark)
val md_theme_dark_onTertiary @Composable get() = colorResource(R.color.on_tertiary_dark)
val md_theme_dark_tertiaryContainer @Composable get() = colorResource(R.color.tertiary_container_dark)
val md_theme_dark_onTertiaryContainer @Composable get() = colorResource(R.color.on_tertiary_container_dark)
val md_theme_dark_error @Composable get() = colorResource(R.color.error_dark)
val md_theme_dark_onError @Composable get() = colorResource(R.color.on_error_dark)
val md_theme_dark_background @Composable get() = colorResource(R.color.background_dark)
val md_theme_dark_onBackground @Composable get() = colorResource(R.color.on_background_dark)
val md_theme_dark_surface @Composable get() = colorResource(R.color.surface_dark)
val md_theme_dark_onSurface @Composable get() = colorResource(R.color.on_surface_dark)
val md_theme_dark_surfaceVariant @Composable get() = colorResource(R.color.surface_variant_dark)
val md_theme_dark_onSurfaceVariant @Composable get() = colorResource(R.color.on_surface_variant_dark)
val md_theme_dark_outline @Composable get() = colorResource(R.color.outline_dark)

/**
 * 2. AppColor: 颜色的“逻辑封装层”
 */
object AppColor {

    @Composable
    fun getScheme(isDark: Boolean): ColorScheme {
        return if (isDark) darkScheme() else lightScheme()
    }

    @Composable
    fun lightScheme(): ColorScheme = lightColorScheme(
        primary = md_theme_light_primary,
        onPrimary = md_theme_light_onPrimary,
        primaryContainer = md_theme_light_primaryContainer,
        onPrimaryContainer = md_theme_light_onPrimaryContainer,
        secondary = md_theme_light_secondary,
        onSecondary = md_theme_light_onSecondary,
        secondaryContainer = md_theme_light_secondaryContainer,
        onSecondaryContainer = md_theme_light_onSecondaryContainer,
        tertiary = md_theme_light_tertiary,
        onTertiary = md_theme_light_onTertiary,
        tertiaryContainer = md_theme_light_tertiaryContainer,
        onTertiaryContainer = md_theme_light_onTertiaryContainer,
        error = md_theme_light_error,
        onError = md_theme_light_onError,
        background = md_theme_light_background,
        onBackground = md_theme_light_onBackground,
        surface = md_theme_light_surface,
        onSurface = md_theme_light_onSurface,
        surfaceVariant = md_theme_light_surfaceVariant,
        onSurfaceVariant = md_theme_light_onSurfaceVariant,
        outline = md_theme_light_outline
    )

    @Composable
    fun darkScheme(): ColorScheme = darkColorScheme(
        primary = md_theme_dark_primary,
        onPrimary = md_theme_dark_onPrimary,
        primaryContainer = md_theme_dark_primaryContainer,
        onPrimaryContainer = md_theme_dark_onPrimaryContainer,
        secondary = md_theme_dark_secondary,
        onSecondary = md_theme_dark_onSecondary,
        secondaryContainer = md_theme_dark_secondaryContainer,
        onSecondaryContainer = md_theme_dark_onSecondaryContainer,
        tertiary = md_theme_dark_tertiary,
        onTertiary = md_theme_dark_onTertiary,
        tertiaryContainer = md_theme_dark_tertiaryContainer,
        onTertiaryContainer = md_theme_dark_onTertiaryContainer,
        error = md_theme_dark_error,
        onError = md_theme_dark_onError,
        background = md_theme_dark_background,
        onBackground = md_theme_dark_onBackground,
        surface = md_theme_dark_surface,
        onSurface = md_theme_dark_onSurface,
        surfaceVariant = md_theme_dark_surfaceVariant,
        onSurfaceVariant = md_theme_dark_onSurfaceVariant,
        outline = md_theme_dark_outline
    )

    // 逻辑：动态计算颜色（例如按下态调暗 20%）
    fun getPrimaryPressed(context: Context, factor: Float = 0.8f): Int {
        val primary = ContextCompat.getColor(context, R.color.primary_light)
        val r = (android.graphics.Color.red(primary) * factor).toInt().coerceIn(0, 255)
        val g = (android.graphics.Color.green(primary) * factor).toInt().coerceIn(0, 255)
        val b = (android.graphics.Color.blue(primary) * factor).toInt().coerceIn(0, 255)
        return android.graphics.Color.rgb(r, g, b)
    }
}
