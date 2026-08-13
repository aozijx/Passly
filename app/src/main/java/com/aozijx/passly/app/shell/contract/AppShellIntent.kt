package com.aozijx.passly.app.shell.contract

import com.aozijx.passly.domain.authentication.SensitiveAccessAction
import com.aozijx.passly.domain.authentication.SensitiveAccessLevel

sealed interface AppShellIntent {
    data object Lock : AppShellIntent
    data object ExitRecovery : AppShellIntent
    data object UpdateInteraction : AppShellIntent
    data object RetryDatabaseInitialization : AppShellIntent
    data object RecoverDatabase : AppShellIntent
    data object RequestAuth : AppShellIntent
    data object RequestReauth : AppShellIntent
    data class RequestSensitiveAccess(
        val action: SensitiveAccessAction,
        val accessLevel: SensitiveAccessLevel
    ) : AppShellIntent
}
