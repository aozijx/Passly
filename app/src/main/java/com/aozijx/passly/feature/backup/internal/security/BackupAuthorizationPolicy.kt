package com.aozijx.passly.feature.backup.internal.security

import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.AuthenticationRequest
import com.aozijx.passly.domain.access.model.AuthenticationResult
import com.aozijx.passly.domain.access.port.AuthenticationManager
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import javax.inject.Inject

internal class BackupAuthorizationPolicy @Inject constructor(
    private val authenticationManager: AuthenticationManager,
    private val sessionAccessState: SecureSessionAccessState,
) {
    suspend fun authorize(purpose: AuthenticationPurpose): BackupAuthorizationResult {
        if (!sessionAccessState.hasFullSecureSessionAccess()) {
            return BackupAuthorizationResult.Denied
        }
        return when (authenticationManager.authenticate(AuthenticationRequest(purpose))) {
            is AuthenticationResult.Success ->
                if (sessionAccessState.hasFullSecureSessionAccess()) {
                    BackupAuthorizationResult.Authorized
                } else {
                    BackupAuthorizationResult.Denied
                }

            is AuthenticationResult.Cancelled -> BackupAuthorizationResult.Cancelled
            is AuthenticationResult.Failure -> BackupAuthorizationResult.Denied
        }
    }
}

internal enum class BackupAuthorizationResult {
    Authorized,
    Cancelled,
    Denied,
}
