package com.aozijx.passly.presentation.feature.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.feature.backup.internal.model.BackupExportUiFormat
import com.aozijx.passly.presentation.feature.backup.BackupUiAction
import com.aozijx.passly.presentation.feature.backup.BackupViewModel
import com.aozijx.passly.presentation.ui.settings.backup.BackupRestoreDetail
import com.aozijx.passly.presentation.feature.settings.backup.component.BackupRestoreSheetHost
import com.aozijx.passly.presentation.feature.settings.backup.component.BackupSheet
import com.aozijx.passly.domain.sensitive.OwnedChars

/**
 * Public settings entry point for the Backup feature.
 *
 * Consumers provide only settings-owned directory callbacks. Backup presentation state,
 * actions, platform document launchers, and sheets remain private to this feature.
 */
@Composable
fun BackupSettingsFeature(
    directoryUri: String?,
    directoryLabel: String,
    lastExportFileLabel: String,
    onPickBackupPath: () -> Unit,
    onClearBackupPath: (() -> Unit)?,
) {
    val viewModel: BackupViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var activeSheet by remember { mutableStateOf<BackupSheet?>(null) }

    fun startManualExport(uri: Uri?) {
        if (uri == null) {
            viewModel.onAction(BackupUiAction.CancelPendingOperation)
            return
        }
        viewModel.onAction(
            BackupUiAction.StartExport(
                uri = uri,
            )
        )
        viewModel.onAction(BackupUiAction.ProcessBackupAction)
    }

    val encryptedExportPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream"),
        ::startManualExport,
    )
    val jsonExportPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
        ::startManualExport,
    )
    val textExportPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain"),
        ::startManualExport,
    )
    val importPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            viewModel.onAction(BackupUiAction.StartImport(uri))
            activeSheet = BackupSheet.IMPORT_OPTIONS
        }
    }

    DisposableEffect(viewModel) {
        onDispose {
            viewModel.onAction(BackupUiAction.CancelPendingOperation)
        }
    }

    BackupRestoreDetail(
        backupPathLabel = directoryLabel,
        lastExportFileLabel = lastExportFileLabel,
        onExport = { activeSheet = BackupSheet.FORMAT_PICKER },
        onImport = {
            importPicker.launch(
                arrayOf(
                    "application/octet-stream",
                    "application/json",
                    "text/json",
                    "*/*",
                )
            )
        },
        onPickBackupPath = onPickBackupPath,
        onTestBackupWrite = {
            viewModel.onAction(BackupUiAction.CheckDirectoryPermission(directoryUri))
        },
        onClearBackupPath = onClearBackupPath,
    )

    BackupRestoreSheetHost(
        sheet = activeSheet,
        state = state,
        configuredDirectoryLabel = directoryLabel.takeIf { !directoryUri.isNullOrBlank() },
        onDismiss = {
            activeSheet = null
            viewModel.onAction(BackupUiAction.CancelPendingOperation)
        },
        onFormatSelected = { format ->
            viewModel.onAction(BackupUiAction.PrepareExport(format))
            activeSheet = BackupSheet.EXPORT_OPTIONS
        },
        onPasswordChange = {
            viewModel.onAction(BackupUiAction.UpdatePassword(OwnedChars.fromString(it)))
        },
        onIncludeIconsChange = { viewModel.onAction(BackupUiAction.UpdateIncludeIcons(it)) },
        onIncludeAttachmentsChange = {
            viewModel.onAction(BackupUiAction.UpdateIncludeAttachments(it))
        },
        onIncludeDeletedChange = { viewModel.onAction(BackupUiAction.UpdateIncludeDeleted(it)) },
        onIncludedEntryTypesChange = {
            viewModel.onAction(BackupUiAction.UpdateIncludedEntryTypes(it))
        },
        onImportModeChange = { viewModel.onAction(BackupUiAction.UpdateImportMode(it)) },
        onExport = onExport@{
            activeSheet = null
            if (!directoryUri.isNullOrBlank()) {
                viewModel.onAction(BackupUiAction.StartExportInConfiguredDirectory)
            } else {
                val fileName = state.pendingExportFileName ?: return@onExport
                when (state.selectedExportFormat) {
                    BackupExportUiFormat.ENCRYPTED -> encryptedExportPicker.launch(fileName)
                    BackupExportUiFormat.JSON -> jsonExportPicker.launch(fileName)
                    BackupExportUiFormat.TEXT -> textExportPicker.launch(fileName)
                }
            }
        },
        onImport = {
            activeSheet = null
            viewModel.onAction(BackupUiAction.ProcessBackupAction)
        },
    )
}
