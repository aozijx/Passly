package com.example.poop.util

import android.content.Context
import android.net.Uri
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
        APPEND,    // 追加模式：将备份数据添加到现有数据之后
        OVERWRITE  // 覆盖模式：清空当前数据库并导入备份数据
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
                // 关键改进：在导出前显式解密原有数据。如果 Keystore 已失效或授权过期，则抛出异常中断导出。
                val ivUser = CryptoManager.getIvFromCipherText(item.username)
                val ivPass = CryptoManager.getIvFromCipherText(item.password)
                
                val username = ivUser?.let { CryptoManager.getDecryptCipher(it) }
                    ?.let { CryptoManager.decrypt(item.username, it) }
                    ?: throw Exception("无法导出: 条目 \"${item.title}\" 的账号解密失败。请确保已通过生物识别授权。")
                
                val passwordText = ivPass?.let { CryptoManager.getDecryptCipher(it) }
                    ?.let { CryptoManager.decrypt(item.password, it) }
                    ?: throw Exception("无法导出: 条目 \"${item.title}\" 的密码解密失败。请确保已通过生物识别授权。")

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

            // 2. 加密 JSON 数据
            val salt = ByteArray(SALT_LENGTH).apply { SecureRandom().nextBytes(this) }
            val secretKey = deriveKey(password, salt)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encryptedData = cipher.doFinal(rawData)

            // 3. 写入文件格式: VERSION(1) + SALT(16) + IV(12) + ENCRYPTED_DATA
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(BACKUP_VERSION)
                output.write(salt)
                output.write(iv)
                output.write(encryptedData)
            } ?: return@withContext Result.failure(Exception("无法打开输出流"))

            Result.success(Unit)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Logcat.e("BackupManager", "Export failed", e)
            } else {
                Logcat.e("BackupManager", "Export failed: ${e.message}")
            }
            Result.failure(e)
        }
    }

    /**
     * 导入并恢复备份
     * @param mode 导入模式：追加或覆盖
     */
    suspend fun importBackup(
        context: Context,
        uri: Uri,
        password: CharArray,
        mode: ImportMode = ImportMode.OVERWRITE
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. 获取并解密备份数据
            val backupDataResult = getBackupData(context, uri, password)
            if (backupDataResult.isFailure) return@withContext Result.failure(backupDataResult.exceptionOrNull()!!)
            val itemsToImport = backupDataResult.getOrNull() ?: return@withContext Result.failure(Exception("备份数据为空"))

            // 2. 准备数据库
            val db = AppDatabase.getDatabase(context)
            val dao = db.vaultDao()

            // 3. 重新加密并导入 (在数据库事务中执行)
            // 关键优化：提前获取 Cipher，并重用于所有条目的重新加密，提高效率
            db.withTransaction {
                val cipher = CryptoManager.getEncryptCipher() 
                    ?: throw Exception("导入失败: 无法初始化加密引擎。请确保已通过生物识别授权。")

                val encryptedItems = itemsToImport.map { item ->
                    val encUser = CryptoManager.encrypt(item.username, cipher)
                    val encPass = CryptoManager.encrypt(item.password, cipher)
                    item.copy(username = encUser, password = encPass)
                }

                // 执行写入逻辑
                if (mode == ImportMode.OVERWRITE) {
                    val currentItems = dao.getAllItems().first()
                    currentItems.forEach { dao.delete(it) }
                }
                
                encryptedItems.forEach { dao.insert(it) }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Logcat.e("BackupManager", "Import failed", e)
            } else {
                Logcat.e("BackupManager", "Import failed: ${e.message}")
            }
            Result.failure(e)
        }
    }
    
    /**
     * 从备份文件中读取数据并解密。
     * 返回的 VaultItem 列表中 username 和 password 字段是【明文】。
     */
    suspend fun getBackupData(
        context: Context,
        uri: Uri,
        password: CharArray
    ): Result<List<VaultItem>> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                // 1. 读取版本号
                val version = input.read()
                if (version != BACKUP_VERSION) {
                    throw Exception("不支持的备份版本 (发现版本: $version, 需要版本: $BACKUP_VERSION)")
                }

                // 2. 读取 Salt 和 IV
                val salt = ByteArray(SALT_LENGTH).also { 
                    if (input.read(it) != SALT_LENGTH) throw Exception("无效的备份文件 (Salt 长度错误)")
                }
                val iv = ByteArray(IV_LENGTH).also { 
                    if (input.read(it) != IV_LENGTH) throw Exception("无效的备份文件 (IV 长度错误)")
                }
                val encryptedData = input.readBytes()

                // 3. 解密数据
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
            } ?: Result.failure(Exception("无法打开输入流"))
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Logcat.e("BackupManager", "Get backup data failed", e)
            } else {
                Logcat.e("BackupManager", "Get backup data failed: ${e.message}")
            }
            Result.failure(e)
        }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_LENGTH)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }
}
