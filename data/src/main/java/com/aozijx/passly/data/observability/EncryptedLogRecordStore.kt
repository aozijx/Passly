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
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
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
import java.util.concurrent.atomic.AtomicLong
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
    private val maxFileBytes: Long = MAX_FILE_BYTES,
    maxQueuedRecords: Int = MAX_QUEUED_RECORDS,
    maxTotalBytes: Long = EncryptedLogFileManager.MAX_TOTAL_BYTES,
    maxFiles: Int = EncryptedLogFileManager.MAX_FILES,
    private val beforeWrite: () -> Unit = {},
    private val insideWriteLock: () -> Unit = {},
) : LogRecordStore {
    private val writeAcceptanceLock = Any()
    private val writeLock = Any()
    private val manager =
        EncryptedLogFileManager(
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
    private val writeFailureCount = AtomicLong()
    private val observedWriteFailureCount = AtomicLong()
    private val emergencyThreads = ConcurrentHashMap.newKeySet<Thread>()

    @Volatile private var acceptingWrites = true
    private val droppedCounts = AtomicLongArray(LogLevel.entries.size)
    private val writer =
        ThreadPoolExecutor(
            1,
            1,
            0,
            TimeUnit.MILLISECONDS,
            ArrayBlockingQueue(maxQueuedRecords),
            { task -> Thread(task, "passly-observability-writer").apply { isDaemon = true } },
            ThreadPoolExecutor.AbortPolicy(),
        )
    private val emergencyEntry =
        AtomicReference(
            manager.prepareEntry(File(directory, "emergency_${nowMs()}.log"), nowMs()),
        )

    override fun write(record: SanitizedLogRecord) =
        synchronized(writeAcceptanceLock) {
            if (!acceptingWrites || writer.isShutdown || !directory.isDirectory || !directory.canWrite()) {
                recordDrop(record.level)
                writeFailureCount.incrementAndGet()
                return@synchronized
            }
            val task = WriteTask(record)
            try {
                writer.execute(task)
            } catch (_: RejectedExecutionException) {
                val evicted = evictQueuedLowerPriority(record.level)
                if (evicted != null) {
                    recordDrop(evicted)
                    try {
                        writer.execute(task)
                        return@synchronized
                    } catch (_: RejectedExecutionException) {
                        // Continue to emergency storage or record the dropped entry.
                    }
                }
                if (record.level in setOf(LogLevel.ERROR, LogLevel.FATAL) &&
                    overflowEmergencyClaimed.compareAndSet(false, true)
                ) {
                    if (emergencyWriteAsync(record, "overflow")) return@synchronized
                }
                recordDrop(record.level)
                writeFailureCount.incrementAndGet()
            }
        }

    override fun readPage(
        cursor: LogRecordCursor?,
        limit: Int,
    ): LogRecordPage = synchronized(writeLock) { readPageLocked(cursor, limit) }

    private fun readPageLocked(
        cursor: LogRecordCursor?,
        limit: Int,
    ): LogRecordPage {
        require(limit in 1..LogRecordStore.MAX_PAGE_RECORDS) { "Invalid page size" }
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
                    val offset =
                        if (cursor != null && cursor.fileIndex == fileIndex) {
                            require(cursor.fileId == fileId) { "Cursor file mismatch" }
                            require(cursor.nextRecordOffset in 0..(input.length() - headerEnd)) {
                                "Invalid cursor offset"
                            }
                            cursor.nextRecordOffset
                        } else {
                            0L
                        }
                    var expected =
                        if (cursor != null && cursor.fileIndex == fileIndex) {
                            cursor.nextSequence
                        } else {
                            1L
                        }
                    input.seek(headerEnd + offset)
                    while (input.filePointer < input.length() && records.size < limit) {
                        val nonce =
                            ByteArray(input.readInt().checkedExact(NONCE_BYTES))
                                .also(input::readFully)
                        val sequence = input.readLong()
                        require(sequence == expected) { "Non-monotonic record sequence" }
                        val level = input.readUnsignedByte()
                        require(LogLevel.fromWireId(level) != null) { "Invalid record level" }
                        val ciphertext =
                            ByteArray(
                                input.readInt().checkedMax(LogRecordCodec.MAX_RECORD_BYTES + TAG_BYTES),
                            ).also(input::readFully)
                        val plain = decrypt(opened.dataKey, opened.fileId, sequence, level, nonce, ciphertext)
                        ciphertext.fill(0)
                        val record =
                            try {
                                LogRecordCodec.decode(plain)
                            } finally {
                                plain.fill(0)
                            }
                        require(record.level.wireId == level) { "Authenticated level mismatch" }
                        records += record
                        expected++
                        if (records.size == limit) {
                            next =
                                if (input.filePointer < input.length()) {
                                    LogRecordCursor(
                                        fileIndex,
                                        fileId,
                                        input.filePointer - headerEnd,
                                        expected,
                                    )
                                } else {
                                    nextFileCursor(files, fileIndex + 1)
                                }
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
            val page = readPage(cursor, minOf(LogRecordStore.MAX_PAGE_RECORDS, limit - result.size))
            result +=
                page.records
                    .filter { it.timestampMs in startTimestampMs until endTimestampMs }
                    .take(limit - result.size)
            cursor = page.nextCursor
        } while (cursor != null && result.size < limit)
        return result
    }

    override fun flush(timeoutMs: Long): Boolean {
        require(timeoutMs >= 0) { "Flush timeout cannot be negative" }
        if (writer.isShutdown) return false
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs)
        val completed = CountDownLatch(1)
        writer.prestartCoreThread()
        return try {
            val queued =
                writer.queue.offer(
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
            val failures = writeFailureCount.get()
            failures == observedWriteFailureCount.getAndSet(failures)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            false
        }
    }

    override fun clear(): LogClearResult =
        synchronized(writeAcceptanceLock) {
            if (!flush(CLEAR_FLUSH_TIMEOUT_MS)) return@synchronized LogClearResult.Failed
            synchronized(writeLock) {
                if (manager.clear()) LogClearResult.Cleared else LogClearResult.Failed
            }
        }

    override fun storageBytes(): Long = synchronized(writeLock) { manager.storageBytes() }

    override fun drainDroppedCounts(): LogDropCounts =
        LogDropCounts(
            debug = droppedCounts.getAndSet(LogLevel.DEBUG.ordinal, 0),
            info = droppedCounts.getAndSet(LogLevel.INFO.ordinal, 0),
            warn = droppedCounts.getAndSet(LogLevel.WARN.ordinal, 0),
            error = droppedCounts.getAndSet(LogLevel.ERROR.ordinal, 0),
            fatal = droppedCounts.getAndSet(LogLevel.FATAL.ordinal, 0),
        )

    override fun crashEmergencyWrite(
        record: SanitizedLogRecord,
        timeoutMs: Long,
    ): Boolean =
        synchronized(writeAcceptanceLock) {
            require(timeoutMs > 0) { "Emergency timeout must be positive" }
            if (!crashClaimed.compareAndSet(false, true)) return@synchronized false
            emergencyWrite(record, timeoutMs, "crash")
        }

    override fun close() {
        synchronized(writeAcceptanceLock) {
            if (!acceptingWrites) return
            acceptingWrites = false
            writer.shutdown()
        }
        val terminated =
            try {
                if (writer.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                    true
                } else {
                    writer.shutdownNow().filterIsInstance<WriteTask>().forEach { task ->
                        recordDrop(task.record.level)
                        writeFailureCount.incrementAndGet()
                    }
                    writer.awaitTermination(500, TimeUnit.MILLISECONDS)
                }
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                false
            }
        val emergencyDeadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(CLOSE_TIMEOUT_MS)
        for (emergencyThread in emergencyThreads.toList()) {
            val remaining = (emergencyDeadline - System.nanoTime()).coerceAtLeast(0)
            if (remaining <= 0) break
            try {
                TimeUnit.NANOSECONDS.timedJoin(emergencyThread, remaining)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            }
        }
        if (terminated && emergencyThreads.isEmpty()) synchronized(writeLock) { manager.close() }
        emergencyEntry.getAndSet(null)?.destroy()
    }

    private inner class WriteTask(
        val record: SanitizedLogRecord,
    ) : Runnable {
        override fun run() {
            val batch = mutableListOf(this)
            var boundary: Runnable? = null
            try {
                val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(BATCH_WINDOW_MS)
                while (batch.size < MAX_BATCH_RECORDS) {
                    val remaining = (deadline - System.nanoTime()).coerceAtLeast(0)
                    if (remaining == 0L) break
                    val queued = writer.queue.poll(remaining, TimeUnit.NANOSECONDS) ?: break
                    if (queued is WriteTask) {
                        batch += queued
                    } else {
                        boundary = queued
                        break
                    }
                }
                writeBatch(batch)
            } catch (error: Exception) {
                if (error is InterruptedException) Thread.currentThread().interrupt()
                batch.forEach { recordDrop(it.record.level) }
                writeFailureCount.addAndGet(batch.size.toLong())
            } finally {
                boundary?.run()
            }
        }
    }

    private fun writeBatch(batch: List<WriteTask>) {
        val encodedRecords = ArrayList<Pair<WriteTask, ByteArray>>(batch.size)
        val encryptedFrames = ArrayList<ByteArray>(batch.size)
        try {
            batch.forEach { task ->
                check(directory.isDirectory && directory.canWrite()) { "Log storage is offline" }
                beforeWrite()
                encodedRecords += task to LogRecordCodec.encode(task.record)
            }
            synchronized(writeLock) {
                check(directory.isDirectory && directory.canWrite()) { "Log storage is offline" }
                insideWriteLock()
                var recordIndex = 0
                while (recordIndex < encodedRecords.size) {
                    val entry = manager.currentEntry()
                    var sequence = manager.countRecords(entry.file).toLong()
                    var estimatedBytes = 0L
                    val batchStart = recordIndex
                    while (recordIndex < encodedRecords.size) {
                        val plain = encodedRecords[recordIndex].second
                        val frameBytes = estimateEncryptedFrameBytes(plain.size)
                        val projectedSize = entry.file.length() + estimatedBytes + frameBytes
                        if (recordIndex > batchStart && projectedSize > maxFileBytes) break
                        val (task, recordPlain) = encodedRecords[recordIndex]
                        encryptedFrames += encryptFrame(entry, ++sequence, task.record.level.wireId, recordPlain)
                        estimatedBytes += frameBytes
                        recordIndex++
                    }
                    appendFrames(entry.file, encryptedFrames)
                    encryptedFrames.clear()
                    manager.cleanup()
                }
            }
        } finally {
            encryptedFrames.forEach { it.fill(0) }
            encodedRecords.forEach { (_, plain) -> plain.fill(0) }
        }
    }

    private fun encryptFrame(
        entry: EncryptedLogFileManager.Entry,
        sequence: Long,
        level: Int,
        plain: ByteArray,
    ): ByteArray {
        val nonce = ByteArray(NONCE_BYTES).also(random::nextBytes)
        val cipher = Cipher.getInstance(EncryptedLogFileManager.TRANSFORMATION)
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(entry.dataKey, "AES"),
            GCMParameterSpec(EncryptedLogFileManager.TAG_BITS, nonce),
        )
        cipher.updateAAD(manager.recordAad(entry.fileId, sequence, level))
        val encrypted = cipher.doFinal(plain)
        return try {
            ByteArrayOutputStream().use { bytes ->
                DataOutputStream(bytes).use { output ->
                    output.writeInt(nonce.size)
                    output.write(nonce)
                    output.writeLong(sequence)
                    output.writeByte(level)
                    output.writeInt(encrypted.size)
                    output.write(encrypted)
                }
                bytes.toByteArray()
            }
        } finally {
            nonce.fill(0)
            encrypted.fill(0)
        }
    }

    private fun estimateEncryptedFrameBytes(plainBytes: Int): Long =
        FRAME_METADATA_BYTES.toLong() + plainBytes + EncryptedLogFileManager.GCM_TAG_BYTES

    private fun appendFrames(
        file: File,
        frames: List<ByteArray>,
    ) {
        val originalLength = file.length()
        try {
            FileOutputStream(file, true).use { output ->
                BufferedOutputStream(output).use { buffered ->
                    frames.forEach(buffered::write)
                    buffered.flush()
                    output.fd.sync()
                }
            }
        } catch (error: Throwable) {
            try {
                RandomAccessFile(file, "rw").use { rollback ->
                    rollback.setLength(originalLength)
                    rollback.fd.sync()
                }
            } catch (cleanupFailure: Throwable) {
                if (cleanupFailure !== error) error.addSuppressed(cleanupFailure)
            }
            throw error
        } finally {
            frames.forEach { it.fill(0) }
        }
    }

    private fun evictQueuedLowerPriority(incoming: LogLevel): LogLevel? {
        val evictableLevels =
            listOf(LogLevel.DEBUG, LogLevel.INFO)
                .filter { it.ordinal < incoming.ordinal }
        for (level in evictableLevels) {
            val task =
                writer.queue.firstOrNull {
                    it is WriteTask && it.record.level == level
                } ?: continue
            if (writer.queue.remove(task)) return level
        }
        return null
    }

    private fun recordDrop(level: LogLevel) {
        droppedCounts.incrementAndGet(level.ordinal)
    }

    private fun emergencyWriteAsync(
        record: SanitizedLogRecord,
        prefix: String,
    ): Boolean {
        val entry = emergencyEntry.getAndSet(null) ?: return false
        launchEmergencyWrite(record, prefix, entry)
        return true
    }

    /** Uses only pre-created in-memory key material and a separate file; no Keystore or normal lock. */
    private fun emergencyWrite(
        record: SanitizedLogRecord,
        timeoutMs: Long,
        prefix: String,
    ): Boolean {
        val completed = AtomicBoolean(false)
        val entry = emergencyEntry.getAndSet(null) ?: return false
        val thread = launchEmergencyWrite(record, prefix, entry, completed)
        try {
            thread.join(timeoutMs)
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        return completed.get()
    }

    private fun launchEmergencyWrite(
        record: SanitizedLogRecord,
        prefix: String,
        entry: EncryptedLogFileManager.Entry,
        completed: AtomicBoolean = AtomicBoolean(false),
    ): Thread {
        val thread =
            Thread(
                {
                    var plain: ByteArray? = null
                    var stagingFile: File? = null
                    var failure: Throwable? = null
                    try {
                        plain = LogRecordCodec.encode(record)
                        val staging =
                            File
                                .createTempFile("${prefix}_${nowMs()}_", ".tmp", directory)
                                .also { stagingFile = it }
                        manager.writeHeader(staging, entry)
                        val frame = encryptFrame(entry, 1, record.level.wireId, plain)
                        appendFrames(staging, listOf(frame))
                        val completedFile = File(directory, "${staging.nameWithoutExtension}.log")
                        completed.set(staging.renameTo(completedFile))
                    } catch (caught: Throwable) {
                        // Logging must never replace the original failure.
                        failure = caught
                    } finally {
                        fun capture(cleanupFailure: Throwable) {
                            val current = failure
                            failure =
                                when {
                                    cleanupFailure is Error && current !is Error -> {
                                        current?.let { if (it !== cleanupFailure) cleanupFailure.addSuppressed(it) }
                                        cleanupFailure
                                    }

                                    current == null -> {
                                        cleanupFailure
                                    }

                                    current !== cleanupFailure -> {
                                        current.addSuppressed(cleanupFailure)
                                        current
                                    }

                                    else -> {
                                        current
                                    }
                                }
                        }

                        fun cleanup(action: () -> Unit) {
                            try {
                                action()
                            } catch (cleanupFailure: Throwable) {
                                capture(cleanupFailure)
                            }
                        }

                        if (!completed.get()) cleanup { stagingFile?.delete() }
                        cleanup { plain?.fill(0) }
                        cleanup { entry.destroy() }
                        if (!completed.get()) writeFailureCount.incrementAndGet()
                        cleanup { emergencyThreads.remove(Thread.currentThread()) }
                        (failure as? Error)?.let { throw it }
                    }
                },
                "passly-observability-emergency",
            ).apply { isDaemon = true }
        emergencyThreads.add(thread)
        thread.start()
        return thread
    }

    private fun decrypt(
        key: ByteArray,
        fileId: ByteArray,
        sequence: Long,
        level: Int,
        nonce: ByteArray,
        ciphertext: ByteArray,
    ): ByteArray {
        val cipher = Cipher.getInstance(EncryptedLogFileManager.TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(key, "AES"),
            GCMParameterSpec(EncryptedLogFileManager.TAG_BITS, nonce),
        )
        cipher.updateAAD(manager.recordAad(fileId, sequence, level))
        return cipher.doFinal(ciphertext)
    }

    private fun nextFileCursor(
        files: List<File>,
        start: Int,
    ): LogRecordCursor? {
        for (index in start until files.size) {
            val cursor =
                try {
                    RandomAccessFile(files[index], "r").use { input ->
                        val opened = manager.readHeader(input)
                        try {
                            LogRecordCursor(index, opened.fileId.toHex(), 0, 1)
                        } finally {
                            opened.destroy()
                        }
                    }
                } catch (_: Exception) {
                    null
                }
            if (cursor != null) return cursor
        }
        return null
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun Int.checkedMax(max: Int): Int = also { require(it in TAG_BYTES..max) }

    private fun Int.checkedExact(expected: Int): Int = also { require(it == expected) }

    companion object {
        const val DIRECTORY_NAME = "logs"
        private const val NONCE_BYTES = EncryptedLogFileManager.NONCE_BYTES
        private const val TAG_BYTES = EncryptedLogFileManager.GCM_TAG_BYTES
        private const val MAX_FILE_BYTES = 1024 * 1024L
        private const val MAX_QUEUED_RECORDS = 256
        private const val MAX_BATCH_RECORDS = 16
        private const val BATCH_WINDOW_MS = 50L
        private const val FRAME_METADATA_BYTES = 4 + NONCE_BYTES + 8 + 1 + 4
        private const val CLEAR_FLUSH_TIMEOUT_MS = 500L
        private const val CLOSE_TIMEOUT_MS = 500L
    }
}

@Singleton
internal class EncryptedLogRecordStoreFactory
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : LogRecordStoreFactory {
        override fun create(): LogRecordStore =
            EncryptedLogRecordStore(
                directory = File(context.noBackupFilesDir, EncryptedLogRecordStore.DIRECTORY_NAME),
                keyWrapper = AndroidKeystoreLogKeyWrapper(),
            )
    }
