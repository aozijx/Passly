package com.aozijx.passly.presentation.feature.backup

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupSheetOwnershipBoundaryTest {
    @Test
    fun `backup sheet uses one typed event sink and feature option models`() {
        val sourceRoot = sourceRoot()
        val backupRoot = sourceRoot.resolve(
            "com/aozijx/passly/presentation/feature/backup",
        )
        val route = backupRoot.resolve("BackupOperationRoute.kt").readText()
        val models = backupRoot.resolve("ui/model/BackupRestoreUiModels.kt").readText()
        val sheets = backupRoot.resolve("ui/BackupRestoreSheets.kt").readText()
        val mapper = backupRoot.resolve("BackupSheetUiMapper.kt").readText()

        assertFalse(models.contains("BackupRestoreSheetEventHandler"))
        assertFalse(models.contains("BackupExportFormatUiModel"))
        assertFalse(models.contains("BackupImportModeUiModel"))
        assertTrue(models.contains("sealed interface BackupSheetEvent"))
        assertTrue(sheets.contains("onEvent: (BackupSheetEvent) -> Unit"))
        assertFalse(route.contains("object : BackupRestoreSheetEventHandler"))
        assertFalse(mapper.contains("valueOf("))
    }

    private fun sourceRoot(): File = listOf(File("src/main/java"), File("app/src/main/java"))
        .firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
}
