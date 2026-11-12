package com.aozijx.passly.feature.vault.presentation.navigation

import com.aozijx.passly.app.navigation.AppRoute
import com.aozijx.passly.domain.entry.policy.WifiQrPayload
import com.aozijx.passly.feature.vault.entry.EntryEditorSeed
import com.aozijx.passly.feature.vault.model.AddType

internal sealed interface VaultDestinationRequest {
    data object OpenSettings : VaultDestinationRequest

    data class OpenDetail(
        val entryId: String,
    ) : VaultDestinationRequest

    data class OpenEditor(
        val type: AddType,
    ) : VaultDestinationRequest

    data class EditScannedWifi(
        val payload: WifiQrPayload,
    ) : VaultDestinationRequest
}

internal sealed interface VaultNavigationResult {
    data object Navigated : VaultNavigationResult

    data object UnsupportedDestination : VaultNavigationResult

    data object AccessDenied : VaultNavigationResult

    data class NavigationFailed(
        val failure: Exception,
    ) : VaultNavigationResult
}

internal class VaultNavigation(
    private val navigateToRoute: (String) -> Unit,
    private val navigateToSingleTopRoute: (String) -> Unit,
    private val seedCoordinator: EntryEditorSeedCoordinator,
) {
    fun navigate(request: VaultDestinationRequest): VaultNavigationResult {
        when (request) {
            VaultDestinationRequest.OpenSettings -> {
                navigateToRoute(AppRoute.Settings.route)
            }

            is VaultDestinationRequest.OpenDetail -> {
                navigateToRoute(AppRoute.Detail.createRoute(request.entryId))
            }

            is VaultDestinationRequest.OpenEditor -> {
                val route =
                    VaultEditorDestinationRegistry.routeFor(request.type)
                        ?: return VaultNavigationResult.UnsupportedDestination
                navigateToSingleTopRoute(route)
            }

            is VaultDestinationRequest.EditScannedWifi -> {
                return when (
                    val result =
                        seedCoordinator.navigateWithSeed(
                            seed = EntryEditorSeed.Wifi(request.payload),
                            routeForToken = { token -> AppRoute.AddWifiFromSeed.createRoute(token.value) },
                            navigate = navigateToSingleTopRoute,
                        )
                ) {
                    EntryEditorSeedNavigationResult.Navigated -> {
                        VaultNavigationResult.Navigated
                    }

                    EntryEditorSeedNavigationResult.AccessDenied -> {
                        VaultNavigationResult.AccessDenied
                    }

                    EntryEditorSeedNavigationResult.UnsupportedDestination -> {
                        VaultNavigationResult.UnsupportedDestination
                    }

                    is EntryEditorSeedNavigationResult.NavigationFailed -> {
                        VaultNavigationResult.NavigationFailed(result.failure)
                    }
                }
            }
        }
        return VaultNavigationResult.Navigated
    }
}
