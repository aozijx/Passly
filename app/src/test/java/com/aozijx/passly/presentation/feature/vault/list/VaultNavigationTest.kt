package com.aozijx.passly.presentation.feature.vault.list

import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultAddTypeUiModel
import com.aozijx.passly.presentation.feature.vault.navigation.VaultDestinationRequest
import com.aozijx.passly.presentation.feature.vault.navigation.VaultNavigation
import com.aozijx.passly.presentation.feature.shell.navigation.AppRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultNavigationTest {
    @Test
    fun `registered editor types resolve through one single top navigation path`() {
        val routes = mutableListOf<String>()
        val navigation = navigation(singleTopRoutes = routes)

        val handled = mapOf(
            VaultAddTypeUiModel.PASSWORD to AppRoute.AddPassword.route,
            VaultAddTypeUiModel.TOTP to AppRoute.AddOtp.route,
            VaultAddTypeUiModel.BANK_CARD to AppRoute.AddBankCard.route,
            VaultAddTypeUiModel.WIFI to AppRoute.AddWifi.route,
        ).map { (type, expectedRoute) ->
            navigation.navigate(VaultDestinationRequest.OpenEditor(type)) to expectedRoute
        }

        assertTrue(handled.all { it.first })
        assertEquals(handled.map { it.second }, routes)
    }

    @Test
    fun `unregistered editor type remains available to the existing fallback owner`() {
        val routes = mutableListOf<String>()
        val navigation = navigation(singleTopRoutes = routes)

        val handled = navigation.navigate(
            VaultDestinationRequest.OpenEditor(VaultAddTypeUiModel.SSH_KEY),
        )

        assertFalse(handled)
        assertTrue(routes.isEmpty())
    }

    @Test
    fun `settings and detail use regular navigation`() {
        val routes = mutableListOf<String>()
        val navigation = navigation(routes = routes)

        assertTrue(navigation.navigate(VaultDestinationRequest.OpenSettings))
        assertTrue(navigation.navigate(VaultDestinationRequest.OpenDetail("entry-1")))

        assertEquals(
            listOf(AppRoute.Settings.route, AppRoute.Detail.createRoute("entry-1")),
            routes,
        )
    }

    private fun navigation(
        routes: MutableList<String> = mutableListOf(),
        singleTopRoutes: MutableList<String> = mutableListOf(),
    ) = VaultNavigation(
        navigateToRoute = routes::add,
        navigateToSingleTopRoute = singleTopRoutes::add,
    )
}
