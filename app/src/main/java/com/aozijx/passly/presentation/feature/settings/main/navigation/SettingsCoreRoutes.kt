package com.aozijx.passly.presentation.feature.settings.main.navigation

import androidx.compose.runtime.Composable
import com.aozijx.passly.presentation.feature.settings.main.SettingsViewModel
import com.aozijx.passly.presentation.feature.settings.main.navigation.core.AppearanceRouteContent
import com.aozijx.passly.presentation.feature.settings.main.navigation.core.InterfaceRouteContent
import com.aozijx.passly.presentation.feature.settings.main.navigation.core.PrivacyRouteContent
import com.aozijx.passly.presentation.feature.settings.main.navigation.core.SecurityRouteContent

@Composable
internal fun CoreSettingsRouteContent(
    route: SettingsRoute,
    settingsViewModel: SettingsViewModel,
    onBack: (() -> Unit)?,
) {
    when (route) {
        SettingsRoute.Security -> SecurityRouteContent(settingsViewModel, onBack)
        SettingsRoute.Privacy -> PrivacyRouteContent(settingsViewModel, onBack)
        SettingsRoute.Appearance -> AppearanceRouteContent(settingsViewModel, onBack)
        SettingsRoute.Interface -> InterfaceRouteContent(settingsViewModel, onBack)
        else -> error("Unsupported core settings route: ${route.route}")
    }
}
