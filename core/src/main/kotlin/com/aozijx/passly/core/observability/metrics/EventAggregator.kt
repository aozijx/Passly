package com.aozijx.passly.core.observability.metrics

import com.aozijx.passly.core.observability.Events
import com.aozijx.passly.core.observability.LogFieldKeys
import com.aozijx.passly.core.observability.LogLevel
import com.aozijx.passly.core.observability.LogValue
import com.aozijx.passly.core.observability.SanitizedLogRecord
import java.util.ArrayDeque
import java.util.LinkedHashMap

/** In-process aggregation and deterministic DEBUG sampling; it performs no I/O. */
class EventAggregator(
    private val capacity: Int = DEFAULT_CAPACITY,
    private val windowMs: Long = DEFAULT_WINDOW_MS,
    private val debugSamplePercent: Int = DEFAULT_DEBUG_SAMPLE_PERCENT,
    private val snapshotSink: (MetricSnapshot) -> Unit = {},
) {
    private val buckets = LinkedHashMap<MetricKey, Bucket>(capacity, 0.75f, true)
    private val emittedSnapshots = ArrayDeque<MetricSnapshot>(capacity)
    private var dispatchingSnapshot = false

    init {
        require(capacity > 0) { "Aggregator capacity must be positive" }
        require(windowMs > 0) { "Aggregation window must be positive" }
        require(debugSamplePercent in 0..100) { "Debug sample percent must be in 0..100" }
    }

    val activeBucketCount: Int
        @Synchronized get() = buckets.size

    /** Returns whether the original record should continue to the log sinks. */
    @Synchronized
    fun record(record: SanitizedLogRecord): Boolean {
        if (dispatchingSnapshot) return true
        flushExpired(record.timestampMs)
        val key = MetricKey.from(record)
        val existing = buckets[key]
        if (existing == null) {
            if (buckets.size >= capacity) flushKey(buckets.entries.first().key)
            buckets[key] = Bucket.from(record)
        } else {
            existing.add(record)
        }
        return shouldEmit(record)
    }

    @Synchronized
    fun flush(): List<MetricSnapshot> {
        val flushed = buckets.keys.toList().mapNotNull(::flushKey)
        return flushed
    }

    @Synchronized
    fun snapshots(): List<MetricSnapshot> = emittedSnapshots.toList()

    private fun flushExpired(nowMs: Long) {
        buckets.entries
            .filter { nowMs - it.value.firstTimestampMs >= windowMs }
            .map { it.key }
            .forEach(::flushKey)
    }

    private fun flushKey(key: MetricKey): MetricSnapshot? {
        val bucket = buckets.remove(key) ?: return null
        val snapshot = bucket.snapshot(key)
        if (emittedSnapshots.size >= capacity) emittedSnapshots.removeFirst()
        emittedSnapshots.addLast(snapshot)
        dispatchingSnapshot = true
        try {
            snapshotSink(snapshot)
        } finally {
            dispatchingSnapshot = false
        }
        return snapshot
    }

    private fun shouldEmit(record: SanitizedLogRecord): Boolean {
        if (record.level != LogLevel.DEBUG || record.event in NEVER_SAMPLE_EVENTS) return true
        if (debugSamplePercent == 0) return false
        if (debugSamplePercent == 100) return true
        val stableSeed = "${record.event.value}|${record.processSessionId.value}".hashCode()
        val bucket = (stableSeed and Int.MAX_VALUE) % 100
        return bucket < debugSamplePercent
    }

    private data class MetricKey(
        val event: com.aozijx.passly.core.observability.EventKey,
        val level: LogLevel,
        val category: com.aozijx.passly.core.observability.LogCategory,
        val fields: com.aozijx.passly.core.observability.LogFields,
        val errorCode: com.aozijx.passly.core.observability.ErrorCode?,
        val reason: com.aozijx.passly.core.observability.ReasonCode?,
    ) {
        companion object {
            fun from(record: SanitizedLogRecord) = MetricKey(
                record.event,
                record.level,
                record.category,
                record.fields,
                record.error?.errorCode,
                record.error?.reason,
            )
        }
    }

    private data class Bucket(
        var count: Long,
        val firstTimestampMs: Long,
        var lastTimestampMs: Long,
        var elapsedCount: Long,
        var elapsedTotalMs: Long,
        var minElapsedMs: Long?,
        var maxElapsedMs: Long?,
    ) {
        fun add(record: SanitizedLogRecord) {
            count++
            lastTimestampMs = maxOf(lastTimestampMs, record.timestampMs)
            record.metricElapsedMs()?.let(::addElapsed)
        }

        fun snapshot(key: MetricKey) = MetricSnapshot(
            event = key.event,
            level = key.level,
            category = key.category,
            dimensions = key.fields,
            errorCode = key.errorCode,
            reason = key.reason,
            count = count,
            firstTimestampMs = firstTimestampMs,
            lastTimestampMs = lastTimestampMs,
            minElapsedMs = minElapsedMs,
            maxElapsedMs = maxElapsedMs,
            averageElapsedMs = elapsedCount.takeIf { it > 0 }?.let { elapsedTotalMs.toDouble() / it },
        )

        private fun addElapsed(value: Long) {
            elapsedCount++
            elapsedTotalMs += value
            minElapsedMs = minElapsedMs?.let { minOf(it, value) } ?: value
            maxElapsedMs = maxElapsedMs?.let { maxOf(it, value) } ?: value
        }

        companion object {
            fun from(record: SanitizedLogRecord): Bucket = Bucket(
                count = 1,
                firstTimestampMs = record.timestampMs,
                lastTimestampMs = record.timestampMs,
                elapsedCount = 0,
                elapsedTotalMs = 0,
                minElapsedMs = null,
                maxElapsedMs = null,
            ).also { bucket -> record.metricElapsedMs()?.let(bucket::addElapsed) }
        }
    }

    private companion object {
        const val DEFAULT_CAPACITY = 256
        const val DEFAULT_WINDOW_MS = 5 * 60 * 1000L
        const val DEFAULT_DEBUG_SAMPLE_PERCENT = 25
        val NEVER_SAMPLE_EVENTS = setOf(
            Events.Autofill.ResultDeliveryStarted,
            Events.Autofill.ResultDelivered,
            Events.Autofill.ResultDeliveryFailed,
        )
    }
}

private fun SanitizedLogRecord.metricElapsedMs(): Long? =
    (fields.values[LogFieldKeys.ElapsedMs] as? LogValue.DurationMs)?.value
