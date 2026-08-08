package com.aozijx.passly.data.backup.source

import android.content.Context
import com.aozijx.passly.app.diagnostics.AppTelemetry
import com.aozijx.passly.core.platform.VaultResourcePaths
import com.aozijx.passly.core.session.UnifiedSessionManager
import com.aozijx.passly.data.backup.BackupBundleValidator
import com.aozijx.passly.data.backup.mapper.BackupDocumentMapper
import com.aozijx.passly.data.backup.model.BackupBundle
import com.aozijx.passly.data.backup.model.BackupResourceKind
import com.aozijx.passly.data.codec.entry.EntryHighSensitivitySecretCodec
import com.aozijx.passly.data.codec.entry.EntrySecretCodec
import com.aozijx.passly.data.codec.entry.EntrySummaryCodec
import com.aozijx.passly.data.crypto.AadProvider
import com.aozijx.passly.data.crypto.AttachmentCipher
import com.aozijx.passly.data.local.database.maintenance.VaultDatabaseCleaner
import com.aozijx.passly.data.model.entity.EntryAttachmentEntity
import com.aozijx.passly.data.model.entity.EntryEntity
import com.aozijx.passly.data.model.entity.EntrySecretEntity
import com.aozijx.passly.data.model.payload.attachment.AttachmentPayload
import com.aozijx.passly.domain.backup.model.ImportMode
import com.aozijx.passly.domain.entry.model.EntryCapabilityFlags
import com.aozijx.passly.domain.entry.model.attachment.AttachmentStatus
import com.aozijx.passly.domain.entry.model.extractHighSensitivity
import com.aozijx.passly.domain.entry.model.withoutHighSensitivity
import com.aozijx.passly.security.crypto.FieldEncryptor
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 备份恢复器。
 *
 * 唯一负责将 [BackupBundle] 写入数据库：
 * - 校验 entryId/type/version
 * - 生成 Summary/Secret 密文
 * - 写 Entry、Secret
 * - OVERWRITE 时在同一个事务中清库并插入
 * - 重建或标记 Blind Index 待重建
 */
