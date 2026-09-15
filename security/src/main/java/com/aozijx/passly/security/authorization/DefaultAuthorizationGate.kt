package com.aozijx.passly.security.authorization

import com.aozijx.passly.domain.access.model.AuthInput
import com.aozijx.passly.domain.access.model.AuthorizationPermit
import com.aozijx.passly.domain.access.model.AuthorizationResult
import com.aozijx.passly.domain.access.model.AuthorizationScope
import com.aozijx.passly.domain.access.port.AuthorizationGate
import com.aozijx.passly.domain.access.port.AuthenticationManager
import com.aozijx.passly.domain.access.model.AuthenticationRequest
import com.aozijx.passly.domain.access.model.AuthenticationResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultAuthorizationGate @Inject constructor(
    private val authenticationManager: AuthenticationManager,
    private val permitRegistry: AuthorizationPermitRegistry,
) : AuthorizationGate {
    override suspend fun <T> authorize(
        scope: AuthorizationScope,
        input: AuthInput,
        block: suspend (AuthorizationPermit) -> T,
    ): AuthorizationResult<T> {
        val authentication = authenticationManager.authenticate(
            request = AuthenticationRequest(scope.purpose),
            input = input,
        )
        return when (authentication) {
            is AuthenticationResult.Success -> executeAuthorized(scope, block)
            is AuthenticationResult.Cancelled -> AuthorizationResult.Cancelled
            is AuthenticationResult.Failure -> AuthorizationResult.Denied(authentication.failure)
        }
    }

    private suspend fun <T> executeAuthorized(
        scope: AuthorizationScope,
        block: suspend (AuthorizationPermit) -> T,
    ): AuthorizationResult<T> {
        val permit = permitRegistry.issue(scope, PERMIT_TTL_MS)
        return try {
            AuthorizationResult.Allowed(block(permit))
        } finally {
            permitRegistry.revoke(permit)
        }
    }

    private companion object {
        const val PERMIT_TTL_MS = 10_000L
    }
}
