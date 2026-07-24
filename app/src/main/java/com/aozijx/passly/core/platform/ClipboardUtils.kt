package com.aozijx.passly.core.platform

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import com.aozijx.passly.core.diagnostics.AppLog
import com.aozijx.passly.domain.notice.model.NoticeCode
import com.aozijx.passly.domain.notice.model.newAppNotice
import com.aozijx.passly.domain.notice.port.AppNoticePublisher

/**
 * 剪贴板工具类，提供安全复制、自动清除以及内容获取功能
 */
object ClipboardUtils {
    private const val CLIP_LABEL = "vault_data"
    private const val CLEAR_DELAY_MS = 60000L // 60秒自动清除

    private val handler = Handler(Looper.getMainLooper())
    private val clearLock = Any()

    @Volatile
    private var lastClearRunnable: Runnable? = null

    /**
     * 安全复制到剪贴板
     */
    fun copy(
        context: Context,
        text: String,
        isSensitive: Boolean = true,
        noticePublisher: AppNoticePublisher? = null
    ) {
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

        synchronized(clearLock) {
            lastClearRunnable?.let { handler.removeCallbacks(it) }
        }

        val clearRunnable = Runnable {
            try {
                if (clipboard.hasPrimaryClip() && clipboard.primaryClipDescription?.label == CLIP_LABEL) {
                    clear(appContext, noticePublisher)
                }
            } catch (e: Exception) {
                AppLog.e("ClipboardUtils", "Failed to auto-clear clipboard", e)
            }
        }

        synchronized(clearLock) {
            lastClearRunnable = clearRunnable
        }
        handler.postDelayed(clearRunnable, CLEAR_DELAY_MS)
    }

    /**
     * 清除剪贴板内容
     */
    fun clear(context: Context, noticePublisher: AppNoticePublisher? = null) {
        try {
            val clipboard = context.applicationContext
                .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            if (clipboard.hasPrimaryClip()) {
                clipboard.clearPrimaryClip()
                noticePublisher?.publish(newAppNotice(NoticeCode.CLIPBOARD_CLEARED))
            }
        } catch (e: Exception) {
            AppLog.e("ClipboardUtils", "Clear clipboard failed", e)
            noticePublisher?.publish(newAppNotice(NoticeCode.CLIPBOARD_CLEAR_FAILED))
        }
    }

    fun cancelPendingAutoClear() {
        synchronized(clearLock) {
            lastClearRunnable?.let { handler.removeCallbacks(it) }
            lastClearRunnable = null
        }
    }

    /**
     * 获取剪贴板中的第一条文本内容
     */
    fun getText(context: Context): String {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        if (!clipboard.hasPrimaryClip()) return ""
        val item = clipboard.primaryClip?.getItemAt(0)
        return item?.text?.toString() ?: ""
    }
}
