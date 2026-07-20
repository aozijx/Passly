package com.aozijx.passly.data.repository.vault

import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.core.error.AuthFailed
import com.aozijx.passly.data.local.database.DatabaseSession
import com.aozijx.passly.data.mapper.VaultEntryCryptoMapper
import com.aozijx.passly.data.mapper.toDomain
import com.aozijx.passly.data.mapper.toEntity
import com.aozijx.passly.data.model.entity.VaultCredentialEntity
import com.aozijx.passly.data.model.entity.VaultMetadataEntity
import com.aozijx.passly.data.util.Clock
import com.aozijx.passly.domain.authentication.VaultAccessState
import com.aozijx.passly.domain.model.activity.ActivityType
import com.aozijx.passly.domain.model.activity.VaultActivity
import com.aozijx.passly.domain.repository.vault.ActivityRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityRepositoryImpl @Inject constructor(
    private val sessionManager: DatabaseSession,
    private val sessionState: VaultAccessState,
    private val cryptoMapper: VaultEntryCryptoMapper,
    private val clock: Clock
) : ActivityRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeByEntryId(entryId: String): Flow<List<VaultActivity>> =
        sessionState.isAuthorized.flatMapLatest { authorized ->
            if (!authorized) flowOf(emptyList())
            else sessionManager.withDatabase {
                activityDao().observeByEntryId(entryId)
                    .map { entities -> entities.map { it.toDomain() } }
                    .flowOn(Dispatchers.IO)
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeAll(): Flow<List<VaultActivity>> =
        sessionState.isAuthorized.flatMapLatest { authorized ->
            if (!authorized) flowOf(emptyList())
            else sessionManager.withDatabase {
                activityDao().observeAll()
                    .map { entities -> entities.map { it.toDomain() } }
                    .flowOn(Dispatchers.IO)
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeByType(activityType: ActivityType): Flow<List<VaultActivity>> =
        sessionState.isAuthorized.flatMapLatest { authorized ->
            if (!authorized) flowOf(emptyList())
            else sessionManager.withDatabase {
                activityDao().observeByType(activityType)
                    .map { entities -> entities.map { it.toDomain() } }
                    .flowOn(Dispatchers.IO)
            }
        }

    override suspend fun getById(activityId: String): VaultActivity? {
        if (sessionState.isLocked()) return null
        return sessionManager.withDatabase { activityDao().getById(activityId)?.toDomain() }
    }

    override suspend fun getByEntryId(entryId: String): List<VaultActivity> {
        if (sessionState.isLocked()) return emptyList()
        return sessionManager.withDatabase {
            activityDao().getByEntryId(entryId).map { it.toDomain() }
        }
    }

    override suspend fun exists(activityId: String): Boolean {
        if (sessionState.isLocked()) return false
        return sessionManager.withDatabase { activityDao().exists(activityId) }
    }

    override suspend fun count(): Int {
        if (sessionState.isLocked()) return 0
        return sessionManager.withDatabase { activityDao().count() }
    }

    override suspend fun insert(activity: VaultActivity) {
        if (sessionState.isLocked()) return
        sessionManager.withDatabase { activityDao().insert(activity.toEntity()) }
    }

    override suspend fun insertAll(activities: List<VaultActivity>) {
        if (sessionState.isLocked()) return
        sessionManager.withDatabase { activityDao().insertAll(activities.map { it.toEntity() }) }
    }

    override suspend fun record(entryId: String, type: ActivityType): AppResult<Unit> {
        if (sessionState.isLocked()) return AppResult.failure(AuthFailed("数据库未解锁"))
        return sessionManager.withDatabase {
            AppResult.runSuspendCatching("activity.record") {
                // 1. 插入活动记录
                val activity = VaultActivity(entryId = entryId, activityType = type)
                activityDao().insert(activity.toEntity())

                // 2. 更新条目使用统计 (如果需要同步更新)
                val metaEntity = metadataDao().getById(entryId) ?: return@runSuspendCatching
                val credEntity = credentialDao().getByEntryId(entryId)
                val entry =
                    cryptoMapper.assembleEntry(metaEntity, credEntity) ?: return@runSuspendCatching

                val updatedMeta = entry.metadata.copy(
                    usageCount = entry.usageCount + 1,
                    lastUsedAt = clock.now()
                )

                val metaBlob = cryptoMapper.encryptMetadata(updatedMeta, entryId)
                val credBlob = cryptoMapper.encryptCredential(entry.credential, entryId)

                val newMetaEntity = VaultMetadataEntity(
                    entryId = entryId,
                    entryType = entry.entryType,
                    metadataBlob = metaBlob,
                    vaultId = metaEntity.vaultId,
                    entryVersion = metaEntity.entryVersion + 1,
                    createdAt = metaEntity.createdAt,
                    updatedAt = clock.now()
                )
                metadataDao().update(newMetaEntity)
                credentialDao().update(
                    VaultCredentialEntity(
                        entryId = entryId,
                        credentialBlob = credBlob
                    )
                )
            }
        }
    }

    override suspend fun deleteByEntryId(entryId: String) {
        if (sessionState.isLocked()) return
        sessionManager.withDatabase { activityDao().deleteByEntryId(entryId) }
    }

    override suspend fun deleteBefore(timestamp: Long) {
        if (sessionState.isLocked()) return
        sessionManager.withDatabase { activityDao().deleteBefore(timestamp) }
    }

    override suspend fun clear() {
        if (sessionState.isLocked()) return
        sessionManager.withDatabase { activityDao().clear() }
    }

}
