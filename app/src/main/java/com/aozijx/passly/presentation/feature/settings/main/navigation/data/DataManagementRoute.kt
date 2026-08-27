package com.aozijx.passly.presentation.feature.settings.main.navigation.data

import com.aozijx.passly.presentation.feature.settings.main.navigation.SettingsRoute
import android.content.Context
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.presentation.feature.settings.main.SettingsViewModel
import com.aozijx.passly.presentation.feature.settings.main.SettingsUiAction
import com.aozijx.passly.presentation.feature.settings.main.SettingsUiState
import com.aozijx.passly.presentation.feature.settings.backup.DataManagementSettingsUiAction
import com.aozijx.passly.presentation.feature.settings.backup.DataManagementSettingsViewModel
import com.aozijx.passly.presentation.feature.settings.backup.DatabaseRecoveryViewModel
import com.aozijx.passly.presentation.feature.settings.backup.DatabaseRecoveryUiAction
import com.aozijx.passly.presentation.feature.settings.backup.toDetailState
import com.aozijx.passly.presentation.feature.settings.backup.toSheetState
import com.aozijx.passly.presentation.feature.settings.main.interaction.InteractionSettingsViewModel
import com.aozijx.passly.presentation.ui.settings.backup.DataManagementDetail
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
    recoveryViewModel: DatabaseRecoveryViewModel,
    settingsViewModel: SettingsViewModel,
    settingsState: SettingsUiState,
    onOpenTrash: () -> Unit,
    onBack: (() -> Unit)?
) {
    val state by dataViewModel.uiState.collectAsStateWithLifecycle()
    val recoveryState by recoveryViewModel.uiState.collectAsStateWithLifecycle()
    SettingsSecondaryPage(
        title = stringResource(SettingsGroup.DATA_MANAGEMENT.titleRes),
        onBack = onBack
    ) {
        item {
            DataManagementDetail(
                state = state.toDetailState(),
                recoveryState = recoveryState.toSheetState(),
                isClearingDatabase = settingsState.isClearingDatabase,
                onAutoDownloadIconsChange = {
                    dataViewModel.onAction(
                        DataManagementSettingsUiAction.SetAutoDownloadIcons(it)
                    )
                },
                onOpenTrash = onOpenTrash,
                onRefreshRecoveryPackages = {
                    recoveryViewModel.onAction(DatabaseRecoveryUiAction.RefreshRecoveryPackages)
                },
                onClearRecoveryResult = {
                    recoveryViewModel.onAction(DatabaseRecoveryUiAction.ClearRecoveryResult)
                },
                onScanRecoveryPackage = {
                    recoveryViewModel.onAction(DatabaseRecoveryUiAction.ScanRecoveryPackage(it))
                },
                onRestoreRecoveryPackage = {
                    recoveryViewModel.onAction(DatabaseRecoveryUiAction.RestoreRecoveryPackage(it))
                },
                onToggleRecoveryType = {
                    recoveryViewModel.onAction(
                        DatabaseRecoveryUiAction.ToggleRecoveryType(EntryType.valueOf(it))
                    )
                },
                onDeleteRecoveryPackage = {
                    recoveryViewModel.onAction(DatabaseRecoveryUiAction.DeleteRecoveryPackage(it))
                },
                onClearDatabase = {
                    settingsViewModel.onAction(SettingsUiAction.ClearDatabase)
                }
            )
        }
    }
}
