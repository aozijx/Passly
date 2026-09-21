package com.aozijx.passly.presentation.feature.vault.navigation

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultDetailOwnershipBoundaryTest {

    @Test
    fun `detail ui lives inside the detail feature root`() {
        val legacyRoot = File(
            "app/src/main/java/com/aozijx/passly/presentation/ui/vault/detail",
        ).takeIf { File("app").isDirectory }
            ?: File("src/main/java/com/aozijx/passly/presentation/ui/vault/detail")

        assertFalse(
            "Detail UI still has a parallel presentation ui root",
            legacyRoot.walkTopDown().any { it.isFile && it.extension == "kt" },
        )
    }

    @Test
    fun `vault list ui lives inside the list feature root`() {
        val legacyRoot = File(
            "app/src/main/java/com/aozijx/passly/presentation/ui/vault/list",
        ).takeIf { File("app").isDirectory }
            ?: File("src/main/java/com/aozijx/passly/presentation/ui/vault/list")

        assertFalse(
            "Vault list UI still has a parallel presentation ui root",
            legacyRoot.walkTopDown().any { it.isFile && it.extension == "kt" },
        )
        source("com/aozijx/passly/presentation/feature/vault/list/ui/VaultScreen.kt")
    }

    @Test
    fun `root navigation does not own the vault view model`() {
        val source = source(
            "com/aozijx/passly/presentation/feature/shell/PasslyAppNavigation.kt",
        )

        assertFalse(source.contains("VaultViewModel"))
        assertFalse(source.contains("hiltViewModel"))
    }

    @Test
    fun `vault graph exposes one navigation contract without a content wrapper`() {
        val graph = source(
            "com/aozijx/passly/presentation/feature/vault/navigation/VaultGraphRegistration.kt",
        )
        val route = source(
            "com/aozijx/passly/presentation/feature/vault/list/VaultRoute.kt",
        )
        val signature = route.substringAfter("fun VaultRoute(").substringBefore(") {")

        assertFalse(graph.contains("VaultDestinationContent"))
        assertTrue(signature.contains("navigation: VaultNavigation"))
        listOf(
            "onAddPassword", "onAddOtp", "onAddBankCard",
            "onSettingsClick", "onShowDetail",
        ).forEach { callback ->
            assertFalse("VaultRoute still accepts $callback directly", signature.contains(callback))
        }
    }

    @Test
    fun `vault graph does not create destination owned view models`() {
        val graph = source(
            "com/aozijx/passly/presentation/feature/vault/navigation/VaultGraphRegistration.kt",
        )

        listOf(
            "AddPasswordViewModel",
            "AddOtpViewModel",
            "AddBankCardViewModel",
            "TrashViewModel",
        ).forEach { viewModel ->
            assertFalse("Vault graph still creates $viewModel", graph.contains(viewModel))
        }
    }
    @Test
    fun `detail destination never depends on vault view model`() {
        val graph = source(
            "com/aozijx/passly/presentation/feature/vault/navigation/VaultGraphRegistration.kt",
        )
        val detailDestination = graph.substringAfter("route = AppRoute.Detail.route")
            .substringBefore("\n    }\n}")

        assertFalse(detailDestination.contains("vaultViewModel"))
        assertFalse(detailDestination.contains("VaultUiAction"))
    }

    @Test
    fun `detail route accepts an id and detail view model loads it`() {
        val route = source(
            "com/aozijx/passly/presentation/feature/vault/detail/DetailRoute.kt",
        )
        val viewModel = source(
            "com/aozijx/passly/presentation/feature/vault/detail/DetailViewModel.kt",
        )
        val sessionLoader = source(
            "com/aozijx/passly/presentation/feature/vault/detail/DetailSessionLoader.kt",
        )

        val signature = route.substringAfter("fun DetailRoute(").substringBefore(") {")
        assertTrue(route.contains("entryId: String"))
        assertTrue(signature.contains("onOpenRelatedEntry: (String) -> Unit"))
        assertFalse(signature.contains("(Entry) -> Unit"))
        assertFalse(route.contains("initialEntry"))
        assertFalse(route.contains("onAutoUnlockTotp"))
        assertTrue(viewModel.contains("sessionLoader.open(entryId)"))
        assertTrue(sessionLoader.contains("entryQueryRepository.getById(entryId)"))
    }

    @Test
    fun `vault view model receives authorized operations instead of assembling them`() {
        val viewModel = source(
            "com/aozijx/passly/presentation/feature/vault/list/VaultViewModel.kt",
        )

        assertFalse(viewModel.contains("AuthorizationGate"))
        assertFalse(viewModel.contains("MoveEntryToTrashUseCase("))
        assertFalse(viewModel.contains("CopyEntryFieldUseCase("))
        assertFalse(viewModel.contains("CopyOtpCodeUseCase("))
    }
    @Test
    fun `detail view model receives authorized operations instead of assembling them`() {
        val viewModel = source(
            "com/aozijx/passly/presentation/feature/vault/detail/DetailViewModel.kt",
        )

        assertFalse(viewModel.contains("AuthorizationGate"))
        assertFalse(viewModel.contains("ExportOtpQrUseCase("))
        assertFalse(viewModel.contains("RevealEntryFieldsUseCase("))
        assertFalse(viewModel.contains("CopyEntryFieldUseCase("))
        assertFalse(viewModel.contains("CopyOtpCodeUseCase("))
        assertFalse(viewModel.contains("DetailEntryPatch"))
        assertFalse(viewModel.contains("SensitiveFieldRepository"))
        assertFalse(viewModel.contains("ActivityRecorder"))
        assertFalse(viewModel.contains("EntryQueryRepository"))
        assertFalse(viewModel.contains("userConfigExtras"))
        assertTrue(viewModel.contains("private val sessionLoader: DetailSessionLoader"))
        assertTrue(viewModel.contains("private val editDetailEntry: EditDetailEntryUseCase"))
        assertTrue(viewModel.contains("private val entryTagQuery: EntryTagQuery"))
    }

    @Test
    fun `detail passive ui emits feature actions without a route bridge`() {
        val detailRoot = "com/aozijx/passly/presentation/feature/vault/detail/"
        val content = source("${detailRoot}ui/DetailContent.kt")
        val overlays = source("${detailRoot}ui/DetailEditorOverlays.kt")
        val models = source("${detailRoot}ui/model/DetailPresentationModels.kt")
        val bridge = sourceFile("${detailRoot}DetailPresentationEvents.kt")

        assertFalse("Detail Route still needs a presentation event bridge", bridge.exists())
        assertFalse(models.contains("sealed interface DetailContentEvent"))
        assertFalse(models.contains("sealed interface DetailEditorOverlayEvent"))
        assertTrue(content.contains("onAction: (DetailUiAction) -> Unit"))
        assertTrue(content.contains("onOpenRelatedEntry: (String) -> Unit"))
        assertTrue(overlays.contains("onAction: (DetailUiAction) -> Unit"))
        assertTrue(overlays.contains("onUploadFavicon: () -> Unit"))
    }

    private fun source(relativePath: String): String {
        return sourceFile(relativePath).readText()
    }

    private fun sourceFile(relativePath: String): File {
        val sourceRoot = listOf(
            File("src/main/java"),
            File("app/src/main/java"),
        ).firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
        return File(sourceRoot, relativePath)
    }
}
