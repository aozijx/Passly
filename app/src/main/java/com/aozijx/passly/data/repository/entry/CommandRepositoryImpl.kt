package com.aozijx.passly.data.repository.entry

import androidx.room.withTransaction
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.core.error.AuthFailed
import com.aozijx.passly.core.session.UnifiedSessionManager
import com.aozijx.passly.data.mapper.VaultEntryCryptoMapper
import com.aozijx.passly.data.model.entity.VaultCredentialEntity
import com.aozijx.passly.data.model.entity.VaultMetadataEntity
import com.aozijx.passly.data.util.Clock
import com.aozijx.passly.domain.authentication.VaultAccessState
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.repository.entry.CommandRepository
import com.github.f4b6a3.uuid.UuidCreator
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 命令 Repository 实现：负责业务逻辑修改，使用 withTransaction 保证原子性。
 * 写操作统一处理加解密和关联表的同步更新。
 */
@Singleton
class CommandRepositoryImpl @Inject constructor(
    private val sessionState: VaultAccessState,
    private val sessionManager: UnifiedSessionManager,
    private val cryptoMapper: VaultEntryCryptoMapper,
    private val clock: Clock
) : CommandRepository {

    override suspend fun insert(entry: VaultEntry): AppResult<Long> {
        if (sessionState.isLocked()) return AppResult.failure(AuthFailed("数据库未解锁"))
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
                    0L
                }
            }
        }
    }

    override suspend fun update(entry: VaultEntry): AppResult<Unit> {
        if (sessionState.isLocked()) return AppResult.failure(AuthFailed("数据库未解锁"))
        return sessionManager.transaction {
            AppResult.runSuspendCatching("vault.update") {
                withTransaction {
                    val oldMetaEntity = metadataDao().getById(entry.id) ?: return@withTransaction
                    val oldCredEntity = credentialDao().getByEntryId(entry.id)
                    val old = cryptoMapper.assembleEntry(oldMetaEntity, oldCredEntity)
                        ?: return@withTransaction

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
                }
            }
        }
    }

    override suspend fun delete(entry: VaultEntry): AppResult<Unit> {
        if (sessionState.isLocked()) return AppResult.failure(AuthFailed("数据库未解锁"))
        return sessionManager.transaction {
            AppResult.runSuspendCatching("vault.delete") {
                metadataDao().softDelete(entry.id, System.currentTimeMillis())
            }
        }
    }
}
