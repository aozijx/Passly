package com.aozijx.passly.data.repository.vault

import com.aozijx.passly.core.crypto.keystore.DatabasePassphraseManager
import com.aozijx.passly.core.error.AppError
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.data.entity.VaultHistoryEntity
import com.aozijx.passly.data.local.dao.VaultEntryDao
import com.aozijx.passly.data.local.dao.VaultHistoryDao
import com.aozijx.passly.data.mapper.toDomain
import com.aozijx.passly.data.mapper.toDomainList
import com.aozijx.passly.data.mapper.toEntity
import com.aozijx.passly.data.repository.vault.internal.diffFields
import com.aozijx.passly.data.repository.vault.internal.withLockGuard
import com.aozijx.passly.domain.model.EntryType
import com.aozijx.passly.domain.model.VaultEntry
import com.aozijx.passly.domain.model.VaultHistory
import com.aozijx.passly.domain.repository.vault.VaultRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultRepositoryImpl @Inject constructor(
    private val entryDao: VaultEntryDao,
    private val historyDao: VaultHistoryDao? = null,
    private val passphraseManager: DatabasePassphraseManager
) : VaultRepository {

    override val allEntries: Flow<List<VaultEntry>> =
        passphraseManager.withLockGuard({ emptyFlow() }) {
        entryDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeByType(type: EntryType): Flow<List<VaultEntry>> =
        passphraseManager.withLockGuard({ emptyFlow() }) {
            entryDao.observeByType(type.value).map { it.toDomainList() }
        }

    override suspend fun getEntryById(entryId: Int): VaultEntry? =
        passphraseManager.withLockGuard({ null }) {
        entryDao.getEntryById(entryId)?.toDomain()
    }

    override suspend fun getByType(type: EntryType): List<VaultEntry> =
        passphraseManager.withLockGuard({ emptyList() }) {
            entryDao.getByType(type.value).toDomainList()
        }

    override suspend fun getEntriesForIconResync(): List<VaultEntry> =
        passphraseManager.withLockGuard({ emptyList() }) {
            entryDao.getAll().toDomainList().filter {
                !it.associatedDomain.isNullOrEmpty()
            }
        }

    override suspend fun count(): Int = passphraseManager.withLockGuard({ 0 }) { entryDao.count() }

    override suspend fun countByType(type: EntryType): Int =
        passphraseManager.withLockGuard({ 0 }) { entryDao.countByType(type.value) }

    override suspend fun insert(entry: VaultEntry): AppResult<Long> =
        passphraseManager.withLockGuard({
            AppResult.failure(AppError.AuthFailed("数据库未解锁，无法保存条目"))
    }) {
            AppResult.runSuspendCatching("vault.insert") {
            val id = entryDao.insert(entry.toEntity())
            if (id > 0) {
                historyDao?.insertHistory(
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

    override suspend fun update(entry: VaultEntry): AppResult<Unit> {
        if (passphraseManager.isLocked) {
            return AppResult.failure(AppError.AuthFailed("数据库未解锁，无法更新条目"))
        }
        return AppResult.runSuspendCatching("vault.update") {
            if (historyDao != null) {
                val old = entryDao.getEntryById(entry.id)?.toDomain()
                if (old != null) {
                    val now = System.currentTimeMillis()
                    diffFields(old, entry).forEach { (field, oldVal, newVal) ->
                        historyDao.insertHistory(
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
            }
            entryDao.update(entry.toEntity())
        }
    }

    override suspend fun recordUsage(entryId: Int): AppResult<Unit> {
        if (passphraseManager.isLocked) {
            return AppResult.failure(AppError.AuthFailed("数据库未解锁，无法记录使用次数"))
        }
        return AppResult.runSuspendCatching("vault.recordUsage") {
            val entity = entryDao.getEntryById(entryId) ?: return@runSuspendCatching
            val entry = entity.toDomain()

            val updated = entry.copy(
                usageCount = entry.usageCount + 1,
                lastUsedAt = System.currentTimeMillis()
            )
            entryDao.update(updated.toEntity())

            historyDao?.insertHistory(
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

    override suspend fun delete(entry: VaultEntry): AppResult<Unit> {
        if (passphraseManager.isLocked) {
            return AppResult.failure(AppError.AuthFailed("数据库未解锁，无法删除条目"))
        }
        return AppResult.runSuspendCatching("vault.delete") {
            entryDao.deleteById(entry.id)
        }
    }

    override suspend fun deleteAll(): AppResult<Unit> {
        if (passphraseManager.isLocked) {
            return AppResult.failure(AppError.AuthFailed("数据库未解锁，无法删除所有条目"))
        }
        return AppResult.runSuspendCatching("vault.deleteAll") {
            entryDao.deleteAll()
        }
    }
}