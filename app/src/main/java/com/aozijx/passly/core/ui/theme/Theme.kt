package com.aozijx.passly.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aozijx.passly.R

private val PasslyShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

/** 预设主题色。color 为 0 表示使用默认 AppColor。 */
data class ThemePreset(
    val color: Long,
    val nameKey: Int
)

val themePresets = listOf(
    ThemePreset(0, R.string.default_label),
    ThemePreset(0xFF4285F4, R.string.settings_theme_color_blue),
    ThemePreset(0xFF34A853, R.string.settings_theme_color_green),
    ThemePreset(0xFFEA4335, R.string.settings_theme_color_red),
    ThemePreset(0xFF9C27B0, R.string.settings_theme_color_purple),
    ThemePreset(0xFFFF9800, R.string.settings_theme_color_orange),
    ThemePreset(0xFF009688, R.string.settings_theme_color_teal),
    ThemePreset(0xFFE91E63, R.string.settings_theme_color_pink),
)

fun themePresetByColor(color: Long): ThemePreset =
    themePresets.firstOrNull { it.color == color } ?: themePresets.first()

/**
 * 将种子色微混入背景/表面色中。
 * Material3 的 seedColor 方案生成的 neutral 调色板不带主题色 tint，
 * 所以手动 blend 让背景和卡片也能体现所选主题。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTheme(
    darkTheme: Boolean? = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    themeColor: Long = 0,
    useSystemFont: Boolean = false,
    content: @Composable () -> Unit
) {
    val isDark = darkTheme ?: isSystemInDarkTheme()
    val context = LocalContext.current
    val seedColor = remember(themeColor) {
        if (themeColor != 0L) Color(themeColor) else null
    }

    val colorScheme = when {
        seedColor != null -> {
            val base = if (isDark) darkColorScheme(seedColor) else lightColorScheme(seedColor)
            tintSurfaceColors(base, seedColor, isDark)
        }
        dynamicColor -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> AppColor.darkScheme()
        else -> AppColor.lightScheme()
    }

    val typography = if (useSystemFont) SystemTypography else themeTypography()

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        shapes = PasslyShapes,
        content = content
    )
}

/** 将种子色微量混入背景与表面色，使整体界面呈现主题色调。 */
private fun tintSurfaceColors(
    base: androidx.compose.material3.ColorScheme,
    seed: Color,
    isDark: Boolean
): androidx.compose.material3.ColorScheme {
    return if (isDark) {
        base.copy(
            primaryContainer = lerp(base.primaryContainer, base.primary, 0.15f),
            secondaryContainer = lerp(base.secondaryContainer, base.secondary, 0.12f),
            background = lerp(base.background, seed, 0.08f),
            surface = lerp(base.surface, seed, 0.10f),
            surfaceContainerLowest = lerp(base.surfaceContainerLowest, seed, 0.06f),
            surfaceContainerLow = lerp(base.surfaceContainerLow, seed, 0.08f),
            surfaceContainer = lerp(base.surfaceContainer, seed, 0.10f),
            surfaceContainerHigh = lerp(base.surfaceContainerHigh, seed, 0.12f),
            surfaceContainerHighest = lerp(base.surfaceContainerHighest, seed, 0.13f),
            surfaceVariant = lerp(base.surfaceVariant, seed, 0.08f),
            surfaceDim = lerp(base.surfaceDim, seed, 0.06f),
            surfaceBright = lerp(base.surfaceBright, seed, 0.12f),
        )
    } else {
        base.copy(
            background = lerp(base.background, seed, 0.10f),
            surface = lerp(base.surface, seed, 0.12f),
            surfaceContainerLowest = lerp(base.surfaceContainerLowest, seed, 0.08f),
            surfaceContainerLow = lerp(base.surfaceContainerLow, seed, 0.10f),
            surfaceContainer = lerp(base.surfaceContainer, seed, 0.12f),
            surfaceContainerHigh = lerp(base.surfaceContainerHigh, seed, 0.13f),
            surfaceContainerHighest = lerp(base.surfaceContainerHighest, seed, 0.15f),
            surfaceVariant = lerp(base.surfaceVariant, seed, 0.10f),
            surfaceDim = lerp(base.surfaceDim, seed, 0.08f),
            surfaceBright = lerp(base.surfaceBright, seed, 0.13f),
        )
    }
}
