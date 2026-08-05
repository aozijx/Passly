package com.aozijx.passly.feature.main.contract

sealed interface MainIntent {
    data object Lock : MainIntent
    data object ExitRecovery : MainIntent
    data object UpdateInteraction : MainIntent
    data object RetryDatabaseInitialization : MainIntent
    data object RecoverDatabase : MainIntent
}
