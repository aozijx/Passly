package com.aozijx.passly.app.database.backup

import android.content.Context
import com.aozijx.passly.core.platform.VaultResourcePaths
import com.aozijx.passly.core.telemetry.EventCategory
import com.aozijx.passly.core.telemetry.EventLevel
import com.aozijx.passly.core.telemetry.TelemetryReporter
import com.aozijx.passly.core.telemetry.report
import com.aozijx.passly.data.local.database.session.AppDatabaseSession
import com.aozijx.passly.feature.backup.internal.archive.BackupBundleValidator
import com.aozijx.passly.feature.backup.internal.archive.model.BackupBundle
import com.aozijx.passly.feature.backup.internal.archive.model.BackupResourceKind
import com.aozijx.passly.data.local.database.maintenance.DatabaseCleaner
import com.aozijx.passly.data.local.database.entity.AttachmentResourceEntity
import com.aozijx.passly.data.local.database.entity.AttachmentResourceState
import com.aozijx.passly.data.local.database.entity.AttachmentRefEntity
import com.aozijx.passly.data.local.database.entity.EntryEntity
import com.aozijx.passly.data.local.database.entity.EntryLinkEntity
import com.aozijx.passly.data.repository.entry.SecretFieldStore
import com.aozijx.passly.feature.backup.internal.model.ImportMode
import com.aozijx.passly.feature.backup.internal.archive.snapshot.RestoreFileJournal
import com.aozijx.passly.domain.entry.model.query.EntryCapabilities
import com.aozijx.passly.data.mapper.entry.toDatabaseFlags
import com.aozijx.passly.domain.entry.model.attachment.AttachmentStatus
import com.aozijx.passly.domain.entry.model.relation.EntryRelationType
import com.aozijx.passly.security.dek.AttachmentContentCrypto
import com.aozijx.passly.data.repository.attachment.AttachmentResourceGarbageCollector
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
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
internal class RoomBackupSnapshotRestorer @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val databaseSession: AppDatabaseSession,
    private val databaseCleaner: DatabaseCleaner,
    private val secretFieldStore: SecretFieldStore,
    private val documentMapper: RoomBackupSnapshotMapper,
    private val attachmentContentCrypto: AttachmentContentCrypto,
    private val attachmentGarbageCollector: AttachmentResourceGarbageCollector,
    private val telemetry: TelemetryReporter,
) {

    suspend fun restore(bundle: BackupBundle, mode: ImportMode) =
        attachmentGarbageCollector.withMutationLock {
        BackupBundleValidator.validate(
            bundle,
            requireResourceData = bundle.document.resources.isNotEmpty()
        )
        val resourcesByEntry = bundle.document.resources.groupBy { it.entryId }
        val fileJournal = RestoreFileJournal()
        val restoredFiles = mutableSetOf<String>()

        try {
            databaseSession.transaction {
                if (mode == ImportMode.OVERWRITE) {
                    databaseCleaner.clearAllData()
                }

                bundle.document.entries.forEach { record ->
                    val entryId = record.id
                    if (mode == ImportMode.APPEND && entryQueryDao().exists(entryId)) {
                        return@forEach
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
                            }${resource.mimeType?.imageFileExtension() ?: ".png"}"
                        )
                        fileJournal.replace(target, content)
                        restoredFiles += target.canonicalPath
                        target.absolutePath
                    }
                    val profile = restoredEntry.profile.copy(
                        icon = restoredEntry.profile.icon.copy(customReference = iconPath),
                    )
                    val secret = restoredEntry.secret
                    val attachmentResources = entryResources.filter {
                        it.kind == BackupResourceKind.ATTACHMENT
                    }
                    val capabilityFlags = EntryCapabilities.from(
                        secret = secret,
                        hasAttachments = attachmentResources.isNotEmpty()
                    ).toDatabaseFlags()
                    val otpType = secret.otp?.config?.type

                    val metaEntity = EntryEntity(
                        entryId = entryId,
                        entryType = com.aozijx.passly.domain.entry.model.EntryType.valueOf(record.type),
                        version = record.version,
                        capabilityFlags = capabilityFlags,
                        otpType = otpType?.name,
                        title = profile.title,
                        username = profile.username,
                        primaryUrl = profile.associations.primaryUrl,
                        domains = profile.associations.domains,
                        applicationIds = profile.associations.applicationIds,
                        iconName = profile.icon.name,
                        iconCustomReference = profile.icon.customReference,
                        favorite = profile.favorite,
                        tags = profile.tags,
                        iconColor = profile.icon.color,
                        expiresAt = profile.expiresAtMs,
                        createdAt = record.createdAt,
                        updatedAt = record.updatedAt,
                        deletedAt = record.deletedAt
                    )

                    entryCommandDao().insertStrict(metaEntity)
                    secretFieldStore.replaceAll(this, entryId, secret)

                    attachmentResources.forEach { resource ->
                        val content = requireNotNull(bundle.resourceData[resource.id])
                        val resourceId = attachmentContentCrypto.contentId(content)
                        val existingResource = attachmentResourceDao().getById(resourceId)
                        if (existingResource == null) {
                            attachmentResourceDao().insertStrict(
                                AttachmentResourceEntity(
                                    resourceId = resourceId,
                                    fileSize = content.size.toLong(),
                                    createdAt = resource.createdAt ?: record.updatedAt,
                                )
                            )
                        } else {
                            require(existingResource.fileSize == content.size.toLong()) {
                                "附件资源哈希碰撞: ${resource.id}"
                            }
                            if (existingResource.lifecycleState != AttachmentResourceState.ACTIVE) {
                                attachmentGarbageCollector.reactivateInTransaction(this, resourceId)
                            }
                        }
                        val target = attachmentGarbageCollector.resourceFile(resourceId)
                        target.parentFile?.mkdirs()
                        if (!target.isFile) {
                            val encryptedContent = attachmentContentCrypto.encrypt(content, resourceId)
                            try {
                                fileJournal.replace(target, encryptedContent)
                                restoredFiles += target.canonicalPath
                            } finally {
                                encryptedContent.fill(0)
                            }
                        }

                        attachmentRefCommandDao().insertStrict(
                            AttachmentRefEntity(
                                attachmentId = resource.id,
                                entryId = entryId,
                                resourceId = resourceId,
                                fileName = resource.fileName.orEmpty(),
                                mimeType = resource.mimeType,
                                status = AttachmentStatus.COMMITTED.name,
                                stagingOwnerId = null,
                                createdAt = resource.createdAt ?: record.updatedAt,
                            )
                        )
                    }
                }
                bundle.document.links.forEach { link ->
                    if (
                        entryQueryDao().exists(link.sourceEntryId) &&
                        entryQueryDao().exists(link.targetEntryId)
                    ) {
                        entryLinkCommandDao().upsert(
                            EntryLinkEntity(
                                linkId = link.id,
                                sourceEntryId = link.sourceEntryId,
                                targetEntryId = link.targetEntryId,
                                relationType = EntryRelationType.valueOf(link.relationType),
                                createdAt = link.createdAt,
                                updatedAt = link.updatedAt
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
                }.onFailure { error ->
                    report("backup.resource_cleanup_failed", error)
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
                        report("backup.resource_delete_failed")
                    }
                } else if (candidate.isDirectory) {
                    candidate.delete()
                }
            }
        }
    }

    private fun report(name: String, throwable: Throwable? = null) {
        telemetry.report(EventLevel.WARN, EventCategory.BACKUP, name, throwable)
    }
}

private fun String.imageFileExtension(): String = when (lowercase()) {
    "image/webp" -> ".webp"
    "image/jpeg" -> ".jpg"
    "image/gif" -> ".gif"
    "image/png" -> ".png"
    else -> error("Unsupported icon MIME type")
}
