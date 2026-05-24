package com.aozijx.passly.domain.config

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.domain.config.UserConfig.Vault.AutofillUiMode
import com.aozijx.passly.domain.config.UserConfig.Vault.SwipeActionType
import com.aozijx.passly.domain.model.VaultCardStyle
import com.aozijx.passly.domain.usecase.backup.BackupUseCases
import com.aozijx.passly.domain.usecase.settings.backup.BackupSettingsUseCases
import com.aozijx.passly.domain.usecase.settings.security.SecuritySettingsUseCases
import com.aozijx.passly.domain.usecase.settings.system.SystemSettingsUseCases
import com.aozijx.passly.features.backup.BackupCoordinator
import com.aozijx.passly.features.settings.state.SettingsUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UserConfigProvider(
    private val systemSettingsUseCases: SystemSettingsUseCases,
    private val securitySettingsUseCases: SecuritySettingsUseCases,
    private val backupSettingsUseCases: BackupSettingsUseCases,
    backupUseCases: BackupUseCases,
    application: Application
) : ViewModel() {

    val backup = BackupCoordinator(viewModelScope, backupSettingsUseCases, backupUseCases, application)

    val config: StateFlow<SettingsUiState> = combine(
        securitySettingsUseCases.lockTimeout,
        securitySettingsUseCases.isInvalidateKeyOnBioChange,
        securitySettingsUseCases.isSecureContentEnabled,
        securitySettingsUseCases.isPasswordPreferredAuthFirst,
        securitySettingsUseCases.isDeviceCredentialFallbackEnabled
    ) { lt, ibc, sec, pfa, dcf ->
        SettingsUiState(security = UserConfig.Security(
            lockTimeout = lt, isInvalidateKeyOnBioChange = ibc,
            isSecureContentEnabled = sec, isPasswordPreferredAuthFirst = pfa,
            isDeviceCredentialFallbackEnabled = dcf,
        ))
    }
    .combine(securitySettingsUseCases.isFlipToLockEnabled) { st, v ->
        st.copy(security = st.security.copy(isFlipToLockEnabled = v))
    }
    .combine(securitySettingsUseCases.isFlipExitAndClearStackEnabled) { st, v ->
        st.copy(security = st.security.copy(isFlipExitAndClearStackEnabled = v))
    }
    .combine(systemSettingsUseCases.isStatusBarAutoHide) { st, v ->
        st.copy(display = st.display.copy(isStatusBarAutoHide = v))
    }
    .combine(systemSettingsUseCases.isTopBarCollapsible) { st, v ->
        st.copy(display = st.display.copy(isTopBarCollapsible = v))
    }
    .combine(systemSettingsUseCases.isTabBarCollapsible) { st, v ->
        st.copy(display = st.display.copy(isTabBarCollapsible = v))
    }
    .combine(systemSettingsUseCases.cardStyle) { st, v ->
        st.copy(display = st.display.copy(cardStyle = v))
    }
    .combine(systemSettingsUseCases.cardStyleByEntryType) { st, v ->
        st.copy(display = st.display.copy(perTypeMap = v))
    }
    .combine(systemSettingsUseCases.isAutoDownloadIcons) { st, v ->
        st.copy(display = st.display.copy(isAutoDownloadIcons = v))
    }
    .combine(systemSettingsUseCases.autofillUiMode) { st, v ->
        st.copy(vault = st.vault.copy(autofillUiMode = v))
    }
    .combine(systemSettingsUseCases.isSwipeEnabled) { st, v ->
        st.copy(vault = st.vault.copy(isSwipeEnabled = v))
    }
    .combine(systemSettingsUseCases.swipeLeftAction) { st, v ->
        st.copy(vault = st.vault.copy(swipeLeftAction = v))
    }
    .combine(systemSettingsUseCases.swipeRightAction) { st, v ->
        st.copy(vault = st.vault.copy(swipeRightAction = v))
    }
    .combine(systemSettingsUseCases.visibleVaultTabs) { st, v ->
        st.copy(vault = st.vault.copy(visibleVaultTabs = v))
    }
    .combine(systemSettingsUseCases.tabBarMaxTabsWithoutScroll) { st, v ->
        st.copy(vault = st.vault.copy(tabBarMaxTabsWithoutScroll = v))
    }
    .combine(backupSettingsUseCases.backupDirectoryUri) { st, v ->
        st.copy(backup = st.backup.copy(directoryUri = v))
    }
    .combine(backupSettingsUseCases.lastBackupExportFileName) { st, v ->
        st.copy(backup = st.backup.copy(lastExportFileName = v))
    }
    .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000L),
        SettingsUiState()
    )

    fun setLockTimeout(timeoutMs: Long) {
        viewModelScope.launch {
            securitySettingsUseCases.setLockTimeout(timeoutMs.coerceAtLeast(AppDefaults.MIN_LOCK_TIMEOUT_MS))
        }
    }

    fun setStatusBarAutoHide(enabled: Boolean) {
        viewModelScope.launch { systemSettingsUseCases.setStatusBarAutoHide(enabled) }
    }

    fun setTopBarCollapsible(enabled: Boolean) {
        viewModelScope.launch { systemSettingsUseCases.setTopBarCollapsible(enabled) }
    }

    fun setTabBarCollapsible(enabled: Boolean) {
        viewModelScope.launch { systemSettingsUseCases.setTabBarCollapsible(enabled) }
    }

    fun setSecureContentEnabled(enabled: Boolean) {
        viewModelScope.launch { securitySettingsUseCases.setSecureContentEnabled(enabled) }
    }

    fun setPasswordPreferredAuthFirst(enabled: Boolean) {
        viewModelScope.launch { securitySettingsUseCases.setPasswordPreferredAuthFirst(enabled) }
    }

    fun setDeviceCredentialFallbackEnabled(enabled: Boolean) {
        viewModelScope.launch { securitySettingsUseCases.setDeviceCredentialFallbackEnabled(enabled) }
    }

    fun setFlipToLockEnabled(enabled: Boolean) {
        viewModelScope.launch { securitySettingsUseCases.setFlipToLockEnabled(enabled) }
    }

    fun setFlipExitAndClearStackEnabled(enabled: Boolean) {
        viewModelScope.launch { securitySettingsUseCases.setFlipExitAndClearStackEnabled(enabled) }
    }

    fun setCardStyle(style: VaultCardStyle) {
        viewModelScope.launch { systemSettingsUseCases.setCardStyle(style) }
    }

    fun setCardStyleForEntryType(entryType: Int, style: VaultCardStyle) {
        viewModelScope.launch { systemSettingsUseCases.setCardStyleForEntryType(entryType, style) }
    }

    fun toggleAutofillUiMode(current: AutofillUiMode) {
        val next = when (current) {
            AutofillUiMode.SYSTEM_INLINE -> AutofillUiMode.BOTTOM_SHEET
            AutofillUiMode.BOTTOM_SHEET -> AutofillUiMode.SYSTEM_INLINE
        }
        viewModelScope.launch { systemSettingsUseCases.setAutofillUiMode(next) }
    }

    fun setSwipeEnabled(enabled: Boolean) {
        viewModelScope.launch { systemSettingsUseCases.setSwipeEnabled(enabled) }
    }

    fun setSwipeLeftAction(action: SwipeActionType) {
        viewModelScope.launch { systemSettingsUseCases.setSwipeLeftAction(action) }
    }

    fun setSwipeRightAction(action: SwipeActionType) {
        viewModelScope.launch { systemSettingsUseCases.setSwipeRightAction(action) }
    }

    fun setTabBarMaxTabsWithoutScroll(maxTabs: Int) {
        viewModelScope.launch { systemSettingsUseCases.setTabBarMaxTabsWithoutScroll(maxTabs) }
    }

    fun setVisibleVaultTabs(tabs: Set<String>) {
        viewModelScope.launch { systemSettingsUseCases.setVisibleVaultTabs(tabs) }
    }

    fun setAutoDownloadIcons(enabled: Boolean) {
        viewModelScope.launch { systemSettingsUseCases.setAutoDownloadIcons(enabled) }
    }

    fun setBackupDirectoryUri(uri: String) {
        viewModelScope.launch { backupSettingsUseCases.setBackupDirectoryUri(uri) }
    }

    fun clearBackupDirectoryUri() {
        viewModelScope.launch { backupSettingsUseCases.clearBackupDirectoryUri() }
    }

    fun testBackupDirectoryWritePermission(uri: String?) {
        backup.testBackupDirectoryWritePermission(uri)
    }
}