package com.aozijx.passly.presentation.feature.vault.detail

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailPresentationBoundaryTest {
    @Test
    fun `detail body and overlays are passive ui`() {
        val sourceRoot = sourceRoot()
        val body = sourceRoot.resolve(
            "com/aozijx/passly/presentation/feature/vault/detail/ui/DetailBody.kt",
        )
        val overlays = sourceRoot.resolve(
            "com/aozijx/passly/presentation/feature/vault/detail/ui/DetailEditorOverlays.kt",
        )

        assertTrue(body.exists())
        assertTrue(overlays.exists())
        listOf(body, overlays).forEach { file ->
            val source = file.readText()
            assertFalse(source.contains("DetailUiState"))
            assertFalse(source.contains("rememberImagePicker"))
            assertFalse(source.contains("DetailViewModel"))
            assertTrue(source.contains("onAction: (DetailUiAction) -> Unit"))
        }
        val detailRoot = sourceRoot.resolve(
            "com/aozijx/passly/presentation/feature/vault/detail",
        )
        assertFalse(detailRoot.resolve("DetailInteractionCallbacks.kt").exists())
        assertFalse(
            detailRoot.resolve("ui/model/DetailInteractionContracts.kt").exists(),
        )
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
        assertTrue(route.contains("onAction = viewModel::onAction"))
        assertFalse(route.contains("DetailInteractionCallbacks("))
        assertFalse(bindingRoot.resolve("DetailBodyBinding.kt").exists())
        assertFalse(bindingRoot.resolve("DetailEditorOverlayBinding.kt").exists())
    }

    @Test
    fun `detail presentation does not own session authorization policy`() {
        val sourceRoot = sourceRoot()
        val detailRoot = sourceRoot.resolve(
            "com/aozijx/passly/presentation/feature/vault/detail",
        )
        val viewModel = detailRoot.resolve("DetailViewModel.kt").readText()

        assertFalse(detailRoot.resolve("DetailAccessPolicy.kt").exists())
        assertFalse(viewModel.contains("DetailAccessPolicy"))
        assertFalse(viewModel.contains("canHandle(event)"))
        assertFalse(viewModel.contains("edit !is DetailEntryEdit.SetTags"))
        assertTrue(viewModel.contains("feature.vault.SecureSessionAccessPolicy"))
    }

    @Test
    fun `detail screen owns the complete page composition`() {
        val sourceRoot = sourceRoot()
        val detailRoot = sourceRoot.resolve(
            "com/aozijx/passly/presentation/feature/vault/detail",
        )
        val route = detailRoot.resolve("DetailRoute.kt").readText()
        val screen = detailRoot.resolve("ui/DetailScreen.kt").readText()

        assertFalse(route.contains("DetailContent"))
        assertFalse(route.contains("DetailEditorOverlays"))
        assertTrue(screen.contains("model: DetailPresentationModel"))
        assertTrue(screen.contains("DetailBody("))
        assertTrue(screen.contains("DetailEditorOverlays("))
        assertFalse(screen.contains("content: @Composable"))
    }

    private fun sourceRoot(): File = listOf(File("src/main/java"), File("app/src/main/java"))
        .firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
}
