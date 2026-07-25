package com.aozijx.passly.feature.backup.model

import com.aozijx.passly.domain.backup.model.BackupFormatId
import com.aozijx.passly.domain.backup.model.BackupFormats

enum class BackupExportUiFormat(
    val formatId: BackupFormatId,
    val extension: String,
    val mimeType: String,
    val requiresPassword: Boolean,
    val supportsResources: Boolean
) {
    ENCRYPTED(
        formatId = BackupFormats.PASSLY_ENCRYPTED,
        extension = "passly",
        mimeType = "application/octet-stream",
        requiresPassword = true,
        supportsResources = true
    ),
    JSON(
        formatId = BackupFormats.PASSLY_JSON,
        extension = "json",
        mimeType = "application/json",
        requiresPassword = false,
        supportsResources = true
    ),
    TEXT(
        formatId = BackupFormats.READABLE_TEXT,
        extension = "txt",
        mimeType = "text/plain",
        requiresPassword = false,
        supportsResources = false
    )
}
