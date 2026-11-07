package com.aozijx.passly.domain.backup.model

import com.aozijx.passly.domain.entry.model.EntryType

data class VaultBackupReadOptions(
    val includeDeleted: Boolean,
    val includedEntryTypes: Set<EntryType>,
    val includeIcons: Boolean,
    val includeAttachments: Boolean,
    val maxResourceBytes: Long,
    val maxTotalResourceBytes: Long,
) {
    init {
        require(includedEntryTypes.isNotEmpty()) { "At least one entry type must be selected" }
        require(maxResourceBytes > 0L) { "Maximum resource size must be positive" }
        require(maxTotalResourceBytes > 0L) { "Maximum total resource size must be positive" }
    }
}
