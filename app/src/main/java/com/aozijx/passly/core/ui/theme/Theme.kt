package com.aozijx.passly.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.aozijx.passly.R
import com.aozijx.passly.domain.settings.model.FontFamilyMode
import com.aozijx.passly.domain.settings.model.InterfaceStyleConstraints
import com.aozijx.passly.domain.settings.model.ThemeMode

/**
 * [color] 是兼容既有 DataStore 字段的选择键；真正应用的是包含三组强调色的 [palette]。
 * color 为 0 表示使用默认 AppColor。
 */
data class ThemePreset(
    val color: Long,
    val nameKey: Int,
    val palette: ThemePalette? = null
)

val themePresets = listOf(
    ThemePreset(0, R.string.default_label),
    ThemePreset(
        color = 0xFF4285F4,
        nameKey = R.string.settings_theme_color_blue,
        palette = ThemePalette(0xFF4285F4, 0xFF5B5F97, 0xFFA5417A)
    ),
    ThemePreset(
        color = 0xFF34A853,
        nameKey = R.string.settings_theme_color_green,
        palette = ThemePalette(0xFF34A853, 0xFF00796B, 0xFFA06400)
    ),
    ThemePreset(
        color = 0xFFEA4335,
        nameKey = R.string.settings_theme_color_red,
        palette = ThemePalette(0xFFEA4335, 0xFF8E4B61, 0xFF8A5A00)
    ),
    ThemePreset(
        color = 0xFF9C27B0,
        nameKey = R.string.settings_theme_color_purple,
        palette = ThemePalette(0xFF9C27B0, 0xFF4658A9, 0xFFC23E70)
    ),
    ThemePreset(
        color = 0xFFFF9800,
        nameKey = R.string.settings_theme_color_orange,
        palette = ThemePalette(0xFFFF9800, 0xFF795548, 0xFFC13E68)
    ),
    ThemePreset(
        color = 0xFF009688,
        nameKey = R.string.settings_theme_color_teal,
        palette = ThemePalette(0xFF009688, 0xFF3974B8, 0xFF708B22)
    ),
    ThemePreset(
        color = 0xFFE91E63,
        nameKey = R.string.settings_theme_color_pink,
        palette = ThemePalette(0xFFE91E63, 0xFF8246AF, 0xFFE7653B)
    ),
)

fun themePresetByColor(color: Long): ThemePreset =
    themePresets.firstOrNull { it.color == color } ?: themePresets.first()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    customSeedArgb: Long? = null,
    fontFamily: FontFamilyMode = FontFamilyMode.APP_BUNDLED,
    expressive: Boolean = true,
    outerCornerRadiusDp: Float = InterfaceStyleConstraints.DEFAULT_OUTER_RADIUS_DP,
    innerCornerRadiusDp: Float = InterfaceStyleConstraints.DEFAULT_INNER_RADIUS_DP,
    groupItemSpacingDp: Float = InterfaceStyleConstraints.DEFAULT_ITEM_SPACING_DP,
    groupContentPaddingDp: Float = InterfaceStyleConstraints.DEFAULT_CONTENT_PADDING_DP,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val selectedPalette = remember(customSeedArgb) {
        customSeedArgb
            ?.takeIf { it != 0L }
            ?.let { selected -> themePresets.firstOrNull { it.color == selected }?.palette }
    }
    val appColorScheme = if (isDark) AppColor.darkScheme() else AppColor.lightScheme()
    val colorScheme = when {
        selectedPalette != null -> selectedPalette.applyTo(appColorScheme, isDark)
        dynamicColor -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> appColorScheme
    }
    val typography =
        if (fontFamily == FontFamilyMode.SYSTEM) SystemTypography else themeTypography()
    val themeDefinition = remember(
        expressive,
        outerCornerRadiusDp,
        innerCornerRadiusDp,
        groupItemSpacingDp,
        groupContentPaddingDp
    ) {
        passlyThemeDefinition(
            expressive = expressive,
            outerCornerRadiusDp = outerCornerRadiusDp,
            innerCornerRadiusDp = innerCornerRadiusDp,
            groupItemSpacingDp = groupItemSpacingDp,
            groupContentPaddingDp = groupContentPaddingDp
        )
    }

    CompositionLocalProvider(LocalPasslyThemeTokens provides themeDefinition.tokens) {
        if (expressive) {
            MaterialExpressiveTheme(
                colorScheme = colorScheme,
                motionScheme = MotionScheme.expressive(),
                typography = typography,
                shapes = themeDefinition.shapes,
                content = content
            )
        } else {
            MaterialTheme(
                colorScheme = colorScheme,
                motionScheme = MotionScheme.standard(),
                typography = typography,
                shapes = themeDefinition.shapes,
                content = content
            )
        }
    }
}
