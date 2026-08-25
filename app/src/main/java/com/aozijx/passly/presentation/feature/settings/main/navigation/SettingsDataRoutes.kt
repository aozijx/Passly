package com.aozijx.passly.presentation.feature.settings.main.navigation

import android.content.Context
import android.os.Build
import android.widget.Toast
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
import com.aozijx.passly.presentation.feature.backup.BackupSettingsFeature
import com.aozijx.passly.feature.backup.internal.archive.platform.BackupStorageSupport
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.presentation.feature.settings.main.SettingsViewModel
import com.aozijx.passly.presentation.feature.settings.autofill.AutofillSettingsAction
import com.aozijx.passly.presentation.feature.settings.autofill.AutofillSettingsViewModel
import com.aozijx.passly.presentation.feature.settings.autofill.toAutofillSettingsUiModel
import com.aozijx.passly.presentation.feature.settings.autofill.toDomainModel
import com.aozijx.passly.presentation.feature.settings.main.SettingsUiAction
import com.aozijx.passly.presentation.feature.settings.main.SettingsUiState
import com.aozijx.passly.presentation.feature.settings.backup.DataManagementSettingsUiAction
import com.aozijx.passly.presentation.feature.settings.backup.DataManagementSettingsViewModel
import com.aozijx.passly.presentation.feature.settings.backup.DatabaseRecoveryViewModel
import com.aozijx.passly.presentation.feature.settings.backup.DatabaseRecoveryUiAction
import com.aozijx.passly.presentation.feature.settings.backup.toDetailState
import com.aozijx.passly.presentation.feature.settings.backup.toSheetState
import com.aozijx.passly.presentation.feature.settings.backup.handleBackupPathPicked
import com.aozijx.passly.presentation.feature.settings.main.interaction.InteractionSettingsAction
import com.aozijx.passly.presentation.feature.settings.main.interaction.InteractionSettingsViewModel
import com.aozijx.passly.presentation.feature.settings.security.RecoveryDraftAction
import com.aozijx.passly.presentation.feature.settings.security.RecoveryDraftState
import com.aozijx.passly.presentation.feature.settings.security.RecoveryDraftViewModel
import com.aozijx.passly.presentation.feature.settings.security.SecuritySettingsAction
import com.aozijx.passly.presentation.feature.settings.security.SecuritySettingsViewModel
import com.aozijx.passly.presentation.feature.settings.security.messageOrNull
import com.aozijx.passly.presentation.ui.settings.autofill.AutofillDetail
import com.aozijx.passly.presentation.ui.settings.backup.DataManagementDetail
import com.aozijx.passly.presentation.feature.settings.main.general.GeneralDetail
import com.aozijx.passly.presentation.feature.settings.main.general.NotificationDetail
import com.aozijx.passly.presentation.ui.settings.interaction.InteractionDetail
import com.aozijx.passly.presentation.feature.settings.main.interaction.toUiModel
import com.aozijx.passly.presentation.ui.settings.main.component.SettingsGroup
import com.aozijx.passly.presentation.ui.settings.security.RecoveryCodeDetail
import com.aozijx.passly.presentation.ui.settings.security.RecoveryCodeSheet
import com.aozijx.passly.presentation.ui.settings.main.SettingsScreenLocalState
import com.aozijx.passly.presentation.ui.settings.main.SettingsSecondaryPage

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
                        state = state.toUiModel(),
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
                        state = state.toAutofillSettingsUiModel(
                            supportsCredentialManager =
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
                        ),
                        onOpenAutofillSettings = {
                            viewModel.onAction(
                                AutofillSettingsAction.OpenSystemAutofillSettings
                            )
                        },
                        onEnabledChange = {
                            viewModel.onAction(AutofillSettingsAction.SetEnabled(it))
                        },
                        onPresentationChange = {
                            viewModel.onAction(
                                AutofillSettingsAction.SetPresentation(it.toDomainModel())
                            )
                        },
                        onCredentialManagerEnabledChange = {
                            viewModel.onAction(
                                AutofillSettingsAction.SetCredentialManagerEnabled(it)
                            )
                        },
                        onAuthenticationRequiredChange = {
                            viewModel.onAction(
                                AutofillSettingsAction.SetAuthenticationRequired(it)
                            )
                        },
                        onOtpEnabledChange = {
                            viewModel.onAction(AutofillSettingsAction.SetOtpEnabled(it))
                        },
                        onSavePromptsEnabledChange = {
                            viewModel.onAction(
                                AutofillSettingsAction.SetSavePromptsEnabled(it)
                            )
                        },
                        onUnmatchedSuggestionsEnabledChange = {
                            viewModel.onAction(
                                AutofillSettingsAction.SetUnmatchedSuggestionsEnabled(it)
                            )
                        },
                        onMaxSuggestionsChange = {
                            viewModel.onAction(AutofillSettingsAction.SetMaxSuggestions(it))
                        },
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
                        state = state.toDetailState(),
                        recoveryState = recoveryState.toSheetState(),
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
                        onRefreshRecoveryPackages = {
                            recoveryViewModel.onAction(DatabaseRecoveryUiAction.RefreshRecoveryPackages)
                        },
                        onClearRecoveryResult = {
                            recoveryViewModel.onAction(DatabaseRecoveryUiAction.ClearRecoveryResult)
                        },
                        onScanRecoveryPackage = {
                            recoveryViewModel.onAction(DatabaseRecoveryUiAction.ScanRecoveryPackage(it))
                        },
                        onRestoreRecoveryPackage = {
                            recoveryViewModel.onAction(DatabaseRecoveryUiAction.RestoreRecoveryPackage(it))
                        },
                        onToggleRecoveryType = {
                            recoveryViewModel.onAction(
                                DatabaseRecoveryUiAction.ToggleRecoveryType(EntryType.valueOf(it))
                            )
                        },
                        onDeleteRecoveryPackage = {
                            recoveryViewModel.onAction(DatabaseRecoveryUiAction.DeleteRecoveryPackage(it))
                        },
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
                    val copySuccessMessage = stringResource(
                        R.string.field_copy_success_message,
                        stringResource(R.string.recovery_code_label),
                    )
                    RecoveryCodeSheet(
                        recoveryCode = code,
                        sheetState = localState.recoveryCodeSheetState,
                        onCopy = {
                            settingsViewModel.copySensitive(code)
                            Toast.makeText(
                                context,
                                copySuccessMessage,
                                Toast.LENGTH_SHORT,
                            ).show()
                        },
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
