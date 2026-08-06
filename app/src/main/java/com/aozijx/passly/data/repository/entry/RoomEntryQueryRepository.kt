package com.aozijx.passly.data.repository.entry

import com.aozijx.passly.core.session.UnifiedSessionManager
import com.aozijx.passly.data.codec.entry.EntryHighSensitivitySecretCodec
import com.aozijx.passly.data.codec.entry.EntrySecretCodec
import com.aozijx.passly.data.codec.entry.EntrySummaryCodec
import com.aozijx.passly.data.mapper.entry.EntryAggregateAssembler
import com.aozijx.passly.domain.authentication.VaultAccessState
import com.aozijx.passly.domain.entry.model.EntryHighSensitivitySecret
import com.aozijx.passly.domain.entry.model.VaultEntry
import com.aozijx.passly.domain.entry.repository.EntryHighSensitivityRepository
import com.aozijx.passly.domain.entry.repository.EntryQueryRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 查询 Repository 实现：仅执行读操作，不涉及事务。
 * 直接对接 DAO 查询，无写操作副作用。
 */
@Singleton
class RoomEntryQueryRepository @Inject constructor(
    private val sessionManager: UnifiedSessionManager,
    private val sessionState: VaultAccessState,
    private val summaryCodec: EntrySummaryCodec,
    private val secretCodec: EntrySecretCodec,
    private val highSensitivitySecretCodec: EntryHighSensitivitySecretCodec
) : EntryQueryRepository, EntryHighSensitivityRepository {

    override suspend fun getByIdWithoutHighSensitivity(entryId: String): VaultEntry? {
        if (!sessionState.hasFullVaultAccess()) return null
        return sessionManager.query {
            val metaEntity = entryQueryDao().getById(entryId) ?: return@query null
            val credEntity = entrySecretQueryDao().getByEntryId(entryId)
            val summary = summaryCodec.decrypt(metaEntity.summaryBlob, metaEntity.entryId)
            val secret = credEntity?.let { secretCodec.decrypt(it.secretBlob, it.entryId) }
            EntryAggregateAssembler.assembleFromDatabase(metaEntity, summary, secret)
        }
    }

    override suspend fun getHighSensitivitySecretForReveal(entryId: String): EntryHighSensitivitySecret? {
        if (!sessionState.hasFullVaultAccess()) return null
        return sessionManager.query {
            val entity = entrySecretQueryDao().getByEntryId(entryId) ?: return@query null
            val blob = entity.highSensitivityBlob ?: return@query null
            highSensitivitySecretCodec.decrypt(blob, entity.entryId)
        }
    }

    override suspend fun getEntriesForIconResync(): List<VaultEntry> {
        if (!sessionState.hasFullVaultAccess()) return emptyList()
        return sessionManager.query {
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
        if (!sessionState.hasFullVaultAccess()) return 0
        return sessionManager.query { entryQueryDao().countActive() }
    }
}
