package com.aozijx.passly.data.local.database.dao.entry

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.aozijx.passly.data.local.database.entity.EntryActivityEntity
import com.aozijx.passly.data.local.database.entity.EntryLinkEntity
import com.aozijx.passly.data.local.database.entity.EntryEntity
import com.aozijx.passly.data.local.database.model.EntryPagingRow
import com.aozijx.passly.domain.entry.model.EntryType
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryQueryDao {

    // ---- observe (Flow) ----

    @Query("SELECT * FROM entries WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeActive(): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun observeDeleted(): Flow<List<EntryEntity>>

    // ---- paging (Paging 3) ----

    @Query("SELECT * FROM entries WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun pagingActive(): PagingSource<Int, EntryEntity>

    @Query("SELECT * FROM entries WHERE entryType = :entryType AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun pagingActiveByType(entryType: EntryType): PagingSource<Int, EntryEntity>

    @Query("SELECT * FROM entries WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun pagingDeleted(): PagingSource<Int, EntryEntity>

    @Query("SELECT * FROM entries WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    fun pagingActiveRecentlyCreated(): PagingSource<Int, EntryEntity>

    @RawQuery(
        observedEntities = [EntryEntity::class, EntryActivityEntity::class, EntryLinkEntity::class]
    )
    fun paging(query: SupportSQLiteQuery): PagingSource<Int, EntryPagingRow>

    // ---- get (suspend) ----

    @Query("SELECT * FROM entries WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    suspend fun getActive(): List<EntryEntity>

    @Query("SELECT * FROM entries WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    suspend fun getDeleted(): List<EntryEntity>

    @Query("SELECT * FROM entries ORDER BY updatedAt DESC")
    suspend fun getAll(): List<EntryEntity>

    @Query("SELECT * FROM entries WHERE entryId = :entryId LIMIT 1")
    suspend fun getById(entryId: String): EntryEntity?

    @Query("SELECT * FROM entries WHERE entryId IN (:entryIds)")
    suspend fun getByIds(entryIds: List<String>): List<EntryEntity>

    @Query("SELECT * FROM entries WHERE entryType = :entryType AND deletedAt IS NULL ORDER BY updatedAt DESC")
    suspend fun getActiveByType(entryType: EntryType): List<EntryEntity>

    // ---- exists ----

    @Query("SELECT EXISTS(SELECT 1 FROM entries WHERE entryId = :entryId)")
    suspend fun exists(entryId: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM entries WHERE entryId = :entryId AND deletedAt IS NULL)")
    suspend fun existsActive(entryId: String): Boolean

    // ---- count ----

    @Query("SELECT COUNT(*) FROM entries WHERE deletedAt IS NULL")
    suspend fun countActive(): Int

    @Query("SELECT COUNT(*) FROM entries WHERE deletedAt IS NULL AND entryType = :entryType")
    suspend fun countActiveByType(entryType: EntryType): Int

    @Query("SELECT COUNT(*) FROM entries WHERE deletedAt IS NOT NULL")
    suspend fun countDeleted(): Int

    @Query("SELECT entryId FROM entries WHERE deletedAt IS NULL AND searchIndexVersion < :currentVersion")
    suspend fun getActiveEntryIdsNeedingIndexRebuild(currentVersion: Int): List<String>

    @Query("SELECT * FROM entries WHERE entryId IN (:entryIds)")
    suspend fun getByIdsForMaintenance(entryIds: List<String>): List<EntryEntity>
}
