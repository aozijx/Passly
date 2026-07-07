package com.aozijx.passly.data.repository.vault

import androidx.room.withTransaction
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.data.entity.VaultHistoryEntity
import com.aozijx.passly.data.mapper.toDomain
import com.aozijx.passly.data.mapper.toDomainList
import com.aozijx.passly.data.mapper.toEntity
import com.aozijx.passly.data.repository.vault.internal.diffFields
import com.aozijx.passly.data.repository.vault.internal.failIfLocked
import com.aozijx.passly.data.repository.vault.internal.ifLockedReturn
import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.domain.model.VaultHistory
import com.aozijx.passly.domain.repository.vault.VaultRepository
import com.aozijx.passly.security.crypto.DatabaseSessionManager
import com.aozijx.passly.security.crypto.VaultLockManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultRepositoryImpl @Inject constructor(
    private val lockManager: VaultLockManager,
    private val sessionManager: DatabaseSessionManager
) : VaultRepository {

    override suspend fun getEntryById(entryId: Int): VaultEntry? {
        lockManager.ifLockedReturn { return null }
        return sessionManager.withDatabase {
            vaultEntryDao().getEntryById(entryId)?.toDomain()
        }
    }

    override suspend fun getEntriesForIconResync(): List<VaultEntry> {
        lockManager.ifLockedReturn { return emptyList() }
        return sessionManager.withDatabase {
            vaultEntryDao().getAll().toDomainList()
                .filter { !it.associatedDomain.isNullOrEmpty() }
        }
    }

    override suspend fun insert(entry: VaultEntry): AppResult<Long> {
        lockManager.failIfLocked<Long>("数据库未解锁，无法保存条目")?.let { return it }
        return AppResult.runSuspendCatching("vault.insert") {
            sessionManager.withDatabase {
                withTransaction {
                    val id = vaultEntryDao().insert(entry.toEntity())
                    if (id > 0) {
                        vaultHistoryDao().insertHistory(
                            VaultHistoryEntity(
                                entryId = id.toInt(),
                                fieldName = "entry",
                                oldValue = null,
                                newValue = entry.title,
                                changeType = VaultHistory.HistoryType.CREATE.value,
                                changedAt = System.currentTimeMillis()
                            )
                        )
                    }
                    id
                }
            }
        }
    }

    override suspend fun update(entry: VaultEntry): AppResult<Unit> {
        lockManager.failIfLocked<Unit>("数据库未解锁，无法更新条目")?.let { return it }
        return AppResult.runSuspendCatching("vault.update") {
            sessionManager.withDatabase {
                withTransaction {
                    val old = vaultEntryDao().getEntryById(entry.id)?.toDomain()
                    if (old != null) {
                        val now = System.currentTimeMillis()
                        diffFields(old, entry).forEach { (field, oldVal, newVal) ->
                            vaultHistoryDao().insertHistory(
                                VaultHistoryEntity(
                                    entryId = entry.id,
                                    fieldName = field,
                                    oldValue = oldVal,
                                    newValue = newVal,
                                    changeType = VaultHistory.HistoryType.UPDATE.value,
                                    changedAt = now
                                )
                            )
                        }
                    }
                    vaultEntryDao().update(entry.toEntity())
                }
            }
        }
    }

    override suspend fun recordUsage(entryId: Int): AppResult<Unit> {
        lockManager.failIfLocked<Unit>("数据库未解锁，无法记录使用次数")?.let { return it }
        return AppResult.runSuspendCatching("vault.recordUsage") {
            sessionManager.withDatabase {
                withTransaction {
                    val entity = vaultEntryDao().getEntryById(entryId) ?: return@withTransaction
                    val entry = entity.toDomain()

                    val updated = entry.copy(
                        usageCount = entry.usageCount + 1,
                        lastUsedAt = System.currentTimeMillis()
                    )
                    vaultEntryDao().update(updated.toEntity())

                    vaultHistoryDao().insertHistory(
                        VaultHistoryEntity(
                            entryId = entryId,
                            fieldName = "details",
                            oldValue = null,
                            newValue = null,
                            changeType = VaultHistory.HistoryType.ACCESS.value,
                            changedAt = System.currentTimeMillis()
                        )
                    )
                }
            }
        }
    }

    override suspend fun delete(entry: VaultEntry): AppResult<Unit> {
        lockManager.failIfLocked<Unit>("数据库未解锁，无法删除条目")?.let { return it }
        return AppResult.runSuspendCatching("vault.delete") {
            sessionManager.withDatabase {
                vaultEntryDao().deleteById(entry.id)
            }
        }
    }
}