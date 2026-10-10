package com.aozijx.passly.presentation.feature.vault.editor.wifi

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WifiEditorOwnershipBoundaryTest {
    @Test
    fun `wifi route owns orchestration while screen stays passive`() {
        val sourceRoot = sourceRoot()
        val editorRoot = sourceRoot.resolve("com/aozijx/passly/presentation/feature/vault/editor")
        val route = editorRoot.resolve("wifi/AddWifiEditorRoute.kt").readText()
        val screen = editorRoot.resolve("ui/wifi/AddWifiEditorScreen.kt").readText()

        assertTrue(route.contains("hiltViewModel<AddWifiViewModel>"))
        assertTrue(route.contains("EditorSaveEffectHandler"))
        assertTrue(screen.contains("state: AddWifiUiState"))
        assertTrue(screen.contains("onAction: (AddWifiAction) -> Unit"))
        assertFalse(screen.contains("ViewModel"))
        assertFalse(screen.contains("CreateEntryUseCase"))
        assertFalse(screen.contains("hiltViewModel"))
    }

    private fun sourceRoot(): File = listOf(File("src/main/java"), File("app/src/main/java"))
        .firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
}
