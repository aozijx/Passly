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
import com.aozijx.passly.core.backup.BackupManager.readFullyOrThrow
import com.aozijx.passly.core.backup.BackupManager.readSingleByteOrThrow
import com.aozijx.passly.core.backup.EmergencyBackupExporter
import com.aozijx.passly.data.dto.VaultPayload
import com.aozijx.passly.data.repository.backup.internal.BackupFieldEncryptor
import com.aozijx.passly.data.repository.backup.internal.BackupVInternalImageStore
import com.aozijx.passly.data.repository.backup.internal.BackupVSerializer
import com.aozijx.passly.data.repository.backup.internal.mapToAppError
import com.aozijx.passly.data.repository.backup.internal.openBackupInputStream
import com.aozijx.passly.data.repository.backup.internal.openBackupOutputStream
import com.aozijx.passly.domain.model.BackupImportMode
import com.aozijx.passly.domain.model.BackupException
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

internal class BackupRepositoryImpl(
    private val context: Context,
    private val dataSource: BackupDataSource = BackupRoomDataSource(context),
    private val imageStore: BackupVInternalImageStore = BackupVInternalImageStore(context)
) : BackupRepository {

    override suspend fun exportEncryptedBackup(
        uri: Uri,
        password: CharArray,
        includeImages: Boolean
    ) = withContext(Dispatchers.IO) {
        try {
            val entities = dataSource.readAllEntries()
            val exportPayloads = mutableListOf<VaultPayload>()
            val imageExports = mutableListOf<Pair<String, File>>()

            entities.forEach { entity ->
                val payload = BackupFieldEncryptor.toExportPayload(entity, null)
                val imageZipPath = if (includeImages) {
                    imageStore.resolveReadable(payload.iconCustomPath)?.let {
                        val path = "${IMAGE_ENTRY_PREFIX}${entity.id}_${it.name}"
                        imageExports += path to it
                        path
                    }
                } else null
                exportPayloads += if (imageZipPath != null) payload.copy(iconCustomPath = imageZipPath) else payload
            }

            val salt = generateSalt()
            val key = deriveKeyArgon2id(password, salt)
            val cipher = getCipher(Cipher.ENCRYPT_MODE, key)
            val iv = cipher.iv

            context.openBackupOutputStream(uri).use { output ->
                output.write(MAGIC_NUMBER)
                output.write(BACKUP_VERSION)
                output.write(salt)
                output.write(iv)

                CipherOutputStream(output, cipher).use { encrypted ->
                    ZipOutputStream(encrypted).use { zip ->
                        zip.putNextEntry(ZipEntry(DATA_ENTRY_NAME))
                        BackupVSerializer.writeEntries(zip, exportPayloads)
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
            }

        } catch (e: Throwable) {
            throw mapToAppError("backup.export.encrypted", e)
        }
    }

    override suspend fun exportPlainBackup(uri: Uri) = withContext(Dispatchers.IO) {
        try {
            val entities = dataSource.readAllEntries()
            val payloads = entities.map { BackupFieldEncryptor.toExportPayload(it, null) }
            context.openBackupOutputStream(uri).use { output ->
                BackupVSerializer.writeEntries(output, payloads)
            }
        } catch (e: Throwable) {
            throw mapToAppError("backup.export.plain", e)
        }
    }

    override suspend fun exportEmergencyBackup(): File = withContext(Dispatchers.IO) {
        try {
            EmergencyBackupExporter.exportOnFailure(context).getOrElse {
                throw mapToAppError("backup.export.emergency", it)
            }
        } catch (e: Throwable) {
            throw mapToAppError("backup.export.emergency", e)
        }
    }

    override suspend fun importBackup(
        uri: Uri,
        password: CharArray,
        mode: BackupImportMode
    ) = withContext(Dispatchers.IO) {
        try {
            context.openBackupInputStream(uri).use { input ->
                val readMagic = ByteArray(MAGIC_NUMBER.size)
                val readCount = input.read(readMagic)
                val isEncrypted = readCount == MAGIC_NUMBER.size &&
                        readMagic.contentEquals(MAGIC_NUMBER)

                if (isEncrypted) {
                    val version = readSingleByteOrThrow(input)
                    if (version != BACKUP_VERSION) error("不支持的备份版本: $version")
                    importEncryptedStream(input, password, mode)
                } else {
                    importPlainJson(uri, mode)
                }
            }
        } catch (e: Throwable) {
            throw mapToAppError("backup.import", e)
        }
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
        var plainPayloads = emptyList<VaultPayload>()

        CipherInputStream(encryptedInput, cipher).use { encryptedIn ->
            ZipInputStream(encryptedIn).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    when {
                        entry.name == DATA_ENTRY_NAME -> {
                            plainPayloads = BackupVSerializer.readEntries(zip)
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

        if (plainPayloads.isEmpty()) throw BackupException.FileCorrupted()

        val entities = plainPayloads.map { payload ->
            val restoredIcon = imageRestoredPaths[payload.iconCustomPath]
            val finalPayload = if (restoredIcon != null) payload.copy(iconCustomPath = restoredIcon) else payload
            BackupFieldEncryptor.toImportEntity(finalPayload)
        }
        dataSource.writeEntries(entities, mode)
    }

    private suspend fun importPlainJson(uri: Uri, mode: BackupImportMode) {
        val plainPayloads = context.openBackupInputStream(uri).use {
            BackupVSerializer.readEntries(it)
        }

        val entities = plainPayloads.map { BackupFieldEncryptor.toImportEntity(it) }
        dataSource.writeEntries(entities, mode)
    }

    override suspend fun testDirectoryWritePermission(directoryUri: String) {
        BackupExportStorageSupport.testWritePermission(context, directoryUri)
            .onFailure { throw mapToAppError("backup.permission.test", it) }
    }
}