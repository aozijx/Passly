package com.aozijx.passly.presentation.feature.settings.appearance

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.R
import com.aozijx.passly.domain.settings.model.AppLanguage
import com.aozijx.passly.domain.settings.model.ThemeMode
import com.aozijx.passly.presentation.ui.settings.appearance.model.LanguageOptionUiModel

@StringRes
fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.settings_follow_system
    ThemeMode.LIGHT -> R.string.settings_theme_mode_light
    ThemeMode.DARK -> R.string.settings_theme_mode_dark
}

/**
 * 语言选择器使用语言自称（中文 / English / 日本語），不会随当前界面语言二次翻译。
 */
@Composable
fun AppLanguage.localizedDisplayName(): String {
    if (this == AppLanguage.SYSTEM) return stringResource(R.string.settings_follow_system)

    val languageLocale = locale ?: return name
    val displayName = languageLocale.getDisplayName(languageLocale)
    return displayName.replaceFirstChar { first ->
        if (first.isLowerCase()) first.titlecase(languageLocale) else first.toString()
    }
}

@Composable
fun languagePickerOptions(): List<LanguageOptionUiModel> = AppLanguage.entries.map { language ->
    LanguageOptionUiModel(language.name, language.localizedDisplayName())
}

fun appLanguageFromKey(key: String): AppLanguage? = AppLanguage.entries.firstOrNull { it.name == key }
