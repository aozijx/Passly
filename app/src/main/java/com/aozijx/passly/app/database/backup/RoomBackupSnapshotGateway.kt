package com.aozijx.passly.app.database.backup

import com.aozijx.passly.feature.backup.internal.archive.model.BackupBundle
import com.aozijx.passly.feature.backup.internal.archive.snapshot.BackupSnapshotGateway
import com.aozijx.passly.feature.backup.internal.archive.snapshot.BackupSnapshotReadOptions
import com.aozijx.passly.feature.backup.internal.model.ImportMode
import javax.inject.Inject

internal class RoomBackupSnapshotGateway @Inject constructor(
    private val reader: RoomBackupSnapshotReader,
    private val restorer: RoomBackupSnapshotRestorer,
) : BackupSnapshotGateway {
    override suspend fun read(options: BackupSnapshotReadOptions) = reader.readBundle(
        includeIcons = options.includeIcons,
        includeAttachments = options.includeAttachments,
        includeDeleted = options.includeDeleted,
        includedEntryTypes = options.includedEntryTypes,
    )

    override suspend fun restore(bundle: BackupBundle, mode: ImportMode) =
        restorer.restore(bundle, mode)
}
