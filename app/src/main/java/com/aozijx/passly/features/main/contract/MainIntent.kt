package com.aozijx.passly.features.main.contract

import android.content.Context
import android.net.Uri
import androidx.fragment.app.FragmentActivity

sealed interface MainIntent {
    data class Authenticate(
        val activity: FragmentActivity,
        val title: String,
        val subtitle: String = "",
        val onError: ((String) -> Unit)? = null,
        val onSuccess: () -> Unit
    ) : MainIntent

    data object Lock : MainIntent
    data object UpdateInteraction : MainIntent
    data object CheckAndLock : MainIntent
    data object RetryDatabaseInitialization : MainIntent
    data class ExportEmergencyBackup(val context: Context) : MainIntent

    /**
     * 明文导出：优先写入 [dirUri] 指定的 SAF 目录；
     * 若 [dirUri] 为空则通过 Effect 通知 UI 弹出文件选择器。
     */
    data class ExportPlainBackup(val context: Context, val dirUri: String? = null) : MainIntent

    /** 明文导出到已选定的 SAF URI（来自文件选择器或已解析的目录）。 */
    data class ExportPlainBackupToUri(val context: Context, val uri: Uri) : MainIntent
}
