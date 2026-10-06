package com.aozijx.passly.presentation.feature.settings.backup

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupDirectoryOwnershipBoundaryTest {
    @Test
    fun `route delegates persisted grant lifecycle to feature interactor`() {
        val sourceRoot = listOf(File("src/main/java"), File("app/src/main/java"))
            .firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
        val route = sourceRoot.resolve(
            "com/aozijx/passly/presentation/feature/settings/main/navigation/data/BackupRoute.kt",
        ).readText()
        val viewModel = sourceRoot.resolve(
            "com/aozijx/passly/presentation/feature/settings/backup/DataManagementSettingsViewModel.kt",
        ).readText()
        val oldFlow = sourceRoot.resolve(
            "com/aozijx/passly/presentation/feature/settings/backup/BackupFlows.kt",
        )
        val directoryAccess = sourceRoot.resolve(
            "com/aozijx/passly/app/backup/AndroidBackupDirectoryAccess.kt",
        ).readText()
        val exportStorage = sourceRoot.resolve(
            "com/aozijx/passly/feature/backup/internal/archive/platform/BackupExportStorageSupport.kt",
        ).readText()
        val fileStore = sourceRoot.resolve(
            "com/aozijx/passly/feature/backup/internal/archive/AndroidBackupFileStore.kt",
        ).readText()

        assertFalse(route.contains("contentResolver"))
        assertFalse(route.contains("takePersistableUriPermission"))
        assertFalse(route.contains("releasePersistableUriPermission"))
        assertFalse(route.contains("handleBackupPathPicked"))
        assertTrue(viewModel.contains("BackupDirectorySettingsInteractor"))
        assertFalse(viewModel.contains("BackupDirectorySettingsRepository"))
        assertFalse(oldFlow.exists())
        assertTrue(directoryAccess.contains("BackupDirectoryGrantInspector"))
        assertTrue(exportStorage.contains("BackupDirectoryGrantInspector"))
        assertFalse(directoryAccess.contains("persistedUriPermissions"))
        assertFalse(exportStorage.contains("persistedUriPermissions"))
        assertFalse(fileStore.contains("persistedUriPermissions"))
        assertTrue(fileStore.contains("openFileDescriptor(parsed, \"rw\")"))
    }
}
