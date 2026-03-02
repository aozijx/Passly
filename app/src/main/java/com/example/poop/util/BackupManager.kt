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
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * 备份管理类：支持将数据库内容导出为加密文件，并支持从加密文件恢复。
 * 使用 PBKDF2 派生密钥 + AES-GCM 加密，不依赖 Keystore。
 */
object BackupManager {
    // 备份文件格式版本号
    private const val BACKUP_VERSION = 1
    
    // 增加迭代次数以增强抗暴力破解能力，符合现代安全标准
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
            
            // 1. 将数据转换为 JSON
            val jsonArray = JSONArray()
            items.forEach { item ->
                val ivUser = CryptoManager.getIvFromCipherText(item.username)
                val ivPass = CryptoManager.getIvFromCipherText(item.password)
                
                // 每一个字段解密都需要在密钥解锁的 10 秒窗口内完成
                val username = try {
                    ivUser?.let { CryptoManager.getDecryptCipher(it) }
                        ?.let { CryptoManager.decrypt(item.username, it) }
                } catch (e: UserNotAuthenticatedException) {
                    Logcat.e("BackupManager", "Export timeout on username", e)
                    throw Exception("导出超时：生物识别授权已过期，请重试并保持操作连贯。")
                } ?: throw Exception("无法导出: 条目 \"${item.title}\" 的账号解密失败。")
                
                val passwordText = try {
                    ivPass?.let { CryptoManager.getDecryptCipher(it) }
                        ?.let { CryptoManager.decrypt(item.password, it) }
                } catch (e: UserNotAuthenticatedException) {
                    Logcat.e("BackupManager", "Export timeout on password", e)
                    throw Exception("导出超时：生物识别授权已过期。")
                } ?: throw Exception("无法导出: 条目 \"${item.title}\" 的密码解密失败。")

                val obj = JSONObject().apply {
                    put("title", item.title)
                    put("username", username)
                    put("password", passwordText)
                    put("category", item.category)
                    put("notes", item.notes)
                }
                jsonArray.put(obj)
            }

            val rawData = jsonArray.toString().toByteArray()

            // 2. 加密 JSON 数据 (使用备份密码)
            val salt = ByteArray(SALT_LENGTH).apply { SecureRandom().nextBytes(this) }
            val secretKey = deriveKey(password, salt)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encryptedData = cipher.doFinal(rawData)

            // 3. 写入文件
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(BACKUP_VERSION)
                output.write(salt)
                output.write(iv)
                output.write(encryptedData)
            } ?: return@withContext Result.failure(Exception("无法保存到指定路径"))

            Result.success(Unit)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Logcat.e("BackupManager", "Export failed", e)
            Result.failure(e)
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
            // 0. 预热数据库
            val db = AppDatabase.getDatabase(context)
            val dao = db.vaultDao()

            // 1. 获取解密后的明文数据
            val backupDataResult = getBackupData(context, uri, password)
            if (backupDataResult.isFailure) return@withContext Result.failure(backupDataResult.exceptionOrNull()!!)
            val itemsToImport = backupDataResult.getOrNull() ?: return@withContext Result.failure(Exception("备份为空"))

            // 2. 重新加密
            val encryptedItems = try {
                itemsToImport.map { item ->
                    val cipherUser = CryptoManager.getEncryptCipher() 
                        ?: throw Exception("无法初始化加密引擎。")
                    val encUser = CryptoManager.encrypt(item.username, cipherUser)
                    
                    val cipherPass = CryptoManager.getEncryptCipher() 
                        ?: throw Exception("无法初始化加密引擎。")
                    val encPass = CryptoManager.encrypt(item.password, cipherPass)
                    
                    item.copy(username = encUser, password = encPass)
                }
            } catch (e: UserNotAuthenticatedException) {
                Logcat.e("BackupManager", "Import timeout during re-encryption", e)
                throw Exception("导入超时：生物识别授权已过期，请重试。")
            }

            // 3. 执行写入
            db.withTransaction {
                if (mode == ImportMode.OVERWRITE) {
                    val currentItems = dao.getAllItems().first()
                    currentItems.forEach { dao.delete(it) }
                }
                encryptedItems.forEach { dao.insert(it) }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Logcat.e("BackupManager", "Import failed", e)
            
            val refinedMessage = when {
                e.message?.contains("超时") == true -> e.message!!
                e is android.database.sqlite.SQLiteException -> "导入失败: 数据库访问出错。请尝试清除应用数据后重试。"
                else -> "导入失败: ${e.message}"
            }
            Result.failure(Exception(refinedMessage))
        }
    }
    
    /**
     * 读取备份并解密
     */
    suspend fun getBackupData(
        context: Context,
        uri: Uri,
        password: CharArray
    ): Result<List<VaultItem>> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val version = input.read()
                if (version != BACKUP_VERSION) throw Exception("不支持的备份版本 ($version)")

                val salt = ByteArray(SALT_LENGTH)
                if (readAll(input, salt) != SALT_LENGTH) throw Exception("备份文件 Salt 损坏")
                
                val iv = ByteArray(IV_LENGTH)
                if (readAll(input, iv) != IV_LENGTH) throw Exception("备份文件 IV 损坏")
                
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
                        notes = obj.optString("notes", ""),
                        timestamp = System.currentTimeMillis()
                    ))
                }
                Result.success(items)
            } ?: Result.failure(Exception("无法读取文件"))
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Logcat.e("BackupManager", "Get backup data failed", e)
            
            val message = when (e) {
                is AEADBadTagException -> "备份密码错误，请重试"
                else -> e.message ?: "读取备份失败"
            }
            Result.failure(Exception(message))
        }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_LENGTH)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    private fun readAll(input: java.io.InputStream, buffer: ByteArray): Int {
        var totalRead = 0
        while (totalRead < buffer.size) {
            val read = input.read(buffer, totalRead, buffer.size - totalRead)
            if (read == -1) break
            totalRead += read
        }
        return totalRead
    }
}
