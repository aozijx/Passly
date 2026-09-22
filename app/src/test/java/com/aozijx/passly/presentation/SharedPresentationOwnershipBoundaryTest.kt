package com.aozijx.passly.presentation

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedPresentationOwnershipBoundaryTest {
    @Test
    fun `shared presentation ui has one explicit source root`() {
        val root = sourceRoot()
        val legacy = root.resolve("com/aozijx/passly/presentation/ui/shared")
        val shared = root.resolve("com/aozijx/passly/presentation/shared")

        assertFalse(legacy.walkTopDown().any { it.isFile && it.extension == "kt" })
        assertTrue(shared.resolve("components/VaultItemIcon.kt").isFile)
        assertTrue(shared.resolve("media/ImagePicker.kt").isFile)
        assertTrue(shared.resolve("entry/EntryTypeUiModel.kt").isFile)
    }

    private fun sourceRoot(): File = listOf(File("src/main/java"), File("app/src/main/java"))
        .firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
}
