package com.aozijx.passly.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aozijx.passly.data.local.database.DatabaseSchema
import com.aozijx.passly.data.model.entity.VaultSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultHistoryDao {

    // ---- observe (Flow) ----

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_HISTORY} WHERE entryId = :entryId ORDER BY version DESC")
    fun observeByEntryId(entryId: String): Flow<List<VaultSnapshotEntity>>

    // ---- paging (Paging 3) ----

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_HISTORY} WHERE entryId = :entryId ORDER BY version DESC")
    fun pagingByEntryId(entryId: String): PagingSource<Int, VaultSnapshotEntity>

    // ---- get (suspend) ----

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_HISTORY} WHERE historyId = :historyId LIMIT 1")
    suspend fun getById(historyId: String): VaultSnapshotEntity?

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_HISTORY} WHERE entryId = :entryId AND version = :version LIMIT 1")
    suspend fun getByVersion(entryId: String, version: Int): VaultSnapshotEntity?

    @Query("SELECT COALESCE(MAX(version), 0) + 1 FROM ${DatabaseSchema.TABLE_HISTORY} WHERE entryId = :entryId")
    suspend fun getNextVersion(entryId: String): Int

    // ---- exists ----

    @Query("SELECT EXISTS(SELECT 1 FROM ${DatabaseSchema.TABLE_HISTORY} WHERE historyId = :historyId)")
    suspend fun exists(historyId: String): Boolean

    // ---- count ----

    @Query("SELECT COUNT(*) FROM ${DatabaseSchema.TABLE_HISTORY} WHERE entryId = :entryId")
    suspend fun countByEntryId(entryId: String): Int

    // ---- insert ----

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(history: VaultSnapshotEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(histories: List<VaultSnapshotEntity>)

    // ---- delete ----

    @Query("DELETE FROM ${DatabaseSchema.TABLE_HISTORY} WHERE entryId = :entryId")
    suspend fun deleteByEntryId(entryId: String)

    @Query(
        """
        DELETE FROM ${DatabaseSchema.TABLE_HISTORY}
        WHERE entryId = :entryId
        AND version <= (
            SELECT version
            FROM ${DatabaseSchema.TABLE_HISTORY}
            WHERE entryId = :entryId
            ORDER BY version DESC
            LIMIT 1 OFFSET :keepCount
        )
    """
    )
    suspend fun deleteOldVersions(entryId: String, keepCount: Int)

    @Query("DELETE FROM ${DatabaseSchema.TABLE_HISTORY}")
    suspend fun clear()
}