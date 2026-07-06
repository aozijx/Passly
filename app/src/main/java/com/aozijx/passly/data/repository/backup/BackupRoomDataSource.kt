package com.aozijx.passly.data.repository.backup

import android.content.Context
import androidx.room.withTransaction
import com.aozijx.passly.core.crypto.keystore.BiometricPassphraseBridge
import com.aozijx.passly.data.entity.VaultEntryEntity
import com.aozijx.passly.data.local.AppDatabase
import com.aozijx.passly.data.local.dao.VaultEntryDao
import com.aozijx.passly.domain.model.BackupImportMode


/**
 * 备份数据源的 Room 实现。
 */
internal class BackupRoomDataSource(
    context: Context,
    private val passphraseManager: BiometricPassphraseBridge,
    private val vaultEntryDao: VaultEntryDao? = null
) : BackupDataSource {
    private val appContext = context.applicationContext
    
    private fun getDao(): VaultEntryDao {
        return vaultEntryDao ?: AppDatabase.getDatabase(appContext, passphraseManager)
            .vaultEntryDao()
    }

    private fun getDb(): AppDatabase {
        return AppDatabase.getDatabase(appContext, passphraseManager)
    }

    override suspend fun readAllEntries(): List<VaultEntryEntity> {
        return getDao().getAll()
    }

    override suspend fun writeEntries(entries: List<VaultEntryEntity>, mode: BackupImportMode) {
        val db = getDb()
        db.withTransaction {
            if (mode == BackupImportMode.OVERWRITE) {
                getDao().deleteAll()
            }
            getDao().insertAll(entries)
        }
    }
}