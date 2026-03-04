package com.example.poop.ui.screens.vault.utils

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
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
 */
object BackupManager {
    private const val BACKUP_VERSION = 2
    private const val PBKDF2_ITERATIONS = 200000
    private const val KEY_LENGTH = 256
    private const val SALT_LENGTH = 16
    private const val IV_LENGTH = 12

    enum class ImportMode {
        APPEND, OVERWRITE
    }

    suspend fun exportBackup(
        context: Context,
        uri: Uri,
        password: CharArray
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(context)
            val items = db.vaultDao().getAllItems().first()

            val jsonArray = JSONArray()
            items.forEach { item ->
                val username = decryptField(item.username, "账号")
                val passwordText = decryptField(item.password, "密码")
                val totpSecretText = item.totpSecret?.let { decryptField(it, "TOTP密钥") }

                val obj = JSONObject().apply {
                    put("title", item.title)
                    put("username", username)
                    put("password", passwordText)
                    put("category", item.category)
                    put("notes", item.notes ?: JSONObject.NULL)
                    put("iconName", item.iconName ?: JSONObject.NULL)
                    put("iconCustomPath", item.iconCustomPath ?: JSONObject.NULL)
                    put("totpSecret", totpSecretText ?: JSONObject.NULL)
                    put("totpPeriod", item.totpPeriod)
                    put("totpDigits", item.totpDigits)
                    put("totpAlgorithm", item.totpAlgorithm)
                    put("associatedAppPackage", item.associatedAppPackage ?: JSONObject.NULL)
                    put("associatedDomain", item.associatedDomain ?: JSONObject.NULL)
                    put("uriList", item.uriList ?: JSONObject.NULL)
                    put("matchType", item.matchType)
                    put("customFieldsJson", item.customFieldsJson ?: JSONObject.NULL)
                    put("autoSubmit", item.autoSubmit)
                    put("strengthScore", item.strengthScore?.toDouble() ?: JSONObject.NULL)
                    put("lastUsedAt", item.lastUsedAt ?: JSONObject.NULL)
                    put("usageCount", item.usageCount)
                    put("favorite", item.favorite)
                    put("tags", item.tags ?: JSONObject.NULL)
                    put("createdAt", item.createdAt ?: JSONObject.NULL)
                    put("updatedAt", item.updatedAt ?: JSONObject.NULL)
                    put("expiresAt", item.expiresAt ?: JSONObject.NULL)
                }
                jsonArray.put(obj)
            }

            val rawData = jsonArray.toString().toByteArray()
            val salt = ByteArray(SALT_LENGTH).apply { SecureRandom().nextBytes(this) }
            val secretKey = deriveKey(password, salt)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encryptedData = cipher.doFinal(rawData)

            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(BACKUP_VERSION)
                output.write(salt)
                output.write(iv)
                output.write(encryptedData)
            } ?: throw Exception("无法写入文件")

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun decryptField(encryptedText: String, label: String): String {
        val iv = CryptoManager.getIvFromCipherText(encryptedText) ?: throw Exception("$label IV缺失")
        val cipher = CryptoManager.getDecryptCipher(iv) ?: throw Exception("$label 引擎初始化失败")
        return CryptoManager.decrypt(encryptedText, cipher) ?: throw Exception("$label 解密失败")
    }

    suspend fun importBackup(
        context: Context,
        uri: Uri,
        password: CharArray,
        mode: ImportMode = ImportMode.OVERWRITE
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(context)
            val itemsToImport = getBackupData(context, uri, password).getOrThrow()

            val encryptedItems = itemsToImport.map { item ->
                val encUser = CryptoManager.encrypt(item.username, CryptoManager.getEncryptCipher()!!)
                val encPass = CryptoManager.encrypt(item.password, CryptoManager.getEncryptCipher()!!)
                val encTotp = item.totpSecret?.let { CryptoManager.encrypt(it, CryptoManager.getEncryptCipher()!!) }
                item.copy(username = encUser, password = encPass, totpSecret = encTotp)
            }

            db.withTransaction {
                if (mode == ImportMode.OVERWRITE) db.vaultDao().deleteAll()
                db.vaultDao().insertAll(encryptedItems)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBackupData(context: Context, uri: Uri, password: CharArray): Result<List<VaultItem>> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val versionBuf = ByteArray(1)
                input.read(versionBuf)
                val version = versionBuf[0].toInt() and 0xFF
                if (version > BACKUP_VERSION) throw Exception("版本不支持")

                val salt = ByteArray(SALT_LENGTH)
                input.read(salt)
                val iv = ByteArray(IV_LENGTH)
                input.read(iv)

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
                        lastUsedAt = if (obj.isNull("lastUsedAt")) null else obj.getLong("lastUsedAt"),
                        usageCount = obj.optInt("usageCount", 0),
                        favorite = obj.optBoolean("favorite", false),
                        tags = obj.optNullableString("tags"),
                        createdAt = if (obj.isNull("createdAt")) System.currentTimeMillis() else obj.getLong("createdAt"),
                        updatedAt = if (obj.isNull("updatedAt")) null else obj.getLong("updatedAt"),
                        expiresAt = if (obj.isNull("expiresAt")) null else obj.getLong("expiresAt")
                    ))
                }
                Result.success(items)
            } ?: Result.failure(Exception("无法读取"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun JSONObject.optNullableString(name: String): String? = if (isNull(name)) null else optString(name)

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_LENGTH)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }
}
