package com.aozijx.passly.presentation.feature.settings.main.navigation.data

import com.aozijx.passly.presentation.feature.settings.main.navigation.SettingsRoute
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.core.platform.path.UriDisplayNameFormatter
import com.aozijx.passly.presentation.feature.backup.BackupSettingsFeature
import com.aozijx.passly.feature.backup.internal.archive.platform.BackupStorageSupport
import com.aozijx.passly.presentation.feature.settings.main.SettingsViewModel
import com.aozijx.passly.presentation.feature.settings.main.SettingsUiState
import com.aozijx.passly.presentation.feature.settings.backup.DataManagementSettingsUiAction
import com.aozijx.passly.presentation.feature.settings.backup.DataManagementSettingsViewModel
import com.aozijx.passly.presentation.feature.settings.backup.handleBackupPathPicked
import com.aozijx.passly.presentation.feature.settings.main.interaction.InteractionSettingsViewModel
import com.aozijx.passly.presentation.ui.settings.main.component.SettingsGroup
import com.aozijx.passly.presentation.ui.settings.main.SettingsScreenLocalState
import com.aozijx.passly.presentation.ui.settings.main.SettingsSecondaryPage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BackupRouteContent(
    route: SettingsRoute,
    context: Context,
    localState: SettingsScreenLocalState,
    interactionViewModel: InteractionSettingsViewModel,
    dataViewModel: DataManagementSettingsViewModel,
    settingsViewModel: SettingsViewModel,
    settingsState: SettingsUiState,
    onBack: (() -> Unit)?
) {
    val state by dataViewModel.uiState.collectAsStateWithLifecycle()
    val notSetText = stringResource(R.string.not_set)
    val pathLabel = remember(state.directoryUri) {
        UriDisplayNameFormatter.format(state.directoryUri) ?: notSetText
    }
    val backupPathPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        handleBackupPathPicked(context, uri) { resolvedUri ->
            dataViewModel.onAction(
                DataManagementSettingsUiAction.SetBackupDirectoryUri(resolvedUri)
            )
        }
    }

    SettingsSecondaryPage(
        title = stringResource(SettingsGroup.BACKUP_RESTORE.titleRes),
        onBack = onBack
    ) {
        item {
            BackupSettingsFeature(
                directoryUri = state.directoryUri,
                directoryLabel = pathLabel,
                lastExportFileLabel = notSetText,
                onPickBackupPath = {
                    backupPathPicker.launch(
                        BackupStorageSupport.defaultDocumentsTreeUri()
                    )
                },
                onClearBackupPath = if (state.directoryUri.isNullOrBlank()) null
                else localState::openClearBackupDirConfirmDialog
            )
        }
    }
}
