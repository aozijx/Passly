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
 * 注意：文件删除必须在 DB 记录成功删除之后执行。
 * 调用方应在 DB 删除成功后，再执行文件系统清理。
 *
 * PENDING 附件使用 .staging/<operationId> 临时文件，不写入 Room。
 * Room 仅保存 COMMITTED 附件，启动时清理超时 staging 文件即可。
 */
@Dao
interface VaultAttachmentDao {

    // ---- observe (Flow) ----

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_ATTACHMENT} WHERE entryId = :entryId ORDER BY createdAt DESC")
    fun observeByEntryId(entryId: String): Flow<List<VaultAttachmentEntity>>

    // ---- paging (Paging 3) ----

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_ATTACHMENT} WHERE entryId = :entryId ORDER BY createdAt DESC")
    fun pagingByEntryId(entryId: String): PagingSource<Int, VaultAttachmentEntity>

    // ---- get (suspend) ----

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_ATTACHMENT} WHERE attachmentId = :attachmentId LIMIT 1")
    suspend fun getById(attachmentId: String): VaultAttachmentEntity?

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_ATTACHMENT} WHERE entryId = :entryId ORDER BY createdAt DESC")
    suspend fun getByEntryId(entryId: String): List<VaultAttachmentEntity>

    @Query("SELECT * FROM ${DatabaseSchema.TABLE_ATTACHMENT} WHERE attachmentId IN (:attachmentIds)")
    suspend fun getByIds(attachmentIds: List<String>): List<VaultAttachmentEntity>

    // ---- exists ----

    @Query("SELECT EXISTS(SELECT 1 FROM ${DatabaseSchema.TABLE_ATTACHMENT} WHERE attachmentId = :attachmentId)")
    suspend fun exists(attachmentId: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM ${DatabaseSchema.TABLE_ATTACHMENT} WHERE entryId = :entryId)")
    suspend fun existsByEntryId(entryId: String): Boolean

    // ---- count ----

    @Query("SELECT COUNT(*) FROM ${DatabaseSchema.TABLE_ATTACHMENT} WHERE entryId = :entryId")
    suspend fun countByEntryId(entryId: String): Int

    // === Strict Insert (fail on duplicate PK) ===

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStrict(attachment: VaultAttachmentEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAllStrict(attachments: List<VaultAttachmentEntity>)

    // === Import Upsert (overwrite on duplicate) ===

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertForImport(attachment: VaultAttachmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAllForImport(attachments: List<VaultAttachmentEntity>)

    // === Maintenance API ===

    @Query("DELETE FROM ${DatabaseSchema.TABLE_ATTACHMENT} WHERE attachmentId = :attachmentId")
    suspend fun deleteById(attachmentId: String)

    @Query("DELETE FROM ${DatabaseSchema.TABLE_ATTACHMENT} WHERE entryId = :entryId")
    suspend fun deleteByEntryId(entryId: String)

    @Query("DELETE FROM ${DatabaseSchema.TABLE_ATTACHMENT} WHERE attachmentId IN (:attachmentIds)")
    suspend fun deleteByIds(attachmentIds: List<String>)

    @Query("DELETE FROM ${DatabaseSchema.TABLE_ATTACHMENT}")
    suspend fun clear(): Int
}
