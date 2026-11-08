package com.aozijx.passly.feature.backup.presentation

import com.aozijx.passly.app.message.model.AppNotice
import com.aozijx.passly.app.message.model.ArgumentKey
import com.aozijx.passly.app.message.model.ArgumentValue
import com.aozijx.passly.app.message.model.NoticeCode
import com.aozijx.passly.app.message.model.newAppNotice
import com.aozijx.passly.feature.backup.contract.BackupImportResult
import com.aozijx.passly.feature.backup.contract.BackupOperation

internal fun BackupNoticeEffect.toAppNotice(): AppNotice =
    when (this) {
        BackupNoticeEffect.DirectoryWritable -> newAppNotice(NoticeCode.BACKUP_DIRECTORY_CHECK_COMPLETED)
        BackupNoticeEffect.ExportSucceeded -> newAppNotice(NoticeCode.BACKUP_EXPORT_COMPLETED)
        is BackupNoticeEffect.ImportSucceeded ->
            newAppNotice(
                code = NoticeCode.BACKUP_IMPORT_COMPLETED,
                arguments = result.toNoticeArguments(),
            )
        is BackupNoticeEffect.Failed ->
            newAppNotice(
                when (operation) {
                    BackupOperation.EXPORT -> NoticeCode.BACKUP_EXPORT_FAILED
                    BackupOperation.IMPORT -> NoticeCode.BACKUP_IMPORT_FAILED
                    BackupOperation.DIRECTORY_CHECK -> NoticeCode.BACKUP_DIRECTORY_CHECK_FAILED
                },
            )
        BackupNoticeEffect.ExportCleanupFailed -> newAppNotice(NoticeCode.BACKUP_EXPORT_CLEANUP_FAILED)
    }

private fun BackupImportResult.toNoticeArguments(): Map<ArgumentKey, ArgumentValue> =
    mapOf(
        ArgumentKey.IMPORTED_ITEM_COUNT to ArgumentValue.Count(importedEntryCount.toLong()),
        ArgumentKey.EXISTING_ITEM_COUNT to ArgumentValue.Count(existingEntryCount.toLong()),
        ArgumentKey.SKIPPED_ITEM_COUNT to ArgumentValue.Count(skippedEntryCount.toLong()),
        ArgumentKey.IGNORED_FIELD_COUNT to ArgumentValue.Count(ignoredFieldCount.toLong()),
        ArgumentKey.PRUNED_LINK_COUNT to ArgumentValue.Count(prunedLinkCount.toLong()),
        ArgumentKey.PRUNED_RESOURCE_COUNT to ArgumentValue.Count(prunedResourceCount.toLong()),
    )
