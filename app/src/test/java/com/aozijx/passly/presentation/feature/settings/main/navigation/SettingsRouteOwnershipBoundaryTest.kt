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
    fun `general and notifications destinations have one route owner`() {
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
        val notificationsRoute = File(routeRoot, "NotificationsRoute.kt").readText()
        assertTrue(generalRoute.contains("GeneralSettingsViewModel"))
        assertTrue(generalRoute.contains("DiagnosticsSettingsViewModel"))
        assertTrue(notificationsRoute.contains("NotificationSettingsViewModel"))
        assertTrue(notificationsRoute.contains("rememberPermissionRequestHost"))
    }
}
