package com.aozijx.passly.domain.access.port

import com.aozijx.passly.domain.access.model.AuthenticationAttemptState
import com.aozijx.passly.domain.access.model.AuthenticationMethod

interface AuthenticationAttemptStore {
    suspend fun update(
        method: AuthenticationMethod,
        transform: (AuthenticationAttemptState) -> AuthenticationAttemptState,
    ): AuthenticationAttemptState
}
