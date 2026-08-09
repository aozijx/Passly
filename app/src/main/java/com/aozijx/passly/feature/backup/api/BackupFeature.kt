package com.aozijx.passly.feature.backup.api

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.domain.backup.model.BackupExportUiFormat
import com.aozijx.passly.feature.backup.internal.contract.BackupIntent
import com.aozijx.passly.feature.backup.internal.contract.BackupUiState
import com.aozijx.passly.feature.backup.internal.presentation.BackupViewModel
import com.aozijx.passly.feature.backup.internal.ui.BackupRestoreDetail
import com.aozijx.passly.feature.backup.internal.ui.BackupRestoreSheetHost
import com.aozijx.passly.feature.backup.internal.ui.BackupSheet

/**
 * Public settings entry point for the Backup feature.
 *
 * Consumers provide only settings-owned directory callbacks. Backup presentation state,
 * intents, platform document launchers, and sheets remain private to this feature.
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
            viewModel.onIntent(BackupIntent.CancelPendingOperation)
            return
        }
        viewModel.onIntent(
            BackupIntent.StartExport(
                uri = uri,
                fileNameHint = viewModel.buildExportFileName(),
            )
        )
        viewModel.onIntent(BackupIntent.ProcessBackupAction)
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
            viewModel.onIntent(BackupIntent.StartImport(uri))
            activeSheet = BackupSheet.IMPORT_OPTIONS
        }
    }

    DisposableEffect(viewModel) {
        onDispose {
            viewModel.onIntent(BackupIntent.CancelPendingOperation)
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
            viewModel.onIntent(BackupIntent.CheckDirectoryPermission(directoryUri))
        },
        onClearBackupPath = onClearBackupPath,
    )

    BackupRestoreSheetHost(
        sheet = activeSheet,
        state = state,
        configuredDirectoryLabel = directoryLabel.takeIf { !directoryUri.isNullOrBlank() },
        onDismiss = {
            activeSheet = null
            viewModel.onIntent(BackupIntent.CancelPendingOperation)
        },
        onFormatSelected = { format ->
            viewModel.onIntent(BackupIntent.PrepareExport(format))
            activeSheet = BackupSheet.EXPORT_OPTIONS
        },
        onPasswordChange = { viewModel.onIntent(BackupIntent.UpdatePassword(it)) },
        onIncludeIconsChange = { viewModel.onIntent(BackupIntent.UpdateIncludeIcons(it)) },
        onIncludeAttachmentsChange = {
            viewModel.onIntent(BackupIntent.UpdateIncludeAttachments(it))
        },
        onIncludeDeletedChange = { viewModel.onIntent(BackupIntent.UpdateIncludeDeleted(it)) },
        onIncludedEntryTypesChange = {
            viewModel.onIntent(BackupIntent.UpdateIncludedEntryTypes(it))
        },
        onImportModeChange = { viewModel.onIntent(BackupIntent.UpdateImportMode(it)) },
        onExport = {
            activeSheet = null
            if (!directoryUri.isNullOrBlank()) {
                viewModel.onIntent(BackupIntent.StartExportInConfiguredDirectory)
            } else {
                val fileName = viewModel.buildExportFileName()
                when (state.selectedExportFormat) {
                    BackupExportUiFormat.ENCRYPTED -> encryptedExportPicker.launch(fileName)
                    BackupExportUiFormat.JSON -> jsonExportPicker.launch(fileName)
                    BackupExportUiFormat.TEXT -> textExportPicker.launch(fileName)
                }
            }
        },
        onImport = {
            activeSheet = null
            viewModel.onIntent(BackupIntent.ProcessBackupAction)
        },
    )
}

/**
 * Narrow Recovery-facing API. Recovery owns its restricted-mode workflow while Backup owns
 * the reusable form UI and its internal presentation models.
 */
@Composable
fun RecoveryBackupExportSheet(
    visible: Boolean,
    password: String,
    includeIcons: Boolean,
    includeAttachments: Boolean,
    includeDeleted: Boolean,
    onDismiss: () -> Unit,
    onPasswordChange: (String) -> Unit,
    onIncludeIconsChange: (Boolean) -> Unit,
    onIncludeAttachmentsChange: (Boolean) -> Unit,
    onIncludeDeletedChange: (Boolean) -> Unit,
    onExport: () -> Unit,
) {
    val state = remember(
        password,
        includeIcons,
        includeAttachments,
        includeDeleted,
    ) {
        BackupUiState(
            isExporting = true,
            isRecoveryExport = true,
            backupPassword = password,
            selectedExportFormat = BackupExportUiFormat.ENCRYPTED,
            includeIcons = includeIcons,
            includeAttachments = includeAttachments,
            includeDeleted = includeDeleted,
        )
    }

    BackupRestoreSheetHost(
        sheet = BackupSheet.EXPORT_OPTIONS.takeIf { visible },
        state = state,
        configuredDirectoryLabel = null,
        onDismiss = onDismiss,
        onFormatSelected = {},
        onPasswordChange = onPasswordChange,
        onIncludeIconsChange = onIncludeIconsChange,
        onIncludeAttachmentsChange = onIncludeAttachmentsChange,
        onIncludeDeletedChange = onIncludeDeletedChange,
        onIncludedEntryTypesChange = {},
        onImportModeChange = {},
        onExport = onExport,
        onImport = {},
    )
}
