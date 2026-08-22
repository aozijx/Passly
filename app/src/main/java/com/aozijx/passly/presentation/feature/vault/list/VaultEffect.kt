package com.aozijx.passly.presentation.feature.vault.list

sealed interface VaultEffect {
    data class ShowToast(val message: String) : VaultEffect
    data class ShowError(val message: String) : VaultEffect
}
