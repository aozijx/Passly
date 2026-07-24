package com.aozijx.passly.data.diagnostics

import com.aozijx.passly.core.telemetry.EventCategory
import com.aozijx.passly.core.telemetry.EventLevel
import com.aozijx.passly.core.telemetry.SafeLogValue
import com.aozijx.passly.core.telemetry.TelemetryEvent
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.charset.StandardCharsets

/**
 * 遥测记录编解码器。
 *
 * 使用长度前缀二进制格式（非 pipe-delimited 文本），
 * 避免转义问题和格式注入：
 *
 * ```
 * [timestampMs: i8][level: i1][category: i1][name_len: i2][name: utf8]
 * [fields_count: i2][for each field: key_len i2, key utf8, type i1, value]
 * [throwable_len: i2][throwable: utf8]
 * [frames_count: i2][for each frame: frame_len i2, frame: utf8]
 * [correlation_len: i2][correlation: utf8]
 * ```
 */
object TelemetryRecordCodec {

    // ============================== 边界常量 ==============================

    /** 单个字符串最大 UTF-8 字节数（兜底） */
    const val MAX_STRING_BYTES = 4096

    /** 事件名最大长度 */
    const val MAX_NAME_BYTES = 256

    /** 字段 key 最大长度 */
    const val MAX_FIELD_KEY_BYTES = 128

    /** 单条事件最大字段数 */
    const val MAX_FIELDS_COUNT = 64

    /** 最大栈帧数 */
    const val MAX_FRAMES_COUNT = 64

    /** 单帧字符串最大长度 */
    const val MAX_FRAME_BYTES = 512

    /** correlationId 最大长度（UUID 36 字符） */
    const val MAX_CORRELATION_BYTES = 64

    /** throwableType 最大长度 */
    const val MAX_THROWABLE_BYTES = 256

    /** SafeLogValue.EnumName 最大长度 */
    const val MAX_ENUM_NAME_BYTES = 64

    /** SafeLogValue.ErrorCodeValue / OperationCodeValue 最大长度 */
    const val MAX_CODE_BYTES = 128

    fun encode(event: TelemetryEvent): ByteArray {
        require(event.name.isNotEmpty()) { "Event name must not be empty" }

        val nameBytes = event.name.toByteArray(StandardCharsets.UTF_8)
        require(nameBytes.size <= MAX_NAME_BYTES) {
            "Event name exceeds $MAX_NAME_BYTES bytes: ${nameBytes.size}"
        }

        val correlationBytes = event.correlationId.toByteArray(StandardCharsets.UTF_8)
        require(correlationBytes.size <= MAX_CORRELATION_BYTES) {
            "correlationId exceeds $MAX_CORRELATION_BYTES bytes: ${correlationBytes.size}"
        }

        val throwableBytes = event.throwableType?.toByteArray(StandardCharsets.UTF_8)
        throwableBytes?.let {
            require(it.size <= MAX_THROWABLE_BYTES) {
                "throwableType exceeds $MAX_THROWABLE_BYTES bytes: ${it.size}"
            }
        }

        require(event.fields.size <= MAX_FIELDS_COUNT) {
            "Fields count ${event.fields.size} exceeds max $MAX_FIELDS_COUNT"
        }
        event.fields.keys.forEach { key ->
            val kb = key.toByteArray(StandardCharsets.UTF_8)
            require(kb.size <= MAX_FIELD_KEY_BYTES) {
                "Field key exceeds $MAX_FIELD_KEY_BYTES bytes: '${key.take(32)}' (${kb.size})"
            }
        }

        require(event.appStackFrames.size <= MAX_FRAMES_COUNT) {
            "Stack frames count ${event.appStackFrames.size} exceeds max $MAX_FRAMES_COUNT"
        }
        val frameBytes = event.appStackFrames.map { s ->
            val b = s.toByteArray(StandardCharsets.UTF_8)
            require(b.size <= MAX_FRAME_BYTES) {
                "Stack frame exceeds $MAX_FRAME_BYTES bytes: ${b.size}"
            }
            b
        }

        return ByteArrayOutputStream().use { bos ->
            DataOutputStream(bos).use { out ->
                out.writeLong(event.timestampMs)
                out.writeByte(event.level.ordinal)
                out.writeByte(event.category.ordinal)
                out.writeShort(nameBytes.size); out.write(nameBytes)

                // fields
                out.writeShort(event.fields.size)
                event.fields.forEach { (key, value) ->
                    val keyBytes = key.toByteArray(StandardCharsets.UTF_8)
                    out.writeShort(keyBytes.size); out.write(keyBytes)
                    encodeFieldValue(out, value)
                }

                // throwable type (optional)
                if (throwableBytes != null) {
                    out.writeShort(throwableBytes.size); out.write(throwableBytes)
                } else {
                    out.writeShort(0)
                }

                // stack frames
                out.writeShort(frameBytes.size)
                frameBytes.forEach { f ->
                    out.writeShort(f.size); out.write(f)
                }

                // correlation id
                out.writeShort(correlationBytes.size); out.write(correlationBytes)
            }
            bos.toByteArray()
        }
    }

