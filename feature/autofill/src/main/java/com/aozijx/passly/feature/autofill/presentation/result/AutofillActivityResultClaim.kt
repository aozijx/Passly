package com.aozijx.passly.feature.autofill.presentation.result

import java.util.concurrent.atomic.AtomicBoolean

/** Claims one Autofill Activity result, releasing the claim only when platform delivery fails. */
internal class AutofillActivityResultClaim {
    private val claimed = AtomicBoolean(false)

    val isClaimed: Boolean
        get() = claimed.get()

    fun commit(deliverToPlatform: () -> Unit): Boolean {
        if (!claimed.compareAndSet(false, true)) return false
        try {
            deliverToPlatform()
        } catch (error: Throwable) {
            claimed.compareAndSet(true, false)
            throw error
        }
        return true
    }
}
