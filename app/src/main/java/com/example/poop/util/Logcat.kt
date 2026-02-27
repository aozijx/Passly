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

object Logcat {
    private const val TAG = "AppLog"
    private val isDebug = BuildConfig.DEBUG

    fun d(tag: String = TAG, msg: String) {
        if (isDebug) Log.d(tag, msg)
    }

    fun e(tag: String = TAG, msg: String, tr: Throwable? = null) {
        Log.e(tag, msg, tr)
        // 如果有异常，写入文件
        if (tr != null) {
            saveErrorToLocal(msg, tr)
        }
    }

    /**
     * 将错误日志保存到本地文件
     */
    private fun saveErrorToLocal(msg: String, tr: Throwable) {
        try {
            // 获取应用外部私有目录: /storage/emulated/0/Android/data/com.example.poop/files/logs
            // 这样不需要申请额外权限，且卸载后会自动删除
            val logDir = AppContext.get().getExternalFilesDir("logs")
            if (logDir?.exists() == false) logDir.mkdirs()

            val fileName =
                "error_${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}.log"
            val logFile = File(logDir, fileName)

            val timestamp =
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())

            FileWriter(logFile, true).use { fw ->
                PrintWriter(fw).use { pw ->
                    pw.println("\n--- Error at $timestamp ---")
                    pw.println("Message: $msg")
                    tr.printStackTrace(pw)
                    pw.println("----------------------------------")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save log to file", e)
        }
    }
}