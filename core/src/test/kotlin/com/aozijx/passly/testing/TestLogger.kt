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

fun testLogger(): Logger = object : Logger {
    override fun debug(event: EventKey, fields: LogFields) = Unit
    override fun info(event: EventKey, fields: LogFields) = Unit
    override fun degraded(event: EventKey, errorCode: ErrorCode, reason: ReasonCode, fields: LogFields) = Unit
    override fun warn(event: EventKey, errorCode: ErrorCode, reason: ReasonCode, throwable: Throwable, fields: LogFields) = Unit
    override fun error(event: EventKey, errorCode: ErrorCode, reason: ReasonCode, throwable: Throwable, fields: LogFields) = Unit
    override fun fatal(event: EventKey, errorCode: ErrorCode, reason: ReasonCode, throwable: Throwable, fields: LogFields) = Unit

    override suspend fun <T> withRootTrace(operation: OperationCode, block: suspend TraceScope.() -> T): T =
        TestTraceScope.block()

    override suspend fun <T> inSpan(operation: OperationCode, block: suspend TraceScope.() -> T): T =
        TestTraceScope.block()

    override fun captureTraceToken(singleUse: Boolean): TraceToken =
        error("Trace token capture is not used by this test logger")

    override suspend fun <T> resumeTrace(
        token: TraceToken,
        operation: OperationCode,
        block: suspend TraceScope.() -> T,
    ): TraceResumeResult<T> = error("Trace resume is not used by this test logger")
}

private object TestTraceScope : TraceScope {
    override val traceId = TraceId("test_trace")
    override val spanId = SpanId("test_span")
    override val parentSpanId: SpanId? = null
}
