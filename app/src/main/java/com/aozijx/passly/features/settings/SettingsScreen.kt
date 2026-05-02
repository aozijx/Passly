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
import com.aozijx.passly.features.backup.ui.BackupPathSettingsConfig
import com.aozijx.passly.features.settings.internal.AppPasswordAction
import com.aozijx.passly.features.settings.internal.handleAppPasswordAction
import com.aozijx.passly.features.settings.internal.handleBackupPathPicked
import com.aozijx.passly.features.settings.internal.rememberSettingsScreenLocalState
import com.aozijx.passly.features.settings.internal.verifyBeforeSetAppPassword

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel
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

    fun handleAppPasswordEntryClick() {
        if (isAppPasswordEnabled) {
            localState.showAppPasswordActionDialog = true
            return
        }
        verifyBeforeSetAppPassword(
            context = context,
            activity = context as? FragmentActivity,
            authGateway = viewModel.authGateway,
            title = authDecryptTitle,
            subtitle = setAppPasswordSubtitle,
            authFailedMsg = authFailedMsg,
            onVerified = { localState.showSetAppPasswordDialog = true }
        )
    }

    fun handleInvalidateKeyToggle(enabled: Boolean) {
        val activity = context as? FragmentActivity ?: return
        viewModel.switchKeyInvalidationPolicy(activity, enabled) { result ->
            result.onFailure { e ->
                Toast.makeText(context, "切换失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val backupPathLabel = remember(uiState.backupDirectoryUri) {
        BackupPathSettingsConfig.displayValue(uiState.backupDirectoryUri)
    }
    val lastExportFileLabel = remember(uiState.lastBackupExportFileName) {
        BackupPathSettingsConfig.displayRecentFileName(uiState.lastBackupExportFileName)
    }

    val backupPathPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            handleBackupPathPicked(context, uri) { resolvedUri ->
                viewModel.setBackupDirectoryUri(resolvedUri)
            }
        }

    SettingsScreenContent(
        state = SettingsContentState(
            lockTimeout = uiState.lockTimeout,
            isAppPasswordEnabled = isAppPasswordEnabled,
            isPasswordPreferredAuthFirst = uiState.isPasswordPreferredAuthFirst,
            isInvalidateKeyOnBioChange = uiState.isInvalidateKeyOnBioChange,
            isSecureContentEnabled = uiState.isSecureContentEnabled,
            isFlipToLockEnabled = uiState.isFlipToLockEnabled,
            isFlipExitAndClearStackEnabled = uiState.isFlipExitAndClearStackEnabled,
            isStatusBarAutoHide = uiState.isStatusBarAutoHide,
            isTopBarCollapsible = uiState.isTopBarCollapsible,
            isTabBarCollapsible = uiState.isTabBarCollapsible,
            isSwipeEnabled = uiState.isSwipeEnabled,
            swipeLeftAction = uiState.swipeLeftAction,
            swipeRightAction = uiState.swipeRightAction,
            autofillUiMode = uiState.autofillUiMode,
            visibleVaultTabs = uiState.visibleVaultTabs,
            isAutoDownloadIcons = uiState.isAutoDownloadIcons,
            availableCardStyles = availableCardStyles,
            passwordSelectedStyle = passwordSelectedStyle,
            totpSelectedStyle = totpSelectedStyle,
            backupPathLabel = backupPathLabel,
            lastExportFileLabel = lastExportFileLabel
        ),
        actions = SettingsContentActions(
            onBack = onBack,
            onShowLockTimeoutDialog = { localState.showLockTimeoutDialog = true },
            onAppPasswordClick = ::handleAppPasswordEntryClick,
            onPasswordPreferredAuthFirstChange = viewModel::setPasswordPreferredAuthFirst,
            onInvalidateKeyOnBioChangeToggle = ::handleInvalidateKeyToggle,
            onSecureContentEnabledChange = viewModel::setSecureContentEnabled,
            onFlipToLockEnabledChange = viewModel::setFlipToLockEnabled,
            onFlipExitAndClearStackEnabledChange = viewModel::setFlipExitAndClearStackEnabled,
            onStatusBarAutoHideChange = viewModel::setStatusBarAutoHide,
            onTopBarCollapsibleChange = viewModel::setTopBarCollapsible,
            onTabBarCollapsibleChange = viewModel::setTabBarCollapsible,
            onSwipeEnabledChange = viewModel::setSwipeEnabled,
            onLeftSwipeActionClick = { localState.showLeftActionDialog = true },
            onRightSwipeActionClick = { localState.showRightActionDialog = true },
            onToggleAutofillUiMode = { viewModel.toggleAutofillUiMode(uiState.autofillUiMode) },
            onVisibleVaultTabsChange = viewModel::setVisibleVaultTabs,
            onAutoDownloadIconsChange = viewModel::setAutoDownloadIcons,
            onPickBackupPath = { backupPathPicker.launch(BackupExportStorageSupport.defaultDocumentsTreeUri()) },
            onTestBackupWrite = {
                viewModel.testBackupDirectoryWritePermission(uiState.backupDirectoryUri)
            },
            onClearBackupPath =
                if (uiState.backupDirectoryUri.isNullOrBlank()) null
                else {
                    { localState.showClearBackupDirConfirmDialog = true }
                },
            onPasswordStyleSelected = {
                viewModel.setCardStyleForEntryType(
                    EntryType.PASSWORD.value,
                    it
                )
            },
            onTotpStyleSelected = { viewModel.setCardStyleForEntryType(EntryType.TOTP.value, it) }
        )
    )

    SettingsScreenDialogsHost(
        state = SettingsDialogsState(
            showRightActionDialog = localState.showRightActionDialog,
            showLeftActionDialog = localState.showLeftActionDialog,
            showLockTimeoutDialog = localState.showLockTimeoutDialog,
            showClearBackupDirConfirmDialog = localState.showClearBackupDirConfirmDialog,
            showAppPasswordActionDialog = localState.showAppPasswordActionDialog,
            showSetAppPasswordDialog = localState.showSetAppPasswordDialog,
            showChangeAppPasswordDialog = localState.showChangeAppPasswordDialog,
            showDisableAppPasswordDialog = localState.showDisableAppPasswordDialog,
            swipeLeftAction = uiState.swipeLeftAction,
            swipeRightAction = uiState.swipeRightAction,
            lockTimeout = uiState.lockTimeout,
            backupDirectoryUri = uiState.backupDirectoryUri,
            context = context,
            appPasswordCurrent = localState.appPasswordCurrent,
            appPasswordNew = localState.appPasswordNew,
            appPasswordConfirm = localState.appPasswordConfirm
        ),
        actions = SettingsDialogsActions(
            onSetSwipeRightAction = viewModel::setSwipeRightAction,
            onSetSwipeLeftAction = viewModel::setSwipeLeftAction,
            onSetLockTimeout = viewModel::setLockTimeout,
            onClearBackupDirectory = viewModel::clearBackupDirectoryUri,
            onDismissRightActionDialog = { localState.showRightActionDialog = false },
            onDismissLeftActionDialog = { localState.showLeftActionDialog = false },
            onDismissLockTimeoutDialog = { localState.showLockTimeoutDialog = false },
            onDismissClearBackupDirConfirmDialog = {
                localState.showClearBackupDirConfirmDialog = false
            },
            onDismissAppPasswordActionDialog = { localState.showAppPasswordActionDialog = false },
            onShowChangeAppPasswordDialog = { localState.showChangeAppPasswordDialog = true },
            onShowDisableAppPasswordDialog = { localState.showDisableAppPasswordDialog = true },
            onDismissSetAppPasswordDialog = {
                localState.showSetAppPasswordDialog = false
                localState.clearAppPasswordInputs()
            },
            onDismissChangeAppPasswordDialog = {
                localState.showChangeAppPasswordDialog = false
                localState.clearAppPasswordInputs()
            },
            onDismissDisableAppPasswordDialog = {
                localState.showDisableAppPasswordDialog = false
                localState.clearAppPasswordInputs()
            },
            onAppPasswordCurrentChange = { localState.appPasswordCurrent = it },
            onAppPasswordNewChange = { localState.appPasswordNew = it },
            onAppPasswordConfirmChange = { localState.appPasswordConfirm = it },
            onConfirmSetAppPassword = { submitAppPasswordAction(AppPasswordAction.SET) },
            onConfirmChangeAppPassword = { submitAppPasswordAction(AppPasswordAction.CHANGE) },
            onConfirmDisableAppPassword = { submitAppPasswordAction(AppPasswordAction.DISABLE) }
        )
    )
}