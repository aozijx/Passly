package com.aozijx.passly.feature.backup.internal.archive.model

import kotlinx.serialization.Serializable

@Serializable
data class BackupResourceRecord(
    val id: String,
    val entryId: String,
    val kind: BackupResourceKind,
    val fileName: String? = null,
    val mimeType: String? = null,
    val size: Long,
    val sha256: String,
    val createdAt: Long? = null
)
