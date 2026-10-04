package com.aozijx.passly.app.database.backup

import com.aozijx.passly.feature.backup.internal.archive.model.BackupEntryRecord
import com.aozijx.passly.domain.entry.model.Entry
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 备份文档映射器。
 * 只负责 Entry ↔ BackupEntryRecord 的转换，不涉及加密或文件 IO。
 */
@Singleton
internal class RoomBackupSnapshotMapper @Inject constructor() {

    fun toRecord(
        entry: Entry,
        attachmentIds: List<String> = emptyList()
    ): BackupEntryRecord = KeyedEntryArchiveMapper.toRecord(entry, attachmentIds)

    fun toEntry(record: BackupEntryRecord): Entry = KeyedEntryArchiveMapper.toEntry(record)
}
