package com.aozijx.passly.ui.features.backup

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import com.aozijx.passly.R
import com.aozijx.passly.core.error.AppError
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.data.repository.backup.BackupExportStorageSupport
import com.aozijx.passly.domain.model.BackupException
import com.aozijx.passly.domain.model.BackupImportMode
import com.aozijx.passly.domain.usecase.backup.BackupUseCases
import com.aozijx.passly.domain.usecase.settings.backup.BackupSettingsUseCases
import com.aozijx.passly.service.backup.BackupImportIconSyncForegroundService
import com.aozijx.passly.ui.components.toUiMessage
import com.aozijx.passly.ui.features.backup.contract.BackupUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 备份/恢复流程协调器。
 */
@Singleton
class BackupCoordinator @Inject constructor(
    private val backupSettingsUseCases: BackupSettingsUseCases,
    private val backupUseCases: BackupUseCases,
    private val application: Application
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var plainExportTokenIssuedAt: Long = 0L
    private var plainExportTokenTtlMs: Long = 0L

    var state by mutableStateOf(BackupUiState())
        private set

    var backupMessage by mutableStateOf<String?>(null)
        private set

    private fun text(@StringRes resId: Int, vararg args: Any): String =
        application.getString(resId, *args)

    fun issuePlainExportToken(ttlMs: Long = 60_000L) {
        plainExportTokenIssuedAt = System.currentTimeMillis()
        plainExportTokenTtlMs = ttlMs
    }

    private fun isPlainExportTokenValid(): Boolean {
        if (plainExportTokenIssuedAt <= 0L || plainExportTokenTtlMs <= 0L) return false
        return System.currentTimeMillis() <= plainExportTokenIssuedAt + plainExportTokenTtlMs
    }

    private fun consumePlainExportToken(): Boolean {
        val valid = isPlainExportTokenValid()
        plainExportTokenIssuedAt = 0L
        plainExportTokenTtlMs = 0L
        return valid
    }

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
            backupMessage = when (result) {
                is AppResult.Success -> text(R.string.backup_directory_permission_ok)
                is AppResult.Failure -> result.error.toUiMessage(
                    text(R.string.backup_directory_permission_failed)
                )
            }
        }
    }

    // --- 流程控制 ---
    fun startExport(uri: Uri, fileNameHint: String? = null, allowFallback: Boolean = false) {
        state = state.copy(
            backupUri = uri, isExporting = true, showPasswordDialog = true,
            pendingExportFileName = fileNameHint, pendingExportAllowFallback = allowFallback
        )
    }

    fun startImport(uri: Uri) {
        state = state.copy(
            backupUri = uri, isExporting = false, showPasswordDialog = true,
            pendingExportFileName = null, pendingExportAllowFallback = false
        )
    }

    fun updatePassword(password: String) = state.copy(backupPassword = password).also { state = it }
    fun updateImportMode(mode: BackupImportMode) = state.copy(importMode = mode).also { state = it }
    fun updateIncludeImages(include: Boolean) =
        state.copy(includeImages = include).also { state = it }

    fun dismissPasswordDialog() =
        state.copy(showPasswordDialog = false, backupPassword = "", backupUri = null)
            .also { state = it }

    fun clearBackupMessage() {
        backupMessage = null
    }

    fun nextBackupFileName(): String = BackupExportStorageSupport.buildBackupFileName()

    fun tryStartExportInConfiguredDirectory(directoryUri: String?): Boolean {
        if (directoryUri.isNullOrBlank()) return false
        startExport(directoryUri.toUri(), fileNameHint = nextBackupFileName(), allowFallback = true)
        return true
    }

    // --- 核心业务执行 ---

    fun processBackupAction(
        context: Context,
        onAuthRequired: (onSuccess: () -> Unit) -> Unit,
        onSuccess: (String) -> Unit = {},
        onFailure: (String) -> Unit = {}
    ) {
        val currentState = state
        val targetUri = currentState.backupUri ?: return
        val password = currentState.backupPassword.toCharArray()

        onAuthRequired {
            scope.launch {
                try {
                    val finalUri =
                        if (currentState.isExporting && currentState.pendingExportAllowFallback) {
                            val createResult = BackupExportStorageSupport.createNamedExportTarget(
                                context,
                                targetUri.toString(),
                                currentState.pendingExportFileName ?: nextBackupFileName()
                            )
                            if (createResult.isFailure) throw BackupException.StoragePermissionDenied()
                            createResult.getOrThrow().fileUri
                        } else targetUri

                    val outcome = if (currentState.isExporting) {
                        backupUseCases.exportBackup(finalUri, password, currentState.includeImages)
                    } else {
                        backupUseCases.importBackup(finalUri, password, currentState.importMode)
                    }

                    when (outcome) {
                        is AppResult.Success -> {
                            handleSuccess(context, currentState)
                            val msg =
                                text(if (currentState.isExporting) R.string.backup_export_success else R.string.backup_import_success)
                            onSuccess(msg)
                        }

                        is AppResult.Failure -> {
                            handleFailure(context, outcome.error, finalUri, currentState)
                            onFailure(outcome.error.toUiMessage(text(R.string.backup_error_unknown)))
                        }
                    }

                    dismissPasswordDialog()
                } finally {
                    password.fill('\u0000')
                }
            }
        }
    }

    /**
     * 导出明文备份（解密后导出）。
     */
    fun exportPlainBackup(
        context: Context,
        dirUri: String?,
        onPickerRequest: (fileName: String) -> Unit
    ) {
        if (!isPlainExportTokenValid()) {
            backupMessage = text(R.string.backup_plain_export_auth_required)
            return
        }

        val fileName = "Passly_Plain_Backup_${System.currentTimeMillis()}.json"
        if (!dirUri.isNullOrBlank()) {
            scope.launch {
                val targetResult = withContext(Dispatchers.IO) {
                    BackupExportStorageSupport.createNamedExportTarget(context, dirUri, fileName)
                }
                targetResult.fold(
                    onSuccess = { exportPlainBackupToUri(it.fileUri) },
                    onFailure = { onPickerRequest(fileName) }
                )
            }
        } else onPickerRequest(fileName)
    }

    fun exportPlainBackupToUri(uri: Uri) {
        if (!consumePlainExportToken()) {
            backupMessage = text(R.string.backup_plain_export_auth_required)
            return
        }

        scope.launch {
            backupMessage = when (val result = backupUseCases.exportPlainBackup(uri)) {
                is AppResult.Success -> "明文备份已导出"
                is AppResult.Failure -> result.error.toUiMessage("导出失败")
            }
        }
    }

    /**
     * 数据库损坏时的紧急抢救备份。
     */
    fun exportEmergencyBackup() {
        scope.launch {
            when (val result = backupUseCases.exportEmergencyBackup()) {
                is AppResult.Success -> {
                    val file = result.data
                    state = state.copy(emergencyBackupFile = file)
                    backupMessage = "紧急备份已导出: ${file.name}"
                }

                is AppResult.Failure -> {
                    backupMessage = "紧急备份导出失败: ${result.error.toUiMessage()}"
                }
            }
        }
    }

    private suspend fun handleSuccess(context: Context, oldState: BackupUiState) {
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
        context: Context,
        error: AppError,
        finalUri: Uri,
        oldState: BackupUiState
    ) {
        if (oldState.isExporting && oldState.pendingExportAllowFallback)
            BackupExportStorageSupport.deleteDocument(context, finalUri)
        backupMessage = error.toUiMessage(text(R.string.backup_error_unknown))
    }
}