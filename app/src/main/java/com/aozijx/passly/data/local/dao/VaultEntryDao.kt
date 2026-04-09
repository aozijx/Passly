package com.aozijx.passly.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.aozijx.passly.data.entity.VaultEntryEntity
import com.aozijx.passly.data.local.config.DatabaseConfig
import com.aozijx.passly.domain.model.presentation.VaultSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultEntryDao {
    @Query("SELECT * FROM ${DatabaseConfig.TABLE_ENTRIES} ORDER BY favorite DESC, usageCount DESC, createdAt DESC")
    fun getAllEntries(): Flow<List<VaultEntryEntity>>

    /**
     * 核心组合查询：接管了搜索、分类、全量、以及类型过滤（密码/TOTP）
     */
    @Query(
        """
        SELECT id, title, category, entryType, username,
               iconName, iconCustomPath, associatedAppPackage, associatedDomain,
               totpSecret, totpPeriod, totpDigits, totpAlgorithm,
               favorite, createdAt, updatedAt
        FROM ${DatabaseConfig.TABLE_ENTRIES}
        WHERE (:query = '' OR title LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%')
          AND (:category IS NULL OR category = :category)
          AND (
                :entryFilter = 0 
                OR (:entryFilter = 1 AND (totpSecret IS NULL OR totpSecret = ''))
                OR (:entryFilter = 2 AND (totpSecret IS NOT NULL AND totpSecret != ''))
          )
        ORDER BY favorite DESC, usageCount DESC, createdAt DESC
        """
    )
    fun observeEntrySummariesByDemand(
        query: String, category: String?, entryFilter: Int
    ): Flow<List<VaultSummary>>

    @Query(
        """
        SELECT DISTINCT category FROM ${DatabaseConfig.TABLE_ENTRIES} 
        WHERE category IS NOT NULL AND category != ''
          AND (
                :entryFilter = 0 
                OR (:entryFilter = 1 AND (totpSecret IS NULL OR totpSecret = ''))
                OR (:entryFilter = 2 AND (totpSecret IS NOT NULL AND totpSecret != ''))
          )
        """
    )
    fun getCategoriesByFilter(entryFilter: Int): Flow<List<String>>

    @Query("SELECT DISTINCT category FROM ${DatabaseConfig.TABLE_ENTRIES} WHERE category IS NOT NULL AND category != ''")
    fun getAllCategories(): Flow<List<String>>

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_ENTRIES} WHERE category = :category ORDER BY createdAt DESC")
    fun getEntriesByCategory(category: String): Flow<List<VaultEntryEntity>>

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_ENTRIES} WHERE id = :entryId LIMIT 1")
    suspend fun getEntryById(entryId: Int): VaultEntryEntity?

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_ENTRIES} WHERE id IN (:entryIds)")
    suspend fun getEntriesByIds(entryIds: List<Int>): List<VaultEntryEntity>

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_ENTRIES} WHERE iconCustomPath LIKE 'http://%' OR iconCustomPath LIKE 'https://%'")
    suspend fun getEntriesWithRemoteIconPath(): List<VaultEntryEntity>

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_ENTRIES} WHERE associatedDomain IS NOT NULL AND associatedDomain != ''")
    suspend fun getEntriesForIconResync(): List<VaultEntryEntity>

    @Query(
        """
        SELECT * FROM ${DatabaseConfig.TABLE_ENTRIES}
        WHERE ((:packageName IS NOT NULL AND associatedAppPackage = :packageName) OR (:webDomain IS NOT NULL AND associatedDomain = :webDomain))
        ORDER BY CASE WHEN (:packageName IS NOT NULL AND associatedAppPackage = :packageName) THEN 0 ELSE 1 END,
                 favorite DESC, usageCount DESC, IFNULL(updatedAt, createdAt) DESC
        LIMIT :limit
        """
    )
    suspend fun findAutofillCandidates(
        packageName: String?, webDomain: String?, limit: Int
    ): List<VaultEntryEntity>

    @Query("SELECT * FROM ${DatabaseConfig.TABLE_ENTRIES} WHERE title LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%'")
    fun searchEntries(query: String): Flow<List<VaultEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInternal(entry: VaultEntryEntity): Long

    suspend fun insert(entry: VaultEntryEntity): Long {
        val now = System.currentTimeMillis()
        return insertInternal(entry.copy(createdAt = entry.createdAt ?: now, updatedAt = now))
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<VaultEntryEntity>)

    @Update
    suspend fun update(entry: VaultEntryEntity)

    @Delete
    suspend fun delete(entry: VaultEntryEntity)

    @Query("DELETE FROM ${DatabaseConfig.TABLE_ENTRIES}")
    suspend fun deleteAll()
}
