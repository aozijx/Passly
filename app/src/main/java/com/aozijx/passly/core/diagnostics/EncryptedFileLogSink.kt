package com.aozijx.passly.core.diagnostics

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
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
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class EncryptedFileLogSink(
    context: Context,
    private val enabled: () -> Boolean
) : LogSink {
    private val directory = File(context.noBackupFilesDir, DIRECTORY_NAME)
    private val wrappedKeyFile = File(directory, WRAPPED_KEY_FILE)
    private val random = SecureRandom()
    private val writer: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "passly-log-writer").apply { isDaemon = true }
    }
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val writeLock = Any()

    @Volatile
    private var dataKey: ByteArray? = null

    init {
        directory.mkdirs()
        runCatching { dataKey = loadOrCreateDataKey() }
        cleanup()
    }

    override fun write(event: SanitizedLogEvent) {
        if (!enabled()) return
        val line = event.encodeLine().toByteArray(StandardCharsets.UTF_8)
        writer.execute {
            runCatching { appendEncrypted(currentLogFile(), line) }
        }
    }

    override fun flush(timeoutMs: Long): Boolean = runCatching {
        writer.submit { }.get(timeoutMs, TimeUnit.MILLISECONDS)
        true
    }.getOrDefault(false)

    override fun close() {
        writer.shutdown()
        runCatching { writer.awaitTermination(500, TimeUnit.MILLISECONDS) }
        dataKey?.fill(0)
        dataKey = null
    }

    fun emergencyWrite(event: SanitizedLogEvent, timeoutMs: Long = 200L): Boolean {
        val keyReady = dataKey != null
        if (!enabled() || !keyReady) return false
        val completed = java.util.concurrent.atomic.AtomicBoolean(false)
        val thread = Thread({
            runCatching {
                val file = File(directory, "crash_${System.currentTimeMillis()}.elog")
                appendEncrypted(file, event.encodeLine().toByteArray(StandardCharsets.UTF_8))
                completed.set(true)
            }
        }, "passly-emergency-log").apply { isDaemon = true }
        thread.start()
        runCatching { thread.join(timeoutMs) }
        return completed.get()
    }

    fun readAll(): List<String> {
        val key = dataKey?.clone() ?: return emptyList()
        return try {
            directory.listFiles()
                ?.filter { it.extension == LOG_EXTENSION }
                ?.sortedByDescending(File::getName)
                ?.flatMap { readEncryptedFile(it, key) }
                .orEmpty()
        } finally {
            key.fill(0)
        }
    }

    fun clear() {
        flush(500)
        directory.listFiles()?.filter { it.extension == LOG_EXTENSION }?.forEach(File::delete)
    }

    private fun currentLogFile(): File {
        val dated = File(directory, "log_${dateFormatter.format(Date())}.$LOG_EXTENSION")
        if (!dated.exists() || dated.length() < MAX_FILE_BYTES) return dated
        return File(directory, "log_${dateFormatter.format(Date())}_${System.currentTimeMillis()}.$LOG_EXTENSION")
    }

    private fun appendEncrypted(file: File, plain: ByteArray) {
        val keyBytes = dataKey?.clone() ?: return
        try {
            val nonce = ByteArray(NONCE_BYTES).also(random::nextBytes)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.ENCRYPT_MODE,
                SecretKeySpec(keyBytes, KeyProperties.KEY_ALGORITHM_AES),
                GCMParameterSpec(TAG_BITS, nonce)
            )
            val encrypted = cipher.doFinal(plain)
            synchronized(writeLock) {
                DataOutputStream(FileOutputStream(file, true).buffered()).use { output ->
                    output.writeInt(nonce.size)
                    output.write(nonce)
                    output.writeInt(encrypted.size)
                    output.write(encrypted)
                }
            }
            encrypted.fill(0)
        } finally {
            keyBytes.fill(0)
            plain.fill(0)
        }
    }

    private fun readEncryptedFile(file: File, key: ByteArray): List<String> {
        val lines = mutableListOf<String>()
        DataInputStream(FileInputStream(file).buffered()).use { input ->
            while (true) {
                try {
                    val nonceLength = input.readInt()
                    if (nonceLength != NONCE_BYTES) break
                    val nonce = ByteArray(nonceLength)
                    input.readFully(nonce)
                    val cipherLength = input.readInt()
                    if (cipherLength !in 1..MAX_RECORD_BYTES) break
                    val encrypted = ByteArray(cipherLength)
                    input.readFully(encrypted)
                    val cipher = Cipher.getInstance(TRANSFORMATION)
                    cipher.init(
                        Cipher.DECRYPT_MODE,
                        SecretKeySpec(key, KeyProperties.KEY_ALGORITHM_AES),
                        GCMParameterSpec(TAG_BITS, nonce)
                    )
                    lines += cipher.doFinal(encrypted).toString(StandardCharsets.UTF_8)
                    encrypted.fill(0)
                } catch (_: EOFException) {
                    break
                } catch (_: Exception) {
                    break
                }
            }
        }
        return lines
    }

    private fun loadOrCreateDataKey(): ByteArray {
        directory.mkdirs()
        val wrappingKey = getOrCreateWrappingKey()
        if (wrappedKeyFile.exists()) {
            val input = DataInputStream(ByteArrayInputStream(wrappedKeyFile.readBytes()))
            val nonce = ByteArray(input.readInt()).also(input::readFully)
            val ciphertext = ByteArray(input.readInt()).also(input::readFully)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, wrappingKey, GCMParameterSpec(TAG_BITS, nonce))
            return cipher.doFinal(ciphertext)
        }

        val generated = ByteArray(DATA_KEY_BYTES).also(random::nextBytes)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, wrappingKey)
        val wrapped = cipher.doFinal(generated)
        ByteArrayOutputStream().use { bytes ->
            DataOutputStream(bytes).use { output ->
                output.writeInt(cipher.iv.size)
                output.write(cipher.iv)
                output.writeInt(wrapped.size)
                output.write(wrapped)
            }
            wrappedKeyFile.writeBytes(bytes.toByteArray())
        }
        wrapped.fill(0)
        return generated
    }

    private fun getOrCreateWrappingKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            generator.init(
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
            generator.generateKey()
        }
        return (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }

    private fun cleanup() {
        val files = directory.listFiles()?.filter { it.extension == LOG_EXTENSION }.orEmpty()
        val cutoff = System.currentTimeMillis() - RETENTION_MS
        files.filter { it.lastModified() < cutoff }.forEach(File::delete)
        files.sortedByDescending(File::lastModified).drop(MAX_FILES).forEach(File::delete)
        var total = files.filter(File::exists).sumOf(File::length)
        files.sortedBy(File::lastModified).forEach { file ->
            if (total <= MAX_TOTAL_BYTES) return@forEach
            val length = file.length()
            if (file.delete()) total -= length
        }
    }

    companion object {
        private const val DIRECTORY_NAME = "diagnostics"
        private const val WRAPPED_KEY_FILE = "diagnostics.key"
        private const val LOG_EXTENSION = "elog"
        private const val KEY_ALIAS = "com.aozijx.passly.diagnostics.wrap.v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val NONCE_BYTES = 12
        private const val TAG_BITS = 128
        private const val DATA_KEY_BYTES = 32
        private const val MAX_RECORD_BYTES = 64 * 1024
        private const val MAX_FILE_BYTES = 1024 * 1024L
        private const val MAX_TOTAL_BYTES = 3 * 1024 * 1024L
        private const val MAX_FILES = 3
        private const val RETENTION_MS = 3 * 24 * 60 * 60 * 1000L
    }
}
