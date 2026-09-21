package com.aozijx.passly.presentation.feature.settings.main.navigation.data

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.core.platform.path.UriDisplayNameFormatter
import com.aozijx.passly.feature.backup.internal.archive.platform.BackupStorageSupport
import com.aozijx.passly.presentation.feature.backup.BackupOperationRoute
import com.aozijx.passly.presentation.feature.settings.backup.DataManagementSettingsUiAction
import com.aozijx.passly.presentation.feature.settings.backup.DataManagementSettingsViewModel
import com.aozijx.passly.presentation.feature.settings.backup.handleBackupPathPicked
import com.aozijx.passly.presentation.feature.settings.ui.data.BackupDirectoryClearDialog
import com.aozijx.passly.presentation.feature.settings.ui.main.SettingsSecondaryPage
import com.aozijx.passly.presentation.feature.settings.ui.main.component.SettingsGroup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BackupRoute(
    onBack: (() -> Unit)?,
) {
    val context = LocalContext.current
    val viewModel: DataManagementSettingsViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showClearDirectoryDialog by rememberSaveable { mutableStateOf(false) }
    val notSetText = stringResource(R.string.not_set)
    val pathLabel = remember(state.directoryUri) {
        UriDisplayNameFormatter.format(state.directoryUri) ?: notSetText
    }
    val backupPathPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        handleBackupPathPicked(context, uri) { resolvedUri ->
            viewModel.onAction(
                DataManagementSettingsUiAction.SetBackupDirectoryUri(resolvedUri)
            )
        }
    }

    SettingsSecondaryPage(
        title = stringResource(SettingsGroup.BACKUP_RESTORE.titleRes),
        onBack = onBack
    ) {
        item {
            BackupOperationRoute(
                directoryUri = state.directoryUri,
                directoryLabel = pathLabel,
                lastExportFileLabel = notSetText,
                onPickBackupPath = {
                    backupPathPicker.launch(
                        BackupStorageSupport.defaultDocumentsTreeUri()
                    )
                },
                onClearBackupPath = if (state.directoryUri.isNullOrBlank()) null
                else ({ showClearDirectoryDialog = true }),
            )
        }
    }

    if (showClearDirectoryDialog) {
        BackupDirectoryClearDialog(
            onConfirm = {
                state.directoryUri?.takeIf(String::isNotBlank)?.let { directoryUri ->
                    runCatching {
                        context.contentResolver.releasePersistableUriPermission(
                            directoryUri.toUri(),
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                        )
                    }
                }
                viewModel.onAction(DataManagementSettingsUiAction.ClearBackupDirectory)
                showClearDirectoryDialog = false
            },
            onDismiss = { showClearDirectoryDialog = false },
        )
    }
}
