package com.aozijx.passly.feature.backup.internal.model

import com.aozijx.passly.domain.entry.model.EntryType

data class BackupExportOptions(
    val includeIcons: Boolean = true,
    val includeAttachments: Boolean = true,
    val includeDeleted: Boolean = true,
    val includedEntryTypes: Set<EntryType> = EntryType.entries.toSet()
) {
    init {
        require(includedEntryTypes.isNotEmpty()) {
            "At least one entry type must be selected"
        }
    }
}

/**
 * UI-independent export request.
 *
 * The service reads [password] only during the call. The caller retains
 * ownership and must clear it after the operation.
 */
class BackupExportRequest(
    val targetUri: String,
    val format: BackupFormatId,
    val password: CharArray? = null,
    val options: BackupExportOptions = BackupExportOptions()
)

/**
 * UI-independent import request.
 *
 * A null [format] enables content-based format detection. This is the normal
 * path for future third-party importers such as Bitwarden.
 */
class BackupImportRequest(
    val sourceUri: String,
    val mode: ImportMode,
    val format: BackupFormatId? = null,
    val password: CharArray? = null
)
