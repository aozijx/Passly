package com.aozijx.passly.core.diagnostics

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class EncryptedFileLogSink(
    context: Context,
    private val enabled: () -> Boolean
) : LogSink {
    private data class FileKey(
        val raw: ByteArray,
        val wrapNonce: ByteArray,
        val wrapped: ByteArray
    ) {
        fun destroy() {
            raw.fill(0)
            wrapNonce.fill(0)
            wrapped.fill(0)
        }
    }

    private val directory = File(context.noBackupFilesDir, DIRECTORY_NAME)
    private val random = SecureRandom()
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val writeLock = Any()
    private val emergencyClaimed = AtomicBoolean(false)
    private val writer = ThreadPoolExecutor(
        1,
        1,
        0L,
        TimeUnit.MILLISECONDS,
        ArrayBlockingQueue(MAX_QUEUED_RECORDS),
        { task -> Thread(task, "passly-log-writer").apply { isDaemon = true } },
        ThreadPoolExecutor.AbortPolicy()
    )

    @Volatile
    private var activeFile: File? = null
    private var activeKey: FileKey? = null
    private var emergencyKey: FileKey? = null

    init {
        directory.mkdirs()
        runCatching { emergencyKey = prepareFileKey() }
        cleanup()
    }

    override fun write(event: SanitizedLogEvent) {
        if (!enabled()) return
        try {
            writer.execute {
                val plain = event.encodeLine().toByteArray(StandardCharsets.UTF_8)
                runCatching { appendNormal(plain) }.onFailure { plain.fill(0) }
            }
        } catch (_: RejectedExecutionException) {
            if (event.level >= LogLevel.ERROR) emergencyWrite(event, EMERGENCY_QUEUE_FALLBACK_MS)
        }
    }

    override fun flush(timeoutMs: Long): Boolean = runCatching {
        writer.submit { }.get(timeoutMs, TimeUnit.MILLISECONDS)
        true
    }.getOrDefault(false)

    override fun close() {
        writer.shutdown()
        runCatching { writer.awaitTermination(500, TimeUnit.MILLISECONDS) }
        synchronized(writeLock) {
            activeKey?.destroy()
            activeKey = null
            emergencyKey?.destroy()
            emergencyKey = null
        }
    }

    fun emergencyWrite(event: SanitizedLogEvent, timeoutMs: Long = 200L): Boolean {
        if (!enabled() || !emergencyClaimed.compareAndSet(false, true)) return false
        val prepared = synchronized(writeLock) { emergencyKey?.copyOwned() } ?: return false
        val completed = AtomicBoolean(false)
        Thread({
            val plain = event.encodeLine().toByteArray(StandardCharsets.UTF_8)
            try {
                val file = File(directory, "crash_${System.currentTimeMillis()}.$LOG_EXTENSION")
                writeHeader(file, prepared)
                appendRecord(file, prepared.raw, plain, lockWrites = false)
                completed.set(true)
            } catch (_: Throwable) {
                plain.fill(0)
            } finally {
                prepared.destroy()
            }
        }, "passly-emergency-log").apply { isDaemon = true }.also { thread ->
            thread.start()
            runCatching { thread.join(timeoutMs) }
        }
        return completed.get()
    }

    fun readAll(): List<String> = directory.listFiles()
        ?.filter { it.extension == LOG_EXTENSION }
        ?.sortedByDescending(File::getName)
        ?.flatMap(::readEncryptedFile)
        .orEmpty()

    fun clear() {
        flush(500)
        synchronized(writeLock) {
            directory.listFiles()?.filter { it.extension == LOG_EXTENSION }?.forEach(File::delete)
            activeFile = null
            activeKey?.destroy()
            activeKey = null
        }
    }

    private fun appendNormal(plain: ByteArray) = synchronized(writeLock) {
        val file = currentLogFile()
        val key = keyFor(file)
        appendRecord(file, key.raw, plain, lockWrites = true)
    }

    private fun currentLogFile(): File {
        val prefix = "log_${dateFormatter.format(Date())}"
        activeFile?.takeIf { it.exists() && it.name.startsWith(prefix) && it.length() < MAX_FILE_BYTES }
            ?.let { return it }
        val reusable = directory.listFiles()
            ?.filter { it.name.startsWith(prefix) && it.extension == LOG_EXTENSION }
            ?.filter { it.length() < MAX_FILE_BYTES }
            ?.maxByOrNull(File::lastModified)
        return reusable ?: File(directory, "${prefix}_${System.currentTimeMillis()}.$LOG_EXTENSION")
    }

    private fun keyFor(file: File): FileKey {
        if (activeFile == file) return requireNotNull(activeKey)
        activeKey?.destroy()
        val key = if (file.exists() && file.length() > 0L) readHeader(file) else prepareFileKey()
        if (!file.exists() || file.length() == 0L) writeHeader(file, key)
        activeFile = file
        activeKey = key
        return key
    }

    private fun appendRecord(file: File, rawKey: ByteArray, plain: ByteArray, lockWrites: Boolean) {
        val nonce = ByteArray(NONCE_BYTES).also(random::nextBytes)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(rawKey, "AES"), GCMParameterSpec(TAG_BITS, nonce))
        cipher.updateAAD(RECORD_AAD)
        val encrypted = cipher.doFinal(plain)
        val append = {
            DataOutputStream(FileOutputStream(file, true).buffered()).use { output ->
                output.writeInt(nonce.size)
                output.write(nonce)
                output.writeInt(encrypted.size)
                output.write(encrypted)
            }
        }
        try {
            if (lockWrites) synchronized(writeLock) { append() } else append()
        } finally {
            plain.fill(0)
            encrypted.fill(0)
        }
    }

    private fun prepareFileKey(): FileKey {
        val raw = ByteArray(DATA_KEY_BYTES).also(random::nextBytes)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateWrappingKey())
        return FileKey(raw, cipher.iv.copyOf(), cipher.doFinal(raw))
    }

    private fun writeHeader(file: File, key: FileKey) {
        DataOutputStream(FileOutputStream(file, false).buffered()).use { output ->
            output.writeInt(FILE_MAGIC)
            output.writeInt(FILE_VERSION)
            output.writeInt(key.wrapNonce.size)
            output.write(key.wrapNonce)
            output.writeInt(key.wrapped.size)
            output.write(key.wrapped)
        }
    }

    private fun readHeader(file: File): FileKey = DataInputStream(FileInputStream(file).buffered()).use {
        readHeader(it)
    }

    private fun readHeader(input: DataInputStream): FileKey {
        require(input.readInt() == FILE_MAGIC) { "Unknown diagnostics file" }
        require(input.readInt() == FILE_VERSION) { "Unsupported diagnostics version" }
        val wrapNonceLength = input.readInt().also { require(it == NONCE_BYTES) }
        val wrapNonce = ByteArray(wrapNonceLength).also(input::readFully)
        val wrapped = ByteArray(input.readInt().checkedLength(MAX_WRAPPED_KEY_BYTES)).also(input::readFully)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateWrappingKey(),
            GCMParameterSpec(TAG_BITS, wrapNonce)
        )
        return FileKey(cipher.doFinal(wrapped), wrapNonce, wrapped)
    }

    private fun readEncryptedFile(file: File): List<String> = runCatching {
        DataInputStream(FileInputStream(file).buffered()).use { input ->
            val key = readHeader(input)
            try {
                buildList {
                    while (true) {
                        try {
                            val nonceLength = input.readInt().also { require(it == NONCE_BYTES) }
                            val nonce = ByteArray(nonceLength).also(input::readFully)
                            val encrypted = ByteArray(
                                input.readInt().checkedLength(MAX_RECORD_BYTES)
                            ).also(input::readFully)
                            val cipher = Cipher.getInstance(TRANSFORMATION)
                            cipher.init(
                                Cipher.DECRYPT_MODE,
                                SecretKeySpec(key.raw, "AES"),
                                GCMParameterSpec(TAG_BITS, nonce)
                            )
                            cipher.updateAAD(RECORD_AAD)
                            val plain = try {
                                cipher.doFinal(encrypted)
                            } finally {
                                encrypted.fill(0)
                            }
                            try {
                                add(plain.toString(StandardCharsets.UTF_8))
                            } finally {
                                plain.fill(0)
                            }
                        } catch (_: EOFException) {
                            break
                        }
                    }
                }
            } finally {
                key.destroy()
            }
        }
    }.getOrDefault(emptyList())

    private fun getOrCreateWrappingKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
                init(
                    KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .setUserAuthenticationRequired(false)
                        .build()
                )
            }.generateKey()
        }
        return (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }

    private fun cleanup() {
        val files = directory.listFiles()?.filter { it.extension == LOG_EXTENSION }.orEmpty()
        val cutoff = System.currentTimeMillis() - RETENTION_MS
        files.filter { it.lastModified() < cutoff }.forEach(File::delete)
        files.filter(File::exists).sortedByDescending(File::lastModified).drop(MAX_FILES).forEach(File::delete)
        var total = files.filter(File::exists).sumOf(File::length)
        files.filter(File::exists).sortedBy(File::lastModified).forEach { file ->
            if (total > MAX_TOTAL_BYTES) {
                val length = file.length()
                if (file.delete()) total -= length
            }
        }
    }

    private fun FileKey.copyOwned() = FileKey(raw.copyOf(), wrapNonce.copyOf(), wrapped.copyOf())
    private fun Int.checkedLength(max: Int): Int = also { require(it in 1..max) }

    private companion object {
        const val DIRECTORY_NAME = "diagnostics"
        const val LOG_EXTENSION = "elog"
        const val KEY_ALIAS = "com.aozijx.passly.diagnostics.wrap.v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val FILE_MAGIC = 0x504C4447
        const val FILE_VERSION = 1
        const val NONCE_BYTES = 12
        const val TAG_BITS = 128
        const val DATA_KEY_BYTES = 32
        const val MAX_WRAPPED_KEY_BYTES = 128
        const val MAX_RECORD_BYTES = 64 * 1024
        const val MAX_QUEUED_RECORDS = 256
        const val EMERGENCY_QUEUE_FALLBACK_MS = 50L
        const val MAX_FILE_BYTES = 1024 * 1024L
        const val MAX_TOTAL_BYTES = 3 * 1024 * 1024L
        const val MAX_FILES = 3
        const val RETENTION_MS = 3 * 24 * 60 * 60 * 1000L
        val RECORD_AAD = "PasslyDiagnosticsRecordV1".toByteArray(StandardCharsets.US_ASCII)
    }
}
