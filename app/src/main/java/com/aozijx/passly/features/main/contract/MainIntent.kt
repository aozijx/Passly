package com.aozijx.passly.features.main.contract

import android.content.Context
import androidx.fragment.app.FragmentActivity

sealed interface MainIntent {
    data class Authenticate(
        val activity: FragmentActivity,
        val title: String,
        val subtitle: String = "",
        val onError: ((String) -> Unit)? = null,
        val onSuccess: () -> Unit
    ) : MainIntent

    data object Authorize : MainIntent
    data object Lock : MainIntent
    data object UpdateInteraction : MainIntent
    data object CheckAndLock : MainIntent
    data object RetryDatabaseInitialization : MainIntent
    data class ExportEmergencyBackup(val context: Context) : MainIntent
    data class ExportPlainBackup(val context: Context) : MainIntent
}
