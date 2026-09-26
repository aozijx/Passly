package com.aozijx.passly.presentation.feature.vault.editor.bankcard

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BankCardEditorOwnershipBoundaryTest {
    @Test
    fun `bank card editor uses feature contract without ui mapping bridge`() {
        val sourceRoot = sourceRoot()
        val editorRoot = sourceRoot.resolve(
            "com/aozijx/passly/presentation/feature/vault/editor",
        )
        val route = editorRoot.resolve("bankcard/AddBankCardEditorRoute.kt").readText()
        val uiRoot = editorRoot.resolve("ui/bankcard")
        val screen = uiRoot.resolve("AddBankCardEditorScreen.kt").readText()

        assertFalse(route.contains("BankCardEditorState"))
        assertFalse(route.contains("BankCardEditorEventHandler"))
        assertFalse(route.contains("valueOf(name)"))
        assertFalse(route.contains("fun submit"))
        assertTrue(screen.contains("state: AddBankCardUiState"))
        assertTrue(screen.contains("onAction: (AddBankCardAction) -> Unit"))
        assertTrue(screen.contains("selected: CardType?"))
        assertFalse(uiRoot.resolve("BankCardEditorContract.kt").exists())
    }

    private fun sourceRoot(): File = listOf(File("src/main/java"), File("app/src/main/java"))
        .firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
}
