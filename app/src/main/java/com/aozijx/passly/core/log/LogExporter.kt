package com.aozijx.passly.core.log

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.aozijx.passly.BuildConfig
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.core.error.ErrorLayer
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 日志导出工具
 * 提供导出全部日志为 ZIP、导出错误日志为 TXT，以及分享/清理等功能。
 * 所有方法需要传入 Context，不依赖 Application 单例。
 */
object LogExporter {

    private const val LOG_PREFIX = "log_"
    private const val EXPORT_ZIP_PREFIX = "passly_logs_"
    private const val EXPORT_TXT_PREFIX = "passly_errors_"
    private const val DATE_FORMAT_PATTERN = "yyyyMMdd_HHmmss"
    private val dateFormatter by lazy { SimpleDateFormat(DATE_FORMAT_PATTERN, Locale.US) }

    /**
     * 导出所有日志文件为 ZIP 包
     * @param context 上下文（用于获取缓存目录）
     * @return 生成的 ZIP 文件，失败返回 AppResult.Failure
     */
    fun exportAllLogsAsZip(context: Context): AppResult<File> {
        return AppResult.runCatching("exportAllLogsAsZip", ErrorLayer.DATA) {
            Logcat.flushLogs()

            val logDir = Logcat.getLogFolder().getOrThrow()
            val logFiles =
                logDir.listFiles()?.filter { it.isFile && it.name.startsWith(LOG_PREFIX) }
            if (logFiles.isNullOrEmpty()) {
                Logcat.w("LogExporter", "No log files to export")
                throw IllegalStateException("No log files to export")
            }

            val timestamp = dateFormatter.format(Date())
            val zipFile = File(context.cacheDir, "${EXPORT_ZIP_PREFIX}${timestamp}.zip")

            ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                // 写入 README 信息
                val readmeEntry = ZipEntry("README.txt")
                zos.putNextEntry(readmeEntry)
                val readmeContent = buildReadme(context, logFiles.size)
                zos.write(readmeContent.toByteArray())
                zos.closeEntry()

                // 写入每个日志文件
                logFiles.forEach { file ->
                    zos.putNextEntry(ZipEntry(file.name))
                    FileInputStream(file).use { input ->
                        input.copyTo(zos)
                    }
                    zos.closeEntry()
                }
            }
            Logcat.i("LogExporter", "Exported ${logFiles.size} logs to ${zipFile.name}")
            zipFile
        }.onFailure {
            val timestamp = dateFormatter.format(Date())
            val zipFile = File(context.cacheDir, "${EXPORT_ZIP_PREFIX}${timestamp}.zip")
            if (zipFile.exists()) zipFile.delete()
        }
    }

    /**
     * 导出错误日志（仅包含级别为 ERROR 的日志行）为 TXT 文件
     * @param context 上下文
     * @return 生成的 TXT 文件，失败返回 AppResult.Failure
     */
    fun exportErrorLogsAsTxt(context: Context): AppResult<File> {
        return AppResult.runCatching("exportErrorLogsAsTxt", ErrorLayer.DATA) {
            Logcat.flushLogs()

            val logDir = Logcat.getLogFolder().getOrThrow()
            val logFiles =
                logDir.listFiles()?.filter { it.isFile && it.name.startsWith(LOG_PREFIX) }
                    ?.sortedByDescending { it.name }
            if (logFiles.isNullOrEmpty()) {
                Logcat.w("LogExporter", "No log files to export errors")
                throw IllegalStateException("No log files to export errors")
            }

            val timestamp = dateFormatter.format(Date())
            val txtFile = File(context.cacheDir, "${EXPORT_TXT_PREFIX}${timestamp}.txt")

            PrintWriter(FileOutputStream(txtFile)).use { writer ->
                // 写入文件头
                writer.println("=== Passly Error Logs Export ===")
                writer.println(
                    "Export time: ${
                        SimpleDateFormat(
                            "yyyy-MM-dd HH:mm:ss",
                            Locale.getDefault()
                        ).format(Date())
                    }"
                )
                writer.println("App version: ${BuildConfig.VERSION_NAME}")
                writer.println("Total log files scanned: ${logFiles.size}")
                writer.println()

                var hasErrors = false
                logFiles.forEach { file ->
                    var fileHasContent = false
                    file.forEachLine { line ->
                        // 更精确的错误行检测：以 "[E]" 开头且可能包含时间戳
                        if (line.contains(" [E] ")) {
                            if (!fileHasContent) {
                                writer.println("--- File: ${file.name} ---")
                                fileHasContent = true
                                hasErrors = true
                            }
                            writer.println(line)
                        }
                    }
                    if (fileHasContent) writer.println()
                }

                if (!hasErrors) {
                    writer.println("No error logs found.")
                }
            }
            Logcat.i("LogExporter", "Exported error logs to ${txtFile.name}")
            txtFile
        }.onFailure {
            val timestamp = dateFormatter.format(Date())
            val txtFile = File(context.cacheDir, "${EXPORT_TXT_PREFIX}${timestamp}.txt")
            if (txtFile.exists()) txtFile.delete()
        }
    }

    /**
     * 分享日志 ZIP 文件
     * @param context 上下文
     * @param zipFile 要分享的 ZIP 文件
     */
    fun shareLogsZip(context: Context, zipFile: File) {
        shareFile(context, zipFile, "application/zip")
    }

    /**
     * 分享日志 TXT 文件
     * @param context 上下文
     * @param txtFile 要分享的 TXT 文件
     */
    fun shareLogsTxt(context: Context, txtFile: File) {
        shareFile(context, txtFile, "text/plain")
    }

    /**
     * 通用文件分享方法（使用 FileProvider）
     */
    private fun shareFile(context: Context, file: File, mimeType: String) {
        if (!file.exists()) {
            Logcat.w("LogExporter", "File does not exist: ${file.absolutePath}")
            return
        }

        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "分享日志文件")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            Logcat.d("LogExporter", "Sharing file: ${file.name}")
        } catch (e: Exception) {
            Logcat.e("LogExporter", "Failed to share file: ${file.name}", e)
        }
    }

    /**
     * 清理所有由本工具导出的临时文件（位于 cacheDir 下以指定前缀开头的文件）
     * @param context 上下文
     */
    fun clearExportedLogs(context: Context) {
        try {
            val cacheDir = context.cacheDir
            val files = cacheDir.listFiles() ?: return
            var deletedCount = 0
            files.forEach { file ->
                if (file.isFile && (file.name.startsWith(EXPORT_ZIP_PREFIX) || file.name.startsWith(
                        EXPORT_TXT_PREFIX
                    ))
                ) {
                    if (file.delete()) deletedCount++
                }
            }
            Logcat.i("LogExporter", "Cleared $deletedCount exported log files")
        } catch (e: Exception) {
            Logcat.e("LogExporter", "Failed to clear exported logs", e)
        }
    }

    /**
     * 构建 README 内容
     */
    private fun buildReadme(context: Context, fileCount: Int): String {
        val version = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: Exception) {
            "unknown"
        }
        return buildString {
            appendLine("=== Passly Logs Export ===")
            appendLine(
                "Export time: ${
                    SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss",
                        Locale.getDefault()
                    ).format(Date())
                }"
            )
            appendLine("App version: $version")
            appendLine("Total log files: $fileCount")
            appendLine("Log file format: [timestamp] [LEVEL] [tag] message")
            appendLine("Levels: V(VERBOSE), D(DEBUG), I(INFO), W(WARN), E(ERROR)")
            appendLine("This ZIP contains all log files from the app's internal storage.")
        }
    }
}