@Singleton
class VaultBackupRestorer @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val sessionManager: UnifiedSessionManager,
    private val databaseCleaner: VaultDatabaseCleaner,
    private val summaryCodec: EntrySummaryCodec,
    private val secretCodec: EntrySecretCodec,
    private val highSensitivitySecretCodec: EntryHighSensitivitySecretCodec,
    private val documentMapper: BackupDocumentMapper,
    private val fieldEncryptor: FieldEncryptor
) {

    suspend fun restore(bundle: BackupBundle, mode: ImportMode) {
        BackupBundleValidator.validate(
            bundle,
            requireResourceData = bundle.document.resources.isNotEmpty()
        )
        val resourcesByEntry = bundle.document.resources.groupBy { it.entryId }
        val fileJournal = RestoreFileJournal()
        val restoredFiles = mutableSetOf<String>()

        try {
            sessionManager.transaction {
                if (mode == ImportMode.OVERWRITE) {
                    databaseCleaner.clearVaultData()
                }

                val orderedRecords = bundle.document.entries.sortedBy {
                    if (it.type == com.aozijx.passly.domain.entry.model.EntryType.ACCOUNT.name) 0 else 1
                }
                orderedRecords.forEach { record ->
                    val entryId = record.id
                    if (mode == ImportMode.APPEND && entryQueryDao().exists(entryId)) {
                        if (
                            record.type ==
                            com.aozijx.passly.domain.entry.model.EntryType.ACCOUNT.name
                        ) {
                            val existing = requireNotNull(entryQueryDao().getById(entryId))
                            require(
                                existing.entryType ==
                                    com.aozijx.passly.domain.entry.model.EntryType.ACCOUNT &&
                                    existing.parentEntryId == null &&
                                    existing.vaultId == record.vaultId
                            ) {
                                "已存在的父账户与备份层级不兼容: $entryId"
                            }
                        }
                        return@forEach
                    }
                    record.parentEntryId?.let { parentEntryId ->
                        val parent = requireNotNull(entryQueryDao().getById(parentEntryId)) {
                            "恢复时找不到父账户: $parentEntryId"
                        }
                        require(
                            parent.entryType ==
                                com.aozijx.passly.domain.entry.model.EntryType.ACCOUNT &&
                                parent.parentEntryId == null &&
                                parent.vaultId == record.vaultId
                        ) {
                            "恢复时父账户层级无效: $parentEntryId"
                        }
                    }
                    val restoredEntry = documentMapper.toEntry(record)
                    val entryResources = resourcesByEntry[entryId].orEmpty()
                    val iconRecord = entryResources.singleOrNull {
                        it.kind == BackupResourceKind.ICON
                    }
                    val iconPath = iconRecord?.let { resource ->
                        val content = requireNotNull(bundle.resourceData[resource.id])
                        val iconDir = VaultResourcePaths.vaultImagesDir(context).apply { mkdirs() }
                        val target = File(
                            iconDir,
                            "restored_${
                                BackupBundleValidator.sha256Hex(resource.id.toByteArray()).take(32)
                            }.bin"
                        )
                        fileJournal.replace(target, content)
                        restoredFiles += target.canonicalPath
                        target.absolutePath
                    }
                    val summary = restoredEntry.summary.copy(iconCustomPath = iconPath)
                    val secret = restoredEntry.fullSecret
                    val highSensitivitySecret = restoredEntry.highSensitivitySecret
                        ?: secret.extractHighSensitivity()
                    val persistedSecret = secret.withoutHighSensitivity()
                    val metaBlob = summaryCodec.encrypt(summary, entryId)
                    val credBlob = secretCodec.encrypt(persistedSecret, entryId)
                    val highSensitivityBlob = highSensitivitySecret
                        .takeUnless { it.isEmpty }
                        ?.let { highSensitivitySecretCodec.encrypt(it, entryId) }

                    val attachmentResources = entryResources.filter {
                        it.kind == BackupResourceKind.ATTACHMENT
                    }
                    val capabilityFlags = EntryCapabilityFlags.computeFrom(
                        secret = secret,
                        hasAttachments = attachmentResources.isNotEmpty()
                    )
                    val otpType = EntryCapabilityFlags.otpTypeFrom(secret)

                    val metaEntity = EntryEntity(
                        entryId = entryId,
                        vaultId = record.vaultId,
                        parentEntryId = record.parentEntryId,
                        entryType = com.aozijx.passly.domain.entry.model.EntryType.valueOf(record.type),
                        version = record.version,
                        capabilityFlags = capabilityFlags,
                        otpType = otpType,
                        summaryBlob = metaBlob,
                        createdAt = record.createdAt,
                        updatedAt = record.updatedAt,
                        deletedAt = record.deletedAt
                    )

                    val credEntity = EntrySecretEntity(
                        entryId = entryId,
                        secretBlob = credBlob,
                        highSensitivityBlob = highSensitivityBlob
                    )

                    entryCommandDao().insertStrict(metaEntity)
                    entrySecretCommandDao().insertStrict(credEntity)

                    attachmentResources.forEach { resource ->
                        val content = requireNotNull(bundle.resourceData[resource.id])
                        val attachmentDir =
                            File(
                                context.filesDir,
                                "${VaultResourcePaths.ATTACHMENTS}/$entryId"
                            ).apply { mkdirs() }
                        val target = File(attachmentDir, "${resource.id}.enc")
                        val encryptedContent = fieldEncryptor.encrypt(
                            Base64.getEncoder().encodeToString(content),
                            AadProvider.attachmentContent(entryId, resource.id)
                        )
                        try {
                            fileJournal.replace(target, encryptedContent)
                            restoredFiles += target.canonicalPath
                        } finally {
                            encryptedContent.fill(0)
                        }

                        val payload = AttachmentPayload(
                            attachmentId = resource.id,
                            fileName = resource.fileName.orEmpty(),
                            mimeType = resource.mimeType.orEmpty(),
                            fileSize = resource.size,
                            encryptedPath = "$entryId/${resource.id}.enc",
                            sha256 = resource.sha256,
                            createdAt = resource.createdAt ?: record.updatedAt
                        )
                        entryAttachmentCommandDao().insertStrict(
                            EntryAttachmentEntity(
                                attachmentId = resource.id,
                                entryId = entryId,
                                fileName = resource.fileName.orEmpty(),
                                fileSize = resource.size,
                                mimeType = resource.mimeType,
                                status = AttachmentStatus.COMMITTED.name,
                                owner = entryId,
                                encryptedBlob = AttachmentCipher.encrypt(
                                    payload,
                                    entryId,
                                    resource.id,
                                    fieldEncryptor
                                ),
                                createdAt = resource.createdAt ?: record.updatedAt
                            )
                        )
                    }
                }
            }
            fileJournal.commit()
            if (mode == ImportMode.OVERWRITE) {
                runCatching {
                    cleanupUnreferencedFiles(
                        roots = VaultResourcePaths.resourceDirectories(context),
                        retainedCanonicalPaths = restoredFiles
                    )
                }.onFailure {
                    AppTelemetry.w("VaultBackupRestorer", "恢复成功，但旧资源清理未完全完成")
                }
            }
        } catch (error: Throwable) {
            fileJournal.rollback()
            throw error
        }
    }

    private fun cleanupUnreferencedFiles(
        roots: List<File>,
        retainedCanonicalPaths: Set<String>
    ) {
        roots.filter(File::isDirectory).forEach { root ->
            root.walkBottomUp().forEach candidateLoop@{ candidate ->
                if (candidate == root) return@candidateLoop
                if (candidate.isFile && candidate.canonicalPath !in retainedCanonicalPaths) {
                    if (!candidate.delete()) {
                        AppTelemetry.w("VaultBackupRestorer", "无法删除未引用恢复文件: ${candidate.name}")
                    }
                } else if (candidate.isDirectory) {
                    candidate.delete()
                }
            }
        }
    }
}
