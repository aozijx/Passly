package com.aozijx.passly.data.observability

import com.aozijx.passly.core.observability.AppStackFrame
import com.aozijx.passly.core.observability.EnumName
import com.aozijx.passly.core.observability.ErrorCode
import com.aozijx.passly.core.observability.ErrorPayload
import com.aozijx.passly.core.observability.EventId
import com.aozijx.passly.core.observability.EventKey
import com.aozijx.passly.core.observability.Events
import com.aozijx.passly.core.observability.ExceptionSnapshot
import com.aozijx.passly.core.observability.ExceptionKind
import com.aozijx.passly.core.observability.ExceptionType
import com.aozijx.passly.core.observability.HashedIdentifier
import com.aozijx.passly.core.observability.LogCategory
import com.aozijx.passly.core.observability.LogFieldKey
import com.aozijx.passly.core.observability.LogFields
import com.aozijx.passly.core.observability.LogLevel
import com.aozijx.passly.core.observability.LogValue
import com.aozijx.passly.core.observability.OperationCode
import com.aozijx.passly.core.observability.ProcessSessionId
import com.aozijx.passly.core.observability.ReasonCode
import com.aozijx.passly.core.observability.SanitizedLogRecord
import com.aozijx.passly.core.observability.SecureSessionId
import com.aozijx.passly.core.observability.SpanId
import com.aozijx.passly.core.observability.TraceId
import com.aozijx.passly.core.observability.TraceSnapshot
import com.aozijx.passly.core.observability.category
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

object LogRecordCodec {
    private const val SCHEMA_VERSION = 3
    private const val MAX_STRING_BYTES = 4096
    private const val MAX_FIELDS = 64
    private const val MAX_FRAMES = 24
    const val MAX_RECORD_BYTES = 64 * 1024

    fun encode(record: SanitizedLogRecord): ByteArray {
        require(record.event in Events.All) { "Unregistered event" }
        require(record.category == record.event.category()) { "Event category mismatch" }
        when (record.level) {
            LogLevel.DEBUG, LogLevel.INFO -> require(record.error == null) {
                "Non-failure record cannot contain an error"
            }
            LogLevel.WARN, LogLevel.ERROR, LogLevel.FATAL -> require(record.error?.exception != null) {
                "Failure record requires an exception snapshot"
            }
        }
        require(record.fields.values.values.none { it is LogValue.HashedIdentifierInput }) {
            "Pending identifier cannot be persisted"
        }
        val bytes = ByteArrayOutputStream().use { buffer ->
            DataOutputStream(buffer).use { out ->
                out.writeInt(SCHEMA_VERSION)
                out.writeLong(record.timestampMs)
                out.writeLong(record.elapsedRealtimeMs)
                out.writeByte(record.level.ordinal)
                out.writeByte(record.category.ordinal)
                out.writeString(record.eventId.value, 64)
                out.writeString(record.event.value, 96)
                out.writeString(record.processSessionId.value, 64)
                out.writeNullableString(record.secureSessionId?.value, 64)
                out.writeString(record.trace.traceId.value, 64)
                out.writeString(record.trace.spanId.value, 64)
                out.writeNullableString(record.trace.parentSpanId?.value, 64)
                out.writeShort(record.fields.values.size)
                record.fields.values.forEach { (key, value) ->
                    out.writeString(key.value, 64)
                    out.writeValue(value)
                }
                out.writeBoolean(record.error != null)
                record.error?.let { out.writeError(it) }
            }
            buffer.toByteArray()
        }
        require(bytes.size <= MAX_RECORD_BYTES) { "Log record is too large" }
        return bytes
    }

    fun decode(bytes: ByteArray): SanitizedLogRecord {
        require(bytes.isNotEmpty() && bytes.size <= MAX_RECORD_BYTES) { "Invalid record length" }
        return ByteArrayInputStream(bytes).use { buffer ->
            DataInputStream(buffer).use { input ->
                require(input.readInt() == SCHEMA_VERSION) { "Unsupported record schema" }
                val timestampMs = input.readLong()
                val elapsedRealtimeMs = input.readLong()
                val level = enumAt<LogLevel>(input.readUnsignedByte(), "level")
                val category = enumAt<LogCategory>(input.readUnsignedByte(), "category")
                val eventId = EventId(input.readString(64))
                val event = EventKey(input.readString(96))
                require(event in Events.All) { "Unregistered event" }
                require(category == event.category()) { "Event category mismatch" }
                val processSessionId = ProcessSessionId(input.readString(64))
                val secureSessionId = input.readNullableString(64)?.let(::SecureSessionId)
                val traceId = TraceId(input.readString(64))
                val spanId = SpanId(input.readString(64))
                val parentSpanId = input.readNullableString(64)?.let(::SpanId)
                val fieldCount = input.readUnsignedShort()
                require(fieldCount <= MAX_FIELDS) { "Too many fields" }
                val values = linkedMapOf<LogFieldKey, LogValue>()
                repeat(fieldCount) {
                    val key = LogFieldKey(input.readString(64))
                    require(key !in values) { "Duplicate field" }
                    values[key] = input.readValue()
                }
                val error = when (input.readStrictBoolean()) {
                    true -> input.readError()
                    false -> null
                }
                when (level) {
                    LogLevel.DEBUG, LogLevel.INFO -> require(error == null) {
                        "Non-failure record cannot contain an error"
                    }
                    LogLevel.WARN, LogLevel.ERROR, LogLevel.FATAL -> require(error?.exception != null) {
                        "Failure record requires an exception snapshot"
                    }
                }
                require(buffer.available() == 0) { "Trailing bytes after log record" }
                SanitizedLogRecord(
                    eventId = eventId,
                    timestampMs = timestampMs,
                    elapsedRealtimeMs = elapsedRealtimeMs,
                    level = level,
                    category = category,
                    event = event,
                    processSessionId = processSessionId,
                    secureSessionId = secureSessionId,
                    trace = TraceSnapshot(traceId, spanId, parentSpanId),
                    fields = LogFields.fromSanitized(values),
                    error = error,
                )
            }
        }
    }

