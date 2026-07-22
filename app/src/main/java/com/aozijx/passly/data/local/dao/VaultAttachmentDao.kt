package com.aozijx.passly.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aozijx.passly.data.local.database.DatabaseSchema
import com.aozijx.passly.data.model.entity.VaultAttachmentEntity
import kotlinx.coroutines.flow.Flow

/**
 * 附件 DAO。
 *
 * 注意：「文件删除必须在 DB 记录成功删除之后执行」。
 * 调用方应在 DB 删除成功后，再执行文件系统清理，避免以下问题：
 * - 文件已删但 DB 仍持有引用（孤儿记录）
 * - DB 已删但文件残留（磁盘浪费）
 */
@Dao
interface VaultAttachmentDao {

    // ---- observe (Flow) ----

    /** 观察条目的全部附件。 */
    @Query("SELECT * FROM ${DatabaseSchema.TABLE_ATTACHMENT} WHERE entryId = :entryId ORDER BY createdAt DESC")
    fun observeByEntryId(entryId: String): Flow<List<VaultAttachmentEntity>>

    /** 观察孤儿附件（父条目已不存在）。 */
    @Query(
        """
        SELECT * FROM ${DatabaseSchema.TABLE_ATTACHMENT}
        WHERE entryId NOT IN (SELECT entryId FROM ${DatabaseSchema.TABLE_METADATA})
        ORDER BY createdAt DESC
    """
    )
    fun observeOrphanAttachments(): Flow<List<VaultAttachmentEntity>>

    // ---- paging (Paging 3) ----

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_ATTACHMENT} WHERE entryId = :entryId ORDER BY createdAt DESC")
    fun pagingByEntryId(entryId: String): PagingSource<Int, VaultAttachmentEntity>

    /** 分页查询孤儿附件。 */
    @Query(
        """
        SELECT * FROM ${DatabaseSchema.TABLE_ATTACHMENT}
        WHERE entryId NOT IN (SELECT entryId FROM ${DatabaseSchema.TABLE_METADATA})
        ORDER BY createdAt DESC
    """
    )
    fun pagingOrphanAttachments(): PagingSource<Int, VaultAttachmentEntity>

    // ---- get (suspend) ----

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_ATTACHMENT} WHERE attachmentId = :attachmentId LIMIT 1")
    suspend fun getById(attachmentId: String): VaultAttachmentEntity?

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_ATTACHMENT} WHERE entryId = :entryId ORDER BY createdAt DESC")
    suspend fun getByEntryId(entryId: String): List<VaultAttachmentEntity>

    /** 批量查询。 */
    @Query("SELECT * FROM ${DatabaseSchema.TABLE_ATTACHMENT} WHERE attachmentId IN (:attachmentIds)")
    suspend fun getByIds(attachmentIds: List<String>): List<VaultAttachmentEntity>

    /** 查询孤儿附件。 */
    @Query(
        """
        SELECT * FROM ${DatabaseSchema.TABLE_ATTACHMENT}
        WHERE entryId NOT IN (SELECT entryId FROM ${DatabaseSchema.TABLE_METADATA})
        ORDER BY createdAt DESC
    """
    )
    suspend fun getOrphanAttachments(): List<VaultAttachmentEntity>

    // ---- exists ----

    @Query("SELECT EXISTS(SELECT 1 FROM ${DatabaseSchema.TABLE_ATTACHMENT} WHERE attachmentId = :attachmentId)")
    suspend fun exists(attachmentId: String): Boolean

    /** 条目是否存在附件。 */
    @Query("SELECT EXISTS(SELECT 1 FROM ${DatabaseSchema.TABLE_ATTACHMENT} WHERE entryId = :entryId)")
    suspend fun existsByEntryId(entryId: String): Boolean

    /** 是否存在孤儿附件。 */
    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM ${DatabaseSchema.TABLE_ATTACHMENT}
            WHERE entryId NOT IN (SELECT entryId FROM ${DatabaseSchema.TABLE_METADATA})
        )
    """
    )
    suspend fun existsOrphan(): Boolean

    // ---- count ----

    @Query("SELECT COUNT(*) FROM ${DatabaseSchema.TABLE_ATTACHMENT} WHERE entryId = :entryId")
    suspend fun countByEntryId(entryId: String): Int

    /** 统计孤儿附件数。 */
    @Query(
        """
        SELECT COUNT(*) FROM ${DatabaseSchema.TABLE_ATTACHMENT}
        WHERE entryId NOT IN (SELECT entryId FROM ${DatabaseSchema.TABLE_METADATA})
    """
    )
    suspend fun countOrphan(): Int

    // ---- insert ----

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attachment: VaultAttachmentEntity)

    /** 批量插入。 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(attachments: List<VaultAttachmentEntity>)

    // ---- delete ----

    @Query("DELETE FROM ${DatabaseSchema.TABLE_ATTACHMENT} WHERE attachmentId = :attachmentId")
    suspend fun deleteById(attachmentId: String)

    @Query("DELETE FROM ${DatabaseSchema.TABLE_ATTACHMENT} WHERE entryId = :entryId")
    suspend fun deleteByEntryId(entryId: String)

    /** 批量删除。 */
    @Query("DELETE FROM ${DatabaseSchema.TABLE_ATTACHMENT} WHERE attachmentId IN (:attachmentIds)")
    suspend fun deleteByIds(attachmentIds: List<String>)

    /** 清理孤儿附件。 */
    @Query(
        """
        DELETE FROM ${DatabaseSchema.TABLE_ATTACHMENT}
        WHERE entryId NOT IN (SELECT entryId FROM ${DatabaseSchema.TABLE_METADATA})
    """
    )
    suspend fun deleteOrphanAttachments(): Int

    /** 清空全表，返回删除行数。 */
    @Query("DELETE FROM ${DatabaseSchema.TABLE_ATTACHMENT}")
    suspend fun clear(): Int
}
