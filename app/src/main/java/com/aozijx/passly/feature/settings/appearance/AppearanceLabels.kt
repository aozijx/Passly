package com.aozijx.passly.feature.settings.appearance

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.R
import com.aozijx.passly.domain.settings.model.AppLanguage
import com.aozijx.passly.domain.settings.model.ThemeMode

@StringRes
fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.follow_system
    ThemeMode.LIGHT -> R.string.settings_theme_mode_light
    ThemeMode.DARK -> R.string.settings_theme_mode_dark
}

/**
 * 使用 Locale 自带的本地化名称，避免在 ViewModel 中维护语言名到字符串资源的映射。
 */
@Composable
fun AppLanguage.localizedDisplayName(): String {
    if (this == AppLanguage.SYSTEM) return stringResource(R.string.follow_system)

    val displayLocale = LocalConfiguration.current.locales[0]
    val displayName = locale?.getDisplayName(displayLocale).orEmpty()
    return displayName.replaceFirstChar { first ->
        if (first.isLowerCase()) first.titlecase(displayLocale) else first.toString()
    }
}
