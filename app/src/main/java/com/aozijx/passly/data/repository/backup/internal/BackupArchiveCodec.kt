package com.aozijx.passly.data.repository.backup.internal

import com.aozijx.passly.core.backup.BackupManager
import com.aozijx.passly.core.security.KeyDerivation
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

internal data class BackupArchiveContent(
    val snapshotJson: ByteArray,
    val images: Map<String, ByteArray>
)

internal object BackupArchiveCodec {
    private const val MAX_ARCHIVE_BYTES = 128 * 1024 * 1024
    private const val MAX_ENTRY_BYTES = 16 * 1024 * 1024

    fun encode(
        content: BackupArchiveContent,
        password: CharArray,
        deriveKey: (CharArray, ByteArray) -> SecretKeySpec = KeyDerivation::deriveKeyArgon2id
    ): ByteArray {
        val plainArchive = buildZip(content)
        val salt = KeyDerivation.generateSalt()
        val key = deriveKey(password, salt)
        return try {
            val cipher = BackupManager.getCipher(Cipher.ENCRYPT_MODE, key)
            val ciphertext = cipher.doFinal(plainArchive)
            ByteArrayOutputStream().use { bytes ->
                DataOutputStream(bytes).use { output ->
                    output.write(BackupManager.MAGIC_NUMBER)
                    output.writeInt(BackupManager.BACKUP_VERSION)
                    output.write(salt)
                    output.write(cipher.iv)
                    output.writeInt(ciphertext.size)
                    output.write(ciphertext)
                }
                bytes.toByteArray()
            }
        } finally {
            key.encoded.fill(0)
            plainArchive.fill(0)
        }
    }

    fun decode(
        encoded: ByteArray,
        password: CharArray,
        deriveKey: (CharArray, ByteArray) -> SecretKeySpec = KeyDerivation::deriveKeyArgon2id
    ): BackupArchiveContent {
        require(encoded.size <= MAX_ARCHIVE_BYTES) { "备份文件过大" }
        val input = DataInputStream(ByteArrayInputStream(encoded))
        val magic = ByteArray(BackupManager.MAGIC_NUMBER.size)
        BackupManager.readFullyOrThrow(input, magic, "magic")
        require(magic.contentEquals(BackupManager.MAGIC_NUMBER)) { "不支持的备份文件格式" }
        val version = input.readInt()
        require(version == BackupManager.BACKUP_VERSION) { "不支持的备份版本: $version" }
        val salt = ByteArray(BackupManager.SALT_LENGTH)
        val iv = ByteArray(BackupManager.IV_LENGTH)
        BackupManager.readFullyOrThrow(input, salt, "salt")
        BackupManager.readFullyOrThrow(input, iv, "nonce")
        val ciphertextSize = input.readInt()
        require(ciphertextSize in 1..MAX_ARCHIVE_BYTES) { "备份密文长度无效" }
        val ciphertext = ByteArray(ciphertextSize)
        BackupManager.readFullyOrThrow(input, ciphertext, "ciphertext")
        require(input.read() == -1) { "备份文件包含多余数据" }

        val key = deriveKey(password, salt)
        val plainArchive = try {
            BackupManager.getCipher(Cipher.DECRYPT_MODE, key, iv).doFinal(ciphertext)
        } finally {
            key.encoded.fill(0)
            ciphertext.fill(0)
        }
        return try {
            readZip(plainArchive)
        } finally {
            plainArchive.fill(0)
        }
    }

    private fun buildZip(content: BackupArchiveContent): ByteArray =
        ByteArrayOutputStream().use { bytes ->
            ZipOutputStream(bytes).use { zip ->
                zip.putNextEntry(ZipEntry(BackupManager.DATA_ENTRY_NAME))
                zip.write(content.snapshotJson)
                zip.closeEntry()
                content.images.toSortedMap().forEach { (name, data) ->
                    validateImageEntryName(name)
                    require(data.size <= MAX_ENTRY_BYTES) { "图片过大: $name" }
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(data)
                    zip.closeEntry()
                }
            }
            bytes.toByteArray()
        }

    private fun readZip(bytes: ByteArray): BackupArchiveContent {
        var snapshotJson: ByteArray? = null
        val images = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                require(!entry.isDirectory) { "备份中不允许目录条目" }
                when (entry.name) {
                    BackupManager.DATA_ENTRY_NAME -> {
                        require(snapshotJson == null) { "备份包含重复数据条目" }
                        snapshotJson = zip.readLimited(MAX_ARCHIVE_BYTES)
                    }
                    else -> {
                        validateImageEntryName(entry.name)
                        require(images.put(entry.name, zip.readLimited(MAX_ENTRY_BYTES)) == null) {
                            "备份包含重复图片条目"
                        }
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return BackupArchiveContent(
            snapshotJson = requireNotNull(snapshotJson) { "备份缺少数据条目" },
            images = images
        )
    }

    private fun validateImageEntryName(name: String) {
        require(name.startsWith(BackupManager.IMAGE_ENTRY_PREFIX)) { "未知备份条目: $name" }
        require(name.length > BackupManager.IMAGE_ENTRY_PREFIX.length) { "图片条目名称为空" }
        require(".." !in name && '\\' !in name && !name.startsWith('/')) {
            "非法备份路径: $name"
        }
    }

    private fun ZipInputStream.readLimited(maxBytes: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0
        while (true) {
            val read = read(buffer)
            if (read == -1) break
            total += read
            require(total <= maxBytes) { "备份条目超过大小限制" }
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }
}
