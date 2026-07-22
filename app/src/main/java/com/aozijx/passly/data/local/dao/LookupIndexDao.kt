package com.aozijx.passly.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.aozijx.passly.data.local.database.DatabaseSchema
import com.aozijx.passly.data.model.entity.LookupIndexEntity
import com.aozijx.passly.domain.model.lookup.LookupField

@Dao
interface LookupIndexDao {

    // ---- search (single token) ----

    @Query("SELECT entryId FROM ${DatabaseSchema.TABLE_LOOKUP_INDEX} WHERE keywordHash = :hash AND gramLength = :length AND field IN (:fields) ORDER BY weight DESC")
    suspend fun searchByHash(hash: ByteArray, length: Int, fields: List<LookupField>): List<String>

    /**
     * 多 Token 交集搜索。
     * 返回同时匹配所有令牌的 entryId 列表。
     * 使用 INTERSECT 逐令牌过滤，在 SQL 层面完成交集，避免在内存中
     * 加载大量候选结果后再取交集。
     *
     * @param query 由 [buildIntersectionQuery] 构建的 SQLite 查询
     */
    @RawQuery(observedEntities = [LookupIndexEntity::class])
    suspend fun searchByTokenIntersection(query: SupportSQLiteQuery): List<String>

    // ---- get (suspend) ----

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_LOOKUP_INDEX} WHERE entryId = :entryId")
    suspend fun getByEntryId(entryId: String): List<LookupIndexEntity>

    // ---- exists ----

    @Query("SELECT EXISTS(SELECT 1 FROM ${DatabaseSchema.TABLE_LOOKUP_INDEX} WHERE entryId = :entryId)")
    suspend fun exists(entryId: String): Boolean

    // ---- count / status ----

    @Query("SELECT COUNT(*) FROM ${DatabaseSchema.TABLE_LOOKUP_INDEX}")
    suspend fun count(): Int

    /**
     * 返回已建索引的去重条目数。
     * 用于判断索引是否完整（与活跃条目数对比）。
     */
    @Query("SELECT COUNT(DISTINCT entryId) FROM ${DatabaseSchema.TABLE_LOOKUP_INDEX}")
    suspend fun countDistinctEntryIds(): Int

    // ---- insert ----

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LookupIndexEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<LookupIndexEntity>)

    // ---- delete ----

    @Query("DELETE FROM ${DatabaseSchema.TABLE_LOOKUP_INDEX} WHERE entryId = :entryId")
    suspend fun deleteByEntryId(entryId: String)

    @Query("DELETE FROM ${DatabaseSchema.TABLE_LOOKUP_INDEX} WHERE entryId IN (:entryIds)")
    suspend fun deleteByEntryIds(entryIds: List<String>)

    @Query("DELETE FROM ${DatabaseSchema.TABLE_LOOKUP_INDEX} WHERE entryId = :entryId AND field = :field")
    suspend fun deleteByEntryAndField(entryId: String, field: LookupField)

    @Query("DELETE FROM ${DatabaseSchema.TABLE_LOOKUP_INDEX}")
    suspend fun clear(): Int
}
