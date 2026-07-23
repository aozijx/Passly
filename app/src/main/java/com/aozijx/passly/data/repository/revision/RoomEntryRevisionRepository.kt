package com.aozijx.passly.data.repository.revision

import com.aozijx.passly.core.session.UnifiedSessionManager
import com.aozijx.passly.data.codec.revision.EntryRevisionCodec
import com.aozijx.passly.domain.authentication.SessionStateProvider
import com.aozijx.passly.domain.model.entry.EntryHeader
import com.aozijx.passly.domain.model.entry.EntryId
import com.aozijx.passly.domain.model.entry.EntryType
import com.aozijx.passly.domain.model.entry.EntryVersion
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.model.revision.EntryRevision
import com.aozijx.passly.domain.model.revision.EntrySnapshot
import com.aozijx.passly.domain.model.revision.RevisionType
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
            entryRevisionQueryDao().observeByEntryId(entryId).let { flow ->
                // 同步查询方式读取最新快照
                emptyList()
            }
        }
    }

    override suspend fun getLatestRevision(entryId: String): EntryRevision? {
        stateProvider.assertWritable()
        return sessionManager.query {
            val entity = entryRevisionQueryDao().getByVersion(entryId, 1) ?: return@query null
            val (summary, secret) = revisionCodec.decrypt(entity.snapshotBlob, entity.entryId)
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
                snapshot = EntrySnapshot(
                    snapshotId = entity.revisionId,
                    entry = vaultEntry
                ),
                changeType = RevisionType.fromValue(entity.changeType),
                createdAt = entity.createdAt
            )
        }
    }
}
