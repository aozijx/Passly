package com.aozijx.passly.core.observability.trace

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.ThreadContextElement

class TraceContextElement(
    val traceContext: TraceContext,
) : ThreadContextElement<TraceContext?>,
    CoroutineContext.Element {
    override val key: CoroutineContext.Key<*> = Key

    override fun updateThreadContext(context: CoroutineContext): TraceContext? {
        val previous = CURRENT.get()
        CURRENT.set(traceContext)
        return previous
    }

    override fun restoreThreadContext(context: CoroutineContext, oldState: TraceContext?) {
        if (oldState == null) {
            CURRENT.remove()
        } else {
            CURRENT.set(oldState)
        }
    }

    companion object Key : CoroutineContext.Key<TraceContextElement> {
        private val CURRENT = ThreadLocal<TraceContext?>()

        internal fun currentOrNull(): TraceContext? = CURRENT.get()
    }
}
