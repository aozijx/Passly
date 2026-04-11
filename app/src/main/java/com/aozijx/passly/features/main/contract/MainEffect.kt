package com.aozijx.passly.features.main.contract

sealed interface MainEffect {
    data class ShowToast(val message: String) : MainEffect
    data class ShowError(val error: String) : MainEffect
    data object LockedByTimeout : MainEffect
    data object NavigateToVault : MainEffect
}
