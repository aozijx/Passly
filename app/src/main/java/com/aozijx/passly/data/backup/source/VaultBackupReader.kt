package com.aozijx.passly.data.backup.source

import android.content.Context
import com.aozijx.passly.BuildConfig
import com.aozijx.passly.core.session.UnifiedSessionManager
import com.aozijx.passly.data.backup.BackupBundleValidator
import com.aozijx.passly.data.backup.mapper.BackupDocumentMapper
import com.aozijx.passly.data.backup.model.BackupBundle
import com.aozijx.passly.data.backup.model.BackupDocument
import com.aozijx.passly.data.backup.model.BackupResourceKind
import com.aozijx.passly.data.codec.entry.EntrySecretCodec
import com.aozijx.passly.data.codec.entry.EntrySummaryCodec
import com.aozijx.passly.data.crypto.AadProvider
import com.aozijx.passly.data.crypto.AttachmentCipher
import com.aozijx.passly.data.mapper.entry.EntryAggregateAssembler
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.security.crypto.FieldEncryptor
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.Base64
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
class VaultBackupReader @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val sessionManager: UnifiedSessionManager,
    private val summaryCodec: EntrySummaryCodec,
    private val secretCodec: EntrySecretCodec,
    private val fieldEncryptor: FieldEncryptor,
    private val documentMapper: BackupDocumentMapper
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
        return sessionManager.query {
            val metadataEntities = entryQueryDao().getAll()
                .asSequence()
                .filter { includeDeleted || it.deletedAt == null }
                .filter { it.entryType in includedEntryTypes }
                .toList()
            val entryIds = metadataEntities.map { it.entryId }
            val credentialEntities = entrySecretQueryDao().getByEntryIds(entryIds)
            val credentialMap = credentialEntities.associateBy { it.entryId }

            val entries = metadataEntities.map { metaEntity ->
                val summary = summaryCodec.decrypt(metaEntity.summaryBlob, metaEntity.entryId)
                val secret = credentialMap[metaEntity.entryId]
                    ?.let { secretCodec.decrypt(it.secretBlob, it.entryId) }
                EntryAggregateAssembler.assembleFromDatabase(metaEntity, summary, secret)
            }

            val resourceRecords =
                mutableListOf<com.aozijx.passly.data.backup.model.BackupResourceRecord>()
            val resourceData = mutableMapOf<String, ByteArray>()
            val attachmentIdsByEntry = mutableMapOf<String, MutableList<String>>()

            if (includeIcons) {
                val iconRoot = File(context.filesDir, "vault_images").canonicalFile
                entries.forEach { entry ->
                    entry.iconCustomPath?.let { iconPath ->
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
                            val recordId = "icon_${entry.id}"
                            resourceRecords.add(
                                com.aozijx.passly.data.backup.model.BackupResourceRecord(
                                    id = recordId,
                                    entryId = entry.id,
                                    kind = com.aozijx.passly.data.backup.model.BackupResourceKind.ICON,
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
                val attachmentRoot = File(context.filesDir, "attachments").canonicalFile
                entryAttachmentQueryDao().getCommittedByEntryIds(entryIds).forEach { entity ->
                    val payload = AttachmentCipher.decrypt(
                        entity.encryptedBlob,
                        entity.entryId,
                        entity.attachmentId,
                        fieldEncryptor
                    )
                    val encryptedFile = File(
                        context.filesDir,
                        "attachments/${entity.entryId}/${entity.attachmentId}.enc"
                    ).canonicalFile
                    require(
                        encryptedFile.path.startsWith(
                            attachmentRoot.path + File.separator
                        )
                    ) {
                        "附件路径超出应用附件目录: ${entity.attachmentId}"
                    }
                    require(encryptedFile.isFile) {
                        "附件文件缺失: ${entity.attachmentId}"
                    }
                    require(encryptedFile.length() <= BackupBundleValidator.MAX_RESOURCE_BYTES * 2L) {
                        "附件密文过大: ${entity.attachmentId}"
                    }
                    val encodedContent = fieldEncryptor.decrypt(
                        encryptedFile.readBytes(),
                        AadProvider.attachmentContent(entity.entryId, entity.attachmentId)
                    )
                    val content = try {
                        Base64.getDecoder().decode(encodedContent)
                    } catch (error: IllegalArgumentException) {
                        throw IllegalArgumentException(
                            "附件内容损坏: ${entity.attachmentId}",
                            error
                        )
                    }
                    require(content.size <= BackupBundleValidator.MAX_RESOURCE_BYTES) {
                        "附件过大: ${entity.attachmentId}"
                    }
                    val sha256 = BackupBundleValidator.sha256Hex(content)
                    require(payload.sha256 == null || payload.sha256.equals(sha256, true)) {
                        "附件校验失败: ${entity.attachmentId}"
                    }
                    resourceRecords += com.aozijx.passly.data.backup.model.BackupResourceRecord(
                        id = entity.attachmentId,
                        entryId = entity.entryId,
                        kind = BackupResourceKind.ATTACHMENT,
                        fileName = entity.fileName,
                        mimeType = entity.mimeType,
                        size = content.size.toLong(),
                        sha256 = sha256,
                        createdAt = entity.createdAt
                    )
                    resourceData[entity.attachmentId] = content
                    attachmentIdsByEntry.getOrPut(entity.entryId, ::mutableListOf)
                        .add(entity.attachmentId)
                }
            }

            val now = System.currentTimeMillis()
            val entryRecords = entries.map { entry ->
                documentMapper.toRecord(
                    entry = entry,
                    attachmentIds = attachmentIdsByEntry[entry.id].orEmpty()
                )
            }
            val document = BackupDocument(
                format = BackupDocument.FORMAT,
                version = BackupDocument.CURRENT_VERSION,
                exportedAt = now,
                appVersion = BuildConfig.VERSION_NAME,
                entries = entryRecords,
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
