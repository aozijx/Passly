package com.aozijx.passly.core.observability.metrics

import com.aozijx.passly.core.observability.ErrorCode
import com.aozijx.passly.core.observability.EventKey
import com.aozijx.passly.core.observability.LogCategory
import com.aozijx.passly.core.observability.LogFields
import com.aozijx.passly.core.observability.LogLevel
import com.aozijx.passly.core.observability.ReasonCode

/** Bounded aggregate derived only from data that already crossed the privacy boundary. */
data class MetricSnapshot(
    val event: EventKey,
    val level: LogLevel,
    val category: LogCategory,
    val dimensions: LogFields,
    val errorCode: ErrorCode?,
    val reason: ReasonCode?,
    val count: Long,
    val firstTimestampMs: Long,
    val lastTimestampMs: Long,
    val minElapsedMs: Long?,
    val maxElapsedMs: Long?,
    val averageElapsedMs: Double?,
)
