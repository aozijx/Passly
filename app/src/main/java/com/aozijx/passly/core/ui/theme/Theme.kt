package com.aozijx.passly.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import com.aozijx.passly.domain.settings.model.FontFamilyMode
import com.aozijx.passly.domain.settings.model.InterfaceStyleConstraints
import com.aozijx.passly.domain.settings.model.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    themeKey: String = "",
    canvasTintPercent: Int = 8,
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
    val colorScheme = rememberAppColorScheme(
        isDark = isDark,
        dynamicColor = dynamicColor,
        themeKey = themeKey,
        canvasTintPercent = canvasTintPercent,
    )
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
