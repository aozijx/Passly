package com.aozijx.passly.core.observability.internal

import com.aozijx.passly.core.observability.DiagnosticId
import com.aozijx.passly.core.observability.ErrorCode
import com.aozijx.passly.core.observability.EventId
import com.aozijx.passly.core.observability.EventKey
import com.aozijx.passly.core.observability.LogFields
import com.aozijx.passly.core.observability.LogLevel
import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.OperationCode
import com.aozijx.passly.core.observability.PendingError
import com.aozijx.passly.core.observability.PendingLogEvent
import com.aozijx.passly.core.observability.ProcessSessionId
import com.aozijx.passly.core.observability.ReasonCode
import com.aozijx.passly.core.observability.SecureSessionId
import com.aozijx.passly.core.observability.SpanId
import com.aozijx.passly.core.observability.TraceId
import com.aozijx.passly.core.observability.TraceScope
import com.aozijx.passly.core.observability.privacy.LogSanitizer
import com.aozijx.passly.core.observability.privacy.SanitizationContext
import com.aozijx.passly.core.observability.privacy.SanitizationResult
import com.aozijx.passly.core.observability.sink.LogSink
import com.aozijx.passly.core.observability.trace.SpanCompleted
import com.aozijx.passly.core.observability.trace.SpanResult
import com.aozijx.passly.core.observability.trace.SpanStarted
import com.aozijx.passly.core.observability.trace.TraceContext
import com.aozijx.passly.core.observability.trace.TraceContextElement
import com.aozijx.passly.core.observability.trace.TraceLifecycleSink
import java.security.SecureRandom
import java.util.Base64
import java.util.concurrent.CancellationException
import kotlinx.coroutines.withContext

fun interface ProcessSessionProvider {
    fun current(): ProcessSessionId
}

fun interface SecureSessionProvider {
    fun currentOrNull(): SecureSessionId?
}

interface ObservabilityTimeSource {
    fun wallTimeMs(): Long
    fun elapsedRealtimeMs(): Long
}

interface ObservabilityIdGenerator {
    fun eventId(): EventId
    fun traceId(): TraceId
    fun spanId(): SpanId
    fun diagnosticId(): DiagnosticId
}

class SecureObservabilityIdGenerator(
    private val secureRandom: SecureRandom = SecureRandom(),
) : ObservabilityIdGenerator {
    override fun eventId(): EventId = EventId(randomOpaqueId())
    override fun traceId(): TraceId = TraceId(randomOpaqueId())
    override fun spanId(): SpanId = SpanId(randomOpaqueId(8))
    override fun diagnosticId(): DiagnosticId =
        DiagnosticId.fromEntropy(ByteArray(8).also(secureRandom::nextBytes))

    private fun randomOpaqueId(byteCount: Int = 16): String =
        Base64.getUrlEncoder().withoutPadding()
            .encodeToString(ByteArray(byteCount).also(secureRandom::nextBytes))
}

