package com.aozijx.passly.data.local.dao.entry

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import com.aozijx.passly.data.model.entity.EntryEntity
import com.aozijx.passly.domain.entry.model.EntryType
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryQueryDao {

    // ---- observe (Flow) ----

    @Query("SELECT * FROM vault_entries WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeActive(): Flow<List<EntryEntity>>

    @Query("SELECT * FROM vault_entries WHERE entryType = :entryType AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeActiveByType(entryType: EntryType): Flow<List<EntryEntity>>

    @Query("SELECT * FROM vault_entries WHERE deletedAt IS NULL AND (capabilityFlags & :capability) != 0 ORDER BY updatedAt DESC")
    fun observeActiveWithCapability(capability: Int): Flow<List<EntryEntity>>

    @Query("SELECT * FROM vault_entries WHERE entryId IN (:entryIds) AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeActiveByIds(entryIds: List<String>): Flow<List<EntryEntity>>

    @Query("SELECT DISTINCT entryType FROM vault_entries WHERE deletedAt IS NULL ORDER BY entryType")
    fun observeDistinctActiveEntryTypes(): Flow<List<EntryType>>

    // ---- paging (Paging 3) ----

    @Query("SELECT * FROM vault_entries WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun pagingActive(): PagingSource<Int, EntryEntity>

    @Query("SELECT * FROM vault_entries WHERE entryType = :entryType AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun pagingActiveByType(entryType: EntryType): PagingSource<Int, EntryEntity>

    @Query("SELECT * FROM vault_entries WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun pagingDeleted(): PagingSource<Int, EntryEntity>

    @Query("SELECT * FROM vault_entries WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    fun pagingActiveRecentlyCreated(): PagingSource<Int, EntryEntity>

    // ---- get (suspend) ----

    @Query("SELECT * FROM vault_entries WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    suspend fun getActive(): List<EntryEntity>

    @Query("SELECT * FROM vault_entries WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    suspend fun getDeleted(): List<EntryEntity>

    @Query("SELECT * FROM vault_entries ORDER BY updatedAt DESC")
    suspend fun getAll(): List<EntryEntity>

    @Query("SELECT * FROM vault_entries WHERE entryId = :entryId LIMIT 1")
    suspend fun getById(entryId: String): EntryEntity?

    @Query("SELECT * FROM vault_entries WHERE entryId IN (:entryIds)")
    suspend fun getByIds(entryIds: List<String>): List<EntryEntity>

    @Query("SELECT * FROM vault_entries WHERE entryType = :entryType AND deletedAt IS NULL ORDER BY updatedAt DESC")
    suspend fun getActiveByType(entryType: EntryType): List<EntryEntity>

    @Query("SELECT * FROM vault_entries WHERE deletedAt IS NULL ORDER BY updatedAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getActivePage(limit: Int, offset: Int): List<EntryEntity>

    @Query("SELECT * FROM vault_entries WHERE deletedAt IS NULL ORDER BY updatedAt DESC LIMIT :limit")
    suspend fun getActiveRecentlyUpdated(limit: Int): List<EntryEntity>

    @Query("SELECT * FROM vault_entries WHERE deletedAt IS NULL ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getActiveRecentlyCreated(limit: Int): List<EntryEntity>

    // ---- exists ----

    @Query("SELECT EXISTS(SELECT 1 FROM vault_entries WHERE entryId = :entryId)")
    suspend fun exists(entryId: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM vault_entries WHERE entryId = :entryId AND deletedAt IS NULL)")
    suspend fun existsActive(entryId: String): Boolean

    // ---- count ----

    @Query("SELECT COUNT(*) FROM vault_entries WHERE deletedAt IS NULL")
    suspend fun countActive(): Int

    @Query("SELECT COUNT(*) FROM vault_entries WHERE deletedAt IS NULL AND entryType = :entryType")
    suspend fun countActiveByType(entryType: EntryType): Int

    @Query("SELECT COUNT(*) FROM vault_entries WHERE deletedAt IS NOT NULL")
    suspend fun countDeleted(): Int

    @Query("SELECT entryId FROM vault_entries WHERE deletedAt IS NULL AND searchIndexVersion < :currentVersion")
    suspend fun getActiveEntryIdsNeedingIndexRebuild(currentVersion: Int): List<String>

    @Query("SELECT * FROM vault_entries WHERE entryId IN (:entryIds)")
    suspend fun getByIdsForMaintenance(entryIds: List<String>): List<EntryEntity>
}
