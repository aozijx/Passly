package com.aozijx.passly.feature.recovery

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.feature.auth.presentation.AuthenticationViewModel
import com.aozijx.passly.feature.backup.BackupViewModel
import com.aozijx.passly.feature.backup.contract.BackupIntent
import com.aozijx.passly.feature.backup.contract.BackupOperationStatus
import com.aozijx.passly.feature.settings.apppassword.ui.AppPasswordSetDialog
import com.aozijx.passly.feature.settings.datamanagement.BackupRestoreSheetHost
import com.aozijx.passly.feature.settings.datamanagement.BackupSheet

/** Restricted UI shown after recovery-code verification. No Vault content is mounted here. */
@Composable
fun RecoveryModeScreen(
    authenticationViewModel: AuthenticationViewModel,
    backupViewModel: BackupViewModel,
    onExit: () -> Unit
) {
    val authState by authenticationViewModel.uiState.collectAsStateWithLifecycle()
    val backupState by backupViewModel.uiState.collectAsStateWithLifecycle()
    var showExportOptions by remember { mutableStateOf(false) }
    var biometricResult by remember { mutableStateOf<Boolean?>(null) }

    val exportPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri == null) {
            backupViewModel.onIntent(BackupIntent.CancelPendingOperation)
        } else {
            backupViewModel.onIntent(
                BackupIntent.StartExport(
                    uri = uri,
                    fileNameHint = backupViewModel.buildExportFileName()
                )
            )
            backupViewModel.onIntent(BackupIntent.ProcessBackupAction)
        }
    }

    DisposableEffect(backupViewModel) {
        onDispose {
            backupViewModel.onIntent(BackupIntent.CancelPendingOperation)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Restore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.recovery_mode_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.recovery_mode_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))

        RecoveryActionButton(
            icon = Icons.Default.LockReset,
            text = stringResource(R.string.recovery_mode_set_password),
            onClick = authenticationViewModel::onShowSetPasswordDialog
        )
        Spacer(Modifier.height(8.dp))
        RecoveryActionButton(
            icon = Icons.Default.Fingerprint,
            text = stringResource(R.string.recovery_mode_reconfigure_biometric),
            onClick = {
                biometricResult = null
                authenticationViewModel.recoverBiometric { biometricResult = it }
            }
        )
        biometricResult?.let { success ->
            Text(
                text = stringResource(
                    if (success) R.string.recovery_mode_biometric_success
                    else R.string.recovery_mode_biometric_failed
                ),
                style = MaterialTheme.typography.bodySmall,
                color = if (success) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
        }
        Spacer(Modifier.height(8.dp))
        RecoveryActionButton(
            icon = Icons.Default.FileDownload,
            text = stringResource(R.string.recovery_mode_export),
            onClick = {
                backupViewModel.onIntent(BackupIntent.PrepareRecoveryExport)
                showExportOptions = true
            }
        )
        if (backupState.status is BackupOperationStatus.Failure) {
            Text(
                text = stringResource(R.string.recovery_mode_export_failed),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
            Text(
                text = stringResource(R.string.recovery_mode_exit),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }

    if (authState.showSetPasswordDialog) {
        AppPasswordSetDialog(
            newPassword = authState.newAppPassword.toPlainString(),
            confirmPassword = authState.confirmAppPassword.toPlainString(),
            onNewPasswordChange = authenticationViewModel::onNewAppPasswordChange,
            onConfirmPasswordChange = authenticationViewModel::onConfirmAppPasswordChange,
            onConfirm = authenticationViewModel::bootstrapAppPassword,
            onDismiss = authenticationViewModel::onDismissSetPasswordDialog,
            isBusy = authState.isSettingAppPassword,
            errorMessage = authState.setupFailure?.let {
                stringResource(R.string.auth_error_app_password_setup_failed)
            }
        )
    }

    BackupRestoreSheetHost(
        sheet = if (showExportOptions) BackupSheet.EXPORT_OPTIONS else null,
        state = backupState,
        configuredDirectoryLabel = null,
        onDismiss = {
            showExportOptions = false
            backupViewModel.onIntent(BackupIntent.CancelPendingOperation)
        },
        onFormatSelected = {},
        onPasswordChange = { backupViewModel.onIntent(BackupIntent.UpdatePassword(it)) },
        onIncludeIconsChange = {
            backupViewModel.onIntent(BackupIntent.UpdateIncludeIcons(it))
        },
        onIncludeAttachmentsChange = {
            backupViewModel.onIntent(BackupIntent.UpdateIncludeAttachments(it))
        },
        onIncludeDeletedChange = {
            backupViewModel.onIntent(BackupIntent.UpdateIncludeDeleted(it))
        },
        onIncludedEntryTypesChange = {
            backupViewModel.onIntent(BackupIntent.UpdateIncludedEntryTypes(it))
        },
        onImportModeChange = {},
        onExport = {
            showExportOptions = false
            exportPicker.launch(backupViewModel.buildExportFileName())
        },
        onImport = {}
    )
}

@Composable
private fun RecoveryActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Icon(icon, contentDescription = null)
        Text(text = text, modifier = Modifier.padding(start = 8.dp))
    }
}
