package com.aozijx.passly.core.observability

interface TraceScope {
    val traceId: TraceId
    val spanId: SpanId
    val parentSpanId: SpanId?
}

interface Logger {
    fun debug(event: EventKey, fields: LogFields = LogFields.Empty)
    fun info(event: EventKey, fields: LogFields = LogFields.Empty)

    fun degraded(
        event: EventKey,
        errorCode: ErrorCode,
        reason: ReasonCode,
        fields: LogFields = LogFields.Empty,
    ): DiagnosticId

    fun warn(
        event: EventKey,
        errorCode: ErrorCode,
        reason: ReasonCode,
        throwable: Throwable,
        fields: LogFields = LogFields.Empty,
    ): DiagnosticId

    fun error(
        event: EventKey,
        errorCode: ErrorCode,
        reason: ReasonCode,
        throwable: Throwable,
        fields: LogFields = LogFields.Empty,
    ): DiagnosticId

    suspend fun <T> withRootTrace(
        operation: OperationCode,
        block: suspend TraceScope.() -> T,
    ): T

    suspend fun <T> inSpan(
        operation: OperationCode,
        block: suspend TraceScope.() -> T,
    ): T
}
