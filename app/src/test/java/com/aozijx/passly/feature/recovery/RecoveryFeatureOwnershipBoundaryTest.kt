package com.aozijx.passly.feature.recovery

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveryFeatureOwnershipBoundaryTest {
    @Test
    fun `recovery owns one vertical feature root`() {
        val root = sourceRoot()
        val legacy = root.resolve("com/aozijx/passly/presentation/feature/recovery")
        val feature = root.resolve("com/aozijx/passly/feature/recovery")

        assertFalse(legacy.walkTopDown().any { it.isFile && it.extension == "kt" })
        assertTrue(feature.resolve("RecoveryModeRoute.kt").isFile)
        assertTrue(feature.resolve("RecoveryModeViewModel.kt").isFile)
        assertTrue(feature.resolve("ui/RecoveryModeScreen.kt").isFile)
    }

    private fun sourceRoot(): File = listOf(File("src/main/java"), File("app/src/main/java"))
        .firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
}
