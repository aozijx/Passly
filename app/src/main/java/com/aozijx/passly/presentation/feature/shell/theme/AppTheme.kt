package com.aozijx.passly.presentation.feature.shell.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp
import com.aozijx.passly.core.ui.theme.SystemTypography
import com.aozijx.passly.core.ui.theme.rememberAppColorScheme
import com.aozijx.passly.core.ui.theme.themeTypography
import com.aozijx.passly.domain.settings.model.AppCornerRadiusConstraints
import com.aozijx.passly.domain.settings.model.AppearanceSettings
import com.aozijx.passly.domain.settings.model.FontFamilyMode
import com.aozijx.passly.domain.settings.model.ThemeMode

val LocalAppearanceSettings = staticCompositionLocalOf { AppearanceSettings() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTheme(
    appearance: AppearanceSettings = AppearanceSettings(),
    appCornerRadiusDp: Float = AppCornerRadiusConstraints.DEFAULT_DP,
    content: @Composable () -> Unit
) {
    val motionScheme = remember { MotionScheme.expressive() }
    val isDark = when (appearance.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val targetColorScheme = rememberAppColorScheme(
        isDark = isDark,
        dynamicColor = appearance.isDynamicColor,
        themeKey = appearance.themeKey,
        canvasTintPercent = appearance.canvasTintPercent,
    )
    val colorScheme = rememberAnimatedColorScheme(
        target = targetColorScheme,
        animationSpec = motionScheme.defaultEffectsSpec(),
    )
    val typography =
        if (appearance.fontFamily == FontFamilyMode.SYSTEM) SystemTypography else themeTypography()
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = motionScheme,
        shapes = appShapes(appCornerRadiusDp),
        typography = typography,
    ) {
        CompositionLocalProvider(LocalAppearanceSettings provides appearance, content = content)
    }
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
