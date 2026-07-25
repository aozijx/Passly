package com.aozijx.passly.domain.failure

enum class FailureOrigin { PLATFORM, SECURITY, DATA, DOMAIN, PRESENTATION }
enum class FailureSeverity { INFO, WARNING, ERROR, FATAL }
enum class RecoveryAction { NONE, RETRY, REAUTHENTICATE, OPEN_SETTINGS, CLEAR_INPUT }

/**
 * 应用失败模型。
 *
 * 从 foundation/diagnostics 移入 domain/failure，失败模型不属于日志模块。
 */
interface AppFailure {
    val code: String
    val origin: FailureOrigin
    val severity: FailureSeverity
    val recoveryAction: RecoveryAction
    val correlationId: String
}

/**
 * 通用失败实现（用于 data 层映射等场景）。
 */
data class StandardFailure(
    override val code: String,
    override val origin: FailureOrigin,
    override val severity: FailureSeverity,
    override val recoveryAction: RecoveryAction = RecoveryAction.NONE,
    override val correlationId: String = java.util.UUID.randomUUID().toString()
) : AppFailure
