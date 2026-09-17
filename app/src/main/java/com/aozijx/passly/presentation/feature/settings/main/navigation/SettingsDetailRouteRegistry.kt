package com.aozijx.passly.presentation.feature.settings.main.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import com.aozijx.passly.presentation.feature.settings.backup.DataManagementSettingsViewModel
import com.aozijx.passly.presentation.feature.settings.main.SettingsViewModel
import com.aozijx.passly.presentation.ui.settings.main.SettingsDetailPlaceholder
import com.aozijx.passly.presentation.ui.settings.main.SettingsOverlayState

@Composable
internal fun SettingsDetailRouteRegistry(
    route: SettingsDestination?,
    context: Context,
    localState: SettingsOverlayState,
    settingsViewModel: SettingsViewModel,
    dataViewModel: DataManagementSettingsViewModel,
    onOpenTrash: () -> Unit,
    onBack: (() -> Unit)?,
) {
    when (route) {
        null,
        SettingsDestination.Main -> SettingsDetailPlaceholder()

        SettingsDestination.Security,
        SettingsDestination.Privacy,
        SettingsDestination.Appearance,
        SettingsDestination.Interface -> CoreSettingsRoute(route, settingsViewModel, onBack)

        SettingsDestination.Interaction,
        SettingsDestination.Autofill,
        SettingsDestination.DataManagement,
        SettingsDestination.BackupRestore,
        SettingsDestination.RecoveryCode,
        SettingsDestination.General,
        SettingsDestination.Notifications -> DataSettingsRoute(
            route = route,
            context = context,
            localState = localState,
            dataViewModel = dataViewModel,
            onOpenTrash = onOpenTrash,
            onBack = onBack,
        )
    }
}
