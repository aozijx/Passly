package com.aozijx.passly.testing

import com.aozijx.passly.core.observability.ErrorCode
import com.aozijx.passly.core.observability.EventKey
import com.aozijx.passly.core.observability.LogFields
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.OperationCode
import com.aozijx.passly.core.observability.ReasonCode
import com.aozijx.passly.core.observability.SpanId
import com.aozijx.passly.core.observability.TraceId
import com.aozijx.passly.core.observability.TraceResumeResult
import com.aozijx.passly.core.observability.TraceScope
import com.aozijx.passly.core.observability.trace.TraceToken

internal fun testLogger(): Logger = object : Logger {
    override fun debug(event: EventKey, fields: LogFields) = Unit
    override fun info(event: EventKey, fields: LogFields) = Unit
    override fun degraded(event: EventKey, errorCode: ErrorCode, reason: ReasonCode, fields: LogFields) = Unit
    override fun warn(event: EventKey, errorCode: ErrorCode, reason: ReasonCode, throwable: Throwable, fields: LogFields) = Unit
    override fun error(event: EventKey, errorCode: ErrorCode, reason: ReasonCode, throwable: Throwable, fields: LogFields) = Unit
    override fun fatal(event: EventKey, errorCode: ErrorCode, reason: ReasonCode, throwable: Throwable, fields: LogFields) = Unit

    override suspend fun <T> withRootTrace(
        operation: OperationCode,
        block: suspend TraceScope.() -> T,
    ): T = TestTraceScope.block()

    override suspend fun <T> inSpan(
        operation: OperationCode,
        block: suspend TraceScope.() -> T,
    ): T = TestTraceScope.block()

    override fun captureTraceToken(singleUse: Boolean): TraceToken = error("not used")

    override suspend fun <T> resumeTrace(
        token: TraceToken,
        operation: OperationCode,
        block: suspend TraceScope.() -> T,
    ): TraceResumeResult<T> = error("not used")
}

internal data class RecordedTestLog(
    val event: EventKey,
    val fields: LogFields,
    val throwable: Throwable? = null,
)

internal class RecordingTestLogger : Logger {
    val events = mutableListOf<RecordedTestLog>()
    val spans = mutableListOf<OperationCode>()

    override fun debug(event: EventKey, fields: LogFields) { events += RecordedTestLog(event, fields) }
    override fun info(event: EventKey, fields: LogFields) { events += RecordedTestLog(event, fields) }
    override fun degraded(event: EventKey, errorCode: ErrorCode, reason: ReasonCode, fields: LogFields) {
        events += RecordedTestLog(event, fields)
    }
    override fun warn(
        event: EventKey,
        errorCode: ErrorCode,
        reason: ReasonCode,
        throwable: Throwable,
        fields: LogFields,
    ) { events += RecordedTestLog(event, fields, throwable) }
    override fun error(
        event: EventKey,
        errorCode: ErrorCode,
        reason: ReasonCode,
        throwable: Throwable,
        fields: LogFields,
    ) { events += RecordedTestLog(event, fields, throwable) }
    override fun fatal(
        event: EventKey,
        errorCode: ErrorCode,
        reason: ReasonCode,
        throwable: Throwable,
        fields: LogFields,
    ) { events += RecordedTestLog(event, fields, throwable) }

    override suspend fun <T> withRootTrace(
        operation: OperationCode,
        block: suspend TraceScope.() -> T,
    ): T = inSpan(operation, block)

    override suspend fun <T> inSpan(
        operation: OperationCode,
        block: suspend TraceScope.() -> T,
    ): T {
        spans += operation
        return TestTraceScope.block()
    }

    override fun captureTraceToken(singleUse: Boolean): TraceToken = error("not used")
    override suspend fun <T> resumeTrace(
        token: TraceToken,
        operation: OperationCode,
        block: suspend TraceScope.() -> T,
    ): TraceResumeResult<T> = error("not used")
}

private object TestTraceScope : TraceScope {
    override val traceId = TraceId("test_trace")
    override val spanId = SpanId("test_span")
    override val parentSpanId: SpanId? = null
}
