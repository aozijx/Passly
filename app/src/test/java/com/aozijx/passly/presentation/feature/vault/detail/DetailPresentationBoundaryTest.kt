package com.aozijx.passly.presentation.feature.vault.detail

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailPresentationBoundaryTest {
    @Test
    fun `detail content and overlays are passive ui`() {
        val sourceRoot = sourceRoot()
        val content = sourceRoot.resolve(
            "com/aozijx/passly/presentation/ui/vault/detail/DetailContent.kt",
        )
        val overlays = sourceRoot.resolve(
            "com/aozijx/passly/presentation/ui/vault/detail/DetailEditorOverlays.kt",
        )

        assertTrue(content.exists())
        assertTrue(overlays.exists())
        listOf(content, overlays).forEach { file ->
            val source = file.readText()
            assertFalse(source.contains("presentation.feature.vault.detail"))
            assertFalse(source.contains("DetailUiState"))
            assertFalse(source.contains("DetailUiAction"))
            assertFalse(source.contains("rememberImagePicker"))
        }
    }

    @Test
    fun `detail route owns presentation mapping and platform image picking`() {
        val sourceRoot = sourceRoot()
        val route = sourceRoot.resolve(
            "com/aozijx/passly/presentation/feature/vault/detail/DetailRoute.kt",
        ).readText()
        val bindingRoot = sourceRoot.resolve(
            "com/aozijx/passly/presentation/feature/vault/detail/binding",
        )

        assertTrue(route.contains("toDetailPresentationModel"))
        assertTrue(route.contains("rememberImagePicker"))
        assertFalse(bindingRoot.resolve("DetailBodyBinding.kt").exists())
        assertFalse(bindingRoot.resolve("DetailEditorOverlayBinding.kt").exists())
    }

    private fun sourceRoot(): File = listOf(File("src/main/java"), File("app/src/main/java"))
        .firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
}
