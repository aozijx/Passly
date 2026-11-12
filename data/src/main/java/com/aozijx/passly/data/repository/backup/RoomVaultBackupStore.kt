package com.aozijx.passly.data.repository.backup

import android.content.Context
import com.aozijx.passly.core.crypto.AttachmentContentProtector
import com.aozijx.passly.core.error.model.SessionModeRestricted
import com.aozijx.passly.core.observability.LogStatements
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.Operations
import com.aozijx.passly.core.platform.VaultResourcePaths
import com.aozijx.passly.data.local.database.AppDatabase
import com.aozijx.passly.data.local.database.entity.AttachmentRefEntity
import com.aozijx.passly.data.local.database.entity.AttachmentResourceEntity
import com.aozijx.passly.data.local.database.entity.AttachmentResourceState
import com.aozijx.passly.data.local.database.entity.EntryEntity
import com.aozijx.passly.data.local.database.entity.EntryLinkEntity
import com.aozijx.passly.data.local.database.maintenance.clearAllVaultData
import com.aozijx.passly.data.local.database.session.AppDatabaseSession
import com.aozijx.passly.data.mapper.entry.EntryAssembler
import com.aozijx.passly.data.mapper.entry.EntryProfileMapper
import com.aozijx.passly.data.mapper.entry.toDatabaseFlags
import com.aozijx.passly.data.repository.attachment.AttachmentResourceGarbageCollector
import com.aozijx.passly.data.repository.entry.SecretFieldStore
import com.aozijx.passly.domain.backup.model.VaultBackupReadOptions
import com.aozijx.passly.domain.backup.model.VaultBackupResource
import com.aozijx.passly.domain.backup.model.VaultBackupResourceKind
import com.aozijx.passly.domain.backup.model.VaultBackupRestoreMode
import com.aozijx.passly.domain.backup.model.VaultBackupRestoreResult
import com.aozijx.passly.domain.backup.model.VaultBackupSnapshot
import com.aozijx.passly.domain.backup.port.VaultBackupStore
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.query.EntryCapabilities
import com.aozijx.passly.domain.entry.model.relation.EntryLink
import com.aozijx.passly.domain.entry.model.relation.EntryLinkId
import com.aozijx.passly.domain.entry.model.relation.EntryRelationType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

private const val WEBP_EXTENSION = "webp"
private const val WEBP_MIME_TYPE = "image/webp"

