package com.aozijx.passly.feature.backup.contract

data class BackupImportResult(
    val importedEntryCount: Int,
    val existingEntryCount: Int,
    val skippedEntryCount: Int,
    val ignoredFieldCount: Int,
    val prunedLinkCount: Int,
    val prunedResourceCount: Int,
)
