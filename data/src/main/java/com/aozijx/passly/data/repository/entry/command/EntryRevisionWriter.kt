package com.aozijx.passly.data.repository.entry.command

import com.aozijx.passly.data.codec.revision.EntryContentSnapshotCodec
import com.aozijx.passly.data.codec.revision.SensitiveRevisionSnapshotCodec
import com.aozijx.passly.data.codec.entry.EntrySecretCodec
import com.aozijx.passly.data.codec.entry.EntrySummaryCodec
import com.aozijx.passly.data.local.database.AppDatabase
import com.aozijx.passly.data.local.database.entity.EntryRevisionEntity
import com.aozijx.passly.data.local.database.entity.RevisionAttachmentRefEntity
import com.aozijx.passly.data.repository.attachment.AttachmentResourceGarbageCollector
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntrySummary
import com.aozijx.passly.domain.entry.model.link.EntryLink
import com.aozijx.passly.domain.entry.model.link.EntryLinkId
import com.aozijx.passly.domain.entry.model.revision.RevisionType
import com.github.f4b6a3.uuid.UuidCreator
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 历史修订协作器。
 *
 * 封装修订的加密与写入，供各 Command Executor 在事务内调用。
 */
@Singleton
internal class EntryRevisionWriter @Inject constructor(
    private val contentSnapshotCodec: EntryContentSnapshotCodec,
    private val sensitiveRevisionCodec: SensitiveRevisionSnapshotCodec,
    private val summaryCodec: EntrySummaryCodec,
    private val secretCodec: EntrySecretCodec,
    private val attachmentGarbageCollector: AttachmentResourceGarbageCollector,
) {

    /**
     * 写入一条历史修订，版本号直接使用 [entryVersion]（即 EntryEntity.version）。
     */
    suspend fun snapshotChanges(
        db: AppDatabase,
        entryId: String,
        entryVersion: Int,
        summary: EntrySummary,
        secret: EntrySecret,
        now: Long,
        changeType: RevisionType = RevisionType.VALUE_CHANGED,
    ) = with(db) {
        val links = entryLinkQueryDao().getByEntryId(entryId).map { link ->
            EntryLink.create(
                id = EntryLinkId(link.linkId),
                sourceEntryId = EntryId(link.sourceEntryId),
                targetEntryId = EntryId(link.targetEntryId),
                relationType = link.relationType,
                createdAt = link.createdAt,
                updatedAt = link.updatedAt,
            )
        }
        val attachmentRefs = attachmentRefQueryDao().getCommittedByEntryId(entryId)
        val sensitiveFields = sensitiveFieldQueryDao().getFields(entryId)
        val entryContentCipher = contentSnapshotCodec.encrypt(
            summary = summary,
            secret = secret,
            entryId = entryId,
            links = links,
        )
        val revisionId = UuidCreator.getTimeOrderedEpoch().toString()
        entryRevisionCommandDao().insertStrict(
            EntryRevisionEntity(
                revisionId = revisionId,
                version = entryVersion,
                entryId = entryId,
                entryContentCipher = entryContentCipher,
                sensitiveFieldCipherSet = sensitiveRevisionCodec.encode(sensitiveFields),
                changeType = changeType.value,
                createdAt = now
            )
        )
        val revisionAttachmentRefs = attachmentRefs.map { ref ->
            RevisionAttachmentRefEntity(
                revisionId = revisionId,
                attachmentId = ref.attachmentId,
                resourceId = ref.resourceId,
                fileName = ref.fileName,
                mimeType = ref.mimeType,
                displayOrder = ref.displayOrder,
                createdAt = ref.createdAt,
            )
        }
        if (revisionAttachmentRefs.isNotEmpty()) {
            revisionAttachmentRefDao().insertAllStrict(revisionAttachmentRefs)
        }
        entryRevisionCommandDao().deleteOldVersions(entryId, REVISION_LIMIT)
        entryRevisionCommandDao().deleteOldestBeyondGlobalLimit(GLOBAL_REVISION_LIMIT)
        attachmentGarbageCollector.scheduleInTransaction(this)
    }

    suspend fun snapshotCurrent(
        db: AppDatabase,
        entryId: String,
        now: Long,
        changeType: RevisionType = RevisionType.VALUE_CHANGED,
    ) = with(db) {
        val metadata = entryQueryDao().getById(entryId) ?: return@with
        val secretEntity = entrySecretQueryDao().getByEntryId(entryId)
            ?: return@with
        val summary = summaryCodec.decrypt(metadata.summaryBlob, entryId)
        val secret = secretCodec.decrypt(secretEntity.secretBlob, entryId)
        val affected = entryCommandDao().bumpVersion(
            entryId = entryId,
            expectedVersion = metadata.version,
            updatedAt = now,
        )
        check(affected == 1) { "Entry version changed while recording relation revision" }
        snapshotChanges(
            db = this,
            entryId = entryId,
            entryVersion = metadata.version + 1,
            summary = summary,
            secret = secret,
            now = now,
            changeType = changeType,
        )
    }

    private companion object {
        const val REVISION_LIMIT = 50
        const val GLOBAL_REVISION_LIMIT = 1_000
    }
}
