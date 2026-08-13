package com.aozijx.passly.data.repository.entry

import com.aozijx.passly.data.local.database.session.AppDatabaseSession
import com.aozijx.passly.data.codec.entry.EntrySecretCodec
import com.aozijx.passly.data.codec.entry.EntrySummaryCodec
import com.aozijx.passly.data.mapper.entry.EntryAggregateAssembler
import com.aozijx.passly.domain.authentication.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.EntryAggregate
import com.aozijx.passly.domain.entry.repository.EntryQueryRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 查询 Repository 实现：仅执行读操作，不涉及事务。
 * 直接对接 DAO 查询，无写操作副作用。
 */
@Singleton
internal class RoomEntryQueryRepository @Inject constructor(
    private val databaseSession: AppDatabaseSession,
    private val sessionState: SecureSessionAccessState,
    private val summaryCodec: EntrySummaryCodec,
    private val secretCodec: EntrySecretCodec
) : EntryQueryRepository {

    override suspend fun getByIdWithoutHighSensitivity(entryId: String): EntryAggregate? {
        if (!sessionState.hasFullSecureSessionAccess()) return null
        return databaseSession.query {
            val metaEntity = entryQueryDao().getById(entryId) ?: return@query null
            val credEntity = entrySecretQueryDao().getByEntryId(entryId)
            val summary = summaryCodec.decrypt(metaEntity.summaryBlob, metaEntity.entryId)
            val secret = credEntity?.let { secretCodec.decrypt(it.secretBlob, it.entryId) }
            EntryAggregateAssembler.assembleFromDatabase(metaEntity, summary, secret)
        }
    }

    override suspend fun getEntriesForIconResync(): List<EntryAggregate> {
        if (!sessionState.hasFullSecureSessionAccess()) return emptyList()
        return databaseSession.query {
            val metaEntities = entryQueryDao().getActive()
            val credEntities = entrySecretQueryDao().getByEntryIds(metaEntities.map { it.entryId })
            val credMap = credEntities.associateBy { it.entryId }
            metaEntities.map { metaEntity ->
                val summary = summaryCodec.decrypt(metaEntity.summaryBlob, metaEntity.entryId)
                val secret = credMap[metaEntity.entryId]?.let {
                    secretCodec.decrypt(
                        it.secretBlob,
                        it.entryId
                    )
                }
                EntryAggregateAssembler.assembleFromDatabase(metaEntity, summary, secret)
            }
        }
    }

    override suspend fun count(): Int {
        if (!sessionState.hasFullSecureSessionAccess()) return 0
        return databaseSession.query { entryQueryDao().countActive() }
    }
}
