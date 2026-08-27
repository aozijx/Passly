package com.aozijx.passly.presentation.feature.settings.main.navigation.general

import com.aozijx.passly.presentation.feature.settings.main.navigation.SettingsRoute
import android.content.Context
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aozijx.passly.presentation.feature.settings.main.SettingsViewModel
import com.aozijx.passly.presentation.feature.settings.main.SettingsUiState
import com.aozijx.passly.presentation.feature.settings.backup.DataManagementSettingsViewModel
import com.aozijx.passly.presentation.feature.settings.main.interaction.InteractionSettingsViewModel
import com.aozijx.passly.presentation.feature.settings.main.general.NotificationDetail
import com.aozijx.passly.presentation.ui.settings.main.component.SettingsGroup
import com.aozijx.passly.presentation.ui.settings.main.SettingsScreenLocalState
import com.aozijx.passly.presentation.ui.settings.main.SettingsSecondaryPage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NotificationsRouteContent(
    route: SettingsRoute,
    context: Context,
    localState: SettingsScreenLocalState,
    interactionViewModel: InteractionSettingsViewModel,
    dataViewModel: DataManagementSettingsViewModel,
    settingsViewModel: SettingsViewModel,
    settingsState: SettingsUiState,
    onBack: (() -> Unit)?
) {
    SettingsSecondaryPage(
        title = stringResource(SettingsGroup.NOTIFICATIONS.titleRes),
        onBack = onBack
    ) {
        item { NotificationDetail() }
    }
}
