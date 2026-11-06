package com.aozijx.passly.app.database.backup

import android.content.Context
import com.aozijx.passly.domain.backup.model.VaultBackupReadOptions
import com.aozijx.passly.domain.backup.model.VaultBackupResource
import com.aozijx.passly.domain.backup.model.VaultBackupResourceKind
import com.aozijx.passly.domain.backup.model.VaultBackupRestoreMode
import com.aozijx.passly.domain.backup.model.VaultBackupSnapshot
import com.aozijx.passly.domain.backup.port.VaultBackupStore
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.relation.EntryLinkId
import com.aozijx.passly.feature.backup.contract.ImportMode
import com.aozijx.passly.feature.backup.internal.archive.BackupArchiveKeyRegistry
import com.aozijx.passly.feature.backup.internal.archive.BackupBundleValidator
import com.aozijx.passly.feature.backup.internal.archive.mapper.KeyedEntryArchiveMapper
import com.aozijx.passly.feature.backup.internal.archive.model.BackupBundle
import com.aozijx.passly.feature.backup.internal.archive.model.BackupDocument
import com.aozijx.passly.feature.backup.internal.archive.model.BackupLinkRecord
import com.aozijx.passly.feature.backup.internal.archive.model.BackupResourceKind
import com.aozijx.passly.feature.backup.internal.archive.model.BackupResourceRecord
import com.aozijx.passly.feature.backup.internal.archive.snapshot.BackupRestoreResult
import com.aozijx.passly.feature.backup.internal.archive.snapshot.BackupSnapshotGateway
import com.aozijx.passly.feature.backup.internal.archive.snapshot.BackupSnapshotReadOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Bridges the App-owned archive contract to the persistence-neutral Domain snapshot port. */
@Singleton
internal class RoomBackupSnapshotGateway
    @Inject
    constructor(
        private val vaultBackupStore: VaultBackupStore,
        @param:ApplicationContext private val context: Context,
    ) : BackupSnapshotGateway {
        override suspend fun read(options: BackupSnapshotReadOptions): BackupBundle {
            val snapshot =
                vaultBackupStore.read(
                    VaultBackupReadOptions(
                        includeDeleted = options.includeDeleted,
                        includedEntryTypes = options.includedEntryTypes,
                        includeIcons = options.includeIcons,
                        includeAttachments = options.includeAttachments,
                        maxResourceBytes = BackupBundleValidator.MAX_RESOURCE_BYTES.toLong(),
                    ),
                )
            val attachmentIdsByEntry =
                snapshot.resources
                    .filter { it.kind == VaultBackupResourceKind.ATTACHMENT }
                    .groupBy { it.entryId.value }
                    .mapValues { (_, resources) -> resources.map(VaultBackupResource::id) }
            val records =
                snapshot.entries.map { entry ->
                    KeyedEntryArchiveMapper.toRecord(
                        entry = entry,
                        attachmentIds = attachmentIdsByEntry[entry.id.value].orEmpty(),
                    )
                }
            val resources = snapshot.resources.map(VaultBackupResource::toArchiveRecord)
            val resourceData =
                snapshot.resources.associate { resource ->
                    resource.toArchiveRecordId() to resource.content
                }
            val exportedIds = snapshot.entries.mapTo(hashSetOf()) { it.id }
            val links =
                snapshot.links
                    .filter { it.sourceEntryId in exportedIds && it.targetEntryId in exportedIds }
                    .map { link ->
                        BackupLinkRecord(
                            id = link.id.value,
                            sourceEntryId = link.sourceEntryId.value,
                            targetEntryId = link.targetEntryId.value,
                            relationType = BackupArchiveKeyRegistry.relationTypeKey(link.relationType),
                            createdAt = link.createdAt,
                            updatedAt = link.updatedAt,
                        )
                    }
            val document =
                BackupDocument(
                    format = BackupDocument.FORMAT,
                    version = BackupDocument.CURRENT_VERSION,
                    exportedAt = System.currentTimeMillis(),
                    appVersion =
                        context.packageManager
                            .getPackageInfo(context.packageName, 0)
                            .versionName
                            .orEmpty(),
                    entries = records,
                    links = links,
                    resources = resources,
                )
            return BackupBundle(
                document = document,
                resourceData = resourceData,
            ).also { bundle ->
                BackupBundleValidator.validate(bundle, requireResourceData = resources.isNotEmpty())
            }
        }

        override suspend fun restore(bundle: BackupBundle, mode: ImportMode): BackupRestoreResult {
            require(
                mode != ImportMode.OVERWRITE ||
                    bundle.sourceEntryCount == 0 ||
                    bundle.document.entries.isNotEmpty(),
            ) { "兼容导入未找到可恢复条目，已取消覆盖以保护现有数据" }
            BackupBundleValidator.validate(bundle, requireResourceData = bundle.document.resources.isNotEmpty())
            val entries = bundle.document.entries.map(KeyedEntryArchiveMapper::toEntry)
            val links =
                bundle.document.links.map { link ->
                    val relation = requireNotNull(BackupArchiveKeyRegistry.relationType(link.relationType)) {
                        "未知关系类型: ${link.relationType}"
                    }
                    EntryLink.create(
                        id = EntryLinkId(link.id),
                        sourceEntryId = EntryId(link.sourceEntryId),
                        targetEntryId = EntryId(link.targetEntryId),
                        relationType = relation,
                        createdAt = link.createdAt,
                        updatedAt = link.updatedAt,
                    )
                }
            val resources =
                bundle.document.resources.map { record ->
                    val kind =
                        when (record.kind) {
                            BackupResourceKind.ICON -> VaultBackupResourceKind.ICON
                            BackupResourceKind.ATTACHMENT -> VaultBackupResourceKind.ATTACHMENT
                        }
                    VaultBackupResource(
                        id =
                            when (record.kind) {
                                BackupResourceKind.ICON -> "icon:${record.entryId}"
                                BackupResourceKind.ATTACHMENT -> record.id
                            },
                        entryId = EntryId(record.entryId),
                        kind = kind,
                        fileName = record.fileName,
                        mimeType = record.mimeType,
                        createdAt = record.createdAt,
                        content = requireNotNull(bundle.resourceData[record.id]) {
                            "备份资源内容缺失: ${record.id}"
                        },
                    )
                }
            val result =
                vaultBackupStore.restore(
                    VaultBackupSnapshot(entries = entries, links = links, resources = resources),
                    when (mode) {
                        ImportMode.APPEND -> VaultBackupRestoreMode.APPEND
                        ImportMode.OVERWRITE -> VaultBackupRestoreMode.OVERWRITE
                    },
                )
            return BackupRestoreResult(result.importedEntryCount, result.existingEntryCount)
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
