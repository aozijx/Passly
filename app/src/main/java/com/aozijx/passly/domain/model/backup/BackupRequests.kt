package com.aozijx.passly.domain.model.backup

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
    val includeIcons: Boolean = false
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
