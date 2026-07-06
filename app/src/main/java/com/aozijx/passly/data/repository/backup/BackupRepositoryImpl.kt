package com.aozijx.passly.data.repository.backup

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.room.withTransaction
import com.aozijx.passly.core.backup.BackupManager.DATA_ENTRY_NAME
import com.aozijx.passly.core.backup.BackupManager.IV_LENGTH
import com.aozijx.passly.core.backup.BackupManager.MAGIC_NUMBER
import com.aozijx.passly.core.backup.BackupManager.SALT_LENGTH
import com.aozijx.passly.core.backup.BackupManager.deriveKeyArgon2id
import com.aozijx.passly.core.backup.BackupManager.generateSalt
import com.aozijx.passly.core.backup.BackupManager.getCipher
import com.aozijx.passly.core.backup.EmergencyBackupExporter
import com.aozijx.passly.core.di.IoDispatcher
import com.aozijx.passly.core.error.AppResult
import com.aozijx.passly.data.repository.backup.internal.BackupFieldEncryptor
import com.aozijx.passly.data.repository.backup.internal.BackupVSerializer
import com.aozijx.passly.domain.model.BackupImportMode
import com.aozijx.passly.domain.repository.backup.BackupRepository
import com.aozijx.passly.security.crypto.CryptoEngine
import com.aozijx.passly.security.crypto.DatabaseSessionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.SequenceInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.inject.Inject

