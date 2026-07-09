package com.aozijx.passly.core.log

import android.annotation.SuppressLint
import android.content.Context
import android.security.keystore.UserNotAuthenticatedException
import android.util.Log
import com.aozijx.passly.BuildConfig
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.core.error.ErrorLayer
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class Logcat(
    private val context: Context,
    private val filter: LogFilter = NoOpFilter()
) {

    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: Logcat

        fun init(logcat: Logcat) {
            instance = logcat
        }

        fun v(tag: String = "AppLog", msg: String) = instance.v(tag, msg)
        fun d(tag: String = "AppLog", msg: String) = instance.d(tag, msg)
        fun i(tag: String = "AppLog", msg: String) = instance.i(tag, msg)
        fun w(tag: String = "AppLog", msg: String, tr: Throwable? = null) = instance.w(tag, msg, tr)
        fun e(tag: String = "AppLog", msg: String, tr: Throwable? = null) = instance.e(tag, msg, tr)
        fun logCryptoException(tag: String, action: String, e: Exception) =
            instance.logCryptoException(tag, action, e)

        fun getLogFolder(): AppResult<File> = instance.getLogFolder()
        fun flushLogs() = instance.flushLogs()
        fun clearOldLogs(daysToKeep: Int = 7) = instance.clearOldLogs(daysToKeep)
        fun readAllLogs(): AppResult<String> = instance.readAllLogs()
        fun clearAllLogs() = instance.clearAllLogs()
        fun shutdown() = instance.shutdown()
    }

    private val logExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val fileDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val logTimeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    private val fileSinkThreshold: Level =
        if (BuildConfig.DEBUG) Level.INFO else Level.WARN

    enum class Level(val prefix: String) {
        VERBOSE("V"), DEBUG("D"), INFO("I"), WARN("W"), ERROR("E")
    }

    fun v(tag: String = "AppLog", msg: String) = log(Level.VERBOSE, tag, msg)
    fun d(tag: String = "AppLog", msg: String) = log(Level.DEBUG, tag, msg)
    fun i(tag: String = "AppLog", msg: String) = log(Level.INFO, tag, msg)
    fun w(tag: String = "AppLog", msg: String, tr: Throwable? = null) =
        log(Level.WARN, tag, msg, tr)

    fun e(tag: String = "AppLog", msg: String, tr: Throwable? = null) =
        log(Level.ERROR, tag, msg, tr)

    fun logCryptoException(tag: String, action: String, e: Exception) {
        if (e is UserNotAuthenticatedException) {
            w(tag, "$action: User not authenticated (Key is locked)")
        } else {
            e(tag, "$action failed", e)
        }
    }

    fun getLogFolder(): AppResult<File> {
        return AppResult.runCatching("getLogFolder", ErrorLayer.DATA) {
            File(context.filesDir, "logs").apply { if (!exists()) mkdirs() }
        }
    }

    fun flushLogs() {
        logExecutor.submit { }.get()
    }

    fun clearOldLogs(daysToKeep: Int = 7) {
        logExecutor.execute {
            try {
                val logDir = getLogFolder().getOrNull() ?: return@execute
                val files = logDir.listFiles() ?: return@execute
                val now = System.currentTimeMillis()
                val keepInterval = daysToKeep * 24 * 60 * 60 * 1000L

                files.forEach { file ->
                    if (now - file.lastModified() > keepInterval) {
                        file.delete()
                    }
                }
            } catch (e: Exception) {
                Log.e("AppLog", "Failed to clear old logs", e)
            }
        }
    }

    fun readAllLogs(): AppResult<String> {
        flushLogs()
        return AppResult.runCatching("readAllLogs", ErrorLayer.DATA) {
            val logDir = getLogFolder().getOrThrow()
            val files =
                logDir.listFiles()?.sortedByDescending { it.name }?.toTypedArray() ?: emptyArray()
            val builder = StringBuilder()
            files.forEach { file ->
                runCatching { file.readText() }
                    .onSuccess { builder.append(it).append("\n") }
                    .onFailure { Log.w("AppLog", "Failed to read log file: ${file.name}") }
            }
            builder.toString()
        }
    }

    fun clearAllLogs() {
        logExecutor.execute {
            try {
                val logDir = getLogFolder().getOrNull() ?: return@execute
                val files = logDir.listFiles() ?: return@execute
                files.forEach { it.delete() }
            } catch (e: Exception) {
                Log.e("AppLog", "Failed to clear logs", e)
            }
        }
    }

    fun shutdown() {
        logExecutor.shutdown()
        try {
            if (!logExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                logExecutor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            Log.w("AppLog", "Log executor shutdown interrupted", e)
            logExecutor.shutdownNow()
            Thread.currentThread().interrupt()
        }
    }

    private fun log(level: Level, tag: String, msg: String, tr: Throwable? = null) {
        when (level) {
            Level.VERBOSE -> Log.v(tag, msg)
            Level.DEBUG -> Log.d(tag, msg)
            Level.INFO -> Log.i(tag, msg)
            Level.WARN -> Log.w(tag, msg, tr)
            Level.ERROR -> Log.e(tag, msg, tr)
        }

        if (level.ordinal >= fileSinkThreshold.ordinal) {
            val filteredMsg = filter.filter(msg)
            val filteredTr = tr?.let { filter.filterThrowable(it) }
            saveToFile(level, tag, filteredMsg, filteredTr)
        }
    }

    private fun saveToFile(level: Level, tag: String, msg: String, tr: Throwable?) {
        logExecutor.execute {
            val logDir = File(context.filesDir, "logs")
            if (!logDir.exists()) logDir.mkdirs()

            val now = Date()
            val fileName = "log_${fileDateFormatter.format(now)}.log"
            val logFile = File(logDir, fileName)
            val timestamp = logTimeFormatter.format(now)

            PrintWriter(BufferedWriter(FileWriter(logFile, true))).use { writer ->
                writer.println("[$timestamp] [${level.prefix}] [$tag] $msg")
                tr?.let {
                    writer.println("--- StackTrace ---")
                    it.printStackTrace(writer)
                    writer.println("------------------")
                }
                writer.flush()
            }
        }
    }
}