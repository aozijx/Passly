package com.aozijx.passly.data.repository.entry

import androidx.room.withTransaction
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.core.error.AuthFailed
import com.aozijx.passly.data.local.database.DatabaseSession
import com.aozijx.passly.data.mapper.VaultEntryCryptoMapper
import com.aozijx.passly.data.model.entity.VaultCredentialEntity
import com.aozijx.passly.data.model.entity.VaultMetadataEntity
import com.aozijx.passly.data.util.Clock
import com.aozijx.passly.domain.authentication.VaultAccessState
import com.aozijx.passly.domain.model.entry.VaultEntry
import com.aozijx.passly.domain.repository.entry.VaultEntryRepository
import com.github.f4b6a3.uuid.UuidCreator
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultEntryRepositoryImpl @Inject constructor(
    private val sessionState: VaultAccessState,
    private val sessionManager: DatabaseSession,
    private val cryptoMapper: VaultEntryCryptoMapper,
    private val clock: Clock
) : VaultEntryRepository {

    override suspend fun getById(entryId: String): VaultEntry? {
        if (sessionState.isLocked()) return null
        return sessionManager.withDatabase {
            val metaEntity = metadataDao().getById(entryId) ?: return@withDatabase null
            val credEntity = credentialDao().getByEntryId(entryId)
            cryptoMapper.assembleEntry(metaEntity, credEntity)
        }
    }

    override suspend fun getEntriesForIconResync(): List<VaultEntry> {
        if (sessionState.isLocked()) return emptyList()
        return sessionManager.withDatabase {
            val metaEntities = metadataDao().getActive()
            val credEntities = credentialDao().getByEntryIds(metaEntities.map { it.entryId })
            val credMap = credEntities.associateBy { it.entryId }
            metaEntities.mapNotNull { cryptoMapper.assembleEntry(it, credMap[it.entryId]) }
        }
    }

    override suspend fun insert(entry: VaultEntry): AppResult<Long> {
        if (sessionState.isLocked()) return AppResult.failure(AuthFailed("数据库未解锁"))
        return sessionManager.withDatabase {
            AppResult.runSuspendCatching("vault.insert") {
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

    override suspend fun update(entry: VaultEntry): AppResult<Unit> {
        if (sessionState.isLocked()) return AppResult.failure(AuthFailed("数据库未解锁"))
        return sessionManager.withDatabase {
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
        return sessionManager.withDatabase {
            AppResult.runSuspendCatching("vault.delete") {
                metadataDao().softDelete(entry.id, System.currentTimeMillis())
            }
        }
    }

    override suspend fun count(): Int {
        if (sessionState.isLocked()) return 0
        return sessionManager.withDatabase { metadataDao().countActive() }
    }
}
