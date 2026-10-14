package com.aozijx.passly.data.observability

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
import javax.crypto.spec.SecretKeySpec

internal data class WrappedLogKey(val nonce: ByteArray, val ciphertext: ByteArray)

internal interface LogKeyWrapper {
    fun wrap(dataKey: ByteArray, aad: ByteArray): WrappedLogKey
    fun unwrap(wrapped: WrappedLogKey, aad: ByteArray): ByteArray
}

internal object AesGcmKeyWrapping {
    fun wrap(key: SecretKey, dataKey: ByteArray, aad: ByteArray): WrappedLogKey {
        val cipher = Cipher.getInstance(EncryptedLogFileManager.TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        cipher.updateAAD(aad)
        return WrappedLogKey(cipher.iv.copyOf(), cipher.doFinal(dataKey))
    }

    fun unwrap(key: SecretKey, wrapped: WrappedLogKey, aad: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(EncryptedLogFileManager.TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            key,
            GCMParameterSpec(EncryptedLogFileManager.TAG_BITS, wrapped.nonce),
        )
        cipher.updateAAD(aad)
        return cipher.doFinal(wrapped.ciphertext)
    }
}

internal class AndroidKeystoreLogKeyWrapper : LogKeyWrapper {
    override fun wrap(dataKey: ByteArray, aad: ByteArray): WrappedLogKey =
        AesGcmKeyWrapping.wrap(getOrCreateKey(), dataKey, aad)

    override fun unwrap(wrapped: WrappedLogKey, aad: ByteArray): ByteArray =
        AesGcmKeyWrapping.unwrap(getOrCreateKey(), wrapped, aad)

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            javax.crypto.KeyGenerator.getInstance("AES", "AndroidKeyStore").apply {
                init(
                    android.security.keystore.KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or
                            android.security.keystore.KeyProperties.PURPOSE_DECRYPT,
                    )
                        .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(
                            android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE,
                        )
                        .setKeySize(256)
                        .setUserAuthenticationRequired(false)
                        .build(),
                )
            }.generateKey()
        }
        return (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }

    private companion object {
        const val KEY_ALIAS = "com.aozijx.passly.observability.wrap.v1"
    }
}

