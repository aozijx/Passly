package com.aozijx.passly.feature.database.reset

import com.aozijx.passly.core.error.model.AppError
import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.AuthorizationResult
import com.aozijx.passly.domain.access.model.AuthorizationScope
import com.aozijx.passly.domain.access.port.AuthorizationGate
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import javax.inject.Inject

class ResetDatabaseUseCase @Inject constructor(
    private val secureSessionAccessState: SecureSessionAccessState,
    private val authorizationGate: AuthorizationGate,
    private val databaseResetGateway: DatabaseResetGateway,
) {
    suspend operator fun invoke(): DatabaseResetResult {
        if (!secureSessionAccessState.hasFullSecureSessionAccess()) {
            return DatabaseResetResult.SessionRestricted
        }
        return when (
            val authorization = authorizationGate.authorize(
                AuthorizationScope.Global(AuthenticationPurpose.CLEAR_DATABASE),
            ) { databaseResetGateway.reset() }
        ) {
            is AuthorizationResult.Allowed -> when (val reset = authorization.value) {
                is AppResult.Success -> DatabaseResetResult.Completed
                is AppResult.Failure -> DatabaseResetResult.Failed(reset.error)
            }
            is AuthorizationResult.Denied -> DatabaseResetResult.AuthorizationDenied
            AuthorizationResult.Cancelled -> DatabaseResetResult.Cancelled
        }
    }
}

sealed interface DatabaseResetResult {
    data object Completed : DatabaseResetResult
    data object Cancelled : DatabaseResetResult
    data object SessionRestricted : DatabaseResetResult
    data object AuthorizationDenied : DatabaseResetResult
    data class Failed(val error: AppError) : DatabaseResetResult
}