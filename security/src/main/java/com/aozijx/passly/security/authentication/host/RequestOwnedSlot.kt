package com.aozijx.passly.security.authentication.host

import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CancellableContinuation

internal class AuthUiRequestId

internal class ActiveBiometricPrompt(
    val continuation: CancellableContinuation<BiometricHostResult>,
    private val cancelPromptAction: () -> Unit,
) {
    private val lock = Any()
    private var cancelled = false

    fun startIfActive(startPrompt: () -> Unit): Boolean = synchronized(lock) {
        if (cancelled || !continuation.isActive) return@synchronized false
        startPrompt()
        true
    }

    fun cancelPrompt() = synchronized(lock) {
        if (cancelled) return@synchronized
        cancelled = true
        cancelPromptAction()
    }

    fun cancel() {
        continuation.cancel()
        cancelPrompt()
    }
}

internal class RequestOwnedSlot<T : Any> {
    private data class OwnedValue<T>(
        val requestId: AuthUiRequestId,
        val value: T,
    )

    private val current = AtomicReference<OwnedValue<T>?>(null)

    fun install(requestId: AuthUiRequestId, value: T): Boolean =
        current.compareAndSet(null, OwnedValue(requestId, value))

    fun take(requestId: AuthUiRequestId): T? {
        while (true) {
            val owned = current.get() ?: return null
            if (owned.requestId !== requestId) return null
            if (current.compareAndSet(owned, null)) return owned.value
        }
    }

    fun takeCurrent(): T? = current.getAndSet(null)?.value
}
