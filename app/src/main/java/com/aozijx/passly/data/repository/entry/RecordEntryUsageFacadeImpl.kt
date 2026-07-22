package com.aozijx.passly.data.repository.entry

import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.core.session.UnifiedSessionManager
import com.aozijx.passly.data.mapper.VaultEntryCryptoMapper
import com.aozijx.passly.data.mapper.toEntity
import com.aozijx.passly.data.model.entity.VaultCredentialEntity
import com.aozijx.passly.data.model.entity.VaultMetadataEntity
import com.aozijx.passly.data.util.Clock
import com.aozijx.passly.domain.authentication.SessionStateProvider
import com.aozijx.passly.domain.model.activity.ActivityType
import com.aozijx.passly.domain.model.activity.VaultActivity
import com.aozijx.passly.domain.repository.entry.RecordEntryUsageFacade
import javax.inject.Inject
import javax.inject.Singleton

/**
 * data 层实现，通过 @Transaction 保证"插入活动记录"和"更新使用统计"的原子性。
 */
@Singleton
class RecordEntryUsageFacadeImpl @Inject constructor(
    private val stateProvider: SessionStateProvider,
    private val sessionManager: UnifiedSessionManager,
    private val cryptoMapper: VaultEntryCryptoMapper,
    private val clock: Clock
) : RecordEntryUsageFacade {

    override suspend fun record(entryId: String, type: ActivityType): AppResult<Unit> {
        stateProvider.assertWritable()
        return sessionManager.transaction {
            AppResult.runSuspendCatching("record_entry_usage") {
                // 1. 插入活动记录
                activityDao().insert(
                    VaultActivity(
                        entryId = entryId,
                        activityType = type
                    ).toEntity()
                )

                // 2. 更新条目使用统计
                val metaEntity = metadataDao().getById(entryId)
                    ?: throw IllegalStateException("Entry not found: $entryId")
                val credEntity = credentialDao().getByEntryId(entryId)
                val entry = cryptoMapper.assembleEntry(metaEntity, credEntity)
                    ?: throw IllegalStateException("Failed to assemble entry: $entryId")

                val updatedMeta = entry.metadata.copy(
                    usageCount = entry.usageCount + 1,
                    lastUsedAt = clock.now()
                )

                val metaBlob = cryptoMapper.encryptMetadata(updatedMeta, entryId)
                val credBlob = cryptoMapper.encryptCredential(entry.credential, entryId)

                metadataDao().update(
                    VaultMetadataEntity(
                        entryId = entryId,
                        entryType = entry.entryType,
                        metadataBlob = metaBlob,
                        vaultId = metaEntity.vaultId,
                        entryVersion = metaEntity.entryVersion + 1,
                        createdAt = metaEntity.createdAt,
                        updatedAt = clock.now()
                    )
                )
                credentialDao().update(
                    VaultCredentialEntity(
                        entryId = entryId,
                        credentialBlob = credBlob
                    )
                )
            }
        }
    }
}
