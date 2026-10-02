package com.aozijx.passly.presentation.feature.settings.security

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppPasswordDialogOwnershipBoundaryTest {
    @Test
    fun `change dialog emits one typed event stream without callback interface`() {
        val sourceRoot = sourceRoot()
        val settingsRoot = sourceRoot.resolve(
            "com/aozijx/passly/presentation/feature/settings",
        )
        val dialog = settingsRoot.resolve("ui/security/AppPasswordChangeDialog.kt").readText()
        val dialogsFile = settingsRoot.resolve("ui/security/AppPasswordDialogs.kt")
        val contractFile = settingsRoot.resolve("security/AppPasswordDialogContract.kt")
        val dialogs = dialogsFile.readText()

        assertTrue(dialogsFile.isFile)
        assertTrue(contractFile.isFile)
        assertFalse(settingsRoot.resolve("ui/main/AppPasswordDialogs.kt").exists())
        assertFalse(settingsRoot.resolve("ui/main/model/AppPasswordDialogModels.kt").exists())
        assertFalse(dialog.contains("AppPasswordChangeDialogEventHandler"))
        assertTrue(dialog.contains("sealed interface AppPasswordChangeDialogEvent"))
        assertTrue(dialog.contains("onEvent: (AppPasswordChangeDialogEvent) -> Unit"))
        assertFalse(dialogs.contains("object : AppPasswordChangeDialogEventHandler"))
    }

    private fun sourceRoot(): File = listOf(File("src/main/java"), File("app/src/main/java"))
        .firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
}
