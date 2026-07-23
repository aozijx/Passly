package com.aozijx.passly.data.repository.revision

import com.aozijx.passly.core.session.UnifiedSessionManager
import com.aozijx.passly.data.codec.revision.EntryRevisionCodec
import com.aozijx.passly.domain.authentication.SessionStateProvider
import com.aozijx.passly.domain.model.revision.EntryRevision
import com.aozijx.passly.domain.repository.revision.EntryRevisionRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomEntryRevisionRepository @Inject constructor(
    private val stateProvider: SessionStateProvider,
    private val sessionManager: UnifiedSessionManager,
    private val revisionCodec: EntryRevisionCodec
) : EntryRevisionRepository {

    override suspend fun getRevisions(entryId: String): List<EntryRevision> {
        stateProvider.assertWritable()
        return sessionManager.query {
            entryRevisionQueryDao().getByEntryId(entryId).map { entity ->
                val summary = revisionCodec.decryptSummary(entity.snapshotBlob, entity.entryId)
                EntryRevision(
                    revisionId = entity.revisionId,
                    version = entity.version,
                    entryId = entity.entryId,
                    summary = summary,
                    changeType = entity.changeType,
                    createdAt = entity.createdAt
                )
            }
        }
    }

    override suspend fun getLatestRevision(entryId: String): EntryRevision? {
        stateProvider.assertWritable()
        return sessionManager.query {
            val entity = entryRevisionQueryDao().getLatestByEntryId(entryId) ?: return@query null
            val summary = revisionCodec.decryptSummary(entity.snapshotBlob, entity.entryId)
            EntryRevision(
                revisionId = entity.revisionId,
                version = entity.version,
                entryId = entity.entryId,
                summary = summary,
                changeType = entity.changeType,
                createdAt = entity.createdAt
            )
        }
    }
}
