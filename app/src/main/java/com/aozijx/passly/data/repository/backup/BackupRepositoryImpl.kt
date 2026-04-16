package com.aozijx.passly.data.repository.backup

import android.content.Context
import android.net.Uri
import com.aozijx.passly.core.backup.BackupExportStorageSupport
import com.aozijx.passly.core.backup.BackupFieldEncryptor
import com.aozijx.passly.core.backup.BackupImportMode
import com.aozijx.passly.core.backup.BackupManager
import com.aozijx.passly.core.backup.BackupVInternalImageStore
import com.aozijx.passly.core.backup.BackupVSerializer
import com.aozijx.passly.core.backup.EmergencyBackupExporter
import com.aozijx.passly.data.entity.VaultEntryEntity
import com.aozijx.passly.domain.repository.backup.BackupRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream

/**
 * 备份仓库的深度实现。
 * 已经内聚了原本散落在 BackupManager 中的业务编排逻辑。
 */
class BackupRepositoryImpl(
    private val context: Context,
    private val dataSource: BackupRoomDataSource = BackupRoomDataSource(context),
    private val imageStore: BackupVInternalImageStore = BackupVInternalImageStore(context)
) : BackupRepository {

    override suspend fun exportEncryptedBackup(
        uri: Uri,
        password: CharArray,
        includeImages: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val entries = dataSource.readAllEntries()
            val preparedEntries = mutableListOf<VaultEntryEntity>()
            val imageExports = mutableListOf<Pair<String, File>>()

            // 1. 准备数据和图片
            entries.forEach { raw ->
                val imageZipPath = if (includeImages) {
                    val imageFile = imageStore.resolveReadable(raw.iconCustomPath)
                    imageFile?.let {
                        val imageName = "${raw.id}_${it.name}"
                        val path = "${BackupManager.IMAGE_ENTRY_PREFIX}$imageName"
                        imageExports += path to it
                        path
                    }
                } else {
                    null
                }
                preparedEntries += BackupFieldEncryptor.toExportEntry(raw, imageZipPath)
            }

            // 2. 初始化加密
            val salt = BackupManager.generateSalt()
            val key = BackupManager.deriveKeyArgon2id(password, salt)
            val cipher = BackupManager.getCipher(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv

            // 3. 写入文件容器
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(BackupManager.MAGIC_NUMBER)
                output.write(BackupManager.BACKUP_VERSION)
                output.write(salt)
                output.write(iv)

                CipherOutputStream(output, cipher).use { encrypted ->
                    ZipOutputStream(encrypted).use { zip ->
                        // 写入 JSON 数据
                        zip.putNextEntry(ZipEntry(BackupManager.DATA_ENTRY_NAME))
                        BackupVSerializer.writeEntries(zip, preparedEntries)
                        zip.closeEntry()

                        // 写入图片
                        if (includeImages) {
                            imageExports.forEach { (zipPath, file) ->
                                zip.putNextEntry(ZipEntry(zipPath))
                                file.inputStream().use { input -> input.copyTo(zip) }
                                zip.closeEntry()
                            }
                        }
                    }
                }
            } ?: error("无法打开输出流")

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun exportPlainBackup(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val entries = dataSource.readAllEntries()
            // 解密所有敏感字段
            val decryptedEntries = entries.map { BackupFieldEncryptor.toExportEntry(it, it.iconCustomPath) }

            context.contentResolver.openOutputStream(uri)?.use { output ->
                BackupVSerializer.writeEntries(output, decryptedEntries)
            } ?: error("无法打开输出流")

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun exportEmergencyBackup(): Result<File> = withContext(Dispatchers.IO) {
        EmergencyBackupExporter.exportOnFailure(context)
    }

    override suspend fun importBackup(
        uri: Uri,
        password: CharArray,
        mode: BackupImportMode
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val readMagic = ByteArray(BackupManager.MAGIC_NUMBER.size)
                val readCount = input.read(readMagic)
                val isEncrypted = readCount == BackupManager.MAGIC_NUMBER.size && 
                                 readMagic.contentEquals(BackupManager.MAGIC_NUMBER)

                if (isEncrypted) {
                    val version = BackupManager.readSingleByteOrThrow(input)
                    if (version != BackupManager.BACKUP_VERSION) {
                        error("不支持的备份版本: $version")
                    }
                    importEncryptedStream(input, password, mode)
                } else {
                    importEmergencyJson(uri, mode)
                }
            } ?: error("无法打开输入流")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(BackupManager.mapImportFailure(e))
        }
    }

    private suspend fun importEncryptedStream(
        encryptedInput: InputStream,
        password: CharArray,
        mode: BackupImportMode
    ) {
        val salt = ByteArray(BackupManager.SALT_LENGTH)
        BackupManager.readFullyOrThrow(encryptedInput, salt, "salt")
        val iv = ByteArray(BackupManager.IV_LENGTH)
        BackupManager.readFullyOrThrow(encryptedInput, iv, "iv")

        val key = BackupManager.deriveKeyArgon2id(password, salt)
        val cipher = BackupManager.getCipher(Cipher.DECRYPT_MODE, key, iv)

        val imageRestoredPaths = mutableMapOf<String, String>()
        var plainEntries = emptyList<VaultEntryEntity>()

        CipherInputStream(encryptedInput, cipher).use { encryptedIn ->
            ZipInputStream(encryptedIn).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    when {
                        entry.name == BackupManager.DATA_ENTRY_NAME -> {
                            plainEntries = BackupVSerializer.readEntries(zip)
                        }
                        entry.name.startsWith(BackupManager.IMAGE_ENTRY_PREFIX) -> {
                            val fileName = entry.name.substringAfterLast('/')
                            val restored = imageStore.saveFromBackup(fileName, zip)
                            if (restored != null) imageRestoredPaths[entry.name] = restored
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }

        if (plainEntries.isEmpty()) error("备份文件内容为空")

        val finalEntries = plainEntries.map { plain ->
            val restoredPath = plain.iconCustomPath?.let { imageRestoredPaths[it] }
            BackupFieldEncryptor.toImportEntry(plain, restoredPath)
        }
        dataSource.writeEntries(finalEntries, mode)
    }

    private suspend fun importEmergencyJson(uri: Uri, mode: BackupImportMode) {
        val entries = context.contentResolver.openInputStream(uri)?.use { 
            BackupVSerializer.readEntries(it) 
        } ?: error("无法读取紧急备份")
        dataSource.writeEntries(entries, mode)
    }

    override suspend fun testDirectoryWritePermission(directoryUri: String): Result<Unit> {
        return BackupExportStorageSupport.testWritePermission(context, directoryUri).map { }
    }
}