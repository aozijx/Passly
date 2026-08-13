package com.aozijx.passly.security.authorization

import com.aozijx.passly.domain.auth.failure.AuthFailure
import com.aozijx.passly.domain.auth.model.AuthInput
import com.aozijx.passly.domain.auth.model.AuthorizationPermit
import com.aozijx.passly.domain.auth.model.AuthorizationResult
import com.aozijx.passly.domain.auth.model.AuthorizationScope
import com.aozijx.passly.domain.auth.port.AuthorizationGate
import com.aozijx.passly.domain.authentication.AuthenticationManager
import com.aozijx.passly.domain.authentication.AuthenticationRequest
import com.aozijx.passly.domain.authentication.AuthenticationResult
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
        val credential = input.consumeCredential()
        val authentication = try {
            authenticationManager.authenticate(
                request = AuthenticationRequest(scope.purpose),
                credential = credential,
            )
        } finally {
            credential?.fill('\u0000')
        }
        return when (authentication) {
            is AuthenticationResult.Success -> executeAuthorized(scope, block)
            is AuthenticationResult.Cancelled -> AuthorizationResult.Cancelled
            is AuthenticationResult.Failure -> AuthorizationResult.Denied(
                if (authentication.failure.authCode.name.contains("CREDENTIAL")) {
                    AuthFailure.CredentialIncorrect
                } else {
                    AuthFailure.SessionExpired
                }
            )
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
