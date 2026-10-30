package com.aozijx.passly.data.observability

import android.content.Context
import com.aozijx.passly.core.observability.LogLevel
import com.aozijx.passly.core.observability.SanitizedLogRecord
import com.aozijx.passly.core.observability.sink.LogClearResult
import com.aozijx.passly.core.observability.sink.LogDropCounts
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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLongArray
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
    private val writeAcceptanceLock = Any()
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
    private val emergencyThreads = ConcurrentHashMap.newKeySet<Thread>()
    private val droppedCounts = AtomicLongArray(LogLevel.entries.size)
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

    override fun write(record: SanitizedLogRecord) = synchronized(writeAcceptanceLock) {
        val task = WriteTask(record)
        try {
            writer.execute(task)
        } catch (_: RejectedExecutionException) {
            val evicted = evictQueuedLowerPriority(record.level)
            if (evicted != null) {
                recordDrop(evicted)
                if (runCatching { writer.execute(task) }.isSuccess) return@synchronized
            }
            if (record.level in setOf(LogLevel.ERROR, LogLevel.FATAL) &&
                overflowEmergencyClaimed.compareAndSet(false, true)
            ) {
                if (emergencyWriteAsync(record, "overflow")) return@synchronized
            }
            recordDrop(record.level)
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
                        require(LogLevel.fromWireId(level) != null) { "Invalid record level" }
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
                        require(record.level.wireId == level) { "Authenticated level mismatch" }
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
            var remaining = (deadline - System.nanoTime()).coerceAtLeast(0)
            if (!completed.await(remaining, TimeUnit.NANOSECONDS)) return false
            emergencyThreads.toList().forEach { emergencyThread ->
                remaining = (deadline - System.nanoTime()).coerceAtLeast(0)
                TimeUnit.NANOSECONDS.timedJoin(emergencyThread, remaining)
                if (emergencyThread.isAlive) return false
            }
            true
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            false
        }
    }

    override fun clear(): LogClearResult = synchronized(writeAcceptanceLock) {
        if (!flush(CLEAR_FLUSH_TIMEOUT_MS)) return@synchronized LogClearResult.Failed
        synchronized(writeLock) {
            if (manager.clear()) LogClearResult.Cleared else LogClearResult.Failed
        }
    }

    override fun storageBytes(): Long = synchronized(writeLock) { manager.storageBytes() }

    override fun drainDroppedCounts(): LogDropCounts = LogDropCounts(
        debug = droppedCounts.getAndSet(LogLevel.DEBUG.ordinal, 0),
        info = droppedCounts.getAndSet(LogLevel.INFO.ordinal, 0),
        warn = droppedCounts.getAndSet(LogLevel.WARN.ordinal, 0),
        error = droppedCounts.getAndSet(LogLevel.ERROR.ordinal, 0),
        fatal = droppedCounts.getAndSet(LogLevel.FATAL.ordinal, 0),
    )

    override fun crashEmergencyWrite(record: SanitizedLogRecord, timeoutMs: Long): Boolean =
        synchronized(writeAcceptanceLock) {
        require(timeoutMs > 0) { "Emergency timeout must be positive" }
        if (!crashClaimed.compareAndSet(false, true)) return@synchronized false
        emergencyWrite(record, timeoutMs, "crash")
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
        val emergencyDeadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(CLOSE_TIMEOUT_MS)
        emergencyThreads.toList().forEach { emergencyThread ->
            val remaining = (emergencyDeadline - System.nanoTime()).coerceAtLeast(0)
            if (remaining > 0) runCatching { TimeUnit.NANOSECONDS.timedJoin(emergencyThread, remaining) }
        }
        if (terminated && emergencyThreads.isEmpty()) synchronized(writeLock) { manager.close() }
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
                        append(entry.file, entry, sequence, record.level.wireId, plain)
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

    private fun evictQueuedLowerPriority(incoming: LogLevel): LogLevel? {
        val evictableLevels = listOf(LogLevel.DEBUG, LogLevel.INFO)
            .filter { it.ordinal < incoming.ordinal }
        for (level in evictableLevels) {
            val task = writer.queue.firstOrNull {
                it is WriteTask && it.record.level == level
            } ?: continue
            if (writer.queue.remove(task)) return level
        }
        return null
    }

    private fun recordDrop(level: LogLevel) {
        droppedCounts.incrementAndGet(level.ordinal)
    }

    private fun emergencyWriteAsync(record: SanitizedLogRecord, prefix: String): Boolean {
        val entry = emergencyEntry.getAndSet(null) ?: return false
        launchEmergencyWrite(record, prefix, entry)
        return true
    }

    /** Uses only pre-created in-memory key material and a separate file; no Keystore or normal lock. */
    private fun emergencyWrite(record: SanitizedLogRecord, timeoutMs: Long, prefix: String): Boolean {
        val completed = AtomicBoolean(false)
        val entry = emergencyEntry.getAndSet(null) ?: return false
        val thread = launchEmergencyWrite(record, prefix, entry, completed)
        runCatching { thread.join(timeoutMs) }
        return completed.get()
    }

    private fun launchEmergencyWrite(
        record: SanitizedLogRecord,
        prefix: String,
        entry: EncryptedLogFileManager.Entry,
        completed: AtomicBoolean = AtomicBoolean(false),
    ): Thread {
        val thread = Thread(
            {
            var plain: ByteArray? = null
            var stagingFile: File? = null
            try {
                plain = LogRecordCodec.encode(record)
                val staging = File.createTempFile("${prefix}_${nowMs()}_", ".tmp", directory)
                    .also { stagingFile = it }
                manager.writeHeader(staging, entry)
                append(staging, entry, 1, record.level.wireId, plain)
                val completedFile = File(directory, "${staging.nameWithoutExtension}.log")
                completed.set(staging.renameTo(completedFile))
            } catch (_: Throwable) {
                // Logging must never replace the original failure.
            } finally {
                if (completed.get().not()) stagingFile?.delete()
                plain?.fill(0)
                entry.destroy()
                emergencyThreads.remove(Thread.currentThread())
            }
            },
            "passly-observability-emergency",
        ).apply { isDaemon = true }
        emergencyThreads.add(thread)
        thread.start()
        return thread
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
        private const val CLEAR_FLUSH_TIMEOUT_MS = 500L
        private const val CLOSE_TIMEOUT_MS = 500L
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
