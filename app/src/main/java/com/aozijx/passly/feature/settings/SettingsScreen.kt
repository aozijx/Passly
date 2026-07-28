package com.aozijx.passly.feature.settings

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.aozijx.passly.feature.settings.navigation.SettingsNavGraph

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    settingsViewModel: SettingsViewModel
) {
    val navController = rememberNavController()

    SettingsNavGraph(
        navController = navController,
        settingsViewModel = settingsViewModel,
        onOuterBack = onBack
    )
}
