package com.aozijx.passly.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aozijx.passly.data.local.database.DatabaseConfig
import com.aozijx.passly.data.model.entity.VaultMetadataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultMetadataDao {

    // ---- observe (Flow) ----

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_METADATA} WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeActive(): Flow<List<VaultMetadataEntity>>

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_METADATA} WHERE entryType = :entryType AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeByType(entryType: Int): Flow<List<VaultMetadataEntity>>

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_METADATA} WHERE entryType IN (:entryTypes) AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun observeByEntryTypes(entryTypes: List<Int>): Flow<List<VaultMetadataEntity>>

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
    suspend fun getByType(entryType: Int): List<VaultMetadataEntity>

    // ---- pagination (offset-based) ----

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_METADATA} WHERE deletedAt IS NULL ORDER BY updatedAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getPage(limit: Int, offset: Int): List<VaultMetadataEntity>

    // ---- paging (Paging 3) ----

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_METADATA} WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun pagingActive(): PagingSource<Int, VaultMetadataEntity>

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_METADATA} WHERE entryType = :entryType AND deletedAt IS NULL ORDER BY updatedAt DESC")
    fun pagingByType(entryType: Int): PagingSource<Int, VaultMetadataEntity>

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_METADATA} WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun pagingDeleted(): PagingSource<Int, VaultMetadataEntity>

    // 收藏分页预留 — favorite 字段在 Payload 中，后续若提升到 Entity 层再启用
    // @Query("SELECT * FROM ${DatabaseConfig.TABLE_METADATA} WHERE deletedAt IS NULL AND favorite = 1 ORDER BY updatedAt DESC")
    // fun pagingFavorite(): PagingSource<Int, VaultMetadataEntity>

    // ---- recent ----

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_METADATA} WHERE deletedAt IS NULL ORDER BY updatedAt DESC LIMIT :limit")
    suspend fun getRecentlyUpdated(limit: Int): List<VaultMetadataEntity>

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_METADATA} WHERE deletedAt IS NULL ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentlyCreated(limit: Int): List<VaultMetadataEntity>

    // ---- paging recent ----

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_METADATA} WHERE deletedAt IS NULL ORDER BY updatedAt DESC")
    fun pagingRecentlyUpdated(): PagingSource<Int, VaultMetadataEntity>

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_METADATA} WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    fun pagingRecentlyCreated(): PagingSource<Int, VaultMetadataEntity>

    // ---- exists ----

    @Query("SELECT EXISTS(SELECT 1 FROM ${DatabaseConfig.TABLE_METADATA} WHERE entryId = :entryId)")
    suspend fun exists(entryId: String): Boolean

    // ---- count ----

    @Query("SELECT COUNT(*) FROM ${DatabaseConfig.TABLE_METADATA} WHERE deletedAt IS NULL")
    suspend fun countActive(): Int

    @Query("SELECT COUNT(*) FROM ${DatabaseConfig.TABLE_METADATA} WHERE deletedAt IS NULL AND entryType = :entryType")
    suspend fun countByType(entryType: Int): Int

    @Query("SELECT COUNT(*) FROM ${DatabaseConfig.TABLE_METADATA} WHERE deletedAt IS NOT NULL")
    suspend fun countDeleted(): Int

    // ---- insert / update ----

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: VaultMetadataEntity)

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