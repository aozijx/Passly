package com.aozijx.passly.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aozijx.passly.data.model.entity.EntryRevisionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryRevisionDao {

    // ---- observe (Flow) ----

    @Query("SELECT * FROM entry_revisions WHERE entryId = :entryId ORDER BY version DESC")
    fun observeByEntryId(entryId: String): Flow<List<EntryRevisionEntity>>

    @Query("SELECT * FROM entry_revisions ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<EntryRevisionEntity>>

    // ---- paging (Paging 3) ----

    @Query("SELECT * FROM entry_revisions WHERE entryId = :entryId ORDER BY version DESC")
    fun pagingByEntryId(entryId: String): PagingSource<Int, EntryRevisionEntity>

    @Query("SELECT * FROM entry_revisions ORDER BY createdAt DESC")
    fun pagingAll(): PagingSource<Int, EntryRevisionEntity>

    // ---- get (suspend) ----

    @Query("SELECT * FROM entry_revisions WHERE revisionId = :revisionId LIMIT 1")
    suspend fun getById(revisionId: String): EntryRevisionEntity?

    @Query("SELECT * FROM entry_revisions WHERE entryId = :entryId AND version = :version LIMIT 1")
    suspend fun getByVersion(entryId: String, version: Int): EntryRevisionEntity?

    @Query("SELECT COALESCE(MAX(version), 0) + 1 FROM entry_revisions WHERE entryId = :entryId")
    suspend fun getNextVersion(entryId: String): Int

    // ---- exists ----

    @Query("SELECT EXISTS(SELECT 1 FROM entry_revisions WHERE revisionId = :revisionId)")
    suspend fun exists(revisionId: String): Boolean

    // ---- count ----

    @Query("SELECT COUNT(*) FROM entry_revisions WHERE entryId = :entryId")
    suspend fun countByEntryId(entryId: String): Int

    // === Strict Insert (ignore duplicate) ===

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertStrict(revision: EntryRevisionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllStrict(revisions: List<EntryRevisionEntity>)

    // === Import Upsert ===

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertForImport(revision: EntryRevisionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllForImport(revisions: List<EntryRevisionEntity>)

    // === Maintenance API ===

    @Query("DELETE FROM entry_revisions WHERE entryId = :entryId")
    suspend fun deleteByEntryId(entryId: String)

    @Query("DELETE FROM entry_revisions WHERE entryId = :entryId AND version <= (SELECT version FROM entry_revisions WHERE entryId = :entryId ORDER BY version DESC LIMIT 1 OFFSET :keepCount)")
    suspend fun deleteOldVersions(entryId: String, keepCount: Int)

    @Query("DELETE FROM entry_revisions WHERE createdAt < :timestamp")
    suspend fun deleteBefore(timestamp: Long)

    @Query("DELETE FROM entry_revisions")
    suspend fun clear(): Int
}
