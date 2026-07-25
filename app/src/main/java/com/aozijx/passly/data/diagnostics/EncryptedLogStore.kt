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
import java.io.RandomAccessFile
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

// ============================== 公共类型 ==============================

/**
 * 每个日志文件的不变上下文，读取文件头后返回。
 */
class LogFileContext(
    val fileId: ByteArray,
    val createdAtMs: Long,
    val formatVersion: Int,
    private val _dataKey: ByteArray
) {
    val dataKey: ByteArray get() = _dataKey

    fun destroy() {
        _dataKey.fill(0)
    }

    companion object {
        const val FILE_ID_BYTES = 16
    }
}

/**
 * 分页游标。
 *
 * @param fileIndex 在排序文件列表中的索引
 * @param fileId 文件 ID（hex），用于检测文件轮换后游标失效
 * @param nextRecordOffset 下一条记录的 byte offset（从文件头后开始计数，0 表示第一条记录）
 * @param nextSequence 下一条记录的序号
 */
data class LogCursor(
    val fileIndex: Int,
    val fileId: String,
    val nextRecordOffset: Long,
    val nextSequence: Long
)

/**
 * 分页结果。
 */
data class DiagnosticsPage(
    val events: List<TelemetryEvent>,
    val totalRecords: Int,
    val nextCursor: LogCursor?
)

// ============================== 加密日志存储 ==============================

/**
 * 加密日志存储 v1。
 *
 * ## 文件格式
 *
 * ```
 * [Header]
 *   magic        i4   0x504C4447
 *   version      i4   1
 *   fileId       16B  UUID 字节
 *   createdAtMs  i8   文件创建时间戳
 *   wrapNonceLen i4   12
 *   wrapNonce    12B  包装密钥 Nonce
 *   wrappedKeyLen i4  ≤128
 *   wrappedKey   N B  用 Android KeyStore 包装的数据密钥
 *
 * [Record] × N
 *   nonceLen     i4   12
 *   nonce        12B  记录 Nonce
 *   sequence     i8   文件内序号（从 1 递增）
 *   level        i1   EventLevel.ordinal
 *   ciphertextLen i4  ≤64KB
 *   ciphertext   N B  AES-GCM 密文
 * ```
 *
 * ## AAD
 * - 包装加密：`magic + version + fileId + createdAtMs`（32 字节）
 * - 记录加密：`magic + version + fileId + sequence + level`（33 字节）
 *
 * ## 读取方式
 * - [readPage] 流式读取，不将整个文件加载到内存
 * - 按文件最后修改时间升序读取（最旧优先）
 */
