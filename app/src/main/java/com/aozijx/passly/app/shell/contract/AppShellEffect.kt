package com.aozijx.passly.app.shell.contract

sealed interface AppShellEffect {
    data class ShowToast(val message: String) : AppShellEffect
    data class ShowError(val error: String) : AppShellEffect
    data object LockedByTimeout : AppShellEffect
    data object NavigateToVault : AppShellEffect
}

sealed interface AppShellAuthResult {
    data object Success : AppShellAuthResult
    data object NotAuthorized : AppShellAuthResult
}
