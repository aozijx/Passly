package com.example.poop.features.settings

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.poop.core.util.BackupManager
import com.example.poop.data.local.AppDatabase
import com.example.poop.data.local.VaultPrefs
import com.example.poop.data.repository.VaultRepository
import kotlinx.coroutines.launch

/**
 * 设置与备份业务逻辑
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: VaultRepository by lazy {
        val database = AppDatabase.getDatabase(application)
        VaultRepository(database.vaultDao(), VaultPrefs(application))
    }

    // --- 备份/恢复状态 ---
    var backupMessage by mutableStateOf<String?>(null)
        private set
    var isExporting by mutableStateOf(false)
    var backupPassword by mutableStateOf("")
    var backupUri by mutableStateOf<Uri?>(null)
    var showBackupPasswordDialog by mutableStateOf(false)
    var importMode by mutableStateOf(BackupManager.ImportMode.OVERWRITE)

    fun startExport(uri: Uri) {
        backupUri = uri
        isExporting = true
        importMode = BackupManager.ImportMode.OVERWRITE
        showBackupPasswordDialog = true
    }

    fun startImport(uri: Uri) {
        backupUri = uri
        isExporting = false
        importMode = BackupManager.ImportMode.OVERWRITE
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
            val result = if (isExporting) {
                BackupManager.exportBackup(context, uri, password)
            } else {
                BackupManager.importBackup(context, uri, password, importMode)
            }
            
            backupMessage = if (result.isSuccess) {
                if (isExporting) "导出成功" else "导入成功"
            } else {
                val error = result.exceptionOrNull()?.message ?: "未知错误"
                (if (isExporting) "导出失败: " else "导入失败: ") + error
            }
            dismissBackupPasswordDialog()
        }
    }

    fun clearBackupMessage() {
        backupMessage = null
    }
}
