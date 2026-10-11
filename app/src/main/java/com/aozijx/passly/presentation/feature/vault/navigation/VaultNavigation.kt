package com.aozijx.passly.presentation.feature.vault.navigation

import com.aozijx.passly.presentation.feature.shell.navigation.AppRoute
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultAddTypeUiModel
import com.aozijx.passly.feature.vault.entry.EntryEditorSeed
import com.aozijx.passly.feature.vault.wifi.WifiQrPayload

internal sealed interface VaultDestinationRequest {
    data object OpenSettings : VaultDestinationRequest
    data class OpenDetail(val entryId: String) : VaultDestinationRequest
    data class OpenEditor(val type: VaultAddTypeUiModel) : VaultDestinationRequest
    data class EditScannedWifi(val payload: WifiQrPayload) : VaultDestinationRequest
}

internal sealed interface VaultNavigationResult {
    data object Navigated : VaultNavigationResult
    data object UnsupportedDestination : VaultNavigationResult
    data object AccessDenied : VaultNavigationResult
}

internal class VaultNavigation(
    private val navigateToRoute: (String) -> Unit,
    private val navigateToSingleTopRoute: (String) -> Unit,
    private val seedStager: EntryEditorSeedStager,
) {
    fun navigate(request: VaultDestinationRequest): VaultNavigationResult {
        when (request) {
            VaultDestinationRequest.OpenSettings -> navigateToRoute(AppRoute.Settings.route)
            is VaultDestinationRequest.OpenDetail -> {
                navigateToRoute(AppRoute.Detail.createRoute(request.entryId))
            }
            is VaultDestinationRequest.OpenEditor -> {
                val route = VaultEditorDestinationRegistry.routeFor(request.type)
                    ?: return VaultNavigationResult.UnsupportedDestination
                navigateToSingleTopRoute(route)
            }
            is VaultDestinationRequest.EditScannedWifi -> {
                when (
                    val staged = seedStager.stage(EntryEditorSeed.Wifi(request.payload))
                ) {
                    is EntryEditorSeedStageResult.Staged -> navigateToSingleTopRoute(
                        AppRoute.AddWifiFromSeed.createRoute(staged.token.value),
                    )
                    EntryEditorSeedStageResult.AccessDenied ->
                        return VaultNavigationResult.AccessDenied
                }
            }
        }
        return VaultNavigationResult.Navigated
    }
}
