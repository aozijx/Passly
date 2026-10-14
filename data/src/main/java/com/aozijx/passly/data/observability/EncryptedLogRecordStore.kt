package com.aozijx.passly.data.observability

import android.content.Context
import com.aozijx.passly.core.observability.LogLevel
import com.aozijx.passly.core.observability.SanitizedLogRecord
import com.aozijx.passly.core.observability.sink.LogRecordCursor
import com.aozijx.passly.core.observability.sink.LogRecordPage
import com.aozijx.passly.core.observability.sink.LogRecordStore
import com.aozijx.passly.core.observability.sink.LogRecordStoreFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.security.SecureRandom
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

internal class EncryptedLogRecordStore(
    private val directory: File,
    keyWrapper: LogKeyWrapper,
    private val nowMs: () -> Long = System::currentTimeMillis,
    private val random: SecureRandom = SecureRandom(),
    maxFileBytes: Long = MAX_FILE_BYTES,
    maxQueuedRecords: Int = MAX_QUEUED_RECORDS,
    maxTotalBytes: Long = EncryptedLogFileManager.MAX_TOTAL_BYTES,
    maxFiles: Int = EncryptedLogFileManager.MAX_FILES,
    private val beforeWrite: () -> Unit = {},
    private val insideWriteLock: () -> Unit = {},
) : LogRecordStore {
    private val writeLock = Any()
    private val manager = EncryptedLogFileManager(
        directory = directory,
        keyWrapper = keyWrapper,
        random = random,
        nowMs = nowMs,
        maxFileBytes = maxFileBytes,
        maxTotalBytes = maxTotalBytes,
        maxFiles = maxFiles,
    )
    private val crashClaimed = AtomicBoolean(false)
    private val overflowEmergencyClaimed = AtomicBoolean(false)
    private val writer = ThreadPoolExecutor(
        1,
        1,
        0,
        TimeUnit.MILLISECONDS,
        ArrayBlockingQueue(maxQueuedRecords),
        { task -> Thread(task, "passly-observability-writer").apply { isDaemon = true } },
        ThreadPoolExecutor.AbortPolicy(),
    )
    private val emergencyEntry = AtomicReference(
        manager.prepareEntry(File(directory, "emergency_${nowMs()}.log"), nowMs()),
    )

    override fun write(record: SanitizedLogRecord) {
        val task = WriteTask(record)
        try {
            writer.execute(task)
        } catch (_: RejectedExecutionException) {
            if (record.level != LogLevel.DEBUG && evictQueuedDebug()) {
                if (runCatching { writer.execute(task) }.isSuccess) return
            }
            if (record.level in setOf(LogLevel.ERROR, LogLevel.FATAL) &&
                overflowEmergencyClaimed.compareAndSet(false, true)
            ) {
                emergencyWrite(record, EMERGENCY_TIMEOUT_MS, "overflow")
            }
        }
    }

    override fun readPage(cursor: LogRecordCursor?, limit: Int): LogRecordPage =
        synchronized(writeLock) { readPageLocked(cursor, limit) }

    private fun readPageLocked(cursor: LogRecordCursor?, limit: Int): LogRecordPage {
        require(limit in 1..MAX_PAGE_RECORDS) { "Invalid page size" }
        val files = manager.listLogFiles()
        val total = files.sumOf(manager::countRecords)
        val startIndex = cursor?.fileIndex ?: 0
        if (startIndex !in 0..files.size) return LogRecordPage(emptyList(), total, null)
        val records = mutableListOf<SanitizedLogRecord>()
        var next: LogRecordCursor? = null

        files@ for (fileIndex in startIndex until files.size) {
            val file = files[fileIndex]
            RandomAccessFile(file, "r").use { input ->
                val opened = manager.readHeader(input)
                try {
                    val headerEnd = input.filePointer
                    val fileId = opened.fileId.toHex()
                    val offset = if (cursor != null && cursor.fileIndex == fileIndex) {
                        require(cursor.fileId == fileId) { "Cursor file mismatch" }
                        require(cursor.nextRecordOffset in 0..(input.length() - headerEnd)) {
                            "Invalid cursor offset"
                        }
                        cursor.nextRecordOffset
                    } else 0L
                    var expected = if (cursor != null && cursor.fileIndex == fileIndex) {
                        cursor.nextSequence
                    } else 1L
                    input.seek(headerEnd + offset)
                    while (input.filePointer < input.length() && records.size < limit) {
                        val nonce = ByteArray(input.readInt().checkedExact(NONCE_BYTES))
                            .also(input::readFully)
                        val sequence = input.readLong()
                        require(sequence == expected) { "Non-monotonic record sequence" }
                        val level = input.readUnsignedByte()
                        require(level in LogLevel.entries.indices) { "Invalid record level" }
                        val ciphertext = ByteArray(
                            input.readInt().checkedMax(LogRecordCodec.MAX_RECORD_BYTES + TAG_BYTES),
                        ).also(input::readFully)
                        val plain = decrypt(opened.dataKey, opened.fileId, sequence, level, nonce, ciphertext)
                        ciphertext.fill(0)
                        val record = try {
                            LogRecordCodec.decode(plain)
                        } finally {
                            plain.fill(0)
                        }
                        require(record.level.ordinal == level) { "Authenticated level mismatch" }
                        records += record
                        expected++
                        if (records.size == limit) {
                            next = if (input.filePointer < input.length()) {
                                LogRecordCursor(
                                    fileIndex,
                                    fileId,
                                    input.filePointer - headerEnd,
                                    expected,
                                )
                            } else nextFileCursor(files, fileIndex + 1)
                            break@files
                        }
                    }
                } finally {
                    opened.destroy()
                }
            }
        }
        return LogRecordPage(records, total, next)
    }

    override fun readBetween(
        startTimestampMs: Long,
        endTimestampMs: Long,
        limit: Int,
    ): List<SanitizedLogRecord> {
        require(startTimestampMs < endTimestampMs && limit > 0)
        val result = ArrayList<SanitizedLogRecord>(limit)
        var cursor: LogRecordCursor? = null
        do {
            val page = readPage(cursor, minOf(MAX_PAGE_RECORDS, limit - result.size))
            result += page.records.filter { it.timestampMs in startTimestampMs until endTimestampMs }
                .take(limit - result.size)
            cursor = page.nextCursor
        } while (cursor != null && result.size < limit)
        return result
    }

    override fun flush(timeoutMs: Long): Boolean {
        require(timeoutMs >= 0) { "Flush timeout cannot be negative" }
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs)
        val completed = CountDownLatch(1)
        writer.prestartCoreThread()
        return try {
            val queued = writer.queue.offer(
                Runnable { completed.countDown() },
                timeoutMs,
                TimeUnit.MILLISECONDS,
            )
            if (!queued) return false
            val remaining = (deadline - System.nanoTime()).coerceAtLeast(0)
            completed.await(remaining, TimeUnit.NANOSECONDS)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            false
        }
    }

    override fun clear() {
        flush(500)
        synchronized(writeLock) { manager.clear() }
    }

    override fun crashEmergencyWrite(record: SanitizedLogRecord, timeoutMs: Long): Boolean {
        require(timeoutMs > 0) { "Emergency timeout must be positive" }
        if (!crashClaimed.compareAndSet(false, true)) return false
        return emergencyWrite(record, timeoutMs, "crash")
    }

    override fun close() {
        writer.shutdown()
        val terminated = try {
            if (writer.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                true
            } else {
                writer.shutdownNow()
                writer.awaitTermination(500, TimeUnit.MILLISECONDS)
            }
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            false
        }
        if (terminated) synchronized(writeLock) { manager.close() }
        emergencyEntry.getAndSet(null)?.destroy()
    }

    private inner class WriteTask(val record: SanitizedLogRecord) : Runnable {
        override fun run() {
            try {
                beforeWrite()
                val plain = LogRecordCodec.encode(record)
                try {
                    synchronized(writeLock) {
                        insideWriteLock()
                        val entry = manager.currentEntry()
                        val sequence = manager.countRecords(entry.file).toLong() + 1
                        append(entry.file, entry, sequence, record.level.ordinal, plain)
                        manager.cleanup()
                    }
                } finally {
                    plain.fill(0)
                }
            } catch (error: Exception) {
                if (error is InterruptedException) Thread.currentThread().interrupt()
                // A sink failure is terminal for this record and must never recurse into Logger.
            }
        }
    }

    private fun evictQueuedDebug(): Boolean {
        val debugTask = writer.queue.firstOrNull {
            it is WriteTask && it.record.level == LogLevel.DEBUG
        } ?: return false
        return writer.queue.remove(debugTask)
    }

    /** Uses only pre-created in-memory key material and a separate file; no Keystore or normal lock. */
    private fun emergencyWrite(record: SanitizedLogRecord, timeoutMs: Long, prefix: String): Boolean {
        val completed = AtomicBoolean(false)
        val entry = emergencyEntry.getAndSet(null) ?: return false
        val thread = Thread({
            var plain: ByteArray? = null
            try {
                plain = LogRecordCodec.encode(record)
                val file = File(directory, "${prefix}_${nowMs()}.log")
                manager.writeHeader(file, entry)
                append(file, entry, 1, record.level.ordinal, plain)
                completed.set(true)
            } catch (_: Throwable) {
                // Logging must never replace the original failure.
            } finally {
                plain?.fill(0)
                entry.destroy()
            }
        }, "passly-observability-emergency").apply { isDaemon = true }
        thread.start()
        runCatching { thread.join(timeoutMs) }
        return completed.get()
    }

    private fun append(
        file: File,
        entry: EncryptedLogFileManager.Entry,
        sequence: Long,
        level: Int,
        plain: ByteArray,
    ) {
        val nonce = ByteArray(NONCE_BYTES).also(random::nextBytes)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(entry.dataKey, "AES"),
            GCMParameterSpec(EncryptedLogFileManager.TAG_BITS, nonce),
        )
        cipher.updateAAD(manager.recordAad(entry.fileId, sequence, level))
        val encrypted = cipher.doFinal(plain)
        val originalLength = file.length()
        try {
            DataOutputStream(FileOutputStream(file, true).buffered()).use { out ->
                out.writeInt(nonce.size)
                out.write(nonce)
                out.writeLong(sequence)
                out.writeByte(level)
                out.writeInt(encrypted.size)
                out.write(encrypted)
            }
            file.setLastModified(entry.createdAtMs)
        } catch (error: Throwable) {
            runCatching { RandomAccessFile(file, "rw").use { it.setLength(originalLength) } }
            throw error
        } finally {
            encrypted.fill(0)
        }
    }

    private fun decrypt(
        key: ByteArray,
        fileId: ByteArray,
        sequence: Long,
        level: Int,
        nonce: ByteArray,
        ciphertext: ByteArray,
    ): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(key, "AES"),
            GCMParameterSpec(EncryptedLogFileManager.TAG_BITS, nonce),
        )
        cipher.updateAAD(manager.recordAad(fileId, sequence, level))
        return cipher.doFinal(ciphertext)
    }

    private fun nextFileCursor(files: List<File>, start: Int): LogRecordCursor? {
        for (index in start until files.size) {
            val cursor = runCatching {
                RandomAccessFile(files[index], "r").use { input ->
                    val opened = manager.readHeader(input)
                    try {
                        LogRecordCursor(index, opened.fileId.toHex(), 0, 1)
                    } finally {
                        opened.destroy()
                    }
                }
            }.getOrNull()
            if (cursor != null) return cursor
        }
        return null
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
    private fun Int.checkedMax(max: Int): Int = also { require(it in TAG_BYTES..max) }
    private fun Int.checkedExact(expected: Int): Int = also { require(it == expected) }

    companion object {
        const val DIRECTORY_NAME = "logs"
        private const val TRANSFORMATION = EncryptedLogFileManager.TRANSFORMATION
        private const val NONCE_BYTES = EncryptedLogFileManager.NONCE_BYTES
        private const val TAG_BYTES = EncryptedLogFileManager.GCM_TAG_BYTES
        private const val MAX_FILE_BYTES = 1024 * 1024L
        private const val MAX_QUEUED_RECORDS = 256
        private const val MAX_PAGE_RECORDS = 500
        private const val EMERGENCY_TIMEOUT_MS = 50L
    }
}

@Singleton
internal class EncryptedLogRecordStoreFactory @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : LogRecordStoreFactory {
    override fun create(): LogRecordStore = EncryptedLogRecordStore(
        directory = File(context.noBackupFilesDir, EncryptedLogRecordStore.DIRECTORY_NAME),
        keyWrapper = AndroidKeystoreLogKeyWrapper(),
    )
}
