package com.aozijx.passly.presentation.feature.settings.main.navigation

import androidx.compose.runtime.Composable
import com.aozijx.passly.presentation.feature.settings.main.SettingsViewModel
import com.aozijx.passly.presentation.feature.settings.main.navigation.autofill.AutofillRoute
import com.aozijx.passly.presentation.feature.settings.main.navigation.core.AppearanceRoute
import com.aozijx.passly.presentation.feature.settings.main.navigation.core.InterfaceRoute
import com.aozijx.passly.presentation.feature.settings.main.navigation.core.PrivacyRoute
import com.aozijx.passly.presentation.feature.settings.main.navigation.core.RecoveryCodeRoute
import com.aozijx.passly.presentation.feature.settings.main.navigation.core.SecurityRoute
import com.aozijx.passly.presentation.feature.settings.main.navigation.data.BackupRoute
import com.aozijx.passly.presentation.feature.settings.main.navigation.data.DataManagementRoute
import com.aozijx.passly.presentation.feature.settings.main.navigation.general.GeneralRoute
import com.aozijx.passly.presentation.feature.settings.main.navigation.general.NotificationsRoute
import com.aozijx.passly.presentation.feature.settings.main.navigation.interaction.InteractionRoute
import com.aozijx.passly.presentation.ui.settings.main.SettingsDetailPlaceholder

@Composable
internal fun SettingsDetailRouteRegistry(
    route: SettingsDestination?,
    settingsViewModel: SettingsViewModel,
    onOpenTrash: () -> Unit,
    onBack: (() -> Unit)?,
) {
    when (route) {
        null,
        SettingsDestination.Main -> SettingsDetailPlaceholder()
        SettingsDestination.Security -> SecurityRoute(settingsViewModel, onBack)
        SettingsDestination.Privacy -> PrivacyRoute(onBack)
        SettingsDestination.Appearance -> AppearanceRoute(onBack)
        SettingsDestination.Interface -> InterfaceRoute(onBack)
        SettingsDestination.Interaction -> InteractionRoute(onBack)
        SettingsDestination.Autofill -> AutofillRoute(onBack)
        SettingsDestination.DataManagement -> DataManagementRoute(onOpenTrash, onBack)
        SettingsDestination.BackupRestore -> BackupRoute(onBack)
        SettingsDestination.RecoveryCode -> RecoveryCodeRoute(onBack)
        SettingsDestination.General -> GeneralRoute(onBack)
        SettingsDestination.Notifications -> NotificationsRoute(onBack)
    }
}
