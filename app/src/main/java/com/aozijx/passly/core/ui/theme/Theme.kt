package com.aozijx.passly.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
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
)

val themePresets = listOf(
    ThemePreset(0, R.string.settings_theme_color_default),
    ThemePreset(
        color = 0xFF4285F4,
        nameKey = R.string.settings_theme_color_blue,
    ),
    ThemePreset(
        color = 0xFF34A853,
        nameKey = R.string.settings_theme_color_green,
    ),
    ThemePreset(
        color = 0xFFEA4335,
        nameKey = R.string.settings_theme_color_red,
    ),
    ThemePreset(
        color = 0xFF9C27B0,
        nameKey = R.string.settings_theme_color_purple,
    ),
    ThemePreset(
        color = 0xFFFF9800,
        nameKey = R.string.settings_theme_color_orange,
    ),
    ThemePreset(
        color = 0xFF009688,
        nameKey = R.string.settings_theme_color_teal,
    ),
    ThemePreset(
        color = 0xFFE91E63,
        nameKey = R.string.settings_theme_color_pink,
    ),
)

fun themePresetByColor(color: Long): ThemePreset =
    themePresets.firstOrNull { it.color == color } ?: themePresets.first()

fun manualThemeSeeds(color: Long?): AccentSeeds? = when (color) {
    0xFF4285F4 -> AccentSeeds(0xFF4285F4, 0xFF50606E, 0xFF006A6A)
    0xFF34A853 -> AccentSeeds(0xFF34A853, 0xFF006A6A, 0xFF795900)
    0xFFEA4335 -> AccentSeeds(0xFFEA4335, 0xFF7D5260, 0xFF735C00)
    0xFF9C27B0 -> AccentSeeds(0xFF9C27B0, 0xFF625B71, 0xFF7D5260)
    0xFFFF9800 -> AccentSeeds(0xFFFF9800, 0xFF6F4F46, 0xFF5D6400)
    0xFF009688 -> AccentSeeds(0xFF009688, 0xFF50606E, 0xFF5D6400)
    0xFFE91E63 -> AccentSeeds(0xFFE91E63, 0xFF7D5260, 0xFF8B5000)
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    manualThemeColorArgb: Long? = null,
    fontFamily: FontFamilyMode = FontFamilyMode.APP_BUNDLED,
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
    val selectedSeeds = remember(manualThemeColorArgb) {
        manualThemeSeeds(manualThemeColorArgb)
    }
    val appColorScheme = if (isDark) AppColor.darkScheme() else AppColor.lightScheme()
    val colorScheme = when {
        dynamicColor -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        selectedSeeds != null -> appColorScheme.withGeneratedAccents(selectedSeeds, isDark)
        else -> appColorScheme
    }
    val typography =
        if (fontFamily == FontFamilyMode.SYSTEM) SystemTypography else themeTypography()
    val themeDefinition = remember(
        outerCornerRadiusDp,
        innerCornerRadiusDp,
        groupItemSpacingDp,
        groupContentPaddingDp
    ) {
        passlyThemeDefinition(
            outerCornerRadiusDp = outerCornerRadiusDp,
            innerCornerRadiusDp = innerCornerRadiusDp,
            groupItemSpacingDp = groupItemSpacingDp,
            groupContentPaddingDp = groupContentPaddingDp
        )
    }

    CompositionLocalProvider(LocalPasslyThemeTokens provides themeDefinition.tokens) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = MotionScheme.expressive(),
            typography = typography,
            shapes = themeDefinition.shapes,
            content = content
        )
    }
}
