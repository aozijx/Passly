package com.aozijx.passly.core.observability.trace

import com.aozijx.passly.core.observability.OperationCode
import com.aozijx.passly.core.observability.SpanId
import com.aozijx.passly.core.observability.TraceId
import com.aozijx.passly.core.observability.TraceScope
import com.aozijx.passly.core.observability.TraceSnapshot

class TraceContext internal constructor(
    override val traceId: TraceId,
    override val spanId: SpanId,
    override val parentSpanId: SpanId?,
    val operation: OperationCode,
) : TraceScope {
    fun snapshot(): TraceSnapshot = TraceSnapshot(traceId, spanId, parentSpanId)

    internal fun child(childSpanId: SpanId, childOperation: OperationCode): TraceContext =
        TraceContext(
            traceId = traceId,
            spanId = childSpanId,
            parentSpanId = spanId,
            operation = childOperation,
        )

    companion object {
        fun currentOrNull(): TraceContext? = TraceContextElement.currentOrNull()

        internal fun root(
            traceId: TraceId,
            spanId: SpanId,
            operation: OperationCode,
        ): TraceContext = TraceContext(
            traceId = traceId,
            spanId = spanId,
            parentSpanId = null,
            operation = operation,
        )

        internal fun resumed(
            traceId: TraceId,
            spanId: SpanId,
            parentSpanId: SpanId?,
            operation: OperationCode,
        ): TraceContext = TraceContext(
            traceId = traceId,
            spanId = spanId,
            parentSpanId = parentSpanId,
            operation = operation,
        )
    }
}

enum class SpanResult { SUCCESS, FAILURE, CANCELLED }

data class SpanStarted(
    val operation: OperationCode,
    val trace: TraceSnapshot,
    val startedElapsedRealtimeMs: Long,
)

data class SpanCompleted(
    val operation: OperationCode,
    val trace: TraceSnapshot,
    val elapsedMs: Long,
    val result: SpanResult,
)

interface TraceLifecycleSink {
    fun onSpanStarted(span: SpanStarted)
    fun onSpanCompleted(span: SpanCompleted)
}
