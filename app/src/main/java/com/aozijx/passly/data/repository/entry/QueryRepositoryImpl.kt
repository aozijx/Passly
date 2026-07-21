package com.aozijx.passly.data.repository.entry

import com.aozijx.passly.core.session.UnifiedSessionManager
import com.aozijx.passly.data.mapper.VaultEntryCryptoMapper
import com.aozijx.passly.domain.authentication.SessionStateProvider
import com.aozijx.passly.domain.authentication.VaultAccessState
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.repository.entry.QueryRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 查询 Repository 实现：仅执行读操作，不涉及事务。
 * 直接对接 DAO 查询，无写操作副作用。
 */
@Singleton
class QueryRepositoryImpl @Inject constructor(
    private val sessionState: VaultAccessState,
    private val stateProvider: SessionStateProvider,
    private val sessionManager: UnifiedSessionManager,
    private val cryptoMapper: VaultEntryCryptoMapper
) : QueryRepository {

    override suspend fun getById(entryId: String): VaultEntry? {
        stateProvider.assertWritable()
        return sessionManager.query {
            val metaEntity = metadataDao().getById(entryId) ?: return@query null
            val credEntity = credentialDao().getByEntryId(entryId)
            cryptoMapper.assembleEntry(metaEntity, credEntity)
        }
    }

    override suspend fun getEntriesForIconResync(): List<VaultEntry> {
        stateProvider.assertWritable()
        return sessionManager.query {
            val metaEntities = metadataDao().getActive()
            val credEntities = credentialDao().getByEntryIds(metaEntities.map { it.entryId })
            val credMap = credEntities.associateBy { it.entryId }
            metaEntities.mapNotNull { cryptoMapper.assembleEntry(it, credMap[it.entryId]) }
        }
    }

    override suspend fun count(): Int {
        stateProvider.assertWritable()
        return sessionManager.query { metadataDao().countActive() }
    }
}
