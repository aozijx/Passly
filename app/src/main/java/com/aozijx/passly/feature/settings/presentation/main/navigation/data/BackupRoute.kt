package com.aozijx.passly.feature.settings.presentation.main.navigation.data

import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.core.platform.path.UriDisplayNameFormatter
import com.aozijx.passly.feature.backup.presentation.BackupOperationRoute
import com.aozijx.passly.feature.settings.presentation.backup.DataManagementSettingsEffect
import com.aozijx.passly.feature.settings.presentation.backup.DataManagementSettingsUiAction
import com.aozijx.passly.feature.settings.presentation.backup.DataManagementSettingsUiState
import com.aozijx.passly.feature.settings.presentation.backup.DataManagementSettingsViewModel
import com.aozijx.passly.feature.settings.presentation.ui.data.BackupDirectoryClearDialog
import com.aozijx.passly.feature.settings.presentation.ui.main.SettingsSecondaryPage
import com.aozijx.passly.feature.settings.presentation.ui.main.component.SettingsGroup
import com.aozijx.passly.presentation.shared.event.collectPageEffects
import com.aozijx.passly.presentation.shared.state.SettingsReadFailureContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BackupRoute(onBack: (() -> Unit)?) {
    val context = LocalContext.current
    val viewModel: DataManagementSettingsViewModel = hiltViewModel()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val pageOwner = remember(viewModel) { mutableStateOf<Any?>(null) }
    val directoryRequestId = remember(viewModel) { mutableStateOf<String?>(null) }
    DisposableEffect(viewModel) {
        val owner = viewModel.onPageEntered()
        pageOwner.value = owner
        onDispose {
            pageOwner.value = null
            directoryRequestId.value = null
            viewModel.onPageExited(owner)
        }
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    if (state is DataManagementSettingsUiState.Failed) {
        SettingsSecondaryPage(
            title = stringResource(SettingsGroup.DATA_MANAGEMENT.titleRes),
            onBack = onBack,
        ) {
            item {
                SettingsReadFailureContent(
                    onRetry = { viewModel.onAction(DataManagementSettingsUiAction.RetryRead) },
                )
            }
        }
        return
    }
    val readyState =
        when (val current = state) {
            DataManagementSettingsUiState.Loading -> return
            is DataManagementSettingsUiState.Failed -> return
            is DataManagementSettingsUiState.Ready -> current
        }
    var showClearDirectoryDialog by rememberSaveable { mutableStateOf(false) }
    val notSetText = stringResource(R.string.not_set)
    val selectionCancelledMessage =
        stringResource(
            R.string.settings_backup_directory_selection_cancelled,
        )
    val permissionDeniedMessage =
        stringResource(
            R.string.settings_backup_directory_permission_denied,
        )
    val unavailableMessage =
        stringResource(
            R.string.settings_backup_directory_unavailable,
        )
    val pathLabel =
        remember(readyState.directoryUri) {
            UriDisplayNameFormatter.format(readyState.directoryUri) ?: notSetText
        }
    val backupPathPicker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocumentTree(),
        ) { uri ->
            val owner = pageOwner.value
            val requestId = directoryRequestId.value
            directoryRequestId.value = null
            if (owner != null && requestId != null) {
                viewModel.onAction(
                    DataManagementSettingsUiAction.BackupDirectoryPicked(
                        uri = uri?.toString(),
                        pageOwner = owner,
                        requestId = requestId,
                    ),
                )
            }
        }

    LaunchedEffect(
        viewModel,
        lifecycle,
        context,
        selectionCancelledMessage,
        permissionDeniedMessage,
        unavailableMessage,
    ) {
        collectPageEffects(lifecycle, viewModel.effects) { effect ->
            val message =
                when (effect) {
                    DataManagementSettingsEffect.BackupDirectorySelectionCancelled -> {
                        selectionCancelledMessage
                    }

                    DataManagementSettingsEffect.BackupDirectoryPermissionDenied -> {
                        permissionDeniedMessage
                    }

                    DataManagementSettingsEffect.BackupDirectoryUnavailable -> {
                        unavailableMessage
                    }
                }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    SettingsSecondaryPage(
        title = stringResource(SettingsGroup.BACKUP_RESTORE.titleRes),
        onBack = onBack,
    ) {
        item {
            BackupOperationRoute(
                directoryUri = readyState.directoryUri,
                directoryLabel = pathLabel,
                lastExportFileLabel = notSetText,
                onPickBackupPath = {
                    val owner = pageOwner.value
                    val requestId = owner?.let(viewModel::beginDirectorySelection)
                    if (requestId != null) {
                        directoryRequestId.value = requestId
                        backupPathPicker.launch(
                            DocumentsContract.buildTreeDocumentUri(
                                "com.android.externalstorage.documents",
                                "primary:Documents",
                            ),
                        )
                    }
                },
                onClearBackupPath =
                    if (readyState.directoryUri.isNullOrBlank()) {
                        null
                    } else {
                        ({ showClearDirectoryDialog = true })
                    },
            )
        }
    }

    if (showClearDirectoryDialog) {
        BackupDirectoryClearDialog(
            onConfirm = {
                viewModel.onAction(DataManagementSettingsUiAction.ClearBackupDirectory)
                showClearDirectoryDialog = false
            },
            onDismiss = { showClearDirectoryDialog = false },
        )
    }
}
