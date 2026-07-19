package com.aozijx.passly.feature.backup.contract

import com.aozijx.passly.core.error.AppError

sealed interface BackupEffect {
    data class ShowError(val error: AppError) : BackupEffect
    data class ShowPlainExportPicker(val fileName: String) : BackupEffect
    data object RequestAuth : BackupEffect
    data object StartImportSyncService : BackupEffect
}