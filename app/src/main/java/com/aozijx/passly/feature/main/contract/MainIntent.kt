package com.aozijx.passly.feature.main.contract

import com.aozijx.passly.domain.authentication.SensitiveAccessAction
import com.aozijx.passly.domain.authentication.SensitiveAccessLevel

sealed interface MainIntent {
    data object Lock : MainIntent
    data object ExitRecovery : MainIntent
    data object UpdateInteraction : MainIntent
    data object RetryDatabaseInitialization : MainIntent
    data object RecoverDatabase : MainIntent
    data object RequestAuth : MainIntent
    data object RequestReauth : MainIntent
    data class RequestSensitiveAccess(
        val action: SensitiveAccessAction,
        val accessLevel: SensitiveAccessLevel
    ) : MainIntent
}
