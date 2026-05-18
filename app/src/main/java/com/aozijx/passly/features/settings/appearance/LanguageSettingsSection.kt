package com.aozijx.passly.features.settings.appearance

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.runtime.Composable
import com.aozijx.passly.features.settings.shell.ClickableSettingItem
import com.aozijx.passly.features.settings.shell.SettingsCard
import com.aozijx.passly.features.settings.shell.SettingsGroupTitle

@Composable
fun LanguageSettingsSection(
    currentLanguage: String,
    onLanguageClick: () -> Unit
) {
    SettingsGroupTitle(text = "语言")
    SettingsCard {
        ClickableSettingItem(
            icon = Icons.Default.Language,
            title = "应用语言",
            value = currentLanguage,
            onClick = onLanguageClick
        )
    }
}