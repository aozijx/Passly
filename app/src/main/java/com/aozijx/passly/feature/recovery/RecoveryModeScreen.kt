package com.aozijx.passly.feature.recovery

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.core.ui.components.group.RoundedGroup
import com.aozijx.passly.core.ui.components.group.navigationSettingsGroupItem
import com.aozijx.passly.core.ui.components.group.settingsGroupItem
import com.aozijx.passly.core.ui.components.settings.SettingsSection
import com.aozijx.passly.core.ui.components.settings.SettingsSectionTitle
import com.aozijx.passly.feature.backup.contract.BackupOperationStatus
import com.aozijx.passly.feature.backup.contract.BackupUiState
import com.aozijx.passly.feature.backup.model.BackupExportUiFormat
import com.aozijx.passly.feature.recovery.contract.RecoveryModeEffect
import com.aozijx.passly.feature.recovery.contract.RecoveryModeIntent
import com.aozijx.passly.feature.settings.apppassword.ui.AppPasswordSetDialog
import com.aozijx.passly.feature.settings.datamanagement.BackupRestoreSheetHost
import com.aozijx.passly.feature.settings.datamanagement.BackupSheet

/** Restricted UI shown after recovery-code verification. No Vault content is mounted here. */
@Composable
fun RecoveryModeScreen(
    viewModel: RecoveryModeViewModel,
    onExit: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val exportPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        viewModel.onIntent(RecoveryModeIntent.ExportTargetPicked(uri))
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is RecoveryModeEffect.PickExportTarget -> exportPicker.launch(effect.fileName)
                is RecoveryModeEffect.ExitRecovery -> onExit()
                is RecoveryModeEffect.ShowMessage -> { /* handled by UI state */
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
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

        SettingsSection(modifier = Modifier.fillMaxWidth()) {
            SettingsSectionTitle(text = stringResource(R.string.settings_security_auth_section))
            RoundedGroup(
                items = listOf(
                    navigationSettingsGroupItem(
                        key = "recovery.set_app_password",
                        icon = Icons.Default.LockReset,
                        title = stringResource(R.string.recovery_mode_set_password),
                        subtitle = stringResource(R.string.settings_security_app_password_description),
                        isLoading = state.isSettingPassword,
                        onClick = { viewModel.onIntent(RecoveryModeIntent.SetPasswordClicked) }
                    ),
                    navigationSettingsGroupItem(
                        key = "recovery.reconfigure_biometric",
                        icon = Icons.Default.Fingerprint,
                        title = stringResource(R.string.recovery_mode_reconfigure_biometric),
                        subtitle = state.biometricResult?.let { success ->
                            stringResource(
                                if (success) R.string.recovery_mode_biometric_success
                                else R.string.recovery_mode_biometric_failed
                            )
                        },
                        isLoading = state.isReconfiguringBiometric,
                        onClick = {
                            viewModel.onIntent(RecoveryModeIntent.ReconfigureBiometricClicked)
                        }
                    )
                )
            )
        }

        Spacer(Modifier.height(16.dp))
        SettingsSection(modifier = Modifier.fillMaxWidth()) {
            SettingsSectionTitle(text = stringResource(R.string.settings_backup_restore_section))
            RoundedGroup(
                items = listOf(
                    navigationSettingsGroupItem(
                        key = "recovery.export_backup",
                        icon = Icons.Default.FileDownload,
                        title = stringResource(R.string.recovery_mode_export),
                        subtitle = if (state.exportError != null) {
                            stringResource(R.string.recovery_mode_export_failed)
                        } else {
                            stringResource(R.string.settings_backup_format_encrypted_description)
                        },
                        isLoading = state.isExporting,
                        onClick = { viewModel.onIntent(RecoveryModeIntent.ExportClicked) }
                    )
                )
            )
        }

        Spacer(Modifier.height(16.dp))
        SettingsSection(modifier = Modifier.fillMaxWidth()) {
            RoundedGroup(
                items = listOf(
                    settingsGroupItem(
                        key = "recovery.exit",
                        icon = Icons.AutoMirrored.Filled.Logout,
                        title = stringResource(R.string.recovery_mode_exit),
                        subtitle = stringResource(R.string.notice_app_locked),
                        onClick = { viewModel.onIntent(RecoveryModeIntent.ExitClicked) }
                    )
                ),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        }
    }

    if (state.showSetPasswordDialog) {
        AppPasswordSetDialog(
            newPassword = state.newPassword,
            confirmPassword = state.confirmPassword,
            onNewPasswordChange = {
                viewModel.onIntent(RecoveryModeIntent.NewPasswordChanged(it))
            },
            onConfirmPasswordChange = {
                viewModel.onIntent(RecoveryModeIntent.ConfirmPasswordChanged(it))
            },
            onConfirm = { viewModel.onIntent(RecoveryModeIntent.SubmitNewPassword) },
            onDismiss = { viewModel.onIntent(RecoveryModeIntent.DismissSheet) },
            isBusy = state.isSettingPassword,
            errorMessage = state.passwordSetupError
        )
    }

    // Map RecoveryModeUiState to BackupUiState for the export sheet
    val backupUiState = remember(state) {
        BackupUiState(
            status = if (state.isExporting) BackupOperationStatus.Loading
            else if (state.exportError != null) BackupOperationStatus.Failure
            else BackupOperationStatus.Idle,
            isExporting = true,
            isRecoveryExport = true,
            backupPassword = state.exportPassword,
            selectedExportFormat = BackupExportUiFormat.ENCRYPTED,
            includeIcons = state.includeIcons,
            includeAttachments = state.includeAttachments,
            includeDeleted = state.includeDeleted
        )
    }

    BackupRestoreSheetHost(
        sheet = if (state.showExportOptions) BackupSheet.EXPORT_OPTIONS else null,
        state = backupUiState,
        configuredDirectoryLabel = null,
        onDismiss = { viewModel.onIntent(RecoveryModeIntent.DismissSheet) },
        onFormatSelected = {},
        onPasswordChange = {
            viewModel.onIntent(RecoveryModeIntent.ExportPasswordChanged(it))
        },
        onIncludeIconsChange = {
            viewModel.onIntent(RecoveryModeIntent.IncludeIconsChanged(it))
        },
        onIncludeAttachmentsChange = {
            viewModel.onIntent(RecoveryModeIntent.IncludeAttachmentsChanged(it))
        },
        onIncludeDeletedChange = {
            viewModel.onIntent(RecoveryModeIntent.IncludeDeletedChanged(it))
        },
        onIncludedEntryTypesChange = {},
        onImportModeChange = {},
        onExport = {
            viewModel.onIntent(RecoveryModeIntent.SubmitExport)
        },
        onImport = {}
    )
}
