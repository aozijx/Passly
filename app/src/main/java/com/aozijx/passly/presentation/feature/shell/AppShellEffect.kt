package com.aozijx.passly.presentation.feature.shell

sealed interface AppShellEffect {
    data class ShowError(val error: String) : AppShellEffect
}

sealed interface AppShellAuthResult {
    data object Success : AppShellAuthResult
    data object NotAuthorized : AppShellAuthResult
}
