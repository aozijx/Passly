package com.aozijx.passly.data.local.dao.revision

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import com.aozijx.passly.data.model.entity.EntryRevisionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryRevisionQueryDao {

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
}
