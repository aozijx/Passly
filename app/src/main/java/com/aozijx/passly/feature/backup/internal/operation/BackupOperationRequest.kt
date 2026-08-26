package com.aozijx.passly.feature.backup.internal.operation

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.feature.backup.internal.model.BackupExportFormat
import com.aozijx.passly.feature.backup.internal.model.ImportMode

internal data class BackupOperationRequest(
    val operation: BackupOperation,
    val exportFormat: BackupExportFormat,
    val importMode: ImportMode,
    val includeIcons: Boolean,
    val includeAttachments: Boolean,
    val includeDeleted: Boolean,
    val includedEntryTypes: Set<EntryType>,
    val targetUri: String?,
    val password: SensitiveValue,
    val pendingExportFileName: String?,
    val deleteTargetOnFailure: Boolean,
)
