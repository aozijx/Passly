package com.aozijx.passly.feature.shell.presentation

sealed interface AppShellEffect {
    data class ShowError(val error: String) : AppShellEffect
}