package com.aozijx.passly.feature.backup.presentation

import com.aozijx.passly.core.error.model.BackupFailed
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.feature.backup.internal.archive.BackupArchiveKeyRegistry
import com.aozijx.passly.feature.backup.internal.model.BackupExportFormat
import com.aozijx.passly.feature.backup.internal.operation.BackupOperation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Test

class BackupReducerTest {
    @Test
    fun `reducer owns the complete options workflow`() {
        val opened = BackupReducer.reduce(BackupUiState(), BackupMutation.ExportOptionsOpened)
        val prepared = BackupReducer.reduce(
            opened,
            BackupMutation.ExportPrepared(BackupExportFormat.JSON, "backup.json"),
        )
        val awaitingTarget = BackupReducer.reduce(prepared, BackupMutation.ExportTargetRequested)
        val dismissed = BackupReducer.reduce(awaitingTarget, BackupMutation.WorkflowClosed)

        assertSame(BackupWorkflow.FormatPicker, opened.workflow)
        assertEquals(
            BackupExportFormat.JSON,
            (prepared.workflow as BackupWorkflow.ConfigureExport).options.format,
        )
        assertEquals(
            "backup.json",
            (awaitingTarget.workflow as BackupWorkflow.AwaitingExportTarget).options.fileName,
        )
        assertSame(BackupWorkflow.Idle, dismissed.workflow)
    }

    @Test
    fun `preparing text export derives a clean format-specific configuration`() {
        val state = BackupReducer.reduce(
            BackupUiState(
                workflow = BackupWorkflow.Failed(
                    operation = BackupOperation.IMPORT,
                    error = BackupFailed(),
                ),
            ),
            BackupMutation.ExportPrepared(BackupExportFormat.TEXT, "backup.txt"),
        )

        val options = (state.workflow as BackupWorkflow.ConfigureExport).options
        assertEquals(BackupExportFormat.TEXT, options.format)
        assertEquals("backup.txt", options.fileName)
        assertFalse(options.includeIcons)
        assertFalse(options.includeAttachments)
        assertEquals(BackupArchiveKeyRegistry.archiveEntryTypes, options.includedEntryTypes)
    }

    @Test
    fun `failure is represented by one complete workflow state`() {
        val error = BackupFailed()
        val state = BackupReducer.reduce(
            BackupUiState(
                workflow = BackupWorkflow.ConfigureExport(
                    BackupExportOptions(BackupExportFormat.ENCRYPTED, "backup.passly"),
                ),
            ),
            BackupMutation.OperationFailed(BackupOperation.EXPORT, error),
        )

        val failed = state.workflow as BackupWorkflow.Failed
        assertSame(BackupOperation.EXPORT, failed.operation)
        assertSame(error, failed.error)
    }

    @Test
    fun `updates for another workflow are ignored`() {
        val initial = BackupUiState(workflow = BackupWorkflow.FormatPicker)
        val updated = BackupReducer.reduce(
            initial,
            BackupMutation.IncludedEntryTypesUpdated(setOf(EntryType.LOGIN)),
        )

        assertSame(initial, updated)
    }
}
