package com.aozijx.passly.feature.settings.presentation.main.navigation.core

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.app.platform.permission.rememberPermissionRequestHost
import com.aozijx.passly.core.permission.model.PermissionRequestOutcome
import com.aozijx.passly.core.permission.model.PermissionRequestStart
import com.aozijx.passly.core.permission.model.RuntimePermission
import com.aozijx.passly.core.permission.model.RuntimePermissionAccess
import com.aozijx.passly.feature.settings.presentation.permissions.PermissionManagementScreenAction
import com.aozijx.passly.feature.settings.presentation.permissions.PermissionManagementViewModel
import com.aozijx.passly.feature.settings.presentation.ui.permissions.PermissionManagementScreen

@Composable
internal fun PermissionsRoute(
    onOpenBackupSettings: () -> Unit,
    onBack: (() -> Unit)?,
) {
    val context = LocalContext.current
    val viewModel: PermissionManagementViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var permissionResultGeneration by remember { mutableIntStateOf(0) }

    val permissionDeniedMessage = stringResource(R.string.settings_permissions_request_denied)
    val permissionBusyMessage = stringResource(R.string.settings_permissions_request_busy)
    val permissionUnavailableMessage = stringResource(
        R.string.settings_permissions_request_unavailable,
    )
    val settingsUnavailableMessage = stringResource(
        R.string.settings_permissions_system_settings_unavailable,
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
                    "package:${context.packageName}".toUri(),
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

    LaunchedEffect(permissionHost, permissionResultGeneration) {
        refreshRuntimeAccess()
    }

    LifecycleResumeEffect(permissionHost, viewModel) {
        refreshRuntimeAccess()
        viewModel.refreshDirectoryAccess()
        onPauseOrDispose { }
    }

    PermissionManagementScreen(
        state = state,
        onBack = onBack,
        onAction = { action ->
            when (action) {
                is PermissionManagementScreenAction.RuntimePermissionSelected ->
                    handleRuntimePermission(action.permission)
                PermissionManagementScreenAction.OpenBackupSettings ->
                    onOpenBackupSettings()
            }
        },
    )
}
