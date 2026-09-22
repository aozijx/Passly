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
            "com/aozijx/passly/presentation/feature/vault/detail/ui/DetailContent.kt",
        )
        val overlays = sourceRoot.resolve(
            "com/aozijx/passly/presentation/feature/vault/detail/ui/DetailEditorOverlays.kt",
        )

        assertTrue(content.exists())
        assertTrue(overlays.exists())
        listOf(content, overlays).forEach { file ->
            val source = file.readText()
            assertFalse(source.contains("DetailUiState"))
            assertFalse(source.contains("rememberImagePicker"))
            assertFalse(source.contains("DetailViewModel"))
            assertFalse(source.contains("DetailUiAction"))
            assertTrue(source.contains("callbacks:"))
        }
    }

    @Test
    fun `detail view model owns presentation mapping while route owns platform image picking`() {
        val sourceRoot = sourceRoot()
        val route = sourceRoot.resolve(
            "com/aozijx/passly/presentation/feature/vault/detail/DetailRoute.kt",
        ).readText()
        val bindingRoot = sourceRoot.resolve(
            "com/aozijx/passly/presentation/feature/vault/detail/binding",
        )

        val viewModel = sourceRoot.resolve(
            "com/aozijx/passly/presentation/feature/vault/detail/DetailViewModel.kt",
        ).readText()

        assertFalse(route.contains("toDetailPresentationModel"))
        assertFalse(route.contains("viewModel.uiState"))
        assertFalse(route.contains("viewModel.otpState"))
        assertTrue(route.contains("viewModel.presentation"))
        assertTrue(viewModel.contains("toDetailPresentationModel"))
        assertTrue(route.contains("rememberImagePicker"))
        assertFalse(bindingRoot.resolve("DetailBodyBinding.kt").exists())
        assertFalse(bindingRoot.resolve("DetailEditorOverlayBinding.kt").exists())
    }

    private fun sourceRoot(): File = listOf(File("src/main/java"), File("app/src/main/java"))
        .firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
}
