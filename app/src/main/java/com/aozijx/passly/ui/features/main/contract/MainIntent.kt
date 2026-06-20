package com.aozijx.passly.ui.features.main.contract

import android.content.Context

sealed interface MainIntent {
    data object Lock : MainIntent
    data object UpdateInteraction : MainIntent
    data object CheckAndLock : MainIntent
    data object RetryDatabaseInitialization : MainIntent
    data class ExportEmergencyBackup(val context: Context) : MainIntent
}