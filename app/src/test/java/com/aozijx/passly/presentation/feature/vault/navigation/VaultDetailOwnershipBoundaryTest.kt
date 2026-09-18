package com.aozijx.passly.presentation.feature.vault.navigation

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultDetailOwnershipBoundaryTest {

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

        val signature = route.substringAfter("fun DetailRoute(").substringBefore(") {")
        assertTrue(route.contains("entryId: String"))
        assertTrue(signature.contains("onOpenRelatedEntry: (String) -> Unit"))
        assertFalse(signature.contains("(Entry) -> Unit"))
        assertFalse(route.contains("initialEntry"))
        assertFalse(route.contains("onAutoUnlockTotp"))
        assertTrue(viewModel.contains("entryQueryRepository.getById(entryId)"))
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
        assertFalse(viewModel.contains("UpdateDetailEntryUseCase("))
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
        assertFalse(viewModel.contains("UpdateDetailEntryUseCase("))
        assertTrue(viewModel.contains("entryQueryRepository.getById(entryId)"))
    }
    private fun source(relativePath: String): String {
        val sourceRoot = listOf(
            File("src/main/java"),
            File("app/src/main/java"),
        ).firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
        return File(sourceRoot, relativePath).readText()
    }
}
