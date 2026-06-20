package com.aozijx.passly.ui.features.main.contract

import android.content.Context
import android.net.Uri

sealed interface MainIntent {

    data object Lock : MainIntent
    data object UpdateInteraction : MainIntent
    data object CheckAndLock : MainIntent
    data object RetryDatabaseInitialization : MainIntent
    data class ExportEmergencyBackup(val context: Context) : MainIntent

    /** 明文导出到已选定的 SAF URI（来自文件选择器或已解析的目录）。 */
    data class ExportPlainBackupToUri(val context: Context, val uri: Uri) : MainIntent
}