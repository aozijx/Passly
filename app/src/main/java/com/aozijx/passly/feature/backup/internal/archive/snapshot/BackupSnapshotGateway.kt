package com.aozijx.passly.feature.backup.internal.archive.snapshot

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.feature.backup.internal.archive.model.BackupBundle
import com.aozijx.passly.feature.backup.internal.model.ImportMode

internal interface BackupSnapshotGateway {
    suspend fun read(options: BackupSnapshotReadOptions): BackupBundle
    suspend fun restore(bundle: BackupBundle, mode: ImportMode)
}

internal data class BackupSnapshotReadOptions(
    val includeIcons: Boolean,
    val includeAttachments: Boolean,
    val includeDeleted: Boolean,
    val includedEntryTypes: Set<EntryType>,
)
