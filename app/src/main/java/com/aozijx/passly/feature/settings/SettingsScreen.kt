package com.aozijx.passly.feature.settings

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.aozijx.passly.feature.settings.navigation.SettingsNavGraph

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    settingsViewModel: SettingsViewModel,
    onUpdateInteraction: () -> Unit = {},
    onAuthRequired: (title: String, subtitle: String, onSuccess: () -> Unit) -> Unit = { _, _, cb -> cb() }
) {
    val navController = rememberNavController()

    SettingsNavGraph(
        navController = navController,
        settingsViewModel = settingsViewModel,
        onUpdateInteraction = onUpdateInteraction,
        onOuterBack = onBack,
        onAuthRequired = onAuthRequired
    )
}