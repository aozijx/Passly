package com.aozijx.passly.feature.backup.internal.contract

import android.net.Uri
import com.aozijx.passly.core.error.model.AppError
import com.aozijx.passly.domain.backup.model.BackupExportUiFormat
import com.aozijx.passly.domain.backup.model.BackupOperationStatus
import com.aozijx.passly.domain.backup.model.ImportMode
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.sensitive.EmptySensitiveValue
import com.aozijx.passly.domain.sensitive.SensitiveValue

data class BackupUiState(
    val status: BackupOperationStatus = BackupOperationStatus.Idle,
    val error: AppError? = null,
    val isExporting: Boolean = false,
    val backupUri: Uri? = null,
    val backupPassword: SensitiveValue = EmptySensitiveValue,
    val importMode: ImportMode = ImportMode.APPEND,
    val selectedExportFormat: BackupExportUiFormat = BackupExportUiFormat.ENCRYPTED,
    val includeIcons: Boolean = true,
    val includeAttachments: Boolean = true,
    val includeDeleted: Boolean = true,
    val includedEntryTypes: Set<EntryType> = EntryType.entries.toSet(),
    val pendingExportFileName: String? = null,
    val deleteTargetOnFailure: Boolean = false
) {
    val canSubmitExport: Boolean
        get() = includedEntryTypes.isNotEmpty() &&
                (!selectedExportFormat.requiresPassword || !backupPassword.isEmpty)
}
