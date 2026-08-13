package com.aozijx.passly.app.shell.contract

sealed interface AppShellEffect {
    data class ShowToast(val message: String) : AppShellEffect
    data class ShowError(val error: String) : AppShellEffect
    data object LockedByTimeout : AppShellEffect
    data object NavigateToVault : AppShellEffect
    data object AuthSuccess : AppShellEffect
    data class AuthError(val message: String) : AppShellEffect
}
