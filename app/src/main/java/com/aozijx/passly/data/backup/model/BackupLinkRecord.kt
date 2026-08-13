package com.aozijx.passly.data.backup.model

import kotlinx.serialization.Serializable

@Serializable
data class BackupLinkRecord(
    val id: String,
    val sourceEntryId: String,
    val targetEntryId: String,
    val relationType: String,
    val createdAt: Long,
    val updatedAt: Long
)
