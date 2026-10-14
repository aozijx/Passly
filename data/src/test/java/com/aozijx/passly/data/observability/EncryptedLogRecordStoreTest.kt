package com.aozijx.passly.data.observability

import com.aozijx.passly.core.observability.EventId
import com.aozijx.passly.core.observability.DiagnosticId
import com.aozijx.passly.core.observability.ErrorCode
import com.aozijx.passly.core.observability.ErrorPayload
import com.aozijx.passly.core.observability.Events
import com.aozijx.passly.core.observability.ExceptionSnapshot
import com.aozijx.passly.core.observability.ExceptionType
import com.aozijx.passly.core.observability.LogCategory
import com.aozijx.passly.core.observability.LogFields
import com.aozijx.passly.core.observability.LogLevel
import com.aozijx.passly.core.observability.ProcessSessionId
import com.aozijx.passly.core.observability.ReasonCode
import com.aozijx.passly.core.observability.SanitizedLogRecord
import com.aozijx.passly.core.observability.SpanId
import com.aozijx.passly.core.observability.TraceId
import com.aozijx.passly.core.observability.TraceSnapshot
import java.io.File
import java.nio.file.Files
import java.io.RandomAccessFile
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.crypto.spec.SecretKeySpec
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EncryptedLogRecordStoreTest {
    private lateinit var directory: File
    private var now = 1_700_000_000_000L

    @Before
    fun setUp() {
        directory = Files.createTempDirectory("passly-observability").toFile()
    }

    @After
    fun tearDown() {
        directory.deleteRecursively()
    }

    @Test
    fun `writes encrypted records and ignores legacy elog1 files`() {
        File(directory, "legacy.elog1").writeText("legacy secret")
        val store = newStore()
        val record = sampleRecord(1)

        store.write(record)
        assertTrue(store.flush(2_000))

        assertEquals(listOf(record), store.readPage(null, 10).records)
        assertEquals(1, directory.listFiles()!!.count { it.extension == "log" })
        assertEquals("legacy secret", File(directory, "legacy.elog1").readText())
        store.close()
    }

    @Test
    fun `retention removes expired log but never touches legacy extension`() {
        val expiredLog = File(directory, "expired.log").apply {
            writeText("expired")
            setLastModified(now - EncryptedLogFileManager.RETENTION_MS - 1)
        }
        val legacy = File(directory, "expired.elog1").apply {
            writeText("legacy")
            setLastModified(now - EncryptedLogFileManager.RETENTION_MS - 1)
        }
        val store = newStore()
        store.write(sampleRecord(1))
        assertTrue(store.flush(2_000))

        assertFalse(expiredLog.exists())
        assertTrue(legacy.exists())
        store.close()
    }

    @Test
    fun `ciphertext tampering fails closed`() {
        val store = newStore()
        store.write(sampleRecord(1))
        assertTrue(store.flush(2_000))
        val file = directory.listFiles()!!.single { it.extension == "log" }
        val bytes = file.readBytes().also { it[it.lastIndex] = (it.last() xor 1) }
        file.writeBytes(bytes)

        assertThrows(Exception::class.java) { store.readPage(null, 10) }
        store.close()
    }

    @Test
    fun `reader rejects non monotonic record sequence`() {
        val store = newStore()
        store.write(sampleRecord(1))
        store.write(sampleRecord(2))
        assertTrue(store.flush(2_000))
        val file = directory.listFiles()!!.single { it.extension == "log" }
        overwriteSequence(file, recordIndex = 1, sequence = 7)

        assertThrows(IllegalArgumentException::class.java) { store.readPage(null, 10) }
        store.close()
    }

    @Test
    fun `rotates by date and enforces file count retention`() {
        val store = newStore(maxFileBytes = 256 * 1024)
        repeat(5) { index ->
            now += EncryptedLogFileManager.ONE_DAY_MS
            store.write(sampleRecord(index))
            assertTrue(store.flush(2_000))
        }

        assertTrue(directory.listFiles()!!.count { it.extension == "log" } <= 3)
        assertEquals(3, store.readPage(null, 10).records.size)
        store.close()
    }

    @Test
    fun `rotates by size and enforces total byte retention`() {
        val store = newStore(maxFileBytes = 1, maxTotalBytes = 2_000)
        repeat(8) { index ->
            store.write(sampleRecord(index))
            assertTrue(store.flush(2_000))
        }

        val files = directory.listFiles()!!.filter { it.extension == "log" }
        assertTrue(files.size <= 3)
        assertTrue(files.sumOf(File::length) <= 2_000)
        store.close()
    }

    @Test
    fun `queue overflow drops debug before a higher priority record`() {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val first = AtomicBoolean(true)
        val store = newStore(
            maxQueuedRecords = 1,
            beforeWrite = {
                if (first.compareAndSet(true, false)) {
                    entered.countDown()
                    release.await(2, TimeUnit.SECONDS)
                }
            },
        )
        store.write(sampleRecord(1))
        assertTrue(entered.await(2, TimeUnit.SECONDS))
        store.write(sampleRecord(2).copy(level = LogLevel.DEBUG))
        store.write(sampleRecord(3))
        release.countDown()
        assertTrue(store.flush(2_000))

        assertEquals(
            listOf("event_00000001", "event_00000003"),
            store.readPage(null, 10).records.map { it.eventId.value },
        )
        store.close()
    }

    @Test
    fun `store failure does not recursively log and later writes still succeed`() {
        val store = newStore()
        store.write(sampleRecord(1).copy(level = LogLevel.ERROR))
        assertTrue(store.flush(2_000))
        store.write(sampleRecord(2))
        assertTrue(store.flush(2_000))

        assertEquals(
            listOf("event_00000002"),
            store.readPage(null, 10).records.map { it.eventId.value },
        )
        store.close()
    }

    @Test
    fun `queue overflow emergency writes an error at most once`() {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val first = AtomicBoolean(true)
        val store = newStore(
            maxQueuedRecords = 1,
            beforeWrite = {
                if (first.compareAndSet(true, false)) {
                    entered.countDown()
                    release.await(2, TimeUnit.SECONDS)
                }
            },
        )
        store.write(sampleRecord(1))
        assertTrue(entered.await(2, TimeUnit.SECONDS))
        store.write(sampleRecord(2))
        store.write(errorRecord(3))
        store.write(errorRecord(4))
        release.countDown()
        assertTrue(store.flush(2_000))

        assertEquals(3, store.readPage(null, 10).records.size)
        store.close()
    }

    @Test
    fun `crash emergency write is single shot and does not unwrap a key`() {
        val wrapper = TestKeyWrapper()
        val lockEntered = CountDownLatch(1)
        val releaseLock = CountDownLatch(1)
        val store = newStore(
            wrapper = wrapper,
            insideWriteLock = {
                lockEntered.countDown()
                releaseLock.await(2, TimeUnit.SECONDS)
            },
        )
        val unwrapsAfterInitialization = wrapper.unwrapCalls
        store.write(sampleRecord(9))
        assertTrue(lockEntered.await(2, TimeUnit.SECONDS))

        assertTrue(store.crashEmergencyWrite(sampleRecord(1), 2_000))
        assertFalse(store.crashEmergencyWrite(sampleRecord(2), 2_000))
        assertEquals(unwrapsAfterInitialization, wrapper.unwrapCalls)
        releaseLock.countDown()
        assertTrue(store.flush(2_000))
        store.close()
    }

    @Test
    fun `crash emergency write rejects an unbounded zero timeout`() {
        val store = newStore()

        assertThrows(IllegalArgumentException::class.java) {
            store.crashEmergencyWrite(sampleRecord(1), 0)
        }
        store.close()
    }

    private fun newStore(
        wrapper: TestKeyWrapper = TestKeyWrapper(),
        maxFileBytes: Long = 1024 * 1024,
        maxTotalBytes: Long = 3 * 1024 * 1024,
        maxQueuedRecords: Int = 256,
        beforeWrite: () -> Unit = {},
        insideWriteLock: () -> Unit = {},
    ) = EncryptedLogRecordStore(
        directory = directory,
        keyWrapper = wrapper,
        nowMs = { now },
        maxFileBytes = maxFileBytes,
        maxTotalBytes = maxTotalBytes,
        maxQueuedRecords = maxQueuedRecords,
        beforeWrite = beforeWrite,
        insideWriteLock = insideWriteLock,
    )

    private fun errorRecord(index: Int) = sampleRecord(index).copy(
        level = LogLevel.ERROR,
        event = Events.Autofill.ResultDeliveryFailed,
        error = ErrorPayload(
            errorCode = ErrorCode("AUTOFILL_RESULT_DELIVERY_FAILED"),
            reason = ReasonCode("PLATFORM_CALLBACK_FAILED"),
            diagnosticId = DiagnosticId("DGN-ABCD-EFGH"),
            exception = ExceptionSnapshot(
                type = ExceptionType("IllegalStateException"),
                appStackFrames = emptyList(),
                causeTypes = emptyList(),
                foreignFrameCount = 1,
            ),
        ),
    )

    private fun sampleRecord(index: Int) = SanitizedLogRecord(
        eventId = EventId("event_${index.toString().padStart(8, '0')}"),
        timestampMs = now,
        elapsedRealtimeMs = index.toLong(),
        level = LogLevel.INFO,
        category = LogCategory.AUTOFILL,
        event = Events.Autofill.ResultDelivered,
        processSessionId = ProcessSessionId("process_12345678"),
        secureSessionId = null,
        trace = TraceSnapshot(
            traceId = TraceId("trace_12345678"),
            spanId = SpanId("span_${index.toString().padStart(8, '0')}"),
            parentSpanId = null,
        ),
        fields = LogFields.Empty,
        error = null,
    )

    private class TestKeyWrapper : LogKeyWrapper {
        private val key = SecretKeySpec(ByteArray(32) { 7 }, "AES")
        var unwrapCalls = 0

        override fun wrap(dataKey: ByteArray, aad: ByteArray): WrappedLogKey =
            AesGcmKeyWrapping.wrap(key, dataKey, aad)

        override fun unwrap(wrapped: WrappedLogKey, aad: ByteArray): ByteArray {
            unwrapCalls++
            return AesGcmKeyWrapping.unwrap(key, wrapped, aad)
        }
    }

    private fun overwriteSequence(file: File, recordIndex: Int, sequence: Long) {
        RandomAccessFile(file, "rw").use { input ->
            assertEquals(EncryptedLogFileManager.FILE_MAGIC, input.readInt())
            assertEquals(EncryptedLogFileManager.FILE_VERSION, input.readInt())
            input.skipBytes(EncryptedLogFileManager.FILE_ID_BYTES + Long.SIZE_BYTES)
            input.skipBytes(input.readInt())
            input.skipBytes(input.readInt())
            repeat(recordIndex) {
                input.skipBytes(input.readInt())
                input.skipBytes(Long.SIZE_BYTES + 1)
                input.skipBytes(input.readInt())
            }
            input.skipBytes(input.readInt())
            input.writeLong(sequence)
        }
    }

    private infix fun Byte.xor(other: Int): Byte = (toInt() xor other).toByte()
}
