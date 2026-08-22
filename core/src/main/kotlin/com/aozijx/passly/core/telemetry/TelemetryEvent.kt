package com.aozijx.passly.core.telemetry

import java.util.UUID

/**
 * 遥测事件（纯模型）。
 *
 * 磁盘编码由 [com.aozijx.passly.data.diagnostics.TelemetryRecordCodec] 负责。
 * 本类不包含序列化逻辑。
 */
data class TelemetryEvent(
    val level: EventLevel,
    val category: EventCategory,
    val name: String,
    val fields: Map<String, SafeLogValue> = emptyMap(),
    val throwableType: String? = null,
    val appStackFrames: List<String> = emptyList(),
    val correlationId: String = UUID.randomUUID().toString(),
    val timestampMs: Long = System.currentTimeMillis()
)
