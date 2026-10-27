package com.aozijx.passly.feature.shell.presentation

import com.aozijx.passly.presentation.shared.error.UiErrorMessage

sealed interface AppShellEffect {
    data class ShowError(val error: UiErrorMessage) : AppShellEffect
}