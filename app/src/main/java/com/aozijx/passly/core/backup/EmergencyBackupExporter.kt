package com.aozijx.passly.core.backup

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.util.JsonWriter
import com.aozijx.passly.core.logging.Logcat
import com.aozijx.passly.core.security.DatabasePassphraseManager
import com.aozijx.passly.data.local.config.DatabaseConfig
import net.zetetic.database.sqlcipher.SQLiteDatabase
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

/**
 * 明文 JSON 导出：支持数据库迁移失败时的紧急导出，也支持顶部菜单的手动明文导出。
 */
object EmergencyBackupExporter {

    // 定义需要从整数转换为布尔值的列名
    private val BOOLEAN_COLUMNS = setOf("wifiIsHidden", "autoSubmit", "favorite")

    fun exportOnFailure(context: Context): Result<File> {
        return exportPlainJson(context, "Passly_Emergency_Backup")
    }

    /** 旧路径：写入系统 Downloads 目录（仅保留供紧急备份使用）。 */
    fun exportPlainBackup(context: Context): Result<File> {
        return exportPlainJson(context, "Passly_Plain_Backup")
    }

    /** 新路径：通过 SAF URI 写入用户指定位置。 */
    fun exportPlainBackupToUri(context: Context, uri: Uri): Result<Unit> {
        var db: SQLiteDatabase? = null
        return try {
            val dbFile = context.getDatabasePath(DatabaseConfig.DATABASE_NAME)
            if (!dbFile.exists()) return Result.failure(Exception("数据库文件不存在"))

            val passphrase = DatabasePassphraseManager.getPassphrase(context)
            db = SQLiteDatabase.openDatabase(
                dbFile.path, passphrase, null, SQLiteDatabase.OPEN_READONLY, null, null
            )

            val output = context.contentResolver.openOutputStream(uri)
                ?: return Result.failure(Exception("无法打开输出流"))

            output.use {
                val writer = JsonWriter(OutputStreamWriter(it, StandardCharsets.UTF_8))
                writer.setIndent("  ")
                writeEntriesToJson(db, writer)
            }

            Logcat.i("EmergencyBackup", "明文导出成功: $uri")
            Result.success(Unit)
        } catch (e: Exception) {
            Logcat.e("EmergencyBackup", "明文导出失败", e)
            Result.failure(e)
        } finally {
            db?.close()
        }
    }

    private fun exportPlainJson(context: Context, fileNamePrefix: String): Result<File> {
        var db: SQLiteDatabase? = null
        return try {
            val dbFile = context.getDatabasePath(DatabaseConfig.DATABASE_NAME)
            if (!dbFile.exists()) return Result.failure(Exception("数据库文件不存在"))

            val passphrase = DatabasePassphraseManager.getPassphrase(context)
            db = SQLiteDatabase.openDatabase(
                dbFile.path, passphrase, null, SQLiteDatabase.OPEN_READONLY, null, null
            )

            val downloadsDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val backupFile = File(
                downloadsDir, "${fileNamePrefix}_${System.currentTimeMillis()}.json"
            )

            FileOutputStream(backupFile).use { output ->
                val writer = JsonWriter(OutputStreamWriter(output, StandardCharsets.UTF_8))
                writer.setIndent("  ")
                writeEntriesToJson(db, writer)
            }

            Logcat.i("EmergencyBackup", "紧急导出成功: ${backupFile.absolutePath}")
            Result.success(backupFile)
        } catch (e: Exception) {
            Logcat.e("EmergencyBackup", "紧急导出失败", e)
            Result.failure(e)
        } finally {
            db?.close()
        }
    }

    private fun writeEntriesToJson(db: SQLiteDatabase, writer: JsonWriter) {
        val cursor = db.rawQuery("SELECT * FROM ${DatabaseConfig.TABLE_ENTRIES}", null)
        writer.beginArray()
        cursor.use {
            val columnNames = cursor.columnNames
            while (cursor.moveToNext()) {
                writer.beginObject()
                for (columnName in columnNames) {
                    if (columnName == "encryptedImageData") continue
                    val columnIndex = cursor.getColumnIndex(columnName)
                    writer.name(columnName)
                    when (cursor.getType(columnIndex)) {
                        Cursor.FIELD_TYPE_NULL -> writer.nullValue()
                        Cursor.FIELD_TYPE_INTEGER -> {
                            val value = cursor.getLong(columnIndex)
                            if (BOOLEAN_COLUMNS.contains(columnName)) writer.value(value == 1L)
                            else writer.value(value)
                        }
                        Cursor.FIELD_TYPE_FLOAT -> writer.value(cursor.getDouble(columnIndex))
                        Cursor.FIELD_TYPE_STRING -> writer.value(cursor.getString(columnIndex))
                        Cursor.FIELD_TYPE_BLOB -> writer.value("[BINARY DATA]")
                    }
                }
                writer.endObject()
            }
        }
        writer.endArray()
        writer.close()
    }
}
