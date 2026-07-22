package com.aozijx.passly.data.repository.entry

import androidx.room.withTransaction
import com.aozijx.passly.core.diagnostics.AppLog
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.core.error.Conflict
import com.aozijx.passly.core.session.UnifiedSessionManager
import com.aozijx.passly.data.mapper.VaultEntryCryptoMapper
import com.aozijx.passly.data.mapper.lookup.toLookupFields
import com.aozijx.passly.data.model.entity.LookupIndexEntity
import com.aozijx.passly.data.model.entity.VaultCredentialEntity
import com.aozijx.passly.data.model.entity.VaultMetadataEntity
import com.aozijx.passly.data.util.Clock
import com.aozijx.passly.domain.authentication.SessionStateProvider
import com.aozijx.passly.domain.authentication.VaultAccessState
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.repository.database.TransactionOperator
import com.aozijx.passly.domain.repository.entry.CommandRepository
import com.aozijx.passly.security.search.BlindIndexer
import com.github.f4b6a3.uuid.UuidCreator
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 命令 Repository 实现：负责业务逻辑修改，使用 withTransaction 保证原子性。
 * 写操作统一处理加解密、关联表和盲索引的同步更新。
 */
@Singleton
class CommandRepositoryImpl @Inject constructor(
    private val sessionState: VaultAccessState,
    private val stateProvider: SessionStateProvider,
    private val transactionOperator: TransactionOperator,
    private val sessionManager: UnifiedSessionManager,
    private val cryptoMapper: VaultEntryCryptoMapper,
    private val blindIndexer: BlindIndexer,
    private val clock: Clock
) : CommandRepository {

    override suspend fun insert(entry: VaultEntry): AppResult<Long> {
        stateProvider.assertWritable()
        return sessionManager.transaction {
            AppResult.runSuspendCatching("vault.insert") {
                withTransaction {
                    val entryId = entry.id.ifEmpty { UuidCreator.getTimeOrderedEpoch().toString() }
                    val now = clock.now()
                    val metaBlob = cryptoMapper.encryptMetadata(entry.metadata, entryId)
                    val credBlob = cryptoMapper.encryptCredential(entry.credential, entryId)

                    val metaEntity = VaultMetadataEntity(
                        entryId = entryId,
                        entryType = entry.entryType,
                        metadataBlob = metaBlob,
                        createdAt = now,
                        updatedAt = now
                    )
                    val credEntity = VaultCredentialEntity(
                        entryId = entryId,
                        credentialBlob = credBlob
                    )

                    metadataDao().insert(metaEntity)
                    credentialDao().insert(credEntity)

                    // 构建并写入盲索引
                    val indexedEntry = entry.copy(metadata = entry.metadata.copy(entryId = entryId))
                    val indexRecords = blindIndexer.index(entryId, indexedEntry.toLookupFields())
                    if (indexRecords.isNotEmpty()) {
                        lookupIndexDao().insertAll(indexRecords.toEntityList())
                    }

                    0L
                }
            }
        }
    }

    override suspend fun update(entry: VaultEntry, expectedVersion: Int): AppResult<Unit> {
        stateProvider.assertWritable()
        return sessionManager.transaction {
            AppResult.runSuspendCatching("vault.update") {
                withTransaction {
                    val oldMetaEntity = metadataDao().getById(entry.id) ?: return@withTransaction

                    // 乐观锁版本校验
                    if (oldMetaEntity.entryVersion != expectedVersion) {
                        throw Conflict(
                            "entry:${entry.id} version mismatch: expected=$expectedVersion, actual=${oldMetaEntity.entryVersion}"
                        )
                    }

                    val metaBlob = cryptoMapper.encryptMetadata(entry.metadata, entry.id)
                    val credBlob = cryptoMapper.encryptCredential(entry.credential, entry.id)

                    val metaEntity = VaultMetadataEntity(
                        entryId = entry.id,
                        entryType = entry.entryType,
                        metadataBlob = metaBlob,
                        vaultId = oldMetaEntity.vaultId,
                        entryVersion = oldMetaEntity.entryVersion + 1,
                        createdAt = oldMetaEntity.createdAt,
                        updatedAt = clock.now()
                    )
                    val credEntity = VaultCredentialEntity(
                        entryId = entry.id,
                        credentialBlob = credBlob
                    )

                    metadataDao().update(metaEntity)
                    credentialDao().update(credEntity)

                    // 重建盲索引：先删后插
                    lookupIndexDao().deleteByEntryId(entry.id)
                    val indexRecords = blindIndexer.index(entry.id, entry.toLookupFields())
                    if (indexRecords.isNotEmpty()) {
                        lookupIndexDao().insertAll(indexRecords.toEntityList())
                    }
                }
            }
        }
    }

    override suspend fun delete(entry: VaultEntry): AppResult<Unit> {
        stateProvider.assertWritable()
        return sessionManager.transaction {
            AppResult.runSuspendCatching("vault.delete") {
                withTransaction {
                    metadataDao().softDelete(entry.id, System.currentTimeMillis())
                    // 清除盲索引（外键 CASCADE 兜底）
                    lookupIndexDao().deleteByEntryId(entry.id)
                }
            }
        }
    }

    override suspend fun rebuildIndex(): AppResult<Int> {
        stateProvider.assertWritable()
        return sessionManager.transaction {
            AppResult.runSuspendCatching("vault.rebuildIndex") {
                withTransaction {
                    val metaEntities = metadataDao().getActive()
                    if (metaEntities.isEmpty()) return@withTransaction 0

                    val entryIds = metaEntities.map { it.entryId }
                    val credEntities = credentialDao().getByEntryIds(entryIds)
                    val credMap = credEntities.associateBy { it.entryId }

                    lookupIndexDao().clear()

                    var indexedCount = 0
                    for (metaEntity in metaEntities) {
                        val credEntity = credMap[metaEntity.entryId]
                        val entry = cryptoMapper.assembleEntry(metaEntity, credEntity) ?: continue
                        val indexRecords = blindIndexer.index(entry.id, entry.toLookupFields())
                        if (indexRecords.isNotEmpty()) {
                            lookupIndexDao().insertAll(indexRecords.toEntityList())
                            indexedCount++
                        }
                    }

                    AppLog.i(
                        TAG, "Rebuilt blind index for $indexedCount/${
                            metaEntities.size
                        } entries"
                    )

                    indexedCount
                }
            }
        }
    }

    /**
     * 将 [com.aozijx.passly.security.search.BlindIndexRecord] 转换为 [LookupIndexEntity]。
     */
    private companion object {
        private const val TAG = "CommandRepositoryImpl"

        private fun List<com.aozijx.passly.security.search.BlindIndexRecord>.toEntityList(): List<LookupIndexEntity> =
            map { record ->
                LookupIndexEntity(
                    entryId = record.entryId,
                    field = record.field,
                    keywordHash = record.keywordHash,
                    gramLength = record.gramLength,
                    weight = record.weight
                )
            }
    }
}
