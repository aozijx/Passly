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

    /**
     * 观察指定条目的所有历史快照。
     * 预留：后续可支持按 changeType 过滤。
     */
    @Query("SELECT * FROM ${DatabaseSchema.TABLE_HISTORY} WHERE entryId = :entryId ORDER BY version DESC")
    fun observeByEntryId(entryId: String): Flow<List<VaultSnapshotEntity>>

    /**
     * 观察所有历史快照（全局时间线）。
     * 预留：后续可支持按时间范围分页。
     */
    @Query("SELECT * FROM ${DatabaseSchema.TABLE_HISTORY} ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<VaultSnapshotEntity>>

    // ---- paging (Paging 3) ----

    /** 按条目分页。 */
    @Query("SELECT * FROM ${DatabaseSchema.TABLE_HISTORY} WHERE entryId = :entryId ORDER BY version DESC")
    fun pagingByEntryId(entryId: String): PagingSource<Int, VaultSnapshotEntity>

    /** 全局历史分页（按时间倒序）。 */
    @Query("SELECT * FROM ${DatabaseSchema.TABLE_HISTORY} ORDER BY createdAt DESC")
    fun pagingAll(): PagingSource<Int, VaultSnapshotEntity>

    // ---- get (suspend) ----

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_HISTORY} WHERE historyId = :historyId LIMIT 1")
    suspend fun getById(historyId: String): VaultSnapshotEntity?

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_HISTORY} WHERE entryId = :entryId AND version = :version LIMIT 1")
    suspend fun getByVersion(entryId: String, version: Int): VaultSnapshotEntity?

    /** 返回指定条目下一个可用的版本号。 */
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

    // ---- delete / cleanup ----

    @Query("DELETE FROM ${DatabaseSchema.TABLE_HISTORY} WHERE entryId = :entryId")
    suspend fun deleteByEntryId(entryId: String)

    /**
     * 按条目保留最近 N 个版本，删除更早的快照。
     * 预留：后续可支持全局裁剪策略（如总版本数上限）。
     */
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

    /** 按清理时间清理快照（用于自动维护策略）。 */
    @Query("DELETE FROM ${DatabaseSchema.TABLE_HISTORY} WHERE createdAt < :timestamp")
    suspend fun deleteBefore(timestamp: Long)

    /** 清空全表，返回删除行数。 */
    @Query("DELETE FROM ${DatabaseSchema.TABLE_HISTORY}")
    suspend fun clear(): Int
}
