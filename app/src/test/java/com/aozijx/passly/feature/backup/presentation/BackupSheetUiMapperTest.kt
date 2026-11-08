package com.aozijx.passly.feature.backup.presentation

import com.aozijx.passly.R
import com.aozijx.passly.domain.entry.model.EntryType
import com.aozijx.passly.domain.sensitive.OwnedChars
import com.aozijx.passly.feature.backup.contract.BackupExportFormat
import com.aozijx.passly.feature.backup.presentation.ui.model.BackupRestoreSheetUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupSheetUiMapperTest {
    private data class ExpectedFormatPresentation(
        val titleResource: Int,
        val securityNoticeResource: Int,
        val isWarning: Boolean,
        val requiresPassword: Boolean,
        val supportsResources: Boolean,
    )

    @Test
    fun `maps presentation metadata for every export format`() {
        val expected =
            mapOf(
                BackupExportFormat.ENCRYPTED to
                    ExpectedFormatPresentation(
                        R.string.settings_backup_encrypted_options_title,
                        R.string.settings_backup_encrypted_security_notice,
                        isWarning = false,
                        requiresPassword = true,
                        supportsResources = true,
                    ),
                BackupExportFormat.JSON to
                    ExpectedFormatPresentation(
                        R.string.settings_backup_json_options_title,
                        R.string.settings_backup_json_security_notice,
                        isWarning = true,
                        requiresPassword = false,
                        supportsResources = true,
                    ),
                BackupExportFormat.TEXT to
                    ExpectedFormatPresentation(
                        R.string.settings_backup_text_options_title,
                        R.string.settings_backup_text_security_notice,
                        isWarning = true,
                        requiresPassword = false,
                        supportsResources = false,
                    ),
            )

        BackupExportFormat.entries.forEach { format ->
            val options =
                BackupExportOptions(
                    format = format,
                    fileName = "backup.${format.extension}",
                    includedEntryTypes = setOf(EntryType.LOGIN),
                )
            val actual =
                BackupUiState(workflow = BackupWorkflow.ConfigureExport(options))
                    .toSheetUiState(configuredDirectoryLabel = null) as BackupRestoreSheetUiState.ExportOptions
            val expectation = expected.getValue(format)

            assertEquals("title for $format", expectation.titleResource, actual.titleResource)
            assertEquals(
                "security notice for $format",
                expectation.securityNoticeResource,
                actual.securityNoticeResource,
            )
            assertEquals("warning style for $format", expectation.isWarning, actual.securityNoticeIsWarning)
            assertEquals("password requirement for $format", expectation.requiresPassword, actual.requiresPassword)
            assertEquals("resource support for $format", expectation.supportsResources, actual.supportsResources)
        }
    }

    @Test
    fun `maps export configuration to its dedicated sheet state`() {
        val password = OwnedChars.fromString("temporary")
        try {
            val result =
                BackupUiState(
                    workflow =
                        BackupWorkflow.ConfigureExport(
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
            assertEquals(setOf(EntryType.LOGIN, EntryType.NOTE), result.includedEntryTypes)
            assertFalse(result.includeIcons)
            assertTrue(result.includeAttachments)
            assertFalse(result.includeDeleted)
            assertTrue(result.canSubmit)
        } finally {
            password.close()
        }
    }
}
