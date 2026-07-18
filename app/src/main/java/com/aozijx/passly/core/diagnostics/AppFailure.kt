package com.aozijx.passly.core.diagnostics

import com.github.f4b6a3.uuid.UuidCreator

enum class FailureOrigin { PLATFORM, SECURITY, DATA, DOMAIN, PRESENTATION }
enum class FailureSeverity { INFO, WARNING, ERROR, FATAL }
enum class RecoveryAction { NONE, RETRY, REAUTHENTICATE, OPEN_SETTINGS, CLEAR_INPUT }

interface AppFailure {
    val code: String
    val origin: FailureOrigin
    val severity: FailureSeverity
    val recoveryAction: RecoveryAction
    val correlationId: String
    val safeFields: Map<String, String>
}

data class StandardFailure(
    override val code: String,
    override val origin: FailureOrigin,
    override val severity: FailureSeverity,
    override val recoveryAction: RecoveryAction = RecoveryAction.NONE,
    override val correlationId: String = UuidCreator.getTimeOrderedEpoch().toString(),
    override val safeFields: Map<String, String> = emptyMap()
) : AppFailure
