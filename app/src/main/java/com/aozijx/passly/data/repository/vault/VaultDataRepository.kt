package com.aozijx.passly.data.repository.vault

import com.aozijx.passly.core.common.EntryType
import com.aozijx.passly.core.security.DatabasePassphraseManager
import com.aozijx.passly.data.entity.VaultHistoryEntity
import com.aozijx.passly.data.local.dao.VaultEntryDao
import com.aozijx.passly.data.local.dao.VaultHistoryDao
import com.aozijx.passly.data.mapper.toDomain
import com.aozijx.passly.data.mapper.toDomainList
import com.aozijx.passly.data.mapper.toEntity
import com.aozijx.passly.domain.model.core.VaultEntry
import com.aozijx.passly.domain.model.core.VaultHistory
import com.aozijx.passly.domain.repository.vault.VaultRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map

class VaultDataRepository(
    private val entryDao: VaultEntryDao,
    private val historyDao: VaultHistoryDao? = null
) : VaultRepository {

    private inline fun <T> withLockGuard(onLocked: () -> T, block: () -> T): T {
        return if (DatabasePassphraseManager.isLocked) onLocked() else block()
    }


    override val allEntries: Flow<List<VaultEntry>> = withLockGuard({ emptyFlow() }) {
        entryDao.observeAll().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observeByType(type: EntryType): Flow<List<VaultEntry>> =
        withLockGuard({ emptyFlow() }) {
            entryDao.observeByType(type.value).map { it.toDomainList() }
        }

    override suspend fun getEntryById(entryId: Int): VaultEntry? = withLockGuard({ null }) {
        entryDao.getEntryById(entryId)?.toDomain()
    }

    override suspend fun getByType(type: EntryType): List<VaultEntry> =
        withLockGuard({ emptyList() }) {
            entryDao.getByType(type.value).toDomainList()
        }

    override suspend fun getEntriesForIconResync(): List<VaultEntry> =
        withLockGuard({ emptyList() }) {
            entryDao.getAll().toDomainList().filter {
                !it.associatedDomain.isNullOrEmpty()
            }
        }

    override suspend fun count(): Int = withLockGuard({ 0 }) { entryDao.count() }

    override suspend fun countByType(type: EntryType): Int =
        withLockGuard({ 0 }) { entryDao.countByType(type.value) }

    override suspend fun insert(entry: VaultEntry): Long = withLockGuard({ -1L }) {
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

    override suspend fun update(entry: VaultEntry) {
        if (DatabasePassphraseManager.isLocked) return
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

    override suspend fun recordUsage(entryId: Int) {
        if (DatabasePassphraseManager.isLocked) return
        val entity = entryDao.getEntryById(entryId) ?: return
        val entry = entity.toDomain()

        val updated = entry.copy(
            usageCount = entry.usageCount + 1,
            lastUsedAt = System.currentTimeMillis()
        )
        entryDao.update(updated.toEntity())

        // 记录为 ACCESS 类型
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

    override suspend fun delete(entry: VaultEntry) {
        if (DatabasePassphraseManager.isLocked) return
        entryDao.delete(entry.toEntity())
    }

    override suspend fun deleteAll() {
        if (DatabasePassphraseManager.isLocked) return
        entryDao.deleteAll()
    }

    companion object {
        private fun diffFields(
            old: VaultEntry,
            new: VaultEntry
        ): List<Triple<String, String?, String?>> {
            val diffs = mutableListOf<Triple<String, String?, String?>>()
            fun check(name: String, oldVal: String?, newVal: String?) {
                if (oldVal != newVal) diffs.add(Triple(name, oldVal, newVal))
            }
            check("title", old.title, new.title)
            check("username", old.username, new.username)
            check("password", old.password, new.password)
            check("email", old.email, new.email)
            check("totpSecret", old.totpSecret, new.totpSecret)
            check("notes", old.notes, new.notes)
            check("cardCvv", old.cardCvv, new.cardCvv)
            check("cardExpiration", old.cardExpiration, new.cardExpiration)
            check("sshPrivateKey", old.sshPrivateKey, new.sshPrivateKey)
            check("cryptoSeedPhrase", old.cryptoSeedPhrase, new.cryptoSeedPhrase)
            check("idNumber", old.idNumber, new.idNumber)
            check("paymentPin", old.paymentPin, new.paymentPin)
            check("securityAnswer", old.securityAnswer, new.securityAnswer)
            return diffs
        }
    }
}