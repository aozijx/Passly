package com.example.poop.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle

/**
 * 剪贴板工具类，提供安全复制和自动清除功能
 */
object ClipboardUtils {
    private const val CLIP_LABEL = "vault_data"
    private const val CLEAR_DELAY_MS = 60000L // 60秒自动清除

    private val handler = Handler(Looper.getMainLooper())
    private var lastClearRunnable: Runnable? = null

    /**
     * 安全复制到剪贴板
     * @param context 上下文
     * @param text 要复制的文本
     * @param isSensitive 是否为敏感内容（密码、账号等）
     */
    fun copy(context: Context, text: String, isSensitive: Boolean = true) {
        val appContext = context.applicationContext
        val clipboard = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        
        val clipData = ClipData.newPlainText(CLIP_LABEL, text).apply {
            if (isSensitive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                description.extras = PersistableBundle().apply {
                    putBoolean("android.content.extra.IS_SENSITIVE", true)
                }
            }
        }
        clipboard.setPrimaryClip(clipData)

        // 取消之前的清除任务，确保新的任务重新计时
        lastClearRunnable?.let { handler.removeCallbacks(it) }

        val clearRunnable = Runnable {
            try {
                // 仅当剪贴板内容仍是我们本次复制的内容时才清除
                if (clipboard.hasPrimaryClip() && clipboard.primaryClipDescription?.label == CLIP_LABEL) {
                    clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
                }
            } catch (e: Exception) {
                Logcat.e("ClipboardUtils", "Failed to auto-clear clipboard", e)
            }
        }

        lastClearRunnable = clearRunnable
        handler.postDelayed(clearRunnable, CLEAR_DELAY_MS)
    }
}
