package com.aozijx.passly.domain.backup.model

data class VaultBackupRestoreResult(
    val importedEntryCount: Int,
    val existingEntryCount: Int,
) {
    init {
        require(importedEntryCount >= 0) { "Imported entry count cannot be negative" }
        require(existingEntryCount >= 0) { "Existing entry count cannot be negative" }
    }
}
