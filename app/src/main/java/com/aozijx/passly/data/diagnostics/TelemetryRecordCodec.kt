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

    fun encode(event: TelemetryEvent): ByteArray {
        val nameBytes = event.name.toByteArray(StandardCharsets.UTF_8)
        val correlationBytes = event.correlationId.toByteArray(StandardCharsets.UTF_8)
        val throwableBytes = event.throwableType?.toByteArray(StandardCharsets.UTF_8)
        val frameBytes = event.appStackFrames.map { it.toByteArray(StandardCharsets.UTF_8) }

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
        return ByteArrayInputStream(bytes).use { bis ->
            DataInputStream(bis).use { input ->
                val timestamp = input.readLong()
                val level = EventLevel.entries[input.readByte().toInt()]
                val category = EventCategory.entries[input.readByte().toInt()]
                val name = readString(input)
                val fields = mutableMapOf<String, SafeLogValue>()
                val fieldsCount = input.readShort().toInt()
                repeat(fieldsCount) {
                    val key = readString(input)
                    val value = decodeFieldValue(input)
                    fields[key] = value
                }
                val throwableType = readStringOrNull(input)
                val framesCount = input.readShort().toInt()
                val frames = List(framesCount) { readString(input) }
                val correlationId = readString(input)
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
            5 -> SafeLogValue.EnumName(readString(input))
            6 -> SafeLogValue.ErrorCodeValue(
                com.aozijx.passly.core.telemetry.ErrorCode(
                    readString(
                        input
                    )
                )
            )

            7 -> SafeLogValue.OperationCodeValue(
                com.aozijx.passly.core.telemetry.OperationCode(
                    readString(input)
                )
            )

            else -> throw IllegalArgumentException("Unknown field type")
        }
    }

    private fun readString(input: DataInputStream): String {
        val len = input.readShort().toInt()
        return if (len > 0) {
            val bytes = ByteArray(len); input.readFully(bytes)
            String(bytes, StandardCharsets.UTF_8)
        } else ""
    }

    private fun readStringOrNull(input: DataInputStream): String? {
        val len = input.readShort().toInt()
        return if (len > 0) {
            val bytes = ByteArray(len); input.readFully(bytes)
            String(bytes, StandardCharsets.UTF_8)
        } else null
    }
}
