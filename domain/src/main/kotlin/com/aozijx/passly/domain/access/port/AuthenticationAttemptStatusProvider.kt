package com.aozijx.passly.domain.access.port

import com.aozijx.passly.domain.access.model.AuthenticationAttemptStatus
import com.aozijx.passly.domain.access.model.AuthenticationMethod

interface AuthenticationAttemptStatusProvider {
    suspend fun status(method: AuthenticationMethod): AuthenticationAttemptStatus
}
