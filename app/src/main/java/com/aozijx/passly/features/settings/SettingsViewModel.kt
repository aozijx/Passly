package com.aozijx.passly.features.settings

import android.app.Application
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.common.AutofillUiMode
import com.aozijx.passly.core.common.SwipeActionType
import com.aozijx.passly.core.designsystem.model.VaultCardStyle
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.domain.usecase.auth.AuthUseCases
import com.aozijx.passly.domain.usecase.backup.BackupUseCases
import com.aozijx.passly.domain.usecase.settings.backup.BackupSettingsUseCases
import com.aozijx.passly.domain.usecase.settings.security.SecuritySettingsUseCases
import com.aozijx.passly.domain.usecase.settings.system.SystemSettingsUseCases
import com.aozijx.passly.features.backup.BackupCoordinator
import com.aozijx.passly.features.settings.contract.SettingsUiState
import com.aozijx.passly.features.settings.internal.SettingsStateCoordinator
import com.aozijx.passly.features.verification.VerificationCoordinator
import com.aozijx.passly.features.verification.VerificationGateway
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    application: Application,
    private val systemSettingsUseCases: SystemSettingsUseCases,
    private val securitySettingsUseCases: SecuritySettingsUseCases,
    private val backupSettingsUseCases: BackupSettingsUseCases,
    private val backupUseCases: BackupUseCases,
    authUseCases: AuthUseCases
) : AndroidViewModel(application) {

    private val stateCoordinator = SettingsStateCoordinator(
        scope = viewModelScope,
        systemSettingsUseCases = systemSettingsUseCases,
        securitySettingsUseCases = securitySettingsUseCases,
        backupSettingsUseCases = backupSettingsUseCases
    )

    val uiState: StateFlow<SettingsUiState> = stateCoordinator.uiState

    val backup = BackupCoordinator(
        scope = viewModelScope,
        backupSettingsUseCases = backupSettingsUseCases,
        backupUseCases = backupUseCases,
        application = application
    )

    private val authCoordinator = VerificationCoordinator(
        scope = viewModelScope, authUseCases = authUseCases
    )
    val authGateway: VerificationGateway = authCoordinator

    fun switchKeyInvalidationPolicy(
        activity: FragmentActivity,
        invalidateOnBiometricChange: Boolean,
        onResult: (AppResult<Unit>) -> Unit
    ) {
        authCoordinator.rekeyWithInvalidationPolicy(
            activity, invalidateOnBiometricChange
        ) { result ->
            if (result.isSuccess) {
                viewModelScope.launch {
                    securitySettingsUseCases.setInvalidateKeyOnBioChange(invalidateOnBiometricChange)
                }
            }
            onResult(result)
        }
    }

    fun setStatusBarAutoHide(autoHide: Boolean) =
        viewModelScope.launch { systemSettingsUseCases.setStatusBarAutoHide(autoHide) }

    fun setTopBarCollapsible(collapsible: Boolean) =
        viewModelScope.launch { systemSettingsUseCases.setTopBarCollapsible(collapsible) }

    fun setTabBarCollapsible(collapsible: Boolean) =
        viewModelScope.launch { systemSettingsUseCases.setTabBarCollapsible(collapsible) }

    fun setSecureContentEnabled(enabled: Boolean) =
        viewModelScope.launch { securitySettingsUseCases.setSecureContentEnabled(enabled) }

    fun setFlipToLockEnabled(enabled: Boolean) =
        viewModelScope.launch { securitySettingsUseCases.setFlipToLockEnabled(enabled) }

    fun setFlipExitAndClearStackEnabled(enabled: Boolean) =
        viewModelScope.launch { securitySettingsUseCases.setFlipExitAndClearStackEnabled(enabled) }

    fun setPasswordPreferredAuthFirst(enabled: Boolean) =
        viewModelScope.launch { securitySettingsUseCases.setPasswordPreferredAuthFirst(enabled) }

    fun setDeviceCredentialFallbackEnabled(enabled: Boolean) =
        viewModelScope.launch { securitySettingsUseCases.setDeviceCredentialFallbackEnabled(enabled) }

    fun setLockTimeout(timeoutMs: Long) = viewModelScope.launch {
        securitySettingsUseCases.setLockTimeout(
            timeoutMs.coerceAtLeast(5000L)
        )
    }

    fun setCardStyle(style: VaultCardStyle) =
        viewModelScope.launch { systemSettingsUseCases.setCardStyle(style) }

    fun setCardStyleForEntryType(entryTypeValue: Int, style: VaultCardStyle) =
        viewModelScope.launch {
            systemSettingsUseCases.setCardStyleForEntryType(entryTypeValue, style)
        }

    fun toggleAutofillUiMode(currentMode: AutofillUiMode) = viewModelScope.launch {
        val nextMode = when (currentMode) {
            AutofillUiMode.SYSTEM_INLINE -> AutofillUiMode.BOTTOM_SHEET
            AutofillUiMode.BOTTOM_SHEET -> AutofillUiMode.SYSTEM_INLINE
        }
        systemSettingsUseCases.setAutofillUiMode(nextMode)
    }

    fun setSwipeEnabled(enabled: Boolean) =
        viewModelScope.launch { systemSettingsUseCases.setSwipeEnabled(enabled) }

    fun setSwipeLeftAction(action: SwipeActionType) =
        viewModelScope.launch { systemSettingsUseCases.setSwipeLeftAction(action) }

    fun setSwipeRightAction(action: SwipeActionType) =
        viewModelScope.launch { systemSettingsUseCases.setSwipeRightAction(action) }

    fun setVisibleVaultTabs(keys: Set<String>) =
        viewModelScope.launch { systemSettingsUseCases.setVisibleVaultTabs(keys) }

    fun setTabBarMaxTabsWithoutScroll(maxTabs: Int) =
        viewModelScope.launch { systemSettingsUseCases.setTabBarMaxTabsWithoutScroll(maxTabs) }

    fun setAutoDownloadIcons(enabled: Boolean) =
        viewModelScope.launch { systemSettingsUseCases.setAutoDownloadIcons(enabled) }

    fun setBackupDirectoryUri(uri: String) = backup.setBackupDirectoryUri(uri)

    fun clearBackupDirectoryUri() = backup.clearBackupDirectoryUri()

    fun testBackupDirectoryWritePermission(directoryUri: String?) =
        backup.testBackupDirectoryWritePermission(directoryUri)
}