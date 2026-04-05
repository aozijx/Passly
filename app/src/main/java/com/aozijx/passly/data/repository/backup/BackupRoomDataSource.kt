package com.aozijx.passly.data.repository.backup

import android.content.Context
import androidx.room.withTransaction
import com.aozijx.passly.core.backup.BackupDataSource
import com.aozijx.passly.core.backup.BackupImportMode
import com.aozijx.passly.data.entity.VaultEntryEntity
import com.aozijx.passly.data.local.AppDatabase
import kotlinx.coroutines.flow.first

class BackupRoomDataSource(context: Context) : BackupDataSource {
    private val appContext = context.applicationContext

    override suspend fun readAllEntries(): List<VaultEntryEntity> {
        return AppDatabase.getDatabase(appContext).vaultDao().getAllEntries().first()
    }

    override suspend fun writeEntries(entries: List<VaultEntryEntity>, mode: BackupImportMode) {
        val db = AppDatabase.getDatabase(appContext)
        db.withTransaction {
            if (mode == BackupImportMode.OVERWRITE) {
                db.vaultDao().deleteAll()
            }
            db.vaultDao().insertAll(entries)
        }
    }
}
