package com.aozijx.passly.presentation.feature.backup

import com.aozijx.passly.app.message.model.NoticeCode
import com.aozijx.passly.feature.backup.internal.operation.BackupOperation
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupNoticeMapperTest {
    @Test
    fun operationEffectsMapToGlobalNoticeCodesAtPresentationBoundary() {
        val expected = mapOf(
            BackupEffect.Succeeded(BackupOperation.EXPORT) to NoticeCode.BACKUP_EXPORT_COMPLETED,
            BackupEffect.Succeeded(BackupOperation.IMPORT) to NoticeCode.BACKUP_IMPORT_COMPLETED,
            BackupEffect.Succeeded(BackupOperation.DIRECTORY_CHECK) to
                NoticeCode.BACKUP_DIRECTORY_CHECK_COMPLETED,
            BackupEffect.Failed(BackupOperation.EXPORT) to NoticeCode.BACKUP_EXPORT_FAILED,
            BackupEffect.Failed(BackupOperation.IMPORT) to NoticeCode.BACKUP_IMPORT_FAILED,
            BackupEffect.Failed(BackupOperation.DIRECTORY_CHECK) to
                NoticeCode.BACKUP_DIRECTORY_CHECK_FAILED,
        )

        expected.forEach { (effect, noticeCode) ->
            assertEquals(noticeCode, effect.toNoticeCode())
        }
    }
}
