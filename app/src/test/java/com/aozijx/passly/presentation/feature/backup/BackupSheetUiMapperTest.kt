package com.aozijx.passly.presentation.feature.backup

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.sensitive.OwnedChars
import com.aozijx.passly.feature.backup.internal.model.BackupExportFormat
import com.aozijx.passly.feature.backup.internal.model.ImportMode
import com.aozijx.passly.presentation.ui.settings.backup.model.BackupExportFormatUiModel
import com.aozijx.passly.presentation.ui.settings.backup.model.BackupImportModeUiModel
import com.aozijx.passly.presentation.ui.settings.backup.model.BackupSheet
import com.aozijx.passly.presentation.ui.shared.entry.EntryTypeUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class BackupSheetUiMapperTest {

    @Test
    fun `maps feature backup options to ui-only snapshot`() {
        val password = OwnedChars.fromString("temporary")
        try {
            val result = BackupUiState(
                backupPassword = password,
                importMode = ImportMode.OVERWRITE,
                selectedExportFormat = BackupExportFormat.ENCRYPTED,
                includeIcons = false,
                includeAttachments = true,
                includeDeleted = false,
                includedEntryTypes = setOf(EntryType.LOGIN, EntryType.NOTE),
            ).toSheetUiState(
                activeSheet = BackupSheet.EXPORT_OPTIONS,
                configuredDirectoryLabel = "Documents/Passly",
            )

            assertEquals(BackupSheet.EXPORT_OPTIONS, result.activeSheet)
            assertEquals("Documents/Passly", result.configuredDirectoryLabel)
            assertEquals("temporary", result.password)
            assertEquals(BackupImportModeUiModel.OVERWRITE, result.importMode)
            assertEquals(BackupExportFormatUiModel.ENCRYPTED, result.selectedExportFormat)
            assertEquals(
                setOf(EntryTypeUiModel.LOGIN, EntryTypeUiModel.NOTE),
                result.includedEntryTypes,
            )
            assertFalse(result.includeIcons)
            assertEquals(true, result.includeAttachments)
            assertEquals(false, result.includeDeleted)
            assertEquals(true, result.canSubmitExport)
        } finally {
            password.close()
        }
    }

    @Test
    fun `ui choices map back to feature models`() {
        assertEquals(BackupExportFormat.JSON, BackupExportFormatUiModel.JSON.toFeatureModel())
        assertEquals(ImportMode.APPEND, BackupImportModeUiModel.APPEND.toFeatureModel())
        assertEquals(
            setOf(EntryType.WIFI, EntryType.SSH_KEY),
            setOf(EntryTypeUiModel.WIFI, EntryTypeUiModel.SSH_KEY).toFeatureModels(),
        )
    }
}
