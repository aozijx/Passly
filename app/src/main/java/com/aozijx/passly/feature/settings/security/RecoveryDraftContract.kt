package com.aozijx.passly.feature.settings.security

sealed interface RecoveryDraftState {
    data object Empty : RecoveryDraftState
    data object Authenticating : RecoveryDraftState
    data object Generating : RecoveryDraftState
    data class Ready(val generationId: String) : RecoveryDraftState
    data object DraftExpired : RecoveryDraftState
    data object Committed : RecoveryDraftState
    data object Failed : RecoveryDraftState
}

fun RecoveryDraftState.messageOrNull(): String? = when (this) {
    RecoveryDraftState.DraftExpired -> "恢复码草稿已过期，请重新认证后生成。"
    else -> null
}
