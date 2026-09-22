package com.aozijx.passly.presentation.shared.components

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultItemIconBoundaryTest {
    @Test
    fun `vault item icon does not resolve platform services`() {
        val sourceRoot = listOf(
            File("src/main/java/com/aozijx/passly"),
            File("app/src/main/java/com/aozijx/passly"),
        ).firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
        val source = File(
            sourceRoot,
            "presentation/shared/components/VaultItemIcon.kt",
        ).readText()

        assertFalse(source.contains("EntryPointAccessors"))
        assertFalse(source.contains("InstalledAppIconLoader"))
        assertFalse(source.contains("rememberAppIcon"))

        val coreSourceRoot = listOf(
            File("../core/src/main/kotlin/com/aozijx/passly"),
            File("core/src/main/kotlin/com/aozijx/passly"),
        ).firstOrNull(File::isDirectory) ?: error("Cannot locate core source root")
        val platformState = File(
            coreSourceRoot,
            "core/platform/packageinfo/InstalledAppIconState.kt",
        ).readText()
        assertTrue(platformState.contains("remember(iconLoader, packageName)"))
        assertTrue(platformState.contains("Dispatchers.IO"))
    }
}
