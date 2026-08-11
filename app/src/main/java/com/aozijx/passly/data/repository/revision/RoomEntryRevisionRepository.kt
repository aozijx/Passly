package com.aozijx.passly.data.repository.revision

import com.aozijx.passly.core.session.UnifiedSessionManager
import com.aozijx.passly.data.codec.revision.EntryRevisionCodec
import com.aozijx.passly.data.codec.revision.SensitiveRevisionSnapshotCodec
import com.aozijx.passly.data.model.entity.EntryEntity
import com.aozijx.passly.data.model.entity.EntryRevisionEntity
import com.aozijx.passly.domain.authentication.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.EntryHeader
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.EntryAggregate
import com.aozijx.passly.domain.entry.model.revision.EntryRevision
import com.aozijx.passly.domain.entry.model.revision.RevisionType
import com.aozijx.passly.domain.entry.repository.EntryRevisionRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomEntryRevisionRepository @Inject constructor(
    private val sessionManager: UnifiedSessionManager,
    private val sessionState: SecureSessionAccessState,
    private val revisionCodec: EntryRevisionCodec,
    private val sensitiveRevisionCodec: SensitiveRevisionSnapshotCodec,
) : EntryRevisionRepository {

    override suspend fun getRevisions(entryId: String): List<EntryRevision> {
        if (!sessionState.hasFullSecureSessionAccess()) return emptyList()
        return sessionManager.query {
            val metadata = entryQueryDao().getById(entryId) ?: return@query emptyList()
            entryRevisionQueryDao().getByEntryId(entryId).map { entity ->
                entity.toDomain(metadata)
            }
        }
    }

    override suspend fun getLatestRevision(entryId: String): EntryRevision? {
        if (!sessionState.hasFullSecureSessionAccess()) return null
        return sessionManager.query {
            val entity = entryRevisionQueryDao().getLatest(entryId) ?: return@query null
            val metadata = entryQueryDao().getById(entryId) ?: return@query null
            entity.toDomain(metadata)
        }
    }

    private suspend fun EntryRevisionEntity.toDomain(
        metadata: EntryEntity,
    ): EntryRevision {
        val snapshot = revisionCodec.decrypt(regularSnapshotBlob, entryId)
        val entry = EntryAggregate(
            header = EntryHeader(
                id = EntryId(entryId),
                entryType = metadata.entryType,
                version = EntryVersion(version),
                createdAt = metadata.createdAt,
                updatedAt = createdAt,
                deletedAt = metadata.deletedAt
            ),
            summary = snapshot.summary,
            secret = snapshot.secret,
        )
        return EntryRevision(
            revisionId = revisionId,
            version = version,
            entryId = entryId,
            entry = entry,
            links = snapshot.links,
            attachmentIds = snapshot.attachmentIds,
            sensitiveFieldKeys = sensitiveRevisionCodec.decode(sensitiveFieldsSnapshotBlob)
                .mapTo(linkedSetOf()) { it.key },
            changeType = RevisionType.fromValue(changeType),
            createdAt = createdAt,
        )
    }
}
