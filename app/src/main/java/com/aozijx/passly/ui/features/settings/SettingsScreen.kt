package com.aozijx.passly.ui.features.settings

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.aozijx.passly.ui.features.settings.navigation.SettingsNavGraph

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    settingsViewModel: SettingsViewModel,
    onUpdateInteraction: () -> Unit = {}
) {
    val navController = rememberNavController()

    SettingsNavGraph(
        navController = navController,
        settingsViewModel = settingsViewModel,
        onUpdateInteraction = onUpdateInteraction,
        onOuterBack = onBack
    )
}