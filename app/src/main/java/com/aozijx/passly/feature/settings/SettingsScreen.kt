package com.aozijx.passly.feature.settings

import androidx.compose.runtime.Composable
import com.aozijx.passly.feature.settings.navigation.SettingsNavGraph

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    settingsViewModel: SettingsViewModel
) {
    SettingsNavGraph(
        settingsViewModel = settingsViewModel,
        onOuterBack = onBack
    )
}
