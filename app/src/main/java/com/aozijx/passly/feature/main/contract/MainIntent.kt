package com.aozijx.passly.feature.main.contract

import android.content.Context

sealed interface MainIntent {
    data object Lock : MainIntent
    data object UpdateInteraction : MainIntent
    data object RetryDatabaseInitialization : MainIntent
    data class ExportEmergencyBackup(val context: Context) : MainIntent
}