package com.aozijx.passly.feature.backup.presentation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.aozijx.passly.app.message.compose.LocalAppNoticePublisher
import com.aozijx.passly.app.message.model.newAppNotice
import com.aozijx.passly.presentation.shared.event.collectPageEffects
import com.aozijx.passly.feature.backup.contract.BackupExportFormat
import com.aozijx.passly.feature.backup.presentation.ui.BackupRestoreDetail
import com.aozijx.passly.feature.backup.presentation.ui.BackupRestoreSheet

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
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val noticePublisher = LocalAppNoticePublisher.current
    val pageOwner = remember(viewModel) { mutableStateOf<Any?>(null) }
    val importRequestId = remember(viewModel) { mutableStateOf<String?>(null) }
    val exportRequestId = remember(viewModel) { mutableStateOf<String?>(null) }

    fun startManualExport(uri: Uri?) {
        val owner = pageOwner.value
        val requestId = exportRequestId.value
        exportRequestId.value = null
        if (owner != null && requestId != null) viewModel.onAction(
            BackupUiAction.StartExport(
                uri = uri?.toString(),
                pageOwner = owner,
                requestId = requestId,
                deleteOnFailure = true,
            ),
        )
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
        val owner = pageOwner.value
        val requestId = importRequestId.value
        if (owner != null && requestId != null) {
            viewModel.onAction(BackupUiAction.StartImport(uri?.toString(), owner, requestId))
        }
        importRequestId.value = null
    }

    LaunchedEffect(viewModel, lifecycle, noticePublisher) {
        collectPageEffects(lifecycle, viewModel.effects) { effect ->
            when (effect) {
                is BackupNoticeEffect ->
                    noticePublisher.publish(
                        newAppNotice(effect.toNoticeCode(), effect.toNoticeArguments()),
                    )

                is BackupEffect.SelectImportDocument -> {
                    importRequestId.value = effect.requestId
                    importPicker.launch(
                        arrayOf(
                            "application/octet-stream",
                            "application/json",
                            "text/json",
                            "*/*",
                        ),
                    )
                }

                is BackupEffect.SelectExportDocument -> {
                    exportRequestId.value = effect.requestId
                    when (effect.format) {
                        BackupExportFormat.ENCRYPTED -> encryptedExportPicker.launch(effect.fileName)
                        BackupExportFormat.JSON -> jsonExportPicker.launch(effect.fileName)
                        BackupExportFormat.TEXT -> textExportPicker.launch(effect.fileName)
                    }
                }
            }
        }
    }

    DisposableEffect(viewModel) {
        val owner = viewModel.onPageEntered()
        pageOwner.value = owner
        onDispose {
            pageOwner.value = null
            importRequestId.value = null
            exportRequestId.value = null
            viewModel.onPageExited(owner)
        }
    }

    BackupRestoreDetail(
        backupPathLabel = directoryLabel,
        lastExportFileLabel = lastExportFileLabel,
        onExport = { viewModel.onAction(BackupUiAction.OpenExportOptions) },
        onImport = {
            pageOwner.value?.let { owner ->
                viewModel.onAction(BackupUiAction.RequestImportDocument(owner))
            }
        },
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