internal class BackupRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val cryptoEngine: CryptoEngine,
    private val sessionManager: DatabaseSessionManager,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : BackupRepository {

    override suspend fun exportEncryptedBackup(
        uri: Uri,
        password: CharArray,
        includeImages: Boolean
    ): AppResult<Unit> = withContext(ioDispatcher) {
        AppResult.runSuspendCatching("backup.export.encrypted") {
            val entities = sessionManager.withDatabase {
                vaultEntryDao().getAll()
            }
            val exportPayloads = entities.map { BackupFieldEncryptor.toExportPayload(it, null) }

            val salt = generateSalt()
            val key = deriveKeyArgon2id(password, salt)
            val cipher = getCipher(Cipher.ENCRYPT_MODE, key)

            val byteArrayOutputStream = ByteArrayOutputStream()
            BackupVSerializer.writeEntries(byteArrayOutputStream, exportPayloads)
            val backupData = byteArrayOutputStream.toByteArray()

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                ZipOutputStream(outputStream).use { zipOut ->
                    zipOut.putNextEntry(ZipEntry(DATA_ENTRY_NAME))
                    // 明文写入 salt 和 iv（解密必需）
                    zipOut.write(salt)
                    zipOut.write(cipher.iv)
                    // 加密写入 magic number 和数据
                    CipherOutputStream(zipOut, cipher).use { cipherOut ->
                        cipherOut.write(MAGIC_NUMBER)
                        cipherOut.write(backupData)
                    }
                    zipOut.closeEntry()
                }
            }
            Unit
        }
    }

    override suspend fun exportPlainBackup(uri: Uri): AppResult<Unit> =
        withContext(ioDispatcher) {
            AppResult.runSuspendCatching("backup.export.plain") {
                val entities = sessionManager.withDatabase {
                    vaultEntryDao().getAll()
                }
                val payloads = entities.map { BackupFieldEncryptor.toExportPayload(it, null) }

                context.contentResolver.openOutputStream(uri)?.use {
                    BackupVSerializer.writeEntries(it, payloads)
                }
                Unit
            }
        }

    override suspend fun exportEmergencyBackup(): AppResult<File> =
        withContext(ioDispatcher) {
            EmergencyBackupExporter.exportOnFailure(context, cryptoEngine)
        }

    override suspend fun importBackup(
        uri: Uri,
        password: CharArray,
        mode: BackupImportMode
    ): AppResult<Unit> = withContext(ioDispatcher) {
        AppResult.runSuspendCatching("backup.import") {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                // 检测文件类型：ZIP（加密） vs JSON（明文）
                val header = ByteArray(4)
                val headerRead = inputStream.read(header)
                if (headerRead < 4) {
                    throw IllegalArgumentException("备份文件无效：文件过小")
                }

                // ZIP 文件头：PK\x03\x04 (0x50 0x4B 0x03 0x04)
                val isZipFile = header[0] == 0x50.toByte() && header[1] == 0x4B.toByte() &&
                        header[2] == 0x03.toByte() && header[3] == 0x04.toByte()

                if (isZipFile) {
                    // 加密备份导入
                    importEncryptedBackup(inputStream, header, password, mode)
                } else {
                    // 明文 JSON 导入
                    importPlainTextBackup(inputStream, header, mode)
                }
            }
            Unit
        }
    }

    private suspend fun importEncryptedBackup(
        inputStream: InputStream,
        header: ByteArray,
        password: CharArray,
        mode: BackupImportMode
    ) {
        // 将 header 前缀回流中
        val fullStream = SequenceInputStream(
            ByteArrayInputStream(header),
            inputStream
        )

        ZipInputStream(fullStream).use { zipIn ->
            var entry = zipIn.nextEntry
            while (entry != null && entry.name != DATA_ENTRY_NAME) {
                entry = zipIn.nextEntry
            }

            if (entry == null) {
                throw IllegalArgumentException("备份文件无效：未找到数据条目")
            }

            // 明文读取 salt 和 iv
            val salt = ByteArray(SALT_LENGTH)
            val saltRead = zipIn.read(salt)
            if (saltRead != SALT_LENGTH) {
                throw IllegalArgumentException("备份文件无效：盐读取失败")
            }

            val iv = ByteArray(IV_LENGTH)
            val ivRead = zipIn.read(iv)
            if (ivRead != IV_LENGTH) {
                throw IllegalArgumentException("备份文件无效：IV读取失败")
            }

            // 派生密钥并初始化解密器
            val key = deriveKeyArgon2id(password, salt)
            val cipher = getCipher(Cipher.DECRYPT_MODE, key, iv)

            // 解密剩余数据
            val decryptedBytes = CipherInputStream(zipIn, cipher).readBytes()

            // 验证 magic number
            if (decryptedBytes.size < MAGIC_NUMBER.size) {
                throw IllegalArgumentException("备份文件无效：数据过小")
            }
            val magic = decryptedBytes.sliceArray(0 until MAGIC_NUMBER.size)
            if (!magic.contentEquals(MAGIC_NUMBER)) {
                throw IllegalArgumentException("备份文件无效：魔术字节不匹配")
            }

            // 解析备份数据
            val backupData = decryptedBytes.sliceArray(MAGIC_NUMBER.size until decryptedBytes.size)
            val payloads = BackupVSerializer.readEntries(ByteArrayInputStream(backupData))

            sessionManager.withDatabase {
                withTransaction {
                    if (mode == BackupImportMode.OVERWRITE) {
                        vaultEntryDao().deleteAll()
                    }
                    payloads.forEach { payload ->
                        val entity = BackupFieldEncryptor.toImportEntity(payload)
                        vaultEntryDao().insert(entity)
                    }
                }
            }
        }
    }

    private suspend fun importPlainTextBackup(
        inputStream: InputStream,
        header: ByteArray,
        mode: BackupImportMode
    ) {
        // 将 header 前缀回流中
        val fullStream = SequenceInputStream(
            ByteArrayInputStream(header),
            inputStream
        )

        val payloads = BackupVSerializer.readEntries(fullStream)

        sessionManager.withDatabase {
            withTransaction {
                if (mode == BackupImportMode.OVERWRITE) {
                    vaultEntryDao().deleteAll()
                }
                payloads.forEach { payload ->
                    val entity = BackupFieldEncryptor.toImportEntity(payload)
                    vaultEntryDao().insert(entity)
                }
            }
        }
    }

    override suspend fun importPlainBackup(
        uri: Uri,
        mode: BackupImportMode
    ): AppResult<Unit> = withContext(ioDispatcher) {
        AppResult.runSuspendCatching("backup.import.plain") {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val payloads = BackupVSerializer.readEntries(inputStream)

                sessionManager.withDatabase {
                    withTransaction {
                        if (mode == BackupImportMode.OVERWRITE) {
                            vaultEntryDao().deleteAll()
                        }
                        payloads.forEach { payload ->
                            val entity = BackupFieldEncryptor.toImportEntity(payload)
                            vaultEntryDao().insert(entity)
                        }
                    }
                }
            }
            Unit
        }
    }

    override suspend fun testDirectoryWritePermission(directoryUri: String): AppResult<Unit> =
        withContext(ioDispatcher) {
            AppResult.runSuspendCatching("backup.testPermission") {
                val uri = directoryUri.toUri()
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(ByteArray(0))
                }
                Unit
            }
        }
}