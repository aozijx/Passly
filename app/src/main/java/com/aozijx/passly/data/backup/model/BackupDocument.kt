package com.aozijx.passly.data.backup.model

import kotlinx.serialization.Serializable

@Serializable
data class BackupDocument(
    val format: String,
    val version: Int,
    val exportedAt: Long,
    val appVersion: String? = null,
    val entries: List<BackupEntryRecord>,
    val resources: List<BackupResourceRecord> = emptyList()
) {
    companion object {
        const val FORMAT = "passly-vault"
        const val CURRENT_VERSION = 1
    }
}
