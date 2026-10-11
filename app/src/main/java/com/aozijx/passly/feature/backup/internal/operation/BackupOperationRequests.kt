package com.aozijx.passly.feature.backup.internal.operation

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.sensitive.SensitiveValue
import com.aozijx.passly.feature.backup.internal.model.BackupExportFormat
import com.aozijx.passly.feature.backup.internal.model.BackupImportStrategy
import com.aozijx.passly.feature.backup.internal.model.ImportMode

internal data class BackupExportOperationRequest(
    val format: BackupExportFormat,
    val includeIcons: Boolean,
    val includeAttachments: Boolean,
    val includeDeleted: Boolean,
    val includedEntryTypes: Set<EntryType>,
    val password: SensitiveValue,
    val fileName: String,
)

internal data class BackupImportOperationRequest(
    val sourceUri: String,
    val password: SensitiveValue,
    val mode: ImportMode,
    val strategy: BackupImportStrategy,
)
