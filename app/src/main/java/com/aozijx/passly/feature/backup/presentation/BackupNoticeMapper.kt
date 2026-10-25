package com.aozijx.passly.feature.backup.presentation

import com.aozijx.passly.app.message.model.NoticeCode
import com.aozijx.passly.app.message.model.ArgumentKey
import com.aozijx.passly.app.message.model.ArgumentValue
import com.aozijx.passly.feature.backup.internal.operation.BackupOperation

internal fun BackupNoticeEffect.toNoticeCode(): NoticeCode = when (this) {
    BackupNoticeEffect.DirectoryWritable -> NoticeCode.BACKUP_DIRECTORY_CHECK_COMPLETED
    BackupNoticeEffect.ExportSucceeded -> NoticeCode.BACKUP_EXPORT_COMPLETED
    is BackupNoticeEffect.ImportSucceeded -> NoticeCode.BACKUP_IMPORT_COMPLETED
    is BackupNoticeEffect.Failed -> when (operation) {
        BackupOperation.EXPORT -> NoticeCode.BACKUP_EXPORT_FAILED
        BackupOperation.IMPORT -> NoticeCode.BACKUP_IMPORT_FAILED
        BackupOperation.DIRECTORY_CHECK -> NoticeCode.BACKUP_DIRECTORY_CHECK_FAILED
    }
}

internal fun BackupNoticeEffect.toNoticeArguments(): Map<ArgumentKey, ArgumentValue> {
    val result = (this as? BackupNoticeEffect.ImportSucceeded)?.result ?: return emptyMap()
    return mapOf(
        ArgumentKey.IMPORTED_ITEM_COUNT to ArgumentValue.Count(result.importedEntryCount.toLong()),
        ArgumentKey.EXISTING_ITEM_COUNT to ArgumentValue.Count(result.existingEntryCount.toLong()),
        ArgumentKey.SKIPPED_ITEM_COUNT to ArgumentValue.Count(result.skippedEntryCount.toLong()),
        ArgumentKey.IGNORED_FIELD_COUNT to ArgumentValue.Count(result.ignoredFieldCount.toLong()),
        ArgumentKey.PRUNED_LINK_COUNT to ArgumentValue.Count(result.prunedLinkCount.toLong()),
        ArgumentKey.PRUNED_RESOURCE_COUNT to ArgumentValue.Count(result.prunedResourceCount.toLong()),
    )
}
