package com.aozijx.passly.presentation.feature.vault.list

sealed interface VaultRouteRequest {
    data object OpenScanner : VaultRouteRequest
    data object OpenSettings : VaultRouteRequest
}
