package com.example.poop.ui.screens.vault.utils

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.JsonReader
import android.util.JsonToken
import android.util.JsonWriter
import androidx.room.withTransaction
import com.example.poop.data.AppDatabase
import com.example.poop.data.VaultEntry
import com.example.poop.util.Logcat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * 备份管理类：支持将数据库内容导出为加密文件，并支持从加密文件恢复。
 * 已优化为流式读写（JsonReader/JsonWriter + CipherStream），防止处理大量条目时内存溢出。
 */
object BackupManager {
    private const val TAG = "BackupManager"
    private const val BACKUP_VERSION = 1

    private const val PBKDF2_ITERATIONS = 200000
    private const val KEY_LENGTH = 256
    private const val SALT_LENGTH = 16
    private const val IV_LENGTH = 12

    enum class ImportMode {
        APPEND, OVERWRITE
    }

    /**
     * 导出加密备份 (流式)
     */
    suspend fun exportBackup(
        context: Context,
        uri: Uri,
        password: CharArray
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(context)
            val entries = db.vaultDao().getAllEntries().first()
            Logcat.i(TAG, "开始导出备份 (流式): $uri, 条目数量: ${entries.size}")

            val salt = ByteArray(SALT_LENGTH).apply { SecureRandom().nextBytes(this) }
            val secretKey = deriveKey(password, salt)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv

            context.contentResolver.openOutputStream(uri)?.use { output ->
                // 写入元数据
                output.write(BACKUP_VERSION)
                output.write(salt)
                output.write(iv)

                // 包装加密流与 JSON 流
                CipherOutputStream(output, cipher).use { cos ->
                    JsonWriter(OutputStreamWriter(cos, "UTF-8")).use { writer ->
                        writer.setIndent("") // 紧凑格式
                        writer.beginArray()
                        entries.forEach { entry ->
                            writeEntry(writer, entry)
                        }
                        writer.endArray()
                        writer.flush()
                    }
                }
            } ?: throw Exception("无法写入文件")

            Logcat.i(TAG, "备份导出成功")
            Result.success(Unit)
        } catch (e: Exception) {
            Logcat.e(TAG, "备份导出失败", e)
            Result.failure(e)
        }
    }

    private fun writeEntry(writer: JsonWriter, entry: VaultEntry) {
        writer.beginObject()
        writer.name("title").value(entry.title)
        writer.name("username").value(decryptField(entry.username))
        writer.name("password").value(decryptField(entry.password))
        writer.name("category").value(entry.category)
        writer.name("notes").value(entry.notes?.let { decryptField(it) })
        writer.name("iconName").value(entry.iconName)
        writer.name("iconCustomPath").value(entry.iconCustomPath)
        writer.name("totpSecret").value(entry.totpSecret?.let { decryptField(it) })
        writer.name("totpPeriod").value(entry.totpPeriod.toLong())
        writer.name("totpDigits").value(entry.totpDigits.toLong())
        writer.name("totpAlgorithm").value(entry.totpAlgorithm)
        
        // v3 及扩展字段
        writer.name("passkeyDataJson").value(entry.passkeyDataJson?.let { decryptField(it) })
        writer.name("recoveryCodes").value(entry.recoveryCodes?.let { decryptField(it) })
        writer.name("hardwareKeyInfo").value(entry.hardwareKeyInfo)
        writer.name("wifiEncryptionType").value(entry.wifiEncryptionType)
        writer.name("wifiIsHidden").value(entry.wifiIsHidden)
        
        // 金融与证件加固字段
        writer.name("cardCvv").value(entry.cardCvv?.let { decryptField(it) })
        writer.name("cardExpiration").value(entry.cardExpiration?.let { decryptField(it) })
        writer.name("idNumber").value(entry.idNumber?.let { decryptField(it) })
        writer.name("paymentPin").value(entry.paymentPin?.let { decryptField(it) })
        writer.name("sshPrivateKey").value(entry.sshPrivateKey?.let { decryptField(it) })
        writer.name("cryptoSeedPhrase").value(entry.cryptoSeedPhrase?.let { decryptField(it) })
        
        writer.name("entryType").value(entry.entryType.toLong())
        writer.name("associatedAppPackage").value(entry.associatedAppPackage)
        writer.name("associatedDomain").value(entry.associatedDomain)
        
        writer.name("uriList")
        if (entry.uriList == null) {
            writer.nullValue()
        } else {
            writer.beginArray()
            entry.uriList.forEach { writer.value(it) }
            writer.endArray()
        }

        writer.name("matchType").value(entry.matchType.toLong())
        writer.name("customFieldsJson").value(entry.customFieldsJson?.let { decryptField(it) })
        writer.name("autoSubmit").value(entry.autoSubmit)
        
        writer.name("encryptedImageData").value(entry.encryptedImageData?.let { 
            Base64.encodeToString(it, Base64.NO_WRAP) 
        })

        writer.name("strengthScore").value(entry.strengthScore?.toDouble())
        writer.name("lastUsedAt").value(entry.lastUsedAt)
        writer.name("usageCount").value(entry.usageCount.toLong())
        writer.name("favorite").value(entry.favorite)
        
        writer.name("tags")
        if (entry.tags == null) {
            writer.nullValue()
        } else {
            writer.beginArray()
            entry.tags.forEach { writer.value(it) }
            writer.endArray()
        }
        
        writer.name("createdAt").value(entry.createdAt)
        writer.name("updatedAt").value(entry.updatedAt)
        writer.name("expiresAt").value(entry.expiresAt)
        writer.endObject()
    }

    private fun decryptField(encryptedText: String): String {
        val iv = CryptoManager.getIvFromCipherText(encryptedText) ?: return ""
        val cipher = CryptoManager.getDecryptCipher(iv) ?: return ""
        return CryptoManager.decrypt(encryptedText, cipher) ?: ""
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
            Logcat.i(TAG, "开始导入备份: $uri, 模式: $mode")
            val db = AppDatabase.getDatabase(context)
            val entriesToImport = getBackupData(context, uri, password).getOrThrow()

            // 重新加密字段以适应当前设备的 KeyStore
            val encryptedEntries = entriesToImport.map { entry ->
                encryptEntryFields(entry)
            }

            db.withTransaction {
                if (mode == ImportMode.OVERWRITE) {
                    Logcat.d(TAG, "清空现有数据库 (OVERWRITE 模式)")
                    db.vaultDao().deleteAll()
                }
                db.vaultDao().insertAll(encryptedEntries)
            }
            Logcat.i(TAG, "备份导入成功，共导入 ${encryptedEntries.size} 条数据")
            Result.success(Unit)
        } catch (e: Exception) {
            Logcat.e(TAG, "备份导入失败", e)
            Result.failure(e)
        }
    }

    private fun encryptEntryFields(entry: VaultEntry): VaultEntry {
        val cipher = CryptoManager.getEncryptCipher() ?: throw Exception("无法获取加密器")
        return entry.copy(
            username = CryptoManager.encrypt(entry.username, cipher),
            password = CryptoManager.encrypt(entry.password, cipher),
            notes = entry.notes?.let { CryptoManager.encrypt(it, cipher) },
            totpSecret = entry.totpSecret?.let { CryptoManager.encrypt(it, cipher) },
            passkeyDataJson = entry.passkeyDataJson?.let { CryptoManager.encrypt(it, cipher) },
            recoveryCodes = entry.recoveryCodes?.let { CryptoManager.encrypt(it, cipher) },
            cardCvv = entry.cardCvv?.let { CryptoManager.encrypt(it, cipher) },
            cardExpiration = entry.cardExpiration?.let { CryptoManager.encrypt(it, cipher) },
            idNumber = entry.idNumber?.let { CryptoManager.encrypt(it, cipher) },
            paymentPin = entry.paymentPin?.let { CryptoManager.encrypt(it, cipher) },
            sshPrivateKey = entry.sshPrivateKey?.let { CryptoManager.encrypt(it, cipher) },
            cryptoSeedPhrase = entry.cryptoSeedPhrase?.let { CryptoManager.encrypt(it, cipher) },
            customFieldsJson = entry.customFieldsJson?.let { CryptoManager.encrypt(it, cipher) }
        )
    }

    suspend fun getBackupData(context: Context, uri: Uri, password: CharArray): Result<List<VaultEntry>> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val versionBuf = ByteArray(1)
                if (input.read(versionBuf) == -1) throw Exception("读取失败")
                val version = versionBuf[0].toInt() and 0xFF
                Logcat.d(TAG, "解析备份文件，版本号: $version")
                
                val salt = ByteArray(SALT_LENGTH)
                input.read(salt)
                val iv = ByteArray(IV_LENGTH)
                input.read(iv)

                val secretKey = deriveKey(password, salt)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))

                val entries = mutableListOf<VaultEntry>()
                CipherInputStream(input, cipher).use { cis ->
                    JsonReader(InputStreamReader(cis, "UTF-8")).use { reader ->
                        reader.beginArray()
                        while (reader.hasNext()) {
                            entries.add(readEntry(reader))
                        }
                        reader.endArray()
                    }
                }
                Logcat.i(TAG, "备份解析成功，解析出 ${entries.size} 条数据")
                Result.success(entries)
            } ?: Result.failure(Exception("无法读取文件内容 (InputStream 为 null)"))
        } catch (e: Exception) {
            Logcat.e(TAG, "解析备份数据失败", e)
            Result.failure(e)
        }
    }

    private fun readEntry(reader: JsonReader): VaultEntry {
        var title = ""
        var username = ""
        var password = ""
        var category = ""
        var notes: String? = null
        var iconName: String? = null
        var iconCustomPath: String? = null
        var totpSecret: String? = null
        var totpPeriod = 30
        var totpDigits = 6
        var totpAlgorithm = "SHA1"
        var passkeyDataJson: String? = null
        var recoveryCodes: String? = null
        var hardwareKeyInfo: String? = null
        var wifiEncryptionType: String? = "WPA"
        var wifiIsHidden = false
        var cardCvv: String? = null
        var cardExpiration: String? = null
        var idNumber: String? = null
        var paymentPin: String? = null
        var sshPrivateKey: String? = null
        var cryptoSeedPhrase: String? = null
        var entryType = 0
        var associatedAppPackage: String? = null
        var associatedDomain: String? = null
        var uriList: List<String>? = null
        var matchType = 0
        var customFieldsJson: String? = null
        var autoSubmit = false
        var encryptedImageData: ByteArray? = null
        var strengthScore: Float? = null
        var lastUsedAt: Long? = null
        var usageCount = 0
        var favorite = false
        var tags: List<String>? = null
        var createdAt: Long? = System.currentTimeMillis()
        var updatedAt: Long? = null
        var expiresAt: Long? = null

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "title" -> title = reader.nextString()
                "username" -> username = reader.nextString()
                "password" -> password = reader.nextString()
                "category" -> category = reader.nextString()
                "notes" -> notes = reader.nextNullableString()
                "iconName" -> iconName = reader.nextNullableString()
                "iconCustomPath" -> iconCustomPath = reader.nextNullableString()
                "totpSecret" -> totpSecret = reader.nextNullableString()
                "totpPeriod" -> totpPeriod = reader.nextInt()
                "totpDigits" -> totpDigits = reader.nextInt()
                "totpAlgorithm" -> totpAlgorithm = reader.nextString()
                "passkeyDataJson" -> passkeyDataJson = reader.nextNullableString()
                "recoveryCodes" -> recoveryCodes = reader.nextNullableString()
                "hardwareKeyInfo" -> hardwareKeyInfo = reader.nextNullableString()
                "wifiEncryptionType" -> wifiEncryptionType = reader.nextNullableString()
                "wifiIsHidden" -> wifiIsHidden = reader.nextBoolean()
                "cardCvv" -> cardCvv = reader.nextNullableString()
                "cardExpiration" -> cardExpiration = reader.nextNullableString()
                "idNumber" -> idNumber = reader.nextNullableString()
                "paymentPin" -> paymentPin = reader.nextNullableString()
                "sshPrivateKey" -> sshPrivateKey = reader.nextNullableString()
                "cryptoSeedPhrase" -> cryptoSeedPhrase = reader.nextNullableString()
                "entryType" -> entryType = reader.nextInt()
                "associatedAppPackage" -> associatedAppPackage = reader.nextNullableString()
                "associatedDomain" -> associatedDomain = reader.nextNullableString()
                "uriList" -> uriList = reader.nextStringList()
                "matchType" -> matchType = reader.nextInt()
                "customFieldsJson" -> customFieldsJson = reader.nextNullableString()
                "autoSubmit" -> autoSubmit = reader.nextBoolean()
                "encryptedImageData" -> encryptedImageData = reader.nextNullableString()?.let { Base64.decode(it, Base64.NO_WRAP) }
                "strengthScore" -> strengthScore = reader.nextNullableDouble()?.toFloat()
                "lastUsedAt" -> lastUsedAt = reader.nextNullableLong()
                "usageCount" -> usageCount = reader.nextInt()
                "favorite" -> favorite = reader.nextBoolean()
                "tags" -> tags = reader.nextStringList()
                "createdAt" -> createdAt = reader.nextNullableLong()
                "updatedAt" -> updatedAt = reader.nextNullableLong()
                "expiresAt" -> expiresAt = reader.nextNullableLong()
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        return VaultEntry(
            title = title,
            username = username,
            password = password,
            category = category,
            notes = notes,
            iconName = iconName,
            iconCustomPath = iconCustomPath,
            totpSecret = totpSecret,
            totpPeriod = totpPeriod,
            totpDigits = totpDigits,
            totpAlgorithm = totpAlgorithm,
            passkeyDataJson = passkeyDataJson,
            recoveryCodes = recoveryCodes,
            hardwareKeyInfo = hardwareKeyInfo,
            wifiEncryptionType = wifiEncryptionType,
            wifiIsHidden = wifiIsHidden,
            cardCvv = cardCvv,
            cardExpiration = cardExpiration,
            idNumber = idNumber,
            paymentPin = paymentPin,
            sshPrivateKey = sshPrivateKey,
            cryptoSeedPhrase = cryptoSeedPhrase,
            entryType = entryType,
            associatedAppPackage = associatedAppPackage,
            associatedDomain = associatedDomain,
            uriList = uriList,
            matchType = matchType,
            customFieldsJson = customFieldsJson,
            autoSubmit = autoSubmit,
            encryptedImageData = encryptedImageData,
            strengthScore = strengthScore,
            lastUsedAt = lastUsedAt,
            usageCount = usageCount,
            favorite = favorite,
            tags = tags,
            createdAt = createdAt,
            updatedAt = updatedAt,
            expiresAt = expiresAt
        )
    }

    private fun JsonReader.nextNullableString(): String? {
        return if (peek() == JsonToken.NULL) {
            nextNull()
            null
        } else {
            nextString()
        }
    }

    private fun JsonReader.nextNullableLong(): Long? {
        return if (peek() == JsonToken.NULL) {
            nextNull()
            null
        } else {
            nextLong()
        }
    }

    private fun JsonReader.nextNullableDouble(): Double? {
        return if (peek() == JsonToken.NULL) {
            nextNull()
            null
        } else {
            nextDouble()
        }
    }

    private fun JsonReader.nextStringList(): List<String>? {
        if (peek() == JsonToken.NULL) {
            nextNull()
            return null
        }
        val list = mutableListOf<String>()
        beginArray()
        while (hasNext()) {
            list.add(nextString())
        }
        endArray()
        return list
    }

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_LENGTH)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }
}
