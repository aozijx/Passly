package com.aozijx.passly.feature.settings.navigation

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.aozijx.passly.R
import com.aozijx.passly.core.util.PathUtils
import com.aozijx.passly.feature.backup.BackupViewModel
import com.aozijx.passly.feature.backup.contract.BackupIntent
import com.aozijx.passly.feature.backup.storage.BackupExportStorageSupport
import com.aozijx.passly.feature.settings.datamanagement.DataManagementDetail
import com.aozijx.passly.feature.settings.datamanagement.DataUiAction
import com.aozijx.passly.feature.settings.datamanagement.DataViewModel
import com.aozijx.passly.feature.settings.datamanagement.handleBackupPathPicked
import com.aozijx.passly.feature.settings.general.GeneralDetail
import com.aozijx.passly.feature.settings.interaction.InteractionDetail
import com.aozijx.passly.feature.settings.interaction.InteractionUiAction
import com.aozijx.passly.feature.settings.interaction.InteractionViewModel
import com.aozijx.passly.feature.settings.security.RecoveryDraftState
import com.aozijx.passly.feature.settings.security.RecoveryDraftViewModel
import com.aozijx.passly.feature.settings.security.SecurityUiAction
import com.aozijx.passly.feature.settings.security.SecurityViewModel
import com.aozijx.passly.feature.settings.security.messageOrNull
import com.aozijx.passly.feature.settings.security.ui.RecoveryCodeDetail
import com.aozijx.passly.feature.settings.security.ui.RecoveryCodeSheet
import com.aozijx.passly.feature.settings.shell.SettingsScreenLocalState
import com.aozijx.passly.feature.settings.shell.SettingsSecondaryPage

@OptIn(ExperimentalMaterial3Api::class)
internal fun NavGraphBuilder.registerDataSettingsRoutes(
    navController: NavHostController,
    context: Context,
    localState: SettingsScreenLocalState,
    interactionViewModel: InteractionViewModel,
    dataViewModel: DataViewModel,
    backupViewModel: BackupViewModel
) {
    composable(SettingsRoute.Interaction.route) {
        val state by interactionViewModel.config.collectAsStateWithLifecycle()
        SettingsSecondaryPage(title = "交互与操作", onBack = { navController.popBackStack() }) {
            item {
                InteractionDetail(
                    state = state,
                    onSwipeEnabledChange = {
                        interactionViewModel.onAction(InteractionUiAction.SetSwipeEnabled(it))
                    },
                    onLeftSwipeActionClick = localState::openLeftActionDialog,
                    onRightSwipeActionClick = localState::openRightActionDialog,
                    onToggleAutofillUiMode = {
                        interactionViewModel.onAction(InteractionUiAction.ToggleAutofillUiMode)
                    }
                )
            }
        }
    }

    composable(SettingsRoute.DataManagement.route) {
        val state by dataViewModel.config.collectAsStateWithLifecycle()
        val notSetText = stringResource(R.string.not_set)
        val backupPathLabel = remember(state.directoryUri) {
            PathUtils.formatPath(state.directoryUri) ?: notSetText
        }
        val lastExportFileLabel = remember(state.lastExportFileName) {
            PathUtils.formatPath(state.lastExportFileName) ?: notSetText
        }
        val backupPathPicker =
            rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
                handleBackupPathPicked(context, uri) { resolvedUri ->
                    dataViewModel.onAction(DataUiAction.SetBackupDirectoryUri(resolvedUri))
                }
            }

        SettingsSecondaryPage(title = "数据管理", onBack = { navController.popBackStack() }) {
            item {
                DataManagementDetail(
                    state = state,
                    backupPathLabel = backupPathLabel,
                    lastExportFileLabel = lastExportFileLabel,
                    onAutoDownloadIconsChange = {
                        dataViewModel.onAction(DataUiAction.SetAutoDownloadIcons(it))
                    },
                    onPickBackupPath = {
                        backupPathPicker.launch(
                            BackupExportStorageSupport.defaultDocumentsTreeUri()
                        )
                    },
                    onTestBackupWrite = {
                        backupViewModel.onIntent(BackupIntent.CheckDirectoryPermission(state.directoryUri))
                    },
                    onClearBackupPath = if (state.directoryUri.isNullOrBlank()) null
                    else localState::openClearBackupDirConfirmDialog
                )
            }
        }
    }

    composable(SettingsRoute.RecoveryCode.route) {
        val viewModel: SecurityViewModel = hiltViewModel()
        val draftViewModel: RecoveryDraftViewModel = hiltViewModel()
        val draftState by draftViewModel.state.collectAsStateWithLifecycle()
        val recoveryCode = remember(draftState) {
            if (draftState is RecoveryDraftState.Ready) {
                draftViewModel.revealCode()?.concatToString()
            } else {
                null
            }
        }
        val hasEnvelope by viewModel.hasRecoveryEnvelope.collectAsStateWithLifecycle()
        val verifyResult by viewModel.verifyResult.collectAsStateWithLifecycle()

        LaunchedEffect(recoveryCode) {
            if (recoveryCode != null) localState.showRecoveryCodeSheet = true
        }
        recoveryCode?.let { code ->
            if (localState.showRecoveryCodeSheet) {
                RecoveryCodeSheet(
                    recoveryCode = code,
                    sheetState = localState.recoveryCodeSheetState,
                    onConfirm = {
                        localState.showRecoveryCodeSheet = false
                        draftViewModel.confirmAndEnable()
                    },
                    onDismiss = {
                        localState.showRecoveryCodeSheet = false
                        draftViewModel.dismiss()
                    }
                )
            }
        }

        SettingsSecondaryPage(title = "恢复码", onBack = { navController.popBackStack() }) {
            draftState.messageOrNull()?.let { message ->
                item {
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            item {
                RecoveryCodeDetail(
                    hasRecoveryEnvelope = hasEnvelope || draftState is RecoveryDraftState.Committed,
                    verifyResult = verifyResult,
                    onCreateRecoveryCode = {
                        draftViewModel.generate()
                    },
                    onRegenerate = {
                        draftViewModel.generate()
                    },
                    onVerifyCode = {
                        viewModel.onAction(SecurityUiAction.VerifyRecoveryCode(it))
                    },
                    onClearVerifyResult = {
                        viewModel.onAction(SecurityUiAction.ClearVerifyResult)
                    }
                )
            }
        }
    }

    composable(SettingsRoute.General.route) {
        SettingsSecondaryPage(title = "通用", onBack = { navController.popBackStack() }) {
            item { GeneralDetail() }
        }
    }
}
