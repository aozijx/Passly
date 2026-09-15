package com.aozijx.passly.presentation.feature.shell

sealed interface AppShellEffect {
    data class ShowError(val error: String) : AppShellEffect
}