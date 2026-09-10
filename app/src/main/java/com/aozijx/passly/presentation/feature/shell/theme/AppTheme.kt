package com.aozijx.passly.presentation.feature.shell.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.aozijx.passly.core.ui.theme.SystemTypography
import com.aozijx.passly.core.ui.theme.rememberAppColorScheme
import com.aozijx.passly.core.ui.theme.themeTypography
import com.aozijx.passly.domain.settings.model.FontFamilyMode
import com.aozijx.passly.domain.settings.model.AppCornerRadiusConstraints
import com.aozijx.passly.domain.settings.model.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    themeKey: String = "default",
    canvasTintPercent: Int = 8,
    appCornerRadiusDp: Float = AppCornerRadiusConstraints.DEFAULT_DP,
    fontFamily: FontFamilyMode = FontFamilyMode.APP_BUNDLED,
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
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        shapes = appShapes(appCornerRadiusDp),
        typography = typography,
        content = content,
    )
}

internal fun appShapes(cornerRadiusDp: Float): Shapes {
    val radius = AppCornerRadiusConstraints.normalize(cornerRadiusDp)
    return Shapes(
        extraSmall = RoundedCornerShape((radius * 0.25f).dp),
        small = RoundedCornerShape((radius * 0.4f).dp),
        medium = RoundedCornerShape((radius * 0.6f).dp),
        large = RoundedCornerShape((radius * 0.8f).dp),
        extraLarge = RoundedCornerShape(radius.dp),
        largeIncreased = RoundedCornerShape(
            (radius * 1.25f).coerceAtMost(AppCornerRadiusConstraints.MAX_DP).dp,
        ),
    )
}
