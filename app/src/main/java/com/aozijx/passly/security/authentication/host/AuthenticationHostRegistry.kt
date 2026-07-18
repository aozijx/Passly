package com.aozijx.passly.security.authentication.host

import com.github.f4b6a3.uuid.UuidCreator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

class HostRegistrationToken internal constructor(
    val ownerId: String,
    val value: String
)

class HostLease internal constructor(
    val ownerId: String,
    val token: HostRegistrationToken,
    host: AuthUiHost
) {
    private val hostReference = WeakReference(host)

    fun hostOrNull(): AuthUiHost? = hostReference.get()?.takeIf {
        it.ownerId == ownerId && it.snapshot().usable
    }
}

@Singleton
class AuthenticationHostRegistry @Inject constructor() {
    private data class Registration(
        val ownerId: String,
        val token: HostRegistrationToken,
        val host: WeakReference<AuthUiHost>,
        val registeredAtMs: Long
    )

    private val registration = MutableStateFlow<Registration?>(null)

    fun register(host: AuthUiHost): HostRegistrationToken {
        val token =
            HostRegistrationToken(host.ownerId, UuidCreator.getTimeOrderedEpoch().toString())
        registration.value = Registration(
            ownerId = host.ownerId,
            token = token,
            host = WeakReference(host),
            registeredAtMs = System.currentTimeMillis()
        )
        return token
    }

    fun unregister(token: HostRegistrationToken) {
        if (registration.value?.token == token) registration.value = null
    }

    fun invalidateOwner(ownerId: String) {
        if (registration.value?.ownerId == ownerId) registration.value = null
    }

    suspend fun awaitLease(timeoutMs: Long = HOST_READY_TIMEOUT_MS): HostLease? =
        withTimeoutOrNull(timeoutMs) {
            registration.filterNotNull().first { current ->
                current.host.get()?.snapshot()?.usable == true
            }.let { current ->
                val host = current.host.get() ?: return@let null
                HostLease(current.ownerId, current.token, host)
            }
        }

    fun isCurrent(lease: HostLease): Boolean {
        val current = registration.value ?: return false
        return current.token == lease.token && lease.hostOrNull() != null
    }

    companion object {
        const val HOST_READY_TIMEOUT_MS = 500L
        const val CONFIGURATION_RECONNECT_WINDOW_MS = 2_000L
    }
}
