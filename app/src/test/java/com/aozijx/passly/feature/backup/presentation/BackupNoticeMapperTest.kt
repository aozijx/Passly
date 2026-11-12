package com.aozijx.passly.feature.backup.presentation

import com.aozijx.passly.app.message.model.ArgumentKey
import com.aozijx.passly.app.message.model.ArgumentValue
import com.aozijx.passly.app.message.model.NoticeCode
import com.aozijx.passly.core.observability.Reasons
import com.aozijx.passly.feature.backup.contract.BackupImportResult
import com.aozijx.passly.feature.backup.contract.BackupOperation
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupNoticeMapperTest {
    @Test
    fun operationEffectsMapToGlobalNoticeCodesAtPresentationBoundary() {
        val expected =
            mapOf(
                BackupNoticeEffect.ExportSucceeded to NoticeCode.BACKUP_EXPORT_COMPLETED,
                BackupNoticeEffect.ImportSucceeded(BackupImportResult(0, 0, 0, 0, 0, 0)) to
                    NoticeCode.BACKUP_IMPORT_COMPLETED,
                BackupNoticeEffect.DirectoryWritable to
                    NoticeCode.BACKUP_DIRECTORY_CHECK_COMPLETED,
                BackupNoticeEffect.Failed(BackupOperation.EXPORT) to NoticeCode.BACKUP_EXPORT_FAILED,
                BackupNoticeEffect.ExportCleanupFailed to NoticeCode.BACKUP_EXPORT_CLEANUP_FAILED,
                BackupNoticeEffect.Failed(BackupOperation.IMPORT) to NoticeCode.BACKUP_IMPORT_FAILED,
                BackupNoticeEffect.Failed(
                    BackupOperation.IMPORT,
                    Reasons.Backup.Authentication.BackupAuthenticationFailed,
                ) to NoticeCode.BACKUP_IMPORT_AUTHENTICATION_FAILED,
                BackupNoticeEffect.Failed(BackupOperation.DIRECTORY_CHECK) to
                    NoticeCode.BACKUP_DIRECTORY_CHECK_FAILED,
            )

        expected.forEach { (effect, noticeCode) ->
            assertEquals(noticeCode, effect.toAppNotice().code)
        }
    }

    @Test
    fun successfulImportMapsAllSummaryCountsToSafeNoticeArguments() {
        val effect = BackupNoticeEffect.ImportSucceeded(BackupImportResult(4, 1, 2, 3, 5, 6))

        assertEquals(
            mapOf(
                ArgumentKey.IMPORTED_ITEM_COUNT to ArgumentValue.Count(4),
                ArgumentKey.EXISTING_ITEM_COUNT to ArgumentValue.Count(1),
                ArgumentKey.SKIPPED_ITEM_COUNT to ArgumentValue.Count(2),
                ArgumentKey.IGNORED_FIELD_COUNT to ArgumentValue.Count(3),
                ArgumentKey.PRUNED_LINK_COUNT to ArgumentValue.Count(5),
                ArgumentKey.PRUNED_RESOURCE_COUNT to ArgumentValue.Count(6),
            ),
            effect.toAppNotice().arguments,
        )
    }
}
