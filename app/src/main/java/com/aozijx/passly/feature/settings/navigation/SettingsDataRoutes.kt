package com.aozijx.passly.feature.settings.navigation

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.core.util.PathDisplayFormatter
import com.aozijx.passly.feature.backup.api.BackupSettingsFeature
import com.aozijx.passly.feature.backup.internal.archive.platform.BackupStorageSupport
import com.aozijx.passly.feature.settings.SettingsViewModel
import com.aozijx.passly.feature.settings.autofill.AutofillSettingsAction
import com.aozijx.passly.feature.settings.autofill.AutofillSettingsViewModel
import com.aozijx.passly.feature.settings.contract.SettingsUiAction
import com.aozijx.passly.feature.settings.contract.SettingsUiState
import com.aozijx.passly.feature.settings.datamanagement.DataManagementSettingsUiAction
import com.aozijx.passly.feature.settings.datamanagement.DataManagementSettingsViewModel
import com.aozijx.passly.feature.settings.datamanagement.DatabaseRecoveryViewModel
import com.aozijx.passly.feature.settings.datamanagement.handleBackupPathPicked
import com.aozijx.passly.feature.settings.interaction.InteractionSettingsAction
import com.aozijx.passly.feature.settings.interaction.InteractionSettingsViewModel
import com.aozijx.passly.feature.settings.security.RecoveryDraftAction
import com.aozijx.passly.feature.settings.security.RecoveryDraftState
import com.aozijx.passly.feature.settings.security.RecoveryDraftViewModel
import com.aozijx.passly.feature.settings.security.SecuritySettingsAction
import com.aozijx.passly.feature.settings.security.SecuritySettingsViewModel
import com.aozijx.passly.feature.settings.security.messageOrNull
import com.aozijx.passly.presentation.settings.autofill.AutofillDetail
import com.aozijx.passly.presentation.settings.datamanagement.DataManagementDetail
import com.aozijx.passly.presentation.settings.general.GeneralDetail
import com.aozijx.passly.presentation.settings.general.NotificationDetail
import com.aozijx.passly.presentation.settings.interaction.InteractionDetail
import com.aozijx.passly.presentation.settings.internal.SettingsGroup
import com.aozijx.passly.presentation.settings.security.RecoveryCodeDetail
import com.aozijx.passly.presentation.settings.security.RecoveryCodeSheet
import com.aozijx.passly.presentation.settings.shell.SettingsScreenLocalState
import com.aozijx.passly.presentation.settings.shell.SettingsSecondaryPage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DataSettingsRouteContent(
    route: SettingsRoute,
    context: Context,
    localState: SettingsScreenLocalState,
    interactionViewModel: InteractionSettingsViewModel,
    dataViewModel: DataManagementSettingsViewModel,
    recoveryViewModel: DatabaseRecoveryViewModel,
    settingsViewModel: SettingsViewModel,
    settingsState: SettingsUiState,
    onBack: (() -> Unit)?
) {
    when (route) {
        SettingsRoute.Interaction -> {
            val state by interactionViewModel.uiState.collectAsStateWithLifecycle()
            SettingsSecondaryPage(
                title = stringResource(SettingsGroup.INTERACTION.titleRes),
                onBack = onBack
            ) {
                item {
                    InteractionDetail(
                        state = state,
                        onSwipeEnabledChange = {
                            interactionViewModel.onAction(
                                InteractionSettingsAction.SetSwipeEnabled(it)
                            )
                        },
                        onLeftSwipeActionClick = localState::openLeftActionDialog,
                        onRightSwipeActionClick = localState::openRightActionDialog,
                    )
                }
            }
        }

        SettingsRoute.Autofill -> {
            val viewModel: AutofillSettingsViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            SettingsSecondaryPage(
                title = stringResource(SettingsGroup.AUTOFILL.titleRes),
                onBack = onBack
            ) {
                item {
                    AutofillDetail(
                        state = state,
                        onOpenAutofillSettings = {
                            viewModel.onAction(
                                AutofillSettingsAction.OpenSystemAutofillSettings
                            )
                        },
                        onAction = viewModel::onAction,
                    )
                }
            }
        }

        SettingsRoute.DataManagement -> {
            val state by dataViewModel.uiState.collectAsStateWithLifecycle()
            val recoveryState by recoveryViewModel.uiState.collectAsStateWithLifecycle()
            SettingsSecondaryPage(
                title = stringResource(SettingsGroup.DATA_MANAGEMENT.titleRes),
                onBack = onBack
            ) {
                item {
                    DataManagementDetail(
                        state = state,
                        recoveryState = recoveryState,
                        isClearingDatabase = settingsState.isClearingDatabase,
                        onAutoDownloadIconsChange = {
                            dataViewModel.onAction(
                                DataManagementSettingsUiAction.SetAutoDownloadIcons(it)
                            )
                        },
                        onRestoreTrashEntry = { entryId, expectedVersion ->
                            dataViewModel.onAction(
                                DataManagementSettingsUiAction.RestoreTrashEntry(
                                    entryId,
                                    expectedVersion
                                )
                            )
                        },
                        onDeleteTrashEntry = { entryId, expectedVersion ->
                            dataViewModel.onAction(
                                DataManagementSettingsUiAction.DeleteTrashEntry(
                                    entryId,
                                    expectedVersion
                                )
                            )
                        },
                        onEmptyTrash = {
                            dataViewModel.onAction(DataManagementSettingsUiAction.EmptyTrash)
                        },
                        onClearTrashError = {
                            dataViewModel.onAction(DataManagementSettingsUiAction.ClearTrashError)
                        },
                        onRecoveryAction = recoveryViewModel::onAction,
                        onClearDatabase = {
                            settingsViewModel.onAction(SettingsUiAction.ClearDatabase)
                        }
                    )
                }
            }
        }

        SettingsRoute.BackupRestore -> {
            val state by dataViewModel.uiState.collectAsStateWithLifecycle()
            val notSetText = stringResource(R.string.not_set)
            val pathLabel = remember(state.directoryUri) {
                PathDisplayFormatter.format(state.directoryUri) ?: notSetText
            }
            val backupPathPicker = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocumentTree()
            ) { uri ->
                handleBackupPathPicked(context, uri) { resolvedUri ->
                    dataViewModel.onAction(
                        DataManagementSettingsUiAction.SetBackupDirectoryUri(resolvedUri)
                    )
                }
            }

            SettingsSecondaryPage(
                title = stringResource(SettingsGroup.BACKUP_RESTORE.titleRes),
                onBack = onBack
            ) {
                item {
                    BackupSettingsFeature(
                        directoryUri = state.directoryUri,
                        directoryLabel = pathLabel,
                        lastExportFileLabel = notSetText,
                        onPickBackupPath = {
                            backupPathPicker.launch(
                                BackupStorageSupport.defaultDocumentsTreeUri()
                            )
                        },
                        onClearBackupPath = if (state.directoryUri.isNullOrBlank()) null
                        else localState::openClearBackupDirConfirmDialog
                    )
                }
            }
        }

        SettingsRoute.RecoveryCode -> {
            val viewModel: SecuritySettingsViewModel = hiltViewModel()
            val draftViewModel: RecoveryDraftViewModel = hiltViewModel()
            val draftState by draftViewModel.state.collectAsStateWithLifecycle()
            val securityState by viewModel.uiState.collectAsStateWithLifecycle()
            val recoveryCode = remember(draftState) {
                if (draftState is RecoveryDraftState.Ready) {
                    draftViewModel.revealCode()?.concatToString()
                } else {
                    null
                }
            }
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
                            draftViewModel.onAction(RecoveryDraftAction.ConfirmAndEnable)
                        },
                        onDismiss = {
                            localState.showRecoveryCodeSheet = false
                            draftViewModel.onAction(RecoveryDraftAction.Dismiss)
                        }
                    )
                }
            }

            SettingsSecondaryPage(
                title = stringResource(SettingsGroup.RECOVERY_CODE.titleRes),
                onBack = onBack
            ) {
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
                        hasRecoveryEnvelope = securityState.hasRecoveryEnvelope ||
                                draftState is RecoveryDraftState.Committed,
                        verifyResult = securityState.recoveryCodeVerificationResult,
                        onCreateRecoveryCode = {
                            draftViewModel.onAction(RecoveryDraftAction.Generate)
                        },
                        onRegenerate = {
                            draftViewModel.onAction(RecoveryDraftAction.Generate)
                        },
                        onVerifyCode = {
                            viewModel.onAction(SecuritySettingsAction.VerifyRecoveryCode(it))
                        },
                        onClearVerifyResult = {
                            viewModel.onAction(SecuritySettingsAction.ClearVerifyResult)
                        }
                    )
                }
            }
        }

        SettingsRoute.General -> {
            SettingsSecondaryPage(
                title = stringResource(SettingsGroup.GENERAL.titleRes),
                onBack = onBack
            ) {
                item { GeneralDetail() }
            }
        }

        SettingsRoute.Notifications -> {
            SettingsSecondaryPage(
                title = stringResource(SettingsGroup.NOTIFICATIONS.titleRes),
                onBack = onBack
            ) {
                item { NotificationDetail() }
            }
        }

        else -> error("Unsupported data settings route: ${route.route}")
    }
}
