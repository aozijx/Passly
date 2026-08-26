package com.aozijx.passly.app.database.backup

import android.content.Context
import com.aozijx.passly.core.platform.VaultResourcePaths
import com.aozijx.passly.data.local.database.session.AppDatabaseSession
import com.aozijx.passly.feature.backup.internal.archive.BackupBundleValidator
import com.aozijx.passly.feature.backup.internal.archive.model.BackupBundle
import com.aozijx.passly.feature.backup.internal.archive.model.BackupDocument
import com.aozijx.passly.feature.backup.internal.archive.model.BackupLinkRecord
import com.aozijx.passly.feature.backup.internal.archive.model.BackupResourceKind
import com.aozijx.passly.data.mapper.entry.EntryProfileMapper
import com.aozijx.passly.data.mapper.entry.EntryAssembler
import com.aozijx.passly.data.repository.entry.SecretFieldStore
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.security.dek.AttachmentContentCrypto
import com.aozijx.passly.data.repository.attachment.AttachmentResourceGarbageCollector
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 从解锁数据库读取完整可备份数据。
 *
 * 唯一负责从数据库读取：
 * - 所有条目（包括回收站中的条目）
 * - Secret 数据
 * - 已提交附件
 * - 自定义图标
 *
 * 输出 [BackupBundle]，不关心最终格式。
 * 排除 Draft、SearchToken 等派生/临时数据。
 */
@Singleton
internal class RoomBackupSnapshotReader @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val databaseSession: AppDatabaseSession,
    private val secretFieldStore: SecretFieldStore,
    private val attachmentContentCrypto: AttachmentContentCrypto,
    private val attachmentGarbageCollector: AttachmentResourceGarbageCollector,
    private val documentMapper: RoomBackupSnapshotMapper
) {

    suspend fun readBundle(
        includeIcons: Boolean = false,
        includeAttachments: Boolean = true,
        includeDeleted: Boolean = true,
        includedEntryTypes: Set<EntryType> = EntryType.entries.toSet()
    ): BackupBundle {
        require(includedEntryTypes.isNotEmpty()) {
            "At least one entry type must be selected"
        }
        return databaseSession.query {
            val eligibleEntities = entryQueryDao().getAll()
                .asSequence()
                .filter { includeDeleted || it.deletedAt == null }
                .toList()
            val selectedEntities = eligibleEntities.filter {
                it.entryType in includedEntryTypes
            }
            val metadataEntities = selectedEntities
            val entryIds = metadataEntities.map { it.entryId }

            val entries = metadataEntities.map { metaEntity ->
                val summary = EntryProfileMapper.fromEntity(metaEntity)
                val secret = secretFieldStore.readAll(this, metaEntity.entryId)
                EntryAssembler.assembleFromDatabase(
                    metaEntity,
                    summary,
                    secret,
                )
            }

            val resourceRecords =
                mutableListOf<com.aozijx.passly.feature.backup.internal.archive.model.BackupResourceRecord>()
            val resourceData = mutableMapOf<String, ByteArray>()
            val attachmentIdsByEntry = mutableMapOf<String, MutableList<String>>()

            if (includeIcons) {
                val iconRoot = VaultResourcePaths.vaultImagesDir(context).canonicalFile
                entries.forEach { entry ->
                    entry.icon.customReference?.let { iconPath ->
                        val iconFile = File(iconPath).canonicalFile
                        require(
                            iconFile.path.startsWith(
                                iconRoot.path + File.separator
                            )
                        ) {
                            "图标路径超出应用图标目录: ${entry.id}"
                        }
                        if (iconFile.isFile) {
                            require(iconFile.length() <= BackupBundleValidator.MAX_RESOURCE_BYTES) {
                                "图标文件过大: ${entry.id}"
                            }
                            val content = iconFile.readBytes()
                            val recordId = "icon_${entry.id.value}"
                            resourceRecords.add(
                                com.aozijx.passly.feature.backup.internal.archive.model.BackupResourceRecord(
                                    id = recordId,
                                    entryId = entry.id.value,
                                    kind = com.aozijx.passly.feature.backup.internal.archive.model.BackupResourceKind.ICON,
                                    fileName = iconFile.name,
                                    mimeType = "image/png",
                                    size = content.size.toLong(),
                                    sha256 = BackupBundleValidator.sha256Hex(content)
                                )
                            )
                            resourceData[recordId] = content
                        }
                    }
                }
            }

            if (includeAttachments) {
                attachmentRefQueryDao().getCommittedByEntryIds(entryIds).forEach { entity ->
                    val resource = requireNotNull(attachmentResourceDao().getById(entity.resourceId)) {
                        "附件资源缺失: ${entity.attachmentId}"
                    }
                    val encryptedFile = attachmentGarbageCollector.resourceFile(resource.resourceId)
                    require(encryptedFile.isFile) {
                        "附件文件缺失: ${entity.attachmentId}"
                    }
                    require(encryptedFile.length() <= BackupBundleValidator.MAX_RESOURCE_BYTES * 2L) {
                        "附件密文过大: ${entity.attachmentId}"
                    }
                    val content = attachmentContentCrypto.decrypt(
                        encryptedFile.readBytes(), resource.resourceId
                    )
                    require(content.size <= BackupBundleValidator.MAX_RESOURCE_BYTES) {
                        "附件过大: ${entity.attachmentId}"
                    }
                    val sha256 = BackupBundleValidator.sha256Hex(content)
                    require(attachmentContentCrypto.verifyContentId(content, resource.resourceId)) {
                        "附件校验失败: ${entity.attachmentId}"
                    }
                    resourceRecords += com.aozijx.passly.feature.backup.internal.archive.model.BackupResourceRecord(
                        id = entity.attachmentId,
                        entryId = requireNotNull(entity.entryId),
                        kind = BackupResourceKind.ATTACHMENT,
                        fileName = entity.fileName,
                        mimeType = entity.mimeType,
                        size = content.size.toLong(),
                        sha256 = sha256,
                        createdAt = entity.createdAt
                    )
                    resourceData[entity.attachmentId] = content
                    attachmentIdsByEntry.getOrPut(requireNotNull(entity.entryId), ::mutableListOf)
                        .add(entity.attachmentId)
                }
            }

            val now = System.currentTimeMillis()
            val exportedEntryIds = entries.mapTo(hashSetOf()) { it.id.value }
            val entryRecords = entries.map { entry ->
                documentMapper.toRecord(
                    entry = entry,
                    attachmentIds = attachmentIdsByEntry[entry.id.value].orEmpty()
                )
            }
            val linkRecords = entryLinkQueryDao().getAll()
                .filter { it.sourceEntryId in exportedEntryIds && it.targetEntryId in exportedEntryIds }
                .map { link ->
                    BackupLinkRecord(
                        id = link.linkId,
                        sourceEntryId = link.sourceEntryId,
                        targetEntryId = link.targetEntryId,
                        relationType = link.relationType.name,
                        createdAt = link.createdAt,
                        updatedAt = link.updatedAt
                    )
                }
            val document = BackupDocument(
                format = BackupDocument.FORMAT,
                version = BackupDocument.CURRENT_VERSION,
                exportedAt = now,
                appVersion = context.packageManager
                    .getPackageInfo(context.packageName, 0)
                    .versionName
                    .orEmpty(),
                entries = entryRecords,
                links = linkRecords,
                resources = resourceRecords
            )

            val bundle = BackupBundle(
                document = document,
                resourceData = resourceData
            )
            BackupBundleValidator.validate(
                bundle,
                requireResourceData = resourceRecords.isNotEmpty()
            )
            bundle
        }
    }
}
