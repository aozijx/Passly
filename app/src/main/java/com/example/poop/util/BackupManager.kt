package com.example.poop.util

import android.content.Context
import android.net.Uri
import android.security.keystore.UserNotAuthenticatedException
import androidx.room.withTransaction
import com.example.poop.BuildConfig
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
    
    // 备份文件格式版本号：升级到 2 以支持扩展字段
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
            if (BuildConfig.DEBUG) Logcat.i(TAG, "Starting export. Total items: ${items.size}")
            
            val jsonArray = JSONArray()
            items.forEach { item ->
                // 1. 解密敏感字段
                val username = decryptField(item.username, "账号")
                val passwordText = decryptField(item.password, "密码")
                val totpSecretText = item.totpSecret?.let { decryptField(it, "TOTP密钥") }

                // 2. 构造全量 JSON 对象 (使用 JSONObject.NULL 保持 JSON 语义严谨)
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

            if (BuildConfig.DEBUG) Logcat.i(TAG, "Export completed successfully. File: $uri")
            Result.success(Unit)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Logcat.e(TAG, "Export failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * 安全解密单个加密字段
     * @param encryptedText 加密后的字符串（包含 IV + 数据 + tag）
     * @param label 用于日志和用户提示的字段名称（如 "用户名"、"密码"）
     * @return 解密后的明文字符串
     * @throws Exception 解密失败时抛出，包含用户友好提示 + 详细原因已记录日志
     */
    private fun decryptField(encryptedText: String, label: String): String {
        try {
            val iv = CryptoManager.getIvFromCipherText(encryptedText)
                ?: throw Exception("无法提取 IV，$label 数据格式无效")

            val cipher = CryptoManager.getDecryptCipher(iv)
                ?: throw Exception("无法初始化解密器，$label 密钥可能已失效")

            val decrypted = CryptoManager.decrypt(encryptedText, cipher)
                ?: throw Exception("解密 $label 失败，可能数据已损坏或密码错误")

            return decrypted
        } catch (e: UserNotAuthenticatedException) {
            Logcat.w(TAG, "Biometric auth expired during $label decryption", e)
            throw Exception("授权已过期，请重新验证身份后重试")
        } catch (e: Exception) {
            // 所有其他异常统一记录详细日志
            Logcat.e(TAG, "Failed to decrypt $label: ${e.message}", e)
            // 根据异常类型给出更具体的用户提示（可选进一步细分）
            val userMessage = when {
                e.message?.contains("AEADBadTag") == true -> "解密 $label 失败，可能备份密码错误"
                else -> "解密 $label 时发生错误，请稍后重试"
            }
            throw Exception(userMessage)
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
            if (BuildConfig.DEBUG) Logcat.i(TAG, "Starting import from $uri. Mode: $mode")
            val db = AppDatabase.getDatabase(context)
            val dao = db.vaultDao()

            val itemsToImport = getBackupData(context, uri, password).getOrThrow()
            if (BuildConfig.DEBUG) Logcat.i(TAG, "Decrypted ${itemsToImport.size} items from backup")

            // 重新加密所有敏感字段并导入
            val encryptedItems = try {
                itemsToImport.map { item ->
                    val cipherUser = CryptoManager.getEncryptCipher() ?: throw Exception("引擎初始化失败")
                    val encUser = CryptoManager.encrypt(item.username, cipherUser)
                    
                    val cipherPass = CryptoManager.getEncryptCipher() ?: throw Exception("引擎初始化失败")
                    val encPass = CryptoManager.encrypt(item.password, cipherPass)
                    
                    val encTotp = item.totpSecret?.let {
                        val cipherTotp = CryptoManager.getEncryptCipher() ?: throw Exception("引擎初始化失败")
                        CryptoManager.encrypt(it, cipherTotp)
                    }
                    
                    item.copy(username = encUser, password = encPass, totpSecret = encTotp)
                }
            } catch (e: UserNotAuthenticatedException) {
                if (BuildConfig.DEBUG) Logcat.e(TAG, "Re-encryption interrupted by auth timeout", e)
                throw Exception("导入超时：授权已过期，请重试。")
            }

            db.withTransaction {
                if (mode == ImportMode.OVERWRITE) {
                    if (BuildConfig.DEBUG) Logcat.i(TAG, "Overwrite mode: clearing current database")
                    dao.deleteAll()
                }
                dao.insertAll(encryptedItems)
            }

            if (BuildConfig.DEBUG) Logcat.i(TAG, "Import successful. ${encryptedItems.size} items imported.")
            Result.success(Unit)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Logcat.e(TAG, "Import failed: ${e.message}", e)
            Result.failure(Exception("导入失败: ${e.message}"))
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
                if (readExactly(input, versionBuf) != 1) throw Exception("文件损坏")
                val version = versionBuf[0].toInt() and 0xFF
                if (BuildConfig.DEBUG) Logcat.i(TAG, "Detected backup version: $version")
                
                if (version > BACKUP_VERSION) throw Exception("不支持的备份版本 ($version)")

                val salt = ByteArray(SALT_LENGTH)
                if (readExactly(input, salt) != SALT_LENGTH) throw Exception("文件损坏(Salt)")
                val iv = ByteArray(IV_LENGTH)
                if (readExactly(input, iv) != IV_LENGTH) throw Exception("文件损坏(IV)")
                
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
                        
                        // 图标
                        iconName = obj.optNullableString("iconName"),
                        iconCustomPath = obj.optNullableString("iconCustomPath"),
                        
                        // TOTP
                        totpSecret = obj.optNullableString("totpSecret"),
                        totpPeriod = obj.optInt("totpPeriod", 30),
                        totpDigits = obj.optInt("totpDigits", 6),
                        totpAlgorithm = obj.optString("totpAlgorithm", "SHA1"),
                        
                        // Autofill
                        associatedAppPackage = obj.optNullableString("associatedAppPackage"),
                        associatedDomain = obj.optNullableString("associatedDomain"),
                        uriList = obj.optNullableString("uriList"),
                        matchType = obj.optInt("matchType", 0),
                        customFieldsJson = obj.optNullableString("customFieldsJson"),
                        autoSubmit = obj.optBoolean("autoSubmit", false),
                        
                        // 安全与审计
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
                if (BuildConfig.DEBUG) Logcat.i(TAG, "Backup parsing complete. Items found: ${items.size}")
                Result.success(items)
            } ?: Result.failure(Exception("无法读取文件"))
        } catch (e: Exception) {
            val message = when (e) {
                is AEADBadTagException -> "备份密码错误或文件已损坏"
                else -> e.message ?: "读取失败"
            }
            if (BuildConfig.DEBUG) Logcat.e(TAG, "Get backup data failed: $message", e)
            Result.failure(Exception(message))
        }
    }

    /**
     * 辅助扩展：解决 JSONObject.optString 在 Kotlin 中传递 null 的类型冲突问题
     */
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
