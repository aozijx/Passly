package com.aozijx.passly.presentation.feature.settings.main.navigation

import androidx.compose.runtime.Composable
import com.aozijx.passly.presentation.feature.settings.main.SettingsViewModel
import com.aozijx.passly.presentation.feature.settings.main.navigation.core.AppearanceRoute
import com.aozijx.passly.presentation.feature.settings.main.navigation.core.InterfaceRoute
import com.aozijx.passly.presentation.feature.settings.main.navigation.core.PrivacyRoute
import com.aozijx.passly.presentation.feature.settings.main.navigation.core.SecurityRoute

@Composable
internal fun CoreSettingsRoute(
    route: SettingsDestination,
    settingsViewModel: SettingsViewModel,
    onBack: (() -> Unit)?,
) {
    when (route) {
        SettingsDestination.Security -> SecurityRoute(settingsViewModel, onBack)
        SettingsDestination.Privacy -> PrivacyRoute(onBack)
        SettingsDestination.Appearance -> AppearanceRoute(onBack)
        SettingsDestination.Interface -> InterfaceRoute(onBack)
        else -> error("Unsupported core settings route: ${route.route}")
    }
}
