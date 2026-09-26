package com.aozijx.passly.presentation.feature.vault.editor.password

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordEditorOwnershipBoundaryTest {
    @Test
    fun `password editor uses feature state and action sink without route mapping bridge`() {
        val sourceRoot = sourceRoot()
        val editorRoot = sourceRoot.resolve(
            "com/aozijx/passly/presentation/feature/vault/editor",
        )
        val route = editorRoot.resolve("password/AddPasswordEditorRoute.kt").readText()
        val screen = editorRoot.resolve("ui/password/AddPasswordEditorScreen.kt").readText()

        assertFalse(route.contains("PasswordEditorState"))
        assertFalse(route.contains("PasswordEditorEventHandler"))
        assertFalse(route.contains("fun submit"))
        assertTrue(screen.contains("state: AddPasswordUiState"))
        assertTrue(screen.contains("onAction: (AddPasswordAction) -> Unit"))
        assertFalse(screen.contains("class PasswordEditorState"))
        assertFalse(screen.contains("class PasswordEditorEventHandler"))
    }

    private fun sourceRoot(): File = listOf(File("src/main/java"), File("app/src/main/java"))
        .firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
}
