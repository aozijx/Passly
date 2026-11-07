package com.aozijx.passly.feature.autofill.runtime

import com.aozijx.passly.domain.autofill.model.AutofillInteractionId
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Process-lifetime expiry for interactions abandoned by Android's system UI. */
@Singleton
internal class AutofillInteractionTimeout private constructor(
    private val store: AutofillInteractionStore,
    private val unlockLease: AutofillUnlockLease,
    private val scope: CoroutineScope,
    private val timeoutMillis: Long,
    private val enabled: Boolean,
) {
    private val timers = ConcurrentHashMap<AutofillInteractionId, Channel<Unit>>()

    @Inject
    constructor(
        store: AutofillInteractionStore,
        unlockLease: AutofillUnlockLease,
    ) : this(
        store = store,
        unlockLease = unlockLease,
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
        timeoutMillis = DEFAULT_TIMEOUT_MILLIS,
        enabled = true,
    )

    internal constructor(
        store: AutofillInteractionStore,
        unlockLease: AutofillUnlockLease,
        scope: CoroutineScope,
        timeoutMillis: Long,
    ) : this(store, unlockLease, scope, timeoutMillis, enabled = true)

    fun schedule(interactionId: AutofillInteractionId) {
        if (!enabled) return
        timers.computeIfAbsent(interactionId) { id -> startTimer(id) }.trySend(Unit)
    }

    private fun startTimer(interactionId: AutofillInteractionId): Channel<Unit> {
        val timer = Channel<Unit>(Channel.CONFLATED)
        scope.launch {
            try {
                while (true) {
                    val reset = withTimeoutOrNull(timeoutMillis) { timer.receive() } != null
                    if (reset) continue

                    val result = store.expireIfIdle(interactionId)
                    if (result == null) continue
                    if (result.transitioned) unlockLease.release(interactionId)
                    store.removeTerminal(interactionId)
                    return@launch
                }
            } finally {
                timers.remove(interactionId, timer)
                timer.close()
            }
        }
        return timer
    }

    companion object {
        const val DEFAULT_TIMEOUT_MILLIS: Long = 60_000L

        internal fun disabled(
            store: AutofillInteractionStore,
            unlockLease: AutofillUnlockLease,
        ): AutofillInteractionTimeout =
            AutofillInteractionTimeout(
                store = store,
                unlockLease = unlockLease,
                scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
                timeoutMillis = DEFAULT_TIMEOUT_MILLIS,
                enabled = false,
            )
    }
}
