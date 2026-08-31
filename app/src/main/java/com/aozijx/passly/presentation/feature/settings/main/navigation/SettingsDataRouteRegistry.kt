package com.aozijx.passly.presentation.feature.settings.main.navigation

import android.content.Context
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import com.aozijx.passly.presentation.feature.settings.backup.DataManagementSettingsViewModel
import com.aozijx.passly.presentation.feature.settings.main.SettingsUiState
import com.aozijx.passly.presentation.feature.settings.main.SettingsViewModel
import com.aozijx.passly.presentation.feature.settings.main.interaction.InteractionSettingsViewModel
import com.aozijx.passly.presentation.feature.settings.main.navigation.autofill.AutofillRouteContent
import com.aozijx.passly.presentation.feature.settings.main.navigation.core.RecoveryCodeRouteContent
import com.aozijx.passly.presentation.feature.settings.main.navigation.data.BackupRouteContent
import com.aozijx.passly.presentation.feature.settings.main.navigation.data.DataManagementRouteContent
import com.aozijx.passly.presentation.feature.settings.main.navigation.general.GeneralRouteContent
import com.aozijx.passly.presentation.feature.settings.main.navigation.general.NotificationsRouteContent
import com.aozijx.passly.presentation.feature.settings.main.navigation.interaction.InteractionRouteContent
import com.aozijx.passly.presentation.ui.settings.main.SettingsScreenLocalState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DataSettingsRouteContent(
    route: SettingsRoute,
    context: Context,
    localState: SettingsScreenLocalState,
    interactionViewModel: InteractionSettingsViewModel,
    dataViewModel: DataManagementSettingsViewModel,
    settingsViewModel: SettingsViewModel,
    settingsState: SettingsUiState,
    onOpenTrash: () -> Unit,
    onOpenDatabaseRecovery: () -> Unit,
    onBack: (() -> Unit)?,
) {
    val content: @Composable () -> Unit = when (route) {
        SettingsRoute.Interaction -> ({ InteractionRouteContent(route, context, localState, interactionViewModel, dataViewModel, settingsViewModel, settingsState, onBack) })
        SettingsRoute.Autofill -> ({ AutofillRouteContent(route, context, localState, interactionViewModel, dataViewModel, settingsViewModel, settingsState, onBack) })
        SettingsRoute.DataManagement -> ({ DataManagementRouteContent(route, context, localState, interactionViewModel, onOpenTrash, onOpenDatabaseRecovery, onBack) })
        SettingsRoute.BackupRestore -> ({ BackupRouteContent(route, context, localState, interactionViewModel, dataViewModel, settingsViewModel, settingsState, onBack) })
        SettingsRoute.RecoveryCode -> ({ RecoveryCodeRouteContent(route, context, localState, interactionViewModel, dataViewModel, settingsViewModel, settingsState, onBack) })
        SettingsRoute.General -> ({ GeneralRouteContent(route, context, localState, interactionViewModel, dataViewModel, settingsViewModel, settingsState, onBack) })
        SettingsRoute.Notifications -> ({ NotificationsRouteContent(route, context, localState, interactionViewModel, dataViewModel, settingsViewModel, settingsState, onBack) })
        else -> error("Unsupported data settings route: ${route.route}")
    }
    content()
}
