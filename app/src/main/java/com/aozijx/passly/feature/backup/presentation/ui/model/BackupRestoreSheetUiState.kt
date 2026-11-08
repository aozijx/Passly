package com.aozijx.passly.feature.backup.presentation.ui.model

import androidx.annotation.StringRes
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.feature.backup.contract.BackupImportStrategy
import com.aozijx.passly.feature.backup.contract.ImportMode

internal sealed interface BackupRestoreSheetUiState {
    data object Hidden : BackupRestoreSheetUiState

    data object FormatPicker : BackupRestoreSheetUiState

    data class ExportOptions(
        val configuredDirectoryLabel: String?,
        val password: String,
        @field:StringRes val titleResource: Int,
        @field:StringRes val securityNoticeResource: Int,
        val securityNoticeIsWarning: Boolean,
        val requiresPassword: Boolean,
        val supportsResources: Boolean,
        val includeIcons: Boolean,
        val includeAttachments: Boolean,
        val includeDeleted: Boolean,
        val includedEntryTypes: Set<EntryType>,
        val allEntryTypesSelected: Boolean,
        val needsEntryTypeSelection: Boolean,
        val canSubmit: Boolean,
    ) : BackupRestoreSheetUiState

    data class ImportOptions(
        val password: String,
        val mode: ImportMode,
        val strategy: BackupImportStrategy,
    ) : BackupRestoreSheetUiState
}