class DefaultLogger(
    private val sanitizer: LogSanitizer,
    private val sinks: List<LogSink>,
    private val lifecycleSinks: List<TraceLifecycleSink>,
    private val idGenerator: ObservabilityIdGenerator,
    private val timeSource: ObservabilityTimeSource,
    private val processSessionProvider: ProcessSessionProvider,
    private val secureSessionProvider: SecureSessionProvider,
) : Logger {
    override fun debug(event: EventKey, fields: LogFields) {
        emit(PendingLogEvent(LogLevel.DEBUG, event, fields))
    }

    override fun info(event: EventKey, fields: LogFields) {
        emit(PendingLogEvent(LogLevel.INFO, event, fields))
    }

    override fun degraded(
        event: EventKey,
        errorCode: ErrorCode,
        reason: ReasonCode,
        fields: LogFields,
    ): DiagnosticId = emitFailure(LogLevel.WARN, event, errorCode, reason, null, fields)

    override fun warn(
        event: EventKey,
        errorCode: ErrorCode,
        reason: ReasonCode,
        throwable: Throwable,
        fields: LogFields,
    ): DiagnosticId = emitFailure(LogLevel.WARN, event, errorCode, reason, throwable, fields)

    override fun error(
        event: EventKey,
        errorCode: ErrorCode,
        reason: ReasonCode,
        throwable: Throwable,
        fields: LogFields,
    ): DiagnosticId = emitFailure(LogLevel.ERROR, event, errorCode, reason, throwable, fields)

    override fun fatal(
        event: EventKey,
        errorCode: ErrorCode,
        reason: ReasonCode,
        throwable: Throwable,
        fields: LogFields,
    ): DiagnosticId = emitFailure(LogLevel.FATAL, event, errorCode, reason, throwable, fields)

    override suspend fun <T> withRootTrace(
        operation: OperationCode,
        block: suspend TraceScope.() -> T,
    ): T = runSpan(
        TraceContext.root(idGenerator.traceId(), idGenerator.spanId(), operation),
        block,
    )

    override suspend fun <T> inSpan(
        operation: OperationCode,
        block: suspend TraceScope.() -> T,
    ): T {
        val parent = TraceContext.currentOrNull()
            ?: return withRootTrace(operation, block)
        return runSpan(parent.child(idGenerator.spanId(), operation), block)
    }

    private suspend fun <T> runSpan(
        context: TraceContext,
        block: suspend TraceScope.() -> T,
    ): T {
        val startedAt = timeSource.elapsedRealtimeMs()
        notifyStarted(SpanStarted(context.operation, context.snapshot(), startedAt))
        var result = SpanResult.SUCCESS
        var originalFailure: Throwable? = null
        try {
            return withContext(TraceContextElement(context)) {
                try {
                    context.block()
                } catch (failure: Throwable) {
                    originalFailure = failure
                    throw failure
                }
            }
        } catch (failure: Throwable) {
            val businessFailure = originalFailure ?: failure
            result = if (businessFailure is CancellationException) {
                SpanResult.CANCELLED
            } else {
                SpanResult.FAILURE
            }
            throw businessFailure
        } finally {
            val elapsedMs = (timeSource.elapsedRealtimeMs() - startedAt).coerceAtLeast(0)
            notifyCompleted(SpanCompleted(context.operation, context.snapshot(), elapsedMs, result))
        }
    }

    private fun emitFailure(
        level: LogLevel,
        event: EventKey,
        errorCode: ErrorCode,
        reason: ReasonCode,
        throwable: Throwable?,
        fields: LogFields,
    ): DiagnosticId {
        val traceContext = requireTraceContext()
        val diagnosticId = traceContext.failureDiagnosticId.getOrCreate(idGenerator::diagnosticId)
        emit(
            PendingLogEvent(
                level = level,
                event = event,
                fields = fields,
                error = PendingError(errorCode, reason, throwable, diagnosticId),
            ),
            traceContext,
        )
        return diagnosticId
    }

    private fun emit(event: PendingLogEvent) {
        emit(event, requireTraceContext())
    }

    private fun emit(event: PendingLogEvent, traceContext: TraceContext) {
        val result = sanitizer.sanitize(
            event,
            SanitizationContext(
                eventId = idGenerator.eventId(),
                timestampMs = timeSource.wallTimeMs(),
                elapsedRealtimeMs = timeSource.elapsedRealtimeMs(),
                processSessionId = processSessionProvider.current(),
                secureSessionId = secureSessionProvider.currentOrNull(),
                trace = traceContext.snapshot(),
            ),
        )
        if (result is SanitizationResult.Accepted) {
            sinks.forEach { sink ->
                try {
                    sink.emit(result.record)
                } catch (_: Exception) {
                    // A diagnostic sink must never change application behavior.
                }
            }
        }
    }

    private fun requireTraceContext(): TraceContext = checkNotNull(TraceContext.currentOrNull()) {
        "Logger call requires an active trace"
    }

    private fun notifyStarted(span: SpanStarted) {
        lifecycleSinks.forEach { sink ->
            try {
                sink.onSpanStarted(span)
            } catch (_: Exception) {
                // Platform tracing is best-effort and cannot own business control flow.
            }
        }
    }

    private fun notifyCompleted(span: SpanCompleted) {
        lifecycleSinks.forEach { sink ->
            try {
                sink.onSpanCompleted(span)
            } catch (_: Exception) {
                // Platform tracing is best-effort and cannot own business control flow.
            }
        }
    }
}
