package com.aozijx.passly.feature.shell.presentation

sealed interface AppShellUiAction {
    data object ExitRecovery : AppShellUiAction
    data object RetryDatabaseSession : AppShellUiAction
}
