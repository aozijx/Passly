package com.aozijx.passly.feature.backup.internal.presentation

import com.aozijx.passly.core.error.model.BackupFailed
import com.aozijx.passly.feature.backup.internal.model.BackupExportUiFormat
import com.aozijx.passly.feature.backup.internal.model.BackupOperationStatus
import com.aozijx.passly.feature.backup.internal.model.ImportMode
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.feature.backup.internal.contract.BackupUiState
import com.aozijx.passly.security.crypto.SecureString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupReducerTest {
    @Test
    fun exportPrepared_resetsTransientInputAndDerivesResourceOptions() {
        val initial = BackupUiState(
            status = BackupOperationStatus.Failure,
            error = BackupFailed(),
            backupPassword = SecureString.fromString("stale password"),
            importMode = ImportMode.OVERWRITE,
            includeIcons = true,
            includeAttachments = true,
            includedEntryTypes = emptySet(),
        )

        val state = BackupReducer.reduce(
            initial,
            BackupMutation.ExportPrepared(
                format = BackupExportUiFormat.TEXT,
                fileName = "backup.txt",
            ),
        )

        assertTrue(state.isExporting)
        assertEquals(BackupExportUiFormat.TEXT, state.selectedExportFormat)
        assertEquals("backup.txt", state.pendingExportFileName)
        assertTrue(state.backupPassword.isEmpty)
        assertFalse(state.includeIcons)
        assertFalse(state.includeAttachments)
        assertEquals(EntryType.entries.toSet(), state.includedEntryTypes)
        assertSame(BackupOperationStatus.Idle, state.status)
        assertNull(state.error)
    }

    @Test
    fun clearingPendingFields_afterFailure_preservesFailureForRendering() {
        val error = BackupFailed()
        val failed = BackupReducer.reduce(
            BackupUiState(
                isExporting = true,
                backupPassword = SecureString.fromString("temporary"),
                pendingExportFileName = "recovery.passly",
                deleteTargetOnFailure = true,
            ),
            BackupMutation.OperationFailed(error),
        )

        val cleared = BackupReducer.reduce(failed, BackupMutation.PendingFieldsCleared)

        assertSame(BackupOperationStatus.Failure, cleared.status)
        assertSame(error, cleared.error)
        assertTrue(cleared.backupPassword.isEmpty)
        assertNull(cleared.pendingExportFileName)
        assertFalse(cleared.deleteTargetOnFailure)
    }

    @Test
    fun cancellingPendingOperation_returnsToIdleAndClearsFailure() {
        val state = BackupReducer.reduce(
            BackupUiState(
                status = BackupOperationStatus.Failure,
                error = BackupFailed(),
                backupPassword = SecureString.fromString("temporary"),
                pendingExportFileName = "backup.passly",
            ),
            BackupMutation.PendingOperationCleared,
        )

        assertSame(BackupOperationStatus.Idle, state.status)
        assertNull(state.error)
        assertTrue(state.backupPassword.isEmpty)
        assertNull(state.pendingExportFileName)
    }
}