@Singleton
internal class RoomVaultBackupStore
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val databaseSession: AppDatabaseSession,
        private val secretFieldStore: SecretFieldStore,
        private val attachmentContentProtector: AttachmentContentProtector,
        private val attachmentGarbageCollector: AttachmentResourceGarbageCollector,
        private val logger: Logger,
    ) : VaultBackupStore {
        override suspend fun read(options: VaultBackupReadOptions): VaultBackupSnapshot {
            val resourceAccumulator = VaultBackupResourceAccumulator(options)
            return try {
                val (entries, links) =
                    databaseSession.queryFullVault(
                        onDenied = { throw SessionModeRestricted() },
                    ) {
                        val selectedEntities =
                            entryQueryDao()
                                .getAll()
                                .asSequence()
                                .filter { options.includeDeleted || it.deletedAt == null }
                                .filter { it.entryType in options.includedEntryTypes }
                                .toList()
                        val entries =
                            selectedEntities.map { entity ->
                                EntryAssembler.assembleFromDatabase(
                                    entity,
                                    EntryProfileMapper.fromEntity(entity),
                                    secretFieldStore.readAll(this, entity.entryId, entity.entryType),
                                )
                            }
                        val entryIds = entries.mapTo(hashSetOf()) { it.id.value }

                        if (options.includeIcons) {
                            val iconRoot = VaultResourcePaths.vaultImagesDir(context).canonicalFile
                            entries.forEach { entry ->
                                entry.icon.customReference?.let { iconPath ->
                                    readIcon(
                                        entryId = entry.id,
                                        path = iconPath,
                                        root = iconRoot,
                                        maxBytes = options.maxResourceBytes,
                                    )?.let(resourceAccumulator::add)
                                }
                            }
                        }

                        if (options.includeAttachments && entryIds.isNotEmpty()) {
                            attachmentRefQueryDao().getByEntryIds(entryIds.toList()).forEach { reference ->
                                val resource =
                                    requireNotNull(attachmentResourceDao().getById(reference.resourceId)) {
                                        "附件资源缺失: ${reference.attachmentId}"
                                    }
                                val encryptedFile = attachmentGarbageCollector.resourceFile(resource.resourceId)
                                require(encryptedFile.isFile) { "附件文件缺失: ${reference.attachmentId}" }
                                require(encryptedFile.length() <= options.maxResourceBytes * 2L) {
                                    "附件密文过大: ${reference.attachmentId}"
                                }
                                val encrypted = encryptedFile.readBytes()
                                val content =
                                    try {
                                        attachmentContentProtector.decrypt(encrypted, resource.resourceId)
                                    } finally {
                                        encrypted.fill(0)
                                    }
                                try {
                                    if (content.size.toLong() > options.maxResourceBytes) {
                                        throw IllegalArgumentException("附件过大: ${reference.attachmentId}")
                                    }
                                    if (!attachmentContentProtector.verifyContentId(content, resource.resourceId)) {
                                        throw IllegalArgumentException("附件校验失败: ${reference.attachmentId}")
                                    }
                                    resourceAccumulator.add(
                                        VaultBackupResource(
                                            id = reference.attachmentId,
                                            entryId = EntryId(requireNotNull(reference.entryId)),
                                            kind = VaultBackupResourceKind.ATTACHMENT,
                                            fileName = reference.fileName,
                                            mimeType = reference.mimeType,
                                            createdAt = reference.createdAt,
                                            content = content,
                                        ),
                                    )
                                } catch (failure: Throwable) {
                                    content.fill(0)
                                    throw failure
                                }
                            }
                        }

                        val links =
                            entryLinkQueryDao()
                                .getAll()
                                .asSequence()
                                .filter { it.sourceEntryId in entryIds && it.targetEntryId in entryIds }
                                .map { link ->
                                    EntryLink.create(
                                        id = EntryLinkId(link.linkId),
                                        sourceEntryId = EntryId(link.sourceEntryId),
                                        targetEntryId = EntryId(link.targetEntryId),
                                        relationType = link.relationType,
                                        createdAt = link.createdAt,
                                        updatedAt = link.updatedAt,
                                    )
                                }.toList()
                        entries to links
                    }
                resourceAccumulator.toSnapshot(entries, links)
            } catch (failure: Throwable) {
                resourceAccumulator.fail(failure)
            }
        }

        override suspend fun restore(
            snapshot: VaultBackupSnapshot,
            mode: VaultBackupRestoreMode,
            onCommitted: suspend () -> Unit,
        ): VaultBackupRestoreResult =
            logger.inSpan(Operations.BackupRestore) {
                attachmentGarbageCollector.withMutationLock {
                    require(
                        snapshot.entries
                            .map { it.id }
                            .distinct()
                            .size == snapshot.entries.size,
                    ) {
                        "Snapshot contains duplicate entry IDs"
                    }
                    require(
                        snapshot.resources
                            .map { it.id }
                            .distinct()
                            .size == snapshot.resources.size,
                    ) {
                        "Snapshot contains duplicate resource IDs"
                    }
                    val snapshotEntryIds = snapshot.entries.mapTo(hashSetOf()) { it.id }
                    require(snapshot.resources.all { it.entryId in snapshotEntryIds }) {
                        "Snapshot contains a resource for an unknown entry"
                    }
                    require(
                        snapshot.links.all {
                            it.sourceEntryId in snapshotEntryIds && it.targetEntryId in snapshotEntryIds
                        },
                    ) { "Snapshot contains a link for an unknown entry" }
                    require(
                        snapshot.resources
                            .filter { it.kind == VaultBackupResourceKind.ICON }
                            .groupingBy(VaultBackupResource::entryId)
                            .eachCount()
                            .values
                            .all { it == 1 },
                    ) { "Snapshot contains duplicate entry icons" }

                    val resourcesByEntry = snapshot.resources.groupBy { it.entryId }
                    val fileJournal = VaultRestoreFileJournal()
                    val retainedCanonicalPaths = mutableSetOf<String>()
                    var importedEntryCount = 0
                    var existingEntryCount = 0

                    try {
                        databaseSession.transactionFullVault(
                            onDenied = { throw SessionModeRestricted() },
                        ) {
                            if (mode == VaultBackupRestoreMode.OVERWRITE) clearAllVaultData()

                            snapshot.entries.forEach { sourceEntry ->
                                val entryId = sourceEntry.id.value
                                if (mode == VaultBackupRestoreMode.APPEND && entryQueryDao().exists(entryId)) {
                                    existingEntryCount++
                                    return@forEach
                                }

                                val entryResources = resourcesByEntry[sourceEntry.id].orEmpty()
                                val icon =
                                    entryResources.singleOrNull {
                                        it.kind == VaultBackupResourceKind.ICON
                                    }
                                val entry = sourceEntry.withRestoredIcon(icon, retainedCanonicalPaths, fileJournal)
                                val attachments =
                                    entryResources.filter { it.kind == VaultBackupResourceKind.ATTACHMENT }
                                val capabilityFlags =
                                    EntryCapabilities
                                        .from(
                                            secret = entry.secret,
                                            hasAttachments = attachments.isNotEmpty(),
                                        ).toDatabaseFlags()
                                val profile = entry.profile
                                val timestamps = entry.identity.timestamps
                                val metaEntity =
                                    EntryEntity(
                                        entryId = entryId,
                                        entryType = entry.type,
                                        version = entry.version.value,
                                        capabilityFlags = capabilityFlags,
                                        otpType =
                                            entry.secret.otp
                                                ?.config
                                                ?.type,
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
                                        createdAt = timestamps.createdAtMs,
                                        updatedAt = timestamps.updatedAtMs,
                                        deletedAt = timestamps.deletedAtMs,
                                    )

                                entryCommandDao().insertStrict(metaEntity)
                                secretFieldStore.replaceAll(this, entryId, entry.secret, entry.type)
                                attachments.forEach { attachment ->
                                    restoreAttachment(this, attachment, entry, fileJournal, retainedCanonicalPaths)
                                }
                                importedEntryCount++
                            }

                            snapshot.links.forEach { link ->
                                if (
                                    entryQueryDao().exists(link.sourceEntryId.value) &&
                                    entryQueryDao().exists(link.targetEntryId.value)
                                ) {
                                    entryLinkCommandDao().upsert(
                                        EntryLinkEntity(
                                            linkId = link.id.value,
                                            sourceEntryId = link.sourceEntryId.value,
                                            targetEntryId = link.targetEntryId.value,
                                            relationType = link.relationType,
                                            createdAt = link.createdAt,
                                            updatedAt = link.updatedAt,
                                        ),
                                    )
                                }
                            }
                        }
                    } catch (error: Throwable) {
                        try {
                            fileJournal.rollback()
                        } catch (rollbackFailure: Throwable) {
                            val preferredFailure = preferVaultRestoreFailure(error, rollbackFailure)
                            if (preferredFailure !== error) throw preferredFailure
                        }
                        throw error
                    }

                    // The Room transaction is durable. Notify derived-state owners before
                    // any post-commit file work can fail or observe cancellation.
                    onCommitted()

                    try {
                        fileJournal.commit()
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Error) {
                        throw error
                    } catch (error: Exception) {
                        reportBestEffortBackupCleanupFailure {
                            logger.log(LogStatements.Backup.resourceCleanupFailed(error))
                        }
                    }
                    if (mode == VaultBackupRestoreMode.OVERWRITE) {
                        try {
                            cleanupUnreferencedFiles(retainedCanonicalPaths)
                        } catch (error: CancellationException) {
                            throw error
                        } catch (error: Error) {
                            throw error
                        } catch (error: Exception) {
                            reportBestEffortBackupCleanupFailure {
                                logger.log(LogStatements.Backup.resourceCleanupFailed(error))
                            }
                        }
                    }
                    VaultBackupRestoreResult(importedEntryCount, existingEntryCount)
                }
            }

        private suspend fun readIcon(
            entryId: EntryId,
            path: String,
            root: File,
            maxBytes: Long,
        ): VaultBackupResource? {
            val iconFile = File(path).canonicalFile
            require(iconFile != root && iconFile.toPath().startsWith(root.toPath())) {
                "图标路径超出应用图标目录: ${entryId.value}"
            }
            if (!iconFile.isFile) return null
            require(iconFile.extension.equals(WEBP_EXTENSION, ignoreCase = true)) {
                "图标不是 WebP 格式: ${entryId.value}"
            }
            require(iconFile.length() <= maxBytes) { "图标文件过大: ${entryId.value}" }
            val content = iconFile.readBytes()
            return try {
                VaultBackupResource(
                    id = entryId.value,
                    entryId = entryId,
                    kind = VaultBackupResourceKind.ICON,
                    fileName = iconFile.name,
                    mimeType = WEBP_MIME_TYPE,
                    createdAt = null,
                    content = content,
                )
            } catch (failure: Throwable) {
                content.fill(0)
                throw failure
            }
        }

        private suspend fun restoreAttachment(
            db: AppDatabase,
            resource: VaultBackupResource,
            entry: Entry,
            fileJournal: VaultRestoreFileJournal,
            retainedCanonicalPaths: MutableSet<String>,
        ) {
            require(resource.entryId == entry.id && resource.kind == VaultBackupResourceKind.ATTACHMENT)
            val content = resource.content
            val resourceId = attachmentContentProtector.contentId(content)
            val existingResource = db.attachmentResourceDao().getById(resourceId)
            if (existingResource == null) {
                db.attachmentResourceDao().insertStrict(
                    AttachmentResourceEntity(
                        resourceId = resourceId,
                        fileSize = content.size.toLong(),
                        createdAt = resource.createdAt ?: entry.updatedAt,
                    ),
                )
            } else {
                require(existingResource.fileSize == content.size.toLong()) {
                    "附件资源哈希碰撞: ${resource.id}"
                }
                if (existingResource.lifecycleState != AttachmentResourceState.ACTIVE) {
                    attachmentGarbageCollector.reactivateInTransaction(db, resourceId)
                }
            }
            val target = attachmentGarbageCollector.resourceFile(resourceId)
            target.parentFile?.mkdirs()
            retainRestoredAttachmentFile(
                target = target,
                content = content,
                resourceId = resourceId,
                attachmentContentProtector = attachmentContentProtector,
                fileJournal = fileJournal,
                retainedCanonicalPaths = retainedCanonicalPaths,
            )
            db.attachmentRefCommandDao().insertStrict(
                AttachmentRefEntity(
                    attachmentId = resource.id,
                    entryId = entry.id.value,
                    resourceId = resourceId,
                    fileName = resource.fileName.orEmpty(),
                    mimeType = resource.mimeType,
                    createdAt = resource.createdAt ?: entry.updatedAt,
                ),
            )
        }

        private fun Entry.withRestoredIcon(
            icon: VaultBackupResource?,
            retainedCanonicalPaths: MutableSet<String>,
            fileJournal: VaultRestoreFileJournal,
        ): Entry {
            if (icon == null) return copy(profile = profile.copy(icon = profile.icon.copy(customReference = null)))
            require(icon.entryId == id && icon.kind == VaultBackupResourceKind.ICON)
            require(icon.mimeType == WEBP_MIME_TYPE) { "图标不是 WebP 格式: ${id.value}" }
            require(icon.content.size.toLong() <= MAX_ICON_BYTES) { "图标文件过大: ${id.value}" }
            val root = VaultResourcePaths.vaultImagesDir(context).canonicalFile.apply { mkdirs() }
            val name = "favicon_restored_${sha256Hex("icon_${id.value}").take(32)}.$WEBP_EXTENSION"
            val target = File(root, name).canonicalFile
            require(target.parentFile?.canonicalPath == root.canonicalPath) { "恢复图标路径无效: ${id.value}" }
            fileJournal.replace(target, icon.content)
            retainedCanonicalPaths += target.canonicalPath
            return copy(profile = profile.copy(icon = profile.icon.copy(customReference = target.absolutePath)))
        }

        private fun cleanupUnreferencedFiles(retainedCanonicalPaths: Set<String>) {
            VaultResourcePaths
                .resourceDirectories(context)
                .filter(File::isDirectory)
                .forEach { root ->
                    root.walkBottomUp().forEach candidateLoop@{ candidate ->
                        if (candidate == root) return@candidateLoop
                        if (candidate.isFile && candidate.canonicalPath !in retainedCanonicalPaths) {
                            if (!candidate.delete()) {
                                reportBestEffortBackupCleanupFailure {
                                    logger.log(
                                        LogStatements.Backup.resourceCleanupFailed(
                                            IOException("Vault resource deletion failed"),
                                        ),
                                    )
                                }
                            }
                        } else if (candidate.isDirectory) {
                            candidate.delete()
                        }
                    }
                }
        }
    }

private fun sha256Hex(value: String): String =
    MessageDigest
        .getInstance("SHA-256")
        .digest(value.toByteArray())
        .joinToString("") { byte -> "%02x".format(byte) }

private const val MAX_ICON_BYTES = 16L * 1024L * 1024L
