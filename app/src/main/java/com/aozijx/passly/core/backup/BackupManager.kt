package com.aozijx.passly.core.backup

import android.content.Context
import android.net.Uri
import com.aozijx.passly.core.logging.Logcat
import com.aozijx.passly.data.entity.VaultEntryEntity
import com.aozijx.passly.data.repository.backup.BackupRoomDataSource
import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2Mode
import com.lambdapioneer.argon2kt.Argon2Version
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.EOFException
import java.io.File
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 备份管理（当前格式）：采用 Argon2id + AES-GCM + ZIP 容器。
 */
object BackupManager {
    private const val TAG = "BackupManager"
    private val MAGIC_NUMBER = "PASSLY".toByteArray(StandardCharsets.UTF_8)
    private const val BACKUP_VERSION = 1
    
    private const val SALT_LENGTH = 16
    private const val IV_LENGTH = 12
    private const val KEY_LENGTH_BITS = 256
    private const val KEY_LENGTH_BYTES = KEY_LENGTH_BITS / 8

    private const val ARGON2_ITERATIONS = 3
    private const val ARGON2_MEMORY_KB = 65536
    private const val ARGON2_PARALLELISM = 4

    private const val DATA_ENTRY_NAME = "data.json"
    private const val IMAGE_ENTRY_PREFIX = "images/"

    private val argon2Kt = Argon2Kt()

