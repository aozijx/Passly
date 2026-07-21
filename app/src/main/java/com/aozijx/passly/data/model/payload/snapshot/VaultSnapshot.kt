package com.aozijx.passly.data.model.payload.snapshot

import com.aozijx.passly.data.model.payload.backup.AttachmentPayload
import com.aozijx.passly.data.model.payload.backup.BackupSchema
import com.aozijx.passly.domain.model.entry.EntryType
import com.aozijx.passly.domain.model.entry.VaultCredential
import com.aozijx.passly.domain.model.entry.VaultMetadata
import com.aozijx.passly.domain.model.type.Platform
import kotlinx.serialization.Serializable

@Serializable
data class VaultSnapshot(
    val id: String,
    val vaultId: String = "default",
    val entryType: EntryType = EntryType.LOGIN,

    val schemaVersion: Int = BackupSchema.VERSION,
    val deletedAt: Long? = null,

    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val lastUsedAt: Long? = null,

    val revision: Int = 1,

    val source: String? = null,
    val appVersion: String? = null,
    val platform: Platform? = null,

    val metadata: VaultMetadata,
    val credential: VaultCredential = VaultCredential(entryId = ""),
    val attachments: List<AttachmentPayload> = emptyList()
)
