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
import com.aozijx.passly.core.common.SwipeActionType
import com.aozijx.passly.core.common.VaultCardStyle
import com.aozijx.passly.core.di.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsUseCases = AppContainer.settingsUseCases

    val lockTimeout: StateFlow<Long> = settingsUseCases.lockTimeout
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 60000L)
    
    // --- 沉浸式折叠设置 ---
    val isStatusBarAutoHide: StateFlow<Boolean> = settingsUseCases.isStatusBarAutoHide
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
        
    val isTopBarCollapsible: StateFlow<Boolean> = settingsUseCases.isTopBarCollapsible
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
        
    val isTabBarCollapsible: StateFlow<Boolean> = settingsUseCases.isTabBarCollapsible
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
        
    // 合并后的安全设置 (控制 FLAG_SECURE)
    val isSecureContentEnabled: StateFlow<Boolean> = settingsUseCases.isSecureContentEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isFlipToLockEnabled: StateFlow<Boolean> = settingsUseCases.isFlipToLockEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isFlipExitAndClearStackEnabled: StateFlow<Boolean> = settingsUseCases.isFlipExitAndClearStackEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val cardStyle: StateFlow<VaultCardStyle> = settingsUseCases.cardStyle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VaultCardStyle.BASE)

    // --- 交互设置 ---
    val isSwipeEnabled: StateFlow<Boolean> = settingsUseCases.isSwipeEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val swipeLeftAction: StateFlow<SwipeActionType> = settingsUseCases.swipeLeftAction
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SwipeActionType.COPY_PASSWORD)
    val swipeRightAction: StateFlow<SwipeActionType> = settingsUseCases.swipeRightAction
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SwipeActionType.DETAIL)

    // --- Setter 方法 ---
    fun setStatusBarAutoHide(autoHide: Boolean) = viewModelScope.launch { settingsUseCases.setStatusBarAutoHide(autoHide) }
    fun setTopBarCollapsible(collapsible: Boolean) = viewModelScope.launch { settingsUseCases.setTopBarCollapsible(collapsible) }
    fun setTabBarCollapsible(collapsible: Boolean) = viewModelScope.launch { settingsUseCases.setTabBarCollapsible(collapsible) }
    fun setSecureContentEnabled(enabled: Boolean) = viewModelScope.launch { settingsUseCases.setSecureContentEnabled(enabled) }
    fun setFlipToLockEnabled(enabled: Boolean) = viewModelScope.launch { settingsUseCases.setFlipToLockEnabled(enabled) }
    fun setFlipExitAndClearStackEnabled(enabled: Boolean) = viewModelScope.launch { settingsUseCases.setFlipExitAndClearStackEnabled(enabled) }
    fun setLockTimeout(timeoutMs: Long) = viewModelScope.launch { settingsUseCases.setLockTimeout(timeoutMs.coerceAtLeast(5000L)) }
    fun setCardStyle(style: VaultCardStyle) = viewModelScope.launch { settingsUseCases.setCardStyle(style) }

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
            val result = if (isExporting) BackupManager.exportBackup(context, uri, password)
            else BackupManager.importBackup(context, uri, password, importMode)
            
            backupMessage = if (result.isSuccess) {
                if (isExporting) "导出成功" else "导入成功"
            } else {
                buildBackupFailureMessage(result.exceptionOrNull())
            }
            dismissBackupPasswordDialog()
        }
    }

    private fun buildBackupFailureMessage(error: Throwable?): String {
        val message = error?.message?.trim().orEmpty()
        if (message.contains("密码错误")) {
            return message
        }
        return if (message.isNotEmpty()) {
            "失败: $message"
        } else {
            "失败: 未知错误"
        }
    }

    fun clearBackupMessage() { backupMessage = null }
}



