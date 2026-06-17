package com.aozijx.passly.ui.features.settings

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
import com.aozijx.passly.domain.config.AppDefaults
import com.aozijx.passly.domain.config.UserConfigProvider
import com.aozijx.passly.domain.model.EntryType
import com.aozijx.passly.domain.model.VaultCardStyle
import com.aozijx.passly.ui.features.settings.apppassword.AppPasswordAction
import com.aozijx.passly.ui.features.settings.apppassword.handleAppPasswordAction
import com.aozijx.passly.ui.features.settings.apppassword.handleAppPasswordEntryClick
import com.aozijx.passly.ui.features.settings.data.handleBackupPathPicked
import com.aozijx.passly.ui.features.settings.security.handleInvalidateKeyToggle
import com.aozijx.passly.ui.features.settings.shell.SettingsScreenContent
import com.aozijx.passly.ui.features.settings.shell.SettingsScreenDialogsHost
import com.aozijx.passly.ui.features.settings.shell.buildSettingsContentActions
import com.aozijx.passly.ui.features.settings.shell.buildSettingsContentState
import com.aozijx.passly.ui.features.settings.shell.buildSettingsDialogsActions
import com.aozijx.passly.ui.features.settings.shell.buildSettingsDialogsState
import com.aozijx.passly.ui.features.settings.shell.rememberSettingsScreenLocalState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    configProvider: UserConfigProvider,
    authViewModel: SettingsViewModel,
    onUpdateInteraction: () -> Unit = {}
) {
    val uiState by configProvider.config.collectAsStateWithLifecycle()
    val isAppPasswordEnabled by authViewModel.authGateway.isAppPasswordEnabled.collectAsStateWithLifecycle()

    val availableCardStyles = remember { AppDefaults.CardStyle.PER_TYPE_STYLES }
    val effectiveCardStyle = AppDefaults.CardStyle.normalizeGlobalStyle(uiState.display.cardStyle)
    val passwordSelectedStyle =
        uiState.display.perTypeMap[EntryType.PASSWORD.value] ?: VaultCardStyle.DEFAULT
    val totpSelectedStyle =
        uiState.display.perTypeMap[EntryType.TOTP.value] ?: VaultCardStyle.DEFAULT
    val context = LocalContext.current

    LaunchedEffect(uiState.display.cardStyle) {
        if (uiState.display.cardStyle != effectiveCardStyle)
            configProvider.setCardStyle(effectiveCardStyle)
    }

    LaunchedEffect(configProvider.backup.backupMessage) {
        configProvider.backup.backupMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            configProvider.backup.clearBackupMessage()
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
            authGateway = authViewModel.authGateway,
            onSuccess = localState::onAppPasswordSuccess
        )
    }

    val backupPathLabel = remember(uiState.backup.directoryUri) {
        localState.backupPathLabel(uiState.backup.directoryUri)
    }
    val lastExportFileLabel = remember(uiState.backup.lastExportFileName) {
        localState.lastExportFileLabel(uiState.backup.lastExportFileName)
    }

    val backupPathPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            handleBackupPathPicked(context, uri) { resolvedUri ->
                configProvider.setBackupDirectoryUri(resolvedUri)
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
            configProvider = configProvider,
            onAppPasswordClick = {
                handleAppPasswordEntryClick(
                    context = context,
                    activity = context as? FragmentActivity,
                    isAppPasswordEnabled = isAppPasswordEnabled,
                    authGateway = authViewModel.authGateway,
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
                    switchPolicy = authViewModel::switchKeyInvalidationPolicy
                )
            },
            onPickBackupPath = {
                backupPathPicker.launch(BackupExportStorageSupport.defaultDocumentsTreeUri())
            },
            onTestBackupWrite = {
                configProvider.testBackupDirectoryWritePermission(uiState.backup.directoryUri)
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
            configProvider = configProvider,
            submitAppPasswordAction = ::submitAppPasswordAction
        )
    )
}