package com.aozijx.passly.feature.autofill.presentation

import java.util.concurrent.atomic.AtomicBoolean

/** Claims one Activity result callback, releasing the claim only when platform delivery fails. */
internal class ActivityResultClaim {
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
