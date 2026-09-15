package com.aozijx.passly.presentation.feature.settings.main.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import com.aozijx.passly.presentation.feature.settings.backup.DataManagementSettingsViewModel
import com.aozijx.passly.presentation.feature.settings.main.SettingsUiState
import com.aozijx.passly.presentation.feature.settings.main.SettingsViewModel
import com.aozijx.passly.presentation.feature.settings.main.interaction.InteractionSettingsViewModel
import com.aozijx.passly.presentation.ui.settings.main.SettingsDetailPlaceholder
import com.aozijx.passly.presentation.ui.settings.main.SettingsScreenLocalState

@Composable
internal fun SettingsDetailRouteRegistry(
    route: SettingsRoute?,
    context: Context,
    localState: SettingsScreenLocalState,
    settingsViewModel: SettingsViewModel,
    interactionViewModel: InteractionSettingsViewModel,
    dataViewModel: DataManagementSettingsViewModel,
    settingsState: SettingsUiState,
    onOpenTrash: () -> Unit,
    onBack: (() -> Unit)?,
) {
    when (route) {
        null,
        SettingsRoute.Main -> SettingsDetailPlaceholder()

        SettingsRoute.Security,
        SettingsRoute.Privacy,
        SettingsRoute.Appearance,
        SettingsRoute.Interface -> CoreSettingsRoute(route, settingsViewModel, onBack)

        SettingsRoute.Interaction,
        SettingsRoute.Autofill,
        SettingsRoute.DataManagement,
        SettingsRoute.BackupRestore,
        SettingsRoute.RecoveryCode,
        SettingsRoute.General,
        SettingsRoute.Notifications -> DataSettingsRoute(
            route = route,
            context = context,
            localState = localState,
            interactionViewModel = interactionViewModel,
            dataViewModel = dataViewModel,
            settingsViewModel = settingsViewModel,
            settingsState = settingsState,
            onOpenTrash = onOpenTrash,
            onBack = onBack,
        )
    }
}
