package com.aozijx.passly.feature.backup.contract

import android.net.Uri
import com.aozijx.passly.core.error.AppError
import com.aozijx.passly.domain.model.backup.ImportMode

sealed class BackupOperationStatus {
    data object Idle : BackupOperationStatus()
    data object Loading : BackupOperationStatus()
    data class Success(val type: OperationType) : BackupOperationStatus()
    data object Failure : BackupOperationStatus()

    enum class OperationType {
        EXPORT, IMPORT, PLAIN_EXPORT, PERMISSION_CHECK
    }
}

data class BackupUiState(
    val status: BackupOperationStatus = BackupOperationStatus.Idle,
    val error: AppError? = null,
    val isExporting: Boolean = false,
    val showPasswordDialog: Boolean = false,
    val backupUri: Uri? = null,
    val backupPassword: String = "",
    val importMode: ImportMode = ImportMode.APPEND,
    val includeIcons: Boolean = false,
    val pendingExportFileName: String? = null,
    val pendingExportAllowFallback: Boolean = false
)
