package com.aozijx.passly.data.repository.revision

import com.aozijx.passly.core.session.UnifiedSessionManager
import com.aozijx.passly.data.codec.revision.EntryRevisionCodec
import com.aozijx.passly.domain.entry.model.EntryHeader
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.entry.model.EntryVersion
import com.aozijx.passly.domain.entry.model.VaultEntry
import com.aozijx.passly.domain.entry.model.revision.EntryRevision
import com.aozijx.passly.domain.entry.model.revision.RevisionType
import com.aozijx.passly.domain.entry.repository.EntryRevisionRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomEntryRevisionRepository @Inject constructor(
    private val sessionManager: UnifiedSessionManager,
    private val revisionCodec: EntryRevisionCodec
) : EntryRevisionRepository {

    override suspend fun getRevisions(entryId: String): List<EntryRevision> {
        return sessionManager.query {
            entryRevisionQueryDao().observeByEntryId(entryId).let { flow ->
                emptyList()
            }
        }
    }

    override suspend fun getLatestRevision(entryId: String): EntryRevision? {
        return sessionManager.query {
            val entity = entryRevisionQueryDao().getByVersion(entryId, 1) ?: return@query null
            val (summary, secret) = revisionCodec.decrypt(entity.entryBlob, entity.entryId)
            val vaultEntry = VaultEntry(
                header = EntryHeader(
                    id = EntryId(entity.entryId),
                    entryType = EntryType.LOGIN,
                    version = EntryVersion(entity.version),
                    createdAt = entity.createdAt,
                    updatedAt = entity.createdAt
                ),
                summary = summary,
                secret = secret
            )
            EntryRevision(
                revisionId = entity.revisionId,
                version = entity.version,
                entryId = entity.entryId,
                entry = vaultEntry,
                changeType = RevisionType.fromValue(entity.changeType),
                createdAt = entity.createdAt
            )
        }
    }
}
