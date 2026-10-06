package com.aozijx.passly.presentation.feature.vault.list

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultListActionOwnershipBoundaryTest {
    @Test
    fun `vault sections send the page action without event translation bridges`() {
        val sourceRoot = sourceRoot()
        val listRoot = sourceRoot.resolve(
            "com/aozijx/passly/presentation/feature/vault/list",
        )
        val route = listRoot.resolve("VaultRoute.kt").readText()
        val routeRequest = listRoot.resolve("VaultRouteRequest.kt").readText()
        val pageActions = listRoot.resolve("VaultUiAction.kt").readText()
        val viewModel = listRoot.resolve("VaultViewModel.kt").readText()
        val screen = listRoot.resolve("ui/VaultScreen.kt").readText()
        val models = listRoot.resolve("ui/model/VaultListUiModels.kt").readText()
        val itemActions = listRoot.resolve("ui/model/VaultListItemAction.kt")
        val topBarState = listRoot.resolve(
            "ui/component/topbar/VaultTopBarUiState.kt",
        ).readText()
        val menuState = listRoot.resolve(
            "ui/component/topbar/VaultMenuUiState.kt",
        )
        val searchAction = listRoot.resolve(
            "ui/component/topbar/VaultSearchAction.kt",
        ).readText()
        val dropdownMenu = listRoot.resolve(
            "ui/component/topbar/VaultDropdownMenu.kt",
        ).readText()
        val menuPageTransition = listRoot.resolve(
            "ui/component/topbar/VaultMenuPageTransition.kt",
        ).readText()

        assertFalse(listRoot.resolve("VaultListEventRouting.kt").exists())
        assertFalse(listRoot.resolve("VaultListItemEventHandler.kt").exists())
        assertFalse(models.contains("VaultListEvent"))
        assertFalse(models.contains("VaultListItemEvent"))
        assertTrue(pageActions.contains("sealed interface VaultUiAction"))
        assertTrue(routeRequest.contains("data object OpenScanner"))
        assertTrue(routeRequest.contains("data object OpenSettings"))
        assertEquals(2, Regex("data object").findAll(routeRequest).count())
        assertFalse(pageActions.contains("Scanner"))
        assertFalse(viewModel.contains("showScanner"))
        assertTrue(route.contains("var showScanner by rememberSaveable"))
        assertTrue(route.contains("ScannerRoute("))
        assertTrue(route.contains("VaultUiAction.AddScannedOtp"))
        assertFalse(models.contains("sealed interface VaultListItemAction"))
        assertFalse(listRoot.resolve("ui/component/topbar/VaultMoreMenu.kt").exists())
        assertTrue(menuState.exists())
        assertTrue(menuState.readText().contains("data class VaultMenuUiState"))
        assertTrue(itemActions.readText().contains("sealed interface VaultListItemAction"))
        assertTrue(screen.contains("onAction: (VaultUiAction) -> Unit"))
        assertTrue(screen.contains("onItemAction: (VaultListItemAction) -> Unit"))
        assertTrue(screen.contains("onRouteRequest: (VaultRouteRequest) -> Unit"))
        assertFalse(screen.contains("onSettingsClick"))
        assertFalse(screen.contains("handleTopBarAction"))
        assertFalse(topBarState.contains("VaultTopBarAction"))
        assertTrue(topBarState.contains("val menu: VaultMenuUiState"))
        assertFalse(searchAction.contains("VaultMoreAction("))
        assertFalse(searchAction.contains("VaultMoreMenu("))
        assertTrue(searchAction.contains("VaultDropdownMenu("))
        assertTrue(searchAction.contains("onRouteRequest: (VaultRouteRequest) -> Unit"))
        assertFalse(searchAction.contains("onSettingsClick"))
        assertTrue(dropdownMenu.contains("onRouteRequest: (VaultRouteRequest) -> Unit"))
        assertFalse(dropdownMenu.contains("onSettingsClick"))
        assertFalse(searchAction.contains("AnimatedContent"))
        assertTrue(dropdownMenu.contains("expanded: Boolean"))
        assertTrue(dropdownMenu.contains("uiState: VaultMenuUiState"))
        assertFalse(dropdownMenu.contains("showTOTPCode: Boolean"))
        assertFalse(dropdownMenu.contains("availableCategories: List<String>"))
        assertTrue(dropdownMenu.contains("expanded = expanded"))
        assertTrue(menuPageTransition.contains("AnimatedVisibility"))
        assertFalse(dropdownMenu.contains("AnimatedContent"))
        assertFalse(menuPageTransition.contains("AnimatedContent"))
        assertFalse(menuPageTransition.contains("wrapContentWidth"))
        assertFalse(menuPageTransition.contains("animatedPageWidthPx"))
        assertFalse(dropdownMenu.contains("Crossfade"))
        assertFalse(dropdownMenu.contains("slideInHorizontally"))
        assertFalse(dropdownMenu.contains("slideOutHorizontally"))
        assertFalse(dropdownMenu.contains("widthIn"))
        assertTrue(dropdownMenu.contains("animateContentSize"))
        assertTrue(route.contains("onAction = vaultViewModel::onAction"))
        assertFalse(route.contains("rememberVaultListItemEventHandler"))
    }

    private fun sourceRoot(): File = listOf(File("src/main/java"), File("app/src/main/java"))
        .firstOrNull(File::isDirectory) ?: error("Cannot locate app source root")
}
