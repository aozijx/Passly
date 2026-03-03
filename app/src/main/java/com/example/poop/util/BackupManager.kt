package com.example.poop.util

import android.content.Context
import android.net.Uri
import android.security.keystore.UserNotAuthenticatedException
import androidx.room.withTransaction
import com.example.poop.data.AppDatabase
import com.example.poop.data.VaultItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * 备份管理类：支持将数据库内容导出为加密文件，并支持从加密文件恢复。
 * 版本 2：支持 Autofill、TOTP 及元数据全量备份。
 */
object BackupManager {
    private const val TAG = "BackupManager"
    // 备份文件格式版本号
    private const val BACKUP_VERSION = 2
    private const val PBKDF2_ITERATIONS = 200000
    private const val KEY_LENGTH = 256
    private const val SALT_LENGTH = 16
    private const val IV_LENGTH = 12

    enum class ImportMode {
        APPEND,    // 追加模式
        OVERWRITE  // 覆盖模式
    }

    /**
     * 导出加密备份
     */
    suspend fun exportBackup(
        context: Context,
        uri: Uri,
        password: CharArray
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(context)
            val items = db.vaultDao().getAllItems().first()
            // 摘要信息始终记录，方便线上排查统计
            Logcat.i(TAG, "Export task started. Total items to process: ${items.size}")
            
            val jsonArray = JSONArray()
            items.forEach { item ->
                // 1. 解密敏感字段
                val username = decryptField(item.username, "账号")
                val passwordText = decryptField(item.password, "密码")
                val totpSecretText = item.totpSecret?.let { decryptField(it, "TOTP密钥") }

                // 2. 构造全量 JSON 对象
                val obj = JSONObject().apply {
                    put("title", item.title)
                    put("username", username)
                    put("password", passwordText)
                    put("category", item.category)
                    put("notes", item.notes ?: JSONObject.NULL)

                    // 图标
                    put("iconName", item.iconName ?: JSONObject.NULL)
                    put("iconCustomPath", item.iconCustomPath ?: JSONObject.NULL)

                    // TOTP
                    put("totpSecret", totpSecretText ?: JSONObject.NULL)
                    put("totpPeriod", item.totpPeriod)
                    put("totpDigits", item.totpDigits)
                    put("totpAlgorithm", item.totpAlgorithm)

                    // Autofill
                    put("associatedAppPackage", item.associatedAppPackage ?: JSONObject.NULL)
                    put("associatedDomain", item.associatedDomain ?: JSONObject.NULL)
                    put("uriList", item.uriList ?: JSONObject.NULL)
                    put("matchType", item.matchType)
                    put("customFieldsJson", item.customFieldsJson ?: JSONObject.NULL)
                    put("autoSubmit", item.autoSubmit)

                    // 安全与统计
                    put("strengthScore", item.strengthScore?.toDouble() ?: JSONObject.NULL)
                    put("lastUsedAt", item.lastUsedAt ?: JSONObject.NULL)
                    put("usageCount", item.usageCount)

                    // 元数据
                    put("favorite", item.favorite)
                    put("tags", item.tags ?: JSONObject.NULL)
                    put("createdAt", item.createdAt ?: JSONObject.NULL)
                    put("updatedAt", item.updatedAt ?: JSONObject.NULL)
                    put("expiresAt", item.expiresAt ?: JSONObject.NULL)
                }
                jsonArray.put(obj)
            }

            val rawData = jsonArray.toString().toByteArray()

            // 3. 使用备份密码加密
            val salt = ByteArray(SALT_LENGTH).apply { SecureRandom().nextBytes(this) }
            val secretKey = deriveKey(password, salt)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encryptedData = cipher.doFinal(rawData)

            // 4. 写入文件
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(BACKUP_VERSION)
                output.write(salt)
                output.write(iv)
                output.write(encryptedData)
            } ?: return@withContext Result.failure(Exception("无法保存到指定路径"))

