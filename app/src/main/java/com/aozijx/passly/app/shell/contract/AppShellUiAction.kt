package com.aozijx.passly.app.shell.contract

import com.aozijx.passly.domain.access.model.SensitiveAccessAction
import com.aozijx.passly.app.security.SensitiveAccessLevel

sealed interface AppShellUiAction {
    data object Lock : AppShellUiAction
    data object ExitRecovery : AppShellUiAction
    data object UpdateInteraction : AppShellUiAction
    data object RetryDatabaseInitialization : AppShellUiAction
    data object RecoverDatabase : AppShellUiAction
    data object RequestAuth : AppShellUiAction
    data object RequestReauth : AppShellUiAction
    data class RequestSensitiveAccess(
        val action: SensitiveAccessAction,
        val accessLevel: SensitiveAccessLevel
    ) : AppShellUiAction
}