class EncryptedLogStore(
    context: Context,
    private val loggingEnabledUntil: AtomicLong,
    private val directory: File = File(context.noBackupFilesDir, DIRECTORY_NAME),
    private val emitEnabled: (TelemetryEvent) -> Boolean = { true }
) {
    private val random = SecureRandom()
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val writeLock = Any()

    /** 队列溢出回退 — 不占用 crash emergency */
    private val queueOverflowEmergencyClaimed = java.util.concurrent.atomic.AtomicBoolean(false)
    /** 真正 crash — 独立计数器 */
    private val crashEmergencyClaimed = java.util.concurrent.atomic.AtomicBoolean(false)

    /** 每次新文件时重置，文件内序号从 1 开始 */
    private var fileSequence = 0L

    private val writer = ThreadPoolExecutor(
        1, 1, 0L, TimeUnit.MILLISECONDS,
        ArrayBlockingQueue(MAX_QUEUED_RECORDS),
        { task -> Thread(task, "passly-log-writer").apply { isDaemon = true } },
        ThreadPoolExecutor.AbortPolicy()
    )

    /** 当前写入文件缓存 */
    @Volatile
    private var activeFileEntry: FileEntry? = null

    /** 预生成的 crash emergency key — init 时创建，writeLock 保护 */
    private var crashEmergencyEntry: FileEntry? = null

    private class FileEntry(
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

    init {
        directory.mkdirs()
        runCatching {
            val now = System.currentTimeMillis()
            crashEmergencyEntry = createNewFileEntry(
                File(directory, "crash_$now.$LOG_EXTENSION"),
                now
            )
        }
        cleanup()
    }

    // ============================== 写入 ==============================

    /** 写入事件。在禁用或过期时直接跳过。 */
    fun write(event: TelemetryEvent) {
        if (!emitEnabled(event) || loggingEnabledUntil.get() < System.currentTimeMillis()) return
        try {
            writer.execute {
                val plain = RecordCodec.encode(event)
                try {
                    appendNormal(plain, event.level)
                } finally {
                    plain.fill(0)
                }
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
        val entry =
            synchronized(writeLock) { crashEmergencyEntry?.let { copyEntry(it) } } ?: return false
        val completed = java.util.concurrent.atomic.AtomicBoolean(false)
        Thread({
            val plain = RecordCodec.encode(event)
            try {
                val file = File(directory, "crash_${System.currentTimeMillis()}.$LOG_EXTENSION")
                writeHeader(file, entry)
                appendRecordRaw(file, entry, plain, event.level, sequence = 1L, lockWrites = false)
                completed.set(true)
            } catch (_: Throwable) {
                plain.fill(0)
            } finally {
                entry.destroyDataKey()
            }
        }, "passly-crash-emergency").apply { isDaemon = true }.also { thread ->
            thread.start()
            runCatching { thread.join(timeoutMs) }
        }
        return completed.get()
    }

    /** 队列溢出回退写入 */
    private fun fallbackEmergencyWrite(event: TelemetryEvent, timeoutMs: Long) {
        val entry = synchronized(writeLock) {
            activeFileEntry?.let { copyEntry(it) }
                ?: crashEmergencyEntry?.let { copyEntry(it) }
        } ?: return
        Thread({
            val plain = RecordCodec.encode(event)
            try {
                val file = File(directory, "fallback_${System.currentTimeMillis()}.$LOG_EXTENSION")
                writeHeader(file, entry)
                appendRecordRaw(file, entry, plain, event.level, sequence = 1L, lockWrites = false)
            } catch (_: Throwable) {
                plain.fill(0)
            } finally {
                entry.destroyDataKey()
            }
        }, "passly-fallback-emergency").apply { isDaemon = true }.also { thread ->
            thread.start(); runCatching { thread.join(timeoutMs) }
        }
    }

    // ============================== 读取 ==============================

    /**
     * 流式分页读取。
     *
     * @param cursor 上次返回的游标；null 表示从头开始
     * @param limit 最多返回多少条
     */
    fun readPage(cursor: LogCursor?, limit: Int): DiagnosticsPage {
        require(limit in 1..MAX_PAGE_RECORDS) {
            "Page limit must be between 1 and $MAX_PAGE_RECORDS"
        }
        val sortedFiles = directory.listFiles()
            ?.filter { it.extension == LOG_EXTENSION && it.isFile }
            ?.sortedWith(compareBy(File::lastModified, File::getName))
            .orEmpty()

        val startIndex = cursor?.fileIndex ?: 0
        if (startIndex !in 0..sortedFiles.size) {
            return DiagnosticsPage(emptyList(), totalRecordCount(sortedFiles), null)
        }
        val total = totalRecordCount(sortedFiles)
        val result = mutableListOf<TelemetryEvent>()
        var nextCursor: LogCursor? = null

        files@ for (fileIndex in startIndex until sortedFiles.size) {
            val file = sortedFiles[fileIndex]
            val opened = runCatching {
                val raf = RandomAccessFile(file, "r")
                val ctx = try {
                    readHeader(raf)
                } catch (error: Throwable) {
                    raf.close()
                    throw error
                }
                raf to ctx
            }.getOrNull() ?: continue
            val (input, ctx) = opened
            input.use {
                try {
                    val headerEnd = input.filePointer
                    val ctxFileIdHex = bytesToHex(ctx.fileId)
                    val startOffset = if (cursor != null && cursor.fileIndex == fileIndex) {
                        if (cursor.fileId != ctxFileIdHex ||
                            cursor.nextRecordOffset !in 0..(input.length() - headerEnd)
                        ) {
                            return DiagnosticsPage(emptyList(), total, null)
                        }
                        cursor.nextRecordOffset
                    } else {
                        0L
                    }
                    var expectedSequence = if (cursor != null && cursor.fileIndex == fileIndex) {
                        cursor.nextSequence
                    } else {
                        1L
                    }
                    input.seek(headerEnd + startOffset)

                    while (input.filePointer < input.length() && result.size < limit) {
                        val nonceLen = input.readInt().checkedExact(NONCE_BYTES)
                        val nonce = ByteArray(nonceLen).also(input::readFully)
                        val recordSequence = input.readLong()
                        require(recordSequence == expectedSequence) {
                            "Unexpected diagnostics sequence $recordSequence, expected $expectedSequence"
                        }
                        val level = input.readUnsignedByte()
                        require(level in EventLevel.entries.indices) {
                            "Invalid diagnostics level $level"
                        }
                        val cipherLen = input.readInt().checkedMax(MAX_RECORD_BYTES)
                        require(cipherLen >= GCM_TAG_BYTES) { "Ciphertext is shorter than GCM tag" }
                        val ciphertext = ByteArray(cipherLen).also(input::readFully)

                        val aad = buildRecordAad(ctx.fileId, recordSequence, level)
                        val cipher = Cipher.getInstance(TRANSFORMATION)
                        cipher.init(
                            Cipher.DECRYPT_MODE,
                            SecretKeySpec(ctx.dataKey, "AES"),
                            GCMParameterSpec(TAG_BITS, nonce)
                        )
                        cipher.updateAAD(aad)
                        val plain = try {
                            cipher.doFinal(ciphertext)
                        } finally {
                            ciphertext.fill(0)
                        }
                        val event = try {
                            RecordCodec.decode(plain)
                        } finally {
                            plain.fill(0)
                        }
                        require(event.level.ordinal == level) {
                            "Authenticated record level does not match payload"
                        }
                        result += event
                        expectedSequence = recordSequence + 1

                        if (result.size == limit) {
                            nextCursor = if (input.filePointer < input.length()) {
                                LogCursor(
                                    fileIndex = fileIndex,
                                    fileId = ctxFileIdHex,
                                    nextRecordOffset = input.filePointer - headerEnd,
                                    nextSequence = expectedSequence
                                )
                            } else {
                                cursorForNextReadableFile(sortedFiles, fileIndex + 1)
                            }
                            break@files
                        }
                    }
                } finally {
                    ctx.destroy()
                }
            }
        }

        return DiagnosticsPage(events = result, totalRecords = total, nextCursor = nextCursor)
    }

    // ============================== 生命周期 ==============================

    fun flush(timeoutMs: Long = 300L): Boolean = runCatching {
        writer.submit { }.get(timeoutMs, TimeUnit.MILLISECONDS)
        true
    }.getOrDefault(false)

    fun clear() {
        flush(500)
        synchronized(writeLock) {
            directory.listFiles()
                ?.filter { it.extension == LOG_EXTENSION }
                ?.forEach(File::delete)
            activeFileEntry?.destroyDataKey()
            activeFileEntry = null
        }
    }

    fun close() {
        writer.shutdown()
        runCatching { writer.awaitTermination(500, TimeUnit.MILLISECONDS) }
        synchronized(writeLock) {
            activeFileEntry?.destroyDataKey()
            activeFileEntry = null
            crashEmergencyEntry?.destroyDataKey()
            crashEmergencyEntry = null
        }
    }

    // ============================== 内部写入 ==============================

    private fun appendNormal(plain: ByteArray, level: EventLevel) = synchronized(writeLock) {
        val entry = currentFileEntry()
        fileSequence++
        appendRecordRaw(entry.file, entry, plain, level, fileSequence, lockWrites = true)
    }

    private fun currentFileEntry(): FileEntry {
        val prefix = "log_${dateFormatter.format(Date())}"
        val current = activeFileEntry
        if (current != null && current.file.exists() &&
            current.file.name.startsWith(prefix) &&
            current.file.length() < MAX_FILE_BYTES
        ) {
            return current
        }
        // 轮换 → 执行 retention
        cleanup()
        fileSequence = 0L

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
                wrapNonce = ByteArray(0), // not needed for existing file
                wrappedKey = ByteArray(0),
                recordAadPrefix = buildRecordAadPrefix(ctx.fileId)
            )
            ctx.destroy()
            // 读取已有记录数
            fileSequence = countRecordsInFile(reusable).toLong()
            activeFileEntry = entry
            return entry
        }

        // 创建新文件
        val now = System.currentTimeMillis()
        val file = File(directory, "${prefix}_${now}.$LOG_EXTENSION")
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

    private fun appendRecordRaw(
        file: File,
        entry: FileEntry,
        plain: ByteArray,
        level: EventLevel,
        sequence: Long,
        lockWrites: Boolean
    ) {
        val nonce = ByteArray(NONCE_BYTES).also(random::nextBytes)
        val aad = ByteArray(entry.recordAadPrefix.size + 9) // prefix(24) + seq(8) + level(1)
        entry.recordAadPrefix.copyInto(aad)
        var off = entry.recordAadPrefix.size
        writeLongBE(aad, off, sequence); off += 8
        aad[off] = level.ordinal.toByte()

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(entry.dataKey, "AES"),
            GCMParameterSpec(TAG_BITS, nonce)
        )
        cipher.updateAAD(aad)
        val encrypted = cipher.doFinal(plain)

        val append = {
            DataOutputStream(FileOutputStream(file, true).buffered()).use { out ->
                out.writeInt(nonce.size)
                out.write(nonce)
                out.writeLong(sequence)
                out.writeByte(level.ordinal)
                out.writeInt(encrypted.size)
                out.write(encrypted)
            }
        }
        try {
            if (lockWrites) synchronized(writeLock) { append() } else append()
        } finally {
            encrypted.fill(0)
        }
    }

    // ============================== 文件头 ==============================

    private fun createNewFileEntry(file: File, createdAtMs: Long): FileEntry {
        val fileId = ByteArray(LogFileContext.FILE_ID_BYTES).also(random::nextBytes)
        val dataKey = ByteArray(DATA_KEY_BYTES).also(random::nextBytes)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateWrappingKey())

        // 包装加密 AAD = magic + version + fileId + createdAtMs
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

    private fun buildRecordAadPrefix(fileId: ByteArray): ByteArray {
        val buf = ByteArray(24) // magic(4) + version(4) + fileId(16)
        var off = 0
        off = writeIntBE(buf, off, FILE_MAGIC)
        off = writeIntBE(buf, off, FILE_VERSION)
        fileId.copyInto(buf, off)
        return buf
    }

    private fun buildHeaderAad(fileId: ByteArray, createdAtMs: Long): ByteArray {
        val buf = ByteArray(32) // magic(4) + version(4) + fileId(16) + createdAtMs(8)
        var off = 0
        off = writeIntBE(buf, off, FILE_MAGIC)
        off = writeIntBE(buf, off, FILE_VERSION)
        fileId.copyInto(buf, off); off += LogFileContext.FILE_ID_BYTES
        writeLongBE(buf, off, createdAtMs)
        return buf
    }

    private fun buildRecordAad(fileId: ByteArray, sequence: Long, level: Int): ByteArray {
        val buf = ByteArray(33) // magic(4) + version(4) + fileId(16) + seq(8) + level(1)
        var off = 0
        off = writeIntBE(buf, off, FILE_MAGIC)
        off = writeIntBE(buf, off, FILE_VERSION)
        fileId.copyInto(buf, off); off += LogFileContext.FILE_ID_BYTES
        off = writeLongBE(buf, off, sequence)
        buf[off] = level.toByte()
        return buf
    }

    private fun writeHeader(file: File, entry: FileEntry) {
        DataOutputStream(FileOutputStream(file, false).buffered()).use { out ->
            out.writeInt(FILE_MAGIC)
            out.writeInt(FILE_VERSION)
            out.write(entry.fileId) // 16 bytes
            out.writeLong(entry.createdAtMs)
            out.writeInt(entry.wrapNonce.size)
            out.write(entry.wrapNonce)
            out.writeInt(entry.wrappedKey.size)
            out.write(entry.wrappedKey)
        }
    }

    private fun readHeader(file: File): LogFileContext =
        DataInputStream(FileInputStream(file).buffered()).use { readHeader(it) }

    private fun readHeader(input: DataInputStream): LogFileContext {
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
        // 文件头 AAD = magic + version + fileId + createdAtMs
        val headerAad = buildHeaderAad(fileId, createdAtMs)
        cipher.updateAAD(headerAad)

        val dataKey = cipher.doFinal(wrappedKey)
        require(dataKey.size == DATA_KEY_BYTES) { "Invalid diagnostics data key length" }
        return LogFileContext(fileId, createdAtMs, version, dataKey)
    }

    private fun readHeader(input: RandomAccessFile): LogFileContext {
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

    private fun skipHeader(input: DataInputStream) {
        val magic = input.readInt()
        require(magic == FILE_MAGIC) { "Unknown diagnostics file" }
        val version = input.readInt()
        require(version == FILE_VERSION) { "Unsupported diagnostics version $version" }
        // skip fileId (16)
        input.readFully(ByteArray(LogFileContext.FILE_ID_BYTES))
        // skip createdAtMs (8)
        input.readLong()
        // skip wrapNonce
        val wrapNonceLen = input.readInt().checkedExact(NONCE_BYTES)
        input.readFully(ByteArray(wrapNonceLen))
        // skip wrappedKey
        val wrappedLen = input.readInt().checkedMax(MAX_WRAPPED_KEY_BYTES)
        input.readFully(ByteArray(wrappedLen))
    }

    private fun countRecordsInFile(file: File): Int {
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

    private fun totalRecordCount(files: List<File>): Int =
        files.sumOf(::countRecordsInFile)

    private fun cursorForNextReadableFile(
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

    // ============================== 工具方法 ==============================

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
        val files = directory.listFiles()
            ?.filter { it.extension == LOG_EXTENSION && it.isFile }
            .orEmpty()
        // 超时删除
        val cutoff = System.currentTimeMillis() - RETENTION_MS
        files.filter { it.lastModified() < cutoff }.forEach(File::delete)
        // 数量限制
        val remaining = directory.listFiles()
            ?.filter { it.extension == LOG_EXTENSION && it.isFile }
            .orEmpty()
        remaining.sortedByDescending(File::lastModified).drop(MAX_FILES).forEach(File::delete)
        // 总量限制
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

    private fun copyEntry(entry: FileEntry) = FileEntry(
        file = entry.file,
        fileId = entry.fileId.copyOf(),
        createdAtMs = entry.createdAtMs,
        dataKey = entry.dataKey.copyOf(),
        wrapNonce = entry.wrapNonce.copyOf(),
        wrappedKey = entry.wrappedKey.copyOf(),
        recordAadPrefix = entry.recordAadPrefix
    )

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

    private companion object {
        val HEX_DIGITS = "0123456789abcdef".toCharArray()

        /** 大端写入 int */
        fun writeIntBE(buf: ByteArray, off: Int, value: Int): Int {
            buf[off] = (value ushr 24).toByte()
            buf[off + 1] = (value ushr 16).toByte()
            buf[off + 2] = (value ushr 8).toByte()
            buf[off + 3] = value.toByte()
            return off + 4
        }

        /** 大端写入 long */
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

        const val DIRECTORY_NAME = "diagnostics_v1"
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
        const val MAX_QUEUED_RECORDS = 256
        const val MAX_PAGE_RECORDS = 500
        const val EMERGENCY_QUEUE_FALLBACK_MS = 50L
        const val MAX_FILE_BYTES = 1024 * 1024L
        const val MAX_TOTAL_BYTES = 3 * 1024 * 1024L
        const val MAX_FILES = 3
        const val RETENTION_MS = 3 * 24 * 60 * 60 * 1000L
    }
}
