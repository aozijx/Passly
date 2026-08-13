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
import com.aozijx.passly.data.settings.model.FallbackPalette
import com.aozijx.passly.data.settings.model.FontFamilyMode
import com.aozijx.passly.data.settings.model.InterfaceStyleConstraints
import com.aozijx.passly.data.settings.model.ThemeMode

/**
 * [color] 是兼容既有 DataStore 字段的选择键；真正应用的是包含三组强调色的 [palette]。
 * color 为 0 表示使用默认 AppColor。
 */
data class ThemePreset(
    val color: Long,
    val nameKey: Int,
    val palette: ThemePalette? = null
)

private fun accentFamily(
    lightAccent: Long,
    lightOnAccent: Long,
    lightContainer: Long,
    lightOnContainer: Long,
    darkAccent: Long,
    darkOnAccent: Long,
    darkContainer: Long,
    darkOnContainer: Long,
) = AccentColorFamily(
    light = AccentColorRoles(
        accent = Color(lightAccent),
        onAccent = Color(lightOnAccent),
        container = Color(lightContainer),
        onContainer = Color(lightOnContainer),
    ),
    dark = AccentColorRoles(
        accent = Color(darkAccent),
        onAccent = Color(darkOnAccent),
        container = Color(darkContainer),
        onContainer = Color(darkOnContainer),
    ),
)

/*
 * Static Material 3 role families. These are design tokens, not runtime seeds:
 * each family explicitly supplies light/dark accent, container and foreground roles.
 */
private val AzureAccent = accentFamily(
    0xFF006495, 0xFFFFFFFF, 0xFFCBE6FF, 0xFF001E30,
    0xFF8FCDFF, 0xFF003450, 0xFF004C71, 0xFFCBE6FF,
)
private val SlateAccent = accentFamily(
    0xFF50606E, 0xFFFFFFFF, 0xFFD3E5F5, 0xFF0C1D29,
    0xFFB8C8D9, 0xFF22323F, 0xFF384956, 0xFFD3E5F5,
)
private val AquaAccent = accentFamily(
    0xFF006A6A, 0xFFFFFFFF, 0xFF9CF1F1, 0xFF002020,
    0xFF81D5D5, 0xFF003737, 0xFF004F4F, 0xFF9CF1F1,
)
private val GreenAccent = accentFamily(
    0xFF286C3A, 0xFFFFFFFF, 0xFFACF4B5, 0xFF002108,
    0xFF91D89B, 0xFF003914, 0xFF0D5225, 0xFFACF4B5,
)
private val AmberAccent = accentFamily(
    0xFF795900, 0xFFFFFFFF, 0xFFFFDEA5, 0xFF261900,
    0xFFF5BE48, 0xFF402D00, 0xFF5D4200, 0xFFFFDEA5,
)
private val RedAccent = accentFamily(
    0xFFB3261E, 0xFFFFFFFF, 0xFFF9DEDC, 0xFF410E0B,
    0xFFF2B8B5, 0xFF601410, 0xFF8C1D18, 0xFFF9DEDC,
)
private val RoseAccent = accentFamily(
    0xFF7D5260, 0xFFFFFFFF, 0xFFFFD8E4, 0xFF31111D,
    0xFFEFB8C8, 0xFF492532, 0xFF633B48, 0xFFFFD8E4,
)
private val GoldAccent = accentFamily(
    0xFF735C00, 0xFFFFFFFF, 0xFFFFE17B, 0xFF241A00,
    0xFFE9C349, 0xFF3C2F00, 0xFF574500, 0xFFFFE17B,
)
private val PurpleAccent = accentFamily(
    0xFF6750A4, 0xFFFFFFFF, 0xFFEADDFF, 0xFF21005D,
    0xFFD0BCFF, 0xFF381E72, 0xFF4F378B, 0xFFEADDFF,
)
private val LavenderAccent = accentFamily(
    0xFF625B71, 0xFFFFFFFF, 0xFFE8DEF8, 0xFF1D192B,
    0xFFCCC2DC, 0xFF332D41, 0xFF4A4458, 0xFFE8DEF8,
)
private val OrangeAccent = accentFamily(
    0xFF8B5000, 0xFFFFFFFF, 0xFFFFDCC2, 0xFF2C1600,
    0xFFFFB870, 0xFF4A2800, 0xFF6A3C00, 0xFFFFDCC2,
)
private val BrownAccent = accentFamily(
    0xFF6F4F46, 0xFFFFFFFF, 0xFFF9DBD1, 0xFF281813,
    0xFFDCB9AF, 0xFF3F2D27, 0xFF573A32, 0xFFF9DBD1,
)
private val OliveAccent = accentFamily(
    0xFF5D6400, 0xFFFFFFFF, 0xFFE2EA7B, 0xFF1B1D00,
    0xFFC6CE62, 0xFF303300, 0xFF454B00, 0xFFE2EA7B,
)
private val PinkAccent = accentFamily(
    0xFFA90052, 0xFFFFFFFF, 0xFFFFD9E2, 0xFF3F001B,
    0xFFFFB0C8, 0xFF65002F, 0xFF8B0043, 0xFFFFD9E2,
)

