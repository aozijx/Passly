package com.aozijx.passly.features.backup

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import com.aozijx.passly.R
import com.aozijx.passly.core.backup.BackupExportStorageSupport
import com.aozijx.passly.core.backup.BackupImportMode
import com.aozijx.passly.domain.usecase.backup.BackupUseCases
import com.aozijx.passly.domain.usecase.settings.backup.BackupSettingsUseCases
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 备份/恢复交互状态与流程协调器。
 * 已经重构为完全对接 [BackupUseCases] 架构。
 */
class BackupCoordinator(
    private val scope: CoroutineScope,
    private val backupSettingsUseCases: BackupSettingsUseCases,
    private val backupUseCases: BackupUseCases,
    private val application: Application
) {
    // --- 备份/恢复 UI 状态 ---
    var backupMessage by mutableStateOf<String?>(null)
        private set
    var isExporting by mutableStateOf(false)
    var showBackupPasswordDialog by mutableStateOf(false)
    var backupUri by mutableStateOf<Uri?>(null)
    var backupPassword by mutableStateOf("")
    var importMode by mutableStateOf(BackupImportMode.OVERWRITE)
    var includeImagesInBackup by mutableStateOf(true)
    var backupExportFallbackFileName by mutableStateOf<String?>(null)
        private set

    private var pendingExportFileName: String? = null
    private var pendingExportAllowFallback: Boolean = false

    private fun text(@StringRes resId: Int, vararg args: Any): String =
        application.getString(resId, *args)

    fun startExport(uri: Uri, fileNameHint: String? = null, allowFallback: Boolean = false) {
        backupUri = uri
        isExporting = true
        showBackupPasswordDialog = true
        pendingExportFileName = fileNameHint
        pendingExportAllowFallback = allowFallback
    }

    fun startImport(uri: Uri) {
        backupUri = uri
        isExporting = false
        showBackupPasswordDialog = true
        pendingExportFileName = null
        pendingExportAllowFallback = false
    }

    fun nextBackupFileName(): String = BackupExportStorageSupport.buildBackupFileName()

    fun tryStartExportInConfiguredDirectory(directoryUri: String?): Boolean {
        if (directoryUri.isNullOrBlank()) return false
        startExport(directoryUri.toUri(), fileNameHint = nextBackupFileName(), allowFallback = true)
        return true
    }

    fun consumeBackupExportFallbackFileName() {
        backupExportFallbackFileName = null
    }

    fun dismissBackupPasswordDialog() {
        showBackupPasswordDialog = false
        backupPassword = ""
        backupUri = null
    }

    fun processBackupAction(context: Context, onAuthRequired: (onSuccess: () -> Unit) -> Unit) {
        val targetUri = backupUri ?: return
        val password = backupPassword.toCharArray()
        val exportingNow = isExporting
        val exportFileName = pendingExportFileName
        val allowFallback = pendingExportAllowFallback
        val includeImages = includeImagesInBackup

        onAuthRequired {
            scope.launch {
                try {
                    val finalUri = if (exportingNow && allowFallback) {
                        val createResult = BackupExportStorageSupport.createNamedExportTarget(
                            context, targetUri.toString(), exportFileName ?: nextBackupFileName()
                        )
                        if (createResult.isFailure) {
                            backupMessage = text(R.string.backup_error_create_file_failed)
                            return@launch
                        }
                        createResult.getOrThrow().fileUri
                    } else {
                        targetUri
                    }

                    val outcome = if (exportingNow) {
                        backupUseCases.exportBackup(finalUri, password, includeImages)
                    } else {
                        backupUseCases.importBackup(finalUri, password, importMode)
                    }

                    if (exportingNow && outcome.isFailure && finalUri != targetUri) {
                        BackupExportStorageSupport.deleteDocument(context, finalUri)
                    }

                    if (exportingNow && outcome.isSuccess) {
                        if (!exportFileName.isNullOrBlank()) {
                            backupSettingsUseCases.setLastBackupExportFileName(exportFileName)
                        } else if (allowFallback) {
                            backupExportFallbackFileName = exportFileName ?: nextBackupFileName()
                        }
                    }

                    backupMessage = if (outcome.isSuccess) {
                        text(if (exportingNow) R.string.backup_export_success else R.string.backup_import_success)
                    } else {
                        outcome.exceptionOrNull()?.message ?: text(R.string.backup_error_unknown)
                    }

                    dismissBackupPasswordDialog()
                    pendingExportFileName = null
                    pendingExportAllowFallback = false
                } finally {
                    password.fill('\u0000')
                }
            }
        }
    }

    fun clearBackupMessage() {
        backupMessage = null
    }

    fun testBackupDirectoryWritePermission(directoryUri: String?) {
        if (directoryUri.isNullOrBlank()) {
            backupMessage = text(R.string.backup_directory_set_first)
            return
        }
        scope.launch {
            val result = backupUseCases.testDirectoryWritePermission(directoryUri)
            backupMessage = if (result.isSuccess) {
                text(R.string.backup_directory_permission_ok)
            } else {
                text(R.string.backup_directory_permission_failed)
            }
        }
    }
}