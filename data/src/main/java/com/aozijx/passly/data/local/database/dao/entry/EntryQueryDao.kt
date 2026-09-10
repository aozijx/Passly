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

    @Query("SELECT * FROM entries WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun observeDeleted(): Flow<List<EntryEntity>>

    @RawQuery(
        observedEntities = [EntryEntity::class, EntryActivityEntity::class, EntryLinkEntity::class]
    )
    fun paging(query: SupportSQLiteQuery): PagingSource<Int, EntryPagingRow>

    @RawQuery(observedEntities = [EntryEntity::class])
    fun observeCategories(query: SupportSQLiteQuery): Flow<List<String>>

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

    @Query(
        """
        SELECT * FROM entries
        WHERE entryType = 'LOGIN'
          AND deletedAt IS NULL
          AND EXISTS (
              SELECT 1 FROM json_each(applicationIds)
              WHERE LOWER(TRIM(CAST(value AS TEXT))) = :applicationId
          )
        ORDER BY updatedAt DESC
        LIMIT :limit
        """
    )
    suspend fun getAutofillCandidatesByApplicationId(
        applicationId: String,
        limit: Int,
    ): List<EntryEntity>

    @Query(
        """
        SELECT * FROM entries
        WHERE entryType = 'LOGIN'
          AND deletedAt IS NULL
          AND (
              LOWER(COALESCE(primaryUrl, '')) LIKE '%' || :domain || '%' OR
              EXISTS (
                  SELECT 1 FROM json_each(domains)
                  WHERE LOWER(CAST(value AS TEXT)) LIKE '%' || :domain || '%'
              )
          )
        ORDER BY updatedAt DESC
        LIMIT :limit
        """
    )
    suspend fun getAutofillCandidatesByDomain(
        domain: String,
        limit: Int,
    ): List<EntryEntity>

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

    @Query("SELECT COUNT(*) FROM entries WHERE iconCustomReference = :path")
    suspend fun countByIconCustomReference(path: String): Int

}