    fun decode(bytes: ByteArray): TelemetryEvent {
        require(bytes.isNotEmpty()) { "Empty record bytes" }

        return ByteArrayInputStream(bytes).use { bis ->
            DataInputStream(bis).use { input ->
                val timestamp = input.readLong()
                val levelOrdinal = input.readByte().toInt()
                require(levelOrdinal in EventLevel.entries.indices) {
                    "Invalid EventLevel ordinal: $levelOrdinal"
                }
                val level = EventLevel.entries[levelOrdinal]

                val categoryOrdinal = input.readByte().toInt()
                require(categoryOrdinal in EventCategory.entries.indices) {
                    "Invalid EventCategory ordinal: $categoryOrdinal"
                }
                val category = EventCategory.entries[categoryOrdinal]

                val name = readString(input, MAX_NAME_BYTES)
                val fields = mutableMapOf<String, SafeLogValue>()
                val fieldsCount = input.readShort().toInt()
                require(fieldsCount in 0..MAX_FIELDS_COUNT) {
                    "Field count $fieldsCount exceeds max $MAX_FIELDS_COUNT"
                }
                repeat(fieldsCount) {
                    val key = readString(input, MAX_FIELD_KEY_BYTES)
                    val value = decodeFieldValue(input)
                    fields[key] = value
                }
                val throwableType = readStringOrNull(input, MAX_THROWABLE_BYTES)
                val framesCount = input.readShort().toInt()
                require(framesCount in 0..MAX_FRAMES_COUNT) {
                    "Frame count $framesCount exceeds max $MAX_FRAMES_COUNT"
                }
                val frames = List(framesCount) { readString(input, MAX_FRAME_BYTES) }
                val correlationId = readString(input, MAX_CORRELATION_BYTES)
                TelemetryEvent(
                    level = level,
                    category = category,
                    name = name,
                    fields = fields,
                    throwableType = throwableType,
                    appStackFrames = frames,
                    correlationId = correlationId,
                    timestampMs = timestamp
                )
            }
        }
    }

    // ============================== 字段编码 ==============================

    private fun encodeFieldValue(out: DataOutputStream, value: SafeLogValue) {
        when (value) {
            is SafeLogValue.Count -> {
                out.writeByte(1); out.writeLong(value.value)
            }

            is SafeLogValue.DurationMs -> {
                out.writeByte(2); out.writeLong(value.value)
            }

            is SafeLogValue.Ratio -> {
                out.writeByte(3); out.writeDouble(value.value)
            }

            is SafeLogValue.BooleanValue -> {
                out.writeByte(4); out.writeBoolean(value.value)
            }

            is SafeLogValue.EnumName -> {
                out.writeByte(5)
                val raw = value.name.toByteArray(StandardCharsets.UTF_8)
                out.writeShort(raw.size); out.write(raw)
            }

            is SafeLogValue.ErrorCodeValue -> {
                out.writeByte(6)
                val raw = value.code.value.toByteArray(StandardCharsets.UTF_8)
                out.writeShort(raw.size); out.write(raw)
            }

            is SafeLogValue.OperationCodeValue -> {
                out.writeByte(7)
                val raw = value.code.value.toByteArray(StandardCharsets.UTF_8)
                out.writeShort(raw.size); out.write(raw)
            }
        }
    }

    private fun decodeFieldValue(input: DataInputStream): SafeLogValue {
        return when (input.readByte().toInt()) {
            1 -> SafeLogValue.Count(input.readLong())
            2 -> SafeLogValue.DurationMs(input.readLong())
            3 -> SafeLogValue.Ratio(input.readDouble())
            4 -> SafeLogValue.BooleanValue(input.readBoolean())
            5 -> SafeLogValue.EnumName(readString(input, MAX_ENUM_NAME_BYTES))
            6 -> SafeLogValue.ErrorCodeValue(
                com.aozijx.passly.core.telemetry.ErrorCode(
                    readString(input, MAX_CODE_BYTES)
                )
            )

            7 -> SafeLogValue.OperationCodeValue(
                com.aozijx.passly.core.telemetry.OperationCode(
                    readString(input, MAX_CODE_BYTES)
                )
            )

            else -> throw IllegalArgumentException("Unknown field type")
        }
    }

    private fun readString(input: DataInputStream, maxBytes: Int = MAX_STRING_BYTES): String {
        val len = input.readShort().toInt()
        require(len in 0..maxBytes) {
            "String length $len exceeds max $maxBytes"
        }
        return if (len > 0) {
            val bytes = ByteArray(len); input.readFully(bytes)
            String(bytes, StandardCharsets.UTF_8)
        } else ""
    }

    private fun readStringOrNull(
        input: DataInputStream,
        maxBytes: Int = MAX_STRING_BYTES
    ): String? {
        val len = input.readShort().toInt()
        require(len in 0..maxBytes) {
            "String length $len exceeds max $maxBytes"
        }
        return if (len > 0) {
            val bytes = ByteArray(len); input.readFully(bytes)
            String(bytes, StandardCharsets.UTF_8)
        } else null
    }
}
