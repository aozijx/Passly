package com.aozijx.passly.features.settings

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aozijx.passly.core.backup.BackupManager
import com.aozijx.passly.core.common.AutofillUiMode
import com.aozijx.passly.core.common.SwipeActionType
import com.aozijx.passly.core.common.VaultCardStyle
import com.aozijx.passly.core.di.AppContainer
import com.aozijx.passly.features.settings.internal.BackupActionSupport
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val lockTimeout: Long = 60000L,
    val isStatusBarAutoHide: Boolean = true,
    val isTopBarCollapsible: Boolean = true,
    val isTabBarCollapsible: Boolean = true,
    val isSecureContentEnabled: Boolean = true,
    val isFlipToLockEnabled: Boolean = false,
    val isFlipExitAndClearStackEnabled: Boolean = false,
    val cardStyle: VaultCardStyle = VaultCardStyle.BASE,
    val autofillUiMode: AutofillUiMode = AutofillUiMode.SYSTEM_INLINE,
    val isSwipeEnabled: Boolean = true,
    val swipeLeftAction: SwipeActionType = SwipeActionType.COPY_PASSWORD,
    val swipeRightAction: SwipeActionType = SwipeActionType.DETAIL
)

private data class CoreSettingsFlowState(
    val lockTimeout: Long,
    val isStatusBarAutoHide: Boolean,
    val isTopBarCollapsible: Boolean,
    val isTabBarCollapsible: Boolean,
    val isSecureContentEnabled: Boolean
)

private data class InteractionSettingsFlowState(
    val isFlipToLockEnabled: Boolean,
    val isFlipExitAndClearStackEnabled: Boolean,
    val cardStyle: VaultCardStyle,
    val autofillUiMode: AutofillUiMode,
    val isSwipeEnabled: Boolean,
    val swipeLeftAction: SwipeActionType
)

private data class SecurityAndStyleFlowState(
    val isFlipToLockEnabled: Boolean,
    val isFlipExitAndClearStackEnabled: Boolean,
    val cardStyle: VaultCardStyle
)

