package com.aozijx.passly.presentation.feature.settings.main.navigation

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsRouteOwnershipBoundaryTest {
    @Test
    fun `settings passive ui lives inside the settings feature root`() {
        val sourceRoot = listOf(
            File("src/main/java"),
            File("app/src/main/java"),
        ).firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
        val legacyRoot = File(
            sourceRoot,
            "com/aozijx/passly/presentation/ui/settings",
        )

        assertFalse(
            "Settings UI still has a parallel presentation ui root",
            legacyRoot.walkTopDown().any { it.isFile && it.extension == "kt" },
        )
        assertTrue(
            File(
                sourceRoot,
                "com/aozijx/passly/presentation/feature/settings/ui/main/" +
                    "SettingsMainPage.kt",
            ).isFile,
        )
    }

    @Test
    fun `about storage diagnostics and notifications destinations have one route owner`() {
        val sourceRoot = listOf(
            File("src/main/java"),
            File("app/src/main/java"),
        ).firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
        val dataRoot = File(
            sourceRoot,
            "com/aozijx/passly/presentation/feature/settings/main/data",
        )
        val navigationRoot = File(
            sourceRoot,
            "com/aozijx/passly/presentation/feature/settings/main/navigation",
        )

        val aboutRoute = File(navigationRoot, "about/AboutRoute.kt").readText()
        val dataRoute = File(navigationRoot, "data/DataManagementRoute.kt").readText()
        val diagnosticsRoute = File(navigationRoot, "data/DiagnosticsRoute.kt")
        val notificationsRoute = File(navigationRoot, "general/NotificationsRoute.kt").readText()
        assertTrue(File(dataRoot, "StorageSettingsViewModel.kt").isFile)
        assertTrue(dataRoute.contains("StorageSettingsViewModel"))
        assertTrue(dataRoute.contains("LogSettingsSection"))
        assertFalse(aboutRoute.contains("ViewModel"))
        assertFalse(aboutRoute.contains("DiagnosticsViewModel"))
        assertTrue(diagnosticsRoute.isFile)
        assertTrue(diagnosticsRoute.readText().contains("DiagnosticsViewModel"))
        assertTrue(diagnosticsRoute.readText().contains("CreateDocument"))
        assertTrue(diagnosticsRoute.readText().contains("DiagnosticsAction.PageEntered"))
        assertTrue(notificationsRoute.contains("NotificationSettingsViewModel"))
        assertTrue(notificationsRoute.contains("rememberPermissionRequestHost"))
    }

    @Test
    fun `diagnostics screen exposes filters search and sanitized copy actions`() {
        val sourceRoot = listOf(File("src/main/java"), File("app/src/main/java"))
            .firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
        val screen = sourceRoot.resolve(
            "com/aozijx/passly/presentation/feature/settings/ui/diagnostics/DiagnosticsScreen.kt",
        )

        assertTrue(screen.isFile)
        val source = screen.readText()
        listOf("ERROR", "WARN", "INFO", "DEBUG", "SearchChanged", "CopyRow", "CopyTrace").forEach {
            assertTrue("Missing diagnostics interaction: $it", source.contains(it))
        }
        assertFalse(source.contains("SanitizedLogRecord"))
    }

    @Test
    fun `data management section has no callback handler model layer`() {
        val sourceRoot = listOf(
            File("src/main/java"),
            File("app/src/main/java"),
        ).firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
        val dataUiRoot = File(
            sourceRoot,
            "com/aozijx/passly/presentation/feature/settings/ui/data",
        )

        assertTrue(File(dataUiRoot, "DataManagementSection.kt").isFile)
        assertFalse(File(dataUiRoot, "model/DataManagementUiModels.kt").exists())
    }

    @Test
    fun `permissions destination has one route owner`() {
        val sourceRoot = listOf(File("src/main/java"), File("app/src/main/java"))
            .firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
        val route = sourceRoot.resolve(
            "com/aozijx/passly/presentation/feature/settings/main/navigation/core/" +
                "PermissionsRoute.kt",
        ).readText()

        assertTrue(route.contains("PermissionManagementViewModel"))
        assertTrue(route.contains("PermissionManagementScreen"))
        assertTrue(route.contains("rememberPermissionRequestHost"))
    }
}
