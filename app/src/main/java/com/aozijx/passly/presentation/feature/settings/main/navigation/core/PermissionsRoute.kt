package com.aozijx.passly.presentation.feature.settings.main.navigation.core

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.app.platform.permission.rememberPermissionRequestHost
import com.aozijx.passly.core.permission.model.PermissionRequestOutcome
import com.aozijx.passly.core.permission.model.PermissionRequestStart
import com.aozijx.passly.core.permission.model.RuntimePermission
import com.aozijx.passly.core.permission.model.RuntimePermissionAccess
import com.aozijx.passly.feature.backup.internal.archive.platform.BackupStorageSupport
import com.aozijx.passly.presentation.feature.settings.permissions.PermissionManagementEffect
import com.aozijx.passly.presentation.feature.settings.permissions.PermissionManagementScreenAction
import com.aozijx.passly.presentation.feature.settings.permissions.PermissionManagementViewModel
import com.aozijx.passly.presentation.feature.settings.ui.data.BackupDirectoryClearDialog
import com.aozijx.passly.presentation.feature.settings.ui.permissions.PermissionManagementScreen

@Composable
internal fun PermissionsRoute(
    onBack: (() -> Unit)?,
) {
    val context = LocalContext.current
    val viewModel: PermissionManagementViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var permissionResultGeneration by remember { mutableIntStateOf(0) }
    var showClearDirectoryDialog by rememberSaveable { mutableStateOf(false) }

    val permissionDeniedMessage = stringResource(R.string.settings_permissions_request_denied)
    val permissionBusyMessage = stringResource(R.string.settings_permissions_request_busy)
    val permissionUnavailableMessage = stringResource(
        R.string.settings_permissions_request_unavailable,
    )
    val settingsUnavailableMessage = stringResource(
        R.string.settings_permissions_system_settings_unavailable,
    )
    val selectionCancelledMessage = stringResource(
        R.string.settings_backup_directory_selection_cancelled,
    )
    val directoryPermissionDeniedMessage = stringResource(
        R.string.settings_backup_directory_permission_denied,
    )
    val directoryUnavailableMessage = stringResource(
        R.string.settings_backup_directory_unavailable,
    )

    fun showMessage(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    val permissionHost = rememberPermissionRequestHost("settings.permissions") { _, result ->
        permissionResultGeneration++
        if (result is PermissionRequestOutcome.Denied) {
            showMessage(permissionDeniedMessage)
        }
    }

    fun refreshRuntimeAccess() {
        viewModel.runtimeAccessUpdated(
            RuntimePermission.entries.associateWith(permissionHost::access),
        )
    }

    fun openApplicationSettings() {
        try {
            context.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:${context.packageName}"),
                ),
            )
        } catch (_: ActivityNotFoundException) {
            showMessage(settingsUnavailableMessage)
        } catch (_: SecurityException) {
            showMessage(settingsUnavailableMessage)
        }
    }

    fun handleRuntimePermission(permission: RuntimePermission) {
        when (permissionHost.access(permission)) {
            RuntimePermissionAccess.GRANTED,
            RuntimePermissionAccess.SETTINGS_REQUIRED -> openApplicationSettings()
            RuntimePermissionAccess.NOT_APPLICABLE -> showMessage(permissionUnavailableMessage)
            RuntimePermissionAccess.REQUESTABLE -> {
                when (permissionHost.request(permission)) {
                    PermissionRequestStart.Launched -> Unit
                    PermissionRequestStart.AlreadyGranted -> permissionResultGeneration++
                    PermissionRequestStart.Busy -> showMessage(permissionBusyMessage)
                    PermissionRequestStart.NotApplicable,
                    PermissionRequestStart.HostUnavailable ->
                        showMessage(permissionUnavailableMessage)
                }
            }
        }
    }

    val backupPathPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        viewModel.directoryPicked(uri?.toString())
    }

    LaunchedEffect(permissionHost, permissionResultGeneration) {
        refreshRuntimeAccess()
    }

    LifecycleResumeEffect(permissionHost, viewModel) {
        refreshRuntimeAccess()
        viewModel.refreshDirectoryAccess()
        onPauseOrDispose { }
    }

    LaunchedEffect(
        viewModel,
        selectionCancelledMessage,
        directoryPermissionDeniedMessage,
        directoryUnavailableMessage,
    ) {
        viewModel.effects.collect { effect ->
            showMessage(
                when (effect) {
                    PermissionManagementEffect.BackupDirectorySelectionCancelled ->
                        selectionCancelledMessage
                    PermissionManagementEffect.BackupDirectoryPermissionDenied ->
                        directoryPermissionDeniedMessage
                    PermissionManagementEffect.BackupDirectoryUnavailable ->
                        directoryUnavailableMessage
                },
            )
        }
    }

    PermissionManagementScreen(
        state = state,
        onBack = onBack,
        onAction = { action ->
            when (action) {
                is PermissionManagementScreenAction.RuntimePermissionSelected ->
                    handleRuntimePermission(action.permission)
                PermissionManagementScreenAction.SelectBackupDirectory ->
                    backupPathPicker.launch(BackupStorageSupport.defaultDocumentsTreeUri())
                PermissionManagementScreenAction.ClearBackupDirectory ->
                    showClearDirectoryDialog = true
            }
        },
    )

    if (showClearDirectoryDialog) {
        BackupDirectoryClearDialog(
            onConfirm = {
                viewModel.clearDirectory()
                showClearDirectoryDialog = false
            },
            onDismiss = { showClearDirectoryDialog = false },
        )
    }
}
