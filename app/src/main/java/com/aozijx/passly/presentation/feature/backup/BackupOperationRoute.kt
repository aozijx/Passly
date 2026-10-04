package com.aozijx.passly.presentation.feature.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.app.message.compose.LocalAppNoticePublisher
import com.aozijx.passly.app.message.model.newAppNotice
import com.aozijx.passly.feature.backup.internal.model.BackupExportFormat
import com.aozijx.passly.presentation.feature.backup.ui.BackupRestoreDetail
import com.aozijx.passly.presentation.feature.backup.ui.BackupRestoreSheet

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

    fun startManualExport(uri: Uri?) {
        if (uri == null) {
            viewModel.onAction(BackupUiAction.DismissOptions)
            return
        }
        viewModel.onAction(BackupUiAction.StartExport(uri = uri))
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
        }
    }

    LaunchedEffect(viewModel, noticePublisher) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is BackupNoticeEffect ->
                    noticePublisher.publish(
                        newAppNotice(effect.toNoticeCode(), effect.toNoticeArguments()),
                    )

                BackupEffect.SelectImportDocument -> importPicker.launch(
                    arrayOf(
                        "application/octet-stream",
                        "application/json",
                        "text/json",
                        "*/*",
                    ),
                )

                is BackupEffect.SelectExportDocument -> when (effect.format) {
                    BackupExportFormat.ENCRYPTED -> encryptedExportPicker.launch(effect.fileName)
                    BackupExportFormat.JSON -> jsonExportPicker.launch(effect.fileName)
                    BackupExportFormat.TEXT -> textExportPicker.launch(effect.fileName)
                }
            }
        }
    }

    DisposableEffect(viewModel) {
        onDispose {
            viewModel.onAction(BackupUiAction.DismissOptions)
        }
    }

    BackupRestoreDetail(
        backupPathLabel = directoryLabel,
        lastExportFileLabel = lastExportFileLabel,
        onExport = { viewModel.onAction(BackupUiAction.OpenExportOptions) },
        onImport = { viewModel.onAction(BackupUiAction.RequestImportDocument) },
        onPickBackupPath = onPickBackupPath,
        onTestBackupWrite = {
            viewModel.onAction(BackupUiAction.CheckDirectoryPermission(directoryUri))
        },
        onClearBackupPath = onClearBackupPath,
    )

    BackupRestoreSheet(
        state = state.toSheetUiState(
            configuredDirectoryLabel = directoryLabel.takeIf { !directoryUri.isNullOrBlank() },
        ),
        onAction = viewModel::onAction,
    )
}
