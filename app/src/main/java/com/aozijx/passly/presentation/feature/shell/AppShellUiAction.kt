package com.aozijx.passly.presentation.feature.shell

sealed interface AppShellUiAction {
    data object Lock : AppShellUiAction
    data object ExitRecovery : AppShellUiAction
    data object UpdateInteraction : AppShellUiAction
    data object RetryDatabaseSession : AppShellUiAction
}