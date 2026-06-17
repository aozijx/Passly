package com.aozijx.passly.features.backup.contract

import android.net.Uri
import com.aozijx.passly.domain.model.BackupImportMode
import java.io.File

data class BackupUiState(
    val isExporting: Boolean = false,
    val showPasswordDialog: Boolean = false,
    val backupUri: Uri? = null,
    val backupPassword: String = "",
    val importMode: BackupImportMode = BackupImportMode.OVERWRITE,
    val includeImages: Boolean = true,
    val pendingExportFileName: String? = null,
    val pendingExportAllowFallback: Boolean = false,
    val emergencyBackupFile: File? = null,
    val resultMessage: String? = null
)