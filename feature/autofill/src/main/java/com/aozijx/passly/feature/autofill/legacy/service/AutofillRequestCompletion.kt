package com.aozijx.passly.feature.autofill.legacy.service

import java.util.concurrent.atomic.AtomicBoolean

/** Enforces the platform contract that every fill request gets one terminal callback. */
internal class AutofillRequestCompletion<T>(
    private val onSuccess: (T?) -> Unit,
    private val onFailure: (CharSequence) -> Unit,
) {
    private val completed = AtomicBoolean(false)

    fun success(response: T?): Boolean = complete { onSuccess(response) }

    /** A cancelled request is completed with an empty result so Android can release the request. */
    fun cancel(): Boolean = success(null)

    fun failure(message: CharSequence): Boolean = complete { onFailure(message) }

    private fun complete(deliver: () -> Unit): Boolean {
        if (!completed.compareAndSet(false, true)) return false
        deliver()
        return true
    }
}
