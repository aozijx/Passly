package com.aozijx.passly.data.local.dao.attachment

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.aozijx.passly.data.model.entity.EntryAttachmentEntity

@Dao
interface EntryAttachmentCommandDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStrict(attachment: EntryAttachmentEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAllStrict(attachments: List<EntryAttachmentEntity>)

    @Upsert
    suspend fun upsertForImport(attachment: EntryAttachmentEntity)

    @Upsert
    suspend fun upsertAllForImport(attachments: List<EntryAttachmentEntity>)

    @Query("DELETE FROM entry_attachments WHERE attachmentId = :attachmentId")
    suspend fun deleteById(attachmentId: String)

    @Query("DELETE FROM entry_attachments WHERE entryId = :entryId")
    suspend fun deleteByEntryId(entryId: String)

    @Query("DELETE FROM entry_attachments WHERE attachmentId IN (:attachmentIds)")
    suspend fun deleteByIds(attachmentIds: List<String>)
}