    suspend fun exportBackup(
        context: Context,
        uri: Uri,
        password: CharArray,
        includeImages: Boolean = true,
        dataSource: BackupDataSource = BackupRoomDataSource(context),
        imageStore: BackupImageStore = BackupVInternalImageStore(context)
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val entries = dataSource.readAllEntries()
            val preparedEntries = mutableListOf<VaultEntryEntity>()
            val imageExports = mutableListOf<Pair<String, File>>()

            entries.forEach { raw ->
                val imageZipPath = if (includeImages) {
                    val imageFile = imageStore.resolveReadable(raw.iconCustomPath)
                    imageFile?.let {
                        val imageName = "${raw.id}_${it.name}"
                        imageExports += "images/$imageName" to it
                        "images/$imageName"
                    }
                } else {
                    null
                }
                preparedEntries += BackupFieldEncryptor.toExportEntry(raw, imageZipPath)
            }

            val salt = ByteArray(SALT_LENGTH).also { SecureRandom().nextBytes(it) }
            val key = deriveKeyArgon2id(password, salt)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv

            context.contentResolver.openOutputStream(uri)?.use { output ->
                // 写入幻数标识
                output.write(MAGIC_NUMBER)
                // 写入版本
                output.write(BACKUP_VERSION)
                // 写入加密参数
                output.write(salt)
                output.write(iv)

                CipherOutputStream(output, cipher).use { encrypted ->
                    ZipOutputStream(encrypted).use { zip ->
                        zip.putNextEntry(ZipEntry("data.json"))
                        BackupVSerializer.writeEntries(zip, preparedEntries)
                        zip.closeEntry()

                        if (includeImages) {
                            imageExports.forEach { (zipPath, file) ->
                                zip.putNextEntry(ZipEntry(zipPath))
                                file.inputStream().use { input -> input.copyTo(zip) }
                                zip.closeEntry()
                            }
                        }
                    }
                }
            } ?: throw IllegalStateException("无法写入文件")

            Logcat.i(TAG, "备份导出成功")
            Result.success(Unit)
        } catch (e: Exception) {
            Logcat.e(TAG, "备份导出失败", e)
            Result.failure(e)
        }
    }

    suspend fun importBackup(
        context: Context,
        uri: Uri,
        password: CharArray,
        mode: BackupImportMode,
        dataSource: BackupDataSource = BackupRoomDataSource(context),
        imageStore: BackupImageStore = BackupVInternalImageStore(context)
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                // 1. 校验幻数
                val readMagic = ByteArray(MAGIC_NUMBER.size)
                readFullyOrThrow(input, readMagic, "magic number")
                if (!readMagic.contentEquals(MAGIC_NUMBER)) {
                    throw IllegalArgumentException("该文件不是有效的 Passly 备份文件")
                }

                // 2. 读取版本（仅支持当前格式）
                val version = readSingleByteOrThrow(input)
                if (version != BACKUP_VERSION) {
                    throw IllegalStateException("不支持的备份版本: $version（仅支持当前版本）")
                }
                importCurrentFormat(
                    encryptedInput = input,
                    password = password,
                    mode = mode,
                    dataSource = dataSource,
                    imageStore = imageStore
                )
            } ?: throw IllegalStateException("InputStream 为 null")

            Logcat.i(TAG, "备份导入成功")
            Result.success(Unit)
        } catch (e: Exception) {
            val mapped = mapImportFailure(e)
            Logcat.e(TAG, "备份导入失败: ${mapped.message}")
            Result.failure(mapped)
        }
    }

    private fun mapImportFailure(error: Exception): Exception {
        if (error is IllegalArgumentException) return error
        if (error is IllegalStateException && error.message?.contains("不支持的备份版本") == true) {
            return error
        }

        var current: Throwable? = error
        while (current != null) {
            if (current is AEADBadTagException || current.message?.contains("BAD_DECRYPT") == true || current.message?.contains("tag mismatch") == true) {
                return Exception("备份密码错误，请核对后重试", error)
            }
            current = current.cause
        }
        if (error is EOFException) return Exception("备份文件损坏或格式不正确", error)
        return error
    }

    private suspend fun importCurrentFormat(
        encryptedInput: InputStream,
        password: CharArray,
        mode: BackupImportMode,
        dataSource: BackupDataSource,
        imageStore: BackupImageStore
    ) {
        val salt = ByteArray(SALT_LENGTH)
        readFullyOrThrow(encryptedInput, salt, "salt")
        val iv = ByteArray(IV_LENGTH)
        readFullyOrThrow(encryptedInput, iv, "iv")

        val key = deriveKeyArgon2id(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))

        val imageRestoredPaths = mutableMapOf<String, String>()
        var plainEntries = emptyList<VaultEntryEntity>()

        CipherInputStream(encryptedInput, cipher).use { encryptedIn ->
            ZipInputStream(encryptedIn).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val name = entry.name
                    when {
                        name == DATA_ENTRY_NAME -> {
                            plainEntries = BackupVSerializer.readEntries(zip)
                        }
                        name.startsWith(IMAGE_ENTRY_PREFIX) -> {
                            val fileName = name.substringAfterLast('/')
                            val restored = imageStore.saveFromBackup(fileName, zip)
                            if (restored != null) imageRestoredPaths[name] = restored
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }

        if (plainEntries.isEmpty()) {
            throw IllegalArgumentException("备份内容缺少 data.json 或内容为空")
        }

        val encryptedEntries = plainEntries.map { plain ->
            val restoredPath = plain.iconCustomPath?.let { imageRestoredPaths[it] }
            BackupFieldEncryptor.toImportEntry(plain, restoredPath)
        }
        dataSource.writeEntries(encryptedEntries, mode)
    }

    private fun deriveKeyArgon2id(password: CharArray, salt: ByteArray): SecretKeySpec {
        val passBytes = String(password).toByteArray(StandardCharsets.UTF_8)
        try {
            val rawHash = argon2Kt.hash(
                mode = Argon2Mode.ARGON2_ID,
                password = passBytes,
                salt = salt,
                tCostInIterations = ARGON2_ITERATIONS,
                mCostInKibibyte = ARGON2_MEMORY_KB,
                parallelism = ARGON2_PARALLELISM,
                hashLengthInBytes = KEY_LENGTH_BYTES,
                version = Argon2Version.V13
            )
            val rawKey = rawHash.rawHashAsByteArray()
            return try {
                SecretKeySpec(rawKey, "AES")
            } finally {
                rawKey.fill(0)
            }
        } finally {
            passBytes.fill(0)
        }
    }

    private fun readSingleByteOrThrow(input: InputStream): Int {
        val value = input.read()
        if (value == -1) throw EOFException("文件损坏: 无法读取版本号")
        return value and 0xFF
    }

    private fun readFullyOrThrow(input: InputStream, target: ByteArray, fieldName: String) {
        var offset = 0
        while (offset < target.size) {
            val count = input.read(target, offset, target.size - offset)
            if (count == -1) throw EOFException("文件损坏: $fieldName 不完整")
            offset += count
        }
    }
}
