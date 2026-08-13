package com.aozijx.passly.data.local.database.dao.revision

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.aozijx.passly.data.local.database.entity.EntryRevisionEntity

@Dao
interface EntryRevisionCommandDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStrict(revision: EntryRevisionEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAllStrict(revisions: List<EntryRevisionEntity>)

    @Upsert
    suspend fun upsertForImport(revision: EntryRevisionEntity)

    @Upsert
    suspend fun upsertAllForImport(revisions: List<EntryRevisionEntity>)

    @Query("DELETE FROM entry_revisions WHERE entryId = :entryId")
    suspend fun deleteByEntryId(entryId: String): Int

    @Query("DELETE FROM entry_revisions WHERE entryId = :entryId AND version <= (SELECT version FROM entry_revisions WHERE entryId = :entryId ORDER BY version DESC LIMIT 1 OFFSET :keepCount)")
    suspend fun deleteOldVersions(entryId: String, keepCount: Int): Int

    /**
     * 全局仅保留最新的 [keepCount] 条修订。
     *
     * createdAt 相同时使用 revisionId 作为稳定的次级顺序，避免裁剪结果依赖 SQLite
     * 未定义的同行顺序。调用方必须与新修订写入处于同一事务。
     */
    @Query(
        """
        DELETE FROM entry_revisions
        WHERE revisionId IN (
            SELECT revisionId
            FROM entry_revisions
            ORDER BY createdAt DESC, revisionId DESC
            LIMIT -1 OFFSET :keepCount
        )
        """
    )
    suspend fun deleteOldestBeyondGlobalLimit(keepCount: Int): Int

    /** 删除当前位于回收站的全部 Entry 的修订，供清空回收站事务显式调用。 */
    @Query(
        """
        DELETE FROM entry_revisions
        WHERE entryId IN (SELECT entryId FROM entries WHERE deletedAt IS NOT NULL)
        """
    )
    suspend fun deleteForDeletedEntries(): Int

    @Query("DELETE FROM entry_revisions WHERE createdAt < :timestamp")
    suspend fun deleteBefore(timestamp: Long): Int
}