    private fun DataOutputStream.writeValue(value: LogValue) = when (value) {
        is LogValue.Count -> { require(value.value >= 0); writeByte(1); writeLong(value.value) }
        is LogValue.DurationMs -> { require(value.value >= 0); writeByte(2); writeLong(value.value) }
        is LogValue.Ratio -> {
            require(value.value.isFinite() && value.value in 0.0..1.0)
            writeByte(3); writeDouble(value.value)
        }
        is LogValue.BooleanValue -> { writeByte(4); writeBoolean(value.value) }
        is LogValue.EnumValue -> { writeByte(5); writeString(value.value.value, 64) }
        is LogValue.ErrorCodeValue -> { writeByte(6); writeString(value.value.value, 64) }
        is LogValue.ReasonCodeValue -> { writeByte(7); writeString(value.value.value, 64) }
        is LogValue.OperationCodeValue -> { writeByte(8); writeString(value.value.value, 127) }
        is LogValue.HashedIdentifierValue -> { writeByte(9); writeString(value.value.value, 67) }
        is LogValue.HashedIdentifierInput -> error("Pending identifier cannot be persisted")
    }

    private fun DataInputStream.readValue(): LogValue = when (readUnsignedByte()) {
        1 -> LogValue.Count(readLong().also { require(it >= 0) })
        2 -> LogValue.DurationMs(readLong().also { require(it >= 0) })
        3 -> LogValue.Ratio(readDouble().also { require(it.isFinite() && it in 0.0..1.0) })
        4 -> LogValue.BooleanValue(readStrictBoolean())
        5 -> LogValue.EnumValue(EnumName(readString(64)))
        6 -> LogValue.ErrorCodeValue(ErrorCode(readString(64)))
        7 -> LogValue.ReasonCodeValue(ReasonCode(readString(64)))
        8 -> LogValue.OperationCodeValue(OperationCode(readString(127)))
        9 -> LogValue.HashedIdentifierValue(HashedIdentifier(readString(67)))
        else -> throw IllegalArgumentException("Unknown log value type")
    }

    private fun DataOutputStream.writeError(error: ErrorPayload) {
        writeString(error.errorCode.value, 64)
        writeString(error.reason.value, 64)
        writeBoolean(error.exception != null)
        error.exception?.let { snapshot ->
            writeByte(snapshot.kind.ordinal)
            writeString(snapshot.type.value, 64)
            writeShort(snapshot.appStackFrames.size)
            snapshot.appStackFrames.forEach { writeString(it.value, 256) }
            writeShort(snapshot.causeTypes.size)
            snapshot.causeTypes.forEach { writeString(it.value, 64) }
            writeInt(snapshot.foreignFrameCount)
        }
    }

    private fun DataInputStream.readError(): ErrorPayload {
        val code = ErrorCode(readString(64))
        val reason = ReasonCode(readString(64))
        val exception = if (readStrictBoolean()) {
            val kind = enumAt<ExceptionKind>(readUnsignedByte(), "exception kind")
            val type = ExceptionType(readString(64))
            val frameCount = readUnsignedShort().also { require(it <= MAX_FRAMES) }
            val frames = List(frameCount) { AppStackFrame(readString(256)) }
            val causeCount = readUnsignedShort().also { require(it <= 3) }
            val causes = List(causeCount) { ExceptionType(readString(64)) }
            val foreignCount = readInt().also { require(it >= 0) }
            ExceptionSnapshot(type, frames, causes, foreignCount, kind)
        } else null
        return ErrorPayload(code, reason, exception)
    }

    private fun DataOutputStream.writeString(value: String, maxBytes: Int) {
        val bytes = value.toByteArray(StandardCharsets.UTF_8)
        require(bytes.isNotEmpty() && bytes.size <= maxBytes.coerceAtMost(MAX_STRING_BYTES)) {
            "Invalid string length"
        }
        writeShort(bytes.size)
        write(bytes)
    }

    private fun DataOutputStream.writeNullableString(value: String?, maxBytes: Int) {
        writeBoolean(value != null)
        value?.let { writeString(it, maxBytes) }
    }

    private fun DataInputStream.readString(maxBytes: Int): String {
        val length = readUnsignedShort()
        require(length in 1..maxBytes.coerceAtMost(MAX_STRING_BYTES)) { "Invalid string length" }
        val bytes = ByteArray(length).also(::readFully)
        return StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
    }

    private fun DataInputStream.readNullableString(maxBytes: Int): String? =
        if (readStrictBoolean()) readString(maxBytes) else null

    private fun DataInputStream.readStrictBoolean(): Boolean = when (val value = readUnsignedByte()) {
        0 -> false
        1 -> true
        else -> throw IllegalArgumentException("Invalid boolean $value")
    }

    private inline fun <reified T : Enum<T>> enumAt(ordinal: Int, label: String): T =
        enumValues<T>().getOrNull(ordinal)
            ?: throw IllegalArgumentException("Invalid $label ordinal")
}