            Logcat.i(TAG, "Export completed successfully.")
            Result.success(Unit)
        } catch (e: Exception) {
            // 错误日志始终记录（去除 if DEBUG）
            Logcat.e(TAG, "Export failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun decryptField(encryptedText: String, label: String): String {
        return try {
            val iv = CryptoManager.getIvFromCipherText(encryptedText)
            iv?.let { CryptoManager.getDecryptCipher(it) }
                ?.let { CryptoManager.decrypt(encryptedText, it) }
                ?: throw Exception("解密 $label 失败")
        } catch (e: UserNotAuthenticatedException) {
            Logcat.e(TAG, "Biometric authentication timeout while decrypting $label", e)
            throw e
        }
    }

    /**
     * 导入并恢复备份
     */
    suspend fun importBackup(
        context: Context,
        uri: Uri,
        password: CharArray,
        mode: ImportMode = ImportMode.OVERWRITE
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Logcat.i(TAG, "Import task started. Mode: $mode")
            val db = AppDatabase.getDatabase(context)
            val dao = db.vaultDao()

            val itemsToImport = getBackupData(context, uri, password).getOrThrow()
            Logcat.i(TAG, "Backup decrypted successfully. Found ${itemsToImport.size} entries.")

            // 重新加密所有敏感字段并导入
            val encryptedItems = try {
                itemsToImport.map { item ->
                    val cipherUser = CryptoManager.getEncryptCipher() ?: throw Exception("加密引擎初始化失败")
                    val encUser = CryptoManager.encrypt(item.username, cipherUser)
                    
                    val cipherPass = CryptoManager.getEncryptCipher() ?: throw Exception("加密引擎初始化失败")
                    val encPass = CryptoManager.encrypt(item.password, cipherPass)
                    
                    val encTotp = item.totpSecret?.let {
                        val cipherTotp = CryptoManager.getEncryptCipher() ?: throw Exception("加密引擎初始化失败")
                        CryptoManager.encrypt(it, cipherTotp)
                    }
                    
                    item.copy(username = encUser, password = encPass, totpSecret = encTotp)
                }
            } catch (e: UserNotAuthenticatedException) {
                Logcat.e(TAG, "Import interrupted by biometric timeout", e)
                throw Exception("导入超时：授权已过期，请重试。")
            }

            db.withTransaction {
                if (mode == ImportMode.OVERWRITE) {
                    Logcat.i(TAG, "Overwrite mode: executing database cleanup.")
                    dao.deleteAll()
                }
                dao.insertAll(encryptedItems)
            }

            Logcat.i(TAG, "Import finished. ${encryptedItems.size} items written to vault.")
            Result.success(Unit)
        } catch (e: Exception) {
            Logcat.e(TAG, "Import failed: ${e.message}", e)
            Result.failure(Exception(e.message ?: "导入失败"))
        }
    }

    /**
     * 读取备份并解析 JSON
     */
    suspend fun getBackupData(
        context: Context,
        uri: Uri,
        password: CharArray
    ): Result<List<VaultItem>> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val versionBuf = ByteArray(1)
                if (readExactly(input, versionBuf) != 1) throw Exception("无法识别备份文件格式")
                val version = versionBuf[0].toInt() and 0xFF
                
                if (version > BACKUP_VERSION) throw Exception("不支持的备份文件版本 ($version)")

                val salt = ByteArray(SALT_LENGTH)
                if (readExactly(input, salt) != SALT_LENGTH) throw Exception("备份文件 Salt 数据块损坏")
                val iv = ByteArray(IV_LENGTH)
                if (readExactly(input, iv) != IV_LENGTH) throw Exception("备份文件 IV 数据块损坏")
                
                val encryptedData = input.readBytes()
                val secretKey = deriveKey(password, salt)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))

                val rawData = cipher.doFinal(encryptedData)
                val jsonArray = JSONArray(String(rawData))

                val items = mutableListOf<VaultItem>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    items.add(VaultItem(
                        title = obj.getString("title"),
                        username = obj.getString("username"),
                        password = obj.getString("password"),
                        category = obj.getString("category"),
                        notes = obj.optNullableString("notes"),
                        iconName = obj.optNullableString("iconName"),
                        iconCustomPath = obj.optNullableString("iconCustomPath"),
                        totpSecret = obj.optNullableString("totpSecret"),
                        totpPeriod = obj.optInt("totpPeriod", 30),
                        totpDigits = obj.optInt("totpDigits", 6),
                        totpAlgorithm = obj.optString("totpAlgorithm", "SHA1"),
                        associatedAppPackage = obj.optNullableString("associatedAppPackage"),
                        associatedDomain = obj.optNullableString("associatedDomain"),
                        uriList = obj.optNullableString("uriList"),
                        matchType = obj.optInt("matchType", 0),
                        customFieldsJson = obj.optNullableString("customFieldsJson"),
                        autoSubmit = obj.optBoolean("autoSubmit", false),
                        strengthScore = if (obj.isNull("strengthScore")) null else obj.getDouble("strengthScore").toFloat(),
                        lastUsedAt = if (obj.isNull("lastUsedAt") || obj.optLong("lastUsedAt", 0L) == 0L) null else obj.getLong("lastUsedAt"),
                        usageCount = obj.optInt("usageCount", 0),
                        favorite = obj.optBoolean("favorite", false),
                        tags = obj.optNullableString("tags"),
                        createdAt = if (obj.isNull("createdAt") || obj.optLong("createdAt", 0L) == 0L) System.currentTimeMillis() else obj.getLong("createdAt"),
                        updatedAt = if (obj.isNull("updatedAt") || obj.optLong("updatedAt", 0L) == 0L) null else obj.getLong("updatedAt"),
                        expiresAt = if (obj.isNull("expiresAt") || obj.optLong("expiresAt", 0L) == 0L) null else obj.getLong("expiresAt")
                    ))
                }
                Result.success(items)
            } ?: Result.failure(Exception("无法读取文件流"))
        } catch (e: Exception) {
            val message = when (e) {
                is AEADBadTagException -> "备份密码错误"
                else -> e.message ?: "解析失败"
            }
            Logcat.e(TAG, "Backup parsing error: $message", e)
            Result.failure(Exception(message))
        }
    }

    private fun JSONObject.optNullableString(name: String): String? {
        return if (isNull(name)) null else optString(name)
    }

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_LENGTH)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun readExactly(input: InputStream, buffer: ByteArray): Int {
        var total = 0
        while (total < buffer.size) {
            val read = input.read(buffer, total, buffer.size - total)
            if (read == -1) break
            total += read
        }
        return total
    }
}
