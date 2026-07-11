package com.aozijx.passly.data.repository.vault

import androidx.room.withTransaction
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.data.entity.VaultHistoryEntity
import com.aozijx.passly.data.local.DatabaseSessionManager
import com.aozijx.passly.data.mapper.toDomain
import com.aozijx.passly.data.mapper.toEntity
import com.aozijx.passly.data.repository.vault.internal.diffFields
import com.aozijx.passly.data.repository.vault.internal.failIfLocked
import com.aozijx.passly.data.repository.vault.internal.ifLockedReturn
import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.domain.model.VaultHistory
import com.aozijx.passly.domain.repository.vault.VaultRepository
import com.aozijx.passly.security.crypto.FieldEncryptor
import com.aozijx.passly.security.crypto.VaultLockManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultRepositoryImpl @Inject constructor(
    private val lockManager: VaultLockManager,
    private val sessionManager: DatabaseSessionManager,
    private val fieldEncryptor: FieldEncryptor
) : VaultRepository {

    override suspend fun getEntryById(entryId: Int): VaultEntry? {
        lockManager.ifLockedReturn { return null }
        return sessionManager.withDatabase {
            vaultEntryDao().getEntryById(entryId)?.toDomain(fieldEncryptor)
        }
    }

    override suspend fun getEntriesForIconResync(): List<VaultEntry> {
        lockManager.ifLockedReturn { return emptyList() }
        return sessionManager.withDatabase {
            vaultEntryDao().getAll().map { it.toDomain(fieldEncryptor) }
        }
    }

    override suspend fun insert(entry: VaultEntry): AppResult<Long> {
        lockManager.failIfLocked<Long>()?.let { return it }
        return sessionManager.withDatabase {
            AppResult.runSuspendCatching("vault.insert") {
                val id = vaultEntryDao().insert(entry.toEntity(fieldEncryptor))
                id
            }
        }
    }

    override suspend fun update(entry: VaultEntry): AppResult<Unit> {
        lockManager.failIfLocked<Unit>()?.let { return it }
        return sessionManager.withDatabase {
            AppResult.runSuspendCatching("vault.update") {
                withTransaction {
                    val old = vaultEntryDao().getEntryById(entry.id)?.toDomain(fieldEncryptor)
                    vaultEntryDao().update(entry.toEntity(fieldEncryptor))
                    if (old != null) {
                        diffFields(old, entry).forEach { (fieldName, oldValue, newValue) ->
                            vaultHistoryDao().insertHistory(
                                VaultHistoryEntity(
                                    entryId = entry.id,
                                    fieldName = fieldName,
                                    oldValue = oldValue,
                                    newValue = newValue,
                                    changeType = VaultHistory.HistoryType.UPDATE.value
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    override suspend fun recordUsage(entryId: Int): AppResult<Unit> {
        lockManager.failIfLocked<Unit>()?.let { return it }
        return sessionManager.withDatabase {
            AppResult.runSuspendCatching("vault.recordUsage") {
                val entity = vaultEntryDao().getEntryById(entryId)
                if (entity != null) {
                    val entry = entity.toDomain(fieldEncryptor)
                    val updated = entry.copy(usageCount = entry.usageCount + 1)
                    vaultEntryDao().update(updated.toEntity(fieldEncryptor))
                }
                Unit
            }
        }
    }

    override suspend fun delete(entry: VaultEntry): AppResult<Unit> {
        lockManager.failIfLocked<Unit>()?.let { return it }
        return sessionManager.withDatabase {
            AppResult.runSuspendCatching("vault.delete") {
                vaultEntryDao().deleteById(entry.id)
            }
        }
    }
}