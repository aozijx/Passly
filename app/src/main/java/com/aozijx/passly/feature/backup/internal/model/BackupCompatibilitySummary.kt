package com.aozijx.passly.feature.backup.internal.model

data class BackupCompatibilitySummary(
    val skippedEntryCount: Int = 0,
    val ignoredFieldCount: Int = 0,
    val prunedLinkCount: Int = 0,
    val prunedResourceCount: Int = 0,
)