private data class AutofillAndSwipeFlowState(
    val autofillUiMode: AutofillUiMode,
    val isSwipeEnabled: Boolean,
    val swipeLeftAction: SwipeActionType
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsUseCases = AppContainer.settingsUseCases
    private val backupActionSupport = BackupActionSupport()

    val uiState: StateFlow<SettingsUiState> = combine(
        combine(
            settingsUseCases.lockTimeout,
            settingsUseCases.isStatusBarAutoHide,
            settingsUseCases.isTopBarCollapsible,
            settingsUseCases.isTabBarCollapsible,
            settingsUseCases.isSecureContentEnabled
        ) { lockTimeout, isStatusBarAutoHide, isTopBarCollapsible, isTabBarCollapsible, isSecureContentEnabled ->
            CoreSettingsFlowState(
                lockTimeout = lockTimeout,
                isStatusBarAutoHide = isStatusBarAutoHide,
                isTopBarCollapsible = isTopBarCollapsible,
                isTabBarCollapsible = isTabBarCollapsible,
                isSecureContentEnabled = isSecureContentEnabled
            )
        },
        combine(
            settingsUseCases.isFlipToLockEnabled,
            settingsUseCases.isFlipExitAndClearStackEnabled,
            settingsUseCases.cardStyle
        ) { isFlipToLockEnabled, isFlipExitAndClearStackEnabled, cardStyle ->
            SecurityAndStyleFlowState(
                isFlipToLockEnabled = isFlipToLockEnabled,
                isFlipExitAndClearStackEnabled = isFlipExitAndClearStackEnabled,
                cardStyle = cardStyle
            )
        }.combine(
            combine(
                settingsUseCases.autofillUiMode,
                settingsUseCases.isSwipeEnabled,
                settingsUseCases.swipeLeftAction
            ) { autofillUiMode, isSwipeEnabled, swipeLeftAction ->
                AutofillAndSwipeFlowState(
                    autofillUiMode = autofillUiMode,
                    isSwipeEnabled = isSwipeEnabled,
                    swipeLeftAction = swipeLeftAction
                )
            }
        ) { securityAndStyle, autofillAndSwipe ->
            InteractionSettingsFlowState(
                isFlipToLockEnabled = securityAndStyle.isFlipToLockEnabled,
                isFlipExitAndClearStackEnabled = securityAndStyle.isFlipExitAndClearStackEnabled,
                cardStyle = securityAndStyle.cardStyle,
                autofillUiMode = autofillAndSwipe.autofillUiMode,
                isSwipeEnabled = autofillAndSwipe.isSwipeEnabled,
                swipeLeftAction = autofillAndSwipe.swipeLeftAction
            )
        },
        settingsUseCases.swipeRightAction
    ) { core, interaction, swipeRightAction ->
        SettingsUiState(
            lockTimeout = core.lockTimeout,
            isStatusBarAutoHide = core.isStatusBarAutoHide,
            isTopBarCollapsible = core.isTopBarCollapsible,
            isTabBarCollapsible = core.isTabBarCollapsible,
            isSecureContentEnabled = core.isSecureContentEnabled,
            isFlipToLockEnabled = interaction.isFlipToLockEnabled,
            isFlipExitAndClearStackEnabled = interaction.isFlipExitAndClearStackEnabled,
            cardStyle = interaction.cardStyle,
            autofillUiMode = interaction.autofillUiMode,
            isSwipeEnabled = interaction.isSwipeEnabled,
            swipeLeftAction = interaction.swipeLeftAction,
            swipeRightAction = swipeRightAction
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    // --- Setter 方法 ---
    fun setStatusBarAutoHide(autoHide: Boolean) = viewModelScope.launch { settingsUseCases.setStatusBarAutoHide(autoHide) }
    fun setTopBarCollapsible(collapsible: Boolean) = viewModelScope.launch { settingsUseCases.setTopBarCollapsible(collapsible) }
    fun setTabBarCollapsible(collapsible: Boolean) = viewModelScope.launch { settingsUseCases.setTabBarCollapsible(collapsible) }
    fun setSecureContentEnabled(enabled: Boolean) = viewModelScope.launch { settingsUseCases.setSecureContentEnabled(enabled) }
    fun setFlipToLockEnabled(enabled: Boolean) = viewModelScope.launch { settingsUseCases.setFlipToLockEnabled(enabled) }
    fun setFlipExitAndClearStackEnabled(enabled: Boolean) = viewModelScope.launch { settingsUseCases.setFlipExitAndClearStackEnabled(enabled) }
    fun setLockTimeout(timeoutMs: Long) = viewModelScope.launch { settingsUseCases.setLockTimeout(timeoutMs.coerceAtLeast(5000L)) }
    fun setCardStyle(style: VaultCardStyle) = viewModelScope.launch { settingsUseCases.setCardStyle(style) }
    fun toggleAutofillUiMode(currentMode: AutofillUiMode) = viewModelScope.launch {
        val nextMode = when (currentMode) {
            AutofillUiMode.SYSTEM_INLINE -> AutofillUiMode.BOTTOM_SHEET
            AutofillUiMode.BOTTOM_SHEET -> AutofillUiMode.SYSTEM_INLINE
        }
        settingsUseCases.setAutofillUiMode(nextMode)
    }

    fun setSwipeEnabled(enabled: Boolean) = viewModelScope.launch { settingsUseCases.setSwipeEnabled(enabled) }
    fun setSwipeLeftAction(action: SwipeActionType) = viewModelScope.launch { settingsUseCases.setSwipeLeftAction(action) }
    fun setSwipeRightAction(action: SwipeActionType) = viewModelScope.launch { settingsUseCases.setSwipeRightAction(action) }

    // --- 备份/恢复相关逻辑 ---
    var backupMessage by mutableStateOf<String?>(null); private set
    var isExporting by mutableStateOf(false)
    var showBackupPasswordDialog by mutableStateOf(false)
    var backupUri by mutableStateOf<Uri?>(null)
    var backupPassword by mutableStateOf("")
    var importMode by mutableStateOf(BackupManager.ImportMode.OVERWRITE)

    fun startExport(uri: Uri) {
        backupUri = uri
        isExporting = true
        showBackupPasswordDialog = true
    }

    fun startImport(uri: Uri) {
        backupUri = uri
        isExporting = false
        showBackupPasswordDialog = true
    }

    fun dismissBackupPasswordDialog() {
        showBackupPasswordDialog = false
        backupPassword = ""
        backupUri = null
    }

    fun processBackupAction(context: Context) {
        val uri = backupUri ?: return
        val password = backupPassword.toCharArray()
        viewModelScope.launch {
            backupMessage = backupActionSupport.runBackupAction(
                context = context,
                uri = uri,
                password = password,
                isExporting = isExporting,
                importMode = importMode
            )
            dismissBackupPasswordDialog()
        }
    }

    fun clearBackupMessage() { backupMessage = null }
}



