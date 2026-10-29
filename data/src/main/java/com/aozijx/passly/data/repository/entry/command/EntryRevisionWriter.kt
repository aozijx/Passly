package com.aozijx.passly.data.repository.entry.command

import com.aozijx.passly.data.codec.entry.SecretBundleCodec
import com.aozijx.passly.data.codec.toStorageKey
import com.aozijx.passly.data.codec.revision.EntryContentSnapshotCodec
import com.aozijx.passly.data.codec.revision.SensitiveRevisionSnapshotCodec
import com.aozijx.passly.data.mapper.entry.EntryProfileMapper
import com.aozijx.passly.data.mapper.entry.toBundleSecret
import com.aozijx.passly.data.local.database.AppDatabase
import com.aozijx.passly.data.local.database.entity.EntryRevisionEntity
import com.aozijx.passly.data.local.database.entity.RevisionAttachmentRefEntity
import com.aozijx.passly.data.repository.entry.SecretFieldStore
import com.aozijx.passly.domain.entry.model.EntrySecret
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryProfile
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.activity.ActivityType
import com.aozijx.passly.domain.entry.model.activity.EntryActivityContext
import com.aozijx.passly.domain.entry.model.relation.EntryLink
import com.aozijx.passly.domain.entry.model.relation.EntryLinkId
import com.aozijx.passly.domain.entry.model.history.RevisionChange
import com.aozijx.passly.domain.entry.port.EntryIconResourceCleaner
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
    private val iconResourceCleaner: EntryIconResourceCleaner,
    private val activityWriter: EntryActivityWriter,
) {

    /**
     * 写入一条历史修订，版本号直接使用 [entryVersion]（即 EntryEntity.version）。
     */
    suspend fun snapshotChanges(
        db: AppDatabase,
        entryId: String,
        entryVersion: Int,
        summary: EntryProfile,
        secret: EntrySecret,
        entryType: EntryType,
        now: Long,
        change: RevisionChange = RevisionChange.VALUE_CHANGED,
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
        val sensitiveFields = secretFieldQueryDao().getAll(entryId).filter { row ->
            row.fieldKey != SecretBundleCodec.FIELD_KEY
        }
        val entryContentCipher = contentSnapshotCodec.encrypt(
            summary = summary,
            bundleSecret = secret.toBundleSecret(),
            entryId = entryId,
            expectedType = entryType,
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
                changeType = change.toStorageKey(),
                createdAt = now,
                iconCustomReference = summary.icon.customReference,
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
        val discardedIcons = entryRevisionQueryDao().getIconReferencesBeyondEntryLimit(entryId, REVISION_LIMIT).toMutableSet()
        entryRevisionCommandDao().deleteOldVersions(entryId, REVISION_LIMIT)
        discardedIcons += entryRevisionQueryDao().getIconReferencesBeyondGlobalLimit(GLOBAL_REVISION_LIMIT)
        entryRevisionCommandDao().deleteOldestBeyondGlobalLimit(GLOBAL_REVISION_LIMIT)
        discardedIcons.forEach(iconResourceCleaner::discardUnreferencedIcon)
    }

    suspend fun snapshotCurrent(
        db: AppDatabase,
        entryId: String,
        now: Long,
        secretFieldStore: SecretFieldStore,
        change: RevisionChange = RevisionChange.VALUE_CHANGED,
    ) = with(db) {
        val metadata = entryQueryDao().getById(entryId) ?: return@with
        val summary = EntryProfileMapper.fromEntity(metadata)
        val secret = secretFieldStore.readBundle(this, entryId, metadata.entryType)
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
            entryType = metadata.entryType,
            now = now,
            change = change,
        )
        activityWriter.recordActivity(this, entryId, ActivityType.UPDATE, now,
            EntryActivityContext.ContentRevision(EntryVersion(metadata.version + 1)))
    }

    private companion object {
        const val REVISION_LIMIT = 50
        const val GLOBAL_REVISION_LIMIT = 1_000
    }
}
