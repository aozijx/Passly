package com.aozijx.passly.core.backup

import android.content.Context
import android.database.Cursor
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
 * 紧急备份导出：当 Room 迁移失败时，使用原始 SQL 强制提取数据。
 */
object EmergencyBackupExporter {

    // 定义需要从整数转换为布尔值的列名
    private val BOOLEAN_COLUMNS = setOf("wifiIsHidden", "autoSubmit", "favorite")

    fun exportOnFailure(context: Context): Result<File> {
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
            val backupFile = File(
                downloadsDir, "Passly_Emergency_Backup_${System.currentTimeMillis()}.json"
            )

            val output = FileOutputStream(backupFile)
            val writer = JsonWriter(OutputStreamWriter(output, StandardCharsets.UTF_8))
            writer.setIndent("  ")

            val cursor = db.rawQuery("SELECT * FROM ${DatabaseConfig.TABLE_ENTRIES}", null)
            writer.beginArray()

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
                            // 关键修复：如果是布尔列，则写入 true/false 而不是 0/1
                            if (BOOLEAN_COLUMNS.contains(columnName)) {
                                writer.value(value == 1L)
                            } else {
                                writer.value(value)
                            }
                        }

                        Cursor.FIELD_TYPE_FLOAT -> writer.value(cursor.getDouble(columnIndex))
                        Cursor.FIELD_TYPE_STRING -> writer.value(cursor.getString(columnIndex))
                        Cursor.FIELD_TYPE_BLOB -> writer.value("[BINARY DATA]")
                    }
                }
                writer.endObject()
            }
            writer.endArray()
            writer.close()

            Logcat.i("EmergencyBackup", "紧急导出成功: ${backupFile.absolutePath}")
            Result.success(backupFile)
        } catch (e: Exception) {
            Logcat.e("EmergencyBackup", "紧急导出失败", e)
            Result.failure(e)
        } finally {
            db?.close()
        }
    }
}
