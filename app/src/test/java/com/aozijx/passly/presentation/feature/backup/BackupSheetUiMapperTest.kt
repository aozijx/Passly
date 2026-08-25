package com.aozijx.passly.presentation.feature.backup

import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.sensitive.OwnedChars
import com.aozijx.passly.feature.backup.internal.model.BackupExportUiFormat
import com.aozijx.passly.feature.backup.internal.model.ImportMode
import com.aozijx.passly.presentation.ui.settings.backup.BackupEntryTypeUiModel
import com.aozijx.passly.presentation.ui.settings.backup.BackupExportFormatUiModel
import com.aozijx.passly.presentation.ui.settings.backup.BackupImportModeUiModel
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
                selectedExportFormat = BackupExportUiFormat.ENCRYPTED,
                includeIcons = false,
                includeAttachments = true,
                includeDeleted = false,
                includedEntryTypes = setOf(EntryType.LOGIN, EntryType.NOTE),
            ).toSheetUiState()

            assertEquals("temporary", result.password)
            assertEquals(BackupImportModeUiModel.OVERWRITE, result.importMode)
            assertEquals(BackupExportFormatUiModel.ENCRYPTED, result.selectedExportFormat)
            assertEquals(
                setOf(BackupEntryTypeUiModel.LOGIN, BackupEntryTypeUiModel.NOTE),
                result.includedEntryTypes,
            )
            assertFalse(result.includeIcons)
        } finally {
            password.close()
        }
    }

    @Test
    fun `ui choices map back to feature models`() {
        assertEquals(BackupExportUiFormat.JSON, BackupExportFormatUiModel.JSON.toFeatureModel())
        assertEquals(ImportMode.APPEND, BackupImportModeUiModel.APPEND.toFeatureModel())
        assertEquals(
            setOf(EntryType.WIFI, EntryType.SSH_KEY),
            setOf(BackupEntryTypeUiModel.WIFI, BackupEntryTypeUiModel.SSH_KEY).toFeatureModels(),
        )
    }
}
