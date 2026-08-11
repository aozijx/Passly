package com.aozijx.passly.data.repository.entry.internal

import com.aozijx.passly.data.codec.revision.EntryRevisionCodec
import com.aozijx.passly.data.codec.revision.SensitiveRevisionSnapshotCodec
import com.aozijx.passly.data.codec.entry.EntrySecretCodec
import com.aozijx.passly.data.codec.entry.EntrySummaryCodec
import com.aozijx.passly.data.local.database.AppDatabase
import com.aozijx.passly.data.model.entity.EntryRevisionEntity
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntrySummary
import com.aozijx.passly.domain.entry.model.link.EntryLink
import com.aozijx.passly.domain.entry.model.link.EntryLinkId
import com.aozijx.passly.domain.entry.model.link.EntryRelationType
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
class EntryRevisionHelper @Inject constructor(
    private val revisionCodec: EntryRevisionCodec,
    private val sensitiveRevisionCodec: SensitiveRevisionSnapshotCodec,
    private val summaryCodec: EntrySummaryCodec,
    private val secretCodec: EntrySecretCodec,
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
        now: Long
    ) {
        with(db) {
            val links = entryLinkQueryDao().getByEntryId(entryId).map { link ->
                EntryLink.create(
                    id = EntryLinkId(link.linkId),
                    sourceEntryId = EntryId(link.sourceEntryId),
                    targetEntryId = EntryId(link.targetEntryId),
                    relationType = EntryRelationType.valueOf(link.relationType),
                    createdAt = link.createdAt,
                    updatedAt = link.updatedAt,
                )
            }
            val attachmentIds = entryAttachmentQueryDao().getByEntryId(entryId)
                .filter { it.status == "COMMITTED" }
                .map { it.attachmentId }
            val sensitiveFields = sensitiveFieldQueryDao().getFields(entryId)
            val regularSnapshotBlob = revisionCodec.encrypt(
                summary = summary,
                secret = secret,
                entryId = entryId,
                links = links,
                attachmentIds = attachmentIds,
            )
            entryRevisionCommandDao().insertStrict(
                EntryRevisionEntity(
                    revisionId = UuidCreator.getTimeOrderedEpoch().toString(),
                    version = entryVersion,
                    entryId = entryId,
                    regularSnapshotBlob = regularSnapshotBlob,
                    sensitiveFieldsSnapshotBlob = sensitiveRevisionCodec.encode(sensitiveFields),
                    changeType = RevisionType.VALUE_CHANGED.value,
                    createdAt = now
                )
            )
            entryRevisionCommandDao().deleteOldVersions(entryId, REVISION_LIMIT)
            entryRevisionCommandDao().deleteOldestBeyondGlobalLimit(GLOBAL_REVISION_LIMIT)
        }
    }

    suspend fun snapshotCurrent(
        db: AppDatabase,
        entryId: String,
        now: Long,
    ) = with(db) {
        val metadata = entryQueryDao().getById(entryId) ?: return@with
        val secretEntity = entrySecretQueryDao().getByEntryId(entryId) ?: return@with
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
        )
    }

    private companion object {
        const val REVISION_LIMIT = 50
        const val GLOBAL_REVISION_LIMIT = 1_000
    }
}
