package com.aozijx.passly.feature.backup.internal.presentation

import com.aozijx.passly.core.error.model.BackupFailed
import com.aozijx.passly.domain.backup.model.BackupExportUiFormat
import com.aozijx.passly.domain.backup.model.BackupOperationStatus
import com.aozijx.passly.domain.backup.model.ImportMode
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.feature.backup.internal.contract.BackupUiState
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
            backupPassword = "stale password",
            importMode = ImportMode.OVERWRITE,
            includeIcons = true,
            includeAttachments = true,
            includedEntryTypes = emptySet(),
        )

        val state = BackupReducer.reduce(
            initial,
            BackupMutation.ExportPrepared(
                format = BackupExportUiFormat.TEXT,
                isRecoveryExport = false,
                fileName = "backup.txt",
            ),
        )

        assertTrue(state.isExporting)
        assertFalse(state.isRecoveryExport)
        assertEquals(BackupExportUiFormat.TEXT, state.selectedExportFormat)
        assertEquals("backup.txt", state.pendingExportFileName)
        assertEquals("", state.backupPassword)
        assertFalse(state.includeIcons)
        assertFalse(state.includeAttachments)
        assertEquals(EntryType.entries.toSet(), state.includedEntryTypes)
        assertSame(BackupOperationStatus.Idle, state.status)
        assertNull(state.error)
    }

    @Test
    fun recoveryExportPrepared_keepsResourceOptionsEnabled() {
        val state = BackupReducer.reduce(
            BackupUiState(),
            BackupMutation.ExportPrepared(
                format = BackupExportUiFormat.ENCRYPTED,
                isRecoveryExport = true,
                fileName = "recovery.passly",
            ),
        )

        assertTrue(state.isRecoveryExport)
        assertTrue(state.includeIcons)
        assertTrue(state.includeAttachments)
        assertFalse(state.canSubmitExport)
    }

    @Test
    fun clearingPendingFields_afterFailure_preservesFailureForRendering() {
        val error = BackupFailed()
        val failed = BackupReducer.reduce(
            BackupUiState(
                isExporting = true,
                isRecoveryExport = true,
                backupPassword = "temporary",
                pendingExportFileName = "recovery.passly",
                deleteTargetOnFailure = true,
            ),
            BackupMutation.OperationFailed(error),
        )

        val cleared = BackupReducer.reduce(failed, BackupMutation.PendingFieldsCleared)

        assertSame(BackupOperationStatus.Failure, cleared.status)
        assertSame(error, cleared.error)
        assertEquals("", cleared.backupPassword)
        assertNull(cleared.pendingExportFileName)
        assertFalse(cleared.deleteTargetOnFailure)
        assertFalse(cleared.isRecoveryExport)
    }

    @Test
    fun cancellingPendingOperation_returnsToIdleAndClearsFailure() {
        val state = BackupReducer.reduce(
            BackupUiState(
                status = BackupOperationStatus.Failure,
                error = BackupFailed(),
                isRecoveryExport = true,
                backupPassword = "temporary",
                pendingExportFileName = "backup.passly",
            ),
            BackupMutation.PendingOperationCleared,
        )

        assertSame(BackupOperationStatus.Idle, state.status)
        assertNull(state.error)
        assertEquals("", state.backupPassword)
        assertNull(state.pendingExportFileName)
        assertFalse(state.isRecoveryExport)
    }
}
