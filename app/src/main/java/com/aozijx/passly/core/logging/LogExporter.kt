package com.aozijx.passly.core.logging

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.aozijx.passly.R
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 日志导出工具：
 * 提供日志导出和分享功能。
 */
object LogExporter {
    private const val ERROR_LOG_PREFIX = "[E]"
    private const val CRASH_LOG_PREFIX = "[CRASH]"

    private val exportDateFormatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    /**
     * 导出所有日志为 ZIP 文件
     * @param context 应用上下文
     * @return 导出的 ZIP 文件路径，如果导出失败则返回 null
     */
    fun exportAllLogsAsZip(context: Context): File? {
        try {
            Logcat.flushLogs()

            val logDir = Logcat.getLogFolder()
            if (logDir == null || !logDir.exists()) {
                return null
            }

            val logFiles = logDir.listFiles()?.sortedByDescending { it.lastModified() }
            if (logFiles == null || logFiles.isEmpty()) {
                return null
            }

            val timestamp = exportDateFormatter.format(Date())
            val zipFileName = "passly_logs_$timestamp.zip"

            val exportDir = File(context.cacheDir, "exported_logs")
            if (!exportDir.exists()) exportDir.mkdirs()

            val zipFile = File(exportDir, zipFileName)

            ZipOutputStream(zipFile.outputStream()).use { zipOut ->
                val metaInfo = StringBuilder()
                    .appendLine("=== Passly 日志导出 ===")
                    .appendLine(
                        "导出时间: ${
                            SimpleDateFormat(
                                "yyyy-MM-dd HH:mm:ss",
                                Locale.getDefault()
                            ).format(Date())
                        }"
                    )
                    .appendLine(
                        "应用版本: ${
                            context.packageManager.getPackageInfo(
                                context.packageName,
                                0
                            ).versionName
                        }"
                    )
                    .appendLine("日志文件数量: ${logFiles.size}")
                    .appendLine()

                val metaEntry = ZipEntry("README.txt")
                zipOut.putNextEntry(metaEntry)
                zipOut.write(metaInfo.toString().toByteArray())
                zipOut.closeEntry()

                for (logFile in logFiles) {
                    val entry = ZipEntry(logFile.name)
                    zipOut.putNextEntry(entry)
                    logFile.inputStream().use { input ->
                        input.copyTo(zipOut)
                    }
                    zipOut.closeEntry()
                }
            }

            return zipFile
        } catch (e: Exception) {
            android.util.Log.e("LogExporter", "导出所有日志失败", e)
            return null
        }
    }

    /**
     * 导出错误日志为文本文件
     * @param context 应用上下文
     * @return 导出的文件路径，如果导出失败则返回 null
     */
    fun exportErrorLogsAsTxt(context: Context): File? {
        try {
            Logcat.flushLogs()

            val logDir = Logcat.getLogFolder()
            if (logDir == null || !logDir.exists()) {
                return null
            }

            val logFiles = logDir.listFiles()?.sortedByDescending { it.lastModified() }
            if (logFiles == null || logFiles.isEmpty()) {
                return null
            }

            val timestamp = exportDateFormatter.format(Date())
            val exportFileName = "error_logs_$timestamp.txt"

            val exportDir = File(context.cacheDir, "exported_logs")
            if (!exportDir.exists()) exportDir.mkdirs()

            val exportFile = File(exportDir, exportFileName)

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

                var hasErrors = false

                for (logFile in logFiles) {
                    val fileName = logFile.name
                    var fileHasContent = false

                    try {
                        logFile.bufferedReader().use { reader ->
                            var line: String? = reader.readLine()
                            while (line != null) {
                                val isError = line.contains(ERROR_LOG_PREFIX)
                                val isCrash = line.contains(CRASH_LOG_PREFIX)

                                if (isError || isCrash) {
                                    if (!fileHasContent) {
                                        writer.write("--- 文件: $fileName ---")
                                        writer.newLine()
                                        fileHasContent = true
                                        hasErrors = true
                                    }
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

                    if (fileHasContent) {
                        writer.newLine()
                    }
                }

                if (!hasErrors) {
                    writer.write("暂无错误日志")
                    writer.newLine()
                }
            }

            return exportFile
        } catch (e: Exception) {
            android.util.Log.e("LogExporter", "导出错误日志失败", e)
            return null
        }
    }

    /**
     * 分享日志 ZIP 文件
     * @param context 应用上下文
     * @param zipFile ZIP 文件
     */
    fun shareLogsZip(context: Context, zipFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                zipFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
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
            android.util.Log.e("LogExporter", "分享 ZIP 文件失败", e)
        }
    }

    /**
     * 分享错误日志文件
     * @param context 应用上下文
     * @param logFile 日志文件
     */
    fun shareLogsFile(context: Context, logFile: File) {
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
            android.util.Log.e("LogExporter", "分享 TXT 文件失败", e)
        }
    }

    /**
     * 清理导出的日志文件
     */
    fun clearExportedLogs(context: Context) {
        try {
            val exportDir = File(context.cacheDir, "exported_logs")

            // 检查目录是否存在
            if (!exportDir.exists()) {
                android.util.Log.d("LogExporter", "导出目录不存在，无需清理")
                return
            }

            // 检查目录权限
            if (!exportDir.canWrite()) {
                android.util.Log.w("LogExporter", "导出目录无写权限: ${exportDir.absolutePath}")
                return
            }

            // 递归删除目录中的所有内容
            val deletedCount = deleteRecursivelyWithCount(exportDir)
            android.util.Log.d("LogExporter", "导出日志清理完成，删除了 $deletedCount 个文件/目录")

        } catch (e: Exception) {
            android.util.Log.e("LogExporter", "清理导出日志失败: ${e.message}", e)
        }
    }

    /**
     * 递归删除文件或目录，并返回删除的文件/目录数量
     */
    private fun deleteRecursivelyWithCount(file: File): Int {
        var count = 0

        if (file.isDirectory) {
            file.listFiles()?.forEach { child ->
                count += deleteRecursivelyWithCount(child)
            }
        }

        if (file.delete()) {
            count++
            android.util.Log.d("LogExporter", "已删除: ${file.name}")
        } else {
            android.util.Log.w("LogExporter", "删除失败: ${file.absolutePath}")
        }

        return count
    }
}