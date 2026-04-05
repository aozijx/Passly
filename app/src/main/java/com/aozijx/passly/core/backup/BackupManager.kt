package com.aozijx.passly.core.backup

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.JsonReader
import android.util.JsonToken
import android.util.JsonWriter
import androidx.room.withTransaction
import com.aozijx.passly.core.crypto.CryptoManager
import com.aozijx.passly.core.logging.Logcat
import com.aozijx.passly.data.entity.VaultEntryEntity
import com.aozijx.passly.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * 备份管理类：支持将数据库内容导出为加密文件，并支持从加密文件恢复。
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

    suspend fun exportBackup(
        context: Context,
        uri: Uri,
        password: CharArray
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(context)
            val entries = db.vaultDao().getAllEntries().first()
            Logcat.i(TAG, "开始导出备份: $uri, 条目数量: ${entries.size}")

            val salt = ByteArray(SALT_LENGTH).apply { SecureRandom().nextBytes(this) }
            val secretKey = deriveKey(password, salt)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv

            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(BACKUP_VERSION)
                output.write(salt)
                output.write(iv)

                CipherOutputStream(output, cipher).use { cos ->
                    JsonWriter(OutputStreamWriter(cos, StandardCharsets.UTF_8)).use { writer ->
                        writer.setIndent("")
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

    private fun writeEntry(writer: JsonWriter, entry: VaultEntryEntity) {
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
        writer.name("passkeyDataJson").value(entry.passkeyDataJson?.let { decryptField(it) })
        writer.name("recoveryCodes").value(entry.recoveryCodes?.let { decryptField(it) })
        writer.name("hardwareKeyInfo").value(entry.hardwareKeyInfo)
        writer.name("wifiEncryptionType").value(entry.wifiEncryptionType)
        writer.name("wifiIsHidden").value(entry.wifiIsHidden)
        writer.name("cardCvv").value(entry.cardCvv?.let { decryptField(it) })
        writer.name("cardExpiration").value(entry.cardExpiration?.let { decryptField(it) })
        writer.name("idNumber").value(entry.idNumber?.let { decryptField(it) })
        writer.name("paymentPin").value(entry.paymentPin?.let { decryptField(it) })
        writer.name("paymentPlatform").value(entry.paymentPlatform)
        writer.name("securityQuestion").value(entry.securityQuestion)
        writer.name("securityAnswer").value(entry.securityAnswer?.let { decryptField(it) })
        writer.name("sshPrivateKey").value(entry.sshPrivateKey?.let { decryptField(it) })
        writer.name("cryptoSeedPhrase").value(entry.cryptoSeedPhrase?.let { decryptField(it) })
        writer.name("entryType").value(entry.entryType.toLong())
        writer.name("associatedAppPackage").value(entry.associatedAppPackage)
        writer.name("associatedDomain").value(entry.associatedDomain)
        writer.name("uriList")
        if (entry.uriList == null) writer.nullValue()
        else {
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
        if (entry.tags == null) writer.nullValue()
        else {
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
        if (encryptedText.isEmpty()) return ""
        return try {
            // 核心修复：直接使用重构后的解密逻辑
            CryptoManager.decrypt(encryptedText)
        } catch (e: Exception) {
            Logcat.e(TAG, "Field decryption failed during backup", e)
            ""
        }
    }

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
            val encryptedEntries = entriesToImport.map { encryptEntryFields(it) }

            db.withTransaction {
                if (mode == ImportMode.OVERWRITE) {
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

    private fun encryptEntryFields(entry: VaultEntryEntity): VaultEntryEntity {
        fun encryptIfNotNull(text: String?): String? = if (text.isNullOrEmpty()) text else CryptoManager.encrypt(text)
        return entry.copy(
            username = encryptIfNotNull(entry.username) ?: "",
            password = encryptIfNotNull(entry.password) ?: "",
            notes = encryptIfNotNull(entry.notes),
            totpSecret = encryptIfNotNull(entry.totpSecret),
            passkeyDataJson = encryptIfNotNull(entry.passkeyDataJson),
            recoveryCodes = encryptIfNotNull(entry.recoveryCodes),
            cardCvv = encryptIfNotNull(entry.cardCvv),
            cardExpiration = encryptIfNotNull(entry.cardExpiration),
            idNumber = encryptIfNotNull(entry.idNumber),
            paymentPin = encryptIfNotNull(entry.paymentPin),
            securityAnswer = encryptIfNotNull(entry.securityAnswer),
            sshPrivateKey = encryptIfNotNull(entry.sshPrivateKey),
            cryptoSeedPhrase = encryptIfNotNull(entry.cryptoSeedPhrase),
            customFieldsJson = encryptIfNotNull(entry.customFieldsJson)
        )
    }

    suspend fun getBackupData(
        context: Context,
        uri: Uri,
        password: CharArray
    ): Result<List<VaultEntryEntity>> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val version = readSingleByteOrThrow(input)
                Logcat.i(TAG, "读取到备份文件版本: $version")
                
                if (version > BACKUP_VERSION) {
                    return@withContext Result.failure(Exception("备份文件版本 ($version) 高于当前应用支持的版本 ($BACKUP_VERSION)，请更新应用"))
                }
                
                val salt = ByteArray(SALT_LENGTH)
                readFullyOrThrow(input, salt, "salt")
                val iv = ByteArray(IV_LENGTH)
                readFullyOrThrow(input, iv, "iv")

                val secretKey = deriveKey(password, salt)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))

                val entries = mutableListOf<VaultEntryEntity>()
                CipherInputStream(input, cipher).use { cis ->
                    JsonReader(InputStreamReader(cis, StandardCharsets.UTF_8)).use { reader ->
                        reader.beginArray()
                        while (reader.hasNext()) entries.add(readEntry(reader))
                        reader.endArray()
                    }
                }
                Result.success(entries)
            } ?: Result.failure(Exception("InputStream 为 null"))
        } catch (e: Exception) {
            val mapped = mapImportFailure(e)
            Logcat.w(TAG, "备份解密失败映射: ${mapped.message}")
            Result.failure(mapped)
        }
    }

    private fun readEntry(reader: JsonReader): VaultEntryEntity {
        var title = ""; var username = ""; var password = ""; var category = ""
        var notes: String? = null; var iconName: String? = null; var iconCustomPath: String? = null; var totpSecret: String? = null
        var totpPeriod = 30; var totpDigits = 6; var totpAlgorithm = "SHA1"
        var passkeyDataJson: String? = null; var recoveryCodes: String? = null; var hardwareKeyInfo: String? = null
        var wifiEncryptionType: String? = "WPA"; var wifiIsHidden = false
        var cardCvv: String? = null; var cardExpiration: String? = null; var idNumber: String? = null
        var paymentPin: String? = null; var paymentPlatform: String? = null
        var securityQuestion: String? = null; var securityAnswer: String? = null
        var sshPrivateKey: String? = null; var cryptoSeedPhrase: String? = null
        var entryType = 0; var associatedAppPackage: String? = null; var associatedDomain: String? = null
        var uriList: List<String>? = null; var matchType = 0; var customFieldsJson: String? = null
        var autoSubmit = false; var encryptedImageData: ByteArray? = null; var strengthScore: Float? = null
        var lastUsedAt: Long? = null; var usageCount = 0; var favorite = false; var tags: List<String>? = null
        var createdAt: Long? = System.currentTimeMillis(); var updatedAt: Long? = null; var expiresAt: Long? = null

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
                "paymentPlatform" -> paymentPlatform = reader.nextNullableString()
                "securityQuestion" -> securityQuestion = reader.nextNullableString()
                "securityAnswer" -> securityAnswer = reader.nextNullableString()
                "sshPrivateKey" -> sshPrivateKey = reader.nextNullableString()
                "cryptoSeedPhrase" -> cryptoSeedPhrase = reader.nextNullableString()
                "entryType" -> entryType = reader.nextInt()
                "associatedAppPackage" -> associatedAppPackage = reader.nextNullableString()
                "associatedDomain" -> associatedDomain = reader.nextNullableString()
                "uriList" -> uriList = reader.nextStringList()
                "matchType" -> matchType = reader.nextInt()
                "customFieldsJson" -> customFieldsJson = reader.nextNullableString()
                "autoSubmit" -> autoSubmit = reader.nextBoolean()
                "encryptedImageData" -> encryptedImageData = reader.nextNullableString()?.let {
                    if (it.isEmpty()) null else Base64.decode(it, Base64.NO_WRAP)
                }
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

        return VaultEntryEntity(
            title = title, username = username, password = password, category = category,
            notes = notes, iconName = iconName, iconCustomPath = iconCustomPath,
            totpSecret = totpSecret, totpPeriod = totpPeriod, totpDigits = totpDigits,
            totpAlgorithm = totpAlgorithm, passkeyDataJson = passkeyDataJson,
            recoveryCodes = recoveryCodes, hardwareKeyInfo = hardwareKeyInfo,
            wifiEncryptionType = wifiEncryptionType, wifiIsHidden = wifiIsHidden,
            cardCvv = cardCvv, cardExpiration = cardExpiration, idNumber = idNumber,
            paymentPin = paymentPin, paymentPlatform = paymentPlatform,
            securityQuestion = securityQuestion, securityAnswer = securityAnswer,
            sshPrivateKey = sshPrivateKey,
            cryptoSeedPhrase = cryptoSeedPhrase, entryType = entryType,
            associatedAppPackage = associatedAppPackage, associatedDomain = associatedDomain,
            uriList = uriList, matchType = matchType, customFieldsJson = customFieldsJson,
            autoSubmit = autoSubmit, encryptedImageData = encryptedImageData, strengthScore = strengthScore, lastUsedAt = lastUsedAt,
            usageCount = usageCount, favorite = favorite, tags = tags, createdAt = createdAt,
            updatedAt = updatedAt, expiresAt = expiresAt
        )
    }

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_LENGTH)
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }

    private fun readSingleByteOrThrow(input: InputStream): Int {
        val value = input.read()
        if (value == -1) {
            throw EOFException("备份文件损坏: 缺少 version")
        }
        return value and 0xFF
    }

    private fun readFullyOrThrow(input: InputStream, target: ByteArray, fieldName: String) {
        var offset = 0
        while (offset < target.size) {
            val count = input.read(target, offset, target.size - offset)
            if (count == -1) {
                throw EOFException("备份文件损坏: $fieldName 不完整")
            }
            offset += count
        }
    }

    private fun mapImportFailure(error: Exception): Exception {
        if (error.hasCause<AEADBadTagException>()) {
            return Exception("密码错误，请确认备份密码后重试", error)
        }
        if (error is EOFException) {
            return Exception("备份文件不完整或已损坏", error)
        }
        if (error is IOException && error.hasCause<AEADBadTagException>()) {
            return Exception("密码错误，请确认备份密码后重试", error)
        }
        return error
    }

    private inline fun <reified T : Throwable> Throwable.hasCause(): Boolean {
        var current: Throwable? = this
        while (current != null) {
            if (current is T) return true
            current = current.cause
        }
        return false
    }

    private fun JsonReader.nextNullableString(): String? = if (peek() == JsonToken.NULL) { skipValue(); null } else nextString()
    private fun JsonReader.nextNullableDouble(): Double? = if (peek() == JsonToken.NULL) { skipValue(); null } else nextDouble()
    private fun JsonReader.nextNullableLong(): Long? = if (peek() == JsonToken.NULL) { skipValue(); null } else nextLong()
    private fun JsonReader.nextStringList(): List<String>? {
        if (peek() == JsonToken.NULL) { skipValue(); return null }
        val list = mutableListOf<String>(); beginArray()
        while (hasNext()) list.add(nextString())
        endArray(); return list
    }
}



