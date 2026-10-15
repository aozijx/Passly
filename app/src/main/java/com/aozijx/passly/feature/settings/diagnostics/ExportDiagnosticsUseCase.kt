package com.aozijx.passly.feature.settings.diagnostics

import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.AuthorizationPermit
import com.aozijx.passly.domain.access.model.AuthorizationResult
import com.aozijx.passly.domain.access.model.AuthorizationScope
import com.aozijx.passly.domain.access.port.AuthorizationGate
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import javax.inject.Inject

sealed interface DiagnosticsExportTarget {
    data object Share : DiagnosticsExportTarget
    data class Document(val uri: String) : DiagnosticsExportTarget
}

interface DiagnosticsExportGateway {
    suspend fun export(
        permit: AuthorizationPermit,
        target: DiagnosticsExportTarget,
    ): Result<Unit>

    suspend fun discard(target: DiagnosticsExportTarget)
}

class ExportDiagnosticsUseCase @Inject constructor(
    private val secureSessionAccessState: SecureSessionAccessState,
    private val authorizationGate: AuthorizationGate,
    private val gateway: DiagnosticsExportGateway,
) {
    suspend operator fun invoke(target: DiagnosticsExportTarget): DiagnosticsExportResult {
        if (!secureSessionAccessState.hasFullSecureSessionAccess()) {
            gateway.discard(target)
            return DiagnosticsExportResult.SessionRestricted
        }
        return when (
            val authorization = authorizationGate.authorize(
                AuthorizationScope.Global(AuthenticationPurpose.EXPORT_DIAGNOSTICS),
            ) { permit -> gateway.export(permit, target) }
        ) {
            is AuthorizationResult.Allowed -> authorization.value.fold(
                onSuccess = { DiagnosticsExportResult.Completed },
                onFailure = {
                    gateway.discard(target)
                    DiagnosticsExportResult.Failed(it)
                },
            )
            is AuthorizationResult.Denied -> {
                gateway.discard(target)
                DiagnosticsExportResult.Denied
            }
            AuthorizationResult.Cancelled -> {
                gateway.discard(target)
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
