package com.aozijx.passly.core.logging

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.aozijx.passly.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 日志导出工具：
 * 提供错误日志提取、导出和分享功能。
 */
object LogExporter {
    private const val ERROR_LOG_PREFIX = "[E]"
    private const val CRASH_LOG_PREFIX = "[CRASH]"

    private val exportDateFormatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    /**
     * 导出错误日志
     * @param context 应用上下文
     * @param includeCrashLogs 是否包含崩溃日志
     * @return 导出的文件路径，如果导出失败则返回 null
     */
    fun exportErrorLogs(context: Context, includeCrashLogs: Boolean = true): File? {
        try {
            Logcat.flushLogs()

            val logDir = Logcat.getLogFolder()
            if (logDir == null || !logDir.exists()) {
                return null
            }

            val timestamp = exportDateFormatter.format(Date())
            val exportFileName = if (includeCrashLogs) {
                "error_logs_with_crash_$timestamp.txt"
            } else {
                "error_logs_$timestamp.txt"
            }

            val exportDir = File(context.cacheDir, "exported_logs")
            if (!exportDir.exists()) exportDir.mkdirs()

            val exportFile = File(exportDir, exportFileName)

            val logFiles = logDir.listFiles()
                ?.sortedByDescending { it.lastModified() }
                ?: return null

            exportFile.bufferedWriter().use { writer ->
                writer.write("=== Passly 错误日志导出 ===")
                writer.newLine()
                writer.write(
                    "导出时间: ${
                        SimpleDateFormat(
                            "yyyy-MM-dd HH:mm:ss",
                            Locale.getDefault()
                        ).format(Date())
                    }"
                )
                writer.newLine()
                writer.write(
                    "应用版本: ${
                        context.packageManager.getPackageInfo(
                            context.packageName,
                            0
                        ).versionName
                    }"
                )
                writer.newLine()
                writer.newLine()
                writer.write("=== 错误日志内容 ===")
                writer.newLine()
                writer.newLine()

                for (logFile in logFiles) {
                    val fileName = logFile.name
                    writer.write("--- 文件: $fileName ---")
                    writer.newLine()

                    try {
                        logFile.bufferedReader().use { reader ->
                            var line: String? = reader.readLine()
                            while (line != null) {
                                // 筛选错误级别和崩溃日志
                                val isError = line.contains(ERROR_LOG_PREFIX)
                                val isCrash = line.contains(CRASH_LOG_PREFIX)

                                if (isError || (includeCrashLogs && isCrash)) {
                                    writer.write(line)
                                    writer.newLine()
                                }
                                line = reader.readLine()
                            }
                        }
                    } catch (e: Exception) {
                        writer.write("读取文件失败: ${e.message}")
                        writer.newLine()
                    }

                    writer.newLine()
                }
            }

            return exportFile
        } catch (e: Exception) {
            android.util.Log.e("LogExporter", "导出日志失败", e)
            return null
        }
    }

    /**
     * 分享错误日志
     * @param context 应用上下文
     * @param logFile 日志文件
     */
    fun shareErrorLogs(context: Context, logFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                logFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.log_export_subject))
                putExtra(Intent.EXTRA_TEXT, context.getString(R.string.log_export_message))
            }

            val chooserIntent =
                Intent.createChooser(shareIntent, context.getString(R.string.log_export_title))
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooserIntent)
        } catch (e: Exception) {
            android.util.Log.e("LogExporter", "分享日志失败", e)
        }
    }

    /**
     * 获取错误日志统计信息
     * @return 错误日志数量和崩溃日志数量
     */
    fun getErrorLogStats(): Pair<Int, Int> {
        try {
            Logcat.flushLogs()

            val logDir = Logcat.getLogFolder()
            if (logDir == null || !logDir.exists()) {
                return Pair(0, 0)
            }

            var errorCount = 0
            var crashCount = 0

            logDir.listFiles()?.forEach { logFile ->
                try {
                    logFile.bufferedReader().use { reader ->
                        var line: String? = reader.readLine()
                        while (line != null) {
                            if (line.contains(ERROR_LOG_PREFIX)) errorCount++
                            if (line.contains(CRASH_LOG_PREFIX)) crashCount++
                            line = reader.readLine()
                        }
                    }
                } catch (e: Exception) {
                    // 忽略读取错误
                }
            }

            return Pair(errorCount, crashCount)
        } catch (e: Exception) {
            return Pair(0, 0)
        }
    }

    /**
     * 清理导出的日志文件
     */
    fun clearExportedLogs(context: Context) {
        try {
            val exportDir = File(context.cacheDir, "exported_logs")
            if (exportDir.exists()) {
                exportDir.listFiles()?.forEach { it.delete() }
            }
        } catch (e: Exception) {
            android.util.Log.e("LogExporter", "清理导出日志失败", e)
        }
    }
}