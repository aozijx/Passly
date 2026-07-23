package com.aozijx.passly.data.repository.snapshot

import com.aozijx.passly.core.session.UnifiedSessionManager
import com.aozijx.passly.data.codec.snapshot.EntrySnapshotCodec
import com.aozijx.passly.domain.authentication.SessionStateProvider
import com.aozijx.passly.domain.model.entry.EntryHeader
import com.aozijx.passly.domain.model.entry.EntryId
import com.aozijx.passly.domain.model.entry.EntryType
import com.aozijx.passly.domain.model.entry.EntryVersion
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.model.snapshot.EntryRevision
import com.aozijx.passly.domain.model.snapshot.EntrySnapshot
import com.aozijx.passly.domain.model.snapshot.RevisionType
import com.aozijx.passly.domain.repository.snapshot.EntryRevisionRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomEntrySnapshotRepository @Inject constructor(
    private val stateProvider: SessionStateProvider,
    private val sessionManager: UnifiedSessionManager,
    private val snapshotCodec: EntrySnapshotCodec
) : EntryRevisionRepository {

    override suspend fun getRevisions(entryId: String): List<EntryRevision> {
        stateProvider.assertWritable()
        return sessionManager.query {
            entrySnapshotQueryDao().observeByEntryId(entryId).let { flow ->
                emptyList()
            }
        }
    }

    override suspend fun getLatestRevision(entryId: String): EntryRevision? {
        stateProvider.assertWritable()
        return sessionManager.query {
            val entity = entrySnapshotQueryDao().getByVersion(entryId, 1) ?: return@query null
            val (summary, secret) = snapshotCodec.decrypt(entity.snapshotBlob, entity.entryId)
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
