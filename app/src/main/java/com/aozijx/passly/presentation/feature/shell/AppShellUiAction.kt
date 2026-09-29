package com.aozijx.passly.presentation.feature.shell

sealed interface AppShellUiAction {
    data object ExitRecovery : AppShellUiAction
    data object RetryDatabaseSession : AppShellUiAction
}
