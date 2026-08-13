package com.aozijx.passly.data.backup.model

import kotlinx.serialization.Serializable

@Serializable
data class BackupDocument(
    val format: String,
    val version: Int,
    val exportedAt: Long,
    val appVersion: String? = null,
    val entries: List<BackupEntryRecord>,
    val links: List<BackupLinkRecord> = emptyList(),
    val resources: List<BackupResourceRecord> = emptyList()
) {
    companion object {
        const val FORMAT = "passly-archive"
        const val CURRENT_VERSION = 1
    }
}
