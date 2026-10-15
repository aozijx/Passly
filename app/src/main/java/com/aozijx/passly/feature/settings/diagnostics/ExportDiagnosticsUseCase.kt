package com.aozijx.passly.feature.settings.diagnostics

import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.AuthorizationPermit
import com.aozijx.passly.domain.access.model.AuthorizationResult
import com.aozijx.passly.domain.access.model.AuthorizationScope
import com.aozijx.passly.domain.access.port.AuthorizationGate
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import javax.inject.Inject

interface DiagnosticsExportGateway {
    suspend fun export(
        permit: AuthorizationPermit,
        request: DiagnosticsExportRequest,
    ): Result<Unit>

    suspend fun discard(request: DiagnosticsExportRequest)
}

class ExportDiagnosticsUseCase @Inject constructor(
    private val secureSessionAccessState: SecureSessionAccessState,
    private val authorizationGate: AuthorizationGate,
    private val gateway: DiagnosticsExportGateway,
) {
    suspend operator fun invoke(request: DiagnosticsExportRequest): DiagnosticsExportResult {
        if (!secureSessionAccessState.hasFullSecureSessionAccess()) {
            gateway.discard(request)
            return DiagnosticsExportResult.SessionRestricted
        }
        return when (
            val authorization = authorizationGate.authorize(
                AuthorizationScope.Global(AuthenticationPurpose.EXPORT_DIAGNOSTICS),
            ) { permit -> gateway.export(permit, request) }
        ) {
            is AuthorizationResult.Allowed -> authorization.value.fold(
                onSuccess = { DiagnosticsExportResult.Completed },
                onFailure = {
                    gateway.discard(request)
                    DiagnosticsExportResult.Failed(it)
                },
            )
            is AuthorizationResult.Denied -> {
                gateway.discard(request)
                DiagnosticsExportResult.Denied
            }
            AuthorizationResult.Cancelled -> {
                gateway.discard(request)
                DiagnosticsExportResult.Cancelled
            }
        }
    }
}

sealed interface DiagnosticsExportResult {
    data object Completed : DiagnosticsExportResult
    data object Cancelled : DiagnosticsExportResult
    data object Denied : DiagnosticsExportResult
    data object SessionRestricted : DiagnosticsExportResult
    data class Failed(val cause: Throwable) : DiagnosticsExportResult
}
