package com.aozijx.passly.data.diagnostics

import android.content.Context
import com.aozijx.passly.core.telemetry.EventLevel
import com.aozijx.passly.core.telemetry.TelemetryEvent
import com.aozijx.passly.data.diagnostics.EncryptedLogFileManager.FileEntry
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.security.SecureRandom
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import javax.crypto.Cipher
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
 * 加密日志存储 v1。文件 I/O 委托给 [EncryptedLogFileManager]。
 */
class EncryptedLogStore(
    context: Context,
    private val loggingEnabledUntil: AtomicLong,
    private val directory: File = File(context.noBackupFilesDir, DIRECTORY_NAME),
    private val emitEnabled: (TelemetryEvent) -> Boolean = { true }
) {
    private val random = SecureRandom()
    private val writeLock = Any()
    private val fileManager = EncryptedLogFileManager(directory, random, writeLock)

    private val queueOverflowEmergencyClaimed = java.util.concurrent.atomic.AtomicBoolean(false)
    private val crashEmergencyClaimed = java.util.concurrent.atomic.AtomicBoolean(false)

    private var fileSequence = 0L

    private val writer = ThreadPoolExecutor(
        1, 1, 0L, TimeUnit.MILLISECONDS,
        ArrayBlockingQueue(MAX_QUEUED_RECORDS),
        { task -> Thread(task, "passly-log-writer").apply { isDaemon = true } },
        ThreadPoolExecutor.AbortPolicy()
    )

    private var crashEmergencyEntry: FileEntry? = null

    init {
        directory.mkdirs()
        runCatching {
            val now = System.currentTimeMillis()
            crashEmergencyEntry = fileManager.createNewFileEntry(
                File(directory, "crash_$now.$LOG_EXTENSION"),
                now
            )
        }
        fileManager.cleanup()
    }

    // ============================== 写入 ==============================

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

    fun crashEmergencyWrite(event: TelemetryEvent, timeoutMs: Long = 200L): Boolean {
        if (!crashEmergencyClaimed.compareAndSet(false, true)) return false
        val entry =
            synchronized(writeLock) { crashEmergencyEntry?.let { fileManager.copyEntry(it) } }
                ?: return false
        val completed = java.util.concurrent.atomic.AtomicBoolean(false)
        Thread({
            val plain = RecordCodec.encode(event)
            try {
                val file = File(directory, "crash_${System.currentTimeMillis()}.$LOG_EXTENSION")
                fileManager.writeHeader(file, entry)
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

    private fun fallbackEmergencyWrite(event: TelemetryEvent, timeoutMs: Long) {
        val entry = synchronized(writeLock) {
            fileManager.activeFileEntry?.let { fileManager.copyEntry(it) }
                ?: crashEmergencyEntry?.let { fileManager.copyEntry(it) }
        } ?: return
        Thread({
            val plain = RecordCodec.encode(event)
            try {
                val file = File(directory, "fallback_${System.currentTimeMillis()}.$LOG_EXTENSION")
                fileManager.writeHeader(file, entry)
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
            return DiagnosticsPage(emptyList(), fileManager.totalRecordCount(sortedFiles), null)
        }
        val total = fileManager.totalRecordCount(sortedFiles)
        val result = mutableListOf<TelemetryEvent>()
        var nextCursor: LogCursor? = null

        files@ for (fileIndex in startIndex until sortedFiles.size) {
            val file = sortedFiles[fileIndex]
            val opened = runCatching {
                val raf = RandomAccessFile(file, "r")
                val ctx = try {
                    fileManager.readHeader(raf)
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

                        val aad = fileManager.buildRecordAad(ctx.fileId, recordSequence, level)
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
                                fileManager.cursorForNextReadableFile(sortedFiles, fileIndex + 1)
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
            fileManager.activeFileEntry?.destroyDataKey()
            fileManager.activeFileEntry = null
        }
    }

    fun close() {
        writer.shutdown()
        runCatching { writer.awaitTermination(500, TimeUnit.MILLISECONDS) }
        synchronized(writeLock) {
            fileManager.activeFileEntry?.destroyDataKey()
            fileManager.activeFileEntry = null
            crashEmergencyEntry?.destroyDataKey()
            crashEmergencyEntry = null
        }
    }

    // ============================== 内部写入 ==============================

    private fun appendNormal(plain: ByteArray, level: EventLevel) = synchronized(writeLock) {
        val entry = fileManager.currentFileEntry()
        fileSequence = fileManager.recordCountFor(entry.file).toLong()
        fileSequence++
        appendRecordRaw(entry.file, entry, plain, level, fileSequence, lockWrites = true)
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
        val aad = ByteArray(entry.recordAadPrefix.size + 9)
        entry.recordAadPrefix.copyInto(aad)
        var off = entry.recordAadPrefix.size
        EncryptedLogFileManager.writeLongBE(aad, off, sequence); off += 8
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

    // ============================== 工具方法 ==============================

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
        const val DIRECTORY_NAME = "diagnostics_v1"
        const val LOG_EXTENSION = "elog1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val NONCE_BYTES = 12
        const val TAG_BITS = 128
        const val GCM_TAG_BYTES = TAG_BITS / 8
        const val MAX_RECORD_BYTES = 64 * 1024
        const val MAX_QUEUED_RECORDS = 256
        const val MAX_PAGE_RECORDS = 500
        const val EMERGENCY_QUEUE_FALLBACK_MS = 50L
    }
}