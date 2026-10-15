package com.aozijx.passly.core.observability

import com.aozijx.passly.core.observability.trace.TraceToken

sealed interface TraceResumeResult<out T> {
    data class Resumed<T>(val value: T) : TraceResumeResult<T>
    data object Unavailable : TraceResumeResult<Nothing>
}

interface TraceScope {
    val traceId: TraceId
    val spanId: SpanId
    val parentSpanId: SpanId?
}

interface Logger {
    fun log(statement: LogStatement) = statement.writeTo(this)

    fun debug(event: EventKey, fields: LogFields = LogFields.Empty)
    fun info(event: EventKey, fields: LogFields = LogFields.Empty)

    fun degraded(
        event: EventKey,
        errorCode: ErrorCode,
        reason: ReasonCode,
        fields: LogFields = LogFields.Empty,
    )

    fun warn(
        event: EventKey,
        errorCode: ErrorCode,
        reason: ReasonCode,
        throwable: Throwable,
        fields: LogFields = LogFields.Empty,
    )

    fun error(
        event: EventKey,
        errorCode: ErrorCode,
        reason: ReasonCode,
        throwable: Throwable,
        fields: LogFields = LogFields.Empty,
    )

    fun fatal(
        event: EventKey,
        errorCode: ErrorCode,
        reason: ReasonCode,
        throwable: Throwable,
        fields: LogFields = LogFields.Empty,
    )

    suspend fun <T> withRootTrace(
        operation: OperationCode,
        block: suspend TraceScope.() -> T,
    ): T

    suspend fun <T> inSpan(
        operation: OperationCode,
        block: suspend TraceScope.() -> T,
    ): T

    fun captureTraceToken(singleUse: Boolean = true): TraceToken

    suspend fun <T> resumeTrace(
        token: TraceToken,
        operation: OperationCode,
        block: suspend TraceScope.() -> T,
    ): TraceResumeResult<T>
}
