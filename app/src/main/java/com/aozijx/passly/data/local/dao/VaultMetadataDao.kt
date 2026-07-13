package com.aozijx.passly.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aozijx.passly.data.local.database.DatabaseConfig
import com.aozijx.passly.data.model.entity.EntryType
import com.aozijx.passly.data.model.entity.VaultMetadataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultMetadataDao {

    // ---- observe (Flow) ----

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_METADATA} WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeActive(): Flow<List<VaultMetadataEntity>>

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_METADATA} WHERE entryType = :entryType AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeByType(entryType: EntryType): Flow<List<VaultMetadataEntity>>

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_METADATA} WHERE entryType IN (:entryTypes) AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeByEntryTypes(entryTypes: List<EntryType>): Flow<List<VaultMetadataEntity>>

    // ---- paging (Paging 3) ----

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_METADATA} WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun pagingActive(): PagingSource<Int, VaultMetadataEntity>

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_METADATA} WHERE entryType = :entryType AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun pagingByType(entryType: EntryType): PagingSource<Int, VaultMetadataEntity>

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_METADATA} WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun pagingDeleted(): PagingSource<Int, VaultMetadataEntity>

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_METADATA} WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun pagingRecentlyUpdated(): PagingSource<Int, VaultMetadataEntity>

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_METADATA} WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    fun pagingRecentlyCreated(): PagingSource<Int, VaultMetadataEntity>

    // ---- get (suspend) ----

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_METADATA} WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    suspend fun getActive(): List<VaultMetadataEntity>

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_METADATA} WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    suspend fun getDeleted(): List<VaultMetadataEntity>

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_METADATA} ORDER BY updatedAt DESC")
    suspend fun getAll(): List<VaultMetadataEntity>

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_METADATA} WHERE entryId = :entryId LIMIT 1")
    suspend fun getById(entryId: String): VaultMetadataEntity?

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_METADATA} WHERE entryId IN (:entryIds)")
    suspend fun getByIds(entryIds: List<String>): List<VaultMetadataEntity>

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_METADATA} WHERE entryType = :entryType AND deletedAt IS NULL ORDER BY updatedAt DESC")
    suspend fun getByType(entryType: EntryType): List<VaultMetadataEntity>

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_METADATA} WHERE deletedAt IS NULL ORDER BY updatedAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getPage(limit: Int, offset: Int): List<VaultMetadataEntity>

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_METADATA} WHERE deletedAt IS NULL ORDER BY updatedAt DESC LIMIT :limit")
    suspend fun getRecentlyUpdated(limit: Int): List<VaultMetadataEntity>

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_METADATA} WHERE deletedAt IS NULL ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentlyCreated(limit: Int): List<VaultMetadataEntity>

    // ---- exists ----

    @Query("SELECT EXISTS(SELECT 1 FROM ${DatabaseConfig.TABLE_METADATA} WHERE entryId = :entryId)")
    suspend fun exists(entryId: String): Boolean

    // ---- count ----

    @Query("SELECT COUNT(*) FROM ${DatabaseConfig.TABLE_METADATA} WHERE deletedAt IS NULL")
    suspend fun countActive(): Int

    @Query("SELECT COUNT(*) FROM ${DatabaseConfig.TABLE_METADATA} WHERE deletedAt IS NULL AND entryType = :entryType")
    suspend fun countByType(entryType: EntryType): Int

    @Query("SELECT COUNT(*) FROM ${DatabaseConfig.TABLE_METADATA} WHERE deletedAt IS NOT NULL")
    suspend fun countDeleted(): Int

    // ---- insert / update ----

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: VaultMetadataEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<VaultMetadataEntity>)

    @Update
    suspend fun update(entry: VaultMetadataEntity)

    // ---- soft delete / restore ----

    @Query("UPDATE ${DatabaseConfig.TABLE_METADATA} SET deletedAt = :timestamp, updatedAt = :timestamp WHERE entryId = :entryId")
    suspend fun softDelete(entryId: String, timestamp: Long)

    @Query("UPDATE ${DatabaseConfig.TABLE_METADATA} SET deletedAt = NULL, updatedAt = :now WHERE entryId = :entryId")
    suspend fun restore(entryId: String, now: Long)

    // ---- delete ----

    @Query("DELETE FROM ${DatabaseConfig.TABLE_METADATA} WHERE entryId = :entryId")
    suspend fun deleteById(entryId: String)

    @Query("DELETE FROM ${DatabaseConfig.TABLE_METADATA} WHERE deletedAt IS NOT NULL AND deletedAt < :before")
    suspend fun purgeDeleted(before: Long)

    @Query("DELETE FROM ${DatabaseConfig.TABLE_METADATA}")
    suspend fun clear()
}