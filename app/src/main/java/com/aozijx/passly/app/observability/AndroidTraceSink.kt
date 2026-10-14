package com.aozijx.passly.app.observability

import android.os.Trace
import com.aozijx.passly.core.observability.OperationCode
import com.aozijx.passly.core.observability.Operations
import com.aozijx.passly.core.observability.SpanId
import com.aozijx.passly.core.observability.trace.SpanCompleted
import com.aozijx.passly.core.observability.trace.SpanStarted
import com.aozijx.passly.core.observability.trace.TraceLifecycleSink

internal interface AndroidTraceGateway {
    fun isEnabled(): Boolean
    fun beginAsyncSection(name: String, cookie: Int)
    fun endAsyncSection(name: String, cookie: Int)
}

class AndroidTraceSink internal constructor(
    private val gateway: AndroidTraceGateway = PlatformAndroidTraceGateway,
    private val cookieSeed: Int = 0x50415353,
) : TraceLifecycleSink {
    private val active = mutableMapOf<SpanId, ActiveTrace>()
    private val activeCookies = mutableSetOf<Int>()

    @Synchronized
    override fun onSpanStarted(span: SpanStarted) {
        if (span.trace.spanId in active) return
        val name = TRACE_NAMES[span.operation] ?: return
        val enabled = try {
            gateway.isEnabled()
        } catch (_: Exception) {
            false
        }
        if (!enabled) return

        var cookie = span.trace.spanId.value.hashCode() xor cookieSeed
        while (cookie in activeCookies) cookie += 1
        try {
            gateway.beginAsyncSection(name, cookie)
            active[span.trace.spanId] = ActiveTrace(name, cookie)
            activeCookies += cookie
        } catch (_: Exception) {
            // System tracing is an optional diagnostics side channel.
        }
    }

    @Synchronized
    override fun onSpanCompleted(span: SpanCompleted) {
        val trace = active.remove(span.trace.spanId) ?: return
        activeCookies -= trace.cookie
        try {
            gateway.endAsyncSection(trace.name, trace.cookie)
        } catch (_: Exception) {
            // System tracing is an optional diagnostics side channel.
        }
    }

    private data class ActiveTrace(val name: String, val cookie: Int)

    private companion object {
        val TRACE_NAMES: Map<OperationCode, String> = mapOf(
            Operations.ApplicationStartup to "Passly/AppStartup",
            Operations.ApplicationLifecycle to "Passly/AppLifecycle",
            Operations.DatabaseSession to "Passly/DatabaseSession",
            Operations.SecurityAuthentication to "Passly/Authentication",
            Operations.SecurityLock to "Passly/SecurityLock",
            Operations.AutofillLegacyRequest to "Passly/AutofillLegacy",
            Operations.AutofillCredentialRequest to "Passly/AutofillCredential",
            Operations.AutofillResultDelivery to "Passly/AutofillResult",
            Operations.AutofillSessionClose to "Passly/AutofillClose",
            Operations.DiagnosticsExport to "Passly/DiagnosticsExport",
        )
    }
}

private object PlatformAndroidTraceGateway : AndroidTraceGateway {
    override fun isEnabled(): Boolean = Trace.isEnabled()
    override fun beginAsyncSection(name: String, cookie: Int) = Trace.beginAsyncSection(name, cookie)
    override fun endAsyncSection(name: String, cookie: Int) = Trace.endAsyncSection(name, cookie)
}
