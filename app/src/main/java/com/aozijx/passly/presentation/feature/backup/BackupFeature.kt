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
import com.aozijx.passly.feature.backup.internal.model.BackupExportFormat
import com.aozijx.passly.presentation.feature.backup.BackupUiAction
import com.aozijx.passly.presentation.feature.backup.BackupViewModel
import com.aozijx.passly.presentation.ui.settings.backup.BackupRestoreDetail
import com.aozijx.passly.presentation.ui.settings.backup.BackupRestoreSheet
import com.aozijx.passly.presentation.ui.settings.backup.model.BackupExportFormatUiModel
import com.aozijx.passly.presentation.ui.settings.backup.model.BackupImportModeUiModel
import com.aozijx.passly.presentation.ui.settings.backup.model.BackupRestoreSheetEventHandler
import com.aozijx.passly.presentation.ui.settings.backup.model.BackupSheet
import com.aozijx.passly.presentation.ui.shared.entry.EntryTypeUiModel
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

    BackupRestoreSheet(
        state = state.toSheetUiState(
            activeSheet = activeSheet,
            configuredDirectoryLabel = directoryLabel.takeIf { !directoryUri.isNullOrBlank() },
        ),
        eventHandler = object : BackupRestoreSheetEventHandler {
            override fun onDismiss() {
                activeSheet = null
                viewModel.onAction(BackupUiAction.CancelPendingOperation)
            }

            override fun onFormatSelected(format: BackupExportFormatUiModel) {
                viewModel.onAction(BackupUiAction.PrepareExport(format.toFeatureModel()))
                activeSheet = BackupSheet.EXPORT_OPTIONS
            }

            override fun onPasswordChanged(password: String) {
                viewModel.onAction(BackupUiAction.UpdatePassword(OwnedChars.fromString(password)))
            }

            override fun onIncludeIconsChanged(include: Boolean) {
                viewModel.onAction(BackupUiAction.UpdateIncludeIcons(include))
            }

            override fun onIncludeAttachmentsChanged(include: Boolean) {
                viewModel.onAction(BackupUiAction.UpdateIncludeAttachments(include))
            }

            override fun onIncludeDeletedChanged(include: Boolean) {
                viewModel.onAction(BackupUiAction.UpdateIncludeDeleted(include))
            }

            override fun onIncludedEntryTypesChanged(types: Set<EntryTypeUiModel>) {
                viewModel.onAction(BackupUiAction.UpdateIncludedEntryTypes(types.toFeatureModels()))
            }

            override fun onImportModeChanged(mode: BackupImportModeUiModel) {
                viewModel.onAction(BackupUiAction.UpdateImportMode(mode.toFeatureModel()))
            }

            override fun onExportRequested() {
                activeSheet = null
                if (!directoryUri.isNullOrBlank()) {
                    viewModel.onAction(BackupUiAction.StartExportInConfiguredDirectory)
                } else {
                    val fileName = state.pendingExportFileName ?: return
                    when (state.selectedExportFormat) {
                        BackupExportFormat.ENCRYPTED -> encryptedExportPicker.launch(fileName)
                        BackupExportFormat.JSON -> jsonExportPicker.launch(fileName)
                        BackupExportFormat.TEXT -> textExportPicker.launch(fileName)
                    }
                }
            }

            override fun onImportRequested() {
                activeSheet = null
                viewModel.onAction(BackupUiAction.ProcessBackupAction)
            }
        },
    )
}
