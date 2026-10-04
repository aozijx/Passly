package com.aozijx.passly.presentation.feature.backup.ui.model

import com.aozijx.passly.feature.backup.internal.model.BackupExportFormat
import com.aozijx.passly.feature.backup.internal.model.BackupImportStrategy
import com.aozijx.passly.feature.backup.internal.model.ImportMode
import com.aozijx.passly.presentation.feature.backup.BackupOptionsStage
import com.aozijx.passly.presentation.shared.entry.EntryTypeUiModel

internal data class BackupRestoreSheetUiState(
    val activeSheet: BackupOptionsStage?,
    val configuredDirectoryLabel: String?,
    val password: String,
    val importMode: ImportMode,
    val importStrategy: BackupImportStrategy,
    val selectedExportFormat: BackupExportFormat,
    val includeIcons: Boolean,
    val includeAttachments: Boolean,
    val includeDeleted: Boolean,
    val includedEntryTypes: Set<EntryTypeUiModel>,
    val canSubmitExport: Boolean,
)
