package com.aozijx.passly.security.authentication.host

import com.github.f4b6a3.uuid.UuidCreator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeoutOrNull
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

class HostRegistrationToken internal constructor(
    val ownerId: String,
    val value: String
)

class HostLease internal constructor(
    val ownerId: String,
    val token: HostRegistrationToken,
    host: AuthUiHost,
    private val requestLock: Mutex,
) {
    private val hostReference = WeakReference(host)
    private val released = AtomicBoolean(false)

    fun hostOrNull(): AuthUiHost? = hostReference.get()?.takeIf {
        !released.get() && it.ownerId == ownerId && it.snapshot().usable
    }

    fun release() {
        if (released.compareAndSet(false, true)) requestLock.unlock()
    }

    internal fun isReleased(): Boolean = released.get()
}

@Singleton
class AuthenticationHostRegistry @Inject constructor() {
    private data class Registration(
        val ownerId: String,
        val token: HostRegistrationToken,
        val host: WeakReference<AuthUiHost>
    )

    private val registration = MutableStateFlow<Registration?>(null)
    private val uiRequestMutex = Mutex()

    fun register(host: AuthUiHost): HostRegistrationToken {
        val token =
            HostRegistrationToken(host.ownerId, UuidCreator.getTimeOrderedEpoch().toString())
        registration.value = Registration(
            ownerId = host.ownerId,
            token = token,
            host = WeakReference(host)
        )
        return token
    }

    fun unregister(token: HostRegistrationToken) {
        if (registration.value?.token == token) registration.value = null
    }

    fun invalidateOwner(ownerId: String) {
        if (registration.value?.ownerId == ownerId) registration.value = null
    }

    suspend fun awaitLease(timeoutMs: Long = HOST_READY_TIMEOUT_MS): HostLease? {
        uiRequestMutex.lock()
        var transferred = false
        try {
            val current = withTimeoutOrNull(timeoutMs) {
                registration.filterNotNull().first { candidate ->
                    candidate.host.get()?.snapshot()?.usable == true
                }
            } ?: return null
            val host = current.host.get()?.takeIf { it.snapshot().usable } ?: return null
            val lease = HostLease(current.ownerId, current.token, host, uiRequestMutex)
            transferred = true
            return lease
        } finally {
            if (!transferred) uiRequestMutex.unlock()
        }
    }

    fun isCurrent(lease: HostLease): Boolean {
        if (lease.isReleased()) return false
        val current = registration.value ?: return false
        return current.token == lease.token && lease.hostOrNull() != null
    }

    companion object {
        const val HOST_READY_TIMEOUT_MS = 500L
    }
}
