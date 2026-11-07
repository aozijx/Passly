package com.aozijx.passly.feature.vault.presentation.list

import com.aozijx.passly.app.navigation.AppRoute
import com.aozijx.passly.domain.access.model.AuthenticationState
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import com.aozijx.passly.domain.entry.model.credential.WifiSecurityType
import com.aozijx.passly.domain.entry.policy.WifiQrPayload
import com.aozijx.passly.feature.vault.entry.EntryEditorSeedStore
import com.aozijx.passly.feature.vault.presentation.list.ui.model.VaultAddTypeUiModel
import com.aozijx.passly.feature.vault.presentation.navigation.EntryEditorSeedCoordinator
import com.aozijx.passly.feature.vault.presentation.navigation.VaultDestinationRequest
import com.aozijx.passly.feature.vault.presentation.navigation.VaultNavigation
import com.aozijx.passly.feature.vault.presentation.navigation.VaultNavigationResult
import com.aozijx.passly.testing.RecordingTestLogger
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultNavigationTest {
    @Test
    fun `registered editor types resolve through one single top navigation path`() {
        val routes = mutableListOf<String>()
        val navigation = navigation(singleTopRoutes = routes)

        val handled =
            mapOf(
                VaultAddTypeUiModel.PASSWORD to AppRoute.AddPassword.route,
                VaultAddTypeUiModel.TOTP to AppRoute.AddOtp.route,
                VaultAddTypeUiModel.BANK_CARD to AppRoute.AddBankCard.route,
                VaultAddTypeUiModel.WIFI to AppRoute.AddWifi.route,
            ).map { (type, expectedRoute) ->
                navigation.navigate(VaultDestinationRequest.OpenEditor(type)) to expectedRoute
            }

        assertTrue(handled.all { it.first == VaultNavigationResult.Navigated })
        assertEquals(handled.map { it.second }, routes)
    }

    @Test
    fun `unregistered editor type is rejected without navigating`() {
        val routes = mutableListOf<String>()
        val navigation = navigation(singleTopRoutes = routes)

        val handled =
            navigation.navigate(
                VaultDestinationRequest.OpenEditor(VaultAddTypeUiModel.SSH_KEY),
            )

        assertEquals(VaultNavigationResult.UnsupportedDestination, handled)
        assertTrue(routes.isEmpty())
    }

    @Test
    fun `settings and detail use regular navigation`() {
        val routes = mutableListOf<String>()
        val navigation = navigation(routes = routes)

        assertEquals(
            VaultNavigationResult.Navigated,
            navigation.navigate(VaultDestinationRequest.OpenSettings),
        )
        assertEquals(
            VaultNavigationResult.Navigated,
            navigation.navigate(VaultDestinationRequest.OpenDetail("entry-1")),
        )

        assertEquals(
            listOf(AppRoute.Settings.route, AppRoute.Detail.createRoute("entry-1")),
            routes,
        )
    }

    @Test
    fun `scanned wifi navigates with an opaque one time token`() {
        val routes = mutableListOf<String>()
        val navigation = navigation(singleTopRoutes = routes)

        assertEquals(
            VaultNavigationResult.Navigated,
            navigation.navigate(
                VaultDestinationRequest.EditScannedWifi(
                    WifiQrPayload(
                        "Office",
                        "secret-password",
                        WifiSecurityType.WpaPersonal,
                        false,
                    ),
                ),
            ),
        )
        assertEquals(1, routes.size)
        assertTrue(routes.single().startsWith("vault/add/wifi/from-seed/"))
        assertFalse(routes.single().contains("Office"))
        assertFalse(routes.single().contains("secret-password"))
    }

    private fun navigation(
        routes: MutableList<String> = mutableListOf(),
        singleTopRoutes: MutableList<String> = mutableListOf(),
    ) = VaultNavigation(
        navigateToRoute = routes::add,
        navigateToSingleTopRoute = singleTopRoutes::add,
        seedCoordinator =
            EntryEditorSeedCoordinator(
                sessionAccess = AuthenticatedSession(),
                store = EntryEditorSeedStore(),
                logger = RecordingTestLogger(),
            ),
    )

    private class AuthenticatedSession : SecureSessionAccessState {
        override val authenticationState =
            MutableStateFlow<AuthenticationState>(AuthenticationState.Authenticated(1L))

        override fun isUnlocked(): Boolean = true
    }
}
