package com.aozijx.passly.feature.vault.presentation.detail

import com.aozijx.passly.domain.sensitive.OwnedChars
import com.aozijx.passly.feature.vault.presentation.detail.ui.model.ScopedSensitiveText

/** Owns the payload and cleanup hooks for images derived from it. */
class DetailQrPayload internal constructor(private val chars: OwnedChars) : ScopedSensitiveText, AutoCloseable {
    private val cleanups = mutableSetOf<() -> Unit>()
    private var closed = false
    override val isEmpty: Boolean get() = chars.isEmpty

    override fun <R> useChars(block: (CharArray) -> R): R {
        val copy = chars.toCharArray()
        return try {
            block(copy)
        } finally {
            copy.fill('\u0000')
        }
    }

    fun onClose(cleanup: () -> Unit): AutoCloseable {
        val runNow = synchronized(this) {
            if (closed) true else {
                cleanups += cleanup
                false
            }
        }
        if (runNow) cleanup()
        return AutoCloseable { synchronized(this) { cleanups -= cleanup } }
    }

    override fun close() {
        val pending = synchronized(this) {
            if (closed) return
            closed = true
            chars.wipe()
            cleanups.toList().also { cleanups.clear() }
        }
        pending.forEach { it() }
    }

    override fun toString() = "***"
}
