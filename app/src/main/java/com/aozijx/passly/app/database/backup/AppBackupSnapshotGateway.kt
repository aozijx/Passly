package com.aozijx.passly.app.database.backup

import android.content.Context
import com.aozijx.passly.domain.backup.model.VaultBackupReadOptions
import com.aozijx.passly.domain.backup.model.VaultBackupResource
import com.aozijx.passly.domain.backup.model.VaultBackupResourceKind
import com.aozijx.passly.domain.backup.model.VaultBackupRestoreMode
import com.aozijx.passly.domain.backup.model.VaultBackupSnapshot
import com.aozijx.passly.domain.backup.port.VaultBackupStore
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.relation.EntryLink
import com.aozijx.passly.domain.entry.model.relation.EntryLinkId
import com.aozijx.passly.feature.backup.contract.ImportMode
import com.aozijx.passly.feature.backup.internal.archive.BackupBundleValidator
import com.aozijx.passly.feature.backup.internal.archive.mapper.KeyedEntryArchiveMapper
import com.aozijx.passly.feature.backup.internal.archive.model.BackupBundle
import com.aozijx.passly.feature.backup.internal.archive.model.BackupResourceKind
import com.aozijx.passly.feature.backup.internal.archive.snapshot.BackupRestoreResult
import com.aozijx.passly.feature.backup.internal.archive.snapshot.BackupSnapshotGateway
import com.aozijx.passly.feature.backup.internal.archive.snapshot.BackupSnapshotReadOptions
import com.aozijx.passly.feature.vault.otp.OtpRuntimeInvalidator
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Bridges the App-owned archive contract to the persistence-neutral Domain snapshot port. */
@Singleton
internal class AppBackupSnapshotGateway
    @Inject
    constructor(
        private val vaultBackupStore: VaultBackupStore,
        @param:ApplicationContext private val context: Context,
        private val otpRuntimeInvalidator: OtpRuntimeInvalidator,
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
                        maxTotalResourceBytes = BackupBundleValidator.MAX_TOTAL_RESOURCE_BYTES.toLong(),
                    ),
                )
            return BackupSnapshotArchiveMapper.toBundle(
                snapshot = snapshot,
                appVersion = {
                    context.packageManager
                        .getPackageInfo(context.packageName, 0)
                        .versionName
                        .orEmpty()
                },
                exportedAt = System.currentTimeMillis(),
            )
        }

        override suspend fun restore(
            bundle: BackupBundle,
            mode: ImportMode,
        ): BackupRestoreResult {
            require(
                mode != ImportMode.OVERWRITE ||
                    bundle.sourceEntryCount == 0 ||
                    bundle.document.entries.isNotEmpty(),
            ) { "兼容导入未找到可恢复条目，已取消覆盖以保护现有数据" }
            BackupBundleValidator.validate(bundle, requireResourceData = bundle.document.resources.isNotEmpty())
            val entries = bundle.document.entries.map(KeyedEntryArchiveMapper::toEntry)
            val links =
                bundle.document.links.map { link ->
                    EntryLink.create(
                        id = EntryLinkId(link.id),
                        firstEntryId = EntryId(link.sourceEntryId),
                        secondEntryId = EntryId(link.targetEntryId),
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
                        content =
                            requireNotNull(bundle.resourceData[record.id]) {
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
                    onCommitted = {
                        if (mode == ImportMode.OVERWRITE) otpRuntimeInvalidator.vaultReplaced()
                    },
                )
            return BackupRestoreResult(result.importedEntryCount, result.existingEntryCount)
        }
    }
