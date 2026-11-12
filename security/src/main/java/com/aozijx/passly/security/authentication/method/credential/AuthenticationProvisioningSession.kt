package com.aozijx.passly.security.authentication.method.credential

import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.SessionLockRequest

/** Narrow session contract needed by authentication-method provisioning. */
internal interface AuthenticationProvisioningSession {
    fun isRecoveryMode(): Boolean

    suspend fun lock(request: SessionLockRequest)

    suspend fun markAuthenticated(purpose: AuthenticationPurpose): Boolean
}