internal class EncryptedLogFileManager(
    private val directory: File,
    private val keyWrapper: LogKeyWrapper,
    private val random: SecureRandom,
    private val nowMs: () -> Long,
    private val maxFileBytes: Long,
    private val maxTotalBytes: Long = MAX_TOTAL_BYTES,
    private val maxFiles: Int = MAX_FILES,
) {
    data class Entry(
        val file: File,
        val fileId: ByteArray,
        val createdAtMs: Long,
        val dataKey: ByteArray,
        val wrappedKey: WrappedLogKey,
    ) {
        fun destroy() = dataKey.fill(0)
    }

    data class Opened(val fileId: ByteArray, val createdAtMs: Long, val dataKey: ByteArray) {
        fun destroy() = dataKey.fill(0)
    }

    var activeEntry: Entry? = null
        private set

    init {
        require(maxFileBytes > 0 && maxTotalBytes >= maxFileBytes && maxFiles > 0) {
            "Invalid log retention limits"
        }
        require(directory.mkdirs() || directory.isDirectory) { "Cannot create log directory" }
    }

    fun currentEntry(): Entry {
        val now = nowMs()
        val bucket = now.floorDiv(ONE_DAY_MS)
        activeEntry?.takeIf {
            it.file.exists() &&
                it.createdAtMs.floorDiv(ONE_DAY_MS) == bucket &&
                it.file.length() < maxFileBytes
        }?.let { return it }
        cleanup()
        val file = File.createTempFile("log_${bucket}_${now}_", ".log", directory)
        val entry = try {
            prepareEntry(file, now).also { writeHeader(file, it) }
        } catch (error: Throwable) {
            file.delete()
            throw error
        }
        activeEntry?.destroy()
        activeEntry = entry
        cleanup()
        return entry
    }

    fun prepareEntry(file: File, createdAtMs: Long): Entry {
        val fileId = ByteArray(FILE_ID_BYTES).also(random::nextBytes)
        val dataKey = ByteArray(DATA_KEY_BYTES).also(random::nextBytes)
        return try {
            val wrapped = keyWrapper.wrap(dataKey, headerAad(fileId, createdAtMs))
            require(wrapped.nonce.size == NONCE_BYTES) { "Invalid wrap nonce" }
            require(wrapped.ciphertext.size in 1..MAX_WRAPPED_KEY_BYTES) { "Invalid wrapped key" }
            Entry(file, fileId, createdAtMs, dataKey, wrapped)
        } catch (error: Throwable) {
            dataKey.fill(0)
            throw error
        }
    }

    fun writeHeader(file: File, entry: Entry) {
        DataOutputStream(FileOutputStream(file, false).buffered()).use { out ->
            out.writeInt(FILE_MAGIC)
            out.writeInt(FILE_VERSION)
            out.write(entry.fileId)
            out.writeLong(entry.createdAtMs)
            out.writeInt(entry.wrappedKey.nonce.size)
            out.write(entry.wrappedKey.nonce)
            out.writeInt(entry.wrappedKey.ciphertext.size)
            out.write(entry.wrappedKey.ciphertext)
        }
        file.setLastModified(entry.createdAtMs)
    }

    fun readHeader(input: RandomAccessFile): Opened {
        require(input.readInt() == FILE_MAGIC) { "Unknown observability file" }
        require(input.readInt() == FILE_VERSION) { "Unsupported observability version" }
        val fileId = ByteArray(FILE_ID_BYTES).also(input::readFully)
        val createdAtMs = input.readLong()
        val nonce = ByteArray(input.readInt().checkedExact(NONCE_BYTES)).also(input::readFully)
        val wrappedBytes = ByteArray(input.readInt().checkedMax(MAX_WRAPPED_KEY_BYTES))
            .also(input::readFully)
        val key = keyWrapper.unwrap(
            WrappedLogKey(nonce, wrappedBytes),
            headerAad(fileId, createdAtMs),
        )
        if (key.size != DATA_KEY_BYTES) {
            key.fill(0)
            throw IllegalArgumentException("Invalid data key")
        }
        return Opened(fileId, createdAtMs, key)
    }

    fun recordAad(fileId: ByteArray, sequence: Long, level: Int): ByteArray =
        java.nio.ByteBuffer.allocate(4 + 4 + FILE_ID_BYTES + 8 + 1)
            .putInt(FILE_MAGIC)
            .putInt(FILE_VERSION)
            .put(fileId)
            .putLong(sequence)
            .put(level.toByte())
            .array()

    fun listLogFiles(): List<File> = directory.listFiles()
        ?.filter { it.isFile && it.extension == LOG_EXTENSION }
        ?.sortedWith(compareBy(File::lastModified, File::getName))
        .orEmpty()

    fun countRecords(file: File): Int = runCatching {
        DataInputStream(FileInputStream(file).buffered()).use { input ->
            skipHeader(input)
            var count = 0
            while (true) {
                try {
                    input.skipRecord()
                    count++
                } catch (_: EOFException) {
                    break
                }
            }
            count
        }
    }.getOrDefault(0)

    fun cleanup() {
        val cutoff = nowMs() - RETENTION_MS
        listLogFiles().filter { it.lastModified() < cutoff }.forEach(File::delete)
        listLogFiles().sortedWith(
            compareBy<File>(File::lastModified, File::getName).reversed(),
        ).drop(maxFiles).forEach(File::delete)
        var total = listLogFiles().sumOf(File::length)
        listLogFiles().forEach { file ->
            if (total > maxTotalBytes) {
                val length = file.length()
                if (file.delete()) total -= length
            }
        }
    }

    fun clear() {
        activeEntry?.destroy()
        activeEntry = null
        listLogFiles().forEach(File::delete)
    }

    fun close() {
        activeEntry?.destroy()
        activeEntry = null
    }

    private fun skipHeader(input: DataInputStream) {
        require(input.readInt() == FILE_MAGIC)
        require(input.readInt() == FILE_VERSION)
        input.readFully(ByteArray(FILE_ID_BYTES))
        input.readLong()
        input.readFully(ByteArray(input.readInt().checkedExact(NONCE_BYTES)))
        input.readFully(ByteArray(input.readInt().checkedMax(MAX_WRAPPED_KEY_BYTES)))
    }

    private fun DataInputStream.skipRecord() {
        readFully(ByteArray(readInt().checkedExact(NONCE_BYTES)))
        readLong()
        readUnsignedByte()
        readFully(ByteArray(readInt().checkedMax(LogRecordCodec.MAX_RECORD_BYTES + GCM_TAG_BYTES)))
    }

    private fun headerAad(fileId: ByteArray, createdAtMs: Long): ByteArray =
        java.nio.ByteBuffer.allocate(4 + 4 + FILE_ID_BYTES + 8)
            .putInt(FILE_MAGIC)
            .putInt(FILE_VERSION)
            .put(fileId)
            .putLong(createdAtMs)
            .array()

    private fun Int.checkedMax(max: Int): Int = also { require(it in 1..max) }
    private fun Int.checkedExact(expected: Int): Int = also { require(it == expected) }

    companion object {
        const val LOG_EXTENSION = "log"
        const val FILE_MAGIC = 0x504C4F47
        const val FILE_VERSION = 1
        const val FILE_ID_BYTES = 16
        const val NONCE_BYTES = 12
        const val TAG_BITS = 128
        const val GCM_TAG_BYTES = TAG_BITS / 8
        const val DATA_KEY_BYTES = 32
        const val MAX_WRAPPED_KEY_BYTES = 128
        const val MAX_FILES = 3
        const val MAX_TOTAL_BYTES = 3 * 1024 * 1024L
        const val ONE_DAY_MS = 24 * 60 * 60 * 1000L
        const val RETENTION_MS = 3 * ONE_DAY_MS
        const val TRANSFORMATION = "AES/GCM/NoPadding"

    }
}
