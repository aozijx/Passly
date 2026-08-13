package com.aozijx.passly.data.repository.attachment

import com.aozijx.passly.core.error.model.SessionModeRestricted
import com.aozijx.passly.data.local.database.session.AppDatabaseSession
import com.aozijx.passly.data.mapper.attachment.AttachmentRefMapper
import com.aozijx.passly.data.local.database.entity.AttachmentResourceEntity
import com.aozijx.passly.data.local.database.entity.AttachmentResourceState
import com.aozijx.passly.data.repository.entry.command.EntryRevisionWriter
import com.aozijx.passly.domain.authentication.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.EntryCapabilityFlags
import com.aozijx.passly.domain.entry.model.attachment.AttachmentStatus
import com.aozijx.passly.domain.entry.model.attachment.EntryAttachment
import com.aozijx.passly.domain.entry.repository.AttachmentRepository
import com.aozijx.passly.security.crypto.AttachmentContentCrypto
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class FileBackedAttachmentRepository @Inject constructor(
    private val databaseSession: AppDatabaseSession,
    private val sessionState: SecureSessionAccessState,
    private val contentCrypto: AttachmentContentCrypto,
    private val garbageCollector: AttachmentResourceGarbageCollector,
    private val revisionHelper: EntryRevisionWriter,
) : AttachmentRepository {

    override suspend fun getAttachments(entryId: String): List<EntryAttachment> =
        if (!sessionState.hasFullSecureSessionAccess()) emptyList() else {
            garbageCollector.drain()
            databaseSession.query {
            attachmentRefQueryDao().getCommittedByEntryId(entryId).map { ref ->
                val resource = requireNotNull(attachmentResourceDao().getById(ref.resourceId))
                AttachmentRefMapper.toDomain(ref, resource.fileSize)
            }
            }
        }

    override suspend fun getPendingAttachments(stagingOwnerId: String): List<EntryAttachment> =
        if (!sessionState.hasFullSecureSessionAccess()) emptyList() else {
            garbageCollector.drain()
            databaseSession.query {
            attachmentRefQueryDao().getPendingByOwner(stagingOwnerId).map { ref ->
                val resource = requireNotNull(attachmentResourceDao().getById(ref.resourceId))
                AttachmentRefMapper.toDomain(ref, resource.fileSize)
            }
            }
        }

    override suspend fun saveAttachment(
        entryId: String?,
        attachment: EntryAttachment,
        content: ByteArray,
    ) {
        requireFullSecureSessionAccess()
        val normalized = attachment.copy(
            entryId = entryId,
            fileSize = content.size.toLong(),
            createdAt = System.currentTimeMillis(),
        )
        validateRef(normalized)
        garbageCollector.withMutationLock {
            val resourceId = contentCrypto.contentId(content)
            val existing = databaseSession.query { attachmentResourceDao().getById(resourceId) }
            require(existing == null || existing.fileSize == content.size.toLong()) {
                "Attachment keyed content ID collision"
            }
            val file = garbageCollector.resourceFile(resourceId)
            if (existing == null || !file.isFile) {
                val encrypted = contentCrypto.encrypt(content, resourceId)
                try {
                    writeAtomically(file, encrypted)
                } finally {
                    encrypted.fill(0)
                }
            }
            try {
                databaseSession.transaction {
                    if (existing == null) {
                        attachmentResourceDao().insertStrict(
                            AttachmentResourceEntity(
                                resourceId = resourceId,
                                fileSize = content.size.toLong(),
                                createdAt = normalized.createdAt,
                            )
                        )
                    } else if (existing.lifecycleState == AttachmentResourceState.PENDING_GC) {
                        garbageCollector.reactivateInTransaction(this, resourceId)
                    }
                    attachmentRefCommandDao().insertStrict(
                        AttachmentRefMapper.toEntity(normalized, resourceId)
                    )
                    if (normalized.status == AttachmentStatus.COMMITTED) {
                        val committedEntryId = requireNotNull(normalized.entryId)
                        entryCommandDao().addCapability(committedEntryId, EntryCapabilityFlags.HAS_ATTACHMENTS)
                        revisionHelper.snapshotCurrent(this, committedEntryId, normalized.createdAt)
                    }
                }
            } catch (error: Throwable) {
                if (existing == null) runCatching { file.delete() }
                throw error
            }
        }
        garbageCollector.drain()
    }

    override suspend fun deleteAttachment(attachmentId: String) {
        requireFullSecureSessionAccess()
        databaseSession.transaction {
            val ref = attachmentRefQueryDao().getById(attachmentId) ?: return@transaction
            attachmentRefCommandDao().deleteById(attachmentId)
            if (ref.status == AttachmentStatus.COMMITTED.name) {
                val entryId = requireNotNull(ref.entryId)
                if (attachmentRefQueryDao().countCommittedByEntryId(entryId) == 0) {
                    entryCommandDao().retainCapabilities(entryId, EntryCapabilityFlags.HAS_ATTACHMENTS.inv())
                }
                revisionHelper.snapshotCurrent(this, entryId, System.currentTimeMillis())
            }
            garbageCollector.scheduleInTransaction(this)
        }
        garbageCollector.drain()
    }

    override suspend fun commitPendingAttachments(stagingOwnerId: String, entryId: String) {
        requireFullSecureSessionAccess()
        require(stagingOwnerId.isNotBlank() && entryId.isNotBlank())
        databaseSession.transaction {
            check(entryQueryDao().exists(entryId)) { "Attachment target entry does not exist" }
            val pendingCount = attachmentRefQueryDao().getPendingByOwner(stagingOwnerId).size
            if (pendingCount == 0) return@transaction
            check(attachmentRefCommandDao().commitByOwner(stagingOwnerId, entryId) == pendingCount)
            entryCommandDao().addCapability(entryId, EntryCapabilityFlags.HAS_ATTACHMENTS)
            revisionHelper.snapshotCurrent(this, entryId, System.currentTimeMillis())
        }
        garbageCollector.drain()
    }

    override suspend fun discardPendingAttachments(stagingOwnerId: String) {
        requireFullSecureSessionAccess()
        require(stagingOwnerId.isNotBlank())
        databaseSession.transaction {
            attachmentRefCommandDao().deletePendingByOwner(stagingOwnerId)
            garbageCollector.scheduleInTransaction(this)
        }
        garbageCollector.drain()
    }

    private fun validateRef(attachment: EntryAttachment) {
        when (attachment.status) {
            AttachmentStatus.PENDING -> {
                require(attachment.entryId == null) { "Pending attachment cannot reference an entry" }
                require(!attachment.stagingOwnerId.isNullOrBlank()) { "Pending attachment needs a staging owner" }
            }
            AttachmentStatus.COMMITTED -> {
                require(!attachment.entryId.isNullOrBlank()) { "Committed attachment needs an entry" }
                require(attachment.stagingOwnerId == null) { "Committed attachment cannot have a staging owner" }
            }
        }
        require(attachment.fileName.isNotBlank()) { "Attachment filename cannot be blank" }
        require(attachment.displayOrder >= 0) { "Attachment display order cannot be negative" }
    }

    private fun writeAtomically(target: File, content: ByteArray) {
        val parent = requireNotNull(target.parentFile)
        require(parent.isDirectory || parent.mkdirs()) { "Unable to create attachment directory" }
        val temporary = File(parent, ".${target.name}.importing")
        try {
            FileOutputStream(temporary).use { output ->
                output.write(content)
                output.flush()
                output.fd.sync()
            }
            if (target.exists()) require(target.delete()) { "Unable to replace attachment content" }
            require(temporary.renameTo(target)) { "Unable to commit attachment content" }
        } finally {
            temporary.delete()
        }
    }

    private fun requireFullSecureSessionAccess() {
        if (!sessionState.hasFullSecureSessionAccess()) throw SessionModeRestricted()
    }
}
