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
import androidx.compose.ui.platform.LocalContext
import com.aozijx.passly.domain.settings.model.FallbackPalette
import com.aozijx.passly.domain.settings.model.FontFamilyMode
import com.aozijx.passly.domain.settings.model.InterfaceStyleConstraints
import com.aozijx.passly.domain.settings.model.ThemeMode

fun FallbackPalette.accentSeeds(): AccentSeeds = when (this) {
    FallbackPalette.BLUE -> AccentSeeds(0xFF4285F4, 0xFF50606E, 0xFF006A6A)
    FallbackPalette.GREEN -> AccentSeeds(0xFF34A853, 0xFF006A6A, 0xFF795900)
    FallbackPalette.RED -> AccentSeeds(0xFFEA4335, 0xFF7D5260, 0xFF735C00)
    FallbackPalette.PURPLE -> AccentSeeds(0xFF9C27B0, 0xFF625B71, 0xFF7D5260)
    FallbackPalette.ORANGE -> AccentSeeds(0xFFFF9800, 0xFF6F4F46, 0xFF5D6400)
    FallbackPalette.TEAL -> AccentSeeds(0xFF009688, 0xFF50606E, 0xFF5D6400)
    FallbackPalette.PINK -> AccentSeeds(0xFFE91E63, 0xFF7D5260, 0xFF8B5000)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    fallbackPalette: FallbackPalette = FallbackPalette.BLUE,
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
    val selectedSeeds = remember(fallbackPalette) { fallbackPalette.accentSeeds() }
    val appColorScheme = if (isDark) AppColor.darkScheme() else AppColor.lightScheme()
    val colorScheme = when {
        dynamicColor -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> appColorScheme.withGeneratedAccents(selectedSeeds, isDark)
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
