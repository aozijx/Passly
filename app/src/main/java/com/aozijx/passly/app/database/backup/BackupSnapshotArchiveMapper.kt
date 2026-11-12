package com.aozijx.passly.app.database.backup

import com.aozijx.passly.domain.backup.model.VaultBackupResource
import com.aozijx.passly.domain.backup.model.VaultBackupResourceKind
import com.aozijx.passly.domain.backup.model.VaultBackupSnapshot
import com.aozijx.passly.feature.backup.internal.archive.BackupBundleValidator
import com.aozijx.passly.feature.backup.internal.archive.mapper.KeyedEntryArchiveMapper
import com.aozijx.passly.feature.backup.internal.archive.model.BackupBundle
import com.aozijx.passly.feature.backup.internal.archive.model.BackupDocument
import com.aozijx.passly.feature.backup.internal.archive.model.BackupLinkRecord
import com.aozijx.passly.feature.backup.internal.archive.model.BackupResourceKind
import com.aozijx.passly.feature.backup.internal.archive.model.BackupResourceRecord

internal object BackupSnapshotArchiveMapper {
    fun toBundle(
        snapshot: VaultBackupSnapshot,
        appVersion: () -> String,
        exportedAt: Long,
    ): BackupBundle {
        var ownershipTransferred = false
        try {
            val attachmentIdsByEntry =
                snapshot.resources
                    .filter { it.kind == VaultBackupResourceKind.ATTACHMENT }
                    .groupBy { it.entryId.value }
                    .mapValues { (_, resources) -> resources.map(VaultBackupResource::id) }
            val entries =
                snapshot.entries.map { entry ->
                    KeyedEntryArchiveMapper.toRecord(
                        entry = entry,
                        attachmentIds = attachmentIdsByEntry[entry.id.value].orEmpty(),
                    )
                }
            val resources = snapshot.resources.map { it.toArchiveRecord() }
            val resourceData =
                snapshot.resources.associate { resource ->
                    resource.toArchiveRecordId() to resource.content
                }
            val exportedIds = snapshot.entries.mapTo(hashSetOf()) { it.id }
            val links =
                snapshot.links
                    .filter { it.firstEntryId in exportedIds && it.secondEntryId in exportedIds }
                    .map { link ->
                        BackupLinkRecord(
                            id = link.id.value,
                            sourceEntryId = link.firstEntryId.value,
                            targetEntryId = link.secondEntryId.value,
                            createdAt = link.createdAt,
                            updatedAt = link.updatedAt,
                        )
                    }
            val bundle =
                BackupBundle(
                    document =
                        BackupDocument(
                            format = BackupDocument.FORMAT,
                            version = BackupDocument.CURRENT_VERSION,
                            exportedAt = exportedAt,
                            appVersion = appVersion(),
                            entries = entries,
                            links = links,
                            resources = resources,
                        ),
                    resourceData = resourceData,
                )
            BackupBundleValidator.validate(bundle, requireResourceData = resources.isNotEmpty())
            ownershipTransferred = true
            return bundle
        } finally {
            if (!ownershipTransferred) snapshot.resources.forEach { it.content.fill(0) }
        }
    }

    private fun VaultBackupResource.toArchiveRecord() =
        BackupResourceRecord(
            id = toArchiveRecordId(),
            entryId = entryId.value,
            kind =
                when (kind) {
                    VaultBackupResourceKind.ICON -> BackupResourceKind.ICON
                    VaultBackupResourceKind.ATTACHMENT -> BackupResourceKind.ATTACHMENT
                },
            fileName = fileName,
            mimeType = mimeType,
            size = content.size.toLong(),
            sha256 = BackupBundleValidator.sha256Hex(content),
            createdAt = createdAt,
        )

    private fun VaultBackupResource.toArchiveRecordId(): String =
        when (kind) {
            VaultBackupResourceKind.ICON -> "icon_${entryId.value}"
            VaultBackupResourceKind.ATTACHMENT -> id
        }
}
