package com.aozijx.passly.core.logging

import android.content.Context
import android.os.Process
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 崩溃自动保存处理器：
 * 捕获应用崩溃，自动保存崩溃日志到文件系统。
 */
object CrashHandler : Thread.UncaughtExceptionHandler {
    private const val CRASH_TAG = "CRASH"

    private var defaultHandler: Thread.UncaughtExceptionHandler? = null
    private var appContext: Context? = null

    private val crashDateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    /**
     * 初始化崩溃处理器
     * @param context 应用上下文
     */
    fun init(context: Context) {
        appContext = context.applicationContext
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread, ex: Throwable) {
        try {
            saveCrashLog(thread, ex)
        } catch (e: Exception) {
            android.util.Log.e(CRASH_TAG, "保存崩溃日志失败", e)
        }

        // 调用原有的异常处理器
        defaultHandler?.uncaughtException(thread, ex) ?: Process.killProcess(Process.myPid())
    }

    /**
     * 保存崩溃日志
     * @param thread 发生崩溃的线程
     * @param ex 崩溃异常
     */
    private fun saveCrashLog(thread: Thread, ex: Throwable) {
        val context = appContext ?: return

        val timestamp = crashDateFormatter.format(Date())
        val threadName = thread.name

        val stackTraceWriter = StringWriter()
        val printWriter = PrintWriter(stackTraceWriter)
        ex.printStackTrace(printWriter)
        val stackTrace = stackTraceWriter.toString()

        val crashLog = StringBuilder()
            .appendLine("[$timestamp] [$CRASH_TAG] [CrashHandler] === 应用崩溃 ===")
            .appendLine("[$timestamp] [$CRASH_TAG] [CrashHandler] 崩溃线程: $threadName")
            .appendLine("[$timestamp] [$CRASH_TAG] [CrashHandler] 崩溃时间: $timestamp")
            .appendLine("[$timestamp] [$CRASH_TAG] [CrashHandler] 异常类型: ${ex::class.java.simpleName}")
            .appendLine("[$timestamp] [$CRASH_TAG] [CrashHandler] 异常消息: ${ex.message}")
            .appendLine("--- StackTrace ---")
            .appendLine(stackTrace)
            .appendLine("------------------")
            .toString()

        Logcat.e(CRASH_TAG, crashLog)

        // 强制同步日志，确保崩溃日志已写入磁盘
        Logcat.flushLogs()
    }
}