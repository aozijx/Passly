package com.aozijx.passly.security.authorization

import com.aozijx.passly.domain.access.model.AuthorizationPermit
import com.aozijx.passly.domain.access.model.AuthorizationScope
import com.aozijx.passly.domain.access.model.MonotonicClock
import com.aozijx.passly.domain.access.port.AuthorizationPermitRevoker
import com.aozijx.passly.domain.access.port.AuthorizationPermitVerifier
import java.util.IdentityHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthorizationPermitRegistry @Inject constructor(
    private val clock: MonotonicClock,
) : AuthorizationPermitVerifier, AuthorizationPermitRevoker {
    private val grants = IdentityHashMap<AuthorizationPermit, Grant>()

    internal fun issue(scope: AuthorizationScope, ttlMs: Long): AuthorizationPermit =
        synchronized(grants) {
            val permit = RegistryPermit()
            grants[permit] = Grant(
                scope = scope,
                expiresAtMs = clock.elapsedMs() + ttlMs,
            )
            permit
        }

    override fun consume(
        permit: AuthorizationPermit,
        expectedScope: AuthorizationScope,
    ): Boolean = synchronized(grants) {
        val grant = grants.remove(permit) ?: return@synchronized false
        grant.scope == expectedScope && clock.elapsedMs() < grant.expiresAtMs
    }

    internal fun revoke(permit: AuthorizationPermit) {
        synchronized(grants) { grants.remove(permit) }
    }

    override fun revokeAll() {
        synchronized(grants) { grants.clear() }
    }

    internal fun activePermitCount(): Int = synchronized(grants) { grants.size }

    private class RegistryPermit : AuthorizationPermit

    private data class Grant(
        val scope: AuthorizationScope,
        val expiresAtMs: Long,
    )
}
