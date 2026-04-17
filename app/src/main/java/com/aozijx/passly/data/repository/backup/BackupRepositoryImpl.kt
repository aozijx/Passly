package com.aozijx.passly.data.repository.backup

import android.content.Context
import android.net.Uri
import com.aozijx.passly.core.backup.BackupExportStorageSupport
import com.aozijx.passly.core.backup.BackupManager.BACKUP_VERSION
import com.aozijx.passly.core.backup.BackupManager.DATA_ENTRY_NAME
import com.aozijx.passly.core.backup.BackupManager.IMAGE_ENTRY_PREFIX
import com.aozijx.passly.core.backup.BackupManager.IV_LENGTH
import com.aozijx.passly.core.backup.BackupManager.MAGIC_NUMBER
import com.aozijx.passly.core.backup.BackupManager.SALT_LENGTH
import com.aozijx.passly.core.backup.BackupManager.deriveKeyArgon2id
import com.aozijx.passly.core.backup.BackupManager.generateSalt
import com.aozijx.passly.core.backup.BackupManager.getCipher
import com.aozijx.passly.core.backup.BackupManager.mapImportFailure
import com.aozijx.passly.core.backup.BackupManager.readFullyOrThrow
import com.aozijx.passly.core.backup.BackupManager.readSingleByteOrThrow
import com.aozijx.passly.core.backup.EmergencyBackupExporter
import com.aozijx.passly.data.entity.VaultEntryEntity
import com.aozijx.passly.data.repository.backup.internal.BackupFieldEncryptor
import com.aozijx.passly.data.repository.backup.internal.BackupVInternalImageStore
import com.aozijx.passly.data.repository.backup.internal.BackupVSerializer
import com.aozijx.passly.domain.model.backup.BackupImportMode
import com.aozijx.passly.domain.model.core.BackupException
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
 */
internal class BackupRepositoryImpl(
    private val context: Context,
    private val dataSource: BackupDataSource = BackupRoomDataSource(context),
    private val imageStore: BackupVInternalImageStore = BackupVInternalImageStore(context)
) : BackupRepository {

    override suspend fun exportEncryptedBackup(
        uri: Uri,
        password: CharArray,
        includeImages: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val runResult = runCatching {
            val entries = dataSource.readAllEntries()
            val preparedEntries = mutableListOf<VaultEntryEntity>()
            val imageExports = mutableListOf<Pair<String, File>>()

            entries.forEach { raw ->
                val imageZipPath = if (includeImages) {
                    imageStore.resolveReadable(raw.iconCustomPath)?.let {
                        val path = "${IMAGE_ENTRY_PREFIX}${raw.id}_${it.name}"
                        imageExports += path to it
                        path
                    }
                } else null
                preparedEntries += BackupFieldEncryptor.toExportEntry(raw, imageZipPath)
            }

            val salt = generateSalt()
            val key = deriveKeyArgon2id(password, salt)
            val cipher = getCipher(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv

            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(MAGIC_NUMBER)
                output.write(BACKUP_VERSION)
                output.write(salt)
                output.write(iv)

                CipherOutputStream(output, cipher).use { encrypted ->
                    ZipOutputStream(encrypted).use { zip ->
                        zip.putNextEntry(ZipEntry(DATA_ENTRY_NAME))
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
            } ?: throw BackupException.StoragePermissionDenied()
        }
        
        runResult.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { e -> Result.failure(e as? BackupException ?: BackupException.Unknown(e)) }
        )
    }

    override suspend fun exportPlainBackup(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        val runResult = runCatching {
            val entries = dataSource.readAllEntries()
            val decryptedEntries = entries.map { BackupFieldEncryptor.toExportEntry(it, it.iconCustomPath) }
            context.contentResolver.openOutputStream(uri)?.use { output ->
                BackupVSerializer.writeEntries(output, decryptedEntries)
            } ?: throw BackupException.StoragePermissionDenied()
        }

        runResult.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { e -> Result.failure(e as? BackupException ?: BackupException.Unknown(e)) }
        )
    }

    override suspend fun exportEmergencyBackup(): Result<File> = withContext(Dispatchers.IO) {
        EmergencyBackupExporter.exportOnFailure(context)
    }

    override suspend fun importBackup(
        uri: Uri,
        password: CharArray,
        mode: BackupImportMode
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val runResult = runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val readMagic = ByteArray(MAGIC_NUMBER.size)
                val readCount = input.read(readMagic)
                val isEncrypted = readCount == MAGIC_NUMBER.size &&
                        readMagic.contentEquals(MAGIC_NUMBER)

                if (isEncrypted) {
                    val version = readSingleByteOrThrow(input)
                    if (version != BACKUP_VERSION) error("不支持的备份版本: $version")
                    importEncryptedStream(input, password, mode)
                } else {
                    importEmergencyJson(uri, mode)
                }
            } ?: throw BackupException.FileCorrupted()
        }

        runResult.fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { e -> Result.failure(mapException(e)) }
        )
    }

    private suspend fun importEncryptedStream(
        encryptedInput: InputStream,
        password: CharArray,
        mode: BackupImportMode
    ) {
        val salt = ByteArray(SALT_LENGTH)
        readFullyOrThrow(encryptedInput, salt, "salt")
        val iv = ByteArray(IV_LENGTH)
        readFullyOrThrow(encryptedInput, iv, "iv")

        val key = deriveKeyArgon2id(password, salt)
        val cipher = getCipher(Cipher.DECRYPT_MODE, key, iv)

        val imageRestoredPaths = mutableMapOf<String, String>()
        var plainEntries = emptyList<VaultEntryEntity>()

        CipherInputStream(encryptedInput, cipher).use { encryptedIn ->
            ZipInputStream(encryptedIn).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    when {
                        entry.name == DATA_ENTRY_NAME -> {
                            plainEntries = BackupVSerializer.readEntries(zip)
                        }
                        entry.name.startsWith(IMAGE_ENTRY_PREFIX) -> {
                            val fileName = entry.name.substringAfterLast('/')
                            imageStore.saveFromBackup(fileName, zip)?.let { imageRestoredPaths[entry.name] = it }
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }

        if (plainEntries.isEmpty()) throw BackupException.FileCorrupted()

        val finalEntries = plainEntries.map { plain ->
            BackupFieldEncryptor.toImportEntry(plain, imageRestoredPaths[plain.iconCustomPath])
        }
        dataSource.writeEntries(finalEntries, mode)
    }

    private suspend fun importEmergencyJson(uri: Uri, mode: BackupImportMode) {
        val entries = context.contentResolver.openInputStream(uri)?.use { 
            BackupVSerializer.readEntries(it) 
        } ?: throw BackupException.FileCorrupted()
        dataSource.writeEntries(entries, mode)
    }

    private fun mapException(e: Throwable): BackupException {
        val raw = mapImportFailure(e as? Exception ?: Exception(e))
        return when {
            raw.message?.contains("密码错误") == true -> BackupException.PasswordIncorrect()
            raw.message?.contains("文件损坏") == true -> BackupException.FileCorrupted()
            e is BackupException -> e
            else -> BackupException.Unknown(e)
        }
    }

    override suspend fun testDirectoryWritePermission(directoryUri: String): Result<Unit> {
        return BackupExportStorageSupport.testWritePermission(context, directoryUri).fold(
            onSuccess = { Result.success(Unit) },
            onFailure = { Result.failure(it) }
        )
    }
}