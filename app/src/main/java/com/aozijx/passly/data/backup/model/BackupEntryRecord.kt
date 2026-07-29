package com.aozijx.passly.data.backup.model

import kotlinx.serialization.Required
import kotlinx.serialization.Serializable

@Serializable
data class BackupEntryRecord(
    val id: String,
    @Required
    val vaultId: String = "default",
    val parentEntryId: String? = null,
    val type: String,
    val version: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
    val summary: BackupSummaryRecord,
    val secret: BackupSecretRecord,
    val attachmentIds: List<String> = emptyList()
)
