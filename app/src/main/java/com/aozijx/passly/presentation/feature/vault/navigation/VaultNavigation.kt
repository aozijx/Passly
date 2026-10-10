package com.aozijx.passly.presentation.feature.vault.navigation

import com.aozijx.passly.presentation.feature.shell.navigation.AppRoute
import com.aozijx.passly.presentation.feature.vault.list.ui.model.VaultAddTypeUiModel

internal sealed interface VaultDestinationRequest {
    data object OpenSettings : VaultDestinationRequest
    data class OpenDetail(val entryId: String) : VaultDestinationRequest
    data class OpenEditor(val type: VaultAddTypeUiModel) : VaultDestinationRequest
}

internal class VaultNavigation(
    private val navigateToRoute: (String) -> Unit,
    private val navigateToSingleTopRoute: (String) -> Unit,
) {
    fun navigate(request: VaultDestinationRequest): Boolean {
        when (request) {
            VaultDestinationRequest.OpenSettings -> navigateToRoute(AppRoute.Settings.route)
            is VaultDestinationRequest.OpenDetail -> {
                navigateToRoute(AppRoute.Detail.createRoute(request.entryId))
            }
            is VaultDestinationRequest.OpenEditor -> {
                val route = VaultEditorDestinationRegistry.routeFor(request.type) ?: return false
                navigateToSingleTopRoute(route)
            }
        }
        return true
    }
}
