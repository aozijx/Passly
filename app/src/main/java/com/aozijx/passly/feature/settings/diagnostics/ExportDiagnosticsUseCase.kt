package com.aozijx.passly.feature.settings.diagnostics

import com.aozijx.passly.domain.access.model.AuthenticationPurpose
import com.aozijx.passly.domain.access.model.AuthorizationResult
import com.aozijx.passly.domain.access.model.AuthorizationScope
import com.aozijx.passly.domain.access.port.AuthorizationGate
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import javax.inject.Inject

fun interface DiagnosticsExportGateway {
    suspend fun exportAndShare(): Result<Unit>
}

class ExportDiagnosticsUseCase @Inject constructor(
    private val secureSessionAccessState: SecureSessionAccessState,
    private val authorizationGate: AuthorizationGate,
    private val gateway: DiagnosticsExportGateway,
) {
    suspend operator fun invoke(): DiagnosticsExportResult {
        if (!secureSessionAccessState.hasFullSecureSessionAccess()) {
            return DiagnosticsExportResult.SessionRestricted
        }
        return when (
            val authorization = authorizationGate.authorize(
                AuthorizationScope.Global(AuthenticationPurpose.EXPORT_DIAGNOSTICS),
            ) { gateway.exportAndShare() }
        ) {
            is AuthorizationResult.Allowed -> authorization.value.fold(
                onSuccess = { DiagnosticsExportResult.Completed },
                onFailure = { DiagnosticsExportResult.Failed(it) },
            )
            is AuthorizationResult.Denied -> DiagnosticsExportResult.Denied
            AuthorizationResult.Cancelled -> DiagnosticsExportResult.Cancelled
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