package com.aozijx.passly.feature.autofill.presentation.result

import java.util.concurrent.atomic.AtomicBoolean

/** Claims one Autofill Activity result, releasing the claim only when platform delivery fails. */
internal class AutofillActivityResultClaim {
    private val claimed = AtomicBoolean(false)

    val isClaimed: Boolean
        get() = claimed.get()

    fun commit(deliverToPlatform: (markPlatformCommitted: () -> Unit) -> Unit): Boolean {
        if (!claimed.compareAndSet(false, true)) return false
        var platformCommitted = false
        try {
            deliverToPlatform {
                check(!platformCommitted) { "Autofill platform result was marked committed more than once" }
                platformCommitted = true
            }
            check(platformCommitted) { "Autofill platform result callback returned without committing" }
        } catch (error: Throwable) {
            if (!platformCommitted) claimed.compareAndSet(true, false)
            throw error
        }
        return true
    }
}
