package com.aozijx.passly.presentation.feature.settings.main.navigation.data

import com.aozijx.passly.presentation.feature.settings.main.navigation.SettingsRoute
import android.content.Context
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.presentation.feature.settings.backup.DataManagementSettingsUiAction
import com.aozijx.passly.presentation.feature.settings.backup.DataManagementSettingsViewModel
import com.aozijx.passly.presentation.feature.settings.backup.toDetailState
import com.aozijx.passly.presentation.feature.settings.main.interaction.InteractionSettingsViewModel
import com.aozijx.passly.presentation.ui.settings.backup.DataManagementDetail
import com.aozijx.passly.presentation.ui.settings.backup.model.DataManagementEventHandler
import com.aozijx.passly.presentation.ui.settings.main.component.SettingsGroup
import com.aozijx.passly.presentation.ui.settings.main.SettingsScreenLocalState
import com.aozijx.passly.presentation.ui.settings.main.SettingsSecondaryPage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DataManagementRouteContent(
    route: SettingsRoute,
    context: Context,
    localState: SettingsScreenLocalState,
    interactionViewModel: InteractionSettingsViewModel,
    dataViewModel: DataManagementSettingsViewModel,
    onOpenTrash: () -> Unit,
    onOpenDatabaseRecovery: () -> Unit,
    onBack: (() -> Unit)?
) {
    val state by dataViewModel.uiState.collectAsStateWithLifecycle()
    SettingsSecondaryPage(
        title = stringResource(SettingsGroup.DATA_MANAGEMENT.titleRes),
        onBack = onBack
    ) {
        item {
            DataManagementDetail(
                state = state.toDetailState(),
                eventHandler = object : DataManagementEventHandler {
                    override fun onAutoDownloadIconsChanged(enabled: Boolean) {
                        dataViewModel.onAction(
                            DataManagementSettingsUiAction.SetAutoDownloadIcons(enabled),
                        )
                    }

                    override fun onOpenTrash() = onOpenTrash.invoke()
                    override fun onOpenDatabaseRecovery() = onOpenDatabaseRecovery.invoke()
                },
            )
        }
    }
}
