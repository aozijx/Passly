package com.aozijx.passly.feature.vault.presentation.list

sealed interface VaultRouteRequest {
    data object OpenScanner : VaultRouteRequest
    data object OpenSettings : VaultRouteRequest
}
