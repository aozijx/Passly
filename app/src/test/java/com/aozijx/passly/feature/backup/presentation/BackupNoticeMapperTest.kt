package com.aozijx.passly.feature.backup.presentation

import com.aozijx.passly.app.message.model.NoticeCode
import com.aozijx.passly.feature.backup.internal.operation.BackupOperation
import com.aozijx.passly.feature.backup.internal.model.BackupImportResult
import com.aozijx.passly.app.message.model.ArgumentKey
import com.aozijx.passly.app.message.model.ArgumentValue
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupNoticeMapperTest {
    @Test
    fun operationEffectsMapToGlobalNoticeCodesAtPresentationBoundary() {
        val expected = mapOf(
            BackupNoticeEffect.ExportSucceeded to NoticeCode.BACKUP_EXPORT_COMPLETED,
            BackupNoticeEffect.ImportSucceeded(BackupImportResult(0, 0, 0, 0, 0, 0)) to
                NoticeCode.BACKUP_IMPORT_COMPLETED,
            BackupNoticeEffect.DirectoryWritable to
                NoticeCode.BACKUP_DIRECTORY_CHECK_COMPLETED,
            BackupNoticeEffect.Failed(BackupOperation.EXPORT) to NoticeCode.BACKUP_EXPORT_FAILED,
            BackupNoticeEffect.Failed(BackupOperation.IMPORT) to NoticeCode.BACKUP_IMPORT_FAILED,
            BackupNoticeEffect.Failed(BackupOperation.DIRECTORY_CHECK) to
                NoticeCode.BACKUP_DIRECTORY_CHECK_FAILED,
        )

        expected.forEach { (effect, noticeCode) ->
            assertEquals(noticeCode, effect.toNoticeCode())
        }
    }

    @Test
    fun successfulImportMapsAllSummaryCountsToSafeNoticeArguments() {
        val effect = BackupNoticeEffect.ImportSucceeded(BackupImportResult(4, 1, 2, 3, 5, 6))

        assertEquals(
            ArgumentValue.Count(4),
            effect.toNoticeArguments()[ArgumentKey.IMPORTED_ITEM_COUNT],
        )
        assertEquals(
            ArgumentValue.Count(6),
            effect.toNoticeArguments()[ArgumentKey.PRUNED_RESOURCE_COUNT],
        )
    }
}
