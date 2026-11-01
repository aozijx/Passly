package com.aozijx.passly.data.local.database.dao.attachment

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.aozijx.passly.data.local.database.entity.AttachmentRefEntity

@Dao
interface AttachmentRefCommandDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStrict(attachment: AttachmentRefEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAllStrict(attachments: List<AttachmentRefEntity>)

    @Query("DELETE FROM attachment_refs WHERE attachmentId = :attachmentId")
    suspend fun deleteById(attachmentId: String): Int

    @Query("DELETE FROM attachment_refs WHERE entryId = :entryId")
    suspend fun deleteByEntryId(entryId: String): Int
}
