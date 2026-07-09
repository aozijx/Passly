package com.aozijx.passly.core.log

import android.annotation.SuppressLint
import android.os.Build
import android.os.Process
import com.aozijx.passly.BuildConfig
import java.io.PrintWriter
import java.io.StringWriter

object CrashHandler : Thread.UncaughtExceptionHandler {
    private const val CRASH_TAG = "CRASH"
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null

    @SuppressLint("StaticFieldLeak")
    private var logcat: Logcat? = null  // 可空，防止未初始化时崩溃

    fun init(logcat: Logcat) {
        this.logcat = logcat
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread, ex: Throwable) {
        try {
            saveCrashLog(thread, ex)
        } catch (e: Exception) {
            android.util.Log.e(CRASH_TAG, "保存崩溃日志失败", e)
        }

        defaultHandler?.uncaughtException(thread, ex) ?: Process.killProcess(Process.myPid())
    }

    private fun saveCrashLog(thread: Thread, ex: Throwable) {
        val log = logcat ?: return  // 未初始化则静默退出
        // 获取完整堆栈字符串
        val stackTrace = StringWriter().apply {
            ex.printStackTrace(PrintWriter(this))
        }.toString()

        val crashLog = buildString {
            appendLine("=== 应用崩溃 ===")
            appendLine("应用版本: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("设备型号: ${Build.MODEL} (${Build.MANUFACTURER})")
            appendLine("Android 版本: ${Build.VERSION.SDK_INT}")
            appendLine("崩溃线程: ${thread.name}")
            appendLine("异常类型: ${ex::class.java.simpleName}")
            appendLine("异常消息: ${ex.message ?: "(null)"}")
            appendLine("--- StackTrace ---")
            // 直接将堆栈写入 builder
            appendLine(stackTrace)
            appendLine("------------------")
        }

        log.e(CRASH_TAG, crashLog)
        log.flushLogs()   // 确保崩溃前落盘
    }
}