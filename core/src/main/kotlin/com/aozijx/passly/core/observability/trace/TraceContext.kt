package com.aozijx.passly.core.observability.trace

import com.aozijx.passly.core.observability.DiagnosticId
import com.aozijx.passly.core.observability.OperationCode
import com.aozijx.passly.core.observability.SpanId
import com.aozijx.passly.core.observability.TraceId
import com.aozijx.passly.core.observability.TraceScope
import com.aozijx.passly.core.observability.TraceSnapshot

internal class FailureDiagnosticId(initialValue: DiagnosticId? = null) {
    private var value: DiagnosticId? = initialValue

    @Synchronized
    fun getOrCreate(factory: () -> DiagnosticId): DiagnosticId = value ?: factory().also { value = it }
}

class TraceContext internal constructor(
    override val traceId: TraceId,
    override val spanId: SpanId,
    override val parentSpanId: SpanId?,
    val operation: OperationCode,
    internal val failureDiagnosticId: FailureDiagnosticId,
) : TraceScope {
    fun snapshot(): TraceSnapshot = TraceSnapshot(traceId, spanId, parentSpanId)

    internal fun child(childSpanId: SpanId, childOperation: OperationCode): TraceContext =
        TraceContext(
            traceId = traceId,
            spanId = childSpanId,
            parentSpanId = spanId,
            operation = childOperation,
            failureDiagnosticId = failureDiagnosticId,
        )

    companion object {
        fun currentOrNull(): TraceContext? = TraceContextElement.currentOrNull()

        internal fun root(
            traceId: TraceId,
            spanId: SpanId,
            operation: OperationCode,
            diagnosticId: DiagnosticId? = null,
        ): TraceContext = TraceContext(
            traceId = traceId,
            spanId = spanId,
            parentSpanId = null,
            operation = operation,
            failureDiagnosticId = FailureDiagnosticId(diagnosticId),
        )

        internal fun resumed(
            traceId: TraceId,
            spanId: SpanId,
            parentSpanId: SpanId?,
            operation: OperationCode,
            failureDiagnosticId: FailureDiagnosticId,
        ): TraceContext = TraceContext(
            traceId = traceId,
            spanId = spanId,
            parentSpanId = parentSpanId,
            operation = operation,
            failureDiagnosticId = failureDiagnosticId,
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
