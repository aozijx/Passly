package com.aozijx.passly.features.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aozijx.passly.R
import com.aozijx.passly.core.backup.BackupExportStorageSupport
import com.aozijx.passly.core.common.EntryType
import com.aozijx.passly.core.designsystem.model.VaultCardStyle
import com.aozijx.passly.features.settings.internal.AppPasswordAction
import com.aozijx.passly.features.settings.internal.buildSettingsContentActions
import com.aozijx.passly.features.settings.internal.buildSettingsContentState
import com.aozijx.passly.features.settings.internal.buildSettingsDialogsActions
import com.aozijx.passly.features.settings.internal.buildSettingsDialogsState
import com.aozijx.passly.features.settings.internal.handleAppPasswordAction
import com.aozijx.passly.features.settings.internal.handleAppPasswordEntryClick
import com.aozijx.passly.features.settings.internal.handleBackupPathPicked
import com.aozijx.passly.features.settings.internal.handleInvalidateKeyToggle
import com.aozijx.passly.features.settings.internal.rememberSettingsScreenLocalState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel,
    onUpdateInteraction: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isAppPasswordEnabled by viewModel.authGateway.isAppPasswordEnabled.collectAsStateWithLifecycle()

    val availableCardStyles = remember { VaultCardStyle.styleConfig.perTypeStyles }
    val effectiveCardStyle = VaultCardStyle.normalizeGlobalStyle(uiState.cardStyle)
    val passwordSelectedStyle =
        uiState.cardStyleByEntryType[EntryType.PASSWORD.value] ?: VaultCardStyle.DEFAULT
    val totpSelectedStyle =
        uiState.cardStyleByEntryType[EntryType.TOTP.value] ?: VaultCardStyle.DEFAULT
    val context = LocalContext.current

    LaunchedEffect(uiState.cardStyle) {
        if (uiState.cardStyle != effectiveCardStyle) {
            viewModel.setCardStyle(effectiveCardStyle)
        }
    }

    LaunchedEffect(viewModel.backup.backupMessage) {
        viewModel.backup.backupMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.backup.clearBackupMessage()
        }
    }

    val localState = rememberSettingsScreenLocalState()

    val authDecryptTitle = stringResource(R.string.vault_auth_decrypt_title)
    val setAppPasswordSubtitle = stringResource(R.string.settings_auth_before_set_app_password)
    val authFailedMsg = stringResource(R.string.vault_auth_failed)

    fun submitAppPasswordAction(action: AppPasswordAction) {
        handleAppPasswordAction(
            context = context,
            action = action,
            currentPassword = localState.appPasswordCurrent,
            newPassword = localState.appPasswordNew,
            confirmPassword = localState.appPasswordConfirm,
            authGateway = viewModel.authGateway,
            onSuccess = localState::onAppPasswordSuccess
        )
    }

    val backupPathLabel = remember(uiState.backupDirectoryUri) {
        localState.backupPathLabel(uiState.backupDirectoryUri)
    }
    val lastExportFileLabel = remember(uiState.lastBackupExportFileName) {
        localState.lastExportFileLabel(uiState.lastBackupExportFileName)
    }

    val backupPathPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            handleBackupPathPicked(context, uri) { resolvedUri ->
                viewModel.setBackupDirectoryUri(resolvedUri)
            }
        }

    SettingsScreenContent(
        state = buildSettingsContentState(
            uiState = uiState,
            isAppPasswordEnabled = isAppPasswordEnabled,
            availableCardStyles = availableCardStyles,
            passwordSelectedStyle = passwordSelectedStyle,
            totpSelectedStyle = totpSelectedStyle,
            backupPathLabel = backupPathLabel,
            lastExportFileLabel = lastExportFileLabel
        ),
        actions = buildSettingsContentActions(
            uiState = uiState,
            localState = localState,
            onBack = onBack,
            viewModel = viewModel,
            onAppPasswordClick = {
                handleAppPasswordEntryClick(
                    context = context,
                    activity = context as? FragmentActivity,
                    isAppPasswordEnabled = isAppPasswordEnabled,
                    authGateway = viewModel.authGateway,
                    title = authDecryptTitle,
                    subtitle = setAppPasswordSubtitle,
                    authFailedMsg = authFailedMsg,
                    onAlreadyEnabled = localState::openAppPasswordActionDialog,
                    onVerified = localState::openSetAppPasswordDialog
                )
            },
            onInvalidateKeyOnBioChangeToggle = { enabled ->
                handleInvalidateKeyToggle(
                    context = context,
                    activity = context as? FragmentActivity,
                    enabled = enabled,
                    switchPolicy = viewModel::switchKeyInvalidationPolicy
                )
            },
            onPickBackupPath = {
                backupPathPicker.launch(BackupExportStorageSupport.defaultDocumentsTreeUri())
            },
            onTestBackupWrite = {
                viewModel.testBackupDirectoryWritePermission(uiState.backupDirectoryUri)
            }
        ),
        onUpdateInteraction = onUpdateInteraction
    )

    SettingsScreenDialogsHost(
        state = buildSettingsDialogsState(
            uiState = uiState,
            localState = localState,
            context = context
        ),
        actions = buildSettingsDialogsActions(
            localState = localState,
            viewModel = viewModel,
            submitAppPasswordAction = ::submitAppPasswordAction
        )
    )
}