private val BlueThemePalette = ThemePalette(AzureAccent, SlateAccent, AquaAccent)
private val GreenThemePalette = ThemePalette(GreenAccent, AquaAccent, AmberAccent)
private val RedThemePalette = ThemePalette(RedAccent, RoseAccent, GoldAccent)
private val PurpleThemePalette = ThemePalette(PurpleAccent, LavenderAccent, RoseAccent)
private val OrangeThemePalette = ThemePalette(OrangeAccent, BrownAccent, RoseAccent)
private val TealThemePalette = ThemePalette(AquaAccent, SlateAccent, OliveAccent)
private val PinkThemePalette = ThemePalette(PinkAccent, RoseAccent, OrangeAccent)

val themePresets = listOf(
    ThemePreset(0, R.string.settings_theme_color_default),
    ThemePreset(
        color = 0xFF4285F4,
        nameKey = R.string.settings_theme_color_blue,
        palette = BlueThemePalette,
    ),
    ThemePreset(
        color = 0xFF34A853,
        nameKey = R.string.settings_theme_color_green,
        palette = GreenThemePalette,
    ),
    ThemePreset(
        color = 0xFFEA4335,
        nameKey = R.string.settings_theme_color_red,
        palette = RedThemePalette,
    ),
    ThemePreset(
        color = 0xFF9C27B0,
        nameKey = R.string.settings_theme_color_purple,
        palette = PurpleThemePalette,
    ),
    ThemePreset(
        color = 0xFFFF9800,
        nameKey = R.string.settings_theme_color_orange,
        palette = OrangeThemePalette,
    ),
    ThemePreset(
        color = 0xFF009688,
        nameKey = R.string.settings_theme_color_teal,
        palette = TealThemePalette,
    ),
    ThemePreset(
        color = 0xFFE91E63,
        nameKey = R.string.settings_theme_color_pink,
        palette = PinkThemePalette,
    ),
)

fun themePresetByColor(color: Long): ThemePreset =
    themePresets.firstOrNull { it.color == color } ?: themePresets.first()

internal fun manualThemePalette(color: Long?): ThemePalette? =
    color
        ?.takeIf { it != 0L }
        ?.let { selected -> themePresets.firstOrNull { it.color == selected }?.palette }

fun themePresetByFallbackPalette(palette: FallbackPalette): ThemePreset {
    val color = when (palette) {
        FallbackPalette.BLUE -> 0xFF4285F4
        FallbackPalette.GREEN -> 0xFF34A853
        FallbackPalette.RED -> 0xFFEA4335
        FallbackPalette.PURPLE -> 0xFF9C27B0
        FallbackPalette.ORANGE -> 0xFFFF9800
        FallbackPalette.TEAL -> 0xFF009688
        FallbackPalette.PINK -> 0xFFE91E63
    }
    return themePresetByColor(color)
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
    val selectedPalette = remember(manualThemeColorArgb) {
        manualThemePalette(manualThemeColorArgb)
    }
    val appColorScheme = if (isDark) AppColor.darkScheme() else AppColor.lightScheme()
    val colorScheme = when {
        dynamicColor -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        selectedPalette != null -> selectedPalette.applyTo(appColorScheme, isDark)
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
