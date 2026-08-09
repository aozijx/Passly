package com.aozijx.passly.feature.main.contract

sealed interface MainEffect {
    data class ShowToast(val message: String) : MainEffect
    data class ShowError(val error: String) : MainEffect
    data object LockedByTimeout : MainEffect
    data object NavigateToVault : MainEffect
    data object AuthSuccess : MainEffect
    data class AuthError(val message: String) : MainEffect
}
