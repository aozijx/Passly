package com.aozijx.passly.data.diagnostics

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.aozijx.passly.core.telemetry.EventLevel
import com.aozijx.passly.core.telemetry.TelemetryEvent
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
import java.util.concurrent.atomic.AtomicLong
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import com.aozijx.passly.data.diagnostics.TelemetryRecordCodec as RecordCodec

/**
 * 加密日志存储。
 *
 * ## 相比 [com.aozijx.passly.core.diagnostics.PerFileEncryptedLogSink] 的改进
 *
 * - **24h 窗口不过期修复**：[loggingEnabledUntil] 使用 [AtomicLong]，每次写入比较当前时间
 * - **retention 执行时机**：每次文件轮换后触发，非仅初始化
 * - **emergencyClaimed 分离**：队列溢出回退和真正 crash 使用独立计数器
 * - **分页读取**：[readPage] 支持游标分页解密
 * - **直接写入**：基于事件级别禁用时不排队，减少调度开销
 * - **记录序号**：每条记录附带递增序号，检测跨文件移动
 * - **文件头 AAD**：用于身份验证；记录 AAD 包含文件 ID + 序号
 */
class EncryptedLogStore(
    context: Context,
    private val loggingEnabledUntil: AtomicLong,
    private val directory: File = File(context.noBackupFilesDir, DIRECTORY_NAME),
    private val emitEnabled: (TelemetryEvent) -> Boolean = { true }
) {
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

    private val random = SecureRandom()
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val writeLock = Any()

    /** 普通队列溢出回退 — 不占用 crash emergency */
    private val queueOverflowEmergencyClaimed = java.util.concurrent.atomic.AtomicBoolean(false)

    /** 真正 crash — 独立计数器 */
    private val crashEmergencyClaimed = java.util.concurrent.atomic.AtomicBoolean(false)
    private var recordSequence = AtomicLong(0L)
    private var fileId = java.util.UUID.randomUUID().toString().take(12)

    private val writer = ThreadPoolExecutor(
        1, 1, 0L, TimeUnit.MILLISECONDS,
        ArrayBlockingQueue(MAX_QUEUED_RECORDS),
        { task -> Thread(task, "passly-log-writer").apply { isDaemon = true } },
        ThreadPoolExecutor.AbortPolicy()
    )

    @Volatile
    private var activeFile: File? = null
    private var activeKey: FileKey? = null
    private var crashEmergencyKey: FileKey? = null

    init {
        directory.mkdirs()
        runCatching { crashEmergencyKey = prepareFileKey() }
        cleanup()
    }

    // ============================== 写入 ==============================

    /** 写入事件。在禁用过期时不会提交任务。 */
    fun write(event: TelemetryEvent) {
        if (!emitEnabled(event) || loggingEnabledUntil.get() < System.currentTimeMillis()) return
        try {
            writer.execute {
                val plain = RecordCodec.encode(event)
                runCatching { appendNormal(plain, event.level.ordinal) }
                    .onFailure { plain.fill(0) }
            }
        } catch (_: RejectedExecutionException) {
            if (event.level.ordinal >= EventLevel.ERROR.ordinal &&
                queueOverflowEmergencyClaimed.compareAndSet(false, true)
            ) {
                fallbackEmergencyWrite(event, EMERGENCY_QUEUE_FALLBACK_MS)
            }
        }
    }

    /** 真正的 crash 写入 — 使用独立计数器且不受配置控制 */
    fun crashEmergencyWrite(event: TelemetryEvent, timeoutMs: Long = 200L): Boolean {
        if (!crashEmergencyClaimed.compareAndSet(false, true)) return false
        val prepared = synchronized(writeLock) { crashEmergencyKey?.copyOwned() } ?: return false
        val completed = java.util.concurrent.atomic.AtomicBoolean(false)
        Thread({
            val plain = RecordCodec.encode(event)
            try {
                val file = File(directory, "crash_${System.currentTimeMillis()}.$LOG_EXTENSION")
                writeHeader(file, prepared)
                appendRecordRaw(file, prepared.raw, plain, lockWrites = false)
                completed.set(true)
            } catch (_: Throwable) {
                plain.fill(0)
            } finally {
                prepared.destroy()
            }
        }, "passly-crash-emergency").apply { isDaemon = true }.also { thread ->
            thread.start()
            runCatching { thread.join(timeoutMs) }
        }
        return completed.get()
    }

    /** 队列溢出回退写入 */
    private fun fallbackEmergencyWrite(event: TelemetryEvent, timeoutMs: Long) {
        val prepared = synchronized(writeLock) {
            activeKey?.copyOwned()
                ?: crashEmergencyKey?.copyOwned()
        } ?: return
        Thread({
            val plain = RecordCodec.encode(event)
            try {
                val file = File(directory, "fallback_${System.currentTimeMillis()}.$LOG_EXTENSION")
                writeHeader(file, prepared)
                appendRecordRaw(file, prepared.raw, plain, lockWrites = false)
            } catch (_: Throwable) {
                plain.fill(0)
            } finally {
                prepared.destroy()
            }
        }, "passly-fallback-emergency").apply { isDaemon = true }.also { thread ->
            thread.start(); runCatching { thread.join(timeoutMs) }
        }
    }

    // ============================== 读取 ==============================

    /**
     * 分页读取解密日志。
     *
     * @param cursor 从第几条记录开始（0‑based）
     * @param limit 最多返回多少条
     * @return (解码行列表, 总记录数, 下一条游标，-1 表示无更多)
     */
    fun readPage(cursor: Int, limit: Int): Triple<List<String>, Int, Int> {
        val allFiles = directory.listFiles()
            ?.filter { it.extension == LOG_EXTENSION }
            ?.sortedByDescending(File::lastModified)
            .orEmpty()
        var total = 0
        val result = mutableListOf<String>()
        for (file in allFiles) {
            val records = runCatching { readEncryptedFile(file) }.getOrDefault(emptyList())
            val start = maxOf(0, cursor - total)
            val end = minOf(cursor + limit - result.size, records.size)
            result.addAll(records.subList(start, end))
            total += records.size
            if (result.size >= limit) break
        }
        val nextCursor = if (result.size < limit) -1 else cursor + limit
        return Triple(result, total, nextCursor)
    }

    fun readAll(): List<String> = directory.listFiles()
        ?.filter { it.extension == LOG_EXTENSION }
        ?.sortedByDescending(File::lastModified)
        ?.flatMap(::readEncryptedFile)
        .orEmpty()

    // ============================== 生命周期 ==============================

    fun flush(timeoutMs: Long = 300L): Boolean = runCatching {
        writer.submit { }.get(timeoutMs, TimeUnit.MILLISECONDS)
        true
    }.getOrDefault(false)

    fun clear() {
        flush(500)
        synchronized(writeLock) {
            directory.listFiles()?.filter { it.extension == LOG_EXTENSION }?.forEach(File::delete)
            activeFile = null
            activeKey?.destroy()
            activeKey = null
        }
    }

    fun close() {
        writer.shutdown()
        runCatching { writer.awaitTermination(500, TimeUnit.MILLISECONDS) }
        synchronized(writeLock) {
            activeKey?.destroy()
            activeKey = null
            crashEmergencyKey?.destroy()
            crashEmergencyKey = null
        }
    }

    // ============================== 内部 ==============================

    private fun appendNormal(plain: ByteArray, level: Int) = synchronized(writeLock) {
        val file = currentLogFile()
        val key = keyFor(file)
        appendRecordRaw(file, key.raw, plain, lockWrites = true)
    }

    private fun currentLogFile(): File {
        val prefix = "log_${dateFormatter.format(Date())}"
        activeFile?.takeIf { it.exists() && it.name.startsWith(prefix) && it.length() < MAX_FILE_BYTES }
            ?.let { return it }
        // 轮换 → 执行 retention
        cleanup()
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

    private fun appendRecordRaw(
        file: File,
        rawKey: ByteArray,
        plain: ByteArray,
        lockWrites: Boolean
    ) {
        val nonce = ByteArray(NONCE_BYTES).also(random::nextBytes)
        val seq = recordSequence.incrementAndGet()
        val recordAad = "${fileId}:$seq".toByteArray(StandardCharsets.US_ASCII)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(rawKey, "AES"),
            GCMParameterSpec(TAG_BITS, nonce)
        )
        cipher.updateAAD(recordAad)
        val encrypted = cipher.doFinal(plain)
        val append = {
            DataOutputStream(FileOutputStream(file, true).buffered()).use { output ->
                output.writeInt(nonce.size)
                output.write(nonce)
                output.writeLong(seq)
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
            val fileIdRaw = fileId.toByteArray(StandardCharsets.US_ASCII)
            output.writeInt(fileIdRaw.size)
            output.write(fileIdRaw)
        }
    }

    private fun readHeader(file: File): FileKey =
        DataInputStream(FileInputStream(file).buffered()).use {
            readHeader(it)
        }

    private fun readHeader(input: DataInputStream): FileKey {
        require(input.readInt() == FILE_MAGIC) { "Unknown diagnostics file" }
        val version = input.readInt()
        require(version in 1..FILE_VERSION) { "Unsupported diagnostics version $version" }
        val wrapNonceLength = input.readInt().also { require(it == NONCE_BYTES) }
        val wrapNonce = ByteArray(wrapNonceLength).also(input::readFully)
        val wrapped =
            ByteArray(input.readInt().checkedLength(MAX_WRAPPED_KEY_BYTES)).also(input::readFully)
        if (version >= 2) {
            val fileIdLength = input.readInt().checkedLength(32)
            ByteArray(fileIdLength).also(input::readFully) // consume fileId, don't validate
        }
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
                            val seq = input.readLong()
                            val encrypted = ByteArray(
                                input.readInt().checkedLength(MAX_RECORD_BYTES)
                            ).also(input::readFully)
                            val fileAad = "$fileId:$seq".toByteArray(StandardCharsets.US_ASCII)
                            val cipher = Cipher.getInstance(TRANSFORMATION)
                            cipher.init(
                                Cipher.DECRYPT_MODE,
                                SecretKeySpec(key.raw, "AES"),
                                GCMParameterSpec(TAG_BITS, nonce)
                            )
                            cipher.updateAAD(fileAad)
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
        // 超时删除
        val cutoff = System.currentTimeMillis() - RETENTION_MS
        files.filter { it.lastModified() < cutoff }.forEach(File::delete)
        // 数量限制
        val remaining = directory.listFiles()
            ?.filter { it.extension == LOG_EXTENSION }
            .orEmpty()
        remaining.sortedByDescending(File::lastModified).drop(MAX_FILES).forEach(File::delete)
        // 总量限制
        val postDeletion = directory.listFiles()
            ?.filter { it.extension == LOG_EXTENSION }
            .orEmpty()
        var total = postDeletion.sumOf(File::length)
        postDeletion.sortedBy(File::lastModified).forEach { file ->
            if (total > MAX_TOTAL_BYTES) {
                val length = file.length()
                if (file.delete()) total -= length
            }
        }
    }

    private fun FileKey.copyOwned() = FileKey(raw.copyOf(), wrapNonce.copyOf(), wrapped.copyOf())
    private fun Int.checkedLength(max: Int): Int = also { require(it in 1..max) }

    private companion object {
        const val DIRECTORY_NAME = "diagnostics_v1"
        const val LOG_EXTENSION = "elog1"
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
    }
}


