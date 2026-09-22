package com.aozijx.passly.presentation.feature.database.reset

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DatabaseResetOwnershipBoundaryTest {
    @Test
    fun `database reset uses a route and one presentation state`() {
        val root = sourceRoot()
        val feature = root.resolve(
            "com/aozijx/passly/presentation/feature/database/reset",
        )
        val route = feature.resolve("DatabaseResetRoute.kt")
        val sheet = feature.resolve("ui/DatabaseResetSheet.kt")
        val legacyUi = root.resolve("com/aozijx/passly/presentation/ui/database/reset")

        assertTrue(route.isFile)
        assertTrue(sheet.isFile)
        assertTrue(route.readText().contains("fun DatabaseResetRoute("))
        assertFalse(feature.resolve("DatabaseResetOverlay.kt").exists())
        assertFalse(legacyUi.walkTopDown().any { it.isFile && it.extension == "kt" })
        assertFalse(feature.walkTopDown().filter { it.isFile }.any {
            it.readText().contains("DatabaseResetSheetState") ||
                it.readText().contains("DatabaseResetEventHandler")
        })
        assertFalse(sheet.readText().contains("ViewModel"))
        assertTrue(sheet.readText().contains("onReset: () -> Unit"))
        assertTrue(sheet.readText().contains("onDismiss: () -> Unit"))
    }

    private fun sourceRoot(): File = listOf(File("src/main/java"), File("app/src/main/java"))
        .firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
}
