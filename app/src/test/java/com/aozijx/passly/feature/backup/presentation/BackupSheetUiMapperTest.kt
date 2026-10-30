package com.aozijx.passly.feature.backup.presentation

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.sensitive.OwnedChars
import com.aozijx.passly.feature.backup.internal.model.BackupExportFormat
import com.aozijx.passly.feature.backup.presentation.ui.model.BackupRestoreSheetUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class BackupSheetUiMapperTest {
    @Test
    fun `maps export configuration to its dedicated sheet state`() {
        val password = OwnedChars.fromString("temporary")
        try {
            val result = BackupUiState(
                workflow = BackupWorkflow.ConfigureExport(
                    BackupExportOptions(
                        format = BackupExportFormat.ENCRYPTED,
                        fileName = "backup.passly",
                        password = password,
                        includeIcons = false,
                        includeAttachments = true,
                        includeDeleted = false,
                        includedEntryTypes = setOf(EntryType.LOGIN, EntryType.NOTE),
                    ),
                ),
            ).toSheetUiState(configuredDirectoryLabel = "Documents/Passly")
                as BackupRestoreSheetUiState.ExportOptions

            assertEquals("Documents/Passly", result.configuredDirectoryLabel)
            assertEquals("temporary", result.password)
            assertEquals(BackupExportFormat.ENCRYPTED, result.format)
            assertEquals(setOf(EntryType.LOGIN, EntryType.NOTE), result.includedEntryTypes)
            assertFalse(result.includeIcons)
            assertEquals(true, result.includeAttachments)
            assertEquals(false, result.includeDeleted)
            assertEquals(true, result.canSubmit)
        } finally {
            password.close()
        }
    }
}
