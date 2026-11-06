package com.aozijx.passly.domain.backup.model

import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.relation.EntryLink

/** Persistence-neutral data read from or restored into the vault. */
data class VaultBackupSnapshot(
    val entries: List<Entry>,
    val links: List<EntryLink>,
    val resources: List<VaultBackupResource>,
)

enum class VaultBackupResourceKind {
    ICON,
    ATTACHMENT,
}

/** Stored Vault resource content paired with its entry identity and metadata. */
data class VaultBackupResource(
    val id: String,
    val entryId: EntryId,
    val kind: VaultBackupResourceKind,
    val fileName: String?,
    val mimeType: String?,
    val createdAt: Long?,
    val content: ByteArray,
) {
    init {
        require(id.isNotBlank()) { "Resource ID cannot be blank" }
    }
}
