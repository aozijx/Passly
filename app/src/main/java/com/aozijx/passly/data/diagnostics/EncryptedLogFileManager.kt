package com.aozijx.passly.data.diagnostics

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * 加密日志文件管理 —— 负责文件头读写、AAD 构建、密钥管理、文件清理。
 */
internal class EncryptedLogFileManager(
    private val directory: File,
    private val random: SecureRandom,
    private val writeLock: Any
) {
    private val dateFormatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)

    @Volatile
    var activeFileEntry: FileEntry? = null

    class FileEntry(
        val file: File,
        val fileId: ByteArray,
        val createdAtMs: Long,
        val dataKey: ByteArray,
        val wrapNonce: ByteArray,
        val wrappedKey: ByteArray,
        /** 记录 AAD 前缀：magic(4) + version(4) + fileId(16) = 24 字节 */
        val recordAadPrefix: ByteArray
    ) {
        fun destroyDataKey() {
            dataKey.fill(0)
        }
    }

    fun currentFileEntry(): FileEntry {
        val prefix = "log_${dateFormatter.format(java.util.Date())}"
        val current = activeFileEntry
        if (current != null && current.file.exists() &&
            current.file.name.startsWith(prefix) &&
            current.file.length() < MAX_FILE_BYTES
        ) {
            return current
        }
        cleanup()
        // 重用未满的同日期文件
        val reusable = directory.listFiles()
            ?.filter { it.name.startsWith(prefix) && it.extension == LOG_EXTENSION }
            ?.filter { it.length() < MAX_FILE_BYTES }
            ?.maxByOrNull(File::lastModified)
        if (reusable != null) {
            val ctx = readHeader(reusable)
            val entry = FileEntry(
                file = reusable,
                fileId = ctx.fileId,
                createdAtMs = ctx.createdAtMs,
                dataKey = ctx.dataKey.copyOf(),
                wrapNonce = ByteArray(0),
                wrappedKey = ByteArray(0),
                recordAadPrefix = buildRecordAadPrefix(ctx.fileId)
            )
            ctx.destroy()
            activeFileEntry = entry
            return entry
        }
        // 创建新文件
        val now = System.currentTimeMillis()
        val file = File(directory, "${prefix}_$now.$LOG_EXTENSION")
        val entry = createNewFileEntry(file, now)
        try {
            writeHeader(file, entry)
            activeFileEntry?.destroyDataKey()
            activeFileEntry = entry
        } catch (e: Throwable) {
            entry.destroyDataKey()
            throw e
        }
        return entry
    }

    fun createNewFileEntry(file: File, createdAtMs: Long): FileEntry {
        val fileId = ByteArray(LogFileContext.FILE_ID_BYTES).also(random::nextBytes)
        val dataKey = ByteArray(DATA_KEY_BYTES).also(random::nextBytes)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateWrappingKey())
        val headerAad = buildHeaderAad(fileId, createdAtMs)
        cipher.updateAAD(headerAad)
        return FileEntry(
            file = file,
            fileId = fileId,
            createdAtMs = createdAtMs,
            dataKey = dataKey,
            wrapNonce = cipher.iv.copyOf(),
            wrappedKey = cipher.doFinal(dataKey),
            recordAadPrefix = buildRecordAadPrefix(fileId)
        )
    }

    fun buildRecordAadPrefix(fileId: ByteArray): ByteArray {
        val buf = ByteArray(24)
        var off = 0
        off = writeIntBE(buf, off, FILE_MAGIC)
        off = writeIntBE(buf, off, FILE_VERSION)
        fileId.copyInto(buf, off)
        return buf
    }

    fun buildHeaderAad(fileId: ByteArray, createdAtMs: Long): ByteArray {
        val buf = ByteArray(32)
        var off = 0
        off = writeIntBE(buf, off, FILE_MAGIC)
        off = writeIntBE(buf, off, FILE_VERSION)
        fileId.copyInto(buf, off); off += LogFileContext.FILE_ID_BYTES
        writeLongBE(buf, off, createdAtMs)
        return buf
    }

    fun buildRecordAad(fileId: ByteArray, sequence: Long, level: Int): ByteArray {
        val buf = ByteArray(33)
        var off = 0
        off = writeIntBE(buf, off, FILE_MAGIC)
        off = writeIntBE(buf, off, FILE_VERSION)
        fileId.copyInto(buf, off); off += LogFileContext.FILE_ID_BYTES
        off = writeLongBE(buf, off, sequence)
        buf[off] = level.toByte()
        return buf
    }

    fun writeHeader(file: File, entry: FileEntry) {
        DataOutputStream(FileOutputStream(file, false).buffered()).use { out ->
            out.writeInt(FILE_MAGIC)
            out.writeInt(FILE_VERSION)
            out.write(entry.fileId)
            out.writeLong(entry.createdAtMs)
            out.writeInt(entry.wrapNonce.size)
            out.write(entry.wrapNonce)
            out.writeInt(entry.wrappedKey.size)
            out.write(entry.wrappedKey)
        }
    }

    fun readHeader(file: File): LogFileContext =
        DataInputStream(FileInputStream(file).buffered()).use { readHeader(it) }

    fun readHeader(input: DataInputStream): LogFileContext {
        require(input.readInt() == FILE_MAGIC) { "Unknown diagnostics file" }
        val version = input.readInt()
        require(version == FILE_VERSION) { "Unsupported diagnostics version $version" }
        val fileId = ByteArray(LogFileContext.FILE_ID_BYTES).also(input::readFully)
        val createdAtMs = input.readLong()
        val wrapNonce =
            ByteArray(input.readInt().also { require(it == NONCE_BYTES) }).also(input::readFully)
        val wrappedKey =
            ByteArray(input.readInt().checkedMax(MAX_WRAPPED_KEY_BYTES)).also(input::readFully)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateWrappingKey(),
            GCMParameterSpec(TAG_BITS, wrapNonce)
        )
        val headerAad = buildHeaderAad(fileId, createdAtMs)
        cipher.updateAAD(headerAad)
        val dataKey = cipher.doFinal(wrappedKey)
        require(dataKey.size == DATA_KEY_BYTES) { "Invalid diagnostics data key length" }
        return LogFileContext(fileId, createdAtMs, version, dataKey)
    }

    fun readHeader(input: RandomAccessFile): LogFileContext {
        require(input.readInt() == FILE_MAGIC) { "Unknown diagnostics file" }
        val version = input.readInt()
        require(version == FILE_VERSION) { "Unsupported diagnostics version $version" }
        val fileId = ByteArray(LogFileContext.FILE_ID_BYTES).also(input::readFully)
        val createdAtMs = input.readLong()
        val wrapNonce =
            ByteArray(input.readInt().checkedExact(NONCE_BYTES)).also(input::readFully)
        val wrappedKey =
            ByteArray(input.readInt().checkedMax(MAX_WRAPPED_KEY_BYTES)).also(input::readFully)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateWrappingKey(),
            GCMParameterSpec(TAG_BITS, wrapNonce)
        )
        cipher.updateAAD(buildHeaderAad(fileId, createdAtMs))
        val dataKey = cipher.doFinal(wrappedKey)
        require(dataKey.size == DATA_KEY_BYTES) { "Invalid diagnostics data key length" }
        return LogFileContext(fileId, createdAtMs, version, dataKey)
    }

    fun skipHeader(input: DataInputStream) {
        val magic = input.readInt()
        require(magic == FILE_MAGIC) { "Unknown diagnostics file" }
        val version = input.readInt()
        require(version == FILE_VERSION) { "Unsupported diagnostics version $version" }
        input.readFully(ByteArray(LogFileContext.FILE_ID_BYTES))
        input.readLong()
        val wrapNonceLen = input.readInt().checkedExact(NONCE_BYTES)
        input.readFully(ByteArray(wrapNonceLen))
        val wrappedLen = input.readInt().checkedMax(MAX_WRAPPED_KEY_BYTES)
        input.readFully(ByteArray(wrappedLen))
    }

    fun countRecordsInFile(file: File): Int {
        return runCatching {
            DataInputStream(FileInputStream(file).buffered()).use { input ->
                skipHeader(input)
                var count = 0
                while (true) {
                    try {
                        val nonceLen = input.readInt().checkedExact(NONCE_BYTES)
                        input.readFully(ByteArray(nonceLen))
                        input.readLong()
                        input.readUnsignedByte()
                        val cipherLen = input.readInt().checkedMax(MAX_RECORD_BYTES)
                        require(cipherLen >= GCM_TAG_BYTES)
                        input.readFully(ByteArray(cipherLen))
                        count++
                    } catch (_: EOFException) {
                        break
                    }
                }
                count
            }
        }.getOrDefault(0)
    }

    fun totalRecordCount(files: List<File>): Int =
        files.sumOf(::countRecordsInFile)

    fun cursorForNextReadableFile(
        files: List<File>,
        startIndex: Int
    ): LogCursor? {
        for (fileIndex in startIndex until files.size) {
            val context = runCatching { readHeader(files[fileIndex]) }.getOrNull() ?: continue
            return try {
                LogCursor(
                    fileIndex = fileIndex,
                    fileId = bytesToHex(context.fileId),
                    nextRecordOffset = 0,
                    nextSequence = 1
                )
            } finally {
                context.destroy()
            }
        }
        return null
    }

    fun cleanup() {
        val files = directory.listFiles()
            ?.filter { it.extension == LOG_EXTENSION && it.isFile }
            .orEmpty()
        val cutoff = System.currentTimeMillis() - RETENTION_MS
        files.filter { it.lastModified() < cutoff }.forEach(File::delete)
        val remaining = directory.listFiles()
            ?.filter { it.extension == LOG_EXTENSION && it.isFile }
            .orEmpty()
        remaining.sortedByDescending(File::lastModified).drop(MAX_FILES).forEach(File::delete)
        val postDeletion = directory.listFiles()
            ?.filter { it.extension == LOG_EXTENSION && it.isFile }
            .orEmpty()
        var total = postDeletion.sumOf(File::length)
        postDeletion.sortedBy(File::lastModified).forEach { file ->
            if (total > MAX_TOTAL_BYTES) {
                val length = file.length()
                if (file.delete()) total -= length
            }
        }
    }

    fun recordCountFor(file: File): Int = countRecordsInFile(file)

    fun copyEntry(entry: FileEntry) = FileEntry(
        file = entry.file,
        fileId = entry.fileId.copyOf(),
        createdAtMs = entry.createdAtMs,
        dataKey = entry.dataKey.copyOf(),
        wrapNonce = entry.wrapNonce.copyOf(),
        wrappedKey = entry.wrappedKey.copyOf(),
        recordAadPrefix = entry.recordAadPrefix
    )

    private fun getOrCreateWrappingKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            javax.crypto.KeyGenerator.getInstance("AES", "AndroidKeyStore").apply {
                init(
                    android.security.keystore.KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or
                                android.security.keystore.KeyProperties.PURPOSE_DECRYPT
                    )
                        .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(
                            android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE
                        )
                        .setKeySize(256)
                        .setUserAuthenticationRequired(false)
                        .build()
                )
            }.generateKey()
        }
        return (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        var i = 0
        while (i < bytes.size) {
            val v = bytes[i].toInt() and 0xFF
            hexChars[i * 2] = HEX_DIGITS[v ushr 4]
            hexChars[i * 2 + 1] = HEX_DIGITS[v and 0x0F]
            i++
        }
        return String(hexChars)
    }

    private fun Int.checkedMax(max: Int): Int =
        also { require(it in 1..max) { "Length $it exceeds max $max" } }

    private fun Int.checkedExact(expected: Int): Int =
        also { require(it == expected) { "Expected $expected but got $it" } }

    companion object {
        val HEX_DIGITS = "0123456789abcdef".toCharArray()

        fun writeIntBE(buf: ByteArray, off: Int, value: Int): Int {
            buf[off] = (value ushr 24).toByte()
            buf[off + 1] = (value ushr 16).toByte()
            buf[off + 2] = (value ushr 8).toByte()
            buf[off + 3] = value.toByte()
            return off + 4
        }

        fun writeLongBE(buf: ByteArray, off: Int, value: Long): Int {
            buf[off] = (value ushr 56).toByte()
            buf[off + 1] = (value ushr 48).toByte()
            buf[off + 2] = (value ushr 40).toByte()
            buf[off + 3] = (value ushr 32).toByte()
            buf[off + 4] = (value ushr 24).toByte()
            buf[off + 5] = (value ushr 16).toByte()
            buf[off + 6] = (value ushr 8).toByte()
            buf[off + 7] = value.toByte()
            return off + 8
        }

        const val LOG_EXTENSION = "elog1"
        const val KEY_ALIAS = "com.aozijx.passly.diagnostics.wrap.v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val FILE_MAGIC = 0x504C4447
        const val FILE_VERSION = 1
        const val NONCE_BYTES = 12
        const val TAG_BITS = 128
        const val GCM_TAG_BYTES = TAG_BITS / 8
        const val DATA_KEY_BYTES = 32
        const val MAX_WRAPPED_KEY_BYTES = 128
        const val MAX_RECORD_BYTES = 64 * 1024
        const val MAX_FILE_BYTES = 1024 * 1024L
        const val MAX_TOTAL_BYTES = 3 * 1024 * 1024L
        const val MAX_FILES = 3
        const val RETENTION_MS = 3 * 24 * 60 * 60 * 1000L
    }
}