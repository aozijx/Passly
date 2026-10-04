package com.aozijx.passly.presentation.feature.backup

import com.aozijx.passly.app.message.model.NoticeCode
import com.aozijx.passly.app.message.model.ArgumentKey
import com.aozijx.passly.app.message.model.ArgumentValue
import com.aozijx.passly.feature.backup.internal.operation.BackupOperation

internal fun BackupNoticeEffect.toNoticeCode(): NoticeCode = when (this) {
    is BackupNoticeEffect.Succeeded -> when (operation) {
        BackupOperation.EXPORT -> NoticeCode.BACKUP_EXPORT_COMPLETED
        BackupOperation.IMPORT -> NoticeCode.BACKUP_IMPORT_COMPLETED
        BackupOperation.DIRECTORY_CHECK -> NoticeCode.BACKUP_DIRECTORY_CHECK_COMPLETED
    }

    is BackupNoticeEffect.Failed -> when (operation) {
        BackupOperation.EXPORT -> NoticeCode.BACKUP_EXPORT_FAILED
        BackupOperation.IMPORT -> NoticeCode.BACKUP_IMPORT_FAILED
        BackupOperation.DIRECTORY_CHECK -> NoticeCode.BACKUP_DIRECTORY_CHECK_FAILED
    }
}

internal fun BackupNoticeEffect.toNoticeArguments(): Map<ArgumentKey, ArgumentValue> {
    val result = (this as? BackupNoticeEffect.Succeeded)?.importResult ?: return emptyMap()
    return mapOf(
        ArgumentKey.IMPORTED_ITEM_COUNT to ArgumentValue.Count(result.importedEntryCount.toLong()),
        ArgumentKey.EXISTING_ITEM_COUNT to ArgumentValue.Count(result.existingEntryCount.toLong()),
        ArgumentKey.SKIPPED_ITEM_COUNT to ArgumentValue.Count(result.skippedEntryCount.toLong()),
        ArgumentKey.IGNORED_FIELD_COUNT to ArgumentValue.Count(result.ignoredFieldCount.toLong()),
        ArgumentKey.PRUNED_LINK_COUNT to ArgumentValue.Count(result.prunedLinkCount.toLong()),
        ArgumentKey.PRUNED_RESOURCE_COUNT to ArgumentValue.Count(result.prunedResourceCount.toLong()),
    )
}
