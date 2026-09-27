package com.aozijx.passly.presentation.feature.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.app.message.compose.LocalAppNoticePublisher
import com.aozijx.passly.app.message.model.newAppNotice
import com.aozijx.passly.feature.backup.internal.model.BackupExportFormat
import com.aozijx.passly.presentation.feature.backup.BackupUiAction
import com.aozijx.passly.presentation.feature.backup.BackupViewModel
import com.aozijx.passly.presentation.feature.backup.ui.BackupRestoreDetail
import com.aozijx.passly.presentation.feature.backup.ui.BackupRestoreSheet
import com.aozijx.passly.presentation.feature.backup.ui.model.BackupSheet
import com.aozijx.passly.presentation.feature.backup.ui.model.BackupSheetEvent
import com.aozijx.passly.domain.sensitive.OwnedChars

/**
 * Public settings entry point for the Backup feature.
 *
 * Consumers provide only settings-owned directory callbacks. Backup presentation state,
 * actions, platform document launchers, and sheets remain private to this feature.
 */
@Composable
fun BackupOperationRoute(
    directoryUri: String?,
    directoryLabel: String,
    lastExportFileLabel: String,
    onPickBackupPath: () -> Unit,
    onClearBackupPath: (() -> Unit)?,
) {
    val viewModel: BackupViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val noticePublisher = LocalAppNoticePublisher.current
    var activeSheet by remember { mutableStateOf<BackupSheet?>(null) }

    LaunchedEffect(viewModel, noticePublisher) {
        viewModel.effects.collect { effect ->
            noticePublisher.publish(newAppNotice(effect.toNoticeCode()))
        }
    }

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
        onEvent = { event ->
            when (event) {
                BackupSheetEvent.Dismissed -> {
                    activeSheet = null
                    viewModel.onAction(BackupUiAction.CancelPendingOperation)
                }
                is BackupSheetEvent.FormatSelected -> {
                    viewModel.onAction(BackupUiAction.PrepareExport(event.format))
                    activeSheet = BackupSheet.EXPORT_OPTIONS
                }
                is BackupSheetEvent.PasswordChanged -> viewModel.onAction(
                    BackupUiAction.UpdatePassword(OwnedChars.fromString(event.password)),
                )
                is BackupSheetEvent.IncludeIconsChanged -> viewModel.onAction(
                    BackupUiAction.UpdateIncludeIcons(event.include),
                )
                is BackupSheetEvent.IncludeAttachmentsChanged -> viewModel.onAction(
                    BackupUiAction.UpdateIncludeAttachments(event.include),
                )
                is BackupSheetEvent.IncludeDeletedChanged -> viewModel.onAction(
                    BackupUiAction.UpdateIncludeDeleted(event.include),
                )
                is BackupSheetEvent.IncludedEntryTypesChanged -> viewModel.onAction(
                    BackupUiAction.UpdateIncludedEntryTypes(event.types.toFeatureModels()),
                )
                is BackupSheetEvent.ImportModeChanged -> viewModel.onAction(
                    BackupUiAction.UpdateImportMode(event.mode),
                )
                BackupSheetEvent.ExportRequested -> {
                    activeSheet = null
                    if (!directoryUri.isNullOrBlank()) {
                        viewModel.onAction(BackupUiAction.StartExportInConfiguredDirectory)
                    } else {
                        val fileName = state.pendingExportFileName ?: return@BackupRestoreSheet
                        when (state.selectedExportFormat) {
                            BackupExportFormat.ENCRYPTED -> encryptedExportPicker.launch(fileName)
                            BackupExportFormat.JSON -> jsonExportPicker.launch(fileName)
                            BackupExportFormat.TEXT -> textExportPicker.launch(fileName)
                        }
                    }
                }
                BackupSheetEvent.ImportRequested -> {
                    activeSheet = null
                    viewModel.onAction(BackupUiAction.ProcessBackupAction)
                }
            }
        },
    )
}
