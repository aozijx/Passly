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
import com.aozijx.passly.core.logging.Logcat
import com.aozijx.passly.domain.model.core.BackupException
import com.aozijx.passly.domain.usecase.backup.BackupUseCases
import com.aozijx.passly.domain.usecase.settings.backup.BackupSettingsUseCases
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.aozijx.passly.domain.model.backup.BackupImportMode as DomainBackupImportMode

/**
 * 备份模块 UI 状态聚合。
 */
data class BackupUiState(
    val isExporting: Boolean = false,
    val showPasswordDialog: Boolean = false,
    val backupUri: Uri? = null,
    val backupPassword: String = "",
    val importMode: DomainBackupImportMode = DomainBackupImportMode.OVERWRITE,
    val includeImages: Boolean = true,
    val pendingExportFileName: String? = null,
    val pendingExportAllowFallback: Boolean = false
)

/**
 * 备份/恢复流程协调器。
 * 采用状态聚合模式，实现了 UI 状态与业务逻辑的高内聚。
 */
class BackupCoordinator(
    private val scope: CoroutineScope,
    private val backupSettingsUseCases: BackupSettingsUseCases,
    private val backupUseCases: BackupUseCases,
    private val application: Application
) {
    private val tag = "BackupCoordinator"

    // --- 状态流 ---
    var state by mutableStateOf(BackupUiState())
        private set

    var backupMessage by mutableStateOf<String?>(null)
        private set

    private fun text(@StringRes resId: Int, vararg args: Any): String =
        application.getString(resId, *args)

    // --- 路径设置 ---
    fun setBackupDirectoryUri(uri: String) {
        scope.launch { backupSettingsUseCases.setBackupDirectoryUri(uri) }
    }

    fun clearBackupDirectoryUri() {
        scope.launch { backupSettingsUseCases.clearBackupDirectoryUri() }
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

    // --- 流程控制 ---
    fun startExport(uri: Uri, fileNameHint: String? = null, allowFallback: Boolean = false) {
        state = state.copy(
            backupUri = uri,
            isExporting = true,
            showPasswordDialog = true,
            pendingExportFileName = fileNameHint,
            pendingExportAllowFallback = allowFallback
        )
    }

    fun startImport(uri: Uri) {
        state = state.copy(
            backupUri = uri,
            isExporting = false,
            showPasswordDialog = true,
            pendingExportFileName = null,
            pendingExportAllowFallback = false
        )
    }

    fun updatePassword(password: String) {
        state = state.copy(backupPassword = password)
    }

    fun updateImportMode(mode: DomainBackupImportMode) {
        state = state.copy(importMode = mode)
    }

    fun updateIncludeImages(include: Boolean) {
        state = state.copy(includeImages = include)
    }

    fun nextBackupFileName(): String = BackupExportStorageSupport.buildBackupFileName()

    fun tryStartExportInConfiguredDirectory(directoryUri: String?): Boolean {
        if (directoryUri.isNullOrBlank()) return false
        startExport(directoryUri.toUri(), fileNameHint = nextBackupFileName(), allowFallback = true)
        return true
    }

    fun dismissPasswordDialog() {
        state = state.copy(showPasswordDialog = false, backupPassword = "", backupUri = null)
    }

    fun processBackupAction(context: Context, onAuthRequired: (onSuccess: () -> Unit) -> Unit) {
        val currentState = state
        val targetUri = currentState.backupUri ?: return
        val password = currentState.backupPassword.toCharArray()

        onAuthRequired {
            scope.launch {
                try {
                    Logcat.d(tag, "Starting action. Exporting: ${currentState.isExporting}")

                    val finalUri =
                        if (currentState.isExporting && currentState.pendingExportAllowFallback) {
                            val createResult = BackupExportStorageSupport.createNamedExportTarget(
                                context,
                                targetUri.toString(),
                                currentState.pendingExportFileName ?: nextBackupFileName()
                            )
                            if (createResult.isFailure) {
                                throw BackupException.StoragePermissionDenied()
                            }
                            createResult.getOrThrow().fileUri
                        } else {
                            targetUri
                        }

                    val outcome = if (currentState.isExporting) {
                        backupUseCases.exportBackup(finalUri, password, currentState.includeImages)
                    } else {
                        backupUseCases.importBackup(finalUri, password, currentState.importMode)
                    }

                    if (outcome.isSuccess) {
                        handleSuccess(context, currentState)
                    } else {
                        handleFailure(context, outcome.exceptionOrNull(), finalUri, currentState)
                    }

                    dismissPasswordDialog()
                } finally {
                    password.fill('\u0000')
                }
            }
        }
    }

    private suspend fun handleSuccess(context: Context, oldState: BackupUiState) {
        Logcat.i(tag, "Operation successful")
        if (oldState.isExporting) {
            oldState.pendingExportFileName?.let {
                backupSettingsUseCases.setLastBackupExportFileName(
                    it
                )
            }
        } else {
            BackupImportIconSyncForegroundService.start(context)
        }
        backupMessage =
            text(if (oldState.isExporting) R.string.backup_export_success else R.string.backup_import_success)
    }

    private fun handleFailure(
        context: Context, error: Throwable?, finalUri: Uri, oldState: BackupUiState
    ) {
        Logcat.e(tag, "Operation failed", error)
        if (oldState.isExporting && oldState.pendingExportAllowFallback) {
            BackupExportStorageSupport.deleteDocument(context, finalUri)
        }

        backupMessage = when (error) {
            is BackupException -> error.message
            else -> text(R.string.backup_error_unknown)
        }
    }

    fun clearBackupMessage() {
        backupMessage = null
    }
}