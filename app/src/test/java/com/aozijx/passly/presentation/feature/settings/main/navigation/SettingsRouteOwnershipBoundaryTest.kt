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
    fun `general diagnostics and notifications destinations have one route owner`() {
        val sourceRoot = listOf(
            File("src/main/java"),
            File("app/src/main/java"),
        ).firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
        val generalRoot = File(
            sourceRoot,
            "com/aozijx/passly/presentation/feature/settings/main/general",
        )
        val routeRoot = File(
            sourceRoot,
            "com/aozijx/passly/presentation/feature/settings/main/navigation/general",
        )

        assertFalse(File(generalRoot, "GeneralDetail.kt").exists())
        assertFalse(File(generalRoot, "NotificationDetail.kt").exists())

        val generalRoute = File(routeRoot, "GeneralRoute.kt").readText()
        val diagnosticsRoute = File(routeRoot, "DiagnosticsRoute.kt")
        val notificationsRoute = File(routeRoot, "NotificationsRoute.kt").readText()
        assertTrue(generalRoute.contains("GeneralSettingsViewModel"))
        assertFalse(generalRoute.contains("DiagnosticsViewModel"))
        assertFalse(generalRoute.contains("CreateDocument"))
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
