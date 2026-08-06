package com.aozijx.passly.feature.settings.navigation

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.aozijx.passly.R
import com.aozijx.passly.core.util.PathDisplayFormatter
import com.aozijx.passly.feature.backup.BackupViewModel
import com.aozijx.passly.feature.backup.contract.BackupIntent
import com.aozijx.passly.feature.backup.model.BackupExportUiFormat
import com.aozijx.passly.feature.backup.storage.BackupExportStorageSupport
import com.aozijx.passly.feature.settings.SettingsViewModel
import com.aozijx.passly.feature.settings.contract.SettingsIntent
import com.aozijx.passly.feature.settings.contract.SettingsUiState
import com.aozijx.passly.feature.settings.datamanagement.BackupRestoreDetail
import com.aozijx.passly.feature.settings.datamanagement.BackupRestoreSheetHost
import com.aozijx.passly.feature.settings.datamanagement.BackupSheet
import com.aozijx.passly.feature.settings.datamanagement.DataManagementDetail
import com.aozijx.passly.feature.settings.datamanagement.DataManagementSettingsAction
import com.aozijx.passly.feature.settings.datamanagement.DataManagementSettingsViewModel
import com.aozijx.passly.feature.settings.datamanagement.handleBackupPathPicked
import com.aozijx.passly.feature.settings.general.GeneralDetail
import com.aozijx.passly.feature.settings.general.NotificationDetail
import com.aozijx.passly.feature.settings.interaction.InteractionDetail
import com.aozijx.passly.feature.settings.interaction.InteractionSettingsAction
import com.aozijx.passly.feature.settings.interaction.InteractionSettingsViewModel
import com.aozijx.passly.feature.settings.internal.SettingsGroup
import com.aozijx.passly.feature.settings.security.RecoveryDraftState
import com.aozijx.passly.feature.settings.security.RecoveryDraftViewModel
import com.aozijx.passly.feature.settings.security.SecuritySettingsAction
import com.aozijx.passly.feature.settings.security.SecuritySettingsViewModel
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
    interactionViewModel: InteractionSettingsViewModel,
    dataViewModel: DataManagementSettingsViewModel,
    backupViewModel: BackupViewModel,
    settingsViewModel: SettingsViewModel,
    settingsState: SettingsUiState
) {
    composable(SettingsRoute.Interaction.route) {
        val state by interactionViewModel.config.collectAsStateWithLifecycle()
        SettingsSecondaryPage(
            title = stringResource(SettingsGroup.INTERACTION.titleRes),
            onBack = { navController.popBackStack() }
        ) {
            item {
                InteractionDetail(
                    state = state,
                    onSwipeEnabledChange = {
                        interactionViewModel.onAction(InteractionSettingsAction.SetSwipeEnabled(it))
                    },
                    onLeftSwipeActionClick = localState::openLeftActionDialog,
                    onRightSwipeActionClick = localState::openRightActionDialog,
                    onAutofillAction = interactionViewModel::onAction,
                    onOpenAutofillSettings = {
                        interactionViewModel.openAutofillSettings()
                    }
                )
            }
        }
    }

    composable(SettingsRoute.DataManagement.route) {
        val state by dataViewModel.config.collectAsStateWithLifecycle()
        SettingsSecondaryPage(
            title = stringResource(SettingsGroup.DATA_MANAGEMENT.titleRes),
            onBack = { navController.popBackStack() }
        ) {
            item {
                DataManagementDetail(
                    state = state,
                    isClearingDatabase = settingsState.isClearingDatabase,
                    onAutoDownloadIconsChange = {
                        dataViewModel.onAction(DataManagementSettingsAction.SetAutoDownloadIcons(it))
                    },
                    onRestoreTrashEntry = { entryId, expectedVersion ->
                        dataViewModel.onAction(
                            DataManagementSettingsAction.RestoreTrashEntry(
                                entryId,
                                expectedVersion
                            )
                        )
                    },
                    onDeleteTrashEntry = { entryId, expectedVersion ->
                        dataViewModel.onAction(
                            DataManagementSettingsAction.DeleteTrashEntry(
                                entryId,
                                expectedVersion
                            )
                        )
                    },
                    onEmptyTrash = {
                        dataViewModel.onAction(DataManagementSettingsAction.EmptyTrash)
                    },
                    onClearTrashError = {
                        dataViewModel.onAction(DataManagementSettingsAction.ClearTrashError)
                    },
                    onClearDatabase = {
                        settingsViewModel.handleIntent(SettingsIntent.ClearDatabase)
                    }
                )
            }
        }
    }

    composable(SettingsRoute.BackupRestore.route) {
        val state by dataViewModel.config.collectAsStateWithLifecycle()
        val backupState by backupViewModel.uiState.collectAsStateWithLifecycle()
        val notSetText = stringResource(R.string.not_set)
        val pathLabel = remember(state.directoryUri) {
            PathDisplayFormatter.format(state.directoryUri) ?: notSetText
        }
        var activeSheet by remember { mutableStateOf<BackupSheet?>(null) }

        fun startManualExport(uri: Uri?) {
            if (uri == null) {
                backupViewModel.onIntent(BackupIntent.CancelPendingOperation)
                return
            }
            backupViewModel.onIntent(
                BackupIntent.StartExport(
                    uri = uri,
                    fileNameHint = backupViewModel.buildExportFileName()
                )
            )
            backupViewModel.onIntent(BackupIntent.ProcessBackupAction)
        }

        val encryptedExportPicker = rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/octet-stream"),
            ::startManualExport
        )
        val jsonExportPicker = rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/json"),
            ::startManualExport
        )
        val textExportPicker = rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("text/plain"),
            ::startManualExport
        )
        val importPicker = rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            if (uri != null) {
                backupViewModel.onIntent(BackupIntent.StartImport(uri))
                activeSheet = BackupSheet.IMPORT_OPTIONS
            }
        }
        val backupPathPicker = rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocumentTree()
        ) { uri ->
            handleBackupPathPicked(context, uri) { resolvedUri ->
                dataViewModel.onAction(
                    DataManagementSettingsAction.SetBackupDirectoryUri(resolvedUri)
                )
            }
        }

        DisposableEffect(backupViewModel) {
            onDispose {
                backupViewModel.onIntent(BackupIntent.CancelPendingOperation)
            }
        }

        SettingsSecondaryPage(
            title = stringResource(SettingsGroup.BACKUP_RESTORE.titleRes),
            onBack = { navController.popBackStack() }
        ) {
            item {
                BackupRestoreDetail(
                    backupPathLabel = pathLabel,
                    lastExportFileLabel = notSetText,
                    onExport = { activeSheet = BackupSheet.FORMAT_PICKER },
                    onImport = {
                        importPicker.launch(
                            arrayOf(
                                "application/octet-stream",
                                "application/json",
                                "text/json",
                                "*/*"
                            )
                        )
                    },
                    onPickBackupPath = {
                        backupPathPicker.launch(
                            BackupExportStorageSupport.defaultDocumentsTreeUri()
                        )
                    },
                    onTestBackupWrite = {
                        backupViewModel.onIntent(
                            BackupIntent.CheckDirectoryPermission(state.directoryUri)
                        )
                    },
                    onClearBackupPath = if (state.directoryUri.isNullOrBlank()) null
                    else localState::openClearBackupDirConfirmDialog
                )
            }
        }

        BackupRestoreSheetHost(
            sheet = activeSheet,
            state = backupState,
            configuredDirectoryLabel = pathLabel.takeIf { !state.directoryUri.isNullOrBlank() },
            onDismiss = {
                activeSheet = null
                backupViewModel.onIntent(BackupIntent.CancelPendingOperation)
            },
            onFormatSelected = { format ->
                backupViewModel.onIntent(BackupIntent.PrepareExport(format))
                activeSheet = BackupSheet.EXPORT_OPTIONS
            },
            onPasswordChange = {
                backupViewModel.onIntent(BackupIntent.UpdatePassword(it))
            },
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
            onImportModeChange = {
                backupViewModel.onIntent(BackupIntent.UpdateImportMode(it))
            },
            onExport = {
                activeSheet = null
                val directoryUri = state.directoryUri
                if (!directoryUri.isNullOrBlank()) {
                    backupViewModel.onIntent(BackupIntent.StartExportInConfiguredDirectory)
                } else {
                    val fileName = backupViewModel.buildExportFileName()
                    when (backupState.selectedExportFormat) {
                        BackupExportUiFormat.ENCRYPTED -> encryptedExportPicker.launch(fileName)
                        BackupExportUiFormat.JSON -> jsonExportPicker.launch(fileName)
                        BackupExportUiFormat.TEXT -> textExportPicker.launch(fileName)
                    }
                }
            },
            onImport = {
                activeSheet = null
                backupViewModel.onIntent(BackupIntent.ProcessBackupAction)
            }
        )
    }

    composable(SettingsRoute.RecoveryCode.route) {
        val viewModel: SecuritySettingsViewModel = hiltViewModel()
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

        SettingsSecondaryPage(
            title = stringResource(SettingsGroup.RECOVERY_CODE.titleRes),
            onBack = { navController.popBackStack() }
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
                    hasRecoveryEnvelope = hasEnvelope || draftState is RecoveryDraftState.Committed,
                    verifyResult = verifyResult,
                    onCreateRecoveryCode = {
                        draftViewModel.generate()
                    },
                    onRegenerate = {
                        draftViewModel.generate()
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

    composable(SettingsRoute.General.route) {
        SettingsSecondaryPage(
            title = stringResource(SettingsGroup.GENERAL.titleRes),
            onBack = { navController.popBackStack() }
        ) {
            item { GeneralDetail() }
        }
    }

    composable(SettingsRoute.Notifications.route) {
        SettingsSecondaryPage(
            title = stringResource(SettingsGroup.NOTIFICATIONS.titleRes),
            onBack = { navController.popBackStack() }
        ) {
            item { NotificationDetail() }
        }
    }
}
