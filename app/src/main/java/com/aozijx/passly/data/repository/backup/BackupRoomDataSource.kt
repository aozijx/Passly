package com.aozijx.passly.data.repository.backup

import android.content.Context
import androidx.room.withTransaction
import com.aozijx.passly.data.entity.VaultEntryEntity
import com.aozijx.passly.data.local.AppDatabase
import com.aozijx.passly.data.local.dao.VaultEntryDao
import com.aozijx.passly.domain.model.backup.BackupImportMode
import kotlinx.coroutines.flow.first

/**
 * 备份数据源的 Room 实现。
 */
internal class BackupRoomDataSource(
    context: Context,
    private val vaultEntryDao: VaultEntryDao? = null
) : BackupDataSource {
    private val appContext = context.applicationContext
    
    private fun getDao(): VaultEntryDao {
        return vaultEntryDao ?: AppDatabase.getDatabase(appContext).vaultEntryDao()
    }

    private fun getDb(): AppDatabase {
        return AppDatabase.getDatabase(appContext)
    }

    override suspend fun readAllEntries(): List<VaultEntryEntity> {
        return getDao().getAllEntries().first()
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