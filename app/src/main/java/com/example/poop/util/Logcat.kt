package com.example.poop.util

import android.util.Log
import com.example.poop.AppContext
import com.example.poop.BuildConfig
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/**
 * 增强型日志工具类
 * 支持分级打印、详细时间戳、本地日志持久化，便于后续导出分析
 */
object Logcat {
    private const val DEFAULT_TAG = "AppLog"
    private val isDebug = BuildConfig.DEBUG
    
    // 使用单线程池处理文件写入，避免阻塞主线程且保证写入顺序
    private val logExecutor = Executors.newSingleThreadExecutor()

    // 使用 Locale.US 确保日志格式在不同语言环境下保持一致，同时避免 Lint 警告
    private val fileDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val logTimeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    enum class Level(val prefix: String) {
        VERBOSE("V"),
        DEBUG("D"),
        INFO("I"),
        WARN("W"),
        ERROR("E")
    }

    fun v(tag: String = DEFAULT_TAG, msg: String) = log(Level.VERBOSE, tag, msg)
    fun d(tag: String = DEFAULT_TAG, msg: String) = log(Level.DEBUG, tag, msg)
    fun i(tag: String = DEFAULT_TAG, msg: String) = log(Level.INFO, tag, msg)
    fun w(tag: String = DEFAULT_TAG, msg: String, tr: Throwable? = null) = log(Level.WARN, tag, msg, tr)
    fun e(tag: String = DEFAULT_TAG, msg: String, tr: Throwable? = null) = log(Level.ERROR, tag, msg, tr)

    private fun log(level: Level, tag: String, msg: String, tr: Throwable? = null) {
        // 控制台打印
        if (isDebug || level.ordinal >= Level.INFO.ordinal) {
            when (level) {
                Level.VERBOSE -> Log.v(tag, msg)
                Level.DEBUG -> Log.d(tag, msg)
                Level.INFO -> Log.i(tag, msg)
                Level.WARN -> Log.w(tag, msg, tr)
                Level.ERROR -> Log.e(tag, msg, tr)
            }
        }

        // 只有 INFO 级别及以上才持久化到本地文件
        if (level.ordinal >= Level.INFO.ordinal) {
            saveToFile(level, tag, msg, tr)
        }
    }

    /**
     * 将日志信息保存到本地文件
     */
    private fun saveToFile(level: Level, tag: String, msg: String, tr: Throwable?) {
        logExecutor.execute {
            try {
                val logDir = AppContext.get().getExternalFilesDir("logs")
                if (logDir?.exists() == false) logDir.mkdirs()

                val now = Date()
                val fileName = "log_${fileDateFormatter.format(now)}.log"
                val logFile = File(logDir, fileName)
                val timestamp = logTimeFormatter.format(now)

                FileWriter(logFile, true).use { fw ->
                    PrintWriter(fw).use { pw ->
                        // 格式: [时间] [级别] [标签] 消息
                        pw.println("[$timestamp] [${level.prefix}] [$tag] $msg")
                        tr?.let {
                            pw.println("--- StackTrace ---")
                            it.printStackTrace(pw)
                            pw.println("------------------")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(DEFAULT_TAG, "Failed to write log to file", e)
            }
        }
    }

    /**
     * 获取日志目录，用于导出
     */
    fun getLogFolder(): File? {
        return AppContext.get().getExternalFilesDir("logs")
    }

    /**
     * 清理过期日志（例如保留最近7天）
     */
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
