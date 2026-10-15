package com.aozijx.passly.core.observability.trace

import com.aozijx.passly.core.observability.Logger
import com.aozijx.passly.core.observability.LogStatements
import com.aozijx.passly.core.observability.OperationCode
import com.aozijx.passly.core.observability.TraceResumeResult
import com.aozijx.passly.core.observability.TraceScope
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-local bridge for continuing a trace across callbacks that cannot carry coroutine context.
 * Keys are opaque routing identifiers and are never written to logs.
 */
@Singleton
class TraceContinuationStore @Inject constructor(
    private val logger: Logger,
) {
    private val tokens = LinkedHashMap<String, TraceToken>(MAX_ENTRIES, 0.75f, true)

    fun capture(key: String) {
        requireValidKey(key)
        val token = logger.captureTraceToken()
        synchronized(tokens) {
            tokens[key] = token
            while (tokens.size > MAX_ENTRIES) {
                tokens.entries.iterator().run {
                    next()
                    remove()
                }
            }
        }
    }

    suspend fun <T> resume(
        key: String,
        operation: OperationCode,
        block: suspend TraceScope.() -> T,
    ): TraceResumeResult<T> {
        requireValidKey(key)
        val token = synchronized(tokens) { tokens.remove(key) }
            ?: return TraceResumeResult.Unavailable
        return logger.resumeTrace(token, operation, block)
    }

    suspend fun <T> continueOrRoot(
        key: String,
        operation: OperationCode,
        block: suspend TraceScope.() -> T,
    ): T = when (val resumed = resume(key, operation, block)) {
        is TraceResumeResult.Resumed -> resumed.value
        TraceResumeResult.Unavailable -> logger.withRootTrace(operation) {
            logger.log(LogStatements.Application.traceResumeUnavailable())
            block()
        }
    }

    fun discard(key: String) {
        requireValidKey(key)
        synchronized(tokens) { tokens.remove(key) }
    }

    private fun requireValidKey(key: String) {
        require(key.length in 8..128 && OPAQUE_KEY.matches(key)) { "Invalid continuation key" }
    }

    private companion object {
        const val MAX_ENTRIES = 128
        val OPAQUE_KEY = Regex("^[A-Za-z0-9_-]+$")
    }
}
