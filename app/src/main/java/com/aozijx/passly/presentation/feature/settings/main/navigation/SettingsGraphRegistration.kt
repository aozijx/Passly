package com.aozijx.passly.presentation.feature.settings.main.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.aozijx.passly.presentation.feature.settings.main.SettingsViewModel
import com.aozijx.passly.presentation.feature.shell.navigation.AppRoute
import com.aozijx.passly.presentation.feature.shell.navigation.ShellNavigationContext

internal fun NavGraphBuilder.registerSettingsGraph(context: ShellNavigationContext) {
    composable(AppRoute.Settings.route) {
        val settingsViewModel: SettingsViewModel = hiltViewModel()
        SettingsNavGraph(
            onOuterBack = context.navigateBack,
            settingsViewModel = settingsViewModel,
        )
    }
}
