package com.example.poop.features.settings

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.poop.AppContext
import com.example.poop.core.common.SwipeActionType
import com.example.poop.core.util.BackupManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = AppContext.get().preference
    
    // --- 沉浸式折叠设置 ---
    val isStatusBarAutoHide: StateFlow<Boolean> = prefs.isStatusBarAutoHide
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
        
    val isTopBarCollapsible: StateFlow<Boolean> = prefs.isTopBarCollapsible
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
        
    val isTabBarCollapsible: StateFlow<Boolean> = prefs.isTabBarCollapsible
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
        
    val isPrivacyScreenEnabled: StateFlow<Boolean> = prefs.isPrivacyScreenEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // --- 交互设置 ---
    val isSwipeEnabled: StateFlow<Boolean> = prefs.isSwipeEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val swipeLeftAction: StateFlow<SwipeActionType> = prefs.swipeLeftAction
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SwipeActionType.COPY_PASSWORD)
    val swipeRightAction: StateFlow<SwipeActionType> = prefs.swipeRightAction
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SwipeActionType.DETAIL)

    // --- Setter 方法 ---
    fun setStatusBarAutoHide(autoHide: Boolean) = viewModelScope.launch { prefs.setStatusBarAutoHide(autoHide) }
    fun setTopBarCollapsible(collapsible: Boolean) = viewModelScope.launch { prefs.setTopBarCollapsible(collapsible) }
    fun setTabBarCollapsible(collapsible: Boolean) = viewModelScope.launch { prefs.setTabBarCollapsible(collapsible) }
    fun setPrivacyScreenEnabled(enabled: Boolean) = viewModelScope.launch { prefs.setPrivacyScreenEnabled(enabled) }
    
    fun setSwipeEnabled(enabled: Boolean) = viewModelScope.launch { prefs.setSwipeEnabled(enabled) }
    fun setSwipeLeftAction(action: SwipeActionType) = viewModelScope.launch { prefs.setSwipeLeftAction(action) }
    fun setSwipeRightAction(action: SwipeActionType) = viewModelScope.launch { prefs.setSwipeRightAction(action) }

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
                "失败: ${result.exceptionOrNull()?.message ?: "未知错误"}"
            }
            dismissBackupPasswordDialog()
        }
    }

    fun clearBackupMessage() { backupMessage = null }
}
