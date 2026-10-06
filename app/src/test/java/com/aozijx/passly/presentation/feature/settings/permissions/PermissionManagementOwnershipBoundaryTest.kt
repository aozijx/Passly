package com.aozijx.passly.presentation.feature.settings.permissions

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionManagementOwnershipBoundaryTest {
    @Test
    fun `screen is passive and route owns runtime permission effects`() {
        val sourceRoot = listOf(File("src/main/java"), File("app/src/main/java"))
            .firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
        val screen = sourceRoot.resolve(
            "com/aozijx/passly/presentation/feature/settings/ui/permissions/" +
                "PermissionManagementScreen.kt",
        ).readText()
        val route = sourceRoot.resolve(
            "com/aozijx/passly/presentation/feature/settings/main/navigation/core/" +
                "PermissionsRoute.kt",
        ).readText()

        assertFalse(screen.contains("hiltViewModel"))
        assertFalse(screen.contains("PermissionRequestHost"))
        assertFalse(screen.contains("BackupDirectorySettingsInteractor"))
        assertFalse(screen.contains("android.content"))
        assertTrue(route.contains("rememberPermissionRequestHost"))
        assertTrue(route.contains("ACTION_APPLICATION_DETAILS_SETTINGS"))
        assertTrue(route.contains("LifecycleResumeEffect"))
    }

    @Test
    fun `backup page is the only owner of directory management`() {
        val sourceRoot = listOf(File("src/main/java"), File("app/src/main/java"))
            .firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
        val permissionRoute = sourceRoot.resolve(
            "com/aozijx/passly/presentation/feature/settings/main/navigation/core/" +
                "PermissionsRoute.kt",
        ).readText()
        val permissionViewModel = sourceRoot.resolve(
            "com/aozijx/passly/presentation/feature/settings/permissions/" +
                "PermissionManagementViewModel.kt",
        ).readText()
        val backupRoute = sourceRoot.resolve(
            "com/aozijx/passly/presentation/feature/settings/main/navigation/data/" +
                "BackupRoute.kt",
        ).readText()

        assertFalse(permissionRoute.contains("OpenDocumentTree"))
        assertFalse(permissionRoute.contains("BackupDirectoryClearDialog"))
        assertFalse(permissionRoute.contains("directoryPicked"))
        assertFalse(permissionRoute.contains("clearDirectory"))
        assertTrue(permissionRoute.contains("onOpenBackupSettings"))
        assertFalse(permissionViewModel.contains("BackupDirectorySettingsInteractor"))
        assertTrue(permissionViewModel.contains("BackupDirectoryStatusInteractor"))
        assertTrue(backupRoute.contains("OpenDocumentTree"))
        assertTrue(backupRoute.contains("BackupDirectoryClearDialog"))
    }
}
