package com.aozijx.passly.presentation.feature.settings.permissions

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionManagementOwnershipBoundaryTest {
    @Test
    fun `screen is passive and route owns platform effects`() {
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
        assertTrue(route.contains("OpenDocumentTree"))
        assertTrue(route.contains("ACTION_APPLICATION_DETAILS_SETTINGS"))
        assertTrue(route.contains("LifecycleResumeEffect"))
    }
}
