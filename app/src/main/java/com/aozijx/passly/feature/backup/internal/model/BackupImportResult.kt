package com.aozijx.passly.feature.backup.internal.model

data class BackupImportResult(
    val importedEntryCount: Int,
    val existingEntryCount: Int,
    val skippedEntryCount: Int,
    val ignoredFieldCount: Int,
    val prunedLinkCount: Int,
    val prunedResourceCount: Int,
)

data class BackupCompatibilitySummary(
    val skippedEntryCount: Int = 0,
    val ignoredFieldCount: Int = 0,
    val prunedLinkCount: Int = 0,
    val prunedResourceCount: Int = 0,
)
