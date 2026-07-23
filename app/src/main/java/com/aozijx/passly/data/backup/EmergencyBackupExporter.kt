package com.aozijx.passly.data.backup

import android.content.Context
import android.database.Cursor
import android.util.Base64
import android.util.JsonWriter
import com.aozijx.passly.BuildConfig
import com.aozijx.passly.core.diagnostics.AppLog
import com.aozijx.passly.core.error.AppError
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.core.error.ErrorLayer
import com.aozijx.passly.core.error.fromThrowable
import com.aozijx.passly.core.error.logFailureWithContext
import com.aozijx.passly.data.local.database.DatabaseSchema
import com.aozijx.passly.security.crypto.CryptoEngine
import net.zetetic.database.sqlcipher.SQLiteDatabase
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 紧急备份导出器：仅作为数据库损坏或迁移失败时的最后防线。
 * 它直接操作底层的 SQLCipher 数据库文件。
 */
object EmergencyBackupExporter {
    private const val TAG = "EmergencyBackup"
    private const val IV_LENGTH = 12
    private const val MAX_FILE_AGE_MS = 15 * 60 * 1000L
    private val MAGIC = "PSEM1".toByteArray(StandardCharsets.UTF_8)
    private val KDF_LABEL = "passly-emergency-backup-v1".toByteArray(StandardCharsets.UTF_8)

    /**
     * 当应用检测到数据库无法正常初始化时，尝试抢救数据。
     * 仅在 DEBUG 构建导出，并以密文文件保存到 app cache。
     */
    suspend fun exportOnFailure(
        context: Context,
        cryptoEngine: CryptoEngine
    ): AppResult<File> {
        if (!BuildConfig.DEBUG) {
            return AppResult.failure(
                AppError.fromThrowable(
                    IllegalStateException("Emergency backup export is disabled in release builds."),
                    layer = ErrorLayer.DATA,
                    operation = "emergencyBackup.export"
                )
            )
        }

        return try {
            deleteExpiredBackups(context.cacheDir)

            val dbFile = context.getDatabasePath(DatabaseSchema.DATABASE_NAME)
            if (!dbFile.exists()) return AppResult.failure(
                AppError.fromThrowable(
                    Exception("数据库文件不存在"),
                    layer = ErrorLayer.DATA,
                    operation = "emergencyBackup.export"
                )
            )

            cryptoEngine.withUnlockedDek { dek ->
                var db: SQLiteDatabase? = null
                var plainJson: ByteArray? = null
                var encryptedPayload: ByteArray? = null
                try {
                    db = SQLiteDatabase.openDatabase(
                        dbFile.path, dek, null, SQLiteDatabase.OPEN_READONLY, null, null
                    )

                    plainJson = writeEntriesToJsonBytes(db)
                    encryptedPayload = encryptPayload(plainJson, dek)

                    val backupFile = File(
                        context.cacheDir, "emergency_rescue_${System.currentTimeMillis()}.json.enc"
                    )

                    FileOutputStream(backupFile).use { output ->
                        output.write(MAGIC)
                        output.write(encryptedPayload)
                    }

                    AppLog.i(TAG, "紧急救灾备份已生成(密文): ${backupFile.absolutePath}")
                    AppResult.success(backupFile)
                } catch (e: Exception) {
                    AppResult.failure(
                        AppError.fromThrowable(
                            e,
                            layer = ErrorLayer.DATA,
                            operation = "emergencyBackup.export"
                        )
                    ).logFailureWithContext(TAG, "emergencyBackup.export")
                } finally {
                    plainJson?.fill(0)
                    encryptedPayload?.fill(0)
                    db?.close()
                }
            }
        } catch (e: Exception) {
            AppResult.failure(
                AppError.fromThrowable(
                    e,
                    layer = ErrorLayer.DATA,
                    operation = "emergencyBackup.export"
                )
            ).logFailureWithContext(TAG, "emergencyBackup.export")
        }
    }

    private fun writeEntriesToJsonBytes(db: SQLiteDatabase): ByteArray {
        val output = ByteArrayOutputStream()
        val writer = JsonWriter(OutputStreamWriter(output, StandardCharsets.UTF_8))
        writer.setIndent("  ")

        val cursor = db.rawQuery("SELECT * FROM ${DatabaseSchema.TABLE_ENTRIES}", null)
        writer.beginArray()
        cursor.use {
            while (cursor.moveToNext()) {
                writer.beginObject()
                val idIndex = cursor.getColumnIndex("entryId")
                if (idIndex >= 0) {
                    writer.name("entryId")
                    writer.value(cursor.getString(idIndex))
                }

                val blobIndex = cursor.getColumnIndex("summaryBlob")
                if (blobIndex >= 0) {
                    writer.name("summaryBlob")
                    when (cursor.getType(blobIndex)) {
                        Cursor.FIELD_TYPE_NULL -> writer.nullValue()
                        Cursor.FIELD_TYPE_STRING -> writer.value(cursor.getString(blobIndex))
                        Cursor.FIELD_TYPE_BLOB -> {
                            val blob = cursor.getBlob(blobIndex)
                            writer.value(Base64.encodeToString(blob, Base64.NO_WRAP))
                        }

                        else -> writer.value(cursor.getString(blobIndex))
                    }
                }
                writer.endObject()
            }
        }
        writer.endArray()
        writer.close()
        return output.toByteArray()
    }

    private fun deleteExpiredBackups(cacheDir: File) {
        val now = System.currentTimeMillis()
        cacheDir.listFiles { file ->
            file.name.startsWith("emergency_rescue_") && file.name.endsWith(".json.enc")
        }?.forEach { file ->
            if (now - file.lastModified() > MAX_FILE_AGE_MS) {
                runCatching { file.delete() }
            }
        }
    }

    private fun encryptPayload(plain: ByteArray, dek: ByteArray): ByteArray {
        val iv = ByteArray(IV_LENGTH).also { SecureRandom().nextBytes(it) }

        val keyMaterial = MessageDigest.getInstance("SHA-256").digest(dek + KDF_LABEL)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")

        return try {
            cipher.init(
                Cipher.ENCRYPT_MODE,
                SecretKeySpec(keyMaterial, "AES"),
                GCMParameterSpec(128, iv)
            )
            val encrypted = cipher.doFinal(plain)
            iv + encrypted
        } finally {
            keyMaterial.fill(0)
        }
    }
}
