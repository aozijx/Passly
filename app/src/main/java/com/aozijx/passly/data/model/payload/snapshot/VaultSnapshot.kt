package com.aozijx.passly.data.model.payload.snapshot

import com.aozijx.passly.data.model.payload.backup.AttachmentPayload
import com.aozijx.passly.data.model.payload.backup.BackupSchema
import com.aozijx.passly.data.model.payload.secret.SecretPayload
import com.aozijx.passly.data.model.payload.summary.SummaryPayload
import com.aozijx.passly.domain.model.entry.EntryType
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

    val revision: Int = 1,

    val source: String? = null,
    val appVersion: String? = null,

    val summary: SummaryPayload,
    val secret: SecretPayload = SecretPayload.VaultData(com.aozijx.passly.data.model.payload.secret.VaultDataPayload()),
    val attachments: List<AttachmentPayload> = emptyList()
)
