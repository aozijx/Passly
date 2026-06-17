package com.aozijx.passly.features.settings.appearance

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.runtime.Composable
import com.aozijx.passly.features.settings.components.navigationSettingsItem
import com.aozijx.passly.features.settings.shell.SettingsGroupTitle
import com.aozijx.passly.features.settings.shell.SettingsRoundedGroup

@Composable
fun LanguageSettingsSection(
    currentLanguage: String,
    onLanguageClick: () -> Unit
) {
    SettingsGroupTitle(text = "语言")
    SettingsRoundedGroup {
        navigationSettingsItem(
            icon = Icons.Default.Language,
            title = "应用语言",
            value = currentLanguage,
            onClick = onLanguageClick
        )
    }
}