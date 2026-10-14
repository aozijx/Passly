package com.aozijx.passly.core.observability.trace

import com.aozijx.passly.core.observability.OperationCode
import com.aozijx.passly.core.observability.ProcessSessionId
import com.aozijx.passly.core.observability.SpanId
import com.aozijx.passly.core.observability.TraceId
import java.util.UUID

@JvmInline
value class TraceToken(val value: String) {
    init {
        require(value.matches(Regex("^[A-Za-z0-9_-]{8,64}$"))) { "Invalid trace token" }
    }
}

class TraceTokenRegistry(
    private val nowMs: () -> Long,
    private val tokenFactory: () -> TraceToken = {
        TraceToken(UUID.randomUUID().toString().replace("-", ""))
    },
    private val ttlMs: Long = MAX_TTL_MS,
    private val capacity: Int = MAX_CAPACITY,
) {
    private val entries = LinkedHashMap<TraceToken, Entry>(capacity, 0.75f, true)

    init {
        require(ttlMs in 1..MAX_TTL_MS) { "Trace token TTL exceeds the supported window" }
        require(capacity in 1..MAX_CAPACITY) { "Trace token capacity exceeds the supported bound" }
    }

    @Synchronized
    fun capture(
        context: TraceContext,
        processSessionId: ProcessSessionId,
        singleUse: Boolean = true,
    ): TraceToken {
        pruneExpired()
        while (entries.size >= capacity) {
            entries.entries.iterator().run {
                next()
                remove()
            }
        }
        var token = tokenFactory()
        while (token in entries) token = tokenFactory()
        entries[token] = Entry(
            traceId = context.traceId,
            spanId = context.spanId,
            parentSpanId = context.parentSpanId,
            operation = context.operation,
            failureDiagnosticId = context.failureDiagnosticId,
            processSessionId = processSessionId,
            expiresAtMs = nowMs() + ttlMs,
            singleUse = singleUse,
        )
        return token
    }

    @Synchronized
    fun consume(token: TraceToken, processSessionId: ProcessSessionId): TraceContext? {
        pruneExpired()
        val entry = entries[token] ?: return null
        if (entry.processSessionId != processSessionId) {
            entries.remove(token)
            return null
        }
        if (entry.singleUse) entries.remove(token)
        return TraceContext.resumed(
            traceId = entry.traceId,
            spanId = entry.spanId,
            parentSpanId = entry.parentSpanId,
            operation = entry.operation,
            failureDiagnosticId = entry.failureDiagnosticId,
        )
    }

    private fun pruneExpired() {
        val now = nowMs()
        entries.entries.removeAll { it.value.expiresAtMs <= now }
    }

    private data class Entry(
        val traceId: TraceId,
        val spanId: SpanId,
        val parentSpanId: SpanId?,
        val operation: OperationCode,
        val failureDiagnosticId: FailureDiagnosticId,
        val processSessionId: ProcessSessionId,
        val expiresAtMs: Long,
        val singleUse: Boolean,
    )

    private companion object {
        const val MAX_TTL_MS = 10 * 60 * 1_000L
        const val MAX_CAPACITY = 128
    }
}
