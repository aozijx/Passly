package com.aozijx.passly.feature.autofill.runtime

import com.aozijx.passly.domain.autofill.model.AutofillInteractionId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Process-lifetime expiry for interactions abandoned by Android's system UI. */
@Singleton
class AutofillInteractionTimeout private constructor(
    private val store: AutofillInteractionStore,
    private val unlockLease: AutofillUnlockLease,
    private val scope: CoroutineScope,
    private val timeoutMillis: Long,
    private val enabled: Boolean,
) {
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
        scope.launch {
            delay(timeoutMillis)
            val result = store.expireIfIdle(interactionId)
            if (result == null) {
                schedule(interactionId)
                return@launch
            }
            if (result.transitioned) unlockLease.release(interactionId)
            store.removeTerminal(interactionId)
        }
    }

    companion object {
        const val DEFAULT_TIMEOUT_MILLIS: Long = 60_000L

        internal fun disabled(
            store: AutofillInteractionStore,
            unlockLease: AutofillUnlockLease,
        ): AutofillInteractionTimeout = AutofillInteractionTimeout(
            store = store,
            unlockLease = unlockLease,
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
            timeoutMillis = DEFAULT_TIMEOUT_MILLIS,
            enabled = false,
        )
    }
}
