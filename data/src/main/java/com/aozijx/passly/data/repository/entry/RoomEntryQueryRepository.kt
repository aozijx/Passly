package com.aozijx.passly.data.repository.entry

import com.aozijx.passly.data.local.database.session.AppDatabaseSession
import com.aozijx.passly.data.codec.entry.EntrySecretCodec
import com.aozijx.passly.data.codec.entry.EntryProfileCodec
import com.aozijx.passly.data.mapper.entry.EntryAssembler
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.Entry
import com.aozijx.passly.domain.entry.model.EntryId
import com.aozijx.passly.domain.entry.port.EntryQueryRepository
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
    private val summaryCodec: EntryProfileCodec,
    private val secretCodec: EntrySecretCodec
) : EntryQueryRepository {

    override suspend fun getById(entryId: EntryId): Entry? {
        if (!sessionState.hasFullSecureSessionAccess()) return null
        return databaseSession.query {
            val metaEntity = entryQueryDao().getById(entryId.value) ?: return@query null
            val credEntity = entrySecretQueryDao().getByEntryId(entryId.value)
            val summary = summaryCodec.decrypt(metaEntity.summaryBlob, metaEntity.entryId)
            val secret = credEntity?.let { secretCodec.decrypt(it.secretBlob, it.entryId) }
            EntryAssembler.assembleFromDatabase(metaEntity, summary, secret)
        }
    }

    override suspend fun findEntriesWithCustomIcons(): List<Entry> {
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
                EntryAssembler.assembleFromDatabase(metaEntity, summary, secret)
            }
        }
    }

    override suspend fun count(): Int {
        if (!sessionState.hasFullSecureSessionAccess()) return 0
        return databaseSession.query { entryQueryDao().countActive() }
    }
}
