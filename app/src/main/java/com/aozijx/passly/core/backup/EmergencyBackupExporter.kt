package com.aozijx.passly.core.backup

import android.content.Context
import android.database.Cursor
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
 * 紧急备份导出器：仅作为数据库损坏或迁移失败时的最后防线。
 * 它直接操作底层的 SQLCipher 数据库文件。
 */
object EmergencyBackupExporter {

    private val BOOLEAN_COLUMNS = setOf("wifiIsHidden", "autoSubmit", "favorite")

    /**
     * 当应用检测到数据库无法正常初始化时，尝试抢救数据。
     * 将数据以明文 JSON 形式保存到 App 私有目录（Cache 目录）中。
     */
    fun exportOnFailure(context: Context): Result<File> {
        var db: SQLiteDatabase? = null
        return try {
            val dbFile = context.getDatabasePath(DatabaseConfig.DATABASE_NAME)
            if (!dbFile.exists()) return Result.failure(Exception("数据库文件不存在"))

            val passphrase = DatabasePassphraseManager.getPassphrase(context)
            db = SQLiteDatabase.openDatabase(
                dbFile.path, passphrase, null, SQLiteDatabase.OPEN_READONLY, null, null
            )

            // 将文件保存在 cache 目录下，避免暴露在 Downloads 这种公开区域
            val backupFile = File(
                context.cacheDir, "emergency_rescue_${System.currentTimeMillis()}.json"
            )

            FileOutputStream(backupFile).use { output ->
                val writer = JsonWriter(OutputStreamWriter(output, StandardCharsets.UTF_8))
                writer.setIndent("  ")
                writeEntriesToJson(db, writer)
            }

            Logcat.i("EmergencyBackup", "紧急救灾备份已生成: ${backupFile.absolutePath}")
            Result.success(backupFile)
        } catch (e: Exception) {
            Logcat.e("EmergencyBackup", "紧急救灾备份失败", e)
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
                    // 图片二进制数据不建议放入明文 JSON
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