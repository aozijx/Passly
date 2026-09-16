package com.aozijx.passly.presentation.feature.settings.main.navigation

import androidx.compose.runtime.Composable
import com.aozijx.passly.presentation.feature.settings.main.SettingsViewModel
import com.aozijx.passly.presentation.feature.settings.main.navigation.core.AppearanceRoute
import com.aozijx.passly.presentation.feature.settings.main.navigation.core.InterfaceRoute
import com.aozijx.passly.presentation.feature.settings.main.navigation.core.PrivacyRoute
import com.aozijx.passly.presentation.feature.settings.main.navigation.core.SecurityRoute

@Composable
internal fun CoreSettingsRoute(
    route: SettingsRoute,
    settingsViewModel: SettingsViewModel,
    onBack: (() -> Unit)?,
) {
    when (route) {
        SettingsRoute.Security -> SecurityRoute(settingsViewModel, onBack)
        SettingsRoute.Privacy -> PrivacyRoute(onBack)
        SettingsRoute.Appearance -> AppearanceRoute(onBack)
        SettingsRoute.Interface -> InterfaceRoute(onBack)
        else -> error("Unsupported core settings route: ${route.route}")
    }
}
