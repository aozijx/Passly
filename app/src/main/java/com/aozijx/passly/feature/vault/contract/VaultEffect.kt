package com.aozijx.passly.feature.vault.contract

sealed interface VaultEffect {
    data class ShowToast(val message: String) : VaultEffect
    data class ShowError(val message: String) : VaultEffect
}
