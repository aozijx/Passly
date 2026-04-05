package com.aozijx.passly.core.logging

import android.security.keystore.UserNotAuthenticatedException
import android.util.Log
import com.aozijx.passly.AppContext
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/**
 * 增强型日志工具类
 * 修复了异步写入可能导致的数据丢失问题
 */
object Logcat {
    private const val DEFAULT_TAG = "AppLog"

    // 使用单线程池处理文件写入，保证顺序且不阻塞主线程
    private val logExecutor = Executors.newSingleThreadExecutor()

    private val fileDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val logTimeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    enum class Level(val prefix: String) {
        VERBOSE("V"), DEBUG("D"), INFO("I"), WARN("W"), ERROR("E")
    }

    fun v(tag: String = DEFAULT_TAG, msg: String) = log(Level.VERBOSE, tag, msg)
    fun d(tag: String = DEFAULT_TAG, msg: String) = log(Level.DEBUG, tag, msg)
    fun i(tag: String = DEFAULT_TAG, msg: String) = log(Level.INFO, tag, msg)
    fun w(tag: String = DEFAULT_TAG, msg: String, tr: Throwable? = null) = log(Level.WARN, tag, msg, tr)
    fun e(tag: String = DEFAULT_TAG, msg: String, tr: Throwable? = null) = log(Level.ERROR, tag, msg, tr)

    /**
     * 专门处理 Crypto 相关异常，避免堆栈污染日志文件
     */
    fun cryptoError(tag: String, action: String, e: Exception) {
        if (e is UserNotAuthenticatedException) {
            w(tag, "$action: User not authenticated (Key is locked)")
        } else {
            e(tag, "$action failed", e)
        }
    }

    private fun log(level: Level, tag: String, msg: String, tr: Throwable? = null) {
        if (level.ordinal >= Level.INFO.ordinal) {
            when (level) {
                Level.VERBOSE -> Log.v(tag, msg)
                Level.DEBUG -> Log.d(tag, msg)
                Level.INFO -> Log.i(tag, msg)
                Level.WARN -> Log.w(tag, msg, tr)
                Level.ERROR -> Log.e(tag, msg, tr)
            }
        }

        if (level.ordinal >= Level.INFO.ordinal) {
            try {
                AppContext.get()
                saveToFile(level, tag, msg, tr)
            } catch (e: Exception) {
                Log.w(DEFAULT_TAG, "AppContext not ready, skipping file log: $msg")
            }
        }
    }

    private fun saveToFile(level: Level, tag: String, msg: String, tr: Throwable?) {
        logExecutor.execute {
            var writer: PrintWriter? = null
            try {
                val context = AppContext.get()
                val logDir = context.getExternalFilesDir("logs") ?: return@execute
                if (!logDir.exists()) logDir.mkdirs()

                val now = Date()
                val fileName = "log_${fileDateFormatter.format(now)}.log"
                val logFile = File(logDir, fileName)
                val timestamp = logTimeFormatter.format(now)

                // 使用 BufferedWriter 提升性能，并显式设置 autoFlush = true
                writer = PrintWriter(BufferedWriter(FileWriter(logFile, true)), true)
                writer.println("[$timestamp] [${level.prefix}] [$tag] $msg")
                tr?.let {
                    writer.println("--- StackTrace ---")
                    it.printStackTrace(writer)
                    writer.println("------------------")
                }
                // 确保数据立即刷入磁盘
                writer.flush()
            } catch (e: Exception) {
                Log.e(DEFAULT_TAG, "Failed to write log to file", e)
            } finally {
                try { writer?.close() } catch (_: Exception) {}
            }
        }
    }

    fun getLogFolder(): File? {
        return try {
            AppContext.get().getExternalFilesDir("logs")
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 强行同步所有未写入的日志（用于崩溃处理或手动导出前）
     */
    fun flushLogs() {
        // 由于是单线程池，提交一个空任务并等待执行完毕即可实现类似 flush 的效果
        logExecutor.submit { }.get()
    }

    fun clearOldLogs(daysToKeep: Int = 7) {
        logExecutor.execute {
            try {
                val logDir = getLogFolder() ?: return@execute
                val files = logDir.listFiles() ?: return@execute
                val now = System.currentTimeMillis()
                val keepInterval = daysToKeep * 24 * 60 * 60 * 1000L

                files.forEach { file ->
                    if (now - file.lastModified() > keepInterval) {
                        file.delete()
                    }
                }
            } catch (e: Exception) {
                Log.e(DEFAULT_TAG, "Failed to clear old logs", e)
            }
        }
    }
}



