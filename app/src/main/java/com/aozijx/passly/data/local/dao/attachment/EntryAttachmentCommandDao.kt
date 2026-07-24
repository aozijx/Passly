package com.aozijx.passly.data.local.dao.attachment

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aozijx.passly.data.model.entity.EntryAttachmentEntity

@Dao
interface EntryAttachmentCommandDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStrict(attachment: EntryAttachmentEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAllStrict(attachments: List<EntryAttachmentEntity>)

    @Query("DELETE FROM entry_attachments WHERE attachmentId = :attachmentId")
    suspend fun deleteById(attachmentId: String): Int

    @Query("DELETE FROM entry_attachments WHERE entryId = :entryId")
    suspend fun deleteByEntryId(entryId: String): Int

    @Query("DELETE FROM entry_attachments WHERE attachmentId IN (:attachmentIds)")
    suspend fun deleteByIds(attachmentIds: List<String>): Int

    @Query("UPDATE entry_attachments SET status = :status, owner = :owner WHERE attachmentId = :attachmentId")
    suspend fun updateStatus(attachmentId: String, status: String, owner: String): Int

    @Query("DELETE FROM entry_attachments WHERE owner = :owner AND status = 'PENDING'")
    suspend fun deleteByOwner(owner: String): Int

    @Query("DELETE FROM entry_attachments WHERE status = 'PENDING'")
    suspend fun deleteAllPending(): Int
}
